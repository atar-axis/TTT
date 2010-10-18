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
 * Created on 02.01.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.DeflaterOutputStream;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import ttt.audio.AudioRecorder;
import ttt.gui.GradientPanel;
import ttt.gui.RollOverButton;
import ttt.messages.Annotation;
import ttt.messages.DeleteAllAnnotation;
import ttt.messages.Message;
import ttt.messages.MessageConsumer;
import ttt.messages.WhiteboardMessage;
import ttt.record.LectureProfile;
import ttt.video.*;


public class Recorder implements MessageConsumer, Closeable {

    private RfbProtocol protocol;
    // NOTE: out==null: ready to record / out!=null: recording
    private DataOutputStream out;

    private AudioRecorder audioVideoRecorder;
    private VideoRecorderPanel VideoRecorder;
    
    private LectureProfile lectureProfile;

    public Recorder(RfbProtocol protocol, LectureProfile lectureProfile) throws IOException {
        this.protocol = protocol;
        this.lectureProfile = lectureProfile;

        showRecordPlayRecordWarningDialog(lectureProfile);

        // initialize audio and video
        if (lectureProfile != null){
            audioVideoRecorder = new AudioRecorder();
        
            if(lectureProfile.isRecordVideoEnabled()){
              	VideoRecorder = new VideoRecorderPanel(lectureProfile.getRecordingCamera(), lectureProfile.getVideoFormat(),lectureProfile.getVideoQuality(),/*file.getCanonicalPath().substring(0, file.getCanonicalPath().length()-4)*/null);          
        	}
        }
        // register for shutdown
        register(this);
        // TODO: unregister
    }

    private boolean closing;

    public void close() {
        closing = true;
        try {
            stopRec();
        } catch (IOException e) {
            System.out.println("Closing Recorder failed: " + e);
            e.printStackTrace();
        }

        if (audioVideoRecorder != null)
            audioVideoRecorder.close();
        audioVideoRecorder = null;

        if(VideoRecorder != null) {
     	VideoRecorder.close();     
     	}          
    	VideoRecorder = null;  
        
        if (protocol != null)
            protocol.close();
        protocol = null;
    }

    // create recorder control elements
    public Component getControls() {    	
        return getControls_3Buttons();
    }

    // controls with combine rec/stop button and play button
    public Component getControls_2Buttons() {
        // create control panel
        JPanel controlPanel;

        final String RECORD = "record";
        final String STOP = "stop";
        final String PLAY = "play";

        final Icon record_icon = Constants.getIcon("Record24.gif");
        final Icon stop_icon =Constants.getIcon("esources/Stop24.gif");

        final JButton recordButton = new RollOverButton(record_icon, RECORD);
        final JButton playButton = new RollOverButton(Constants.getIcon("Play24.gif"), PLAY);

        recordButton.setEnabled(out == null);
        playButton.setEnabled(false);

        // avoid focus loss (of main component) in java 1.6
        recordButton.setFocusable(false);
        playButton.setFocusable(false);

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    if (event.getActionCommand().equals(RECORD)) {
                        startRec();
                        recordButton.setIcon(stop_icon);
                        recordButton.setActionCommand(STOP);
                        playButton.setEnabled(false);
                    } else if (event.getActionCommand().equals(STOP)) {
                        stopRec();
                        recordButton.setIcon(record_icon);
                        recordButton.setActionCommand(RECORD);
                        playButton.setEnabled(true);
                    } else if (event.getActionCommand().equals(PLAY)) {
                        // run outside of event dispatching thread
                        new Thread(new Runnable() {
                            public void run() {
                                TTT.createFilePlayer(file);
                            }
                        }).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    TTT.showMessage("Recording failed", "TTT Recorder Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        recordButton.addActionListener(actionListener);
        playButton.addActionListener(actionListener);

        controlPanel = new GradientPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

        controlPanel.add(recordButton);
        controlPanel.add(playButton);
        return controlPanel;
    }

    // controls with Rec, Stop, Play buttons
    public Component getControls_3Buttons() {
        // create control panel
        JPanel controlPanel;

        final String RECORD = "start recording";
        final String STOP = "stop recording";
        final String PLAY = "play";

        final JButton recordButton = new RollOverButton(Constants.getIcon("Record24.png"), RECORD);
        final JButton stopButton = new RollOverButton(Constants.getIcon("Stop24.gif"), STOP);
        final JButton playButton = new RollOverButton(Constants.getIcon("Play24.gif"), PLAY);
              
        recordButton.setEnabled(out == null);
        stopButton.setEnabled(out != null);
        playButton.setEnabled(false);     
        
        // avoid focus loss (of main component) in java 1.6
        recordButton.setFocusable(false);
        stopButton.setFocusable(false);
        playButton.setFocusable(false);      
        
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    if (event.getActionCommand().equals(RECORD)) {
                        playButton.setEnabled(false);
                        recordButton.setEnabled(false);
                        startRec();
                        stopButton.setEnabled(true);
                    } else if (event.getActionCommand().equals(STOP)) {
                        stopButton.setEnabled(false);
                        stopRec();
                        recordButton.setEnabled(true);
                        playButton.setEnabled(true);
                    } else if (event.getActionCommand().equals(PLAY)) {
                        showRecordPlayRecordWarningDialog(lectureProfile);

                        // run outside of event dispatching thread
                        new Thread(new Runnable() {
                            public void run() {
                                TTT.createFilePlayer(file);
                            }
                        }).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    TTT.showMessage("Recording failed", "TTT Recorder Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        recordButton.addActionListener(actionListener);
        stopButton.addActionListener(actionListener);
        playButton.addActionListener(actionListener);

        controlPanel = new GradientPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

        JComponent volumeLevelComponent = audioVideoRecorder.getVolumeLevelComponent();
        if (volumeLevelComponent != null)
            controlPanel.add(volumeLevelComponent);
        controlPanel.add(recordButton);
        controlPanel.add(stopButton);
        controlPanel.add(playButton);
      
        return controlPanel;
    }

    
    //Creates a show/hide button for the videocamera
	public Component getVideoControls() {

		final String WEBCAM = "Hide Webcam";
      
		  final JButton hideButton = new RollOverButton(Constants.getIcon("Webcam24.png"), WEBCAM);
		  hideButton.setEnabled(true);
		  hideButton.setEnabled(true);		  
		  JPanel controlPanel = (JPanel) getControls_3Buttons();
		  
	        hideButton.addActionListener(new ActionListener() {	  
	        	
		            public void actionPerformed(ActionEvent event) {
		            	if(VideoRecorder != null){
		            		if(VideoRecorder.isVisible()){
		            			VideoRecorder.setVisible(false);	
		            			hideButton.setToolTipText("Show Webcam");
		            		} else {
		            			VideoRecorder.setVisible(true);
		            			hideButton.setToolTipText(WEBCAM);
		            		}
		            	
		            		
		            	}
		            }
		        });
		  controlPanel.add(hideButton);
		  
		return controlPanel;
	}
    
    // create controls for loopback recorder (recorder runs on recorder desktop)
    public Component getLoopbackControls() {
        // create control panel
        JPanel controlPanel;

        final String RECORD = "record";
        final String STOP = "stop";

        final Icon record_icon = Constants.getIcon("Record24.gif");
        final Icon stop_icon = Constants.getIcon("Stop24.gif");

        final JButton recordButton = new RollOverButton(record_icon, RECORD);

        recordButton.setEnabled(out == null);

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    if (event.getActionCommand().equals(RECORD)) {
                        startRec();
                        recordButton.setIcon(stop_icon);
                        recordButton.setActionCommand(STOP);
                    } else if (event.getActionCommand().equals(STOP)) {
                        stopRec();
                        recordButton.setIcon(record_icon);
                        recordButton.setActionCommand(RECORD);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    TTT.showMessage("Recording failed", "TTT Recorder Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        recordButton.addActionListener(actionListener);

        final String ZOOM_OUT = "minimize";
        final String ZOOM_IN = "maximize";

        final Icon zoom_in_icon = Constants.getIcon("ZoomIn24.gif");
        final Icon zoom_out_icon = Constants.getIcon("ZoomOut24.gif");

        final JButton zoomButton = new RollOverButton(zoom_in_icon, ZOOM_IN);
        zoomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(ZOOM_IN)) {
                    TTT.getInstance().setVisible(true);
                    zoomButton.setIcon(zoom_out_icon);
                    zoomButton.setActionCommand(ZOOM_OUT);
                } else if (e.getActionCommand().equals(ZOOM_OUT)) {
                    TTT.getInstance().setVisible(false);
                    zoomButton.setIcon(zoom_in_icon);
                    zoomButton.setActionCommand(ZOOM_IN);
                }
            }
        });

        controlPanel = new GradientPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

        controlPanel.add(recordButton);
        controlPanel.add(zoomButton);
        return controlPanel;
    }

    private File file;

    synchronized public void startRec(File file) throws IOException {
        this.file = file;

        // reset timestamp inconsistency check
        lastTimestamp = -1;

        System.out.println("\nRecorder start.");
        System.out.println("    Recording desktop to '" + file.getCanonicalPath() + "'");

        // modified by Ziewer - 10.05.2007
        //
        // WAS
        // // TODO: sync start - maybe start after ttt header is written but BEFORE startime is written
        // if (audioVideoRecorder != null)
        // audioVideoRecorder.startRec(file.getCanonicalPath());
        //
        // // prepare recording
        // writeHeader(file.getCanonicalPath());
        // END WAS

        // prepare recording
        DataOutputStream new_out = writeHeader(file.getCanonicalPath());

        // NOTE: sync start - starts after ttt header is written but BEFORE startime will be written
        // TODO: test if sync better???
        if (audioVideoRecorder != null)
            audioVideoRecorder.startRec(file.getCanonicalPath());

        //VideoRec start       
    if(lectureProfile.isRecordVideoEnabled()){
    	VideoRecorder.setRecordpath(file.getCanonicalPath().substring(0, file.getCanonicalPath().length()-4));
        VideoRecorder.start();
    }
     
        
        // startime
        long starttime = System.currentTimeMillis();
        new_out.writeLong(starttime);

        // offset to adjust timestamps
        offset = (int) (protocol.getStarttime() - starttime);

        new_out.flush();

        // make visible for message handler
        out = new_out;
        // end of modification by Ziewer - 10.05.2007

        // NOTE: end of header

        // NOTE: now start with messages

        // write initial DeleteAll message with initial zero timestamp
        new DeleteAllAnnotation(0).write(out, 0);

        // set whiteboard status
        // not needed if (first) desktop enabled
        Message message = protocol.getCurrentWhiteboardMessage();
        if (message != null && ((WhiteboardMessage) message).getPageNumber() != 0)
            message.write(out, Message.NO_TIMESTAMP);

        // set cursor shape and position
        message = protocol.getCurrentCursorMessage();
        if (message != null) {
            // write buffered cursor shape
            message.write(out, Message.NO_TIMESTAMP);
            // write buffered cursor position
            protocol.getCurrentCursorPositionMessage().write(out, Message.NO_TIMESTAMP);
        }

        // write buffered annotations
        Annotation[] annotations = protocol.getCurrentAnnotationsAsArray();
        for (int i = 0; i < annotations.length; i++)
            annotations[i].write(out, Message.NO_TIMESTAMP);

        // start recording
        protocol.addMessageConsumer(this);

        // request full screen update
        protocol.requestFullscreenUpdate();
    }

    synchronized public void startRec() throws IOException {
        // get filename
        file = null;
        if (file == null)
            file = new File(TTT.userPrefs.get("record_path", ".") + File.separator + lectureProfile.getFilename());

        file = new File(getValidFilename(file.getCanonicalPath()) + Constants.desktopEndings[0]);
        startRec(file);
    }

    synchronized public void stopRec() throws IOException {
        if (out != null) {
            // write final timestamp
            // NOTE: send to protocol to get correct timestamp
            // TODO: could use own STOP_REC_Encoding instead (implicit close in handleMessage)
            protocol.handleMessage(new DeleteAllAnnotation(0));

            if (protocol != null)
                protocol.removeMessageConsumer(this);

            if (audioVideoRecorder != null) {
                if (closing) {
                    audioVideoRecorder.close();
                    audioVideoRecorder = null;
                } else
                    audioVideoRecorder.stopRec();
            }

           
            if (VideoRecorder != null) {
                if (closing) {            
                	
                	VideoRecorder.close();                
                	VideoRecorder = null;   
                	
                } else{                	
                	
                    VideoRecorder.stop();
                    }
            }            
     
            
            
            out.flush();
            out.close();
            out = null;
        }
            TTT.userPrefs.put("last_opened_recording", file.getCanonicalPath());

            System.out.println("Recorder stop.");
        }
    

    // modified by Ziewer - 10.05.2007
    // NOTE: writes file header WITHOUT starttime
    // starttime MUST be written (is done after audio/video recorder has been started)
    //
    // synchronized private void writeHeader(String filename) throws IOException {
    synchronized private DataOutputStream writeHeader(String filename) throws IOException {
        File file = new File(filename);
        if (file.exists())
            throw new IOException("file exists " + file.getCanonicalPath());

        FileOutputStream fileOut = new FileOutputStream(file);
        DataOutputStream out = new DataOutputStream(fileOut);

        // write header
        //
        // write version message
        out.write(Constants.VersionMessageTTT.getBytes());
        // write compressed messages to file
        out = new DataOutputStream(new DeflaterOutputStream(fileOut));

        writeInit(out);

        // no extensions
        out.writeInt(0);

        // modified by Ziewer - 10.05.2007
        //
        // WAS
        // // startime
        // long starttime = System.currentTimeMillis();
        // out.writeLong(starttime);
        //
        // // offset to adjust timestamps
        // offset = (int) (protocol.getStarttime() - starttime);
        //
        // out.flush();
        //
        // // make visible for message handler
        // this.out = out;
        // END WAS
        //
        out.flush();
        return out;
        // end of modification by Ziewer - 10.05.2007
    }

    // initialization
    synchronized private void writeInit(DataOutputStream out) throws IOException {
        // write protocol initialisation
        out.writeShort(protocol.prefs.framebufferWidth);
        out.writeShort(protocol.prefs.framebufferHeight);
        out.writeByte(protocol.prefs.bitsPerPixel);
        out.writeByte(protocol.prefs.depth);
        out.writeByte(protocol.prefs.bigEndian ? 1 : 0);
        out.writeByte(protocol.prefs.trueColour ? 1 : 0);
        out.writeShort(protocol.prefs.redMax);
        out.writeShort(protocol.prefs.greenMax);
        out.writeShort(protocol.prefs.blueMax);
        out.writeByte(protocol.prefs.redShift);
        out.writeByte(protocol.prefs.greenShift);
        out.writeByte(protocol.prefs.blueShift);
        out.writeByte(0); // padding
        out.writeByte(0);
        out.writeByte(0);
        // NOTE: String.length() not always equals String.getBytes().length
        // depending on the system's character encoding (Umlauts may fail)
        // out.writeInt(protocol.prefs.name.length());
        out.writeInt(protocol.prefs.name.getBytes().length);
        out.write(protocol.prefs.name.getBytes());
    }

    private int lastTimestamp = -1;
    private int offset;

    // write body
    // TODO: maybe just buffer messages here and run handling in own Thread
    synchronized public void handleMessage(Message message) {
        if (out == null)
            return;

        try {
            // fix timestamp
            int timestamp = message.getTimestamp() + offset;

            // drop buffered but outdated message
            if (timestamp < 0)
                return;

            // NOTE: slightly delays can occur (typically less than 100 msec)
            if (timestamp + 500 < lastTimestamp)
                System.out.println("WARNING: inconsistent timestamp [diff "
                        + Constants.getStringFromTime(lastTimestamp - timestamp) + "]+\n\t" + message);

            // fix inconsistent timestamps
            if (timestamp < lastTimestamp)
                timestamp = lastTimestamp;

            // write message (with timestamp if needed)
            if (timestamp == lastTimestamp)
                message.write(out, Message.NO_TIMESTAMP);
            else
                message.write(out, timestamp);

            lastTimestamp = timestamp;

            // force writting
            out.flush();

        } catch (IOException e) {
            // TODO error handling
            System.out.println("writing failed:");
            System.out.println("\tmessage: " + message);
            System.out.println("\terror: " + e);
            e.printStackTrace();
        }
    }

    // ///////////////////////////////////////////////////////
    // filename magic
    // //////////////////////////////////////////////////////
    // TODO: looks confusing - rework this

    static String getValidFilename(String file) {
        if (file.endsWith(Constants.desktopEndings[0]))
            file = file.substring(0, file.length() - 4);

        String path = file.substring(0, file.lastIndexOf(File.separator));
        String name = file.substring(file.lastIndexOf(File.separator) + 1);
        return path + File.separator + getFileName(new File(path), name);
    }

    // generates filenames by appending the date + a number (if necessary)
    // used to propose filenames when recording

    static String getFileName(File path, String base) {
        return getFileName(path, base, null);
    }

    static String getFileName(File path, String base, String ending) {
        // format of return filename: <lecture_name>_<date>-<sequence_number>.<File_ending>
        // note that sequence number is optional, i.e. will be omitted if zero
        // e.g.
        // Compilerbau_2006_12_24.ttt
        // Compilerbau_2006_12_24-1.ttt
        // Compilerbau_2006_12_24-2.ttt

        Calendar c = Calendar.getInstance();

        // create name buffer
        StringBuffer base1 = new StringBuffer();

        if ((base != null) && !base.equals("")) {
            base1.append(base);

            // remove previous sequence number
            int j = base.lastIndexOf("-");
            if (j >= 0)
                try {
                    // check if sequence number
                    Integer.parseInt(base.substring(j + 1, base1.length()));
                    // remove sequence number
                    base1.delete(j, base1.length());
                } catch (NumberFormatException e) {
                    // no sequence number - do not remove
                }

        } else {
            base1.append(c.get(Calendar.YEAR));

            base1.append("_");

            int month = c.get(Calendar.MONTH) + 1;
            // Jan=0
            // ..
            // Dec=11
            if (month < 10)
                base1.append(0);
            base1.append(month);

            base1.append("_");

            int day = c.get(Calendar.DAY_OF_MONTH);
            if (day < 10)
                base1.append(0);
            base1.append(day);
        }

        int i = 1;
        StringBuffer newBase = new StringBuffer();
        newBase.append(base1.toString());

        while (checkFileName(path.getPath(), newBase.toString(), ending) != null) {
            newBase.setLength(0);
            newBase.append(base1.toString());
            newBase.append("-");
            newBase.append(i);
            i++;
        }

        return newBase.toString();
    }

    static String[] checkFileName(String path, String name) {
        return checkFileName(path, name, true, null);
    }

    static String[] checkFileName(String path, String name, String ending) {
        if (ending != null)
            return checkFileName(path, name, false, ending);
        else
            return checkFileName(path, name, true,  null);
    }

    static String[] checkFileName(String path, String name, boolean desktop, String ending) {
        File ttt = new File(path, name + Constants.desktopEndings[0]);
        File custom = null;
        if (ending != null)
            custom = new File(path, name + ending);

        String retTtt = null, retMp3 = null, retMov = null, retCustom = null;
        int c = 0;

        if (desktop && ttt.exists()) {
            retTtt = ttt.getName();
            c++;
        }
        if (ending != null && custom.exists()) {
            retCustom = custom.getName();
            c++;
        }

        String[] ret = null;

        if (c > 0) {
            ret = new String[c];
            c = 0;

            if (retTtt != null) {
                ret[c] = retTtt;
                c++;
            }
            if (retMp3 != null) {
                ret[c] = retMp3;
                c++;
            }
            if (retMov != null) {
                ret[c] = retMov;
                c++;
            }
            if (retCustom != null) {
                ret[c] = retCustom;
                c++;
            }
        }

        return ret;
    }

    // /////////////////////////////////////////////////////
    // Shutdown handling
    // /////////////////////////////////////////////////////

    // call stop if programm is closed
    static private ArrayList<Recorder> recorders = new ArrayList<Recorder>();
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < recorders.size(); i++) {
                    try {
                        recorders.get(i).stopRec();
                    } catch (IOException e) {
                        System.out.println("Faild to close recorder: " + e);
                        e.printStackTrace();
                    }
                }
                recorders.clear();
            }
        }));
    }

    static void register(Recorder recorder) {
        recorders.add(recorder);
    }

    static boolean showRecordPlayRecordWarningDialog(LectureProfile lectureProfile) {
        // return true;

        if (lectureProfile.isShowRecordPlayRecordWarning()) {
            // display information about recording controls
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            infoPanel.add(new JLabel("TTT Recorder Warning:"));
            infoPanel.add(new JLabel(" "));
            infoPanel
                    .add(new JLabel("You should check if your system will record properly AFTER replaying a session!"));
            infoPanel.add(new JLabel(" "));
            infoPanel.add(new JLabel("     On some systems the audio device is blocked after replaying any"));
            infoPanel.add(new JLabel("     audio within the TTT. In this case the TTT Recorder will not record"));
            infoPanel.add(new JLabel("     any audio afterwards. The TTT Recorder will not show any error"));
            infoPanel.add(new JLabel("     message during recording. However, later replay will fail due to"));
            infoPanel.add(new JLabel("     an empty audio file. [ detected under Linux with JMF2.1.1a ]"));
            infoPanel.add(new JLabel(" "));
            infoPanel.add(new JLabel("Workaround: Restart the TeleTeachingTool before recording."));

            infoPanel.add(new JLabel(" "));
            JCheckBox showAgainCheckBox = new JCheckBox("Don't show this message again.");
            infoPanel.add(showAgainCheckBox);

            JOptionPane.showMessageDialog(TTT.getInstance(), infoPanel, "TTT Recorder", JOptionPane.WARNING_MESSAGE);

            if (showAgainCheckBox.isSelected()) {
                lectureProfile.setShowRecordPlayRecordWarning(false);
                lectureProfile.storeProfile();
            }
        }
        return true;
    }


}
