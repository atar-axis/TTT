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
 * Created on 13.03.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.messages;

import java.io.DataOutputStream;
import java.io.IOException;

import ttt.Constants;
import ttt.ProtocolPreferences;
import ttt.gui.GraphicsContext;
import ttt.postprocessing.flash.FlashContext;

// must extend FramebufferUpdate or index generation will not detect Whiteboard pages
public class WhiteboardMessage extends FramebufferUpdateMessage {

    // page number
    // > 0: whiteboard enabled
    // <=0: desktop enabled
    // NOTE: only one desktop is support by now
    int pageNumber;

    public WhiteboardMessage(int timestamp, int pageNumber, ProtocolPreferences prefs) {
        super(timestamp, 0, 0, prefs.framebufferWidth, prefs.framebufferHeight);
        this.pageNumber = pageNumber;
    }

    public int getEncoding() {
        return Constants.EncodingWhiteboard;
    }

    public void paint(GraphicsContext graphicsContext) {
        graphicsContext.setWhiteboardPage(pageNumber);
    }

    public boolean isWhiteboardEnabled() {
        return pageNumber > 0;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String toString() {
        return "Message: [" + Constants.getStringFromTime(timestamp, true) + "]" + "\tEncoding: "
                + Constants.encodingToString(getEncoding()) + " (page " + getPageNumber() + ")" + " - " + width + " x "
                + height + " at (" + x + "," + y + ")";
    }

    /*******************************************************************************************************************
     * write message
     ******************************************************************************************************************/

    // write message to TTT output stream
    public void write(DataOutputStream out, int writeTimestamp) throws IOException {
        writeHeader(out, writeTimestamp);
        out.writeByte(pageNumber);
    };

    // return size of message in bytes (if written to stream)
    public int getSize() {
        return 2;
    }

    /*******************************************************************************************************************
     * TTT 2 Flash
     ******************************************************************************************************************/

    static final int whiteboardDepth = 29999;
    public static com.anotherbigidea.flash.movie.Instance whiteBoardInstance;

    public void writeToFlash(FlashContext flashContext) throws IOException {

        flashContext.symbolCount++;
        flashContext.checkNextFrame(this.getTimestamp());
        if (pageNumber > 0) {
            flashContext.clearAnnotations(false, flashContext.frame);
            com.anotherbigidea.flash.movie.Shape shape = FlashContext.insertRect(flashContext.recording
                    .getProtocolPreferences().framebufferWidth,
                    flashContext.recording.getProtocolPreferences().framebufferHeight,
                    new com.anotherbigidea.flash.structs.Color(255, 255, 255),
                    new com.anotherbigidea.flash.structs.Color(255, 255, 255));
            whiteBoardInstance = flashContext.frame.placeSymbol(shape, 0, 0, whiteboardDepth);
        } else {
            if (whiteBoardInstance != null) {
                flashContext.frame.remove(whiteBoardInstance);
            }
        }
    }
}
