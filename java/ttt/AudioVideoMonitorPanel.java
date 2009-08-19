// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universit�t M�nchen
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
 * Created on 22.11.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class AudioVideoMonitorPanel extends JPanel {

    public static void main(String[] args) throws Exception {

        JFrame frame = new JFrame("Audio Video Monitor");
        frame.add(new AudioVideoMonitorPanel());

        frame.setSize(300, 300);
        frame.setVisible(true);
    }

    private AudioMonitorPanel audioMonitorPanel;

    public AudioVideoMonitorPanel() {
        setOpaque(false);

        try {
            audioMonitorPanel = new AudioMonitorPanel(true);
        } catch (Exception e) {
            System.out.println("Cannot open audio monitor: " + e);
            // e.printStackTrace();
        }

        if (audioMonitorPanel != null) {
            add(audioMonitorPanel);
            audioMonitorPanel.setPreferredSize(new Dimension(128, 16));

        }
    }

    public void close() {
        if (audioMonitorPanel != null)
            audioMonitorPanel.stop();
        audioMonitorPanel = null;
        removeAll();
    }
}
