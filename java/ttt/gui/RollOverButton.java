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
package ttt.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.Border;

public class RollOverButton extends JButton {
    private boolean rollover;

    public RollOverButton(String label) {
        this(label, null, null);
    }

    public RollOverButton(Icon icon) {
        this(null, icon, null);
    }

    public RollOverButton(Icon icon, String command) {
        this(null, icon, command);
    }

    public RollOverButton(String label, Icon icon, String command) {
        // transparent background
        setOpaque(false);
        Color color = getBackground();
        setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0));

        // no margin
        if (icon != null)
            setMargin(new Insets(0, 0, 0, 0));
        else
            setMargin(new Insets(4, 2, 3, 2));

        // icon
        setIcon(icon);

        // text
        setText(label);

        // action
        if (command != null)
            setActionCommand(command);

        this.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent arg0) {
                rollover = false;
            }

            public void mouseEntered(MouseEvent arg0) {
                rollover = true;
            }
        });
    }

    public void setBorder(Border arg0) {
        super.setBorder(arg0);
    }

    protected void paintBorder(Graphics g) {
        if (rollover)
            super.paintBorder(g);
    }

    public void setActionCommand(String actionCommand) {
        super.setActionCommand(actionCommand);
        setToolTipText(actionCommand);
    }
}
