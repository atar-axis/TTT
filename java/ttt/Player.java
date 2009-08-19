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
package ttt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import ttt.messages.MessageProducer;

public class Player extends JInternalFrame {

    ArrayList<Closeable> closeables = new ArrayList<Closeable>();;
    private RfbProtocol protocol = null;

    // simple player
    public Player(MessageProducer producer) throws IOException {

        GraphicsContext graphicsContext = new GraphicsContext(producer);

        // add display
        JPanel pane = new JPanel(new BorderLayout());
        final JScrollPane scrollPane = new JScrollPane(graphicsContext);
        pane.add(scrollPane, BorderLayout.CENTER);

        setTitle(graphicsContext.getProtocolPreferences().name);
        setContentPane(pane);

        // finish init
        initInternalFrame(graphicsContext);
    }

    // online player
    private JSplitPane splitPane;

    public Player(TTTConnection connection) throws IOException {
        // TODO: check if already connected - maybe shouldn't be done here
        connection.connect();
        closeables.add(connection);

        // NOTE: must be connected in order to set prefs
        final GraphicsContext graphicsContext = new GraphicsContext(connection);

        // TODO: allow user to set scale factor
        graphicsContext.setScaleToFit(true);

        // dragging component if zoomed
        graphicsContext.enableDraggingIfZoomed();

        // add desktop display
        final JScrollPane scrollPane = new JScrollPane(graphicsContext);

        JComponent pane;

        // add video display
        final Component video = connection.getVideoComponent();
        if (video != null) {
            JPanel videoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            videoPanel.add(video);
            videoPanel.setMinimumSize(video.getPreferredSize());

            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, videoPanel, scrollPane);
            splitPane.setOneTouchExpandable(true);
            pane = splitPane;

        } else {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(scrollPane, BorderLayout.CENTER);
            pane = panel;
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        pane = panel;

        final AbstractButton videoButton = new RollOverToggleButton(new ImageIcon(this.getClass().getResource(
                "resources/Movie24.gif")));
        if (video != null) {
            final JPanel videoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            videoPanel.add(video);
            videoPanel.setMinimumSize(video.getPreferredSize());
            panel.add(videoPanel, BorderLayout.WEST);

            videoButton.setToolTipText("turn on/off video");
            videoButton.setSelected(true);
            videoButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // TODO: fix nasty hack
                    videoPanel.setVisible(((AbstractButton) event.getSource()).isSelected());

                    // set size if not maximized
                    if (!isMaximum) {
                        Rectangle bounds = getBounds();
                        if (videoPanel.isVisible()) {
                            bounds.x -= videoPanel.getSize().width;
                            bounds.width += videoPanel.getSize().width;

                        } else {
                            bounds.width -= videoPanel.getSize().width;
                            bounds.x += videoPanel.getSize().width;
                        }
                        setBounds(bounds);
                    }
                }
            });

        }

        // ///////////////////////////////
        // controls
        // ///////////////////////////////

        // scaling
        final String[] zoomOptions = { "auto", "50%", "75%", "100%", "150%", "200%" };
        final JComboBox zoomBox = new JComboBox(zoomOptions);
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
                    graphicsContext.setScaleToFit(true);

                // try to set scale factor
                else
                    try {
                        if (string.endsWith("%"))
                            string = string.substring(0, string.length() - 1).trim();
                        double value = Integer.parseInt(string);
                        // set scaling
                        graphicsContext.setScaleToFit(false);
                        graphicsContext.setScaleFactor(value / 100);
                        // adjust input field
                        zoomBox.setSelectedItem(string + "%");
                    } catch (NumberFormatException exception) {
                        // invalid user input - reset input field
                        Toolkit.getDefaultToolkit().beep();
                        if (graphicsContext.isScaleToFit())
                            zoomBox.setSelectedIndex(0);
                        else
                            zoomBox.setSelectedItem(((int) (graphicsContext.getScaleFactor() * 100)) + "%");
                    }
            }
        });

        // fullscreen
        final AbstractButton fullscreenButton = new RollOverToggleButton(new ImageIcon(this.getClass().getResource(
                "resources/Fullscreen2_24.png")));
        fullscreenButton.setActionCommand(Recording.FULLSCREEN);
        fullscreenButton.setToolTipText("enable/disable fullscreen mode");
        fullscreenButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setFullscreen(((AbstractButton) event.getSource()).isSelected());
            }
        });

        // volume control
        URL urlVolume = this.getClass().getResource("resources/Volume24.gif");
        Icon volumeOnIcon = new ImageIcon(urlVolume);

        final AbstractButton volumeButton = new RollOverButton(volumeOnIcon, null);

        // control panel
        JPanel controlPanel = new GradientPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

        controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(zoomBox);
        controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        if (video != null) {
            controlPanel.add(videoButton);
            controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        }
        controlPanel.add(fullscreenButton);
        controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        controlPanel.add(volumeButton);
        controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));

        controlPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        pane.add(controlPanel, BorderLayout.SOUTH);

        // finish init
        setTitle(graphicsContext.getProtocolPreferences().name);
        setContentPane(pane);
        initInternalFrame(graphicsContext);

        final JPopupMenu menu = new JPopupMenu();

        // Create and add a menu item
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                zoomBox.setSelectedItem(event.getActionCommand().trim());
            }
        };
        ButtonGroup group = new ButtonGroup();
        JMenuItem item = new JMenuItem("Zoom Level:");
        menu.add(item);
        item = new JRadioButtonMenuItem("  50%");
        item.addActionListener(actionListener);
        menu.add(item);
        group.add(item);
        item = new JRadioButtonMenuItem("  75%");
        item.addActionListener(actionListener);
        menu.add(item);
        group.add(item);
        item = new JRadioButtonMenuItem("  100%");
        item.setSelected(true);
        item.addActionListener(actionListener);
        menu.add(item);
        group.add(item);
        item = new JRadioButtonMenuItem("  150%");
        item.addActionListener(actionListener);
        menu.add(item);
        group.add(item);
        item = new JRadioButtonMenuItem("  200%");
        item.addActionListener(actionListener);
        menu.add(item);
        group.add(item);
        item = new JRadioButtonMenuItem("  Auto");
        item.addActionListener(actionListener);
        menu.add(item);
        group.add(item);

        menu.addSeparator();

        final JMenuItem fullscreenMenuItem = new JRadioButtonMenuItem("Fullscreen");
        fullscreenMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                fullscreenButton.setSelected(!fullscreenButton.isSelected());
                fullscreenButton.doClick();
            }
        });
        menu.add(fullscreenMenuItem);

        final JMenuItem videoMenuItem = new JRadioButtonMenuItem("Video");
        if (video != null) {
            menu.addSeparator();
            videoMenuItem.setSelected(true);
            videoMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    videoButton.setSelected(!videoButton.isSelected());
                    videoButton.doClick();
                }
            });
            menu.add(videoMenuItem);
        }

        // Set the component to show the popup menu
        graphicsContext.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    videoMenuItem.setSelected(videoButton.isSelected());
                    fullscreenMenuItem.setSelected(fullscreenButton.isSelected());
                    menu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }

            public void mouseReleased(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    videoMenuItem.setSelected(videoButton.isSelected());
                    fullscreenMenuItem.setSelected(fullscreenButton.isSelected());
                    menu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }
        });
    }

    // File Playback
    public Player(String file) throws IOException {
        Recording recording = new Recording(file);
        closeables.add(recording);

        GraphicsContext graphicsContext = recording.getGraphicsContext();
        PlaybackControls playbackControls = recording.getPlaybackControls();

        // add display
        JPanel pane = new JPanel(new BorderLayout());
        final JScrollPane scrollPane = new JScrollPane(graphicsContext);
        pane.add(scrollPane, BorderLayout.CENTER);
        pane.add(playbackControls, BorderLayout.SOUTH);

        final JPanel videoAndIndexPanel = new JPanel(new BorderLayout());

        // TODO: fix nasty hack
        playbackControls.indexAndVideoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                videoAndIndexPanel.setVisible(!videoAndIndexPanel.isVisible());

                // set size if not maximized
                if (!isMaximum) {
                    Rectangle bounds = getBounds();
                    if (videoAndIndexPanel.isVisible()) {
                        bounds.x -= videoAndIndexPanel.getSize().width;
                        bounds.width += videoAndIndexPanel.getSize().width;

                    } else {
                        bounds.width -= videoAndIndexPanel.getSize().width;
                        bounds.x += videoAndIndexPanel.getSize().width;
                    }
                    setBounds(bounds);
                }
            }
        });

        // TODO: fix nasty hack
        playbackControls.fullscreenButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setFullscreen(((AbstractButton) event.getSource()).isSelected());
            }
        });

        // create video panel
        Component video = recording.getVideoComponent();
        if (video != null) {
            JPanel videoPanel = new JPanel();
            videoPanel.setBackground(Color.BLACK);
            videoPanel.add(video);
            videoAndIndexPanel.add(videoPanel, BorderLayout.NORTH);
        }

        // add thumbnail overview
        videoAndIndexPanel.add(recording.getIndexComponent(), BorderLayout.CENTER);
        pane.add(videoAndIndexPanel, BorderLayout.WEST);

        setTitle(graphicsContext.getProtocolPreferences().name);
        setContentPane(pane);

        // finish init
        initInternalFrame(graphicsContext);

        
        
        // start playback
        recording.play();
    }

    // Teacher Client (with Recorder)
    public Player(LectureProfile lectureProfile) throws IOException {
        // create connection and set color depth
        RFBConnection connection = new RFBConnection(InetAddress.getByName(lectureProfile.getHost()), lectureProfile
                .getPort(), null);
        // TODO: user input for color depth
        connection.getProtocolPreferences().setDepth(lectureProfile.getColorDepth());
        connection.connect(lectureProfile.isLoopbackRecorder());
        // set title (overwrite name given be server during connect)
        connection.getProtocolPreferences().name = lectureProfile.getTitle();

        // create protocol processing
        protocol = new RfbProtocol(connection);

        System.out.println(protocol.getProtocolPreferences());

        // add user input
        PaintListener listener = new PaintListener(protocol);
        protocol.addKeyListener(listener);
        protocol.addMouseListener(listener);
        protocol.addMouseMotionListener(listener);
        // MODMSG
        protocol.setPaintListener(listener);

        // add display
        JPanel pane = new JPanel(new BorderLayout());
        pane.add(new JScrollPane(protocol), BorderLayout.CENTER);
        setTitle(protocol.getProtocolPreferences().name);

        setContentPane(pane);

        initInternalFrame(protocol);

        // start reading messeges
        protocol.raw = false;
        protocol.start();

        // initialize recorder
        try {
            if (lectureProfile.isRecordEnabled()) {
                recorder = new Recorder(protocol, lectureProfile);
                closeables.add(recorder);
            }
        } catch (Exception e) {
            e.printStackTrace();
            recorder = null;
        }

        JPanel panel = new GradientPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder());

        // Paint Controls
        panel.add(new PaintControls(listener));

        panel.add(Box.createHorizontalGlue());

        if (recorder != null) {
            // Recorder Controls
            panel.add(recorder.getControls());
            panel.add(Box.createHorizontalGlue());
        }

        // Fullscreen Controls
        panel.add(getFullscreenButton());
        panel.add(Box.createRigidArea(new Dimension(20, 0)));

        panel.add(getReconnectButton(connection));
        panel.add(Box.createRigidArea(new Dimension(20, 0)));

        pane.add(panel, BorderLayout.NORTH);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        pack();

        // LOOPBACK RECORDER
        // show recording controls only
        if (recorder != null && lectureProfile.isLoopbackRecorder()) {
            pane = new GradientPanel();
            pane.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
            pane.add(recorder.getLoopbackControls());
            setContentPane(pane);
            pack();
        }
    }

    // TODO: rework
    private Container contentPane;

    public void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            // ensable fullscreen
            contentPane = getContentPane();
            setContentPane(new JPanel());
            setVisible(false);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(contentPane, BorderLayout.CENTER);
            panel.add(Box.createHorizontalGlue(), BorderLayout.NORTH);
            panel.add(Box.createHorizontalGlue(), BorderLayout.SOUTH);
            panel.add(Box.createVerticalGlue(), BorderLayout.WEST);
            panel.add(Box.createVerticalGlue(), BorderLayout.EAST);

            TTT.fullscreen(panel);

        } else if (contentPane != null) {
            // disable fullscreen
            TTT.fullscreen(null);
            setContentPane(contentPane);
            validate();
            setVisible(true);
        }
    }

    private AbstractButton fullscreenButton;

    private AbstractButton getFullscreenButton() {
        if (fullscreenButton != null)
            return fullscreenButton;

        final AbstractButton button = new RollOverToggleButton(new ImageIcon(this.getClass().getResource(
                "resources/Fullscreen2_24.png")));
        fullscreenButton = button;
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createLoweredBevelBorder());
        button.setToolTipText("enable/disable fullscreen mode");

        // avoid focus lost (of main component) in java 1.6
        button.setFocusable(false);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setFullscreen(button.isSelected());
            }
        });
        return button;
    }

    private Component getReconnectButton(final Connection connection) {
        final AbstractButton button = new RollOverButton(new ImageIcon(this.getClass().getResource(
                "resources/Refresh24.gif")));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createLoweredBevelBorder());
        button.setToolTipText("reconnect");

        // avoid focus lost (of main component) in java 1.6
        button.setFocusable(false);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // popup is not visible in fullscreen mode
                // hence return to wiondowed mode first
                TTT.leaveFfullscreen();

                if (JOptionPane.showInternalConfirmDialog(TTT.getInstance().getContentPane(), "Reconnect?", "TTT",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    connection.reconnect();
                button.repaint();
            }
        });
        return button;
    }

    public void close() {
        // close everything
        for (int i = 0; i < closeables.size(); i++)
            try {
                closeables.get(i).close();
            } catch (Exception e) {}
        closeables.clear();

        dispose();

        // explicitly run garbage collector - may free audio device on some systems (which stays blocked otherwise)
        // TODO: maybe run in thread?? Does this block?
        System.gc();
    }

    private void initInternalFrame(final GraphicsContext graphicsContext) {
        setResizable(true);
        setClosable(true);
        setMaximizable(true);
        setIconifiable(true);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosing(InternalFrameEvent arg0) {
                close();
            }

            public void internalFrameActivated(InternalFrameEvent event) {
                graphicsContext.requestFocusInWindow();
            }
        });

        // activate internal window under pointer to gain focus
        graphicsContext.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                // activate window to gain focus
                try {
                    graphicsContext.requestFocusInWindow();
                    setSelected(true);
                } catch (PropertyVetoException e) {}
            }
        });

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                super.componentResized(event);

                // avoid player frame to be larger than desktop
                Container parent = getParent();
                if (parent != null) {
                    Rectangle bounds = getBounds();
                    // adjust negative position
                    if (bounds.x < 0) {
                        bounds.width += bounds.x;
                        bounds.x = 0;
                    }
                    if (bounds.y < 0) {
                        bounds.height += bounds.y;
                        bounds.y = 0;
                    }

                    // respect desktop resolution
                    bounds.width = Math.min(bounds.width, parent.getSize().width - bounds.x);
                    bounds.height = Math.min(bounds.height, parent.getSize().height - bounds.y);
                    setBounds(bounds);
                }
            }
        });

        pack();
        setVisible(true);
    }

    Recorder recorder = null;

    public Recorder getRecorder() {
        return recorder;
    }
    
    //MODMSG
    /**
     * stores RFBProtocol instance for messaging
     */
    RfbProtocol getRfbProtocol() { return protocol; } 
}
