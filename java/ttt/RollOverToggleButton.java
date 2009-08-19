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
 * Created on 05.05.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.Border;

public class RollOverToggleButton extends JButton {
    private boolean rollover;
    private boolean selected;

    private Border defaultBorder;
    private Border selectedBorder;

    public RollOverToggleButton(Icon icon) {
        this(null, icon, null);
    }

    public RollOverToggleButton(String text) {
        this(text, null, null);
    }

    public RollOverToggleButton(Icon icon, String command) {
        this(null, icon, command);
    }

    public RollOverToggleButton(String label, Icon icon, String command) {
        // transparent background
        setOpaque(false);
        Color color = getBackground();
        setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0));

        // no margin
        if (icon != null)
            setMargin(new Insets(0, 0, 0, 0));
        else
            setMargin(new Insets(4, 2, 3, 2));

        // borders
        defaultBorder = getBorder();
        selectedBorder = BorderFactory.createLoweredBevelBorder();
        // add additional empty border if insets of one border are larger then others
        // Note: mix is not supported
        Insets d = defaultBorder.getBorderInsets(this);
        Insets s = selectedBorder.getBorderInsets(this);
        if (d.top > s.top || d.left > s.left || d.bottom > s.bottom || d.right > s.right)
            selectedBorder = BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory
                    .createEmptyBorder(Math.max(0, d.top - s.top), Math.max(0, d.left - s.left), Math.max(0, d.bottom
                            - s.bottom), Math.max(0, d.right - s.right)));
        else if (d.top < s.top || d.left < s.left || d.bottom < s.bottom || d.right < s.right)
            defaultBorder = BorderFactory.createCompoundBorder(defaultBorder, BorderFactory.createEmptyBorder(Math.max(
                    0, s.top - d.top), Math.max(0, s.left - d.left), Math.max(0, s.bottom - d.bottom), Math.max(0,
                    s.right - d.right)));

        // icon
        setIcon(icon);

        // text
        setText(label);

        // action
        if (command != null) {
            setActionCommand(command);
            setToolTipText(command);
        }

        this.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent arg0) {
                rollover = false;
            }

            public void mouseEntered(MouseEvent arg0) {
                rollover = true;
            }

            public void mousePressed(MouseEvent arg0) {
                // NOTE: must be done before mouseReleased() or mouseClicked() to ensure correct selected-state for all
                // other Listeners
                selected = !selected;
                update();
            }
        });
    }

    private void update() {
        rollover = false;
        setBorder(selected ? selectedBorder : defaultBorder);
    }

    public boolean isSelected() {
        return selected;
    }

    private boolean update;

    public void setSelected(boolean selected) {
        super.setSelected(selected);
        this.selected = selected;
        // NOTE: must be done when visible
        update = true;
    }

    protected void paintBorder(Graphics g) {
        if (update) {
            update();
            update = false;
        }

        if (rollover || selected)
            super.paintBorder(g);
    }
}
