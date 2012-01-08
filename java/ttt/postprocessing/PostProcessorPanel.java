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
 * PostProcessorPanel.java
 *
 * Created on 8. M�rz 2007, 13:03
 */

package ttt.postprocessing;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;

import ttt.Constants;
import ttt.TTT;
import ttt.audio.LameEncoder;
import ttt.audio.MP3Converter;
import ttt.gui.GradientPanel;
import ttt.gui.Index;
import ttt.helper.LibraryChecker;
import ttt.postprocessing.flash.FlashContext;
import ttt.postprocessing.podcast.PodcastCreator;
import ttt.record.Recording;

/**
 * 
 * @author ziewer
 */
public class PostProcessorPanel extends GradientPanel {
    // NOTE: post processing dialog is designed with NetBeans and requires Swing Layout Extension Library
    // TODO: redesign without Swing Layout Extension Library
	
    // corresponding recording
    private Recording recording;
    // states if recording has modifications which have not been stored yet
    private boolean modified;

    // batch mode flag
    private boolean batch = false;
    
    //saves the enable status of some components. See setEnabled(boolean)
    private HashMap<Object, Boolean> ctrlSatus = new HashMap<Object, Boolean>();
    
    /** Creates new form PostProcessorPanel */
    public PostProcessorPanel(Recording recording) throws IOException {
        this(recording, false);
    }

    public PostProcessorPanel(Recording recording, boolean batch) throws IOException {
        this.recording = recording;
        this.batch = batch;

        // create GUI
        initComponents();
 
        // set previously used fields
        userField.setText(TTT.userPrefs.get("publish_user", "<enter user name>"));
        serverField.setText(TTT.userPrefs.get("publish_server", "ttt.in.tum.de"));
        pathField.setText(TTT.userPrefs.get("publish_path", "www/recordings/"));

        // update status
        updateStatusFields();
      
        // set tooltips
        titleField.setToolTipText("edit the title - consistent naming is advisable");
        thumbnailsCheckBox.setToolTipText("check to generate thumbnail overview for playback");
        htmlCheckBox.setToolTipText("generate a script in html format");
        if (LibraryChecker.isITextPdfLibraryInstalled())
            pdfCheckBox.setToolTipText("generate a script as a single pdf file");
        else {
            pdfCheckBox.setToolTipText("iText PDF Library not found - cannot generate pdf scripts");
            pdfCheckBox.setSelected(false);
            pdfCheckBox.setEnabled(false);
        }
      
        ocrCheckBox
                .setToolTipText("generate optimized input for optical character recognition (see full text search help)");        
        //the conversion of a audio file to a mp3 file is only accessible if lame is found and if there exists a wav or mp2 file of the recording 
        if (LameEncoder.isLameAvailable() && recording.getExistingFileBySuffix(new String[] {"wav","mp2"}).exists()) {
        	mp3CheckBox.setToolTipText("generate a mp3 audio file from wav or mp2 file");
        } else {
        	if (LameEncoder.isLameAvailable()) {
        		mp3CheckBox.setToolTipText("audio file not found");
        	} else {
        		mp3CheckBox.setToolTipText("lame not found");
        	}
        	mp3CheckBox.setSelected(false);
        	mp3CheckBox.setEnabled(false);
        } 
        if (FlashContext.isCreationPossible(recording)) {
        	flashCheckBox.setToolTipText("generate a flash/swf version of this recording");
        	if (recording.getExistingFileBySuffix("mp3").exists() == false) {
        		flashCheckBox.addItemListener(new ItemListener() {
        			public void itemStateChanged(ItemEvent event) {
        				try {
        					//if there is no mp3 file, a mp3 file must be created before creating the flash movie
	        				if (flashCheckBox.isSelected() && PostProcessorPanel.this.recording.getExistingFileBySuffix("mp3").exists() == false) {
	        					mp3CheckBox.setSelected(true);
	        				}
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
        			}
        		});
        	}
        } else {
        	flashCheckBox.setToolTipText("audio file not found");
        	flashCheckBox.setSelected(false);
        	flashCheckBox.setEnabled(false);
        }
        if (PodcastCreator.isCreationPossible(recording)) {
        	mp4CheckBox.setToolTipText("generate a mp4 podcast of this recording");
        } else {
        	mp4CheckBox.setToolTipText("generating a mp4 podcast requires ffmpeg, mp4box, and an audio file");
        	mp4CheckBox.setSelected(false);
        	mp4CheckBox.setEnabled(false);
        }
        if (recording.getExistingFileBySuffix("bjpg").exists()) {
        	camCheckBox.setToolTipText("generating a CamVid file");
        } else {
        	camCheckBox.setToolTipText("bjpg file not found");
        	camCheckBox.setSelected(false);
        	camCheckBox.setEnabled(false);
        }
        createButton.setToolTipText("generate the selected components");
    
        searchFilenameField.setToolTipText("specify searchbase file (XML or ASCII)");
        openSearchbaseFileDialogButton.setToolTipText("open file dialog");
        importSearchbaseButton
                .setToolTipText("Import searchbase generated with external Optical Character Recognition (OCR) Software");

        if (LibraryChecker.isJSchInstalled()) {
            userField.setToolTipText("specify the user name");
            serverField.setToolTipText("specify the name of the file server");
            pathField.setToolTipText("specify the path to the basic file folder");
            publishButton.setToolTipText("copy recording and additional files to the specified file server");
        } else {
            userField.setToolTipText("JSch Library for ssh/sftp is not installed - cannot copy data to file server");
            userField.setEnabled(false);
            serverField.setToolTipText("JSch Library for ssh/sftp is not installed - cannot copy data to file server");
            serverField.setEnabled(false);
            pathField.setToolTipText("JSch Library for ssh/sftp is not installed - cannot copy data to file server");
            pathField.setEnabled(false);
            publishButton
                    .setToolTipText("JSch Library for ssh/sftp is not installed - cannot copy data to file server");
            publishButton.setEnabled(false);

        }

        doneButton.setToolTipText("close this dialog");

        createButton.setForeground(Color.BLUE);


        /* The conversion of wave files to mp3 files on startup becomes obsolete as recordings generate wave files by default and users can generate mp3 files via the post-processing dialog afterwards.
         * Disabled after adding the mp3 conversion feature via lame.    

        if (Constants.getExistingFile(recording.fileDesktop.getCanonicalPath(), Constants.AUDIO_FILE)
                .getCanonicalPath().toLowerCase().endsWith(".wav")) {

            final JLabel endnote = new JLabel("");

            // show progress
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    JDialog dialog = new JDialog(TTT.getInstance(), "Audio Converter");
                    JLabel progress = new JLabel("Converting audio from WAV to MP3. \nPlease wait ---");
                    int count = 0;
                    dialog.getContentPane().add(progress);
                    dialog.pack();
                    dialog.setVisible(true);

                    while (!endnote.getText().equals("done")) {
                        try {
                            Thread.sleep(500);
                            switch ((count++) % 4) {
                            case 0:
                                progress.setText("Converting audio from WAV to MP3. Please wait - \\");
                                break;
                            case 1:
                                progress.setText("Converting audio from WAV to MP3. Please wait - |");
                                break;
                            case 2:
                                progress.setText("Converting audio from WAV to MP3. Please wait - /");
                                break;
                            case 3:
                                progress.setText("Converting audio from WAV to MP3. Please wait - -");
                                break;
                            }

                        } catch (InterruptedException e) {}
                    }
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });
            // TODO: remove hack - procesing should be independent of GUI
            if (!batch)
                thread.start();

            // convert
            MP3Converter.checkAndConvertAudioFile(recording, AudioFormat.MPEGLAYER3);
            endnote.setText("done");

            // update status
            updateStatusFields();
        }*/

        // running in batch mode
        // TODO: should not be placed in GUI class
        if (batch) {
            createButtonActionPerformed(null);
            publishButtonActionPerformed(null);
            doneButtonActionPerformed(null);
        }
    }

    private void updateStatusFields() {
        try {
            // init info panel
            titleField.setText(recording.prefs.name);
            filenameField.setText(recording.getDirectory() + recording.getFileBase() + ".ttt");
            if (recording.getFileBase().length() > 2
                    && recording.getFileBase().charAt(recording.getFileBase().length() - 2) == '-') {
                filenameField.setForeground(Color.RED);
                filenameField
                        .setToolTipText("maybe file should be renamed to "
                                + recording.getFileBase().substring(0, recording.getFileBase().length() - 2)
                                + ".ttt/.mp3/.mov");
            }
            dateField.setText(new Date(recording.prefs.starttime).toString());
            durationField.setText(Constants.getStringFromTime(recording.getDuration(), false) + " min");
            indexField.setText(recording.index.size() + " pages");
            resolutionField.setText(recording.prefs.framebufferWidth + " x " + recording.prefs.framebufferHeight
                    + " - " + recording.prefs.depth + " bit");

            // audio available
            try {
            	
                if (recording.getAudioFilename() != null) {
                    audioField.setForeground(Color.GREEN);
                    audioField.setText(MP3Converter.getAudioFormat(recording) + " audio found");
                } else {
                    String wavFile = Constants.getExistingFile(recording.getFileName(), Constants.AUDIO_FILE)
                            .getCanonicalPath();
                    if (wavFile == null)
                        throw new FileNotFoundException();

                    // convert wav to mp3

                }
            } catch (Exception e) {
                audioField.setForeground(Color.RED);
                audioField.setText("not found");
            }
          
            // init thumbs and script panel
            if (recording.thumbnailsAvailable()) {
                thumbnailsStatusField.setForeground(Color.GREEN);
                thumbnailsStatusField.setText("found");
            } else {
                thumbnailsStatusField.setForeground(Color.RED);
                thumbnailsStatusField.setText("not found");
            }
            if (recording.getExistingFileBySuffix("html").isDirectory()) {
                htmlStatusField.setForeground(Color.GREEN);
                htmlStatusField.setText("folder found");
                htmlStatusField.setToolTipText("folder exists - content not confirmed");
            } else {
                htmlStatusField.setForeground(Color.RED);
                htmlStatusField.setText("not found");
                htmlStatusField.setToolTipText(null);
            }
            if (recording.getExistingFileBySuffix("pdf").exists()) {
                pdfStatusField.setForeground(Color.GREEN);
                pdfStatusField.setText("found");
            } else {
                pdfStatusField.setForeground(Color.RED);
                pdfStatusField.setText("not found");
            }
            if (recording.getExistingFileBySuffix("ocr").exists()) {
                ocrStatusField.setForeground(Color.GREEN);
                ocrStatusField.setText("folder found");
                ocrStatusField.setToolTipText("folder exists - content not confirmed");
            } else {
                ocrStatusField.setForeground(Color.RED);
                ocrStatusField.setText("not found");
                ocrStatusField.setToolTipText(null);
            }
            if (recording.getExistingFileBySuffix("bjpg").exists()) {
            	videoField.setForeground(Color.GREEN);
            	videoField.setText("found");
               camStatusField.setForeground(Color.GREEN);
               camStatusField.setText("folder found");
               camStatusField.setToolTipText("folder exists - content not confirmed");
            } else {
            	camStatusField.setForeground(Color.RED);
            	camStatusField.setText("not found");
            	videoField.setForeground(Color.RED);
            	videoField.setText("not found");
            	camStatusField.setToolTipText(null);
            }
            if (recording.getExistingFileBySuffix("mp3").exists()) {
                mp3StatusField.setForeground(Color.GREEN);
                mp3StatusField.setText("found");
            } else {
                mp3StatusField.setForeground(Color.RED);
                mp3StatusField.setText("not found");
            }
            File file = recording.getExistingFileBySuffix("swf");
            if (file.isFile() && file.length() < 2000) {
                flashStatusField.setForeground(Color.ORANGE);
                flashStatusField.setText("found but may be corrupted (" + file.length() + " bytes only)");
            } else if (file.isFile()) {
                flashStatusField.setForeground(Color.GREEN);
                flashStatusField.setText("found");
            } else {
                flashStatusField.setForeground(Color.RED);
                flashStatusField.setText("not found");
            }
            if (recording.getExistingFileBySuffix("mp4").exists()) {
                mp4StatusField.setForeground(Color.GREEN);
                mp4StatusField.setText("found");
            } else {
                mp4StatusField.setForeground(Color.RED);
                mp4StatusField.setText("not found");
            }
            
            // init full text search panel
            switch (recording.index.getSearchbaseFormat()) {
            case Index.XML_SEARCHBASE:
                searchStatusField.setForeground(Color.GREEN);
                searchStatusField.setText("searchable ( XML Searchbase )");
                searchStatusField
                        .setToolTipText("search base with coordinates - search results can be emphasized within each page");
                break;
            case Index.ASCII_SEARCHBASE:
                searchStatusField.setForeground(Color.ORANGE);
                searchStatusField.setText("searchable ( ASCII Searchbase )");
                searchStatusField
                        .setToolTipText("search base without coordinates - search results cannot be emphasized within a page (use XML searchbase instead)");
                break;
            default:
                searchStatusField.setForeground(Color.RED);
                searchStatusField.setText("not searchable");
                searchStatusField.setToolTipText("import a searchbase to add full text search");
                break;
            }
            searchFilenameField.setText(recording.getDirectory() + recording.getFileBase()
                    + Constants.searchbaseEndings[0]);

            // init publishing panel
        } catch (IOException e) {
            System.out.println("Error during initialization");
            e.printStackTrace();
        }
    }

    // TODO: batch mode
    public static void main(String args[]) throws IOException {
        boolean batch = false;
        int arg = 0;
        if (args.length < 1) {
            System.out.println("Parameters: [-b|-batch] filename");
            return;
        } else if (args[arg].equals("-b") || args[arg].equals("-batch")) {
            batch = true;
            arg++;
        }

        // open recording
        String file = args[arg];
        boolean verbose = TTT.verbose;
        if (batch)
            TTT.setVerbose(false);
        Recording recording = new Recording(file, false);
        TTT.setVerbose(verbose);

        // post processing
        JInternalFrame frame = new JInternalFrame("TTT: Post Processing");

        if (batch) {
            new PostProcessorPanel(recording, batch);
            System.exit(0);
        } else {
            // internal frame
            frame.setContentPane(new PostProcessorPanel(recording, batch));
            frame.pack();
            frame.setVisible(true);

            // // closing behaviour
            // frame.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
            // frame.setClosable(false);

            TTT ttt = TTT.getInstance();
            ttt.showTTT();
            ttt.addInternalFrameCentered(frame);
        }
    }

    
  
    
    private void initComponents() {    	
        jtabPane =  new JTabbedPane();
        jPanelInfo = new javax.swing.JPanel();
        jlblTitle = new javax.swing.JLabel();
        jlblDate = new javax.swing.JLabel();
        jlblDuration = new javax.swing.JLabel();
        jlblIndex = new javax.swing.JLabel();
        jlblResultion = new javax.swing.JLabel();
        resolutionField = new javax.swing.JLabel();
        indexField = new javax.swing.JLabel();
        durationField = new javax.swing.JLabel();
        dateField = new javax.swing.JLabel();
        titleField = new javax.swing.JTextField();
        jlblFile = new javax.swing.JLabel();
        filenameField = new javax.swing.JLabel();
        jlblAudio = new javax.swing.JLabel();
        jlblVideo = new javax.swing.JLabel();
        audioField = new javax.swing.JLabel();
        videoField = new javax.swing.JLabel();
        jPanelThumbs = new javax.swing.JPanel();
        thumbnailsCheckBox = new javax.swing.JCheckBox();
        htmlCheckBox = new javax.swing.JCheckBox();
        pdfCheckBox = new javax.swing.JCheckBox();
        ocrCheckBox = new javax.swing.JCheckBox();
        flashCheckBox = new javax.swing.JCheckBox();
        createButton = new javax.swing.JButton();
        createHelpButton = new javax.swing.JButton();
        thumbnailsStatusField = new javax.swing.JLabel();
        htmlStatusField = new javax.swing.JLabel();
        pdfStatusField = new javax.swing.JLabel();
        ocrStatusField = new javax.swing.JLabel();
        flashStatusField = new javax.swing.JLabel();
        mp3CheckBox = new javax.swing.JCheckBox();
        mp3StatusField = new javax.swing.JLabel();
        mp4CheckBox = new javax.swing.JCheckBox();
        mp4StatusField = new javax.swing.JLabel();
        camCheckBox = new javax.swing.JCheckBox();
        camStatusField = new javax.swing.JLabel();
        jPanelFullTextSearch = new javax.swing.JPanel();
        jlblStatus = new javax.swing.JLabel();
        searchStatusField = new javax.swing.JLabel();
        searchFilenameField = new javax.swing.JTextField();
        openSearchbaseFileDialogButton = new javax.swing.JButton();
        importSearchbaseButton = new javax.swing.JButton();
        searchHelpButton = new javax.swing.JButton();
        jlblChoosFile = new javax.swing.JLabel();
        doneButton = new javax.swing.JButton();
        jPanelPublishing = new javax.swing.JPanel();
        jlblUser = new javax.swing.JLabel();
        jlblServer = new javax.swing.JLabel();
        jlblPath = new javax.swing.JLabel();
        userField = new javax.swing.JTextField();
        serverField = new javax.swing.JTextField();
        pathField = new javax.swing.JTextField();
        publishButton = new javax.swing.JButton();
        publishHelpButton = new javax.swing.JButton();

        setOpaque(false);

                
        jPanelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder("Info"));
        jPanelInfo.setOpaque(false);

        jlblTitle.setText("Title:");

        jlblDate.setText("Date:");

        jlblDuration.setText("Duration:");

        jlblIndex.setText("Index:");

        jlblResultion.setText("Resolution:");

        resolutionField.setText("N/A");

        indexField.setText("N/A");

        durationField.setText("N/A");

        dateField.setText("N/A");

        titleField.setText("<enter title>");

        jlblFile.setText("File:");

        filenameField.setText("N/A");

        jlblAudio.setText("Audio:");

        jlblVideo.setText("Video:");

        audioField.setText("N/A");

        videoField.setText("N/A");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanelInfo);
        jPanelInfo.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jlblTitle)
                    .add(jlblFile)
                    .add(jlblDate)
                    .add(jlblDuration)
                    .add(jlblIndex)
                    .add(jlblResultion, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 79, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jlblAudio)
                    .add(jlblVideo))
                .add(6, 6, 6)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(videoField)
                    .add(audioField)
                    .add(filenameField)
                    .add(indexField)
                    .add(durationField)
                    .add(dateField)
                    .add(resolutionField)
                    .add(titleField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 433, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(177, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(0, 0, 0)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(titleField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(filenameField))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jlblTitle)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jlblFile)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(dateField)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(durationField)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(indexField)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(resolutionField))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jlblDate)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jlblDuration)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jlblIndex)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jlblResultion)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jlblAudio)
                    .add(audioField))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jlblVideo)
                    .add(videoField))
                .addContainerGap())
        );

        jPanelThumbs.setBorder(javax.swing.BorderFactory.createTitledBorder("Thumbs, Script and Flash"));
        jPanelThumbs.setOpaque(false);

        thumbnailsCheckBox.setSelected(true);
        thumbnailsCheckBox.setText("Thumbnails");
        thumbnailsCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        htmlCheckBox.setSelected(true);
        htmlCheckBox.setText("HTML script");
        htmlCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        pdfCheckBox.setSelected(true);
        pdfCheckBox.setText("PDF script");
        pdfCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        ocrCheckBox.setSelected(true);
        ocrCheckBox.setText("OCR input");
        ocrCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        flashCheckBox.setSelected(true);
        flashCheckBox.setText("Flash/SWF");
        flashCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        createButton.setText("Create");
        createButton.setMargin(new java.awt.Insets(0, 8, 0, 8));
        createButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createButtonActionPerformed(evt);
            }
        });

        createHelpButton.setText("Help");
        createHelpButton.setMargin(new java.awt.Insets(0, 8, 0, 8));
        createHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createHelpButtonActionPerformed(evt);
            }
        });

        thumbnailsStatusField.setText("not found");

        htmlStatusField.setText("not found");

        pdfStatusField.setText("not found");

        ocrStatusField.setText("not found");

        flashStatusField.setText("not found");

        mp3CheckBox.setSelected(true);
        mp3CheckBox.setText("MP3 audio");
        mp3CheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        mp3StatusField.setText("not found");

        mp4CheckBox.setSelected(true);
        mp4CheckBox.setText("MP4 podcast");
        mp4CheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        mp4StatusField.setText("not found");

        camCheckBox.setSelected(true);
        camCheckBox.setText("Dozentenvideo");
        camCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        camStatusField.setBackground(new java.awt.Color(100, 208, 200));
        camStatusField.setText("not found");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanelThumbs);
        jPanelThumbs.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(thumbnailsCheckBox)
                    .add(htmlCheckBox)
                    .add(pdfCheckBox)
                    .add(ocrCheckBox)
                    .add(mp3CheckBox)
                    .add(flashCheckBox)
                    .add(mp4CheckBox)
                    .add(camCheckBox))
                .add(28, 28, 28)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(thumbnailsStatusField)
                    .add(htmlStatusField)
                    .add(pdfStatusField)
                    .add(ocrStatusField)
                    .add(mp3StatusField)
                    .add(flashStatusField)
                    .add(mp4StatusField)
                    .add(camStatusField))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 405, Short.MAX_VALUE)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(createHelpButton)
                    .add(createButton))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(new java.awt.Component[] {createButton, createHelpButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(thumbnailsCheckBox)
                            .add(thumbnailsStatusField))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(htmlCheckBox)
                            .add(htmlStatusField))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(pdfCheckBox)
                            .add(pdfStatusField)))
                    .add(createHelpButton))
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(ocrCheckBox)
                            .add(ocrStatusField))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(mp3CheckBox)
                            .add(mp3StatusField))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(flashCheckBox)
                            .add(flashStatusField))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(mp4CheckBox)
                            .add(mp4StatusField))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(camCheckBox)
                            .add(camStatusField))
                        .addContainerGap(24, Short.MAX_VALUE))
                    .add(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(createButton)
                        .addContainerGap())))
        );

        jPanelFullTextSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Full Text Search"));
        jPanelFullTextSearch.setOpaque(false);

        jlblStatus.setText("Status:");
        jlblStatus.setMaximumSize(new java.awt.Dimension(70, 15));
        jlblStatus.setMinimumSize(new java.awt.Dimension(70, 15));
        jlblStatus.setPreferredSize(new java.awt.Dimension(70, 15));

        searchStatusField.setText("N/A");

        searchFilenameField.setText("<enter filename>");
        searchFilenameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchFilenameFieldActionPerformed(evt);
            }
        });

        openSearchbaseFileDialogButton.setText("...");
        openSearchbaseFileDialogButton.setMargin(new java.awt.Insets(0, 8, 0, 8));
        openSearchbaseFileDialogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openSearchbaseFileDialogButtonButtonActionPerformed(evt);
            }
        });

        importSearchbaseButton.setText("Import");
        importSearchbaseButton.setMargin(new java.awt.Insets(0, 8, 0, 8));
        importSearchbaseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSearchbaseButtonActionPerformed(evt);
            }
        });

        searchHelpButton.setText("Help");
        searchHelpButton.setMargin(new java.awt.Insets(0, 8, 0, 8));
        searchHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchHelpButtonActionPerformed(evt);
            }
        });

        jlblChoosFile.setText("Choose file:");

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanelFullTextSearch);
        jPanelFullTextSearch.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jlblChoosFile)
                    .add(jlblStatus, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(searchFilenameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 331, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(searchStatusField))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(openSearchbaseFileDialogButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 150, Short.MAX_VALUE)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, importSearchbaseButton)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, searchHelpButton))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(new java.awt.Component[] {importSearchbaseButton, searchHelpButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jlblStatus, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(searchStatusField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jlblChoosFile)
                            .add(searchFilenameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(openSearchbaseFileDialogButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(searchHelpButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(importSearchbaseButton)))
                .addContainerGap(26, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(new java.awt.Component[] {importSearchbaseButton, openSearchbaseFileDialogButton}, org.jdesktop.layout.GroupLayout.VERTICAL);

        doneButton.setText("Done");
        doneButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneButtonActionPerformed(evt);
            }
        });

        jPanelPublishing.setBorder(javax.swing.BorderFactory.createTitledBorder("Publishing"));
        jPanelPublishing.setOpaque(false);

        jlblUser.setText("User");

        jlblServer.setText("Server");

        jlblPath.setText("Path");

        userField.setText("<enter user name>");

        serverField.setText("ttt.in.tum.de");

        pathField.setText("www/recordings/");

        publishButton.setText("Publish");
        publishButton.setMargin(new java.awt.Insets(0, 6, 0, 6));
        publishButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                publishButtonActionPerformed(evt);
            }
        });

        publishHelpButton.setText("Help");
        publishHelpButton.setMargin(new java.awt.Insets(0, 8, 0, 8));
        publishHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                publishHelpButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanelPublishingLayout = new org.jdesktop.layout.GroupLayout(jPanelPublishing);
        jPanelPublishing.setLayout(jPanelPublishingLayout);
        jPanelPublishingLayout.setHorizontalGroup(
            jPanelPublishingLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanelPublishingLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanelPublishingLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jlblUser)
                    .add(jlblServer)
                    .add(jlblPath))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanelPublishingLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanelPublishingLayout.createSequentialGroup()
                        .add(userField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 322, Short.MAX_VALUE)
                        .add(publishHelpButton))
                    .add(jPanelPublishingLayout.createSequentialGroup()
                        .add(pathField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 322, Short.MAX_VALUE)
                        .add(publishButton))
                    .add(serverField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 238, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanelPublishingLayout.linkSize(new java.awt.Component[] {pathField, serverField, userField}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanelPublishingLayout.linkSize(new java.awt.Component[] {publishButton, publishHelpButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanelPublishingLayout.setVerticalGroup(
            jPanelPublishingLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanelPublishingLayout.createSequentialGroup()
                .add(jPanelPublishingLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanelPublishingLayout.createSequentialGroup()
                        .add(jPanelPublishingLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jlblUser)
                            .add(userField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(publishHelpButton))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanelPublishingLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jlblServer)
                            .add(serverField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanelPublishingLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jlblPath)
                            .add(pathField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, publishButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

     //   org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        
        
        jtabPane.add("Info", jPanelInfo);
        jtabPane.add("Thumbs, Script and Flash", jPanelThumbs);
        jtabPane.add("Full Text Search", jPanelFullTextSearch);
        jtabPane.add("Publishing", jPanelPublishing);
        this.add(jtabPane);
        this.add(doneButton);
        
    }

    private void publishHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_publishHelpButtonActionPerformed
        String infoText = "By pressing the publish button the TTT generates a zip archive of the current lecture\n"
                + "and copies every lecture related files (archives, scripts, flash, etc.) to the specified file server.\n"
                + "Copying is done via ssh/sftp.\n\n"
                + "    User: the user name for the account on the file server\n"
                + "    Server: the host name of the file server\n"
                + "    Path: the base path under which all lectures are stored (a subfolder will be created for each lecture)\n\n"
                + "Note:\nThe TTT supports the private/public key feature of ssh to loggin without specifying a password.\n"
                + "Options can be set via menu: Post Processing -> ssh options";
        if (!LibraryChecker.isJSchInstalled()) {
            infoText = "WARNING:\n" + "    THE PUBLISHING FEATURE IS CURRENTLY NOT AVAILABLE\n"
                    + "    JSch - Java Secure Channel Library: NOT INSTALLED\n\n" + infoText;
        }

        TTT.showMessage(infoText, "TTT: Publisher - Help", JOptionPane.INFORMATION_MESSAGE);
    }// GEN-LAST:event_publishHelpButtonActionPerformed

    private void doneButtonActionPerformed(java.awt.event.ActionEvent evt) {
        storeRecordingIfNeeded();

        // close parent component
        Component parent = getParent();
        while (parent != null) {
            if (parent instanceof JInternalFrame) {
                ((JInternalFrame) parent).dispose();
                return;
            } else if (parent instanceof JFrame) {
                ((JFrame) parent).dispose();
                return;
            } else
                parent = parent.getParent();
        }

    }// GEN-LAST:event_doneButtonActionPerformed

    public void setEnabled(boolean enabled) {
    	if (enabled == false) {
    		//Save the enable status of components which may be disabled
    		ctrlSatus.put(thumbnailsCheckBox, thumbnailsCheckBox.isEnabled());
    		ctrlSatus.put(pdfCheckBox, pdfCheckBox.isEnabled());
    		ctrlSatus.put(flashCheckBox, flashCheckBox.isEnabled());
    		ctrlSatus.put(mp3CheckBox, mp3CheckBox.isEnabled());
    		ctrlSatus.put(mp4CheckBox, mp4CheckBox.isEnabled());
    		ctrlSatus.put(camCheckBox, camCheckBox.isEnabled());
    		ctrlSatus.put(userField, userField.isEnabled());
    		ctrlSatus.put(serverField, serverField.isEnabled());
    		ctrlSatus.put(pathField, pathField.isEnabled());
    		ctrlSatus.put(publishButton, publishButton.isEnabled());
    	}
        super.setEnabled(enabled);
        titleField.setEnabled(enabled);
        thumbnailsCheckBox.setEnabled(enabled && ctrlSatus.get(thumbnailsCheckBox)); 
        htmlCheckBox.setEnabled(enabled);
        pdfCheckBox.setEnabled(enabled && ctrlSatus.get(pdfCheckBox));
        ocrCheckBox.setEnabled(enabled);

        flashCheckBox.setEnabled(enabled && ctrlSatus.get(flashCheckBox));	     
        mp3CheckBox.setEnabled(enabled && ctrlSatus.get(mp3CheckBox));
        mp4CheckBox.setEnabled(enabled && ctrlSatus.get(mp4CheckBox));	
        camCheckBox.setEnabled(enabled && ctrlSatus.get(camCheckBox));
        createHelpButton.setEnabled(enabled);
        createButton.setEnabled(enabled);
        searchFilenameField.setEnabled(enabled);
        openSearchbaseFileDialogButton.setEnabled(enabled);
        searchHelpButton.setEnabled(enabled);
        importSearchbaseButton.setEnabled(enabled);
        userField.setEnabled(enabled && ctrlSatus.get(userField));	
        serverField.setEnabled(enabled && ctrlSatus.get(serverField));	
        pathField.setEnabled(enabled && ctrlSatus.get(pathField));	
        publishHelpButton.setEnabled(enabled);
        publishButton.setEnabled(enabled && ctrlSatus.get(publishButton));	
        doneButton.setEnabled(enabled);
    }

    public void zip(String zip_file, String files[]) {
        int read = 0;
        FileInputStream in;
        byte[] data = new byte[1024];
        ProgressMonitor monitor = null;
        try {
            // get total size of input files
            int max = 0;
            for (String file : files)
                max += (int) new File(file).length();
            // create monitor
            if (!batch)
                monitor = new ProgressMonitor(TTT.ttt, "Packing archive " + new File(zip_file).getName(), "", 0, max);
            // Zip-Archiv mit Stream verbinden
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip_file));
            // Archivierungs-Modus setzen
            out.setMethod(ZipOutputStream.DEFLATED);
            // Hinzuf�gen der einzelnen Eintr�ge
            int progress = 0;
            for (int i = 0; i < files.length; i++) {
                try {
                    System.out.print(files[i] + "\t");
                    // Eintrag f�r neue Datei anlegen
                    File file = new File(files[i]);
                    if (!file.exists()) {
                        System.out.println("NOT FOUND");
                    } else {
                        // ZipEntry OHNE PFAD!!
                        ZipEntry entry = new ZipEntry(file.getName());
                        in = new FileInputStream(files[i]);
                        // Neuer Eintrag dem Archiv hinzuf�gen
                        out.putNextEntry(entry);
                        // Hinzuf�gen der Daten zum neuen Eintrag
                        while ((read = in.read(data, 0, 1024)) != -1) {
                            out.write(data, 0, read);
                            progress += read;
                            if (!batch)
                                monitor.setProgress(progress);
                            long percentage = progress * 100l / max;
                            System.out.print((percentage < 10 ? " " : "") + (percentage < 100 ? " " : "") + percentage
                                    + "%\b\b\b\b");
                        }
                        System.out.println(i + 1 != files.length ? "    " : "");
                        out.closeEntry(); // Neuen Eintrag abschlie�en
                        in.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (monitor != null)
                monitor.close();
        }
    }

    private void storeRecordingIfNeeded() {
        // store modified recording
        try {
            // check if title was modified
            if (!recording.prefs.name.equals(titleField.getText())) {
                recording.prefs.name = titleField.getText();
                modified = true;
            }

            // test if user has forgotten to import searchbase
            switch (recording.index.getSearchbaseFormat()) {
            case Index.XML_SEARCHBASE:
                // the best possible searchbase already has been loaded
                break;
            case Index.ASCII_SEARCHBASE:
                // check whether XML searchbase is available or not
                try {
                    File file = Constants.getExistingFile(recording.fileDesktop.getCanonicalPath(),
                            Constants.SEARCHBASE_FILE);
                    if (file != null && file.getName().toLowerCase().endsWith(".txt"))
                        // only ASCII searchbase available - already loaded - everything is fine
                        break;
                } catch (FileNotFoundException e) {
                    // no xml searchbase available
                }
                // else load xml searchbase
            case Index.NO_SEARCHBASE:
            default:
                // load searchbase (if available)
                if (recording.readSearchbaseFromFile())
                    modified = true;
                break;
            }

            // check if searchbase was modified
            if (recording.index.getSearchbaseFormat() != recording.index.getSearchbaseFormatStored())
                modified = true;

            // store changes if needed
            if (modified) {
            	if(TTT.verbose){
                System.out.println("writing changes to recording");
            	}
                recording.store();
                modified = false;
            }
        } catch (Exception e) {
            String file = "";
            try {
                file = recording.getFileName();
            } catch (IOException e1) {}
            TTT.showMessage("Error while writing " + file + ":\t" + e);
            e.printStackTrace();
        }
    }

    private void publishButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_publishButtonActionPerformed
        // publish lecture and corresponding data by copying everything to a file server
        // run in own thread to leeav event dispatching thread
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    System.out.println("\nTTT PUBLISHER:\n");

                    // disable user input
                    setEnabled(false);

                    storeRecordingIfNeeded();

                    if (!LibraryChecker.isJSchInstalled())
                        return;

                    // open sftp session
                    String user = userField.getText();
                    String host = serverField.getText();
                    SftpHelper session = new SftpHelper(user, host);
                    
                    // calculate paths
                    String path = pathField.getText();
                    if (!path.endsWith("/"))
                        path = path + "/";
                    String basePath = path + recording.getFileBase() + "/";

                    // store fields for the next time
                    TTT.userPrefs.put("publish_user", user);
                    TTT.userPrefs.put("publish_server", host);
                    TTT.userPrefs.put("publish_path", path);

                    // check flash size
                    File file = new File(recording.getDirectory() + recording.getFileBase() + ".swf");
                    if (file.isFile() && file.length() < 2000)
                        TTT.showMessage("SWF/Flash movie may be corrupted because it is very short (" + file.length()
                                + " bytes)! ", "TTT: Publishing", JOptionPane.WARNING_MESSAGE);

                    // zip archives
                    try {
                        String zip_file = recording.getDirectory() + recording.getFileBase() + "_a.zip";
                        System.out.println("Packing archive " + new File(zip_file).getName()
                                + " (desktop and audio stream)");
                        String filesTA[] = { recording.getFileName(), recording.getAudioFilename() };
                        zip(zip_file, filesTA);
                    } catch (FileNotFoundException e) {
                        System.out.println("Cannot pack desktop and audio files:\n\t" + e);
                    }

                    try {
                        String zip_file = recording.getDirectory() + recording.getFileBase() + "_av.zip";
                        System.out.println("\nPacking archive " + new File(zip_file).getName()
                                + " (desktop, audio and video stream)");
                        String filesTAV[] = { recording.getFileName(), recording.getAudioFilename(),
                                recording.getVideoFilename() };
                        zip(zip_file, filesTAV);
                    } catch (FileNotFoundException e) {
                        System.out.println("Cannot pack desktop, audio and video files:\n\t" + e);
                    }

                    // copy everything to server
                    System.out.println("\nCopying data to server " + user + "@" + host + ":" + basePath);
                    System.out.println();

                    // copy zips
                    session.publish(recording.getDirectory() + recording.getFileBase() + "_a.zip", basePath, batch);
                    session.publish(recording.getDirectory() + recording.getFileBase() + "_av.zip", basePath, batch);

                    // flash movie of recording .swf
                    session.publish(recording.getDirectory() + recording.getFileBase() + ".swf", basePath, batch);
                    session.publish(recording.getDirectory() + recording.getFileBase() + "_without_controls.swf",
                            basePath, batch);

                    // searchbase recording .xml / .txt
                    session.publish(recording.getDirectory() + recording.getFileBase() + ".xml", basePath, batch);
                    session.publish(recording.getDirectory() + recording.getFileBase() + ".txt", basePath, batch);

                    // // OmniPage file .opd (Optical Character Recognition Software)
                    // session.publish(recording.getDirectory() + recording.getFileBase() + ".opd", basePath, batch);

                    // script recording .pdf
                    session.publish(recording.getDirectory() + recording.getFileBase() + ".pdf", basePath, batch);
                    // html script (including subfolders)
                    session.publish(recording.getDirectory() + recording.getFileBase() + ".html", basePath, batch);

                    System.out.println("Backup original files (if modified):");
                    // original/unmodified desktop recording (.ttt.orig)
                    session.publish(recording.getFileName() + ".orig", basePath, batch);
                    // original/unmodified audio recording (if converted for flash
                    String audio_filename = recording.getAudioFilename();
                    if (!session.publish(audio_filename + ".orig", basePath, batch) && audio_filename.length() > 4)
                        // try if original is wav file
                       // if (!session.publish(audio_filename.substring(0, audio_filename.length() - 4) + ".wav", basePath, batch))
                            // try if original is mp2 file
                            session.publish(audio_filename.substring(0, audio_filename.length() - 4) + ".mp2",
                                    basePath, batch);

                    //mp4 podcast
                    session.publish(recording.getDirectory() + recording.getFileBase() + ".mp4", basePath, batch);
                    
                    System.out.println("Published\n");

                    // remote call to update online searchbase
                    // TODO: show progress bar
                    System.out.println("Updating online search base:");
                    String command = "www/search/generate.pl www/search/words.txt " + path + " "
                            + recording.getFileBase();
                    session.executeCommand(command);
                    System.out.println("online search base updated.");

                    // terminate sftp session
                    session.close();

                    publishButton.setForeground(Color.BLACK);
                    doneButton.setForeground(Color.BLUE);
                } catch (Exception e) {
                    TTT.showMessage("Publishing failed: " + e, "TTT: Publishing", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } finally {
                    setEnabled(true);
                }
            }

        });
        if (batch)
            thread.run();
        else
            thread.start();
    }// GEN-LAST:event_publishButtonActionPerformed

    private void searchFilenameFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_searchFilenameFieldActionPerformed
        searchbaseActionPerformed(evt);
    }// GEN-LAST:event_searchFilenameFieldActionPerformed

    private void createHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_createHelpButtonActionPerformed
        String infoText = "Thumbnails:\n"
                + "    Thumbnails are small preview images of all pages of the ttt recording are used as\n"
                + "    clickable thumbnail overview and provides slide based navigation.\n\n";
        infoText += "HTML Script:\n"
                + "    The HTML Script offers a thumbnail overview and linked screenshots of all indexed pages\n"
                + "    The script includes the annoations made by the teacher during the recording.\n\n";
        infoText += "PDF Script:\n"
                + "    The PDF Script contains the annotated screenshots in one single pdf document.\n\n";
        if (!LibraryChecker.isITextPdfLibraryInstalled())
            infoText += "  WARNING:\n" + "      THE PDF FEATURE IS CURRENTLY NOT AVAILABLE\n"
                    + "      iText - a Free Java-PDF library: NOT INSTALLED\n\n";
        infoText += "OCR Input:\n"
                + "    Optical Character Recognition ( abbr. OCR ) is used to create a searchbase to enable\n"
                + "    Full Text Search for ttt recordings ( see Full Text Search Help ). The input for OCR software\n"
                + "    are screenshots of the indexed pages. The screenshots of the corresponding HTML Script can\n"
                + "    also be used. However, the special OCR input is optimized as it does not contain annotations,\n"
                + "    which may otherwise influence the recognition.\n\n";
        infoText += "Flash/SWF:\n"
                + "    Creates a corresponding Flash movie for a TTT recording. Currently thumbnail overview and\n"
                + "    slide based navigation is supported. The teacher video, full text search and timeline navigation\n"
                + "    are not supported yet.\n"
                + "    Note that the generation of the flash movie may take several minutes and consumes a lot\n"
                + "    of memory (up to 1000 MByte and more). So adjust your Java Heap memory size accordingly\n"
                + "    (java option: -Xmx1024m). For some recordings it is necessary to convert the audio track,\n"
                + "    which also may take up to several minutes.\n\n";

        infoText += "The output files are stored in the same directory as the ttt recording:\n";
        try {
            infoText += "    " + recording.getDirectory() + recording.getFileBase() + ".pdf / .html / .ocr / .swf\n\n";
        } catch (IOException e) {}

        TTT.showMessage(infoText, "TTT: Script & Flash Creator - Help", JOptionPane.INFORMATION_MESSAGE);
    }// GEN-LAST:event_createHelpButtonActionPerformed

    private void createButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_createButtonActionPerformed
        // create whatever was selected by the user
        // get out of event dispatching thread
    
        Thread thread = new Thread(new Runnable() {
            public void run() {
                // disable user input
                setEnabled(false);
                // set new name
                // TODO: don't set here - maybe ask to store instead
                if (!recording.prefs.name.equals(titleField.getText())) {
                    recording.prefs.name = titleField.getText();
                    modified = true;
                }

                createScreenShots();

                // now store recording with thumbnails
                // NOTE: store Thumbnails now, because flash generation may fail due to insufficient memory
                storeRecordingIfNeeded();

                convertAudio();
                createFlash();
                createMp4();
                createWebCamVideo();
            
                
                // update status fields
                updateStatusFields();

                // enable user input again
                setEnabled(true);
                createButton.setForeground(Color.BLACK);
                importSearchbaseButton.setForeground(Color.BLUE);
            }
            
            
        });
        if (batch)
            thread.run();
        else
            thread.start();
            
    }// GEN-LAST:event_createButtonActionPerformed
    private void createWebCamVideo() {
        
        //create camVid
          try {
              if (camCheckBox.isSelected()) {
                  ttt.video.VideoCreator newVideo = new  ttt.video.VideoCreator();
                  newVideo.create(recording.getExistingFileBySuffix("bjpg").getPath());
              }
          } catch (Exception e) {
              TTT.showMessage("CamVid creation failed: " + e);
              e.printStackTrace();
          }
          
		
	}
	private void createMp4() {
    	//create mp4 podcast
        try {
            if (mp4CheckBox.isSelected()) {
                PodcastCreator.createPodcast(recording, batch);
            }
        } catch (Exception e) {
            TTT.showMessage("MP4 creation failed: " + e);
            e.printStackTrace();
        }
		
	}
	public void createFlash(){
    	  // create flash movie
        try { 
            if (flashCheckBox.isSelected()) {
                if (!recording.thumbnailsAvailable())
                    modified = true; // thumbnails will be created by flash generator
                recording.createFlash(batch);
            }
        } catch (Exception e) {
            TTT.showMessage("Flash creation failed: " + e);
            e.printStackTrace();
        }
    }
    
    public void convertAudio(){
    	//convert audio file
        try {
            if (mp3CheckBox.isSelected()) {
                LameEncoder.convertAudioFile(recording.getExistingFileBySuffix(new String[] {"wav","mp2"}), recording.getFileBySuffix("mp3"), batch);
            }
        } catch (Exception e) {
            TTT.showMessage("MP3 creation failed: " + e);
            e.printStackTrace();
        }

    }
    
    public void createScreenShots(){
    	  // create screenshot related data
        try {
            // get combined mode
            int mode = 0;
            if (thumbnailsCheckBox.isSelected()) {
                mode |= ScriptCreator.THUMBNAILS;
                modified = true;
            }
            if (htmlCheckBox.isSelected())
                mode |= ScriptCreator.HTML_SCRIPT;
            if (pdfCheckBox.isSelected())
                mode |= ScriptCreator.PDF_SCRIPT;
            if (ocrCheckBox.isSelected())
                mode |= ScriptCreator.OCR_OPTIMIZED;
            
         
            // compute everything specified by mode
            recording.createScript(mode, batch);
        } catch (Exception e) {
         //   TTT.showMessage("Script/screenshot creation failed: " + e);
            e.printStackTrace();
        }
    }
    private void searchbaseActionPerformed(java.awt.event.ActionEvent event) {
        // import searchbase file
        // show file dialog
        if (event.getSource() == openSearchbaseFileDialogButton)
            try {
                JFileChooser fileChooser = new JFileChooser();

                File lastRec = new File(searchFilenameField.getText());
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
                    searchFilenameField.setText(fileChooser.getSelectedFile().getCanonicalPath());

                // or cancel
                else
                    return;
            } catch (IOException e) {
                System.out.println("File selection failed: " + e);
                return;
            }

        // load searchbase
        try {
            String filename = searchFilenameField.getText();
            if (!new File(filename).exists()) {
                TTT.showMessage("File Not Found: " + filename, "Loading Searchbase failed", JOptionPane.ERROR_MESSAGE);
            } else if (recording.readSearchbaseFromFile(filename)) {
                modified = true;
                updateStatusFields();
                importSearchbaseButton.setForeground(Color.BLACK);
                publishButton.setForeground(Color.BLUE);

            } else {
                TTT.showMessage("Loading Searchbase failed: " + filename, "Loading Searchbase failed",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            TTT.showMessage("Loading Searchbase failed:\nError: " + e, "Loading Searchbase failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSearchbaseFileDialogButtonButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_openSearchbaseFileDialogButtonButtonActionPerformed
        searchbaseActionPerformed(evt);
    }// GEN-LAST:event_openSearchbaseFileDialogButtonButtonActionPerformed

    private void importSearchbaseButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_importSearchbaseButtonActionPerformed
        searchbaseActionPerformed(evt);
    }// GEN-LAST:event_importSearchbaseButtonActionPerformed

    private void searchHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_searchHelpButtonActionPerformed
        String line = "----------------------------------------------------------------------------\n";

        String infoText = line + "How to add Full Text Search to a TTT recording?\n" + line + "\n"
                + "In order to perform FULL TEXT SEARCH the TeleTeachingTool can import a SEARCHBASE,\n"
                + "which easily can be created using external OPTICAL CHARACTER RECOGNITION ( OCR ) Software.\n\n\n";
        infoText += line + "How to create a SEARCHBASE?\n" + line + "\n"
                + "The TeleTeachingTool offers the possibility to create SCREENSHOTS optimzed for OCR.\n"
                + "Load all those SCREENSHOTS into your favourite OCR-Software,\n"
                + "let it recognize the included text and store this text as XML or TXT file.\n\n\n";
        infoText += line + "Which SEARCHBASE FORMATS are supported?\n" + line + "\n" + "XML Searchbase:\n\n"
                + "TTT can import XML files written by OmniPage Pro 14 Office ( no WordML ).\n"
                + "This format contains coordinates, which allows not only to find pages including\n"
                + "search results, but highlighting search results within each page.\n\n"
                + "ASCII Searchbase (TXT files):\n\n"
                + "TTT can import TXT files containing ASCII text with page breaks.\n"
                + "Tested with ABBYY FineReader 7.0 ( remember to enable writing of page breaks ).\n"
                + "As this format does not contain coordinates, results can not be highlighted within\n"
                + "a page. However, the page can be found.\n";

        TTT.showMessage(infoText, "TTT: Full Text Search - Help", JOptionPane.INFORMATION_MESSAGE);
    }// GEN-LAST:event_searchHelpButtonActionPerformed

    // Variables declaration 
    private JTabbedPane jtabPane;
    private javax.swing.JLabel audioField;
    private javax.swing.JCheckBox camCheckBox;
    private javax.swing.JLabel camStatusField;
    private javax.swing.JButton createButton;
    private javax.swing.JButton createHelpButton;
    private javax.swing.JLabel dateField;
    private javax.swing.JButton doneButton;
    private javax.swing.JLabel durationField;
    private javax.swing.JLabel filenameField;
    private javax.swing.JCheckBox flashCheckBox;
    private javax.swing.JLabel flashStatusField;
    private javax.swing.JCheckBox htmlCheckBox;
    private javax.swing.JLabel htmlStatusField;
    private javax.swing.JButton importSearchbaseButton;
    private javax.swing.JLabel indexField;
    private javax.swing.JLabel jlblTitle;
    private javax.swing.JLabel jlblStatus;
    private javax.swing.JLabel jlblChoosFile;
    private javax.swing.JLabel jlblUser;
    private javax.swing.JLabel jlblServer;
    private javax.swing.JLabel jlblPath;
    private javax.swing.JLabel jlblDate;
    private javax.swing.JLabel jlblDuration;
    private javax.swing.JLabel jlblIndex;
    private javax.swing.JLabel jlblResultion;
    private javax.swing.JLabel jlblFile;
    private javax.swing.JLabel jlblAudio;
    private javax.swing.JLabel jlblVideo;
    private javax.swing.JPanel jPanelInfo;
    private javax.swing.JPanel jPanelThumbs;
    private javax.swing.JPanel jPanelFullTextSearch;
    private javax.swing.JPanel jPanelPublishing;
    private javax.swing.JCheckBox mp3CheckBox;
    private javax.swing.JLabel mp3StatusField;
    private javax.swing.JCheckBox mp4CheckBox;
    private javax.swing.JLabel mp4StatusField;
    private javax.swing.JCheckBox ocrCheckBox;
    private javax.swing.JLabel ocrStatusField;
    private javax.swing.JButton openSearchbaseFileDialogButton;
    private javax.swing.JTextField pathField;
    private javax.swing.JCheckBox pdfCheckBox;
    private javax.swing.JLabel pdfStatusField;
    private javax.swing.JButton publishButton;
    private javax.swing.JButton publishHelpButton;
    private javax.swing.JLabel resolutionField;
    private javax.swing.JTextField searchFilenameField;
    private javax.swing.JButton searchHelpButton;
    private javax.swing.JLabel searchStatusField;
    private javax.swing.JTextField serverField;
    private javax.swing.JCheckBox thumbnailsCheckBox;
    private javax.swing.JLabel thumbnailsStatusField;
    private javax.swing.JTextField titleField;
    private javax.swing.JTextField userField;
    private javax.swing.JLabel videoField;
    // End of variables declaration
}

