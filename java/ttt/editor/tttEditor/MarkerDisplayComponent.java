package ttt.editor.tttEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A <code>JComponent</code> to display <code>Marker</code>s and allow the user to edit them.
 */
public class MarkerDisplayComponent extends JPanel implements ListCellRenderer, ListDataListener, PlaybackEventListener {

    private MarkerList markerList;
    private JList list;
    private PlaybackController playbackController;

    // JSpinner used for fine tuning of markers
    private JSpinner spinner;
    private SpinnerNumberModel spinnerModel;

    private JButton deleteButton;

    private final URL urlStart = this.getClass().getResource("resources/start_marker16.gif");
    private final URL urlDivide = this.getClass().getResource("resources/divide_marker16.gif");
    private final URL urlEnd = this.getClass().getResource("resources/end_marker16.gif");
    private final URL urlDelete = this.getClass().getResource("resources/delete_marker16.gif");

    /**
     * Class constructor.
     * 
     * @param markers
     *            the <code>MarkerList</code> to use
     * @param playbackControl
     *            the <code>PlaybackController</code> to interact with
     */
    public MarkerDisplayComponent(final MarkerList markers, PlaybackController playbackControl) {
        super(new BorderLayout());
        this.markerList = markers;
        this.playbackController = playbackControl;

        markerList.addListDataListener(this);
        playbackController.addPlaybackEventListener(this);

        list = new JList(markerList);
        list.setCellRenderer(this);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                // nothing has beens selected
                if (list.getSelectedIndex() < 0) {
                    spinner.setEnabled(false);
                    deleteButton.setEnabled(false);
                    return;
                }

                int markerType = ((MarkerList.Marker) list.getSelectedValue()).getMarkerType();

                // don't allow deletion of start/end times
                if (markerType == MarkerList.START_MARKER || markerType == MarkerList.END_MARKER)
                    deleteButton.setEnabled(false);
                else
                    deleteButton.setEnabled(true);

                spinner.setEnabled(true);
                int timeMS = ((MarkerList.Marker) list.getSelectedValue()).getTimestamp();
                if (list.getSelectedIndex() == 0)
                    spinnerModel.setMinimum(new Integer(0));
                else {
                    Object temp = list.getModel().getElementAt(list.getSelectedIndex() - 1);
                    int min = ((MarkerList.Marker) temp).getTimestamp();
                    spinnerModel.setMinimum(new Integer(min + 500));
                }
                if (list.getSelectedIndex() == list.getModel().getSize() - 1)
                    spinnerModel.setMaximum(new Integer(playbackController.getMaxPossibleDurationMS()));
                else {
                    Object temp = list.getModel().getElementAt(list.getSelectedIndex() + 1);
                    int max = ((MarkerList.Marker) temp).getTimestamp();
                    spinnerModel.setMaximum(new Integer(max - 500));
                }
                spinner.setValue(new Integer(timeMS));
            }
        });

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(list);

        spinnerModel = new SpinnerNumberModel();
        spinner = new JSpinner(spinnerModel);        
        spinner.setToolTipText("Selected marker time (ms)");
        spinner.setEnabled(false);
        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                int ms = ((Number) spinner.getValue()).intValue();
                Object object = list.getSelectedValue();
                if (object instanceof MarkerList.Marker) {
                    MarkerList.Marker marker = (MarkerList.Marker) object;
                    if (marker.getTimestamp() != ms) {
                        // added by Ziewer 14.06.2006
                        if (marker.getMarkerType() == MarkerList.START_MARKER)
                            playbackController.setActualStartTime(ms);
                        else if (marker.getMarkerType() == MarkerList.END_MARKER)
                            playbackController.setActualEndTime(ms);
                        // added end

                        marker.setTimestamp(ms);
                        playbackController.setTimeAdjusted(ms);
                    }
                }
            }
        });

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 8, 8));
        buttonPanel.setBorder(new EmptyBorder(0, 4, 4, 4));
        ImageIcon startIcon = new ImageIcon(urlStart);
        JButton trimStartButton = new JButton(startIcon);
        trimStartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                markerList.setTrimStartTime(playbackController.getCurrentMediaTimeMS());
            }
        });
        trimStartButton.setToolTipText("Set the start marker to the current playback point");

        ImageIcon divideIcon = new ImageIcon(urlDivide);
        JButton divisionButton = new JButton(divideIcon);
        divisionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                markerList.addDivision(playbackController.getCurrentMediaTimeMS());
            }
        });
        divisionButton.setToolTipText("Add a division marker at the current playback point");

        ImageIcon endIcon = new ImageIcon(urlEnd);
        JButton trimEndButton = new JButton(endIcon);
        trimEndButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                markerList.setTrimEnd(playbackController.getCurrentMediaTimeMS());
            }
        });
        trimEndButton.setToolTipText("Set the end marker to the current playback point");

        ImageIcon deleteIcon = new ImageIcon(urlDelete);
        deleteButton = new JButton(deleteIcon);
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                markerList.remove(list.getSelectedIndex());
            }
        });
        deleteButton.setToolTipText("Delete the currently selected marker");
        deleteButton.setEnabled(false);

        buttonPanel.add(trimStartButton);
        buttonPanel.add(divisionButton);
        buttonPanel.add(trimEndButton);
        buttonPanel.add(deleteButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        JLabel tuneLabel = new JLabel("Fine tune (ms):  ");
        topPanel.add(buttonPanel, BorderLayout.NORTH);
        topPanel.add(tuneLabel, BorderLayout.WEST);
        topPanel.add(spinner, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void intervalAdded(ListDataEvent e) {
        repaint();
    }

    public void intervalRemoved(ListDataEvent e) {
        repaint();
    }

    public void contentsChanged(ListDataEvent e) {
        repaint();
    }

    public void setIndex(int newIndex) {}

    public void setTime(int newMediaTimeMS) {}

    public void thumbnailsGenerated() {}

    public void setPlayStatus(boolean status) {
        if (!status)
            spinner.setEnabled(false);
        else {
            // don't enable spinner if no index is selected
            if (list.getSelectedIndex() != -1)
                spinner.setEnabled(true);
        }
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {

        MarkerList.Marker marker = ((MarkerList) list.getModel()).getElementAt(index);
        int timestamp = marker.getTimestamp();

        JPanel panel = new JPanel(new GridLayout(1, 3));
        JLabel label = new JLabel();

        if (marker.getMarkerType() == MarkerList.START_MARKER)
            label.setText("Start time:");
        else if (marker.getMarkerType() == MarkerList.END_MARKER)
            label.setText("End time:");
        else if (marker.getMarkerType() == MarkerList.DIVIDE_MARKER)
            label.setText("Division time:");

        JLabel formattedTimeLabel = new JLabel(TTTEditor.getStringFromTime(timestamp));
        JLabel timeLabel = new JLabel(timestamp + " ms");

        label.setOpaque(true);
        label.setEnabled(list.isEnabled());
        timeLabel.setOpaque(true);
        timeLabel.setEnabled(list.isEnabled());
        formattedTimeLabel.setOpaque(true);
        formattedTimeLabel.setEnabled(list.isEnabled());
        if (isSelected) {
            label.setBackground(list.getSelectionBackground());
            timeLabel.setBackground(list.getSelectionBackground());
            formattedTimeLabel.setBackground(list.getSelectionBackground());
        } else {
            label.setBackground(list.getBackground());
            timeLabel.setBackground(list.getBackground());
            formattedTimeLabel.setBackground(list.getBackground());
        }
        if (marker.isMarkerLegal()) {
            label.setForeground(list.getSelectionForeground());
            timeLabel.setForeground(list.getSelectionForeground());
            formattedTimeLabel.setForeground(list.getSelectionForeground());
        } else {
            label.setForeground(Color.RED);
            timeLabel.setForeground(Color.RED);
            formattedTimeLabel.setForeground(Color.RED);
        }

        panel.add(label);
        panel.add(timeLabel);
        panel.add(formattedTimeLabel);

        return panel;
    }

}