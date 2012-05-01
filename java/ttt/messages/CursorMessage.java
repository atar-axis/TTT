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

/*
 * Created on 19.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.messages;

import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ttt.Constants;
import ttt.TTT;
import ttt.gui.GraphicsContext;
import ttt.postprocessing.flash.FlashContext;
import ttt.postprocessing.html5.Html5Context;
import biz.source_code.base64Coder.Base64Coder;

import com.anotherbigidea.flash.SWFConstants;
import com.anotherbigidea.flash.movie.ImageUtil;

// TODO: maybe split XCursor and RichCursor to seperate classes
public class CursorMessage extends FramebufferUpdateMessage {
    // TODO: think about superclass

    private byte[] data;
    private ByteArrayInputStream byteArrayInputStream;
    private DataInputStream is;

    private int encoding;

    // constructor
    private CursorMessage(int timestamp, int encodingType, int xhot, int yhot, int width, int height, byte[] data) {
        super(timestamp, xhot, yhot, width, height);

        // fix tag
        // Note: RFB and TTT cursor encodings have different tags, because RFB uses 4 bytes and TTT 1 byte for tag
        if (encodingType == Constants.EncodingXCursor)
            encodingType = Constants.EncodingTTTXCursor;
        else if (encodingType == Constants.EncodingRichCursor)
            encodingType = Constants.EncodingTTTRichCursor;

        // set tag
        encoding = encodingType;

        this.data = data;
        byteArrayInputStream = new ByteArrayInputStream(this.data);
        is = new DataInputStream(new BufferedInputStream(byteArrayInputStream));
    }

    // read from TTT input stream
    public CursorMessage(int timestamp, int encodingType, DataInputStream in, int size) throws IOException {
        this(timestamp, encodingType, in.readShort(), in.readShort(), in.readShort(), in.readShort(),
                FramebufferUpdateMessage.readBytes(in, size - 8));
    }

    // constructor - reading message from input stream and directly draw cursor shape to graphics context
    public CursorMessage(int encodingType, int timestamp, int xhot, int yhot, int width, int height,
            GraphicsContext graphicsContext, DataInputStream is) throws IOException {
        this(timestamp, encodingType, xhot, yhot, width, height, handleCursorShapeUpdate(graphicsContext, is,
                encodingType, xhot, yhot, width, height));
    }

    // TODO: maybe override Object.clone() and implement clonable instead
    public CursorMessage copy() {
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return new CursorMessage(timestamp, encoding, x, y, width, height, copy);
    }

    public int getEncoding() {
        return encoding;
    }

    // draw cursor shape to graphics context
    public void paint(GraphicsContext graphicsContext) {
        try {
            // // buffer for late comers (needed for recorder and live stream)
            // // TODO: only set if needed
            // NOTE: will only be called during playback, not during rfb protocol processing
            // graphicsContext.setCurrentCursorMessage(this.copy());

            byteArrayInputStream.reset();
            handleCursorShapeUpdate(graphicsContext, is, null, getEncoding(), x, y, width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // handle and buffer cursor
    static private byte[] handleCursorShapeUpdate(GraphicsContext graphicsContext, DataInputStream is,
            int encodingType, int xhot, int yhot, int width, int height) throws IOException {
        // create buffer
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream buffer = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
        // read and handle cursor
        handleCursorShapeUpdate(graphicsContext, is, buffer, encodingType, xhot, yhot, width, height);
        // write buffer
        buffer.flush();
        return byteArrayOutputStream.toByteArray();
    }

    //
    // Handle cursor shape update (XCursor and RichCursor encodings).
    //
    static private void handleCursorShapeUpdate(GraphicsContext graphicsContext, DataInputStream is,
            DataOutputStream buffer, int encodingType, int xhot, int yhot, int width, int height) throws IOException {

        int bytesPerRow = (width + 7) / 8;
        int bytesMaskData = bytesPerRow * height;

        graphicsContext.softCursorFree();

        if (width * height == 0)
            return;

        // Decode cursor pixel data.
        graphicsContext.softCursorPixels = new int[width * height];

        if (encodingType == Constants.EncodingXCursor || encodingType == Constants.EncodingTTTXCursor) {
            // Read foreground and background colors of the cursor.
            byte[] rgb = new byte[6];
            is.readFully(rgb);

            int[] colors = { (0xFF000000 | (rgb[3] & 0xFF) << 16 | (rgb[4] & 0xFF) << 8 | (rgb[5] & 0xFF)),
                    (0xFF000000 | (rgb[0] & 0xFF) << 16 | (rgb[1] & 0xFF) << 8 | (rgb[2] & 0xFF)) };

            // Read pixel and mask data.
            byte[] pixBuf = new byte[bytesMaskData];
            is.readFully(pixBuf);
            byte[] maskBuf = new byte[bytesMaskData];
            is.readFully(maskBuf);

            if (buffer != null) { // buffering
                buffer.write(rgb);
                buffer.write(pixBuf);
                buffer.write(maskBuf);
            }

            // Decode pixel data into softCursorPixels[].
            byte pixByte, maskByte;
            int x, y, n, result;
            int i = 0;
            for (y = 0; y < height; y++) {
                for (x = 0; x < width / 8; x++) {
                    pixByte = pixBuf[y * bytesPerRow + x];
                    maskByte = maskBuf[y * bytesPerRow + x];
                    for (n = 7; n >= 0; n--) {
                        if ((maskByte >> n & 1) != 0) {
                            result = colors[pixByte >> n & 1];
                        } else {
                            result = 0; // Transparent pixel
                        }
                        graphicsContext.softCursorPixels[i++] = result;
                    }
                }
                for (n = 7; n >= 8 - width % 8; n--) {
                    if ((maskBuf[y * bytesPerRow + x] >> n & 1) != 0) {
                        result = colors[pixBuf[y * bytesPerRow + x] >> n & 1];
                    } else {
                        result = 0; // Transparent pixel
                    }
                    graphicsContext.softCursorPixels[i++] = result;
                }
            }

        } else if (encodingType == Constants.EncodingRichCursor || encodingType == Constants.EncodingTTTRichCursor) {

            // Read pixel and mask data.
            byte[] pixBuf = new byte[width * height * graphicsContext.prefs.bytesPerPixel];
            is.readFully(pixBuf);
            byte[] maskBuf = new byte[bytesMaskData];
            is.readFully(maskBuf);

            if (buffer != null) { // buffering
                buffer.write(pixBuf);
                buffer.write(maskBuf);
            }

            // Decode pixel data into softCursorPixels[].
            byte maskByte;
            int x, y, n, result;
            int i = 0;
            for (y = 0; y < height; y++) {
                for (x = 0; x < width / 8; x++) {
                    maskByte = maskBuf[y * bytesPerRow + x];
                    for (n = 7; n >= 0; n--) {
                        if ((maskByte >> n & 1) != 0) {
                            switch (graphicsContext.prefs.bytesPerPixel) {
                            case 1:
                                result = graphicsContext.colorModel.getRGB(pixBuf[i]);
                                break;
                            case 2:
                                result = graphicsContext.colors[(pixBuf[i * 2 + 1] & 0xFF)
                                        | (pixBuf[i * 2] & 0xFF) << 8].getRGB();
                                break;
                            default:
                                result = 0xFF000000 | (pixBuf[i * 4 + 0] & 0xFF) << 16
                                        | (pixBuf[i * 4 + 1] & 0xFF) << 8 | (pixBuf[i * 4 + 2] & 0xFF);
                            }
                        } else {
                            result = 0; // Transparent pixel
                        }
                        graphicsContext.softCursorPixels[i++] = result;
                    }
                }
                for (n = 7; n >= 8 - width % 8; n--) {
                    if ((maskBuf[y * bytesPerRow + x] >> n & 1) != 0) {
                        switch (graphicsContext.prefs.bytesPerPixel) {
                        case 1:
                            result = graphicsContext.colorModel.getRGB(pixBuf[i]);
                            break;
                        case 2:
                            result = graphicsContext.colors[(pixBuf[i * 2 + 1] & 0xFF) | (pixBuf[i * 2] & 0xFF) << 8]
                                    .getRGB();
                            break;
                        default:
                            result = 0xFF000000 | (pixBuf[i * 4 + 0] & 0xFF) << 16 | (pixBuf[i * 4 + 1] & 0xFF) << 8
                                    | (pixBuf[i * 4 + 2] & 0xFF);
                        }
                    } else {
                        result = 0; // Transparent pixel
                    }
                    graphicsContext.softCursorPixels[i++] = result;
                }
            }
        }

        // Draw the cursor on an off-screen image.
        graphicsContext.softCursorSource = new MemoryImageSource(width, height, graphicsContext.softCursorPixels, 0,
                width);
        graphicsContext.softCursor = TTT.getInstance().createImage(graphicsContext.softCursorSource);

        // Set remaining data associated with cursor.
        graphicsContext.cursorWidth = width;
        graphicsContext.cursorHeight = height;
        graphicsContext.hotX = xhot;
        graphicsContext.hotY = yhot;

        graphicsContext.showSoftCursor(true);
    }

    /*******************************************************************************************************************
     * write message
     ******************************************************************************************************************/

    // write message to TTT output stream
    public void write(DataOutputStream out, int writeTimestamp) throws IOException {
        // header
        writeHeader(out, writeTimestamp);
        out.writeShort(x);
        out.writeShort(y);
        out.writeShort(width);
        out.writeShort(height);

        // encoded data
        out.write(data);
    };

    // return size of message in bytes (if written to stream)
    public int getSize() {
        return (data != null ? data.length : 0) + 9;
    }

    /*******************************************************************************************************************
     * TTT 2 Flash
     ******************************************************************************************************************/

    public static com.anotherbigidea.flash.movie.Shape shapeCursor;
    public static com.anotherbigidea.flash.movie.Instance cursorInstance;

    public void writeToFlash(FlashContext flashContext) throws IOException {

        flashContext.symbolCount++;

        flashContext.checkNextFrame(this.getTimestamp());
        flashContext.recording.graphicsContext.enableRefresh(false);

        // rset input stream and handle message data
        byteArrayInputStream.reset();
        handleCursorShapeUpdate(flashContext.recording.graphicsContext, is, null, getEncoding(), x, y, width, height);

        // access to cursor image
        Image softCursor = flashContext.recording.graphicsContext.softCursor;

        if (softCursor != null) {
            // create a Define Bit Lossless image for cursor
            com.anotherbigidea.flash.movie.Image.Lossless imCursor = ImageUtil.createLosslessImage(softCursor,
                    SWFConstants.BITMAP_FORMAT_8_BIT, true);
            // create a shape that uses the image as a fill
            // (images cannot be placed directly. They can only be used as shape fills)
            shapeCursor = ImageUtil.shapeForImage(imCursor, (double) softCursor.getWidth(null), (double) softCursor
                    .getHeight(null));
            // place cursor on the stage
            cursorInstance = flashContext.frame.placeSymbol(shapeCursor, x, y, FlashContext.cursorDepth);
        }
    }
    
    @Override
    public void writeToJson(Html5Context html5Context) throws IOException {
    	html5Context.recording.graphicsContext.enableRefresh(false);
        //reset input stream and handle message data
        byteArrayInputStream.reset();
        handleCursorShapeUpdate(html5Context.recording.graphicsContext, is, null, getEncoding(), x, y, width, height);

    	Image softCursor = html5Context.recording.graphicsContext.softCursor;
    	
    	this.writeToJsonBegin(html5Context);
    	html5Context.out.write(",");
    	html5Context.out.write("\"x\":"+this.x+",");
    	html5Context.out.write("\"y\":"+this.y+",");
    	html5Context.out.write("\"width\":"+this.width+",");
    	html5Context.out.write("\"height\":"+this.height+",");
    	html5Context.out.write("\"image\":\"data:image/"+Html5Context.thumbnailImageFormat+";base64,");
    	

    	if (softCursor != null) {
			html5Context.out.write(Base64Coder.encode(Html5Context.imageToByte(softCursor)));
    	}
    	
    	html5Context.out.write("\"");
    	this.writeToJsonEnd(html5Context);
    }
}
