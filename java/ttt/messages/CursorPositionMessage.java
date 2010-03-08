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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ttt.Constants;
import ttt.gui.GraphicsContext;
import ttt.postprocessing.flash.FlashContext;

public class CursorPositionMessage extends FramebufferUpdateMessage {
    // TODO: think about superclass

    public CursorPositionMessage(int timestamp, int x, int y) {
        super(timestamp, x, y, 0, 0);
    }

    public CursorPositionMessage(int timestamp, int x, int y, GraphicsContext graphicsContext) {
        this(timestamp, x, y);
        paint(graphicsContext);
    }

    public CursorPositionMessage(int timestamp, DataInputStream in) throws IOException {
        this(timestamp, in.readUnsignedShort(), in.readUnsignedShort());
    }

    public int getEncoding() {
        return Constants.EncodingTTTCursorPosition;
    }

    public void paint(GraphicsContext graphicsContext) {
        if (x >= graphicsContext.prefs.framebufferWidth)
            x = graphicsContext.prefs.framebufferWidth - 1;
        if (y >= graphicsContext.prefs.framebufferHeight)
            y = graphicsContext.prefs.framebufferHeight - 1;

        graphicsContext.softCursorMove(x, y);
    }

    /*******************************************************************************************************************
     * write message
     ******************************************************************************************************************/

    // write message to TTT output stream
    public void write(DataOutputStream out, int writeTimestamp) throws IOException {
        writeHeader(out, writeTimestamp);
        out.writeShort(x);
        out.writeShort(y);
    };

    // return size of message in bytes (if written to stream)
    public int getSize() {
        return 5;
    }

    /*******************************************************************************************************************
     * TTT 2 Flash
     ******************************************************************************************************************/

    static int lastCursorX, lastCursorY;
    static int lastFrameUsed = -1;
    static int cursorPositionCount = 0;
    static com.anotherbigidea.flash.movie.Frame lastCursorFrame;

    /*
     * Only the last position of the cursor in a given intervall must be place/alter on the satge
     */
    public void writeToFlash(FlashContext flashContext) throws IOException {

        cursorPositionCount++;

        flashContext.checkNextFrame(this.getTimestamp());

        if (cursorPositionCount == 1) {
            lastCursorFrame = flashContext.frame;
            return;
        }

        if (flashContext.frameNumb == lastFrameUsed)
            return;

        if (CursorMessage.cursorInstance != null && !flashContext.isCursor) {
            if (lastCursorFrame != null) {
                lastCursorFrame.alter(CursorMessage.cursorInstance, lastCursorX, lastCursorY);
            }
        } else if (CursorMessage.shapeCursor != null && flashContext.isCursor) {
            CursorMessage.cursorInstance = flashContext.frame.placeSymbol(CursorMessage.shapeCursor, x, y,
                    FlashContext.cursorDepth);
            flashContext.isCursor = false;
        }

        lastFrameUsed = flashContext.frameNumb;
        lastCursorX = x;
        lastCursorY = y;
        lastCursorFrame = flashContext.frame;
    }
}
