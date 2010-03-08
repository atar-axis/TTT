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
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

/**
 * A complex <code>ColoredShape</code> that displays a colored rectangle with a border.
 *
 * @author TeleTeachingTool
 */
public class HighlightShape extends ColoredShape {
	
	private Shape border;
	private Color borderC;
	private int borderW;

	/**
	 * Constructor.
	 * <p>
	 * Generates a rectangular, colored shape with a border,using the given points as upper left and lower right corner
	 * (which is which depends on the actual coordinates). 
	 * 
	 * @param x1 X coordinate of first corner.
	 * @param y1 Y coordinate of first corner.
	 * @param x2 X coordinate of second corner.
	 * @param y2 Y coordinate of second corner.
	 * @param coreColor Color of the rectangle.
	 * @param borderColor Color of the border.
	 * @param borderWidth Width of the border.
	 */
	public HighlightShape(int x1, int y1, int x2, int y2, Color coreColor, Color borderColor, int borderWidth) {
		super(null, coreColor);
		
		borderC = borderColor;
		borderW = borderWidth;
		
		enclosedShape = getRectangle(x1, y1, x2, y2, 0);
		Shape core = getRectangle(x1, y1, x2, y2, borderW);
		Area b = new Area(enclosedShape);
		b.subtract(new Area(core));
		border = b;
	}

	public void paintShape(Graphics g, Stroke pen) {
		Graphics2D g2 = (Graphics2D) g;
		
		if( pen != null) g2.setStroke(pen);
			
		g2.setPaint(borderC);
		g2.fill(border);
			
		g2.setPaint(shapeColor);
		g2.fill(enclosedShape);
	}
	
	private Shape getRectangle(int startx, int starty, int endx, int endy, int smaller) {
		if (startx < endx) {
			if (starty < endy)
				return new Rectangle2D.Double(startx+smaller, starty+smaller, endx-startx-2*smaller, endy - starty-2*smaller);
			else
				return new Rectangle2D.Double(startx+smaller, endy+smaller, endx - startx-2*smaller, starty - endy-2*smaller);
		} else {
			if (starty < endy)
				return new Rectangle2D.Double(endx+smaller, starty+smaller, startx - endx-2*smaller, endy - starty-2*smaller);
			else
				return new Rectangle2D.Double(endx+smaller, endy+smaller, startx - endx-2*smaller, starty - endy-2*smaller);
		}
	}
	
	/**
	 * @return The border's color.
	 */
	public Color getBorderColor() {
		return borderC;
	}

	/**
	 * @param color The new border color.
	 */
	public void setBorderColor(Color color) {
		borderC = color;
	}

}
