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
 * Created on 05.01.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.record;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;

import ttt.Constants;
import ttt.ProtocolPreferences;
import ttt.TTT;
import ttt.audio.AudioVideoPlayer;
import ttt.audio.VolumeControl;
import ttt.gui.GraphicsContext;
import ttt.gui.Index;
import ttt.gui.IndexEntry;
import ttt.gui.IndexViewer;
import ttt.messages.Message;
import ttt.messages.MessageProducerAdapter;
import ttt.player.PlaybackControls;
import ttt.player.TimeSlider.MyChangeEvent;
import ttt.postprocessing.ScriptCreator;
import ttt.postprocessing.flash.FlashContext;

public class Recording extends MessageProducerAdapter implements Runnable, ActionListener, Closeable, VolumeControl {
    // TODO: visibility
    public ProtocolPreferences prefs = new ProtocolPreferences();
    public GraphicsContext graphicsContext;

    public Messages messages = new Messages(this);

    public AudioVideoPlayer audioVideoPlayer;

    public File fileDesktop;

    public Recording(File file) throws IOException {
        this(file.getCanonicalPath());
    }

    public Recording(File file, boolean loadAudioVideoStreams) throws IOException {
        this(file.getCanonicalPath(), loadAudioVideoStreams);
    }

    public Recording(String filename) throws IOException {
        this(filename, true);
    }

    public Messages getMessages(){
    	return messages;
    }
    
    public void setfileDesktop(File name){
    	fileDesktop = name;
    }
    
    public void setMessages(ArrayList<Message> list){
    	messages.setmessages(list);
    }
    
    public Recording(String filename, boolean loadAudioVideoStreams) throws IOException {
        // read
        read(filename);

        if (TTT.verbose)
            messages.statistics();

        graphicsContext = new GraphicsContext(this);

        // Note: keyframes are computed dynamically during random access
        // TODO: add keyframes if dynamic computation is too slow - e.g. if border stays unchanged over long periods
        // if (!true) {
        // index.computeKeyframes(Index.COLLECT_MESSAGES);
        // // indexExtension.computeKeyframes(Index.RAW_IN_ARRAY);
        // // indexExtension.computeKeyframes(Index.RAW_IN_OFFSCREEN_IMAGE);
        // // indexExtension.computeKeyframes(Index.PAINT_TO_OFFSCREEN_IMAGE);
        // }

        // TODO: audio/video may not bee needed for batch processing (e.g. script generation)
        if (loadAudioVideoStreams)
            audioVideoPlayer = new AudioVideoPlayer(filename, this);
        else
            System.out.println("batch mode - not loading audio/video streams");

        // TODO: fixDuration();
        if (audioVideoPlayer != null) {
            int desktop_duration = messages.get(messages.size() - 1).getTimestamp();
            int audio_duration = audioVideoPlayer.getAudioDuration();
            int video_duration = audioVideoPlayer.getVideoDuration();

            int max_duration = Math.max(desktop_duration, Math.max(audio_duration, video_duration));
            int desktop_diff = desktop_duration - max_duration;
            int audio_diff = audio_duration - max_duration;
            int video_diff = video_duration - max_duration;
            if (TTT.verbose) {
                System.out.println("desktop duration:\t" + Constants.getStringFromTime(desktop_duration) + "\tdiff "
                        + Constants.getStringFromTime(desktop_diff));
                System.out.println("audio duration:\t\t"
                        + Constants.getStringFromTime(audioVideoPlayer.getAudioDuration()) + "\tdiff "
                        + Constants.getStringFromTime(audio_diff));
                System.out.println("video duration:\t\t"
                        + (audioVideoPlayer.getVideoDuration() == 0 ? "NO VIDEO" : Constants
                                .getStringFromTime(audioVideoPlayer.getVideoDuration())
                                + "\tdiff " + Constants.getStringFromTime(video_diff)));
            }

            // calculate replay factor for badly synced audio recordings (audio too short)
            desktop_replay_factor = 1;
            if (audio_duration > 0) {
                desktop_replay_factor = (double) audio_duration / max_duration;
                if (TTT.verbose)
                    System.out.println("Desktop replay speed set to " + desktop_replay_factor);
                desktop_replay_factor_sync = audio_diff < 10000;

                audioVideoPlayer.setReplayRatio(1 / desktop_replay_factor);
            }

            messages.checkDuration(audioVideoPlayer.getDuration());
        }
    }

    public GraphicsContext getGraphicsContext() {
        return graphicsContext;
    }

    public String getFileName() throws IOException {
        return fileDesktop.getCanonicalPath();
    }

    public String getAudioFilename() throws IOException {
        if (audioVideoPlayer != null)
            return audioVideoPlayer.getAudioFilename();
        else
            return Constants.getExistingFile(getFileName(), Constants.AUDIO_FILE).getCanonicalPath();
    }

    public String getVideoFilename() throws IOException {
        if (audioVideoPlayer != null)
            return audioVideoPlayer.getVideoFilename();
        else
            return Constants.getExistingFile(getFileName(), Constants.VIDEO_FILE).getCanonicalPath();

    }
    
    public File getFileBySuffix(String suffix) throws IOException {
    	return new File(getDirectory() + getFileBase() + "." + suffix);
    }
    
    public File getExistingFileBySuffix(String suffix) throws IOException {
    	File file = getFileBySuffix(suffix);
    	return file.exists() ? file : getFileBySuffix(suffix.toUpperCase());
    }
    
    public File getExistingFileBySuffix(String[] suffix) throws IOException {
    	
    	for (String curSuffix: suffix) {
    		File file = getFileBySuffix(curSuffix);
    		if (file.exists()) {
    			return file;
    		}
    	}
    	return getFileBySuffix(suffix[0]);
    }

    void read(String filename) throws IOException {
        // open file
        fileDesktop = Constants.getExistingFile(filename, Constants.DESKTOP_FILE);
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(fileDesktop)));

        // read version
        byte[] b = new byte[12];
        in.readFully(b);

        // TODO: test version
        if (TTT.verbose)
            System.out.println("File Version: " + new String(b));
        prefs.versionMsg = new String(b);

        // read compressed data
        in = new DataInputStream(new BufferedInputStream(new InflaterInputStream(in)));

        // read init parameters
        readServerInit(in, prefs);

        if (TTT.verbose)
            System.out.println(prefs);

        // read and parse all known extensions
        // e.g. index table, thumbnails, searchbase
        readExtensions(in);

        // read time of recording
        prefs.starttime = in.readLong();

        // show preferences of recording
        if (TTT.verbose)
            System.out.println(prefs);

        // read body of recording
        messages.readMessages(in);

        // ensure index is read from extension or computed
        if (!index.isValid())
            index.computeIndex();

        // gather (future) annotations for thumbnail and script generator
        index.extractAnnotations();

        // read search bases
        // TODO: upercase ending??
        readSearchbaseFromFile();
    }

    public boolean readSearchbaseFromFile(String filename) {
        if (filename.endsWith(".xml") || filename.endsWith(".XML"))
            return index.readSearchBaseFromFileXML(filename);
        if (filename.endsWith(".txt") || filename.endsWith(".TXT"))
            return index.readSearchBaseFromFileTXT(filename);
        return false;
    }

    public boolean readSearchbaseFromFile() {
        try {
            File file = Constants.getExistingFile(this.fileDesktop.getCanonicalPath(), Constants.SEARCHBASE_FILE);
            if (file == null)
                return false;

            return readSearchbaseFromFile(file.getCanonicalPath());
        } catch (IOException e) {}

        return false;
    }

    public ProtocolPreferences getProtocolPreferences() {
        return prefs;
    }

    // returns a component displaying the video
    public Component getVideoComponent() {
        if (audioVideoPlayer != null)
            return audioVideoPlayer.getVideo();
        else
            return null;
    }

    public IndexViewer indexViewer;

    // returns a component displaying the thumbnail overview
    public Component getIndexComponent() {
        if (indexViewer == null) {
            // create thumbnail overview
            indexViewer = new IndexViewer(this);
        }
        return indexViewer;
    }

    public String getDirectory() throws IOException {
        int pos = fileDesktop.getCanonicalPath().lastIndexOf(File.separator) + 1;
        if (pos > 0 && pos < fileDesktop.getCanonicalPath().length()) {
            return fileDesktop.getCanonicalPath().substring(0, pos);
        } else {
            return fileDesktop.getCanonicalPath();
        }
    }

    public String getFileBase() throws IOException {
        int pos = fileDesktop.getCanonicalPath().lastIndexOf(File.separator) + 1;
        if (pos > 0 && pos < fileDesktop.getCanonicalPath().length()) {
            return fileDesktop.getCanonicalPath().substring(pos, fileDesktop.getCanonicalPath().length() - 4);
        } else {
            return fileDesktop.getCanonicalPath().substring(0, 4);
        }
    }

    public void close() {
        // stop thread
        if (thread != null && thread.isAlive()) {
            // end Thread
            running = false;

            // leave wait()
            synchronized (this) {
                notify();
            }

            // wait until finished to avoid exceptions
            while (thread != null && thread.isAlive())
                Thread.yield();
        }

        // close everything and free memory
        if (playbackControls != null)
            playbackControls.dispose();
        playbackControls = null;

        if (graphicsContext != null)
            graphicsContext.close();
        graphicsContext = null;

        if (index != null)
            index.close();
        index = null;

        if (messages != null)
            messages.close();
        messages = null;

        if (audioVideoPlayer != null)
            audioVideoPlayer.close();
        audioVideoPlayer = null;
    }

    /*******************************************************************************************************************
     * Initialisation *
     ******************************************************************************************************************/

    // read server initialisation
    static private void readServerInit(DataInputStream in, ProtocolPreferences prefs) throws IOException {
        prefs.framebufferWidth = in.readUnsignedShort();
        prefs.framebufferHeight = in.readUnsignedShort();
        prefs.bitsPerPixel = in.readUnsignedByte();
        switch (prefs.bitsPerPixel) {
        case 8:
            prefs.bytesPerPixel = 1;
            break;
        case 16:
            prefs.bytesPerPixel = 2;
            break;
        default:
            prefs.bytesPerPixel = 4;
            break;
        }
        prefs.depth = in.readUnsignedByte();
        prefs.bigEndian = (in.readUnsignedByte() != 0);
        prefs.trueColour = (in.readUnsignedByte() != 0);
        prefs.redMax = in.readUnsignedShort();
        prefs.greenMax = in.readUnsignedShort();
        prefs.blueMax = in.readUnsignedShort();
        prefs.redShift = in.readUnsignedByte();
        prefs.greenShift = in.readUnsignedByte();
        prefs.blueShift = in.readUnsignedByte();
        // padding
        in.skipBytes(3);
        int nameLength = in.readInt();
        byte[] name = new byte[nameLength];
        in.readFully(name);
        prefs.name = new String(name);
    }

    /*******************************************************************************************************************
     * read extensions *
     ******************************************************************************************************************/

    // list of extensions
    private ArrayList<byte[]> extensions = new ArrayList<byte[]>();

public ArrayList<byte[]> getExtensions(){
	return extensions;
}    

public void setExtensions(ArrayList<byte[]> ext){
	extensions = ext;
}
    
    private void readExtensions(DataInputStream in) throws IOException {
        // new format without total length of all extensions
        int len;
        while ((len = in.readInt()) > 0) {
            byte[] extension = new byte[len];
            in.readFully(extension);
            if (TTT.verbose)
                System.out.println("Extension: Tag[" + extension[0] + "] " + len + " bytes");
            extensions.add(extension);
        }
        if (TTT.verbose)
            System.out.println(extensions.size() + " extensions found.");
        parseExtensions();

        // no original, but modified recording
        if (extensions.size() > 0)
            original = false;
    }

    public Index index = new Index(this);

    private void parseExtensions() throws IOException {
        for (int i = 0; i < extensions.size(); i++) {
            byte[] extension = extensions.get(i);
            DataInputStream ext_in = new DataInputStream(new ByteArrayInputStream(extension));
            int tag = ext_in.readByte();
            switch (tag) {
            case Constants.EXTENSION_INDEX_TABLE:
                if (TTT.verbose)
                    System.out.println("\n-----------------------------------------------\nReading Index Table\n");
                try {
                    index.readIndexExtension(ext_in);
                } catch (Exception e) {
                    System.out.println("READING OF INDEX TABLE  EXTENSION FAILED: " + e);
                    if (TTT.debug)
                        e.printStackTrace();
                }
                break;

            case Constants.EXTENSION_SEARCHBASE_TABLE_WITH_COORDINATES:
                if (TTT.verbose)
                    System.out
                            .println("\n-----------------------------------------------\nReading Searchbase Extension\n");
                try {
                    SearchbaseExtension.readSearchbaseExtension(ext_in, index);
                    // extensions.remove(i);
                } catch (Exception e) {
                    System.out.println("READING OF SEARCHBASE EXTENSION FAILED: " + e);
                    if (TTT.debug)
                        e.printStackTrace();
                }
                break;

            default:
                System.out.println("\n-----------------------------------------------\nUNKNOWN EXTENSION ([" + tag
                        + "] " + extension.length + " bytes)\n");
                break;
            }
        }
    }

    /*******************************************************************************************************************
     * playback
     ******************************************************************************************************************/

    // flags
    private boolean running = true;
    private boolean interrupted;
    private boolean paused;
    private boolean adjusting;

    int next_message;

    // sync desktop replay for badly synced audio recordings (audio too short)
    public double desktop_replay_factor = 1;
    public boolean desktop_replay_factor_sync = false;

    private int sync(int timestamp) {
        // if (desktop_replay_factor_sync)
        // return (int) (timestamp * desktop_replay_factor);
        // else
        return timestamp;
    }

    // main loop
    // display next messages
    // sunchronize message and audio/video stream
    public void run() {
        Timer changeEventDaemon = startChangeEventDaemon();

        long t = System.currentTimeMillis();

        while (running) {
            try {
                synchronized (this) {
                    // wait if pause mode
                    while (running && (paused || adjusting)) {
                        // System.out.println((paused ? "paused " : "") + (adjusting ? "adjusting" : ""));
                        wait();
                    }

                    // closing
                    if (!running)
                        break;

                    // next message
                    Message message = messages.get(next_message);

                    // synchronize message and audio/video player
                    // delay if too early
                    int time = audioVideoPlayer.getTime();

                    int delay = sync(message.getTimestamp()) - time;
                    // System.out.println(next_message+" delay: " + Constants.getStringFromTime(delay)+"\t");
                    if (delay > 0)
                        wait(delay);

                    // closing
                    if (!running)
                        break;

                    // state changed - active message may be outdated - abort
                    if (interrupted || adjusting) {
                        interrupted = false;
                        continue;
                    }

                    // display message
                    // System.out.println(next_message + ".\t" + message);
                    deliverMessage(message);

                    // update index viewer
                    index.updateRunningIndex(message.getTimestamp());

                    // increase message counter
                    next_message++;
                }

            } catch (InterruptedException e) {} catch (IndexOutOfBoundsException e) {
                // TODO:
                // wait until audio finished and stop
                // System.out.println("end reached");
                stop();
                t = System.currentTimeMillis() - t;
                // System.out.println("done elapsed: " + Constants.getStringFromTime((int) t));
                t = System.currentTimeMillis();
            }
        }

        // stop daemon
        changeEventDaemon.cancel();
    }

    // used by acuitus.com
    public void setNextMessage(int next) {
        next_message = next;
    }

    /*******************************************************************************************************************
     * playback control *
     ******************************************************************************************************************/

    private Thread thread;;

    // start playback
    synchronized public void play() {
        // ensure thread is running
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }

        fireTimeChangedEvent(TimeChangedListener.PLAY);
        paused = false;
        if (audioVideoPlayer != null)
            audioVideoPlayer.play();
        interrupt();
    }

    // pause playback
    synchronized public void pause() {
        fireTimeChangedEvent(TimeChangedListener.PAUSE);
        paused = true;
        if (audioVideoPlayer != null)
            audioVideoPlayer.pause();
        interrupt();
    }

    public boolean paused() {
        return paused;
    }

    // stop playback: pause and set time to beginning
    synchronized public void stop() {
        pause();
        // blank framebuffer
        whiteOut();
        setTime(0);
    }

    // display solid white framebuffer
    public void whiteOut() {
        graphicsContext.memGraphics.setColor(Color.WHITE);
        graphicsContext.memGraphics.fillRect(0, 0, prefs.framebufferWidth, prefs.framebufferHeight);
    }

    // set playback to next index
    synchronized public void next() {
        setTime(index.getNextIndex().getTimestamp());
    }

    // set playback to previous index
    synchronized public void previous() {
        setTime(index.getPreviousIndex().getTimestamp());
    }

    private int timeset;

    // private long t;

    // set playback time
    // performs visible scrolling while adjusting
    // also sets audio/video player if not adjusting
    synchronized public void setTime(int time) {
        time = sync(time);
        // System.out.println("set: " + Constants.getStringFromTime(time));
        // long t = System.currentTimeMillis();

        timeset = time;
        // disable display
        boolean refreshStatus = graphicsContext.isRefreshEnabled();
        graphicsContext.enableRefresh(false);

        // paint offscreen
        // setTime_two_minute_check(time);
        messages.setTime_full_frame_check(time);
        // messages.setTime_full_frame_check_regarding_stripes(time);

        index.setCorrespondingIndex(time);

        // update display
        graphicsContext.enableRefresh(refreshStatus);

        graphicsContext.refresh();

        // synchronize audio/video
        // only if not adjusting, because synchronizing is slow
        if (!adjusting && audioVideoPlayer != null)
            setAudioVideoPlayerTime(time);

        // t = System.currentTimeMillis() - t;
        // System.out.println(Constants.getStringFromTime((int) t));
    }

    private void setAudioVideoPlayerTime(int time) {
        // synchronize audio/video
        if (audioVideoPlayer != null)
            audioVideoPlayer.setTime(time);

        // notify playback loop
        interrupt();
    }

    // returns actual playback time
    synchronized public int getTime() {
        if (adjusting)
            // time to be set (immediate feedback)
            return timeset;
        else if (audioVideoPlayer != null)
            // playback time
            return audioVideoPlayer.getTime();
        else
            // not exact as some time has may have passed since message was displayed
            return messages.get(next_message).getTimestamp();
    }

    // playback duration
    public int getDuration() {
        // TODO: compute duration
        if (audioVideoPlayer != null)
            return audioVideoPlayer.getDuration();
        return messages.get(messages.size() - 1).getTimestamp();

    }

    // distinguish notify from timeout after wait
    synchronized private void interrupt() {
        interrupted = true;
        notify();
    }

    // ///////////////////////////////////////////////////////////////////////
    // event handling
    // ///////////////////////////////////////////////////////////////////////

    // action commands
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String STOP = "stop";
    public static final String NEXT = "next";
    public static final String PREVIOUS = "previous";
    public static final String INDEX = "index";
    public static final String FULLSCREEN = "fullscreen";
    public static final String SEARCH = "search";

    // receive action command
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if (command.equals(PLAY))
            play();
        else if (command.equals(PAUSE))
            pause();
        else if (command.equals(STOP))
            stop();
        else if (command.equals(NEXT))
            next();
        else if (command.equals(PREVIOUS))
            previous();
        else if (command.equals(INDEX))
            ;
        else if (command.equals(FULLSCREEN))
            ;
        else
            System.out.println("unknown player command: " + command);
    }

    // set playback time according to slider movement
    // performs visible scrolling while adjusting
    // also sets audio/video player if not adjusting
    synchronized public void sliderStateChanged(ChangeEvent event) {
        if (event instanceof MyChangeEvent) {
            adjusting = ((MyChangeEvent) event).adjusting;
            if (adjusting) {
                // visible scrolling
                setTime(((MyChangeEvent) event).time);
            } else {
                // synchronize audio/video
                // NOTE: the slider fires one last event with adjusting==false but same timestamp than before,
                // therefore setTime has allready been called for this time value
                setAudioVideoPlayerTime(((MyChangeEvent) event).time);
            }
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////
    // Listeners
    // ///////////////////////////////////////////////////////////////////////////////////

    // list of listeners
    private ArrayList<TimeChangedListener> timeChangedListeners = new ArrayList<TimeChangedListener>();

    // This methods allows classes to register
    public void addTimeChangedListener(TimeChangedListener listener) {
        timeChangedListeners.add(listener);
    }

    // This methods allows classes to unregister
    public void removeTimeChangedListener(TimeChangedListener listener) {
        timeChangedListeners.remove(listener);
    }

    private int lastIndexFired = -1;

    public void fireIndexChangedEvent(int index) {
        if (lastIndexFired != index) {
            fireTimeChangedEvent(-index);
            lastIndexFired = index;
        }
    }

    // used to fire events
    public void fireTimeChangedEvent(int event) {
        for (int i = 0; i < timeChangedListeners.size(); i++) {
            timeChangedListeners.get(i).timeChanged(event);
        }
    }

    // Daemon updates listeners with timeline
    private Timer startChangeEventDaemon() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                fireTimeChangedEvent(getTime());
            }
        }, 0, 1000);
        return timer;
    }

    // //////////////////////////////////////////////////////////////////
    // volume control
    // //////////////////////////////////////////////////////////////////
    public int getVolumeLevel() {
        if (audioVideoPlayer != null)
            return audioVideoPlayer.getVolumeLevel();
        else
            return 0;
    }

    public void setVolumeLevel(int volume) {
        if (audioVideoPlayer != null)
            audioVideoPlayer.setVolumeLevel(volume);
    }

    public boolean getMute() {
        if (audioVideoPlayer != null)
            return audioVideoPlayer.getMute();
        else
            return true;
    }

    public void setMute(boolean mute) {
        if (audioVideoPlayer != null)
            audioVideoPlayer.setMute(mute);
    }

    // //////////////////////////////////////////////////////////////////
    // searching
    // //////////////////////////////////////////////////////////////////
    public void highlightSearchResults(Graphics2D g) {
        index.highlightSearchResultsOfCurrentIndex(g);
    }

    /*******************************************************************************************************************
     * write to file *
     ******************************************************************************************************************/
    // original recording flag - used for first backup name
    private boolean original = true;

    /**
     * Save the recording in a new file
     * 
     * @param OutputFile saves the recording into this file
     */
    
    public boolean store(File OutputFile){
    	fileDesktop = OutputFile;
    return	store();
    }
    
    
    public boolean store() {
        // TODO: Progress Monitor (leave EventDispatchingThread)
        // ProgressMonitor progressMonitor = null;
        try {
            // backup ttt file
            File renameFile = null;

            // first backup of original (unmodified) recording gets special name
            if (original)
                renameFile = new File(fileDesktop.getCanonicalPath() + ".orig");

            if (renameFile == null || renameFile.exists())
                renameFile = new File(fileDesktop.getCanonicalPath() + ".bak");

            int i = 1;
            while (renameFile.exists())
                renameFile = new File(fileDesktop.getCanonicalPath() + ".bak." + i++);
            fileDesktop.renameTo(renameFile);

            FileOutputStream fileOut = new FileOutputStream(fileDesktop);
            DataOutputStream out = new DataOutputStream(fileOut);
            // progressMonitor.setProgress(1);

            // write header
            //
            // write version message
            out.write(Constants.VersionMessageTTT.getBytes());

            // write compressed messages to file
            out = new DataOutputStream(new DeflaterOutputStream(fileOut));
            writeInit(out);

            writeExtensions(out);

            // NOTE: badly designed - should be placed before extensions as part of init
            out.writeLong(prefs.starttime);

            // write body
            messages.writeMessages(out);
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Writing failed.", "Error:", JOptionPane.ERROR_MESSAGE);
          if(TTT.verbose)
            System.out.println("Error: Writing failed. " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        // if (progressMonitor != null)
        // progressMonitor.close();
    }

    // initialization
    void writeInit(DataOutputStream out) throws IOException {
        // write protocol initialisation
        out.writeShort(prefs.framebufferWidth);
        out.writeShort(prefs.framebufferHeight);
        out.writeByte(prefs.bitsPerPixel);
        out.writeByte(prefs.depth);
        out.writeByte(prefs.bigEndian ? 1 : 0);
        out.writeByte(prefs.trueColour ? 1 : 0);
        out.writeShort(prefs.redMax);
        out.writeShort(prefs.greenMax);
        out.writeShort(prefs.blueMax);
        out.writeByte(prefs.redShift);
        out.writeByte(prefs.greenShift);
        out.writeByte(prefs.blueShift);
        out.writeByte(0); // padding
        out.writeByte(0);
        out.writeByte(0);
        out.writeInt(prefs.name.length());
        out.write(prefs.name.getBytes());
    }

    // ///////////////////////////////////////
    // Extensions
    // ///////////////////////////////////////

    void writeExtensions(DataOutputStream out) throws IOException {
        // write current index extensions instead of read one (maybe modified)
        System.out.println("Writing Index Table");
        index.writeIndexExtension(out);

        if (index.getSearchbaseFormat() == Index.XML_SEARCHBASE) {
            System.out.println("Write Searchbase");
            SearchbaseExtension.writeSearchbaseExtension(out, index);
        }

        // write unkown extensions
        for (int i = 0; i < extensions.size(); i++) {
            byte[] extension = (byte[]) extensions.get(i);
            switch (extension[0]) {
            case Constants.EXTENSION_INDEX_TABLE:
            case Constants.EXTENSION_SEARCHBASE_TABLE_WITH_COORDINATES:
                // skip, because it's already written
                break;

            default:
                System.out.println("Writing unknown extension [Tag:" + extension[0] + "]");
                out.writeInt(extension.length);
                out.write(extension);
                break;
            }
        }
        // no more extensions
        out.writeInt(0);
    }

    private PlaybackControls playbackControls;

    public PlaybackControls getPlaybackControls() {
        if (playbackControls == null)
            playbackControls = new PlaybackControls(this);
        return playbackControls;
    }

    public boolean thumbnailsAvailable() {
        return index.thumbnailsAvailable();
    }

    public boolean createThumbnails() throws IOException {
        return createScript(ScriptCreator.THUMBNAILS);
    }

    public boolean createScript() throws IOException {
        return createScript(ScriptCreator.HTML_SCRIPT | ScriptCreator.OCR_OPTIMIZED | ScriptCreator.THUMBNAILS);
    }

    // mode can be a & combination of HTML_SCRIPT, OCR_OPTIMIZED, THUMBNAILS (of class ScriptCreator)
    public boolean createScript(final int mode) throws IOException {
        return index.computeScreenshots(mode, false, true);
    }

    public boolean createScript(final int mode, boolean batch) throws IOException {
        return index.computeScreenshots(mode, batch, true);
    }

    
    public boolean createScript(final int mode, boolean batch, String ocrPath) throws IOException {
        return index.computeScreenshots(mode, batch, true, ocrPath);
    }
    
    public boolean createScript(final int mode, boolean batch, boolean ShowProgressMonitor) throws IOException {
        return index.computeScreenshots(mode, batch, ShowProgressMonitor);
    }
    
    /*******************************************************************************************************************
     * TTT 2 Flash
     ******************************************************************************************************************/

    public void createFlash(boolean batch) throws IOException {
        pause();
        int previousTime = getTime();
if(TTT.verbose){
        System.out.println("----------------------------------------------");
        System.out.println("TTT2Flash Converter");
        System.out.println("----------------------------------------------");
}
        long t = System.currentTimeMillis();

        FlashContext.createFlash(this, batch);

        t = System.currentTimeMillis() - t;
if(TTT.verbose){
        System.out.println(" done in " + Constants.getStringFromTime((int) t));
        System.out.println("----------------------------------------------");
}
        // reset playback
        // TODO: reset mode pause/play
        setTime(previousTime);
    }

    /*******************************************************************************************************************
     * Color Histogram
     ******************************************************************************************************************/
    public static void main(String[] args) {
        // message_sizes(args);
        color_histogram(args);
    }

    public static void color_histogram(String[] args) {

        TTT.verbose = false;

        for (String arg : args) {
            try {
                Recording recording = new Recording(arg, false);

                int count = 0;
                ArrayList<HashMap<Integer, Integer>> hashs = new ArrayList<HashMap<Integer, Integer>>();
                for (IndexEntry entry : recording.index.index) {
                    System.out.println("#" + (++count) + "\t" + Constants.getStringFromTime(entry.getTimestamp()));
                    recording.setTime(entry.getTimestamp());

                    HashMap<Integer, Integer> hash = new HashMap<Integer, Integer>();
                    for (int pixel : recording.graphicsContext.getPixels(0, 0, recording.prefs.framebufferWidth,
                            recording.prefs.framebufferHeight)) {
                        pixel &= 0x00FFFFFF;

                        Integer i = hash.get(new Integer(pixel));
                        if (i == null)
                            hash.put(pixel, 1);
                        else
                            hash.put(pixel, ++i);
                    }

                    for (Entry<Integer, Integer> e : hash.entrySet()) {
                        int percentage = ((int) (((double) (e.getValue()) / (recording.prefs.framebufferWidth * recording.prefs.framebufferHeight)) * 10000));
                        if (percentage > 1000)
                            System.out
                                    .println("\t" + e.getKey() + "\t" + e.getValue() + "\t" + percentage / 100d + "%");
                    }

                    hashs.add(hash);

                    System.out.println();

                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        System.exit(0);
    }
}
