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

package ttt.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import ttt.Constants;

public class IndexListModel extends DefaultListModel implements ListCellRenderer {
    ArrayList index;

    ArrayList fullIndex;

    public IndexListModel(ArrayList index, ArrayList fullIndex) {
        this.fullIndex = fullIndex;
        this.index = index;
    }

    /*******************************************************************************************************************
     * ListModel *
     ******************************************************************************************************************/
    public int getSize() {
        return index.size();
    }

    public Object getElementAt(int position) {
        return index.get(position);
    }

    public void update() {
        fireContentsChanged(this, 0, getSize());
    }

    private int nowPlayingIndex = -1;

    public void setNowPlayingIndex(int index) {
        nowPlayingIndex = index;
        update();
    }

    public int getAbsoluteIndex(int index) {
        if (fullIndex == null)
            return index;

        // get index number relative to complete index of recording (not relative to ListModel)
        try {
            return this.index.indexOf(fullIndex.get(index));
        } catch (Exception e) {
            return -1;
        }
    }

    /*******************************************************************************************************************
     * CellRenderer *
     ******************************************************************************************************************/
    static Border raisedetched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED, Color.LIGHT_GRAY, Color.GRAY);
    static Border raisedetchedRed = BorderFactory.createEtchedBorder(EtchedBorder.RAISED, Color.RED, Color.GRAY);
    static Border raisedetchedBlack = BorderFactory.createEtchedBorder(EtchedBorder.RAISED, Color.BLACK, Color.GRAY);

    public Component getListCellRendererComponent(JList list, Object value, int relativeIndex, boolean isSelected,
            boolean cellHasFocus) {

        // get index entry
        IndexEntry entry = (IndexEntry) getElementAt(relativeIndex);

        // get index number relative to complete index of recording (not relative to ListModel)
        int indexNumber = fullIndex == null ? relativeIndex : fullIndex.indexOf(entry);

        // thumbnail painting is handelt by IndexEntry
        JLabel thumbnailLabel = entry;

        // inner border with timestamp at lower right
        Border border = isSelected ? raisedetchedBlack : indexNumber == nowPlayingIndex ? raisedetchedRed
                : raisedetched;
        border = new MyTitledBorder(border, Constants.getStringFromTime(entry.getTimestamp(), false),
                TitledBorder.TRAILING, TitledBorder.ABOVE_BOTTOM);
        thumbnailLabel.setBorder(border);

        // create panel with outer border containing index number and title
        JPanel panel = new GradientPanel(SwingConstants.HORIZONTAL);
        panel.add(thumbnailLabel);
        panel.setToolTipText("#" + (relativeIndex + 1) + ": "
                + (entry.getTitle().length() > 0 ? entry.getTitle() + " " : "") + "["
                + Constants.getStringFromTime(entry.getTimestamp(), false) + "]");

        // outer border with index number and title
        panel.setBorder(new MyTitledBorder(raisedetched, (indexNumber + 1) + "."
                + (entry.getTitle() != null ? " " + entry.getTitle() : "")));

        return panel;
    }
}

class DefaultIcon implements Icon {
    int width, height;

    public DefaultIcon(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getIconHeight() {
        return height;
    }

    public int getIconWidth() {
        return width;
    }

    public void paintIcon(Component arg0, Graphics arg1, int arg2, int arg3) {}
}
