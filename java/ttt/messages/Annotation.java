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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import ttt.Constants;
import ttt.gui.GraphicsContext;

public abstract class Annotation extends Message {

    // return the outer bounds of the annotation
    abstract public Rectangle getBounds();

    // add annotation to graphicContext
    public void paint(GraphicsContext graphicsContext) {
        graphicsContext.addAnnotation(this);

        // update painting
        graphicsContext.refresh(getBounds());
    }

    // used for showing temporary annotations during painting
    public boolean temporary;

    // draw annotation to graphics
    abstract public void paint(Graphics2D graphics);

    // special thumbnail drawing if needed (to avoid ugly scaling)
    public void paintToThumbnail(Graphics2D graphics) {
        paint(graphics);
    }

    abstract public boolean contains(int x, int y);

    abstract public int getEncoding();

    public String toString() {
        return super.toString() + ": Annotation-" + Constants.encodingToString(getEncoding());
    }
    
    // MODMSG
    /**
     * get XML String for messaging
     */
    abstract public String getXMLString();

    /*******************************************************************************************************************
     * Default Color Table for annotations *
     ******************************************************************************************************************/

    public final static int White = 0;
    public final static int DarkGray = 4;
    public final static int Gray = 8;
    public final static int LightGray = 12;
    public final static int Black = 16;
    public final static int Orange = 20;
    public final static int Pink = 24;
    public final static int Blue = 28;
    public final static int Red = 32;
    public final static int Green = 36;
    public final static int Magenta = 40;
    public final static int Yellow = 44;
    public final static int Cyan = 48;

    static public Color[] annotationColors = {
            new Color(255, 255, 255, 255), // white
            new Color(255, 255, 255, 192),
            new Color(255, 255, 255, 128),
            new Color(255, 255, 255, 64),
            new Color(64, 64, 64, 255),
            // darkGray
            new Color(64, 64, 64, 192),
            new Color(64, 64, 64, 128),
            new Color(64, 64, 64, 64),
            new Color(128, 128, 128, 255),
            // gray
            new Color(128, 128, 128, 192),
            new Color(128, 128, 128, 128),
            new Color(128, 128, 128, 64),
            new Color(192, 192, 192, 255),
            // lightGray
            new Color(192, 192, 192, 192),
            new Color(192, 192, 192, 128),
            new Color(192, 192, 192, 64),
            new Color(0, 0, 0, 255),
            // black
            new Color(0, 0, 0, 192),
            new Color(0, 0, 0, 128),
            new Color(0, 0, 0, 64),
            new Color(255, 200, 0, 255),
            // orange
            new Color(255, 200, 0, 192),
            new Color(255, 200, 0, 128),
            new Color(255, 200, 0, 64),
            new Color(255, 175, 175, 255),
            // pink
            new Color(255, 175, 175, 192),
            new Color(255, 175, 175, 128),
            new Color(255, 175, 175, 64),
            new Color(0, 0, 255, 255),
            // blue
            new Color(0, 0, 255, 192),
            new Color(0, 0, 255, 128),
            new Color(0, 0, 255, 64),
            new Color(255, 0, 0, 255),
            // red
            new Color(255, 0, 0, 192),
            new Color(255, 0, 0, 128),
            new Color(255, 0, 0, 64),
            new Color(0, 255, 0, 255),
            // green
            new Color(0, 255, 0, 192),
            new Color(0, 255, 0, 128),
            new Color(0, 255, 0, 64),
            new Color(255, 0, 255, 255),
            // magenta
            new Color(255, 0, 255, 192),
            new Color(255, 0, 255, 128),
            new Color(255, 0, 255, 64),
            new Color(255, 255, 0, 255),
            // yellow
            new Color(255, 255, 0, 192),
            new Color(255, 255, 0, 128),
            new Color(255, 255, 0, 64),
            new Color(0, 255, 255, 255),
            // cyan
            new Color(0, 255, 255, 192),
            new Color(0, 255, 255, 128),
            new Color(0, 255, 255, 64),
            new Color(0, 0, 153, 255),
            // dark blue
            new Color(0, 0, 153, 192),
            new Color(0, 0, 153, 128),
            new Color(0, 0, 153, 64),
            new Color(102, 102, 255, 255),
            // light blue
            new Color(102, 102, 255, 192),
            new Color(102, 102, 255, 128),
            new Color(102, 102, 255, 64),
            new Color(204, 204, 255, 255),
            // very light blue
            new Color(204, 204, 255, 192),
            new Color(204, 204, 255, 128),
            new Color(204, 204, 255, 64),
            new Color(255, 102, 102, 255),
            // light red
            new Color(255, 102, 102, 192),
            new Color(255, 102, 102, 128),
            new Color(255, 102, 102, 64),
            new Color(255, 204, 204, 255),
            // very light red
            new Color(255, 204, 204, 192),
            new Color(255, 204, 204, 128),
            new Color(255, 204, 204, 64),
            new Color(0, 102, 0, 255),
            // dark green
            new Color(0, 102, 0, 192),
            new Color(0, 102, 0, 128),
            new Color(0, 102, 0, 64),
            new Color(102, 255, 102, 255),
            // light green
            new Color(102, 255, 102, 192),
            new Color(102, 255, 102, 128),
            new Color(102, 255, 102, 64),
            new Color(204, 255, 204, 255),
            // very light green
            new Color(204, 255, 204, 192),
            new Color(204, 255, 204, 128),
            new Color(204, 255, 204, 64),
            new Color(102, 0, 102, 255),
            // dark rose
            new Color(102, 0, 102, 192),
            new Color(102, 0, 102, 128),
            new Color(102, 0, 102, 64),
            new Color(255, 0, 255, 255),
            // rose
            new Color(255, 0, 255, 192),
            new Color(255, 0, 255, 128),
            new Color(255, 0, 255, 64),
            new Color(255, 102, 255, 255),
            // light rose
            new Color(255, 102, 255, 192),
            new Color(255, 102, 255, 128),
            new Color(255, 102, 255, 64),
            new Color(255, 204, 255, 255),
            // very light rose
            new Color(255, 204, 255, 192),
            new Color(255, 204, 255, 128),
            new Color(255, 204, 255, 64),
            new Color(102, 102, 0, 255),
            // dark yellow
            new Color(102, 102, 0, 192),
            new Color(102, 102, 0, 128),
            new Color(102, 102, 0, 64),
            new Color(255, 255, 102, 255),
            // light yellow
            new Color(255, 255, 102, 192),
            new Color(255, 255, 102, 128),
            new Color(255, 255, 102, 64),
            new Color(255, 255, 204, 255),
            // very light yellow
            new Color(255, 255, 204, 192),
            new Color(255, 255, 204, 128),
            new Color(255, 255, 204, 64),
            new Color(0, 0, 102, 255),
            // dark turquoise
            new Color(0, 0, 102, 192),
            new Color(0, 0, 102, 128),
            new Color(0, 0, 102, 64),
            new Color(102, 255, 255, 255),
            // light turquoise
            new Color(102, 255, 255, 192),
            new Color(102, 255, 255, 128),
            new Color(102, 255, 255, 64),
            new Color(204, 255, 255, 255),
            // very light turquoise
            new Color(204, 255, 255, 192),
            new Color(204, 255, 255, 128),
            new Color(204, 255, 255, 64),
            new Color(153, 0, 255, 255),
            // violet
            new Color(153, 0, 255, 192),
            new Color(153, 0, 255, 128),
            new Color(153, 0, 255, 64),
            new Color(102, 0, 153, 255),
            // dark violet
            new Color(102, 0, 153, 192),
            new Color(102, 0, 153, 128),
            new Color(102, 0, 153, 64),
            new Color(153, 102, 255, 255),
            // blueish light violet
            new Color(153, 102, 255, 192),
            new Color(153, 102, 255, 128),
            new Color(153, 102, 255, 64),
            new Color(204, 102, 255, 255),
            // redish light violet
            new Color(204, 102, 255, 192),
            new Color(204, 102, 255, 128),
            new Color(204, 102, 255, 64),
            new Color(204, 102, 0, 255),
            // light brown
            new Color(204, 102, 0, 192),
            new Color(204, 102, 0, 128),
            new Color(204, 102, 0, 64),
            new Color(255, 102, 51, 255),
            // dark orange
            new Color(255, 102, 51, 192),
            new Color(255, 102, 51, 128),
            new Color(255, 102, 51, 64),
            new Color(255, 204, 153, 255),
            // light orange
            new Color(255, 204, 153, 192),
            new Color(255, 204, 153, 128),
            new Color(255, 204, 153, 64),
            new Color(255, 215, 0, 255),
            // gold
            new Color(255, 215, 0, 192),
            new Color(255, 215, 0, 128),
            new Color(255, 215, 0, 64),
            new Color(240, 230, 140, 255),
            // khaki
            new Color(240, 230, 140, 192),
            new Color(240, 230, 140, 128),
            new Color(240, 230, 140, 64),
            new Color(218, 165, 32, 255),
            // goldenrod
            new Color(218, 165, 32, 192),
            new Color(218, 165, 32, 128),
            new Color(218, 165, 32, 64),
            new Color(245, 245, 220, 255),
            // beige
            new Color(245, 245, 220, 192),
            new Color(245, 245, 220, 128),
            new Color(245, 245, 220, 64),
            new Color(255, 228, 181, 255),
            // moccasin
            new Color(255, 228, 181, 192),
            new Color(255, 228, 181, 128),
            new Color(255, 228, 181, 64),
            new Color(255, 99, 71, 255),
            // tomato
            new Color(255, 99, 71, 192),
            new Color(255, 99, 71, 128),
            new Color(255, 99, 71, 64),
            new Color(255, 140, 0, 255),
            // darkorange
            new Color(255, 140, 0, 192),
            new Color(255, 140, 0, 128),
            new Color(255, 140, 0, 64),
            new Color(220, 20, 60, 255),
            // crimson
            new Color(220, 20, 60, 192),
            new Color(220, 20, 60, 128),
            new Color(220, 20, 60, 64),
            new Color(70, 130, 180, 255),
            // steelblue
            new Color(70, 130, 180, 192),
            new Color(70, 130, 180, 128),
            new Color(70, 130, 180, 64),
            new Color(65, 105, 225, 255),
            // royalblue
            new Color(65, 105, 225, 192),
            new Color(65, 105, 225, 128),
            new Color(65, 105, 225, 64),
            new Color(123, 104, 238, 255),
            // medslateblue
            new Color(123, 104, 238, 192),
            new Color(123, 104, 238, 128),
            new Color(123, 104, 238, 64),
            new Color(127, 255, 212, 255),
            // aquamarine
            new Color(127, 255, 212, 192),
            new Color(127, 255, 212, 128),
            new Color(127, 255, 212, 64),
            new Color(0, 255, 127, 255),
            // springgreen
            new Color(0, 255, 127, 192),
            new Color(0, 255, 127, 128),
            new Color(0, 255, 127, 64),
            new Color(150, 205, 50, 255),
            // yellowgreen
            new Color(150, 205, 50, 192),
            new Color(150, 205, 50, 128),
            new Color(150, 205, 50, 64),
            new Color(216, 191, 216, 255),
            // thistle
            new Color(216, 191, 216, 192),
            new Color(216, 191, 216, 128),
            new Color(216, 191, 216, 64),
            new Color(245, 222, 179, 255),
            // wheat
            new Color(245, 222, 179, 192),
            new Color(245, 222, 179, 128),
            new Color(245, 222, 179, 64),
            new Color(160, 82, 45, 255),
            // siena
            new Color(160, 82, 45, 192),
            new Color(160, 82, 45, 128),
            new Color(160, 82, 45, 64),
            new Color(233, 150, 122, 255),
            // darksalmon
            new Color(233, 150, 122, 192),
            new Color(233, 150, 122, 128),
            new Color(233, 150, 122, 64),
            new Color(165, 42, 42, 255),
            // brown
            new Color(165, 42, 42, 192),
            new Color(165, 42, 42, 128),
            new Color(165, 42, 42, 64),
            new Color(210, 105, 30, 255),
            // chocolate
            new Color(210, 105, 30, 192),
            new Color(210, 105, 30, 128),
            new Color(210, 105, 30, 64),
            new Color(244, 164, 96, 255),
            // sandybrown
            new Color(244, 164, 96, 192),
            new Color(244, 164, 96, 128),
            new Color(244, 164, 96, 64),
            new Color(255, 20, 147, 255),
            // deeppink
            new Color(255, 20, 147, 192),
            new Color(255, 20, 147, 128),
            new Color(255, 20, 147, 64),
            new Color(255, 105, 180, 255),
            // hotpink
            new Color(255, 105, 180, 192), new Color(255, 105, 180, 128),
            new Color(255, 105, 180, 64),
            new Color(221, 160, 221, 255),
            // plum
            new Color(221, 160, 221, 192), new Color(221, 160, 221, 128), new Color(221, 160, 221, 64),
            new Color(186, 85, 211, 255),
            // medorchid
            new Color(186, 85, 211, 192), new Color(186, 85, 211, 128), new Color(186, 85, 211, 64),
            new Color(112, 128, 144, 255),
            // slategray
            new Color(112, 128, 144, 192), new Color(112, 128, 144, 128), new Color(112, 128, 144, 64) };
}
