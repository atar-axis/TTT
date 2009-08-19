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
 * Created on 16.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public class MemoryMonitor extends JInternalFrame {
    private JProgressBar progressBar;
    private Timer timer;

    public MemoryMonitor() {
        super("Memory Monitor");

        // closing behaviour
        addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosing(InternalFrameEvent arg0) {
                TTT.getInstance().showMemoryMonitor(false);
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setClosable(true);

        // memory progress bar
        progressBar = new JProgressBar(0, (int) (Runtime.getRuntime().maxMemory() / 1024));
        progressBar.setValue((int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024));
        progressBar.setString(((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024)
                + " MByte");

        progressBar.setStringPainted(true);

        // Create timer to update progress bar
        timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                // update display
                if (isVisible()) {
                    progressBar
                            .setValue((int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024));
                    progressBar
                            .setString(((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024)
                                    + " MByte");
                }

                // stop if closed
                else
                    timer.stop();
            }
        });

        // Garbage Collection Button
        JButton button = new JButton("free");
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setToolTipText("Garbage Collection: Free unused memory");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                System.gc();
            }
        });

        // panel
        JPanel panel = new JPanel();
        panel.add(progressBar);
        panel.add(button);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // add and display
        getContentPane().add(panel);
        // setLocation(0, 900);
        // setLocation(1405, 0);
        pack();
        setVisible(true);
        timer.start();
    }
}
