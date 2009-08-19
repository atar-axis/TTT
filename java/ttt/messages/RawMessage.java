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
 * Created on 12.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.messages;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ttt.Constants;
import ttt.GraphicsContext;

public class RawMessage extends FramebufferUpdateMessage {
    // TODO: this is untested

    private ByteArrayInputStream byteArrayInputStream;
    private DataInputStream is;

    // constructor
    public RawMessage(int timestamp, int x, int y, int width, int height, byte[] data) {
        super(timestamp, x, y, width, height);
        this.data = data;

        // create streams to read data
        // NOTE: don't use buffered streams, because this will lead to heavy memory usage;
        byteArrayInputStream = new ByteArrayInputStream(this.data);
        is = new DataInputStream(byteArrayInputStream);
    }

    // read from TTT input stream
    public RawMessage(int timestamp, DataInputStream in, int size) throws IOException {
        this(timestamp, in.readShort(), in.readShort(), in.readShort(), in.readShort(), FramebufferUpdateMessage
                .readBytes(in, size - 8));
    }

    // constructor - reading message from input stream and directly draw rectangle to graphics context
    public RawMessage(int timestamp, int x, int y, int width, int height, GraphicsContext graphicsContext,
            DataInputStream is) throws IOException {
        this(timestamp, x, y, width, height, handleAndBufferRawRect(graphicsContext, is, x, y, width, height));
    }

    public int getEncoding() {
        return Constants.EncodingRaw;
    }

    // draw rectangle to graphics context
    public void paint(GraphicsContext graphicsContext) {
        try {
            byteArrayInputStream.reset();
            handleRawRect(graphicsContext, is, null, x, y, width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // handleRawRect(graphicsContext, x, y, width, height);
    }

    // read data from input stream and draw rectangle to graphics context
    static private byte[] handleAndBufferRawRect(GraphicsContext graphicsContext, DataInputStream is, int x, int y,
            int w, int h) throws IOException {

        // create buffer
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream buffer = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        // handle
        handleRawRect(graphicsContext, is, buffer, x, y, w, h);

        // write buffer
        buffer.flush();
        return byteArrayOutputStream.toByteArray();
    }

    // handle raw encoded sub-rectangle
    static private void handleRawRect(GraphicsContext graphicsContext, DataInputStream is, DataOutputStream os, int x,
            int y, int w, int h) throws IOException {

        switch (graphicsContext.prefs.bytesPerPixel) {
        case 1:
            for (int dy = y; dy < y + h; dy++) {
                is.readFully(graphicsContext.pixels8, dy * graphicsContext.prefs.framebufferWidth + x, w);

                // buffering
                if (os != null)
                    os.write(graphicsContext.pixels8, dy * graphicsContext.prefs.framebufferWidth + x, w);
            }
            break;

        case 2:
            byte[] buf = new byte[w * 2];
            for (int dy = y; dy < y + h; dy++) {
                is.readFully(buf);

                // buffering
                if (os != null)
                    os.write(buf);

                int offset = dy * graphicsContext.prefs.framebufferWidth + x;
                if (graphicsContext.prefs.bigEndian)
                    for (int i = 0; i < w; i++)
                        graphicsContext.pixels[offset + i] = (buf[i * 2] & 0xFF) << 8 | (buf[i * 2 + 1] & 0xFF);
                else
                    for (int i = 0; i < w; i++)
                        graphicsContext.pixels[offset + i] = (buf[i * 2 + 1] & 0xFF) << 8 | (buf[i * 2] & 0xFF);

            }
            break;

        default:
            buf = new byte[w * 4];
            for (int dy = y; dy < y + h; dy++) {
                is.readFully(buf);

                // buffering
                if (os != null)
                    os.write(buf);

                int offset = dy * graphicsContext.prefs.framebufferWidth + x;
                if (graphicsContext.prefs.bigEndian)
                    for (int i = 0; i < w; i++)
                        graphicsContext.pixels[offset + i] = (buf[i * 4 + 1] & 0xFF) << 16
                                | (buf[i * 4 + 2] & 0xFF) << 8 | (buf[i * 4 + 3] & 0xFF);
                else
                    for (int i = 0; i < w; i++)
                        graphicsContext.pixels[offset + i] = (buf[i * 4 + 2] & 0xFF) << 16
                                | (buf[i * 4 + 1] & 0xFF) << 8 | (buf[i * 4] & 0xFF);
            }
        }

        if (!graphicsContext.paint_to_offscreen_image)
            // // NOTE: raw mode calls update for whole rectangle, not for subrectangles
            graphicsContext.handleUpdatedPixels(x, y, w, h);
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
}
