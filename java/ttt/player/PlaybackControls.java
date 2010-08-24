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
 * Created on 31.01.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.player;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import ttt.Constants;
import ttt.gui.GradientPanel;
import ttt.gui.NegatedImageIcon;
import ttt.gui.RollOverButton;
import ttt.record.Recording;
import ttt.record.TimeChangedListener;


@SuppressWarnings("serial")
public class PlaybackControls extends GradientPanel implements TimeChangedListener, KeyListener {

    private Recording recording;

    // create controls
    public PlaybackControls(Recording recording) {
        this.recording = recording;

        // initialize GUI
        createGUI();

        // update controls corresponding to playbacktime
        recording.addTimeChangedListener(this);

        // dragging component if zoomed
        recording.graphicsContext.enableDraggingIfZoomed();

        // add key listener (playback key controls)
        recording.graphicsContext.addKeyListener(this);
        // TODO: seems not to work - fix
        recording.getIndexComponent().addKeyListener(this);
        addKeyListener(this);
    }

    public void dispose() {
        recording = null;
        if (volumeSlider != null)
            volumeSlider.dispose();
    }

    public int getTimeSliderValue(){
    	return timeSlider.getValue();
    }
    
    Icon playIcon, pauseIcon, stopIcon, nextIcon, previousIcon, volumeOnIcon, volumeOffIcon, saveIcon, findIcon,
            movieIcon;
    boolean timerChangedSlider;
    private JLabel durationLabel;
    private JLabel timeLabel;
    private TimeSlider timeSlider;
    private JButton volumeButton;
    private JButton stopButton;
    private JButton playPauseButton;
    private JButton previousButton;
    private JButton nextButton;
    AbstractButton indexAndVideoButton;
    AbstractButton fullscreenButton;

    // event handling
    public void timeChanged(int event) {
        switch (event) {
        case TimeChangedListener.PLAY:
            playPauseButton.setIcon(pauseIcon);
            playPauseButton.setToolTipText(Recording.PAUSE);
            playPauseButton.setActionCommand(Recording.PAUSE);
            break;
        case TimeChangedListener.PAUSE:
            playPauseButton.setIcon(playIcon);
            playPauseButton.setToolTipText(Recording.PLAY);
            playPauseButton.setActionCommand(Recording.PLAY);
            break;

        default:
            if (event >= 0)
                timeSlider.setValue(event, false);
            else
            // index
            if (recording.indexViewer != null)
                recording.indexViewer.setNowPlayingIndex(-event - 1);

        }
    }

    private VolumeSlider volumeSlider;

    // initialize GUI
    private void createGUI() {
        // initialize icons
        createIcons();

        // sync button
        // index and searching
        final AbstractButton syncPlusButton = new RollOverButton("+");
        final AbstractButton syncMinusButton = new RollOverButton("-");
        syncPlusButton.setToolTipText("shift sync +1 sec [now " + recording.audioVideoPlayer.getReplayOffset() / 1000
                + " sec ]");
        syncPlusButton.setSelected(recording.desktop_replay_factor_sync);
        syncPlusButton.setFocusable(false);
        syncPlusButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                recording.audioVideoPlayer.setReplayOffset(recording.audioVideoPlayer.getReplayOffset() + 1000);
                syncMinusButton.setToolTipText("shift sync -1 sec [now " + recording.audioVideoPlayer.getReplayOffset()
                        / 1000 + " sec ]");
                syncPlusButton.setToolTipText("shift sync +1 sec [now " + recording.audioVideoPlayer.getReplayOffset()
                        / 1000 + " sec ]");
            }
        });
        syncMinusButton.setToolTipText("shift sync -1 sec [now " + recording.audioVideoPlayer.getReplayOffset() / 1000
                + " sec ]");
        syncMinusButton.setSelected(recording.desktop_replay_factor_sync);
        syncMinusButton.setFocusable(false);
        syncMinusButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                recording.audioVideoPlayer.setReplayOffset(recording.audioVideoPlayer.getReplayOffset() - 1000);
                syncMinusButton.setToolTipText("shift sync -1 sec [now " + recording.audioVideoPlayer.getReplayOffset()
                        / 1000 + " sec ]");
                syncPlusButton.setToolTipText("shift sync +1 sec [now " + recording.audioVideoPlayer.getReplayOffset()
                        / 1000 + " sec ]");
            }
        });
        AbstractButton syncButton = new RollOverToggleButton("sync");
        syncButton.setToolTipText("turn on/off linear sync of desktop and audio");
        syncButton.setSelected(recording.desktop_replay_factor_sync);
        syncButton.setFocusable(false);
        syncButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                recording.desktop_replay_factor_sync = !recording.desktop_replay_factor_sync;
                syncMinusButton.setEnabled(recording.desktop_replay_factor_sync);
                syncPlusButton.setEnabled(recording.desktop_replay_factor_sync);
                if (recording.desktop_replay_factor_sync) {
                    syncMinusButton.setToolTipText("shift sync -1 sec [now "
                            + recording.audioVideoPlayer.getReplayOffset() / 1000 + " sec ]");
                    syncPlusButton.setToolTipText("shift sync +1 sec [now "
                            + recording.audioVideoPlayer.getReplayOffset() / 1000 + " sec ]");
                } else {
                    syncMinusButton.setToolTipText("shift sync disabled");
                    syncPlusButton.setToolTipText("shift sync disabled");
                }
            }
        });

        // playback buttons
        stopButton = new RollOverButton(stopIcon, Recording.STOP);
        stopButton.addActionListener(recording);
        stopButton.setFocusable(false);

        playPauseButton = new RollOverButton(pauseIcon, Recording.PAUSE);
        playPauseButton.addActionListener(recording);
        playPauseButton.setFocusable(false);

        nextButton = new RollOverButton(nextIcon, Recording.NEXT);
        nextButton.addActionListener(recording);
        nextButton.setFocusable(false);

        previousButton = new RollOverButton(previousIcon, Recording.PREVIOUS);
        previousButton.addActionListener(recording);
        previousButton.setFocusable(false);

        // index and searching
        indexAndVideoButton = new RollOverToggleButton(movieIcon, Recording.INDEX);
        indexAndVideoButton.setToolTipText("turn on/off index and video");
        indexAndVideoButton.setSelected(true);
        indexAndVideoButton.setFocusable(false);

        fullscreenButton = new RollOverToggleButton(Constants.getIcon("Fullscreen2_24.png"));
        fullscreenButton.setActionCommand(Recording.FULLSCREEN);
        fullscreenButton.setToolTipText("enable/disable fullscreen mode");
        fullscreenButton.setFocusable(false);

        // time and duration
        timeLabel = new JLabel(Constants.getStringFromTime(0, false));
        timeLabel.setForeground(Color.black);
        timeLabel.setToolTipText("Playback Time");

        durationLabel = new JLabel(" [" + Constants.getStringFromTime(recording.getDuration(), false) + "]");
        durationLabel.setForeground(Color.darkGray);
        durationLabel.setToolTipText("Duration");

        timeSlider = new TimeSlider(recording, timeLabel);
        timeSlider.setOpaque(false);
        timeSlider.requestFocusInWindow();

        // volume control
        volumeButton = new RollOverButton(recording.getMute() ? volumeOffIcon : volumeOnIcon, null);
        volumeButton.setFocusable(false);
        // TODO: rework volume slider - auto closing is unfamiliar
        // TODO: doesn't work in fullscreen mode
        volumeSlider = new VolumeSlider(recording, volumeButton);

        // scaling
        final String[] zoomOptions = { "auto", "50%", "75%", "100%", "150%", "200%" };
        JComboBox zoomBox = new JComboBox(zoomOptions);
        zoomBox.setSelectedItem("100%");
        zoomBox.setToolTipText("Zoom");
        zoomBox.setMaximumSize(zoomBox.getPreferredSize());
        // after size computation, because enabling editability increases preferred size
        zoomBox.setEditable(true);

        zoomBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox zoomBox = (JComboBox) e.getSource();
                // get and trim manual user input
                String string = zoomBox.getSelectedItem().toString().trim();

                // auto scaling
                if (string.equalsIgnoreCase("auto"))
                    recording.graphicsContext.setScaleToFit(true);

                // try to set scale factor
                else
                    try {
                        if (string.endsWith("%"))
                            string = string.substring(0, string.length() - 1).trim();
                        double value = Integer.parseInt(string);
                        // set scaling
                        recording.graphicsContext.setScaleToFit(false);
                        recording.graphicsContext.setScaleFactor(value / 100);
                        // adjust input field
                        zoomBox.setSelectedItem(string + "%");
                    } catch (NumberFormatException exception) {
                        // invalid user input - reset input field
                        Toolkit.getDefaultToolkit().beep();
                        if (recording.graphicsContext.isScaleToFit())
                            zoomBox.setSelectedIndex(0);
                        else
                            zoomBox.setSelectedItem(((int) (recording.graphicsContext.getScaleFactor() * 100)) + "%");
                    }

                // set focus back to main compinent
                recording.graphicsContext.requestFocusInWindow();
            }
        });

        // control panel
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        add(Box.createRigidArea(new Dimension(10, 0)));
        if (recording.desktop_replay_factor != 1) {
            add(syncMinusButton);
            add(syncButton);
            add(syncPlusButton);
        }
        add(playPauseButton);
        add(stopButton);
        add(Box.createRigidArea(new Dimension(10, 0)));
        add(previousButton);
        add(nextButton);
        add(Box.createRigidArea(new Dimension(10, 0)));
        add(timeSlider);
        add(Box.createRigidArea(new Dimension(10, 0)));
        add(timeLabel);
        add(Box.createRigidArea(new Dimension(5, 0)));
        add(durationLabel);
        add(Box.createRigidArea(new Dimension(20, 0)));
        add(zoomBox);
        add(Box.createRigidArea(new Dimension(10, 0)));
        add(indexAndVideoButton);
        add(Box.createRigidArea(new Dimension(10, 0)));
        add(fullscreenButton);
        add(Box.createRigidArea(new Dimension(10, 0)));
        add(volumeButton);
        add(Box.createRigidArea(new Dimension(10, 0)));

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // initialize icons
    private void createIcons() {
        // TODO: maybe: should be moved to constants

 
        // create icons
        playIcon = Constants.getIcon("Play24.gif");
        pauseIcon = Constants.getIcon("Pause24.gif");
        stopIcon = Constants.getIcon("Stop24.gif");
        nextIcon = Constants.getIcon("StepForward24.gif");
        previousIcon = Constants.getIcon("StepBack24.gif");
        volumeOnIcon = Constants.getIcon("Volume24.gif");
        volumeOffIcon = new NegatedImageIcon(Constants.getResourceUrl("Volume24.gif"));
        saveIcon = Constants.getIcon("Save24.gif");
        findIcon = Constants.getIcon("Find16.gif");
        movieIcon = Constants.getIcon("Movie24.gif");
    }

    // /////////////////////////////////////
    // KetListener
    // /////////////////////////////////////

    public void keyTyped(KeyEvent event) {
        switch (event.getKeyChar()) {
        // jump back in time
        case '1':
            recording.setTime(recording.getTime() - 300000);
            break;
        case '2':
            recording.setTime(recording.getTime() - 60000);
            break;
        case '3':
            recording.setTime(recording.getTime() - 30000);
            break;
        case '4':
            recording.setTime(recording.getTime() - 10000);
            break;
        case '5':
            recording.setTime(recording.getTime() - 5000);
            break;

        // skip some time
        case '6':
            recording.setTime(recording.getTime() + 5000);
            break;
        case '7':
            recording.setTime(recording.getTime() + 10000);
            break;
        case '8':
            recording.setTime(recording.getTime() + 30000);
            break;
        case '9':
            recording.setTime(recording.getTime() + 60000);
            break;
        case '0':
            recording.setTime(recording.getTime() + 300000);
            break;

        // switch pause/play
        case ' ':
            if (recording.paused())
                recording.play();
            else
                recording.pause();
            break;

        // previous/next index
        case '-':
            recording.previous();
            break;
        case '+':
            recording.next();
            break;

        // special to check end of recording
        case '*':
            recording.setTime(recording.getDuration() - 5000);
            break;

        // TODO: g = goto (open dialog and ask for time)

        default:
            break;
        }
    }

    public void keyPressed(KeyEvent event) {}

    public void keyReleased(KeyEvent event) {}
}
