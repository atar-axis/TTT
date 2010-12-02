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
 * Created on 20.11.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.record;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import ttt.TTT;
import ttt.audio.AudioMonitorPanel;
import ttt.audio.AudioVideoMonitorPanel;
import ttt.gui.GradientPanel;
import ttt.gui.NumberField;
import ttt.gui.SpringUtilities;

public class LectureProfileDialog {
    public static void showLectureProfileDialog() {
        new LectureProfileDialog().show();
    }

    // profileSelected is used to distinguish between selection of an item and pressing enter for starting the slected
    // profile within the comboBox
    private boolean profileSelected = false;

    private boolean do_not_show_recording_options_in_present_and_record_dialog = true;

    private AudioMonitorPanel audioMonitorPanel;
    private AudioVideoMonitorPanel monitorPanel;

    private void show() {
        // get last used lecture profile (or default without name)

        final JInternalFrame frame = new JInternalFrame("TTT: Present & Record");

        // name of lecture (also name of profile)
        final JComboBox lectureComboBox = new JComboBox(LectureProfile.getLectures());
        lectureComboBox.setToolTipText("choose profile or enter name of lecture");
        lectureComboBox.setEditable(true);
        lectureComboBox.getEditor().setItem("<choose or create profile>");
        lectureComboBox.getEditor().selectAll();

        // teacher's name
        final JComboBox teacherComboBox = new JComboBox(LectureProfile.getTeachers());
        teacherComboBox.setToolTipText("choose or enter teacher's name");
        teacherComboBox.setEnabled(false);

        // title of recording
        final JLabel titleField = new JLabel();
        titleField.setToolTipText("title generated automatically");
        titleField.setEnabled(false);

        // name of recording file(s)
        final JLabel filenameLabel = new JLabel("Filename:");
        filenameLabel.setEnabled(false);

        // filename
        final JLabel filenameField = new JLabel();
        filenameField.setEnabled(false);
        filenameField.setToolTipText("filename generated automatically");

        // recording folder
        final JLabel recordingPathLabel = new JLabel("Folder:");
        recordingPathLabel.setEnabled(false);

        final JLabel recordingPathField = new JLabel();
        recordingPathField.setText(TTT.userPrefs.get("record_path", "."));
        recordingPathField.setEnabled(false);
        recordingPathField.setToolTipText("recordings are stored in this folder");

        final JButton recordingPathButton = new JButton("...");
        recordingPathButton.setMargin(new Insets(0, 4, 0, 4));
        recordingPathButton.setEnabled(false);
        recordingPathButton.setToolTipText("choose recording folder");
        recordingPathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Choose recording folder");
                fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setCurrentDirectory(new File(TTT.userPrefs.get("record_path", ".")));

                // ask user
                int returnVal = fileChooser.showDialog(TTT.getInstance(), "set recording folder");
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        File file = fileChooser.getSelectedFile();

                        System.out.println("\nRecord path: " + file.getCanonicalPath());
                        TTT.userPrefs.put("record_path", file.getCanonicalPath());
                        recordingPathField.setText(TTT.userPrefs.get("record_path", "."));
                    } catch (IOException e) {
                        TTT.showMessage("Cannot open file: " + e);
                    }
                }
            }
        });

        final JPanel recordingPathPanel = new JPanel();
        recordingPathPanel.setLayout(new BoxLayout(recordingPathPanel, BoxLayout.LINE_AXIS));
        recordingPathPanel.setOpaque(false);
        recordingPathPanel.add(recordingPathField);
        recordingPathPanel.add(Box.createHorizontalGlue());
        recordingPathPanel.add(recordingPathButton);

        // locak desktop (loopback connection)
        final JRadioButton localDesktopCheckBox = new JRadioButton("local desktop (default)");
        localDesktopCheckBox
                .setToolTipText("select if you want to record your local desktop (which is typically the case for Windows)");
        localDesktopCheckBox.setOpaque(false);
        localDesktopCheckBox.setSelected(true);
        localDesktopCheckBox.setEnabled(false);

        // remote desktop (or background server)
        final JRadioButton remoteDesktopCheckBox = new JRadioButton("remote VNC desktop:");
        remoteDesktopCheckBox.setToolTipText("select if you want to record a remote or background VNC desktop");
        remoteDesktopCheckBox.setOpaque(false);
        remoteDesktopCheckBox.setSelected(true);
        remoteDesktopCheckBox.setEnabled(false);

        ButtonGroup vncButtonGroup = new ButtonGroup();
        vncButtonGroup.add(localDesktopCheckBox);
        vncButtonGroup.add(remoteDesktopCheckBox);

        // VNC Server input fields for host and port
        final JTextField hostField = new JTextField("localhost", 12);
        hostField.setToolTipText("name or ip address of vnc server");
        hostField.setEnabled(false);

        final NumberField portField = new NumberField(5);
        portField.setText("5900");
        portField.setToolTipText("port number of vnc server");
        portField.setEnabled(false);

        final JPanel vncServerPanel = new JPanel();
        vncServerPanel.setLayout(new BoxLayout(vncServerPanel, BoxLayout.LINE_AXIS));
        vncServerPanel.setOpaque(false);
        vncServerPanel.add(remoteDesktopCheckBox);
        vncServerPanel.add(Box.createHorizontalStrut(3));
        vncServerPanel.add(hostField);
        vncServerPanel.add(Box.createHorizontalStrut(3));
        final JLabel hostPortDividerLabel = new JLabel(":");
        hostPortDividerLabel.setEnabled(false);
        vncServerPanel.add(hostPortDividerLabel);
        vncServerPanel.add(Box.createHorizontalStrut(3));
        vncServerPanel.add(portField);

        // simple or advanced options
        final JRadioButton loopbackCheckBox = new JRadioButton("loopback recorder");
        loopbackCheckBox.setToolTipText("suitable for most Windows users - records the current Windows desktop");
        loopbackCheckBox.setOpaque(false);
        loopbackCheckBox.setSelected(true);
        loopbackCheckBox.setEnabled(false);
        final JRadioButton advancedOptionsCheckBox = new JRadioButton("advanced options");
        advancedOptionsCheckBox.setOpaque(false);
        advancedOptionsCheckBox.setSelected(false);
        advancedOptionsCheckBox.setEnabled(false);
        ButtonGroup group = new ButtonGroup();
        group.add(loopbackCheckBox);
        group.add(advancedOptionsCheckBox);

        // display desktop and annotation tools
        // or record controls only (for loopback connections)
        final JCheckBox displayDesktopCheckBox = new JCheckBox("display vnc desktop");
        displayDesktopCheckBox
                .setToolTipText("disable if current desktop and recorded desktop are the same (which is typically the case for Windows)");
        displayDesktopCheckBox.setOpaque(false);
        displayDesktopCheckBox.setSelected(!true);
        displayDesktopCheckBox.setEnabled(false);

        // recorder
        final JCheckBox recorderCheckBox = new JCheckBox("enable recording");
        recorderCheckBox.setOpaque(false);
        recorderCheckBox.setSelected(true);
        recorderCheckBox.setEnabled(false);

        // video recording   
  
        final ttt.video.VideoSettingPanel CameraSettings;
        JButton CameraSettingsButton = new JButton("Camera Setting");
		CameraSettingsButton.setEnabled(false);
		
			CameraSettings = new ttt.video.VideoSettingPanel();
			CameraSettingsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					CameraSettings.show(true);
				}
			});
		
        
        
        // enable/disable according to recorder selection
        recorderCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                filenameField.setVisible(recorderCheckBox.isSelected());
                filenameLabel.setVisible(recorderCheckBox.isSelected());
            }
        });

        // listener for local and remote desktop buttons
        ActionListener selectDesktopListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean loopback = e.getSource() == localDesktopCheckBox;
                if (loopback) {
                    // set defaults for simple loopback recorder
                    hostField.setText("localhost");
                    portField.setText("5900");
                } else {
                    // get values from profile
                    LectureProfile profile = LectureProfile.getProfile((String) lectureComboBox.getSelectedItem());

                    // create new profile if required
                    // NOTE: maybe LectureProfile.getProfile() can be modified to never return null - check other
                    // occurences
                    if (profile == null)
                        profile = new LectureProfile((String) lectureComboBox.getSelectedItem());

                    hostField.setText(profile.getHost());
                    portField.setText("" + profile.getPort());
                }
                hostField.setEnabled(!loopback);
                portField.setEnabled(!loopback);
                hostPortDividerLabel.setEnabled(!loopback);
                displayDesktopCheckBox.setSelected(!loopback);

            }
        };
        localDesktopCheckBox.addActionListener(selectDesktopListener);
        remoteDesktopCheckBox.addActionListener(selectDesktopListener);

        // packing everything together
        final JPanel panel = new JPanel(new SpringLayout());
        panel.setOpaque(false);

        panel.add(new JLabel("Lecture:"));
        panel.add(lectureComboBox);

        JLabel label = new JLabel("Teacher:");
        label.setEnabled(false);
        panel.add(label);
        panel.add(teacherComboBox);

        label = new JLabel("Title:");
        label.setEnabled(false);
        panel.add(label);
        panel.add(titleField);

        if (do_not_show_recording_options_in_present_and_record_dialog) {
            panel.add(filenameLabel);
            panel.add(filenameField);

            panel.add(recordingPathLabel);
            panel.add(recordingPathPanel);
        }

        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));

        label = new JLabel("Desktop:");
        label.setEnabled(false);
        panel.add(label);
        panel.add(localDesktopCheckBox);
        panel.add(new JLabel(" "));
        panel.add(vncServerPanel);

        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));

        if (!do_not_show_recording_options_in_present_and_record_dialog) {
            label = new JLabel("Recorder:");
            label.setEnabled(false);
            panel.add(label);
            panel.add(recorderCheckBox);
            panel.add(new JLabel(" "));
          //  panel.add(recordVideoCheckBox);
                      
            panel.add(filenameLabel);
            panel.add(filenameField);
        }

        // audio format
        JLabel audioFormatLabel = new JLabel("Audio Format:");
        audioFormatLabel.setEnabled(false);

        final JRadioButton wavRadioButton = new JRadioButton("wav (default)");
        wavRadioButton.setToolTipText("record linear audio and transcode to mp3 later");
        wavRadioButton.setOpaque(false);
        wavRadioButton.setSelected(true);
        wavRadioButton.setEnabled(false);

        final JRadioButton mp3RadioButton = new JRadioButton("mp3");
        mp3RadioButton.setToolTipText("record mp3 audio - disabled because of compatibility problems");
        mp3RadioButton.setOpaque(false);
        mp3RadioButton.setSelected(false);
        mp3RadioButton.setEnabled(false);

        ButtonGroup audioButtonGroup = new ButtonGroup();
        audioButtonGroup.add(wavRadioButton);
        audioButtonGroup.add(mp3RadioButton);
        
        final JPanel audioFormatPanel = new JPanel();
        audioFormatPanel.setOpaque(false);
        audioFormatPanel.setLayout(new BoxLayout(audioFormatPanel, BoxLayout.X_AXIS));

        audioFormatPanel.add(audioFormatLabel);
        audioFormatPanel.add(Box.createHorizontalStrut(20));
        audioFormatPanel.add(wavRadioButton);
        audioFormatPanel.add(Box.createHorizontalStrut(20));
        audioFormatPanel.add(mp3RadioButton);

        final JLabel audioVolumeLabel = new JLabel("Volume:");

        // video recording
        final JRadioButton videoRecordingOnCheckbox = new JRadioButton("on");
        videoRecordingOnCheckbox.setToolTipText("enable video recoding");
        videoRecordingOnCheckbox.setOpaque(false);
        videoRecordingOnCheckbox.setSelected(true);
        videoRecordingOnCheckbox.setEnabled(false);

        final JRadioButton videoRecordingOffCheckbox = new JRadioButton("off");
        videoRecordingOffCheckbox.setToolTipText("enable video recoding");
        videoRecordingOffCheckbox.setOpaque(false);
        videoRecordingOffCheckbox.setSelected(true);
        videoRecordingOffCheckbox.setEnabled(false);

        ButtonGroup videoRecordingButtonGroup = new ButtonGroup();
        videoRecordingButtonGroup.add(videoRecordingOnCheckbox);
        videoRecordingButtonGroup.add(videoRecordingOffCheckbox);
        

        final JPanel videoRecordingPanel = new JPanel();
        videoRecordingPanel.setLayout(new BoxLayout(videoRecordingPanel, BoxLayout.Y_AXIS));
        videoRecordingPanel.setOpaque(false);
        videoRecordingPanel.setEnabled(false);

        videoRecordingPanel.add(Box.createVerticalStrut(5));

      final JLabel  videolabel = new JLabel("Video Recording:");
      videolabel.setEnabled(false);
        videoRecordingPanel.add(videolabel);
        videoRecordingPanel.add(videoRecordingOnCheckbox);
        videoRecordingPanel.add(videoRecordingOffCheckbox);
        videoRecordingPanel.add(CameraSettingsButton);

        videoRecordingPanel.add(audioFormatLabel);
        videoRecordingPanel.add(wavRadioButton);
        videoRecordingPanel.add(mp3RadioButton);
        
       

        videoRecordingPanel.add(Box.createVerticalStrut(5));

        final JLabel avLabel = new JLabel("Audio/Video:");
        avLabel.setEnabled(false);
        JPanel panel3 = new JPanel();
        panel3.setOpaque(false);
        panel3.add(avLabel);
        panel.add(panel3);

        JPanel avPanel = new JPanel();
        avPanel.setLayout(new BoxLayout(avPanel, BoxLayout.X_AXIS));
        avPanel.setOpaque(false);

        try {
            monitorPanel = new AudioVideoMonitorPanel();
            avPanel.add(monitorPanel);
        } catch (Exception e) {
            JLabel jlabel = new JLabel("Couldn't find Mic!");
            jlabel.setEnabled(false);
            avPanel.add(jlabel);
            System.out.println("Cannot open audio/video monitor: " + e);
        }
      
        avPanel.add(Box.createHorizontalGlue());
        avPanel.add(videoRecordingPanel);
        avPanel.add(Box.createHorizontalGlue());

        panel.add(avPanel);

        // Lay out the panel.
        SpringUtilities.makeCompactGrid(panel, panel.getComponentCount() / 2, 2, // rows, cols
                8, 8, // initX, initY
                12, 8); // xPad, yPad

        ActionListener optionModeListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (loopbackCheckBox.isSelected()) {
                    // set defaults for simple loopback recorder
                    hostField.setText("localhost");
                    portField.setText("5900");
                    displayDesktopCheckBox.setSelected(false);
                    recorderCheckBox.setSelected(true);
                    videoRecordingOnCheckbox.setSelected(true);
                    filenameField.setVisible(recorderCheckBox.isSelected());
                    filenameLabel.setVisible(recorderCheckBox.isSelected());
                }

                boolean enabled = advancedOptionsCheckBox.isSelected();

                Component[] components = panel.getComponents();
                for (int i = 0; i < components.length; i++) {
                    if (components[i] != titleField && components[i] != filenameField)
                        components[i].setEnabled(enabled);
                }
                components = vncServerPanel.getComponents();
                for (int i = 0; i < components.length; i++) {
                    components[i].setEnabled(enabled);
                }
                recordingPathButton.setEnabled(enabled);
                displayDesktopCheckBox.setEnabled(enabled);
                loopbackCheckBox.setEnabled(true);
                advancedOptionsCheckBox.setEnabled(true);
            }
        };
        loopbackCheckBox.addActionListener(optionModeListener);
        advancedOptionsCheckBox.addActionListener(optionModeListener);

        // buttons
        final JButton okButton = new JButton("ok");
        okButton.setEnabled(false);        
    
                
        JButton cancelButton = new JButton("cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            	CameraSettings.release();
            	CameraSettings.show(false);
                // close dialog
                if (monitorPanel != null)
                    monitorPanel.close();
                if (audioMonitorPanel != null)
                    audioMonitorPanel.stop();
                frame.dispose();
            }
        });

        JButton helpButton = new JButton("help");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String line = "----------------------------------------------------------------------------\n";

                String infoText = line
                        + "What is a Profile?\n"
                        + line
                        + "A LECTURE PROFILE contains informationen about a lecture ( e.g. teacher's name or recording info ).\n"
                        + "One profile is stored for each Lecture Title and all corresponding data will be loaded the next time\n"
                        + "you choose this lecture.\n\n";
                infoText += line + "How to create a Profile?\n" + line
                        + "Enter a unique Lecture Title and fill the form.\n"
                        + "NOTE: If the title is already in use, the corresponding profile will be loaded.\n\n";
                infoText += line + "What is a VNC Server?\n" + line + ""
                        + "A VNC Server gives remote access to a desktop environment.\n\n\n";

                TTT.showMessage(infoText, "TTT: Full Text Search - Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 40, 20));
        mainPanel.setOpaque(false);
        mainPanel.add(panel);

        JPanel topButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        topButtonPanel.setOpaque(false);
        topButtonPanel.add(helpButton);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomButtonPanel.setOpaque(false);
        bottomButtonPanel.add(okButton);
        bottomButtonPanel.add(cancelButton);

        JPanel panel2 = new GradientPanel();
        panel2.setLayout(new BorderLayout());
        panel2.add(topButtonPanel, BorderLayout.NORTH);
        panel2.add(mainPanel, BorderLayout.CENTER);
        panel2.add(bottomButtonPanel, BorderLayout.SOUTH);

        // show dialog
        frame.setContentPane(panel2);
        frame.pack();
        frame.setVisible(true);
        TTT.getInstance().addInternalFrameCentered(frame);

        // user input done - start
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    // load profile
                    // Note: profileSelected is used to distinguish between selection of an item and pressing enter for
                    // starting the slected profile
                    if (event.getSource().equals(lectureComboBox)
                            && !(profileSelected && (event.getModifiers() == 0 && event.getActionCommand()
                                    .equalsIgnoreCase("comboBoxEdited")))) {
                        // if(profileSelected && (event.getModifiers()==0 &&
                        // event.getActionCommand().equalsIgnoreCase("comboBoxEdited"))) break;
                        // modifier and edited: selection was done by mouse click
                        // no modifier and changed: selection was done by keyboard
                        // Note: no modifier and edited: selection by keyboard is IN PROGRESS (but unfinished)
                        profileSelected = (event.getModifiers() != 0 && event.getActionCommand().equalsIgnoreCase(
                                "comboBoxChanged"))
                                || (event.getModifiers() == 0 && event.getActionCommand().equalsIgnoreCase(
                                        "comboBoxEdited"));

                        // get profile
                        LectureProfile profile = LectureProfile.getProfile((String) lectureComboBox.getSelectedItem());

                        // set values
                        if (profile != null) {
                        	
                        	// CameraSettings.getCameraIDs().contains(profile.getRecordingCamera());
                        	
                        	
                            lectureComboBox.setSelectedItem(profile.getLecture());
                            teacherComboBox.setSelectedItem(profile.getTeacher());
                            hostField.setText(profile.getHost());
                            portField.setText("" + profile.getPort());

                            // local or remote connection
                            boolean loopback = profile.getHost().equalsIgnoreCase("localhost")
                                    && profile.getPort() == 5900;
                            if (loopback)
                                localDesktopCheckBox.setSelected(true);
                            else
                                remoteDesktopCheckBox.setSelected(true);
                            hostField.setEnabled(!loopback);
                            portField.setEnabled(!loopback);
                            hostPortDividerLabel.setEnabled(!loopback);
                            displayDesktopCheckBox.setSelected(!loopback);

                            recorderCheckBox.setSelected(profile.isRecordEnabled());                            
                           // recordVideoCheckBox.setSelected(profile.isRecordVideoEnabled());
                           // recordVideoCheckBox.setVisible(recorderCheckBox.isSelected());
                            if (CameraSettings!=null) if(CameraSettings.camerasFound()){
                            	videoRecordingOnCheckbox.setSelected(profile.isRecordVideoEnabled());
                            	videoRecordingOffCheckbox.setSelected(!profile.isRecordVideoEnabled());
                            	if( CameraSettings.getCameraIDs().contains(profile.getRecordingCamera()))
                            		CameraSettings.setRecordingCamera(profile.getRecordingCamera());
                            	CameraSettings.setRecordingFormat(profile.getVideoFormat()); 
                            	CameraSettings.setQuality(profile.getVideoQuality());
                            }
                            //Always use wave encoding in order to set profile.isRecordLinearAudioEnabled() to true
                            wavRadioButton.setSelected(true);	
                            mp3RadioButton.setSelected(false);
                            //wavRadioButton.setSelected(profile.isRecordLinearAudioEnabled());
                            //mp3RadioButton.setSelected(!profile.isRecordLinearAudioEnabled());
                            filenameField.setVisible(recorderCheckBox.isSelected());
                            filenameLabel.setVisible(recorderCheckBox.isSelected());
                        }
                        return;
                    }

                    // else

                    CameraSettings.release();                   
                    
                    // store profile
                    LectureProfile lectureProfile = LectureProfile.getProfile((String) lectureComboBox
                            .getSelectedItem());
                    if (lectureProfile == null)
                        lectureProfile = new LectureProfile((String) lectureComboBox.getSelectedItem());
                    
                    final LectureProfile profile = lectureProfile;

                    profile.setTeacher((String) teacherComboBox.getSelectedItem());
                    profile.setHost(hostField.getText());
                    profile.setPort(portField.getNumber() >= 100 ? portField.getNumber()
                            : (portField.getNumber() + 5900));
                    profile.setRecordEnabled(recorderCheckBox.isSelected());
                    profile.setRecordVideoEnabled(videoRecordingOnCheckbox.isSelected());
                    profile.setRecordLinearAudioEnabled(wavRadioButton.isSelected());
                    profile.setLoopbackRecorder(!displayDesktopCheckBox.isSelected());
                    
                  
                    	profile.setRecordingFormat(CameraSettings.getRecordingFormat());                    
                    	profile.setRecordingCamera(CameraSettings.getRecordingCamera());
                    	profile.setVideoQualiy(CameraSettings.getQuality());
                    	CameraSettings.show(false);
                    
                    
                    profile.storeProfile();
                    TTT.userPrefs.put("last_used_lecture_profile", profile.getLecture());

                    // close dialog
                    frame.dispose();

                    // start presenter
                    // NOTE: get out of event dispatching thread
                    new Thread(new Runnable() {
                        public void run() {
                            if (monitorPanel != null)
                                monitorPanel.close();
                            if (audioMonitorPanel != null)
                                audioMonitorPanel.stop();
                            TTT.createPresenter(profile);
                        }
                    }).start();

                } catch (Exception e) {
                    // close dialog
                    frame.dispose();

                    TTT.showMessage("Cannot open connection:\n" + "    Error: " + e);
                }
            }
        };
        okButton.addActionListener(actionListener);
        lectureComboBox.addActionListener(actionListener);
        lectureComboBox.requestFocusInWindow();

        // update title (while editing other fields)
        CaretListener caretListener = new CaretListener() {
            private boolean first = true;

            public void caretUpdate(CaretEvent event) {
                if (first) {
                    first = false;
                    loopbackCheckBox.setEnabled(true);
                    advancedOptionsCheckBox.setEnabled(true);
                    if (true) {
                        // TODO: useless if isSelected per default - REMOVE
                        Component[] components = panel.getComponents();
                        for (int i = 0; i < components.length; i++) {
                            components[i].setVisible(true);
                            if (components[i] != titleField && components[i] != filenameField)
                                components[i].setEnabled(true);
                        }
                        components = audioFormatPanel.getComponents();
                        for (int i = 0; i < components.length; i++) {
                            components[i].setEnabled(true);
                        }
                        
                        if(CameraSettings.camerasFound()){
                        	components = videoRecordingPanel.getComponents();
                        	for (int i = 0; i < components.length; i++) {
                        		components[i].setEnabled(true);
                        }
                        }else{
                        	videolabel.setText("No Camera available");                        	
                        	videoRecordingOffCheckbox.setSelected(true);
                        	
                        }
                        
                        avLabel.setEnabled(true);
                        audioVolumeLabel.setEnabled(true);
                        recordingPathButton.setEnabled(true);
                        localDesktopCheckBox.setEnabled(true);
                        remoteDesktopCheckBox.setEnabled(true);
                        displayDesktopCheckBox.setEnabled(true);
                        teacherComboBox.setEditable(true);
                        mp3RadioButton.setEnabled(false);	//disabled because of compatibility problems
                    }
                }
                okButton.setEnabled(true);

                // NOTE: selected item will be updated AFTER editing, not while editing
                if (((JTextField) event.getSource()).getParent() == lectureComboBox) {
                    profileSelected = false;

                    // set default title
                    titleField.setText(LectureProfile.getDefaultTitle(((JTextField) event.getSource()).getText(),
                            (String) teacherComboBox.getSelectedItem()));
                    // set default filename
                    filenameField
                            .setText(LectureProfile.getDefaultFilename(((JTextField) event.getSource()).getText()));
                } else {
                    // set default title
                    titleField.setText(LectureProfile.getDefaultTitle((String) lectureComboBox.getSelectedItem(),
                            ((JTextField) event.getSource()).getText()));
                    // set default filename
                    filenameField
                            .setText(LectureProfile.getDefaultFilename((String) lectureComboBox.getSelectedItem()));
                }

            }
        };
        ((JTextField) lectureComboBox.getEditor().getEditorComponent()).addCaretListener(caretListener);
        ((JTextField) teacherComboBox.getEditor().getEditorComponent()).addCaretListener(caretListener);
    }
}
