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
 * Created on 31.01.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.messages;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class SimpleAnnotation extends Annotation {
    int color;

    // coordinates
    int startx;
    int starty;
    int endx;
    int endy;

    Shape shape, thumbshape;

    SimpleAnnotation(int timestamp, int color, int startx, int starty, int endx, int endy) {
        this.timestamp = timestamp;
        this.color = color;
        this.startx = startx;
        this.starty = starty;
        this.endx = endx;
        this.endy = endy;

        computeShape();
    }
    
    // MODMSG
    /**
     * constructor for use in parsing messaging xml
     */
    SimpleAnnotation(org.w3c.dom.Element xmlNode) {
    	this.color = Integer.parseInt(xmlNode.getAttribute("color"));
    	this.startx = Integer.parseInt(xmlNode.getAttribute("startx"));
    	this.starty = Integer.parseInt(xmlNode.getAttribute("starty"));
    	this.endx = Integer.parseInt(xmlNode.getAttribute("endx"));
    	this.endy = Integer.parseInt(xmlNode.getAttribute("endy"));
    	computeShape();
    }

    abstract void computeShape();

    public Rectangle getBounds() {
        return shape != null ? shape.getBounds() : null;
    }

    public boolean contains(int x, int y) {
        return shape.contains(x, y);
    }

    /*******************************************************************************************************************
     * getter/setter needed for painting
     ******************************************************************************************************************/

    public void setEndPoint(int x, int y) {
        endx = x;
        endy = y;
        computeShape();
    }

    public Point getStartPoint() {
        return new Point(startx, starty);
    }

    /*******************************************************************************************************************
     * write message
     ******************************************************************************************************************/

    // write message to TTT output stream
    public void write(DataOutputStream out, int writeTimestamp) throws IOException {
        writeHeader(out, writeTimestamp);
        out.writeByte(color);
        out.writeShort(startx);
        out.writeShort(starty);
        out.writeShort(endx);
        out.writeShort(endy);
    };

    // return size of message in bytes (if written to stream)
    public int getSize() {
        return 10;
    }
    
    // MODMSG
    @Override
    public String getXMLString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("<").append(getClass().getSimpleName());
    	sb.append(" color=\"").append(color).append("\" ");
    	sb.append(" startx=\"").append(startx).append("\" ");
    	sb.append(" starty=\"").append(starty).append("\" ");
    	sb.append(" endx=\"").append(endx).append("\" ");
    	sb.append(" endy=\"").append(endy).append("\" ");
    	sb.append(" />\n");
    	return sb.toString();
    }

}
