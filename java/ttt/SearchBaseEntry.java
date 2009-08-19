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
 * Created on 08.09.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author ziewer
 * 
 * To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code
 * and Comments
 */
public class SearchBaseEntry {

    // character widths for dynamic underlining
    static int[] widths = new int[256];
    static {
        // choose standard font (proportions will be similar for other fonts)
        Font f = new Font("Serif", Font.PLAIN, 16);
        FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(f);

        // determine size of printable characters (all others will be zero)
        for (char c = 32; c < 127; c++)
            widths[c] = metrics.charWidth(c);
        for (char c = 161; c < 256; c++)
            widths[c] = metrics.charWidth(c);
    }

    private int x, y, width, height;

    // TODO: maybe ratio should be handled in index
    private double ratio = 1;

    double getRatio() {
        return ratio;
    }

    String searchText = "";
    String searchTextOriginal = "";

    public SearchBaseEntry(String searchtext, int x, int y, int width, int height, double ratio) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.ratio = ratio;

        this.searchTextOriginal = searchtext;
        this.searchText = Constants.reduce(searchtext);
    }

    // for highlighting search results
    static int borderSize = 6;
    static Color highligh = new Color(Color.CYAN.getRed(), Color.CYAN.getGreen(), Color.CYAN.getBlue(), 64);
    static Color underline = Color.RED;
    static Color border = new Color(Color.RED.getRed(), Color.RED.getGreen(), Color.RED.getBlue(), 192);

    // TODO: return value not used / think about naming of method
    // adds words from searchbase containing searchword to resultss
    public boolean contains(String searchword, ArrayList<ColoredShape> results) {
        // System.out.println(searchText);
        if (searchText.indexOf(Constants.reduce(searchword)) >= 0) {

            // highlight search results
            if (results != null) {
                // NOTE: explicit round to avoid java drawing bug (different rounding if color is transparent)
                Rectangle2D rectangle = new Rectangle2D.Double((int) (getX() - borderSize),
                        (int) (getY() - borderSize), (int) (getWidth() + 2 * borderSize),
                        (int) (getHeight() + 2 * borderSize));

                // highlight
                results.add(new ColoredShape(highligh, rectangle, true));

                // border
                results.add(new ColoredShape(border, rectangle, false));

                // underline results
                boolean fixedSize = false;
                if (fixedSize) {
                    // using fixed sized font
                    // TODO: use character specific letter size - variable sized font
                    double sizeOfLetter = (double) (getWidth()) / searchText.length();
                    int indexOf = -1;
                    // System.out.println("\"" + result.searchText + "\"");
                    while (-1 != (indexOf = searchText.indexOf(searchword, indexOf + 1))) {
                        // System.out.println(indexOf + "\t" + sizeOfLetter + "\t" + (result.right - result.left));
                        results.add(new ColoredShape(underline, new Rectangle2D.Double(getX() + sizeOfLetter * indexOf,
                                getY() + getHeight() + borderSize - 2, sizeOfLetter * (searchword.length()), 4), true));
                    }

                    // TODO: only for testing - remove
                    // letter ticks
                    if (!false)
                        for (int j = 0; j <= searchText.length(); j++) {
                            results.add(new ColoredShape(underline, new Line2D.Double(getX() + j * sizeOfLetter, getY()
                                    + getHeight() - 3, getX() + j * sizeOfLetter, getY() + getHeight() + 3), false));
                        }
                } else {
                    // proportional size
                    int charWidths[] = new int[searchTextOriginal.length()];
                    int overall = 0;
                    for (int i = 0; i < charWidths.length; i++) {
                        // determine character widths
                        try {
                            charWidths[i] = widths[searchTextOriginal.charAt(i)];
                            overall += charWidths[i];
                        } catch (IndexOutOfBoundsException e) {}
                    }
                    double ratio = getWidth() / overall;

                    // determine occurrences
                    int indexOf = -1;
                    // System.out.println("\"" + result.searchText + "\"");
                    while (-1 != (indexOf = searchTextOriginal.toLowerCase().indexOf(searchword.toLowerCase(),
                            indexOf + 1))) {
                        int start = 0;
                        for (int i = 0; i < indexOf; i++)
                            start += charWidths[i];
                        int end = 0;
                        for (int i = indexOf; i < indexOf + searchword.length(); i++)
                            end += charWidths[i];

                        // System.out.println(indexOf + "\t" + sizeOfLetter + "\t" + (result.right - result.left));
                        results.add(new ColoredShape(underline, new Rectangle2D.Double(getX() + start * ratio, getY()
                                + getHeight() + borderSize - 3, end * ratio, 5), true));

                        // TODO: only for testing - remove
                        // letter ticks
                        if (false) {
                            int sum = 0;
                            results.add(new ColoredShape(underline, new Line2D.Double(getX(), getY() + getHeight() - 3,
                                    getX(), getY() + getHeight() + 3), false));
                            for (int j = 0; j < searchTextOriginal.length(); j++) {
                                sum += charWidths[j];
                                results.add(new ColoredShape(underline, new Line2D.Double(getX() + sum * ratio, getY()
                                        + getHeight() - 3, getX() + sum * ratio, getY() + getHeight() + 3), false));
                            }
                        }
                    }

                }

            }
            return true;

        } else
            return false;
    }

    public String toString() {
        return "\"" + searchText + "\" at (" + (int) getX() + "," + (int) getY() + ") size " + (int) getWidth() + " x "
                + (int) getHeight();
    }

    private double getX() {
        return x * ratio;
    }

    private double getY() {
        return y * ratio;
    }

    private double getWidth() {
        return width * ratio;

    }

    private double getHeight() {
        return height * ratio;
    }

    // ////////////////////////////////////////////////////
    // I/O used for searchbase extension
    // ////////////////////////////////////////////////////

    // write as part of searchbase extension
    public void write(DataOutputStream out) throws IOException {
        // search string
        // NOTE: String.length() not always equals String.getBytes().length
        // depending on the system's character encoding (Umlauts may fail)
        // out.writeShort(searchTextOriginal.length());
        out.writeShort(searchTextOriginal.getBytes().length);
        out.write(searchTextOriginal.getBytes());

        // coordinates (without ratio)
        out.writeShort(x);
        out.writeShort(y);
        out.writeShort(width);
        out.writeShort(height);
    }

    // read as part of searchbase extension
    static public SearchBaseEntry read(DataInputStream in, double ratio) throws IOException {
        int size = in.readShort();
        byte[] string = new byte[size];
        in.readFully(string);

        return new SearchBaseEntry(new String(string), in.readShort(), in.readShort(), in.readShort(), in.readShort(),
                ratio);
    }
}