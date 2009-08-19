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
 * Created on 11.01.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.messages;

import java.io.IOException;
import java.io.OutputStream;

import ttt.Constants;

public class PointerEventMessage extends UserInputMessage {
    private byte[] buffer = new byte[6];

    public PointerEventMessage(int pointerMask, int x, int y) {
        buffer[0] = (byte) Constants.PointerEvent;
        buffer[1] = (byte) pointerMask;
        buffer[2] = (byte) ((x >> 8) & 0xff);
        buffer[3] = (byte) (x & 0xff);
        buffer[4] = (byte) ((y >> 8) & 0xff);
        buffer[5] = (byte) (y & 0xff);
    }

    public void writeRFB(OutputStream out) throws IOException {
        out.write(buffer);
    }
}
