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

package ttt.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ttt.Constants;
import ttt.record.Recording;
import ttt.record.TimeChangedListener;

public class IndexViewer extends JPanel implements TimeChangedListener {
    Recording recording;
    JTextField searchField;
    JTextField searchField2;
    JTabbedPane tabbedPane;
    IndexComponent indexComponent; // complete index
    IndexComponent searchIndexComponent; // index for search results

    public IndexViewer(Recording recording) {
        this.recording = recording;

        // thumbnail index
        indexComponent = new IndexComponent(recording.index.index, null);

        // thumbnail index for search results
        searchIndexComponent = new IndexComponent(recording.index.search_index, recording.index.index);

        // search fields and buttons
        // NOTE: there are to synchronized searchfields, one for each tab, because it looks better this way
        JButton searchButton = new JButton(Constants.getIcon("FindAgain24.gif"));
        searchButton.setMargin(new Insets(0, 0, 0, 0));
        searchField = new JTextField();

        JButton searchButton2 = new JButton(Constants.getIcon("FindAgain24.gif"));
        searchButton2.setMargin(new Insets(0, 0, 0, 0));
        searchField2 = new JTextField();

        // Listeners
        if (recording.index.getSearchbaseFormat() != Index.NO_SEARCHBASE) {
            searchButton.setToolTipText("find next");
            searchButton.addActionListener(searchIndexComponent);
            searchButton2.setToolTipText("find next");
            searchButton2.addActionListener(searchIndexComponent);
            searchField.setToolTipText("enter search string");
            searchField.addActionListener(searchIndexComponent);
            searchField.addCaretListener(searchIndexComponent);
            searchField2.setToolTipText("enter search string");
            searchField2.addActionListener(searchIndexComponent);
            searchField2.addCaretListener(searchIndexComponent);
        } else {
            searchButton.setEnabled(false);
            searchButton.setToolTipText("recording is not searchable");
            searchButton2.setEnabled(false);
            searchButton2.setToolTipText("recording is not searchable");
            searchField.setText("(not available)");
            searchField.setToolTipText("recording is not searchable");
            searchField.setEnabled(false);
            searchField2.setText("(not available)");
            searchField2.setToolTipText("recording is not searchable");
            searchField2.setEnabled(false);
        }

        // controlling the display of annotations within thumbnails
        final JCheckBox annotationsOn = new JCheckBox();
        // transparent background
        annotationsOn.setOpaque(false);
        annotationsOn.setBackground(new Color(0, 0, 0, 0));
        // no margin
        annotationsOn.setMargin(new Insets(0, 0, 0, 0));

        annotationsOn.setToolTipText("enable/disable annotations");
        annotationsOn.setIcon(Constants.getIcon("Freehand24_new.gif"));
        annotationsOn.setSelectedIcon(Constants.getIcon("Freehand24_new.gif"));
        annotationsOn.setRolloverIcon(Constants.getIcon("Freehand_rollover24_new.gif"));
        annotationsOn.setRolloverSelectedIcon(Constants.getIcon("Freehand_rollover24_new.gif"));
        annotationsOn.setSelected(true);

        final JCheckBox annotationsHighlightsOn = new JCheckBox();
        // transparent background
        annotationsHighlightsOn.setOpaque(false);
        annotationsHighlightsOn.setBackground(new Color(0, 0, 0, 0));
        // no margin
        annotationsHighlightsOn.setMargin(new Insets(0, 0, 0, 0));

        annotationsHighlightsOn.setToolTipText("enable/disable textmarker");
        annotationsHighlightsOn.setIcon(Constants.getIcon("Highlight24_new.gif"));
        annotationsHighlightsOn.setSelectedIcon(Constants.getIcon("Highlight24_new.gif"));
        annotationsHighlightsOn.setRolloverIcon(Constants.getIcon("Highlight_rollover24_new.gif"));
        annotationsHighlightsOn.setRolloverSelectedIcon(Constants.getIcon("Highlight_rollover24_new.gif"));

        annotationsHighlightsOn.setSelected(true);

        annotationsOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (((AbstractButton) event.getSource()).isSelected()) {
                    IndexViewer.this.recording.index
                            .setAnnotationsPaintMode(annotationsHighlightsOn.isSelected() ? Index.PAINT_ALL_ANNOTATIONS
                                    : Index.PAINT_NO_HIGHLIGHT_ANNOTATIONS);
                    annotationsHighlightsOn.setEnabled(true);
                } else {
                    IndexViewer.this.recording.index.setAnnotationsPaintMode(Index.PAINT_NO_ANNOTATIONS);
                    annotationsHighlightsOn.setEnabled(false);
                }
                IndexViewer.this.repaint();
            }
        });

        annotationsHighlightsOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (((AbstractButton) event.getSource()).isSelected())
                    IndexViewer.this.recording.index.setAnnotationsPaintMode(Index.PAINT_ALL_ANNOTATIONS);
                else
                    IndexViewer.this.recording.index.setAnnotationsPaintMode(Index.PAINT_NO_HIGHLIGHT_ANNOTATIONS);
                IndexViewer.this.repaint();
            }
        });

        JPanel annotationsControls = new GradientPanel();
        annotationsControls.add(annotationsOn);
        annotationsControls.add(annotationsHighlightsOn);

        // Layout
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField2, BorderLayout.CENTER);
        searchPanel.add(searchButton2, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(indexComponent, BorderLayout.CENTER);

        JPanel searchPanel2 = new JPanel(new BorderLayout());
        searchPanel2.add(searchField, BorderLayout.CENTER);
        searchPanel2.add(searchButton, BorderLayout.EAST);

        // TODO: create second search panel (must be synched)
        JPanel panel2 = new JPanel(new BorderLayout());
        panel2.add(searchPanel2, BorderLayout.NORTH);
        panel2.add(searchIndexComponent, BorderLayout.CENTER);

        tabbedPane = new JTabbedPane();
        tabbedPane.add("Index", panel);
        tabbedPane.add("Search Results", panel2);
        tabbedPane.setToolTipTextAt(0, "show complete index");
        tabbedPane.setToolTipTextAt(1, "show only search results");

        if (recording.index.getSearchbaseFormat() == Index.NO_SEARCHBASE) {
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setToolTipTextAt(1, "recording is not searchable");
        }

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        recording.index.setAnnotationsPaintMode(Index.PAINT_NO_HIGHLIGHT_ANNOTATIONS);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setVisible(true);

        // search index must contains at least one value to calculate correct size
        // TODO: nasty hack - find better solution
        recording.index.search_index.add(new IndexEntry(recording.index));

        // calculate size
        setPreferredSize(new Dimension((int) getPreferredSize().getWidth(), 210));

        // size calculation done - clear again
        recording.index.search_index.clear();

        final Recording rec = recording;
        // create thumbnails
        if (!recording.index.thumbnailsAvailable())
            try {
                System.out.println("create thumbnails");
                rec.createThumbnails();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    // event handling
    public void timeChanged(int event) {
        // check for index changed event
        if (event < 0 && event > -10000)
            setNowPlayingIndex(-event - 1);
    }

    // move scroller so that given index will be visible
    public void ensureIndexIsVisible(int index) {
        indexComponent.list.ensureIndexIsVisible(index);
    }

    // mark given index
    public void setNowPlayingIndex(int index) {
        // highlight active index
        indexComponent.indexListModel.setNowPlayingIndex(index);

        // TODO: this sometimes stucks if end of playback reached
        indexComponent.list.ensureIndexIsVisible(index);
        // indexComponent.indexListModel.update();

        // highlight corresponding search index
        searchIndexComponent.indexListModel.setNowPlayingIndex(index);
        int i = searchIndexComponent.indexListModel.getAbsoluteIndex(index);
        if (i != -1)
            searchIndexComponent.list.ensureIndexIsVisible(i);
    }

    class IndexComponent extends JScrollPane implements ListSelectionListener, CaretListener, ActionListener,
            MouseListener {
        private IndexListModel indexListModel;
        private JList list;

        // create scrollable thumbnail index
        // if second argument is given - the first list is a subset of the second and represents search results
        // in this case the second list ist needed to determine the absolute index number
        public IndexComponent(ArrayList index, ArrayList fullindex) {
            indexListModel = new IndexListModel(index, fullindex);
            list = new JList(indexListModel);
            list.setCellRenderer(indexListModel);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.addListSelectionListener(this);

            list.addMouseListener(this);
            setViewportView(list);
            // white background for everything
            Component[] components = getComponents();
            for (int i = 0; i < components.length; i++) {
                components[i].setBackground(Color.WHITE);
            }
        }

        // MouseListener
        public void mouseClicked(MouseEvent event) {}

        public void mouseEntered(MouseEvent event) {}

        public void mouseExited(MouseEvent event) {}

        public void mousePressed(MouseEvent event) {
            if (event.getButton() != MouseEvent.BUTTON1) {
                // TODO: create nicer output
                int index = ((JList) event.getSource()).locationToIndex(event.getPoint());
                // String searchable = indexArrayList.get(index).getSearchableText();
                String searchable = ((IndexEntry) indexListModel.getElementAt(index)).getSearchbase();
                System.out.println("Index " + index + ":\n" + searchable);
                JOptionPane.showMessageDialog(null, searchable, "Searchbase of index #" + (index + 1),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }

        public void mouseReleased(MouseEvent event) {}

        // index entry selected -> jump to index timestamp
        public void valueChanged(ListSelectionEvent event) {
            // Ignore extra messages.
            if (event.getValueIsAdjusting())
                return;

            JList list = (JList) event.getSource();
            if (!list.isSelectionEmpty()) {
                // jump to selected index entry
                int selectedRow = list.getMinSelectionIndex();
                recording.setTime((((IndexEntry) list.getModel().getElementAt(selectedRow))).getTimestamp());
            }
            list.clearSelection();
        }

        // perform find again
        public void actionPerformed(ActionEvent event) {
            // search next result
            int time = recording.getTime();
            for (int i = 0; i < recording.index.search_index.size(); i++) {
                // found - set player to index with next search result
                if (recording.index.search_index.get(i).getTimestamp() > time) {
                    recording.setTime(recording.index.search_index.get(i).getTimestamp());
                    break;
                }
                // reached end of recording - start from beginning
                if (i == recording.index.search_index.size() - 1)
                    recording.setTime(recording.index.search_index.get(0).getTimestamp());
            }
        }

        // keep last search string - perform search only if needed
        private String lastSearchedText;

        // distinguishes user input from text field updates caused by program
        private boolean ignoreCareEvent;

        public void caretUpdate(CaretEvent event) {
            if (ignoreCareEvent)
                return;

            JTextField source = (JTextField) event.getSource();
            JTextField target = source == searchField ? searchField2 : searchField;

            // update parallel text field
            ignoreCareEvent = true;

            // set text
            target.setText(source.getText());

            // set selected text and caret position

            // no selection - only caret position
            if (source.getSelectionStart() == source.getSelectionEnd())
                target.setCaretPosition(source.getCaretPosition());

            // left to right selection
            else if (source.getSelectionEnd() == source.getCaretPosition())
                target.select(source.getSelectionStart(), source.getSelectionEnd());

            // right to left selection
            else {
                target.setCaretPosition(source.getSelectionEnd());
                target.moveCaretPosition(source.getCaretPosition());
            }

            ignoreCareEvent = false;

            // check if search text was modified
            String searchText = source.getText();
            if (searchText.equals(lastSearchedText))
                return;
            lastSearchedText = searchText;

            // always present results in search tab
            tabbedPane.setSelectedIndex(1);

            // perform search
            recording.index.search(searchText);

            // force repaint
            indexListModel.update();
        }
    }
}