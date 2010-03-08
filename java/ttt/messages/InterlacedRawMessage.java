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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;

import ttt.Constants;
import ttt.gui.GraphicsContext;

public class InterlacedRawMessage extends FramebufferUpdateMessage {

    private byte[] data;
    private int layer;

    // constructor
    public InterlacedRawMessage(int timestamp, int x, int y, int width, int height, int layer, byte[] data) {
        super(timestamp, x, y, width, height);
        this.data = data;
        this.layer = layer;
    }

    public int getEncoding() {
        return Constants.EncodingInterlacedRaw;
    }

    // creates interlaced messages from given rectangle
    static public Message[] createInterlacedMessages(GraphicsContext graphicsContext, int timestamp, int rx, int ry,
            int rw, int rh) {

        // TODO: support other color depths
        if (graphicsContext.prefs.bytesPerPixel != 4) {
            System.out.println("Skipping: Only 4 Byte Interlaced Encoding supported");
            return null;
        }

        // TODO: switch from interlaced lines to interlaced dots -> 4 instead of 2 messages
        int[] pixel = graphicsContext.getPixelsFromRawImage(rx, ry, rw, rh);

        int numberOfLayers = 4;
        if (rw == 1 && rh == 1)
            numberOfLayers = 1;
        else if (rw == 1 || rh == 1)
            numberOfLayers = 2;

        Message[] messages = new Message[numberOfLayers];
        for (int i = 0; i < numberOfLayers; i++) {
            messages[i] = new InterlacedRawMessage(timestamp, rx, ry, rw, rh, i + 1,
                    extractPixels(i + 1, pixel, rw, rh));
            // System.out.println((i + 1) + ". " + ((InterlacedRawMessage) messages[i]).getSize());
        }
        return messages;
    }

    static private int count;
    static private double ratio;

    // extracts interlaced pixels; every fourth pixel
    private static byte[] extractPixels(int layer, int[] pixel, int rw, int rh) {
        // init
        byte[] bytePixel;
        int xStart, yStart;
        switch (layer) {
        case 1:
            bytePixel = new byte[((rh + 1) / 2) * ((rw + 1) / 2) * 4];
            xStart = 0;
            yStart = 0;
            break;
        case 2:
            bytePixel = new byte[((rh + 1) / 2) * (rw / 2) * 4];
            xStart = 1;
            yStart = 0;
            break;
        case 3:
            bytePixel = new byte[(rh / 2) * ((rw + 1) / 2) * 4];
            xStart = 0;
            yStart = 1;
            break;
        case 4:
        default:
            bytePixel = new byte[(rh / 2) * (rw / 2) * 4];
            xStart = 1;
            yStart = 1;
            break;
        }

        // extract pixels
        int offset = 0;
        for (int y = yStart; y < rh; y += 2) {
            for (int x = xStart; x < rw; x += 2) {
                int value = pixel[y * rw + x];
                bytePixel[offset++] = (byte) ((value) & 0xff);
                bytePixel[offset++] = (byte) ((value >> 8) & 0xff);
                bytePixel[offset++] = (byte) ((value >> 16) & 0xff);
                bytePixel[offset++] = (byte) ((value >> 24) & 0xff);
            }
        }

        if (!true) {
            // compute ratio
            try {
                if (bytePixel.length > 100) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    OutputStream deflaterOutputStream = new BufferedOutputStream(new DeflaterOutputStream(
                            byteArrayOutputStream));
                    deflaterOutputStream.write(bytePixel);
                    deflaterOutputStream.flush();
                    deflaterOutputStream.close();
                    byteArrayOutputStream.flush();
                    int length = byteArrayOutputStream.toByteArray().length;

                    double r = 100.0 * length / bytePixel.length;
                    ratio = (ratio * count++ + r) / count;
                    if (bytePixel.length > 1000)
                        System.out.println(layer + ".\t" + bytePixel.length + "\t-> " + length + "\tratio " + r
                                + "%\tovarall ratio " + ratio + "%");
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return bytePixel;
    }

    // draw rectangle to graphics context
    public void paint(GraphicsContext graphicsContext) {
        handleInterlacedRawRect(graphicsContext, x, y, width, height);
    }

    // draw image to graphics context
    private void handleInterlacedRawRect(GraphicsContext graphicsContext, int x, int y, int w, int h) {
        int xStart;
        int yStart;

        switch (graphicsContext.prefs.bytesPerPixel) {
        case 4:
            switch (layer) {
            case 1:
                xStart = 0;
                yStart = 0;
                break;
            case 2:
                xStart = 1;
                yStart = 0;
                break;
            case 3:
                xStart = 0;
                yStart = 1;
                break;
            case 4:
            default:
                xStart = 1;
                yStart = 1;
                break;
            }

            int bufferOffset = 0;
            for (int dy = y + yStart; dy < y + h; dy += 2) {
                int pixelOffset = dy * graphicsContext.prefs.framebufferWidth;
                for (int dx = x + xStart; dx < x + w; dx += 2) {
                    int value = (data[bufferOffset + 2] & 0xFF) << 16 | (data[bufferOffset + 1] & 0xFF) << 8
                            | (data[bufferOffset] & 0xFF);
                    graphicsContext.pixels[pixelOffset + dx] = value;
                    bufferOffset += 4;

                    // quadruple first and double second layer
                    if (true)
                        switch (layer) {
                        case 1:
                            if (dx + 1 < x + w) // if not last column
                            {
                                graphicsContext.pixels[pixelOffset + dx + 1] = value;
                                if (dy + 1 < y + h) // if not last line
                                    graphicsContext.pixels[pixelOffset + dx + 1
                                            + graphicsContext.prefs.framebufferWidth] = value;
                            }
                        case 2:
                            if (dy + 1 < y + h) // if not last line
                                graphicsContext.pixels[pixelOffset + dx + graphicsContext.prefs.framebufferWidth] = value;
                            break;
                        }
                }
            }

            break;
        default:
            // TODO: support other color depths
            System.out.println("Skipping: Only 4 Byte Interlaced Encoding supported");
            break;
        }

        graphicsContext.handleUpdatedPixels(x, y, w, h);
        graphicsContext.refresh(x, y, w, h);
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
