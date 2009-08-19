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
 * Created on 08.02.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

class VolumeSlider extends JInternalFrame {
    private JSlider volumeLevelSlider;
    private JPanel volumePanel;
    private JLabel volumeLabel;
    private JCheckBox muteBox;

    // TODO: rework volume slider - auto closing is unfamiliar
    // TODO: doesn't work in fullscreen mode
    public VolumeSlider(final VolumeControl volumeControl, final AbstractButton volumeButton) {
        // frame properties
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setClosable(true);

        this.addInternalFrameListener(new InternalFrameAdapter() {
            // don't close - reuse
            public void internalFrameClosing(InternalFrameEvent arg0) {
                setVisible(false);
            }

            // turn off
            public void internalFrameDeactivated(InternalFrameEvent e) {
                setVisible(false);
            }
        });

        // icons
        // TODO: maybe: should be moved to constants
        final URL urlVolume = this.getClass().getResource("resources/Volume24.gif");
        final Icon volumeOnIcon = new ImageIcon(urlVolume);
        final Icon volumeOffIcon = new NegatedImageIcon(urlVolume);

        // mute button
        muteBox = new JCheckBox("mute");
        muteBox.setHorizontalAlignment(JCheckBox.CENTER);
        muteBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                if (muteBox.isSelected()) {
                    volumeLevelSlider.setValue(0);
                    volumeLabel.setText("Vol: muted");
                    volumeButton.setToolTipText("Volume (turned off)");
                    volumeControl.setMute(true); // must be behind setValue
                } else {
                    volumeLevelSlider.setValue(volumeControl.getVolumeLevel());
                    volumeControl.setMute(false); // must be behind setValue
                }
                volumeButton.setIcon(volumeControl.getMute() ? volumeOffIcon : volumeOnIcon);
            }
        });

        // display volume
        volumeLabel = new JLabel(" Vol: 100% ");
        volumeLabel.setHorizontalAlignment(JLabel.CENTER);

        // volume slider
        volumeLevelSlider = new JSlider(JSlider.VERTICAL, 0, 100, 100);
        volumeLevelSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                // set volume
                // only if adjusted by user (filter calls caused by mute button)
                if (volumeLevelSlider.getValueIsAdjusting())
                    volumeControl.setVolumeLevel(volumeLevelSlider.getValue());
                volumeLabel.setText("Vol: " + volumeLevelSlider.getValue() + "%");
                volumeButton.setToolTipText("Volume " + volumeLevelSlider.getValue() + "%");

                // always mute off
                if (volumeControl.getMute()) {
                    volumeControl.setMute(false);
                    muteBox.setSelected(false);
                }
            }
        });

        volumeButton.setToolTipText("Volume " + volumeLevelSlider.getValue() + "%");

        volumeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // show volume slider
                PointerInfo info = MouseInfo.getPointerInfo();
                Point location = info.getLocation();

                if (info.getDevice().getFullScreenWindow() == null)
                    location.translate(-TTT.getInstance().getX(), -TTT.getInstance().getY() - 30);

                showSlider(location.x - getWidth() / 2, location.y - getHeight() + 10);
            }
        });

        // handle clicking on the slider track to set playback time
        volumeLevelSlider.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // only set the timeSlider to the selected value if significantly different from current value
                // don't want it being set if the user is only trying to click on the thumb
                volumeLevelSlider.setValue(100 - e.getY() * 100 / volumeLevelSlider.getHeight());
            }
        });

        volumePanel = new JPanel(new BorderLayout());
        volumePanel.add(volumeLabel, BorderLayout.NORTH);
        volumeLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        volumePanel.add(volumeLevelSlider, BorderLayout.CENTER);
        volumePanel.add(muteBox, BorderLayout.SOUTH);
        getContentPane().add(volumePanel);

        // high layer so stays on top
        // TODO: think about layer number
        // TODO: is not visible in fullscreen mode
        // TTT.getInstance().addInternalFrame(this, 0, 0, 10);

        pack();
    }

    // display volume controls at given position
    public void showSlider(int x, int y) {
        Component component = TTT.getInstance().getGlassPane();
        if (component != null && component instanceof JPanel) {
            JPanel glassPane = (JPanel) component;
            glassPane.setLayout(null);
            glassPane.add(this);
            setLocation(x, y);
            setVisible(true);
            glassPane.setVisible(true);
        }
    }
}