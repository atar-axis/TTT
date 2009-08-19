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
 * Created on 20.02.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class MyTitledBorder extends TitledBorder {

    public MyTitledBorder(Border arg0, String arg1) {
        super(arg0, arg1);
    }

    public MyTitledBorder(Border arg0, String arg1, int arg2, int arg3) {
        super(arg0, arg1, arg2, arg3);
    }

    public Insets getBorderInsets(Component component) {
        return getBorderInsets(component, new Insets(0, 0, 0, 0));
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        FontMetrics fm;
        int descent = 0;
        int ascent = 16;
        int height = 16;

        Border border = getBorder();
        if (border != null) {
            if (border instanceof AbstractBorder) {
                ((AbstractBorder) border).getBorderInsets(c, insets);
            } else {
                // Can't reuse border insets because the Border interface
                // can't be enhanced.
                Insets i = border.getBorderInsets(c);
                insets.top = i.top;
                insets.right = i.right;
                insets.bottom = i.bottom;
                insets.left = i.left;
            }
        } else {
            insets.left = insets.top = insets.right = insets.bottom = 0;
        }

        insets.left += EDGE_SPACING + TEXT_SPACING;
        insets.right += EDGE_SPACING + TEXT_SPACING;
        insets.top += EDGE_SPACING + TEXT_SPACING;
        insets.bottom += EDGE_SPACING + TEXT_SPACING;

        if (c == null || getTitle() == null || getTitle().equals("")) {
            return insets;
        }

        Font font = getFont(c);

        fm = c.getFontMetrics(font);

        if (fm != null) {
            descent = fm.getDescent();
            ascent = fm.getAscent();
            height = fm.getHeight();
        }

        switch (getTitlePosition()) {
        case ABOVE_TOP:
            insets.top += ascent + descent + (Math.max(EDGE_SPACING, TEXT_SPACING * 2) - EDGE_SPACING);
            break;
        case TOP:
        case DEFAULT_POSITION:
            int margin = (ascent + descent) / 2;
            insets.top += (ascent + descent) / 2 - 10 + margin;
            insets.bottom += -9 + 3; 
            insets.left += -9 + 3;
            insets.right += -9 + 3;
            break;
        case BELOW_TOP:
            insets.top += ascent + descent + TEXT_SPACING;
            break;
        case ABOVE_BOTTOM:
            insets.bottom += -3;
            insets.top += -3;
            insets.left += -3;
            insets.right += -3;
            break;
        case BOTTOM:
            insets.bottom += ((ascent + descent) / 2 - 3);
            insets.top += -3;
            insets.left += -3;
            insets.right += -3;
            break;
        case BELOW_BOTTOM:
            insets.bottom += height;
            break;
        }
        return insets;
    }
}
