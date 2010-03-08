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
 * Created on 01.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.net.InetAddress;
import java.util.Date;

public class ProtocolPreferences {
    public String versionMsg = Constants.VersionMessageRFB;
    public String name;
    public int framebufferWidth;
    public int framebufferHeight;
    public int bitsPerPixel;
    public int bytesPerPixel;
    public int depth;
    public int preferedDepth = Constants.defaultColorDepth;
    public boolean bigEndian, trueColour;
    public int redMax, greenMax, blueMax;
    public int redShift, greenShift, blueShift;

    public int[] encodings = Constants.encodings;

    public boolean ignoreCursorUpdates;

    // live connection
    public String password;
    public InetAddress host;
    public int port;

    public long starttime = System.currentTimeMillis();

    // set color depth (and corresponding pixel format)
    // does not send message to server
    public void setDepth(int depth) {

        boolean bigEndian = false; // default

        switch (depth) {
        case 8:
            setPixelFormat(8, 8, bigEndian, true, 7, 7, 3, 0, 3, 6);
            break;
        case 16:
            setPixelFormat(16, 16, bigEndian, true, 31, 31, 63, 0, 5, 10);
            break;
        case 24:
        case 32:
        default:
            setPixelFormat(32, 24, bigEndian, true, 255, 255, 255, 16, 8, 0);
            break;
        }
    }

    // set pixel format
    public void setPixelFormat(int bitsPerPixel, int depth, boolean bigEndian, boolean trueColour, int redMax,
            int greenMax, int blueMax, int redShift, int greenShift, int blueShift) {

        this.bitsPerPixel = bitsPerPixel;
        switch (bitsPerPixel) {
        case 8:
            bytesPerPixel = 1;
            break;
        case 16:
            bytesPerPixel = 2;
            break;
        default:
            bytesPerPixel = 4;
            break;
        }
        this.depth = depth;
        this.preferedDepth = depth;
        this.bigEndian = bigEndian;
        this.trueColour = trueColour;
        this.redMax = redMax;
        this.greenMax = greenMax;
        this.blueMax = blueMax;
        this.redShift = redShift;
        this.greenShift = greenShift;
        this.blueShift = blueShift;
    }

    // set encodings
    public void setEncodings(int[] encodings) {
        this.encodings = encodings;
    }

    public String toString() {
        return "Desktop: " + name + "\n" + "Size: " + framebufferWidth + " x " + framebufferHeight + " (" + depth
                + " bit" + (trueColour ? "" : " no") + " truecolor)\n" + bitsPerPixel + " bits per pixel, "
                + bytesPerPixel + " bytes per pixel, " + (bigEndian ? "BigEndian" : "LittleEndian") + "\n"
                + "RGB max : " + redMax + " " + greenMax + " " + blueMax + " - RGB shift: " + redShift + " "
                + greenShift + " " + blueShift + "\n"
                + (starttime > 0 ? "Starttime: " + new Date(starttime) : "" + "\n" + "Protocol Version: " + versionMsg);
    }
}
