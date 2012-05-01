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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ttt.Constants;
import ttt.gui.GraphicsContext;
import ttt.postprocessing.flash.FlashContext;
import ttt.postprocessing.html5.Html5Context;

public class DeleteAnnotation extends Annotation {

    private int x, y;

    public DeleteAnnotation(int timestamp, int x, int y) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
    }

    public DeleteAnnotation(int timestamp, DataInputStream in) throws IOException {
        this(timestamp, in.readUnsignedShort(), in.readUnsignedShort());
    }

    public void paint(GraphicsContext graphicsContext) {
        graphicsContext.removeAnnotationsAt(x, y);
        graphicsContext.refresh();
    }

    public void paint(Graphics2D graphics) {
    // should never be called for Delete
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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getEncoding() {
        return Constants.AnnotationDelete;
    }

    public void write(OutputStream outputStream) throws IOException {}

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

    public void writeToFlash(FlashContext flashContext) throws IOException {
        flashContext.checkNextFrame(this.getTimestamp());
        // find and remove annotations at given coordinates
        flashContext.removeAnnotationsAt(x, y);
    }
    
    @Override
    public void writeToJson(Html5Context html5Context) throws IOException {
    	this.writeToJsonBegin(html5Context);
    	html5Context.out.write(",");
    	html5Context.out.write("\"x\":"+this.x+",");
    	html5Context.out.write("\"y\":"+this.y);
    	this.writeToJsonEnd(html5Context);
    }
    
    // MODMSG
    @Override
    public String getXMLString() {
    	// should never be called b/c messaging does not send delete annotations
    	return "";
    }
}
