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
 * Created on 14.03.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;

public class RollOverRadioButton extends JButton {
    private boolean rollover;

    RollOverRadioButton(Icon icon, String command) {
        // transparent background
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));
        // no margin
        setMargin(new Insets(0, 0, 0, 0));

        // icon
        setIcon(icon);

        // action
        if (command != null) {
            setToolTipText(command);
            setActionCommand(command);
        }

        this.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent arg0) {
                rollover = false;
            }

            public void mouseEntered(MouseEvent arg0) {
                rollover = true;
            }
        });
    }

    protected void paintBorder(Graphics g) {
        if (rollover)
            super.paintBorder(g);
    }
}
