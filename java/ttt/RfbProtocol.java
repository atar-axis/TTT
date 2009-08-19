// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universität München
// 
//    This file is part of TeleTeachingTool.
//
//    TeleTeachingTool is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    TeleTeachingTool is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with TeleTeachingTool.  If not, see <http://www.gnu.org/licenses/>.

package ttt;

//
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import ttt.messages.Annotation;
import ttt.messages.CopyRectMessage;
import ttt.messages.CursorMessage;
import ttt.messages.CursorPositionMessage;
import ttt.messages.DeleteAllAnnotation;
import ttt.messages.DeleteAnnotation;
import ttt.messages.HextileMessage;
import ttt.messages.Message;
import ttt.messages.MessageConsumer;
import ttt.messages.MessageProducer;
import ttt.messages.RawMessage;
import ttt.messages.UserInputMessage;
import ttt.messages.WhiteboardMessage;

public class RfbProtocol extends GraphicsContext implements Runnable, MessageProducer, MessageConsumer {

    // I/O
    private Connection connection;
    private DataInputStream is;
    private OutputStream os;

    // The constructor
    public RfbProtocol(Connection connection) {
        super(connection.getProtocolPreferences());
        this.connection = connection;

        if (prefs.framebufferWidth > 1024 || prefs.framebufferHeight > 768) {
            TTT.showMessage("Warning:\n" + "The current VNC desktop size is " + prefs.framebufferWidth + "x"
                    + prefs.framebufferHeight + ".\n" + "A size of up to 1024x768 is recommended.",
                    "Warning: large resolution", JOptionPane.WARNING_MESSAGE);
        }

        // streams
        is = new DataInputStream(connection.getInputStream());
        os = connection.getOutputStream();
    }

    public void requestFullscreenUpdate() throws IOException {
        // request full framebuffer update
        Constants.writeFramebufferUpdateRequestMessage(connection.getOutputStream(), 0, 0, prefs.framebufferWidth,
                prefs.framebufferHeight, false);
    }

    /*******************************************************************************************************************
     * protocol processing thread
     ******************************************************************************************************************/

    private Thread thread;
    private boolean running;

    // start thread
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void close() {
        running = false;
        if (connection != null)
            connection.close();
        connection = null;

        // stop thread
        if (thread != null && thread.isAlive()) {
            // end Thread
            is = null;
            os = null;

            // leave wait()
            synchronized (this) {
                notify();
            }

            // wait until finished to avoid exceptions
            while (thread != null && thread.isAlive())
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
        }

        super.close();
    }

    // Thread
    public void run() {
        running = true;
        while (running)
            try {
                processProtocol();
            } catch (Exception e) {
                if (running) {
                    // something happened (lost connection; out of sync; ...)
                    System.out.println("Protocol Error: " + e);
                    e.printStackTrace();

                    // try to continue
                    connection.reconnect();
                }
            }
    }

    /*******************************************************************************************************************
     * protocol processing
     ******************************************************************************************************************/

    public void processProtocol() throws Exception {
        Constants
                .writeFramebufferUpdateRequestMessage(os, 0, 0, prefs.framebufferWidth, prefs.framebufferHeight, false);

        // main dispatch loop
        while (running) {
            // Read message type from the server.
            int msgType = is.readUnsignedByte();

            if (!running)
                break;

            // set timestamp of message
            int timestamp = getTimestamp();

            // Process the message depending on its type.
            switch (msgType) {
            case Constants.FramebufferUpdate:
                is.readByte(); // padding
                int updateNRects = is.readUnsignedShort();

                for (int i = 0; i < updateNRects; i++) {
                    readFramebufferUpdateRectHdr();
                    int rx = updateRectX, ry = updateRectY;
                    int rw = updateRectW, rh = updateRectH;

                    // System.out.println(rx + "," + ry + " - " + rw + "," + rh + " Encoding "
                    // + Constants.encodingToString(updateRectEncoding));

                    if (updateRectEncoding == Constants.EncodingLastRect)
                        break;

                    // TODO: support changing of framebuffer size
                    // if (updateRectEncoding == EncodingNewFBSize) {
                    // setFramebufferSize(rw, rh);
                    // updateFramebufferSize();
                    // break;
                    // }

                    switch (updateRectEncoding) {
                    case Constants.EncodingRaw:
                        handleMessage(new RawMessage(timestamp, rx, ry, rw, rh, this, is));
                        break;
                    case Constants.EncodingCopyRect:
                        handleMessage(new CopyRectMessage(timestamp, rx, ry, rw, rh, this, is));
                        break;
                    case Constants.EncodingHextile:
                        // if (!raw)
                        handleMessage(new HextileMessage(timestamp, rx, ry, rw, rh, this, is));
                        // else {
                        // System.out.println("HEXTILE -> INTERLACED");
                        // new HextileMessage(timestamp, rx, ry, rw, rh, this, is);
                        // handleMessage(InterlacedRawMessage
                        // .createInterlacedMessages(this, timestamp, rx, ry, rw, rh));
                        // }
                        break;
                    case Constants.EncodingXCursor:
                    case Constants.EncodingRichCursor:
                        handleMessage(new CursorMessage(updateRectEncoding, timestamp, rx, ry, rw, rh, this, is));
                        break;
                    case Constants.EncodingPointerPos:
                        handleMessage(new CursorPositionMessage(timestamp, rx, ry, this));
                        break;
                    default:
                        throw new Exception("Unknown RFB rectangle encoding " + updateRectEncoding);
                    }
                }

                boolean fullUpdateNeeded = false;

                // Defer framebuffer update request if necessary. But wake up
                // immediately on keyboard or mouse event.
                if (Constants.deferUpdateRequests > 0) {
                    synchronized (this) {
                        try {
                            wait(Constants.deferUpdateRequests);
                        } catch (InterruptedException e) {}
                    }
                }

                // whiteboard disables desktop repainting
                if (!isWhiteboardEnabled())
                    // request next update
                    Constants.writeFramebufferUpdateRequestMessage(os, 0, 0, prefs.framebufferWidth,
                            prefs.framebufferHeight, !fullUpdateNeeded);

                break;

            case Constants.SetColourMapEntries:
                // throw new Exception("Can't handle SetColourMapEntries message");
                // ignore
                System.out.println("skip unsupported SetColourMapEntries message");
                is.skipBytes(3);
                int number = is.readUnsignedShort();
                is.skipBytes(number * 6);
                break;

            case Constants.Bell:
                Toolkit.getDefaultToolkit().beep();
                break;

            case Constants.ServerCutText:
                // read and ignore
                readServerCutText();
                break;

            default:
                throw new Exception("Unknown RFB message type " + msgType);
            }
        }
    }

    private long starttime = System.currentTimeMillis();

    long getStarttime() {
        return starttime;
    }

    public int getTimestamp() {
        return (int) (System.currentTimeMillis() - starttime);
    }

    /*******************************************************************************************************************
     * server to client message handling *
     ******************************************************************************************************************/

    private int updateRectX, updateRectY, updateRectW, updateRectH, updateRectEncoding;

    // Read a FramebufferUpdate rectangle header
    private void readFramebufferUpdateRectHdr() throws Exception {

        updateRectX = is.readUnsignedShort();
        updateRectY = is.readUnsignedShort();
        updateRectW = is.readUnsignedShort();
        updateRectH = is.readUnsignedShort();
        updateRectEncoding = is.readInt();

        if (updateRectEncoding == Constants.EncodingLastRect || updateRectEncoding == Constants.EncodingNewFBSize)
            return;

        if (updateRectX + updateRectW > prefs.framebufferWidth || updateRectY + updateRectH > prefs.framebufferHeight) {
            throw new Exception("Framebuffer update rectangle too large: " + updateRectW + "x" + updateRectH + " at ("
                    + updateRectX + "," + updateRectY + ")");
        }
    }

    // Read a ServerCutText message
    private String readServerCutText() throws IOException {
        byte[] pad = new byte[3];
        is.readFully(pad);
        int len = is.readInt();
        byte[] text = new byte[len];
        is.readFully(text);
        return new String(text);
    }

    /*******************************************************************************************************************
     * client to server message handling *
     ******************************************************************************************************************/
    // notify all listeners
    public void handleMessage(Message[] messages) {
        for (int i = 0; i < messages.length; i++) {
            handleMessage(messages[i]);
        }
    }

    // nearly invisible cursor (just a small dot)
    static private Cursor invisibleCursor;
    static {
        int[] pixel = { 0x00000000, 0xFF000000, 0xFF000000, 0x00000000 //
                , 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000 //
                , 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000 //
                , 0x00000000, 0xFF000000, 0xFF000000, 0x00000000 };

        Dimension dimension = Toolkit.getDefaultToolkit().getBestCursorSize(4, 4);
        BufferedImage image = new BufferedImage(dimension.width, dimension.height, BufferedImage.TYPE_4BYTE_ABGR);
        image.setRGB(0, 0, 4, 4, pixel, 0, 4);

        invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(1, 1), "Invisible");
    }

    private Cursor defaultCursor = Cursor.getDefaultCursor();
    private Cursor customCursor;

    private void setDefaultCursor(Cursor cursor) {
        defaultCursor = cursor;
        if (customCursor == null)
            setCursor(defaultCursor);
    }

    public void setCustomCursor(Cursor cursor) {
        if (cursor != null)
            setCursor(cursor);
        else
            setCursor(defaultCursor);
        customCursor = cursor;
    }

    // handle message delivered by processProtocol or Client input
    public void handleMessage(Message message) {
        // handle user input events
        if (message instanceof UserInputMessage) {
            try {
                // whiteboard disables output
                if (!isWhiteboardEnabled())
                    // send user input to RFB/VNC Server
                    ((UserInputMessage) message).writeRFB(os);
            } catch (IOException e) {}
        }

        // handle other
        else {
            // add timestamp
            // TODO: should use NO_TIMESTAMP instead
            if (message.getTimestamp() == 0)
                message.setTimestamp(getTimestamp());

            // move local cursor
            if (message.getEncoding() == Constants.EncodingTTTCursorPosition) {
                message.paint(this);
            }

            // keep cursor for late comers
            else if (message instanceof CursorMessage) {
                setCurrentCursorMessage(((CursorMessage) message).copy());
                setDefaultCursor(invisibleCursor);
                // NOTE: no need for calling paint, because messages from VNC Server are already painted
            }

            // enable/disable whiteboard
            else if (message instanceof WhiteboardMessage) {
                message.paint(this);

                // request next update (if desktop is visible)
                if (!isWhiteboardEnabled())
                    try {
                        Constants.writeFramebufferUpdateRequestMessage(os, 0, 0, prefs.framebufferWidth,
                                prefs.framebufferHeight, false);
                    } catch (IOException e) {}
            }

            // show annotations
            else if (message instanceof Annotation) {
                if (((Annotation) message).temporary) {
                    // only show but don't send temporary annotation
                    // TODO: maybe handle in paint instead
                    setTemporaryAnnotation((Annotation) message);
                    return;

                } else {
                    // clear temporary annotation
                    clearTemporaryAnnotation();

                    // paint final annotation
                    message.paint(this);

                    // buffer, because needed to update after whiteboard switch
                    bufferAnnotation((Annotation) message);
                }
            }

            // handle message
            deliverMessage(message);

            // write buffered annotations
            // NOTE: must be done AFTER whiteboard message (which deletes annotations)
            if (message instanceof WhiteboardMessage) {

                // get timestamp of WhiteboardMessage
                int timestamp = message.getTimestamp();
                // get corresponding buffer
                ArrayList<Annotation> bufferedAnnotations = getBufferedAnnotations(whiteboardPage);
                for (int i = 0; i < bufferedAnnotations.size(); i++) {
                    Annotation annotation = bufferedAnnotations.get(i);
                    // set timestamp same as WhiteboardMessage
                    annotation.setTimestamp(timestamp);
                    // display
                    annotation.paint(this);
                    // send to consumers
                    deliverMessage(annotation);
                }
            }
        }
    }

    // ////////////////////////////////////////////////////////////////
    // Annotations
    // ////////////////////////////////////////////////////////////////

    // one buffer for each page
    private ArrayList<ArrayList<Annotation>> annotationBuffers = new ArrayList<ArrayList<Annotation>>();

    // returns annotation buffer for given page number
    private ArrayList<Annotation> getBufferedAnnotations(int page) {
        // initialize annotation buffers up to given page (if needed)
        for (int i = annotationBuffers.size(); i <= page; i++)
            annotationBuffers.add(new ArrayList<Annotation>());

        return annotationBuffers.get(page);
    }

    // add annotations to annoation list
    private void bufferAnnotation(Annotation annotation) {
        if (annotation instanceof DeleteAllAnnotation)
            // remove all annotations
            getBufferedAnnotations(whiteboardPage).clear();

        else if (annotation instanceof DeleteAnnotation) {
            // find and remove annotations at given coordinates
            ArrayList<Annotation> buffer = getBufferedAnnotations(whiteboardPage);
            int x = ((DeleteAnnotation) annotation).getX();
            int y = ((DeleteAnnotation) annotation).getY();
            int i = 0;
            while (i < buffer.size()) {
                if (buffer.get(i).contains(x, y))
                    buffer.remove(i);
                else
                    i++;
            }

        } else
            // at to buffer
            getBufferedAnnotations(whiteboardPage).add(annotation);
    }

    /*******************************************************************************************************************
     * implement MessageProducer
     ******************************************************************************************************************/

    private ArrayList<MessageConsumer> messageConsumers = new ArrayList<MessageConsumer>();

    // register listener
    public void addMessageConsumer(MessageConsumer messageConsumer) {
        synchronized (messageConsumers) {
            messageConsumers.add(messageConsumer);
        }
    }

    // unregister listener
    public void removeMessageConsumer(MessageConsumer messageConsumer) {
        synchronized (messageConsumers) {
            messageConsumers.remove(messageConsumer);
        }
    }

    // notify all listeners
    public void deliverMessage(Message message) {
        synchronized (messageConsumers) {
            int i = 0;
            while (i < messageConsumers.size()) {
                // TODO: maybe clone message
                // TODO: error handling
                try {
                    messageConsumers.get(i).handleMessage(message);
                } catch (Exception e) {
                    // TODO: maybe: remove consumer (if multiple errors?)
                }
                i++;
            }
        }
    }
    
    // MODMSG
    // save reference to PaintListener for calling nextWhiteboard
    private PaintListener pl;
    public void setPaintListener(PaintListener pl) { this.pl = pl; }
    public void nextWhiteboardPage() {
    	pl.nextWhiteboard();
    }
    public void newWhiteboardPage() {
    	pl.newWhiteboardPage();
    }

}
