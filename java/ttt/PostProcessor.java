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
 * Created on 28.04.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

//
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// NOTE: This class is outdated - it contains the old post processing dialog
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//
public class PostProcessor extends JPanel {

    // recording
    private Recording recording;
    private boolean modified;
    private int script_creator_mode;

    // GUI
    private JTabbedPane tabbedPane;

    private PostProcessor() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Open", null, createOpenTab(), "select recording for post processing");
        tabbedPane.addTab("Info", null, createInfoTab(), "see and modify metadata");
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.addTab("Script", null, createScriptTab(), "create HTML Script");
        tabbedPane.setEnabledAt(2, false);
        tabbedPane.addTab("Full Text Search", null, createSearchbaseTab(),
                "read and integrate searchbase for full text search");
        tabbedPane.setEnabledAt(3, false);
        tabbedPane.addTab("Finish", null, createFinishTab(), "process and store modified recording");
        tabbedPane.setEnabledAt(4, false);

        // TODO: remove nasty hack - used to get focus on open file text field
        tabbedPane.setFocusable(false);

        add(tabbedPane);
    }

    private void close() {
        if (modified
                && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Store modified recording?",
                        "Closing Post Processing", JOptionPane.YES_NO_OPTION)) {
            store();
        }

        // close frame
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof JInternalFrame) {
                ((JInternalFrame) parent).dispose();
                break;
            } else if (parent instanceof JFrame) {
                ((JFrame) parent).dispose();
                break;
            } else {
                parent = parent.getParent();
            }
        }
    }

    private void store() {
        // ensure thumbnails are available
        if (!recording.thumbnailsAvailable())
            try {
                recording.createThumbnails();
                modified = true;
            } catch (IOException e) {
                TTT.showMessage("Thumbnail creation failed:\nError: " + e, "Thumbnail Creator Failure",
                        JOptionPane.ERROR_MESSAGE);
            }

        // store
        if (modified)
            recording.store();
        modified = false;
    }

    private void doScriptCreation() {
        // generate whatever was selected
        try {
            if (script_creator_mode != 0) {
                recording.createScript(script_creator_mode);

                // remember to store thumbs
                if ((script_creator_mode & ScriptCreator.THUMBNAILS) != 0)
                    modified = true;

                script_creator_mode = 0;
            }
        } catch (IOException e) {
            TTT
                    .showMessage("Script creation failed:\nError: " + e, "Script Creator Failure",
                            JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        createStandalonePostProcessor();
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // STEP 1: load recording
    // ////////////////////////////////////////////////////////////////////////////////////////////////////

    private JComponent createOpenTab() {
        // select file for postprocessing
        final JTextField fileTextField = new JTextField(TTT.userPrefs.get("last_opened_recording", "."), 40);

        // button for show file dialog
        JButton selectFileButton = new JButton("...");
        selectFileButton.setToolTipText("open file browser");

        // file input and file browser button
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        filePanel.add(fileTextField);
        filePanel.add(Box.createRigidArea(new Dimension(10, 0)));
        filePanel.add(selectFileButton);

        // packing GUI together
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createRigidArea(new Dimension(0, 9)));
        panel.add(new JLabel("Open TTT Recording:"));
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(filePanel);

        panel.add(Box.createRigidArea(new Dimension(0, 100)));
        JLabel label = new JLabel("Reads file for post processing.");
        label.setEnabled(false);
        panel.add(label);

        // action listener
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getActionCommand().equals("cancel")) {
                    close();
                } else
                    try {
                        recording = new Recording(fileTextField.getText());
                        modified = false;

                        // // create Thumbnails
                        // if (!recording.thumbnailsAvailable()) {
                        // recording.createThumbnails();
                        // modified = true;
                        // }

                        updateTabData();

                        // enable others
                        tabbedPane.setEnabledAt(1, true);
                        tabbedPane.setEnabledAt(2, true);
                        tabbedPane.setEnabledAt(3, true);
                        tabbedPane.setEnabledAt(4, true);
                        // goto next
                        tabbedPane.setSelectedIndex(1);
                    } catch (IOException e) {
                        TTT
                                .showMessage("Cannot open recording:\nError: " + e, "Open failed",
                                        JOptionPane.ERROR_MESSAGE);
                        tabbedPane.setEnabledAt(1, false);
                        tabbedPane.setEnabledAt(2, false);
                        tabbedPane.setEnabledAt(3, false);
                        tabbedPane.setEnabledAt(4, false);
                    }
            }
        };
        fileTextField.addActionListener(actionListener);
        // fileTextField.requestFocus(true);

        // action buttons
        final JButton[] buttons = new JButton[2];
        buttons[0] = new JButton("open");
        buttons[0].addActionListener(actionListener);
        buttons[1] = new JButton("cancel");
        buttons[1].addActionListener(actionListener);

        JComponent component = createTab(panel, buttons);

        selectFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                File file = TTT.showFileDialog();
                if (file != null)
                    try {
                        TTT.userPrefs.put("last_opened_recording", file.getCanonicalPath());
                        fileTextField.setText(file.getCanonicalPath());
                        buttons[0].doClick();
                    } catch (IOException e) {
                        System.out.println("File selection failed: " + e);
                    }
            }
        });

        return component;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // STEP 2: show metadata and check name
    // ////////////////////////////////////////////////////////////////////////////////////////////////////

    private JLabel titleLabel = new JLabel();
    private JTextField titleTextField = new JTextField(25);
    private JLabel timeLabel = new JLabel();
    private JLabel resolutionLabel = new JLabel();
    private JLabel versionLabel = new JLabel();
    private JLabel thumbnailsLabel = new JLabel("Thumbnails:");
    private JLabel fullTextSearchLabel = new JLabel("Full Text Search:");
    private JCheckBox thumbsCheckBox;

    private JComponent createInfoTab() {
        JPanel panel = new JPanel(new SpringLayout());
        panel.setOpaque(false);

        panel.add(new JLabel("Title:"));
        panel.add(titleLabel);
        panel.add(new JLabel("change Title:"));
        panel.add(titleTextField);

        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));

        panel.add(new JLabel("Version:"));
        panel.add(versionLabel);
        panel.add(new JLabel("Resolution:"));
        panel.add(resolutionLabel);
        panel.add(new JLabel("Date:"));
        panel.add(timeLabel);

        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));

        // thumbnail generation
        thumbsCheckBox = new JCheckBox();
        thumbsCheckBox.setToolTipText("create and store thumbnails for this recording.");
        thumbsCheckBox.setOpaque(false);

        panel.add(new JLabel("Thumbnails:"));
        panel.add(thumbnailsLabel);

        thumbsCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(thumbsCheckBox);
        JLabel label = new JLabel("create thumbnails");
        label.setToolTipText("create and store thumbnails for this recording.");
        // label.setEnabled(false);
        panel.add(label);

        // Lay out the panel.
        SpringUtilities.makeCompactGrid(panel, panel.getComponentCount() / 2, 2, // rows, cols
                8, 8, // initX, initY
                12, 8); // xPad, yPad

        // action listener
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getActionCommand().equals("skip"))
                    // reset
                    updateTabData();
                else {
                    // set new title
                    if (!titleTextField.getText().equals(recording.prefs.name)) {
                        recording.prefs.name = titleTextField.getText();
                        modified = true;
                    }

                    // create thumbnails
                    if (thumbsCheckBox.isSelected())
                        script_creator_mode |= ScriptCreator.THUMBNAILS;
                    else
                        script_creator_mode &= (ScriptCreator.HTML_SCRIPT | ScriptCreator.OCR_OPTIMIZED);
                }

                // goto next
                tabbedPane.setSelectedIndex(2);
            }
        };
        titleTextField.addActionListener(actionListener);

        // action buttons
        JButton[] buttons = new JButton[2];
        buttons[0] = new JButton("apply");
        buttons[0].addActionListener(actionListener);
        buttons[1] = new JButton("skip");
        buttons[1].addActionListener(actionListener);

        return createTab(panel, buttons);
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // STEP 3: create script & ocr input (& thumbnails)
    // ////////////////////////////////////////////////////////////////////////////////////////////////////

    private JComponent createScriptTab() {
        // HTML script generation
        final JCheckBox htmlScriptCheckBox = new JCheckBox("  create HTML Script  ", TTT.userPrefs.getBoolean(
                "post_process_html", true));
        htmlScriptCheckBox
                .setToolTipText("Creates a folder containing linked HTML pages incl. thumbnails and screenshots.");
        htmlScriptCheckBox.setOpaque(false);
        htmlScriptCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                TTT.userPrefs.putBoolean("post_process_html", ((JCheckBox) event.getSource()).isSelected());
            }
        });

        // packing GUI together
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(Box.createRigidArea(new Dimension(0, 9)));

        panel.add(new JLabel("HTML Script:"));

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        panel.add(htmlScriptCheckBox);

        panel.add(Box.createRigidArea(new Dimension(0, 100)));
        JLabel label = new JLabel("Creates a folder containing linked HTML pages");
        label.setEnabled(false);
        panel.add(label);
        label = new JLabel("incl. thumbnails and screenshots.");
        label.setEnabled(false);
        panel.add(label);

        // action buttons
        JButton[] buttons = new JButton[2];
        buttons[0] = new JButton("apply");
        buttons[0].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (htmlScriptCheckBox.isSelected())
                    script_creator_mode |= ScriptCreator.HTML_SCRIPT;
                else
                    script_creator_mode &= (ScriptCreator.THUMBNAILS | ScriptCreator.OCR_OPTIMIZED);
                tabbedPane.setSelectedIndex(3);
            }
        });
        buttons[1] = new JButton("skip");
        buttons[1].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                tabbedPane.setSelectedIndex(3);
            }
        });

        JComponent component = createTab(panel, buttons);
        htmlScriptCheckBox.setFocusable(false);
        return component;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // STEP 4: read search base
    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    private JTextField searchbaseTextField;
    private JCheckBox ocrCheckBox;

    private JComponent createSearchbaseTab() {
        // OCR input generation
        ocrCheckBox = new JCheckBox("  prepare full text search  ", TTT.userPrefs.getBoolean("post_process_ocr", true));
        ocrCheckBox
                .setToolTipText("Create a folder containing screenshots without annotations optimized for Optical Character Recognition (OCR)");
        ocrCheckBox.setOpaque(false);
        ocrCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                TTT.userPrefs.putBoolean("post_process_ocr", ((JCheckBox) event.getSource()).isSelected());
            }
        });

        JButton ocrButton = new JButton("create OCR input");
        ocrButton
                .setToolTipText("Create a folder containing screenshots without annotations optimized for Optical Character Recognition (OCR)");
        ocrButton.setOpaque(false);
        ocrButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                script_creator_mode |= ScriptCreator.OCR_OPTIMIZED;
                doScriptCreation();
            }
        });

        // select searchbase file
        searchbaseTextField = new JTextField("", 40);

        // button for show file dialog
        JButton selectFileButton = new JButton("...");
        selectFileButton.setToolTipText("open file browser");
        selectFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                try {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                        public boolean accept(File f) {
                            String fname = f.getName().toLowerCase();
                            // TODO; use Constant Endings Field
                            return fname.endsWith(".xml") || fname.endsWith(".txt") || f.isDirectory();
                        }

                        public String getDescription() {
                            return "XML or TXT searchbase";
                        }
                    });

                    // ask user
                    int returnVal = fileChooser.showOpenDialog(TTT.getInstance());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        searchbaseTextField.setText(fileChooser.getSelectedFile().getCanonicalPath());
                    }
                } catch (IOException e) {
                    System.out.println("File selection failed: " + e);
                }
            }
        });

        // file input and file browser button
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        filePanel.add(searchbaseTextField);
        filePanel.add(Box.createRigidArea(new Dimension(10, 0)));
        filePanel.add(selectFileButton);

        // packing GUI together
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel subPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subPanel.setOpaque(false);
        subPanel.add(new JLabel("Status:     "));
        subPanel.add(fullTextSearchLabel);
        fullTextSearchLabel.setForeground(Color.BLUE);

        panel.add(new JLabel("Full Text Search:     "));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(indentation(subPanel));

        panel.add(Box.createRigidArea(new Dimension(0, 50)));

        final JPanel stepsPanel = new JPanel();
        stepsPanel.setOpaque(false);
        stepsPanel.setLayout(new BoxLayout(stepsPanel, BoxLayout.Y_AXIS));

        // STEP 1
        stepsPanel.add(new JLabel("Step 1: Create input for Optical Character Recognition:"));
        stepsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        stepsPanel.add(indentation(ocrButton));

        stepsPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // STEP 3
        stepsPanel.add(new JLabel("Step 2: Optical Character Recognition ( OCR ):"));
        stepsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel label = new JLabel("1. run external OCR Software ( e.g. Omnipage )");
        // label.setEnabled(false);
        stepsPanel.add(indentation(label));
        label = new JLabel("2. apply OCR to screenshots created in [Step 1]");
        // label.setEnabled(false);
        stepsPanel.add(indentation(label));
        label = new JLabel("3. store recognized text as XML or TXT");
        // label.setEnabled(false);
        stepsPanel.add(indentation(label));

        stepsPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // STEP 3
        stepsPanel.add(new JLabel("Step 3: Read searchbase created in [Step 2]:"));
        stepsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        stepsPanel.add(indentation(filePanel));

        panel.add(stepsPanel);

        // action listener
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getActionCommand().equals("skip")) {
                    tabbedPane.setSelectedIndex(4);

                } else
                    try {
                        String filename = searchbaseTextField.getText();
                        if (!new File(filename).exists()) {
                            TTT.showMessage("File Not Found: " + filename, "Loading Searchbase failed",
                                    JOptionPane.ERROR_MESSAGE);
                        } else if (recording.readSearchbaseFromFile(filename)) {
                            modified = true;
                            tabbedPane.setSelectedIndex(4);
                        } else {
                            TTT.showMessage("Loading Searchbase failed: " + filename, "Loading Searchbase failed",
                                    JOptionPane.ERROR_MESSAGE);
                        }

                    } catch (Exception e) {
                        TTT.showMessage("Loading Searchbase failed:\nError: " + e, "Loading Searchbase failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
            }
        };
        searchbaseTextField.addActionListener(actionListener);

        // action buttons
        JButton[] buttons = new JButton[2];
        buttons[0] = new JButton("import searchbase");
        buttons[0].addActionListener(actionListener);
        buttons[1] = new JButton("skip");
        buttons[1].addActionListener(actionListener);

        ocrButton.setFocusable(false);
        selectFileButton.setFocusable(false);

        return createTab(panel, buttons);
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // STEP 5: store
    // ////////////////////////////////////////////////////////////////////////////////////////////////////

    private JComponent createFinishTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(Box.createRigidArea(new Dimension(0, 9)));
        panel.add(new JLabel("Process uncompleted tasks and store modified data."));

        JButton[] buttons = new JButton[2];
        buttons[0] = new JButton("finish");
        buttons[0].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                doScriptCreation();
                store();
                close();
            }
        });
        buttons[1] = new JButton("cancel");
        buttons[1].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                close();
            }
        });

        return createTab(panel, buttons);
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // STEP 6: zip
    // ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // STEP 7: release
    // ////////////////////////////////////////////////////////////////////////////////////////////////////

    // update tabs
    private void updateTabData() {
        if (recording != null) {
            // metadata
            titleLabel.setText(recording.prefs.name);
            titleTextField.setText(LectureProfile.getTitle(recording.prefs.starttime, recording.prefs.name));
            timeLabel.setText(new Date(recording.prefs.starttime).toString());
            resolutionLabel.setText(recording.prefs.framebufferWidth + " x " + recording.prefs.framebufferHeight
                    + " - " + recording.prefs.depth + " bit");
            versionLabel.setText(recording.prefs.versionMsg);

            // thumbnail creation
            thumbnailsLabel.setText(recording.thumbnailsAvailable() ? "available" : "not available");

            if (recording.thumbnailsAvailable()) {
                thumbsCheckBox.setEnabled(true);
                thumbsCheckBox.setSelected(false);
            } else {
                // force thumbnail creation
                thumbsCheckBox.setSelected(true);
                thumbsCheckBox.setEnabled(false);
            }

            // searchbase selection
            switch (recording.index.getSearchbaseFormat()) {
            case Index.XML_SEARCHBASE:
                fullTextSearchLabel.setText("searchable ( XML Searchbase )");
                break;
            case Index.ASCII_SEARCHBASE:
                fullTextSearchLabel.setText("searchable ( ASCII Searchbase )");
                ocrCheckBox.setSelected(true);
                break;
            default:
                fullTextSearchLabel.setText("not searchable");
                ocrCheckBox.setSelected(true);
                break;
            }

            try {
                searchbaseTextField.setText(Constants.getExistingFile(recording.fileDesktop.getCanonicalPath(),
                        Constants.SEARCHBASE_FILE).getCanonicalPath());
            } catch (Exception e) {
                try {
                    searchbaseTextField.setText(recording.getDirectory() + recording.getFileBase()
                            + Constants.searchbaseEndings[0]);
                } catch (Exception ee) {}
            }
        }
    }

    static JComponent createTab(JComponent component, JButton[] buttons) {
        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 40, 40));
        mainPanel.setOpaque(false);
        mainPanel.add(component);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);

        for (int i = 0; i < buttons.length; i++)
            buttonPanel.add(buttons[i]);

        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout());
        panel.add(mainPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    static Component indentation(Component component) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createRigidArea(new Dimension(30, 0)));
        panel.add(component);
        return panel;
    }

    public static JFrame createStandalonePostProcessor() {
        final PostProcessor postProcessor = new PostProcessor();
        final JFrame frame = new JFrame("TTT: Post Processing");
        frame.setContentPane(postProcessor);
        frame.pack();
        frame.setVisible(true);

        // TODO: change closing behaviour
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                postProcessor.close();
            }
        });
        return frame;
    }

    public static JInternalFrame createInternalPostProcessor() {
        final PostProcessor postProcessor = new PostProcessor();
        final JInternalFrame frame = new JInternalFrame("TTT: Post Processing");
        frame.setClosable(true);
        frame.setContentPane(postProcessor);
        frame.pack();
        frame.setVisible(true);

        // TODO: change closing behaviour
        frame.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent arg0) {
                postProcessor.close();
            }
        });
        return frame;
    }

    // //////////////////////////////////////////////////////
    // simple post processing dialog
    // //////////////////////////////////////////////////////

    // TODO: move to own class, because FINALs prevent GarbageCollection
    public static JInternalFrame createSimpleDialog(final Recording recording) {
        // store button
        final JButton storeButton = new JButton("Store");
        storeButton.setToolTipText("write modified data to ttt file");
        storeButton.setEnabled(false);
        final JLabel storeLabel = new JLabel("Store modified data");
        storeLabel.setForeground(Color.RED);
        storeLabel.setVisible(false);

        // helper class to encapsulate boolean
        class Modified {
            private boolean modified;

            boolean isModified() {
                return modified;
            }

            void setModified(boolean modified) {
                this.modified = modified;
                storeButton.setEnabled(modified);
                storeLabel.setVisible(modified);
            }

        }
        // remember to write if modified
        final Modified modified = new Modified();

        // frame
        final JInternalFrame frame = new JInternalFrame("TTT: Post Processing");

        // some defaults
        Border border = BorderFactory.createEtchedBorder(EtchedBorder.RAISED, Color.LIGHT_GRAY, Color.GRAY);
        Insets insets = new Insets(0, 10, 0, 10);

        // ////////////////////////////////////////////////
        // infomation about recording
        // ////////////////////////////////////////////////

        final JLabel titleLabel = new JLabel(recording.prefs.name);
        final JTextField titleTextField = new JTextField(LectureProfile.getTitle(recording.prefs.starttime,
                recording.prefs.name));
        if (!titleLabel.getText().equals(titleTextField.getText())) {
            titleTextField.setForeground(Color.RED);
            modified.setModified(true);
        }
        titleTextField.setToolTipText("enter new title");
        titleTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent event) {
                titleTextField.setForeground(Color.RED);
                modified.setModified(true);
            }

            public void removeUpdate(DocumentEvent event) {
                titleTextField.setForeground(Color.RED);
                modified.setModified(true);
            }

            public void insertUpdate(DocumentEvent event) {
                titleTextField.setForeground(Color.RED);
                modified.setModified(true);
            }
        });

        JPanel infoPanel = new JPanel(new SpringLayout());
        // titlePanel.setBorder(new MyTitledBorder(border, "Info"));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(new MyTitledBorder(border, "Info"), BorderFactory
                .createEmptyBorder(5, 20, 5, 20)));
        infoPanel.setOpaque(false);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(new JLabel("Title:"));
        infoPanel.add(titleLabel);
        infoPanel.add(new JLabel("change Title:"));
        infoPanel.add(titleTextField);
        infoPanel.add(new JLabel("Date:"));
        infoPanel.add(new JLabel(new Date(recording.prefs.starttime).toString()));
        infoPanel.add(new JLabel("Duration:"));
        infoPanel.add(new JLabel(Constants.getStringFromTime(recording.getDuration(), false) + " min"));
        infoPanel.add(new JLabel("Index:"));
        infoPanel.add(new JLabel(recording.index.size() + " pages"));
        infoPanel.add(new JLabel("Resolution:"));
        infoPanel.add(new JLabel(recording.prefs.framebufferWidth + " x " + recording.prefs.framebufferHeight + " - "
                + recording.prefs.depth + " bit"));

        // Lay out the panel.
        SpringUtilities.makeCompactGrid(infoPanel, infoPanel.getComponentCount() / 2, 2, // rows, cols
                8, 8, // initX, initY
                12, 8); // xPad, yPad

        // thumbnails
        final JLabel thumbnailsStatusLabel = new JLabel((recording.thumbnailsAvailable() ? "" : "not ") + "available");
        final JButton thumbnailsButton = new JButton("compute");
        thumbnailsButton.setMargin(insets);
        thumbnailsButton.setToolTipText("(re)compute thumbnails");
        thumbnailsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                // get out of event dispatching thread
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            recording.createScript(ScriptCreator.THUMBNAILS);
                            thumbnailsStatusLabel.setText((recording.thumbnailsAvailable() ? "" : "not ")
                                    + "computed ( but not stored yet )");
                            thumbnailsStatusLabel.setForeground(Color.red);
                            modified.setModified(true);
                        } catch (IOException e) {
                            TTT.showMessage("thumbnail creation failed: " + e);
                        }
                    }
                }).start();
            }
        });

        JPanel thumbnailsPanel = new JPanel(new SpringLayout());
        thumbnailsPanel.setOpaque(false);
        thumbnailsPanel.setBorder(BorderFactory.createCompoundBorder(new MyTitledBorder(border, "Thumbnails"),
                BorderFactory.createEmptyBorder(8, 20, -12, 20)));
        thumbnailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        thumbnailsPanel.add(new JLabel("Status:"));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.add(thumbnailsStatusLabel);
        panel.add(Box.createHorizontalGlue());
        panel.add(thumbnailsButton);

        thumbnailsPanel.add(panel);

        JLabel dummyLabel = new JLabel("change Title:"); // needed for same size as above
        dummyLabel.setVisible(false);
        thumbnailsPanel.add(dummyLabel);
        thumbnailsPanel.add(new JLabel());

        // Lay out the panel.
        SpringUtilities.makeCompactGrid(thumbnailsPanel, thumbnailsPanel.getComponentCount() / 2, 2, // rows, cols
                8, 8, // initX, initY
                12, 8); // xPad, yPad

        // ////////////////////////////////////////////////
        // full text search
        // ////////////////////////////////////////////////

        final JButton searchbaseHelpButton = new JButton("help");
        searchbaseHelpButton.setMargin(insets);
        searchbaseHelpButton.setToolTipText("help");
        searchbaseHelpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String line = "----------------------------------------------------------------------------\n";

                String infoText = line
                        + "How to add Full Text Search to a TTT recording?\n"
                        + line
                        + "\n"
                        + "In order to perform FULL TEXT SEARCH the TeleTeachingTool can import a SEARCHBASE,\n"
                        + "which easily can be created using external OPTICAL CHARACTER RECOGNITION ( OCR ) Software.\n\n\n";
                infoText += line + "How to create a SEARCHBASE?\n" + line + "\n"
                        + "The TeleTeachingTool offers the possibility to create SCREENSHOTS optimzed for OCR.\n"
                        + "Load all those SCREENSHOTS into your favourite OCR-Software,\n"
                        + "let it recognize the included text and store this text as XML or TXT file.\n\n\n";
                infoText += line
                        + "Which SEARCHBASE FORMATS are supported?\n"
                        + line
                        + "\n"
                        + "XML Searchbase:\n\n"
                        + "TTT can import XML files written by OmniPage Pro 14 Office ( no WordML ).\n"
                        + "This format contains coordinates, which allows not only to find pages including search results,\n"
                        + "but highlighting search results within each page.\n\n"
                        + "ASCII Searchbase (TXT files):\n\n"
                        + "TTT can import TXT files containing ASCII text with page breaks.\n"
                        + "Tested with ABBYY FineReader 7.0 ( remember to enable writing of page breaks ).\n"
                        + "As this format does not contain coordinates, results can not be highlighted within a page.\n"
                        + "However, the page can be found.\n";

                TTT.showMessage(infoText, "TTT: Full Text Search - Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        final JTextField searchbaseTextField = new JTextField("");
        try {
            searchbaseTextField.setText(recording.getDirectory() + recording.getFileBase()
                    + Constants.searchbaseEndings[0]);
        } catch (Exception e) {}

        // button for file dialog
        final JButton selectFileButton = new JButton("...");
        selectFileButton.setMargin(insets);
        selectFileButton.setToolTipText("open file browser");

        JButton searchbaseImportButton = new JButton("Import");
        searchbaseImportButton.setMargin(insets);
        searchbaseImportButton
                .setToolTipText("Import searchbase generated with external Optical Character Recognition (OCR) Software.");

        JPanel searchbaseFilePanel = new JPanel();
        searchbaseFilePanel.setLayout(new BoxLayout(searchbaseFilePanel, BoxLayout.X_AXIS));
        searchbaseFilePanel.setOpaque(false);
        searchbaseFilePanel.add(searchbaseTextField);
        searchbaseFilePanel.add(Box.createRigidArea(new Dimension(10, 0)));
        searchbaseFilePanel.add(selectFileButton);

        final JLabel searchbaseStatusLabel = new JLabel();

        switch (recording.index.getSearchbaseFormat()) {
        case Index.XML_SEARCHBASE:
            searchbaseStatusLabel.setText("searchable ( XML Searchbase )");
            break;
        case Index.ASCII_SEARCHBASE:
            searchbaseStatusLabel.setText("searchable ( ASCII Searchbase )");
            break;
        default:
            searchbaseStatusLabel.setText("not searchable");
            break;
        }

        if (recording.index.getSearchbaseFormat() != recording.index.getSearchbaseFormatStored()) {
            searchbaseStatusLabel.setForeground(Color.RED);
            modified.setModified(true);
        }

        // helper panel
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.add(searchbaseStatusLabel);
        panel.add(Box.createHorizontalGlue());
        panel.add(searchbaseHelpButton);

        JPanel searchbasePanel = new JPanel(new SpringLayout());
        searchbasePanel.setOpaque(false);
        searchbasePanel.setBorder(BorderFactory.createCompoundBorder(new MyTitledBorder(border, "Full Text Search"),
                BorderFactory.createEmptyBorder(8, 20, -12, 20)));
        // searchbasePanel.setLayout(new BoxLayout(searchbasePanel, BoxLayout.X_AXIS));
        searchbasePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchbasePanel.add(new JLabel("Status:"));
        searchbasePanel.add(panel);
        // empty label add a little distance between lines
        searchbasePanel.add(new JLabel());
        searchbasePanel.add(new JLabel());
        searchbasePanel.add(searchbaseImportButton);
        searchbasePanel.add(searchbaseFilePanel);
        // needed for same size as above
        dummyLabel = new JLabel("change Title:");
        dummyLabel.setVisible(false);
        searchbasePanel.add(dummyLabel);
        searchbasePanel.add(new JLabel());

        // Lay out the panel.
        SpringUtilities.makeCompactGrid(searchbasePanel, searchbasePanel.getComponentCount() / 2, 2, // rows, cols
                8, 8, // initX, initY
                12, 8); // xPad, yPad

        // listener
        ActionListener searchbaseActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // show file dialog
                if (event.getSource() == selectFileButton)
                    try {
                        JFileChooser fileChooser = new JFileChooser();

                        File lastRec = new File(searchbaseTextField.getText());
                        fileChooser.setSelectedFile(lastRec);

                        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                            public boolean accept(File f) {
                                String fname = f.getName().toLowerCase();
                                // TODO; use Constant Endings Field
                                return fname.endsWith(".xml") || fname.endsWith(".txt") || f.isDirectory();
                            }

                            public String getDescription() {
                                return "XML or TXT searchbase";
                            }
                        });

                        // ask user
                        int returnVal = fileChooser.showOpenDialog(TTT.getInstance());

                        // set file
                        if (returnVal == JFileChooser.APPROVE_OPTION)
                            searchbaseTextField.setText(fileChooser.getSelectedFile().getCanonicalPath());

                        // or cancel
                        else
                            return;
                    } catch (IOException e) {
                        System.out.println("File selection failed: " + e);
                        return;
                    }

                // load searchbase
                try {
                    String filename = searchbaseTextField.getText();
                    if (!new File(filename).exists()) {
                        TTT.showMessage("File Not Found: " + filename, "Loading Searchbase failed",
                                JOptionPane.ERROR_MESSAGE);
                    } else if (recording.readSearchbaseFromFile(filename)) {

                        switch (recording.index.getSearchbaseFormat()) {
                        case Index.XML_SEARCHBASE:
                            searchbaseStatusLabel.setText("searchable ( XML Searchbase )");
                            break;
                        case Index.ASCII_SEARCHBASE:
                            searchbaseStatusLabel.setText("searchable ( ASCII Searchbase )");
                            break;
                        default:
                            searchbaseStatusLabel.setText("not searchable");
                            break;
                        }

                        searchbaseStatusLabel.setForeground(Color.RED);
                        modified.setModified(true);
                    } else {
                        TTT.showMessage("Loading Searchbase failed: " + filename, "Loading Searchbase failed",
                                JOptionPane.ERROR_MESSAGE);
                    }

                } catch (Exception e) {
                    TTT.showMessage("Loading Searchbase failed:\nError: " + e, "Loading Searchbase failed",
                            JOptionPane.ERROR_MESSAGE);
                }

            }
        };
        selectFileButton.addActionListener(searchbaseActionListener);
        searchbaseImportButton.addActionListener(searchbaseActionListener);
        searchbaseTextField.addActionListener(searchbaseActionListener);

        // buttom buttons
        storeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (modified.isModified()) {
                    // set new title
                    recording.prefs.name = titleTextField.getText();
                    recording.store();

                    // reset colors
                    Color defaultColor = new JLabel().getForeground();
                    titleTextField.setForeground(defaultColor);
                    thumbnailsStatusLabel.setForeground(defaultColor);
                    searchbaseStatusLabel.setForeground(defaultColor);

                    titleLabel.setText(recording.prefs.name);
                    thumbnailsStatusLabel.setText((recording.thumbnailsAvailable() ? "" : "not ") + "available");
                }
                modified.setModified(false);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(storeLabel);
        buttonPanel.add(Box.createRigidArea(new Dimension(40, 0)));
        buttonPanel.add(storeButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(32, 0)));

        // ////////////////////////////////////////////////
        // script generator
        // ////////////////////////////////////////////////
        final JButton scriptHelpButton = new JButton("help");
        scriptHelpButton.setMargin(insets);
        scriptHelpButton.setToolTipText("help");
        scriptHelpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String line = "----------------------------------------------------------------------------\n";

                String infoText = line + "What is HTML/PDF Script?\n" + line + "\n"
                        + "The HTML Script offers a thumbnail overview and linked screenshots for all pages of the\n"
                        + "corresponding ttt file. The PDF Script contains the screenshot in one single document.\n"
                        + "These scripts include the annoations made by the teacher during the recording.\n\n\n";

                infoText += line
                        + "What is OCR Input?\n"
                        + line
                        + "\n"
                        + "Optical Character Recognition ( abbr. OCR ) is used to create a searchbase to enable\n"
                        + "Full Text Search for ttt recordings ( see Full Text Search Help ). The input for OCR software\n"
                        + "are screenshots of the pages of the corresponding ttt recoding. The screenshots of the\n"
                        + "corresponding HTML Script can be used. However, the special OCR input is optimized\n"
                        + "as it does not contain annotations, which may otherwise influence the recognition.\n\n\n";

                infoText += line
                        + "Where are the scripts stored?\n"
                        + line
                        + "\n"
                        + "The PDF script, html script and ocr input are stored in the same directory as the ttt recording:\n";
                try {
                    infoText += "    " + recording.getDirectory() + recording.getFileBase() + ".pdf\n" + "    "
                            + recording.getDirectory() + recording.getFileBase() + ".html\n" + "    "
                            + recording.getDirectory() + recording.getFileBase() + ".ocr\n";
                } catch (IOException e) {}
                infoText += "\n\n";

                TTT.showMessage(infoText, "TTT: Script Creator - Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // new version with HTML, PDF and OCR
        final JCheckBox htmlCheckBox = new JCheckBox("HTML - Linked HTML pages incl. thumbnails and screenshots");
        htmlCheckBox.setSelected(true);
        htmlCheckBox.setOpaque(false);
        htmlCheckBox.setMargin(new Insets(0, 20, 0, 0));
        final JCheckBox pdfCheckBox = new JCheckBox("PDF - one document with four screenshots per page");
        pdfCheckBox.setSelected(true);
        pdfCheckBox.setOpaque(false);
        pdfCheckBox.setMargin(new Insets(0, 20, 0, 0));
        final JCheckBox ocrCheckBox = new JCheckBox(
                "OCR - Screenshots optimized for Optical Character Recognition ( OCR )");
        ocrCheckBox.setSelected(true);
        ocrCheckBox.setOpaque(false);
        ocrCheckBox.setMargin(new Insets(0, 20, 0, 0));

        final JButton createScriptButton = new JButton("Create");
        createScriptButton.setMargin(insets);
        createScriptButton.setToolTipText("create selected script(s)");
        // listener creating script
        createScriptButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                // get out of event dispatching thread
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            // set new name
                            // TODO: don't set here - maybe ask to store instead
                            recording.prefs.name = titleTextField.getText();
                            int mode = 0;
                            if (htmlCheckBox.isSelected())
                                mode |= ScriptCreator.HTML_SCRIPT;
                            if (pdfCheckBox.isSelected())
                                mode |= ScriptCreator.PDF_SCRIPT;
                            if (ocrCheckBox.isSelected())
                                mode |= ScriptCreator.OCR_OPTIMIZED;
                            recording.createScript(mode);
                        } catch (IOException e) {
                            TTT.showMessage("script creation failed: " + e);
                        }
                    }
                }).start();
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(createScriptButton);
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(scriptHelpButton);

        JPanel scriptPanel = new JPanel(new SpringLayout());
        scriptPanel.setOpaque(false);
        scriptPanel.setBorder(BorderFactory.createCompoundBorder(new MyTitledBorder(border, "Script Generator"),
                BorderFactory.createEmptyBorder(8, 55, 8, 55)));
        scriptPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        scriptPanel.add(new JLabel());
        scriptPanel.add(buttonsPanel);
        scriptPanel.add(new JLabel());
        // scriptPanel.add(createScriptButton);
        scriptPanel.add(htmlCheckBox);
        scriptPanel.add(new JLabel());
        scriptPanel.add(pdfCheckBox);
        scriptPanel.add(new JLabel());
        // scriptPanel.add(scriptHelpButton);
        scriptPanel.add(ocrCheckBox);

        // Lay out the panel.
        SpringUtilities.makeCompactGrid(scriptPanel, scriptPanel.getComponentCount() / 2, 2, // rows, cols
                8, 8, // initX, initY
                12, 8); // xPad, yPad

        // ////////////////////////////////////////////////
        // flash converter
        // ////////////////////////////////////////////////

        final JButton flashHelpButton = new JButton("help");
        flashHelpButton.setMargin(insets);
        flashHelpButton.setToolTipText("help");
        flashHelpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String line = "----------------------------------------------------------------------------\n";

                String infoText = line;

                infoText = "Creates a corresponding Flash movie for a TTT recording.\n"
                        + "Supports Thumbnail overview and slide based navigation\n"
                        + "Teacher video, full text search and timeline navigation are not supported yet.\n\n"
                        + "Converting a TTT recording to a Flash movie may take several minutes.\n"
                        + "and consumes a lot of memory during conversation\n\n"
                        + "For some recordings it is necessary to convert the audio track,\n"
                        + "which also may take up to several minutes.\n\n"
                        + "Note that the status of the converter is still experimental\n"
                        + "and it may not work for all TTT recordings.\n\n"
                        + "Feel free to report your experiences in order to improve the converter.\n";
                TTT.showMessage(infoText, "TTT: Flash Converter - Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        final JButton createFlashButton = new JButton("Create");
        createFlashButton.setMargin(insets);
        createFlashButton.setToolTipText("convert to flash movie");
        // listener creating flash
        createFlashButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                // get out of event dispatching thread
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            recording.createFlash(false);
                        } catch (IOException e) {
                            TTT.showMessage("Flash creation failed: " + e);
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        JPanel buttonsPanel2 = new JPanel();
        buttonsPanel2.setLayout(new BoxLayout(buttonsPanel2, BoxLayout.X_AXIS));
        buttonsPanel2.setOpaque(false);
        buttonsPanel2.add(createFlashButton);
        buttonsPanel2.add(Box.createHorizontalGlue());
        buttonsPanel2.add(flashHelpButton);

        JPanel flashPanel = new JPanel(new SpringLayout());
        flashPanel.setOpaque(false);
        flashPanel.setBorder(BorderFactory.createCompoundBorder(new MyTitledBorder(border, "Flash Converter"),
                BorderFactory.createEmptyBorder(8, 55, 8, 55)));
        flashPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        flashPanel.add(new JLabel());
        flashPanel.add(buttonsPanel2);

        // Lay out the panel.
        SpringUtilities.makeCompactGrid(flashPanel, flashPanel.getComponentCount() / 2, 2, // rows, cols
                8, 8, // initX, initY
                12, 8); // xPad, yPad

        // ///////////////////////////////////////////
        // packing everything together
        // ///////////////////////////////////////////

        JPanel recordingPanel = new JPanel();
        recordingPanel.setOpaque(false);
        recordingPanel.setLayout(new BoxLayout(recordingPanel, BoxLayout.Y_AXIS));
        // recordingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        recordingPanel.setBorder(BorderFactory.createCompoundBorder(new MyTitledBorder(border, "TTT File: "
                + recording.fileDesktop.getName()), BorderFactory.createEmptyBorder(20, 35, 20, 35)));

        recordingPanel.add(infoPanel);
        recordingPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        recordingPanel.add(thumbnailsPanel);
        recordingPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        recordingPanel.add(searchbasePanel);
        recordingPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        recordingPanel.add(buttonPanel);

        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(recordingPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(scriptPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(flashPanel);

        // internal frame
        frame.setContentPane(mainPanel);
        frame.pack();
        frame.setVisible(true);

        // closing behaviour
        frame.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        frame.setClosable(true);
        frame.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosing(InternalFrameEvent event) {
                // close
                if (!modified.isModified()) {
                    frame.dispose();
                    recording.close();
                }

                // ask before closing
                else
                    switch (JOptionPane.showInternalConfirmDialog(TTT.getInstance().getContentPane(),
                            "Store modified recording?")) {
                    case JOptionPane.YES_OPTION:
                        storeButton.getActionListeners()[0].actionPerformed(null);
                    case JOptionPane.NO_OPTION:
                        frame.dispose();
                        recording.close();
                        break;
                    default: // cancel = do nothing
                    }
            }
        });

        // ensure thumbnails are available
        if (!recording.thumbnailsAvailable())
            try {
                recording.createThumbnails();
                thumbnailsStatusLabel.setText("computed ( but not stored yet )");
                thumbnailsStatusLabel.setForeground(Color.RED);
                modified.setModified(true);
            } catch (Exception e) {
                TTT.showMessage("Thumbnail creation failed: " + e, "TTT Post Processing", JOptionPane.ERROR_MESSAGE);
            }

        return frame;
    }
}
