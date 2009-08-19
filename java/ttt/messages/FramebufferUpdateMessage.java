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

import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ttt.Constants;

public abstract class FramebufferUpdateMessage extends Message {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected byte[] data;

    public FramebufferUpdateMessage(int timestamp, int x, int y, int width, int height) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    abstract public int getEncoding();

    // TODO: not that importent - can be reduced to getBounds()
    public int getCoveredArea() {
        return width * height;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public String toString() {
        return super.toString() + "\tEncoding: " + Constants.encodingToString(getEncoding()) + " - " + width + " x "
                + height + " at (" + x + "," + y + ")";
    }

    // helper method to read byte array of given size
    static byte[] readBytes(DataInputStream in, int size) throws IOException {
        byte[] bytes = new byte[size];
        in.readFully(bytes);
        return bytes;
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
