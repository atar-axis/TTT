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
 * Created on 15.03.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.messages;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ttt.GraphicsContext;

public abstract class UserInputMessage extends Message {
    // these user input messages are RFB/VNC input messages

    abstract public void writeRFB(OutputStream out) throws IOException;

    // not needed for user input event messages
    public int getEncoding() {
        return -1;
    }

    // not needed for user input event messages
    public void paint(GraphicsContext graphicsContext) {}

    // not needed for user input event messages
    public void write(DataOutputStream out, int writeTimestamp) throws IOException {}

    // not needed for user input event messages
    public int getSize() {
        return 0;
    }
}
