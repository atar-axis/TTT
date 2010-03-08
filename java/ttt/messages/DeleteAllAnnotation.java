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
 * Created on 13.01.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.messages;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.DataOutputStream;
import java.io.IOException;

import ttt.Constants;
import ttt.gui.GraphicsContext;
import ttt.postprocessing.flash.FlashContext;

public class DeleteAllAnnotation extends Annotation {

    public DeleteAllAnnotation(int timestamp) {
        this.timestamp = timestamp;
    }

    public void paint(GraphicsContext graphicsContext) {
        graphicsContext.clearAnnotations();
        graphicsContext.refresh();
    }

    public void paint(Graphics2D graphics) {
    // should never be called for DeleteAll
    }

    public void paint(Graphics2D graphics, double scale) {
        paint(graphics);
    }

    @Override
    public Rectangle getBounds() {
        return null;
    }

    public boolean contains(int x, int y) {
        return false;
    }

    public int getEncoding() {
        return Constants.AnnotationDeleteAll;
    }

    /*******************************************************************************************************************
     * write message
     ******************************************************************************************************************/

    // write message to TTT output stream
    public void write(DataOutputStream out, int writeTimestamp) throws IOException {
        writeHeader(out, writeTimestamp);
    };

    // return size of message in bytes (if written to stream)
    public int getSize() {
        return 1;
    }

    /*******************************************************************************************************************
     * TTT 2 Flash
     ******************************************************************************************************************/

    public void writeToFlash(FlashContext flashContext) throws IOException {
        flashContext.checkNextFrame(this.getTimestamp());
        // remove all annotations
        flashContext.clearAnnotations(false, flashContext.frame);
    }
    
    // MODMSG
    @Override
    public String getXMLString() {
    	// should never be called b/c messaging does not send delete annotations
    	return "";
    }

}
