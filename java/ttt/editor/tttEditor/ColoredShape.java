package ttt.editor.tttEditor;
/*
 * TeleTeachingTool - Platform-independent recording and transmission of 
 * arbitrary content
 * 
 * Copyright (C) 2003 by Peter Ziewer
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 *
 * A <code>Shape</code> that stores color information and knows how to paint itself.
 * <p>
 * To use a <code>ColoredShape</code> with one <code>Shape</code> use the class 
 * <code>SimpleColoredShape</code>.
 * <p>
 * More complex instances of <code>ColoredShape</code> (eg several shapes that
 * construct a larger image) should extends <code>ColoredShape</code>
 * and overwrite the <code>paintShape</code> method to bring it to the screen.
 * 
 * @see SimpleColoredShape
 * @author TeleTeachingTool
 */
public abstract class ColoredShape implements Shape {

	protected Shape enclosedShape;
	protected Color shapeColor;

	public ColoredShape(Shape s, Color c) {
		enclosedShape = s;
		shapeColor = c;
	}
	public String toString() {
		return enclosedShape.toString()+";"+shapeColor.toString();
	}

	public Color getColor() {
		return shapeColor;
	}


	public boolean contains(double x, double y) {
		return enclosedShape.contains(x, y);
	}
	public boolean contains(double x, double y, double w, double h) {
		return enclosedShape.contains(x, y, w, h);
	}
	public boolean contains(Point2D p) {
		return enclosedShape.contains(p);
	}
	public boolean contains(Rectangle2D r) {
		return enclosedShape.contains(r);
	}
	public Rectangle getBounds() {
		return enclosedShape.getBounds();
	}
	public Rectangle2D getBounds2D() {
		return enclosedShape.getBounds2D();
	}
	public PathIterator getPathIterator(AffineTransform at) {
		return enclosedShape.getPathIterator(at);
	}
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return enclosedShape.getPathIterator(at, flatness);
	}
	public boolean intersects(double x, double y, double w, double h) {
		return enclosedShape.intersects(x, y, w, h);
	}
	public boolean intersects(Rectangle2D r) {
		return enclosedShape.intersects(r);
	}
	
	public abstract void paintShape(Graphics g, Stroke pen);
}
