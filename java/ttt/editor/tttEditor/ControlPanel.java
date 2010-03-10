package ttt.editor.tttEditor;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.text.ParseException;

import javax.media.GainControl;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.metal.MetalSliderUI;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;



/**
 * Playback controls (play, pause, index increment, index decrement, timeline, adjust volume). This also contains a
 * <code>FineSeekPanel</code> internal class, which is used to select a playback point with millisecond accuracy, and
 * a <code>VolumeSlider</code> which can be used to control volume if the <code>PlaybackController</code> is able to
 * provide a <code>GainControl</code> object.<br>
 * A <code>MarkerList</code> and <code>Index</code> are required so that they may be displayed on the timeline; the
 * control panel also include options to add markers for convenience.<br>
 * A <code>DesktopPanel</code> is required for zoom.
 */
@SuppressWarnings("serial")
public class ControlPanel extends JPanel implements PlaybackEventListener {

    private PlaybackController playbackController;

    // JSlider used for time line
    private JSlider timeSlider;

    // used to distinguish between the time slider changing programmatically,
    // and changing because the user is dragging the thumb
    private boolean timerIncrementing = false;

    // JLabel for displaying the current playback time
    private JLabel timeLabel;
    // slider for the user to adjust the volume if there is audio available
    private VolumeSlider volumeSlider;

    // required for zoom
    private DesktopPanel desktopPanel;

    // required for setting markers
    private MarkerList markers;

    // buttons
    private JButton playButton, stopButton, volumeButton, nextButton, previousButton;

    // icons for buttons
    private final ImageIcon playIcon, pauseIcon, stopIcon, nextIcon, previousIcon, volumeIcon;

    // load icons
    private final URL urlPlay = this.getClass().getResource("resources/Play16.gif");
    private final URL urlPause = this.getClass().getResource("resources/Pause16.gif");
    private final URL urlStop = this.getClass().getResource("resources/Stop16.gif");
    private final URL urlNext = this.getClass().getResource("resources/StepForward16.gif");
    private final URL urlPrevious = this.getClass().getResource("resources/StepBack16.gif");
    private final URL urlVolume = this.getClass().getResource("resources/Volume16.gif");
    private final URL urlStart = this.getClass().getResource("resources/start_marker16.gif");
    private final URL urlDivide = this.getClass().getResource("resources/divide_marker16.gif");
    private final URL urlEnd = this.getClass().getResource("resources/end_marker16.gif");
    private final URL urlSeek = this.getClass().getResource("resources/Find16.gif");

    /**
     * Class constructor.
     * 
     * @param playbackController
     *            the <code>PlaybackController</code> to which to send instructions
     * @param markers
     *            the <code>MarkerList</code> used for editing the selected files
     * @param index
     *            the <code>Index</code> containing all the <code>Message</code>s
     * @param desktopPanel
     *            the <code>DesktopPanel</code> - necessary for setting zoom levels
     */
    public ControlPanel(PlaybackController playbackController, MarkerList markers, Index index,
            DesktopPanel desktopPanel) {

        this.playbackController = playbackController;
        this.desktopPanel = desktopPanel;
        this.markers = markers;
        this.gainControl = playbackController.getGainControls();

        playbackController.addPlaybackEventListener(this);

        playIcon = new ImageIcon(urlPlay);
        pauseIcon = new ImageIcon(urlPause);
        stopIcon = new ImageIcon(urlStop);
        nextIcon = new ImageIcon(urlNext);
        previousIcon = new ImageIcon(urlPrevious);
        volumeIcon = new ImageIcon(urlVolume);

        setLayout(new BorderLayout());

        Component timeline = getTimeline(markers, index);
        add(timeline, BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JPanel innerPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 0.5;
        c.weighty = 0.5;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;

        innerPanel.add(getGeneralControls(), c);

        if (gainControl != null) {
            c.gridx++;
            innerPanel.add(getVolumeControls(), c);
        }
        c.gridx++;
        innerPanel.add(getZoomControls(), c);
        c.gridx++;
        innerPanel.add(getEditControls(), c);

        controlsPanel.add(innerPanel);

        add(controlsPanel, BorderLayout.NORTH);
    }

    private Component getTimeline(MarkerList markers, Index index) {
        JPanel timeline = new JPanel(new BorderLayout());
        // amend if synchronizing players with different durations
        int duration = playbackController.getMaxPossibleDurationMS() / 1000;

        timeSlider = new JSlider();
        timeSlider.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int min = timeSlider.getMinimum();
                int max = timeSlider.getMaximum();
                // only set the timeSlider to the selected value if significantly different
                // from current value - don't want it being set if the user is only trying to
                // click on the thumb
                int newValue = min + ((max - min) * e.getX()) / timeSlider.getWidth();
                int difference = newValue - timeSlider.getValue();
                if (difference < 0)
                    difference = -difference;
                if (difference > 4)
                    timeSlider.setValue(min + ((max - min) * e.getX()) / timeSlider.getWidth());
            }
        });
        timeSlider.setMaximum(duration);
        timeSlider.setMajorTickSpacing(60);
        timeSlider.setMinorTickSpacing(10);
        timeSlider.setPaintTicks(true);
        timeSlider.setValue(0);

        timeSlider.setUI(new TimeSliderUI(markers, index));
        timeSlider.setPreferredSize(new Dimension(60, 60));

        timeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!timeSlider.getValueIsAdjusting() && !timerIncrementing) {
                    int mediaTimeMS = timeSlider.getValue() * 1000;
                    playbackController.setTimeAdjusted(mediaTimeMS);
                } else if (!timerIncrementing) {
                    int mediaTimeMS = timeSlider.getValue() * 1000;
                    timeLabel.setText(TTTEditor.getStringFromTime(mediaTimeMS));
                }
            }
        });
        timeline.add(timeSlider, BorderLayout.CENTER);

        return timeline;
    }

    private Component getGeneralControls() {
        FlowLayout flowLayout = new FlowLayout();
        JPanel controllerPanel = new JPanel(flowLayout);
        controllerPanel.setBorder(new CompoundBorder(new TitledBorder("Playback Controls"), emptyBorder));

        playButton = new JButton(playIcon);
        playButton.setToolTipText("Play");
        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playbackController.togglePlaying();
            }
        });
        playButton.setMnemonic(KeyEvent.VK_P);
        stopButton = new JButton(stopIcon);
        stopButton.setToolTipText("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playbackController.stopPlayer();
            }
        });

        nextButton = new JButton(nextIcon);
        nextButton.setToolTipText("Next slide");
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playbackController.incrementIndex();
            }
        });
        nextButton.setMnemonic(KeyEvent.VK_RIGHT);

        previousButton = new JButton(previousIcon);
        previousButton.setToolTipText("Previous slide");
        previousButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playbackController.decrementIndex();
            }
        });
        previousButton.setMnemonic(KeyEvent.VK_LEFT);

        flowLayout.setHgap(10);
        controllerPanel.add(playButton);
        controllerPanel.add(stopButton);
        controllerPanel.add(previousButton);
        controllerPanel.add(nextButton);

        flowLayout.setHgap(20);
        timeLabel = new JLabel();
        controllerPanel.add(timeLabel);
        ImageIcon seekIcon = new ImageIcon(urlSeek);
        JButton seekButton = new JButton(seekIcon);
        seekButton.setToolTipText("Seek");
        seekButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playbackController.setPaused();
                JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(), new FineSeekPanel(),
                        "Seek", JOptionPane.PLAIN_MESSAGE);
            }
        });
        flowLayout.setHgap(10);
        controllerPanel.add(seekButton);

        return controllerPanel;
    }

    // border used to create space around options
    private EmptyBorder emptyBorder = new EmptyBorder(0, 5, 4, 5);

    // controls for an audio Player, if available
    private GainControl gainControl;

    private Component getVolumeControls() {
        // create audio controls
        if (gainControl != null) {
            JPanel volumePanel = new JPanel();
            volumePanel.setBorder(new CompoundBorder(new TitledBorder("Volume"), emptyBorder));
            JButton volumeButton = createAudioButton(gainControl);
            JCheckBox muteBox = new JCheckBox("Mute");
            muteBox.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    JCheckBox checkBox = (JCheckBox) event.getSource();
                    gainControl.setMute(checkBox.isSelected());
                }
            });
            volumePanel.add(volumeButton);
            volumePanel.add(muteBox);
            return volumePanel;
        } 
            return null;
    }

    private JButton createAudioButton(GainControl gainControl) {
        volumeSlider = new VolumeSlider(gainControl);
        volumeButton = new JButton(volumeIcon);
        volumeButton.setToolTipText("Volume");
        volumeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeVolume();
            }
        });
        return volumeButton;
    }

    private Component getZoomControls() {
        JPanel zoomPanel = new JPanel();
        zoomPanel.setBorder(new CompoundBorder(new TitledBorder("Zoom"), emptyBorder));
        JComboBox zoomBox = new JComboBox(Parameters.zoomOptions);
        zoomBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox zoom = (JComboBox) e.getSource();
                desktopPanel.setZoomLevel(zoom.getSelectedIndex());
            }
        });
        zoomPanel.add(zoomBox);
        return zoomPanel;
    }

    private Component getEditControls() {
        JPanel editPanel = new JPanel();
        editPanel.setBorder(new CompoundBorder(new TitledBorder("Edit Markers"), emptyBorder));
        ImageIcon startIcon = new ImageIcon(urlStart);
        JButton trimStartButton = new JButton(startIcon);
        trimStartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                markers.setTrimStartTime(playbackController.getCurrentMediaTimeMS());
            }
        });
        trimStartButton.setToolTipText("Set the start marker to the current playback point");
        trimStartButton.setMnemonic(KeyEvent.VK_S);

        ImageIcon divideIcon = new ImageIcon(urlDivide);
        JButton divisionButton = new JButton(divideIcon);
        divisionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                markers.addDivision(playbackController.getCurrentMediaTimeMS());
            }
        });
        divisionButton.setToolTipText("Add a division marker at the current playback point");
        divisionButton.setMnemonic(KeyEvent.VK_D);

        ImageIcon endIcon = new ImageIcon(urlEnd);
        JButton trimEndButton = new JButton(endIcon);
        trimEndButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                markers.setTrimEnd(playbackController.getCurrentMediaTimeMS());
            }
        });
        trimEndButton.setToolTipText("Set the end marker to the current playback point");
        trimEndButton.setMnemonic(KeyEvent.VK_E);

        editPanel.add(trimStartButton);
        editPanel.add(divisionButton);
        editPanel.add(trimEndButton);
        return editPanel;
    }

    /**
     * Call repaint on the time line - necessary e.g. if the markers have changed.
     */
    public void refreshSlider() {
        timeSlider.setMaximum(playbackController.getMaxPossibleDurationMS() / 1000);
        timeSlider.setPaintTicks(Parameters.displayTicksOnTimeline);
        timeSlider.repaint();
    }

    /**
     * The volume slider should always be removed whenever the desktop viewer is being removed
     */
    protected void removeVolumeSlider() {
        if (volumeSlider != null)
            TTTEditor.getInstance().getDesktopPane().remove(volumeSlider);
    }

    private void changeVolume() {
        volumeSlider.showSlider();
    }

    public synchronized void setTime(int newMediaTimeMS) {
        if (!timeSlider.getValueIsAdjusting()) {
            timerIncrementing = true;
            int seconds = newMediaTimeMS / 1000;
            timeSlider.setValue(seconds);
            timeLabel.setText(TTTEditor.getStringFromTime(newMediaTimeMS));

            timerIncrementing = false;
        }
    }

    public void setIndex(int newIndex) {
    }

    public void setPlayStatus(boolean playing) {
        if (playing) {
            playButton.setIcon(pauseIcon);
        } else {
            playButton.setIcon(playIcon);
        }
    }

    public void thumbnailsGenerated() {
    }

    class TimeSliderUI extends MetalSliderUI implements IndexListener, ListDataListener {

        private MarkerList markers;
        private Index index;

        public TimeSliderUI(MarkerList markers, Index index) {
            this.markers = markers;
            this.index = index;
            index.addIndexListener(this);
            markers.addListDataListener(this);
        }

        public void paint(Graphics g, JComponent c) {

            recalculateIfInsetsChanged();
            recalculateIfOrientationChanged();
            Rectangle clip = g.getClipBounds();

            if (!clip.intersects(trackRect) && slider.getPaintTrack())
                calculateGeometry();

            if (slider.getPaintTrack() && clip.intersects(trackRect)) {
                paintTrack(g);
            }
            if (slider.getPaintTicks() && clip.intersects(tickRect)) {
                paintTicks(g);
            }
            if (slider.getPaintLabels() && clip.intersects(labelRect)) {
                paintLabels(g);
            }
            if (slider.hasFocus() && clip.intersects(focusRect)) {
                paintFocus(g);
            }

            if (Parameters.displayMarkersOnTimeline)
                maskDeadAreas(g);

            if (clip.intersects(thumbRect)) {
                paintThumb(g);
            }

            if (Parameters.displayIndexesOnTimeline)
                paintIndexes(g);
            if (Parameters.displayMarkersOnTimeline)
                paintMarkers(g);
        }

        private void maskDeadAreas(Graphics g) {
            Color previousColor = g.getColor();

            Color oldColor = slider.getBackground().darker();
            Color color = new Color(oldColor.getRed(), oldColor.getGreen(), oldColor.getBlue(), 150);
            g.setColor(color);

            int markerValue;
            int markerXPos;

            markerValue = markers.getTrimStartTime() / 1000;
            markerXPos = xPositionForValue(markerValue);
            g.fillRect(0, 0, markerXPos, slider.getHeight());

            markerValue = markers.getTrimEndTime() / 1000;
            markerXPos = xPositionForValue(markerValue);
            g.fillRect(markerXPos, 0, slider.getWidth() - markerXPos, slider.getHeight());

            g.setColor(previousColor);
        }

        private void paintMarkers(Graphics g) {
            Color previousColor = g.getColor();

            int markerValue;
            int markerXPos;

            int yPos = trackRect.y + trackRect.height;
            int markerHeight = 14;

            for (int i = 0; i < markers.getSize(); i++) {
                // make start/end markers stand out
                MarkerList.Marker marker = markers.getElementAt(i);
                if (marker.getMarkerType() == MarkerList.START_MARKER
                        || marker.getMarkerType() == MarkerList.END_MARKER)
                    g.setColor(Color.RED);
                else
                    g.setColor(Color.DARK_GRAY);

                markerValue = marker.getTimestamp() / 1000;
                markerXPos = xPositionForValue(markerValue);
                g.drawLine(markerXPos, yPos, markerXPos, yPos + markerHeight);
            }

            g.setColor(previousColor);
        }

        private void paintIndexes(Graphics g) {
            Color previousColor = g.getColor();

            int yPos = trackRect.y + trackRect.height;
            int indexHeight = 8;

            for (int i = 0; i < index.size(); i++) {
                g.setColor(Color.GRAY);
                int indexValue = index.get(i).getTimestamp() / 1000;
                int indexXPos = xPositionForValue(indexValue);
                g.drawLine(indexXPos, yPos, indexXPos, yPos + indexHeight);
            }

            g.setColor(previousColor);
        }

        public void indexEntryAdded(int index, IndexEntry entry) {
            slider.repaint();
        }

        public void indexEntryRemoved(int index) {
            slider.repaint();
        }

        public void intervalAdded(ListDataEvent event) {
            slider.repaint();
        }

        public void intervalRemoved(ListDataEvent event) {
            slider.repaint();
        }

        public void contentsChanged(ListDataEvent event) {
            slider.repaint();
        }

    }

    class FineSeekPanel extends JPanel {
        private JSpinner spinner;
        private SpinnerNumberModel spinnerModel;

        FineSeekPanel() {
            super(new BorderLayout());
            add(getFineControls(), BorderLayout.CENTER);
        }

        private Component getFineControls() {
            JPanel fineControlPanel = new JPanel(new BorderLayout());
            fineControlPanel.setBorder(new CompoundBorder(new TitledBorder("Input time"), new EmptyBorder(5, 5, 5, 5)));

            // modified by Ziewer 27.03.2006
            spinnerModel = new SpinnerNumberModel(playbackController.getCurrentMediaTimeMS(), 0, Math.max(
                    playbackController.getActualEndTime(), playbackController.getCurrentMediaTimeMS()), 1);
//            spinnerModel = new SpinnerNumberModel(playbackController.getCurrentMediaTimeMS(), 0, playbackController
//                    .getActualEndTime(), 1);
            spinner = new JSpinner(spinnerModel);
            spinner.setToolTipText("Time in milliseconds");

            spinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int ms = ((Number) spinner.getValue()).intValue();
                    playbackController.setTimeAdjusted(ms);
                }
            });

            NumberFormatter timeFormatter = new NumberFormatter() {
                public String valueToString(Object o) throws ParseException {
                    Integer integer = (Integer) o;
                    if (integer == null)
                        integer = new Integer(0);
                    return TTTEditor.getStringFromTime(integer.intValue(), true);
                }

                public Object stringToValue(String s) throws ParseException {
                    try {
                        int timeMS = TTTEditor.getTimeFromString(s);
                        return new Integer(timeMS);
                    } catch (Exception e) {
                        return null;
                    }
                }
            };
            JFormattedTextField textField = (JFormattedTextField) spinner.getEditor().getComponent(0);
            textField.setFormatterFactory(new DefaultFormatterFactory(timeFormatter));
            timeFormatter.setAllowsInvalid(true);
            timeFormatter.setCommitsOnValidEdit(false);
            textField.setValue(playbackController.getCurrentMediaTimeMS());

            JPanel topPanel = new JPanel(new BorderLayout());
            JLabel textLabel = new JLabel();
            textLabel.setText("Enter a time in ms, or in the following format:");
            JTextArea textArea = new JTextArea();
            textArea.setText("\tMM:SS");
            textArea.append("\n\tMM:SS.sss");
            textArea.setEditable(false);
            textArea.setOpaque(false);
            textArea.setHighlighter(null);
            topPanel.add(textLabel, BorderLayout.NORTH);
            topPanel.add(textArea, BorderLayout.CENTER);

            JPanel centerPanel = new JPanel();
            centerPanel.add(spinner);
            fineControlPanel.add(centerPanel, BorderLayout.CENTER);
            fineControlPanel.add(topPanel, BorderLayout.NORTH);

            return fineControlPanel;
        }

    }

    
    
    class VolumeSlider extends JInternalFrame implements ChangeListener {

        JSlider volumeLevelSlider;
        int maxVolume;
        JPanel volumePanel;
        JLabel volumeLabel;
        GainControl gainControl;
        JCheckBox holdBox;

        public VolumeSlider(GainControl gainControl, int maxVolume, int initialVolume) {
        	
                  volumeLevelSlider = new JSlider(JSlider.VERTICAL, 0, maxVolume, initialVolume);
            volumeLevelSlider.addChangeListener(this);
            volumeLevelSlider.setPreferredSize(new Dimension(20, 80));

            this.addInternalFrameListener(new InternalFrameAdapter() {
                public void internalFrameDeactivated(InternalFrameEvent e) {
                    if (!holdBox.isSelected())
                        setVisible(false);
                }
            });

            this.maxVolume = maxVolume;
            this.gainControl = gainControl;
            gainControl.setLevel(0.8f);//(float) initialVolume / maxVolume);
            
            holdBox = new JCheckBox("Hold");
            holdBox.setHorizontalAlignment(JCheckBox.CENTER);

            volumeLabel = new JLabel(" Vol: 100% ");
            volumeLabel.setHorizontalAlignment(JLabel.CENTER);
            volumePanel = new JPanel(new BorderLayout());
            volumePanel.add(volumeLabel, BorderLayout.NORTH);
            volumeLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            volumePanel.add(volumeLevelSlider, BorderLayout.CENTER);
            volumePanel.add(holdBox, BorderLayout.SOUTH);
            getContentPane().add(volumePanel);
            
            TTTEditor.getInstance().getDesktopPane().add(this);
            // high layer so stays on top
            TTTEditor.getInstance().getDesktopPane().setLayer(this, 10);

            pack();
        }

        //TODO there seems to be bug for the max value in the java media framework...
        public VolumeSlider(GainControl gainControl) {
            this(gainControl, 80, 80);
        }

        public void showSlider() {
            Point location = volumeButton.getLocationOnScreen();
            location.translate(50, -80);
            setLocation(location);
            setVisible(true);
        }

        public void stateChanged(ChangeEvent event) {
        	float fVolume = (float) maxVolume;
        	float fValue = (float) volumeLevelSlider.getValue();
        	float newlevel = fValue / fVolume;
        	newlevel=(newlevel>0.8f)?0.8f:newlevel; //the max setLevel value is 0.8f and not 1.0f
            gainControl.setLevel(newlevel);
            volumeLabel.setText("Vol: " + (int) (newlevel/0.8f*100) + "%");
        }

    }
}

