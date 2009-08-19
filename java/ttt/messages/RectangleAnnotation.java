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
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.IOException;

import ttt.Constants;
import ttt.FlashContext;

import com.anotherbigidea.flash.movie.Instance;
import com.anotherbigidea.flash.structs.AlphaColor;

public class RectangleAnnotation extends SimpleAnnotation {

    public RectangleAnnotation(int timestamp, int color, int startx, int starty, int endx, int endy) {
        super(timestamp, color, startx, starty, endx, endy);
    }

    public RectangleAnnotation(int timestamp, DataInputStream in) throws IOException {
        this(timestamp, in.readUnsignedByte(), in.readUnsignedShort(), in.readUnsignedShort(), in.readUnsignedShort(),
                in.readUnsignedShort());
    }
    
    // MOD TD
    /**
     * creates RectangleAnnotation from corresponding XML Element.
     * (used by messaging)
     */
    public RectangleAnnotation(org.w3c.dom.Element xmlNode) {
    	super(xmlNode);
    }


    public int getEncoding() {
        return Constants.AnnotationRectangle;
    }

    void computeShape() {
        // calculate x,y,width,height from startx/y and endx/y
        int x, y, width, height;

        // x,y koordinates are not ordered
        if (startx < endx) {
            if (starty < endy) {
                x = startx;
                y = starty;
                width = endx - startx;
                height = endy - starty;
            } else {
                x = startx;
                y = endy;
                width = endx - startx;
                height = starty - endy;
            }
        } else {
            if (starty < endy) {
                x = endx;
                y = starty;
                width = startx - endx;
                height = endy - starty;
            } else {
                x = endx;
                y = endy;
                width = startx - endx;
                height = starty - endy;
            }
        }

        // create shape
        shape = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(new Rectangle(
                x, y, width, height));
        thumbshape = new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(new Rectangle(x, y, width, height));
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
        Rectangle bounds = getBounds();
        return super.toString()
                + (bounds != null ? "  [" + bounds.width + " x " + bounds.height + " at (" + bounds.x + "," + bounds.y
                        + ")]" : "");
    }

    /*******************************************************************************************************************
     * TTT 2 Flash
     ******************************************************************************************************************/

    public void writeToFlash(FlashContext flashContext) throws IOException {
        flashContext.symbolCount++;
        flashContext.checkNextFrame(this.getTimestamp());
        Color borderCol = annotationColors[color];
        com.anotherbigidea.flash.movie.Shape shape = new com.anotherbigidea.flash.movie.Shape();
        AlphaColor border_col = new AlphaColor(borderCol.getRed(), borderCol.getGreen(), borderCol.getBlue(), borderCol
                .getAlpha());
        Rectangle rect = getBounds();

        shape.defineLineStyle(2.5f, border_col);
        shape.setLineStyle(1);
        shape.setRightFillStyle(1);

        shape.drawAWTPathIterator(rect.getPathIterator(null));

        Instance instanceShape = flashContext.frame.placeSymbol(shape, 0, 0, flashContext.annotationsDepth++);

        Annotation annot = (Annotation) flashContext.message;
        // buffer annotation's message and instance
        flashContext.addAnnotations(annot, instanceShape);
    }
}
