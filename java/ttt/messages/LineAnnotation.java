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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.io.DataInputStream;
import java.io.IOException;

import ttt.Constants;
import ttt.FlashContext;

import com.anotherbigidea.flash.movie.Instance;
import com.anotherbigidea.flash.structs.AlphaColor;

public class LineAnnotation extends SimpleAnnotation {

    public LineAnnotation(int timestamp, int color, int startx, int starty, int endx, int endy) {
        super(timestamp, color, startx, starty, endx, endy);
    }

    public LineAnnotation(int timestamp, DataInputStream in) throws IOException {
        this(timestamp, in.readUnsignedByte(), in.readUnsignedShort(), in.readUnsignedShort(), in.readUnsignedShort(),
                in.readUnsignedShort());
    }

    // MODMSG
    /**
     * create LineAnnotation of corresponding XML-Element (used by messaging)
     */
    public LineAnnotation(org.w3c.dom.Element xmlNode) {
    	super(xmlNode);
    }

    public int getEncoding() {
        return Constants.AnnotationLine;
    }

    void computeShape() {
        shape = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(new Line2D.Double(startx, starty, endx, endy));
        thumbshape = new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(new Line2D.Double(startx, starty, endx, endy));
    }

    public void paintToThumbnail(Graphics2D graphics) {
        // TODO: fix color - don't use table for new protocol
        graphics.setColor(annotationColors[color]);
        graphics.fill(thumbshape);
    }

    public void paint(Graphics2D graphics) {
        // TODO: fix color - don't use table for new protocol
        graphics.setColor(annotationColors[color]);
        graphics.fill(shape);
    }

    public String toString() {
        // TODO Auto-generated method stub
        return super.toString() + " [(" + startx + "," + starty + ") to (" + endx + "," + endy + ")]";
    }

    /*******************************************************************************************************************
     * TTT 2 Flash
     ******************************************************************************************************************/

    public void writeToFlash(FlashContext flashContext) throws IOException {
        flashContext.symbolCount++;
        flashContext.checkNextFrame(this.getTimestamp());
        com.anotherbigidea.flash.movie.Shape line_shape = new com.anotherbigidea.flash.movie.Shape();
        Color lineColor = annotationColors[color];
        line_shape.defineLineStyle(2.5f, new AlphaColor(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(),
                lineColor.getAlpha()));
        line_shape.setLineStyle(1);
        line_shape.move(startx, starty);
        line_shape.line(endx, endy);
        Instance instanceLine = flashContext.frame.placeSymbol(line_shape, 0, 0, flashContext.annotationsDepth++);

        Annotation annot = (Annotation) flashContext.message;
        // buffer annotation's message and instance
        flashContext.addAnnotations(annot, instanceLine);
    }
}
