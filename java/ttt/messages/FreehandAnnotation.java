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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import ttt.Constants;
import ttt.FlashContext;

import com.anotherbigidea.flash.movie.Instance;
import com.anotherbigidea.flash.structs.AlphaColor;

public class FreehandAnnotation extends Annotation {

    private int color;
    private GeneralPath path = new GeneralPath();
    private Shape strokedShape;
    private int count;

    // private double scale;

    // empty freehand annotation
    public FreehandAnnotation(int timestamp, int color) {
        this.timestamp = timestamp;
        this.color = color;
    }

    public FreehandAnnotation(int timestamp, DataInputStream in) throws IOException {
        this(timestamp, in.readUnsignedByte());

        // read points
        int number = in.readUnsignedShort();
        for (int i = 0; i < number; i++)
            addPoint(in.readUnsignedShort(), in.readUnsignedShort());
    }

    // MODMSG
    /**
     * constructor for use in parsing messaging xml
     */
    public FreehandAnnotation(org.w3c.dom.Element xmlNode) {
    	this.color = Integer.parseInt(xmlNode.getAttribute("color"));
    	
    	// parse point paths
    	org.w3c.dom.Element elPath = (org.w3c.dom.Element)xmlNode.getElementsByTagName("path").item(0);
    	String[] points = elPath.getAttribute("data").split("\\s");
    	path.moveTo(Float.parseFloat(points[0]), Float.parseFloat(points[1]));
    	for(int i = 2; i < points.length; i+=2) {
    		path.lineTo(Float.parseFloat(points[i]), Float.parseFloat(points[i+1]));
    	}
    	this.count = points.length / 2;
        strokedShape = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(path);
    }

    // add point to freehand annotation
    public void addPoint(int x, int y) {
        if (count++ == 0)
            path.moveTo(x, y);
        else
            path.lineTo(x, y);

        // update shape and bounds
        strokedShape = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(path);
    }

    @Override
    public Rectangle getBounds() {
        return strokedShape != null ? strokedShape.getBounds() : null;
    }

    public void paint(Graphics2D graphics) {
        // TODO: fix color - don't use table for new protocol
        graphics.setColor(annotationColors[color]);

        // painting full sized
        graphics.fill(strokedShape);
    }

    public void paintToThumbnail(Graphics2D graphics) {
        // TODO: fix color - don't use table for new protocol
        graphics.setColor(annotationColors[color]);

        // TODO: that's a nasty but working hack for scaling
        Stroke stroke = graphics.getStroke();
        // painting to thumbnails
        graphics.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(path);
        graphics.setStroke(stroke);
    }

    public boolean contains(int x, int y) {
        return strokedShape.contains(x, y);
    }

    public int getEncoding() {
        return Constants.AnnotationFreehand;
    }

    public String toString() {
        return super.toString() + " [" + count + " points]";
    }

    /*******************************************************************************************************************
     * write message
     ******************************************************************************************************************/

    // write message to TTT output stream
    public void write(DataOutputStream out, int writeTimestamp) throws IOException {
        writeHeader(out, writeTimestamp);
        out.writeByte(color);
        out.writeShort(count);

        PathIterator iterator = path.getPathIterator(null);
        double[] point = new double[6];
        for (int i = 0; i < count; i++) {
            iterator.currentSegment(point);
            out.writeShort((int) point[0]);
            out.writeShort((int) point[1]);
            iterator.next();
        }
    }

    // return size of message in bytes (if written to stream)
    public int getSize() {
        return 4 + 4 * count;
    }

    /*******************************************************************************************************************
     * TTT 2 Flash
     ******************************************************************************************************************/

    public com.anotherbigidea.flash.movie.Frame frame = null;

    public void writeToFlash(FlashContext flashContext) throws IOException {
        // converting Path to ArrayList for points
        // TODO: use path directly
        ArrayList<Point> pointList = new ArrayList<Point>();
        PathIterator iterator = path.getPathIterator(null);
        double[] pointOfPath = new double[6];
        for (int i = 0; i < count; i++) {
            iterator.currentSegment(pointOfPath);
            pointList.add(new Point((int) pointOfPath[0], (int) pointOfPath[1]));
            iterator.next();
        }

        flashContext.symbolCount++;
        flashContext.checkNextFrame(this.getTimestamp());

        com.anotherbigidea.flash.movie.Shape line_shape = new com.anotherbigidea.flash.movie.Shape();
        Point[] point = new Point[pointList.size()];

        Color freehandColor = annotationColors[color];

        if (pointList.size() > 0) {

            point[0] = (Point) pointList.get(0);

            for (int count = 1; count < pointList.size(); count++) {
                line_shape.defineLineStyle(2.5f, new AlphaColor(freehandColor.getRed(), freehandColor.getGreen(),
                        freehandColor.getBlue(), freehandColor.getAlpha()));
                line_shape.setLineStyle(1);
                line_shape.move(point[0].x, point[0].y);

                point[count] = (Point) pointList.get(count);
                line_shape.line(point[count].x, point[count].y);
                // the startpoint of next step is the last point of the current step
                point[0] = point[count - 1];
            }
        }
        // place the drawed path into the frame
        Instance instanceFreeHand = flashContext.frame.placeSymbol(line_shape, 0, 0, flashContext.annotationsDepth++);

        Annotation annot = (Annotation) flashContext.message;
        // buffer annotation's message and instance
        flashContext.addAnnotations(annot, instanceFreeHand);
    }
    
    // MODMSG
    @Override
    public String getXMLString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("<FreehandAnnotation color=\"").append(color).append("\" >\n");
    	sb.append("  <path data=\"");
    	
        PathIterator iterator = path.getPathIterator(null);
        double[] point = new double[6];
        while(!iterator.isDone()) {
            iterator.currentSegment(point);
            sb.append((int)point[0]).append(" ").append((int)point[1]).append(" ");
            iterator.next();
        }
        sb.append("\" />\n</FreehandAnnotation>\n");

        return sb.toString();
    }

}
