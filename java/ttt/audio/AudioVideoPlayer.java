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
 * Created on 30.01.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.audio;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.media.Codec;
import javax.media.Format;
import javax.media.IncompatibleTimeBaseException;
import javax.media.Manager;
import javax.media.MediaException;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.Time;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.media.PlugInManager;

import ttt.Constants;
import ttt.TTT;
import ttt.record.Recording;

public class AudioVideoPlayer {
    /*******************************************************************************************************************
     * Audio/Video Player *
     ******************************************************************************************************************/
    private Player player, audioPlayer, videoPlayer;

    JInternalFrame playerFrame;
    // PlaybackControls playbackControls;
    double ratio = 1d;
    private File fileAudio, fileVideo;
    Recording recording;
    static {
			try {
	            Format [] inputs = null;
	            Format [] outputs = null;
				Object instance = Class.forName("com.sun.media.codec.audio.mp3.JavaDecoder").newInstance();
                inputs = ((Codec)instance).getSupportedInputFormats();
                outputs = ((Codec)instance).getSupportedOutputFormats(null);
                boolean success = PlugInManager.addPlugIn("com.sun.media.codec.audio.mp3.JavaDecoder", inputs, outputs, PlugInManager.CODEC);
                if(TTT.verbose)
                System.out.println("initialized MP3 decoder plugin: "+success);
			} catch (Exception e) {
				String message = "Error initializing MP3 decoder plugin";
	            String jmf_version = TTT.getJMFVersion();
				if (jmf_version.equals("NOT INSTALLED"))
	                message += "JMF is not installed - (re)install JMF";
	            else if (jmf_version.equals("2.1.1a"))
	                message += "JMF" + jmf_version + " found - try to re-install";
	            else if (jmf_version.equals("2.1.1e"))
	                message += "JMF" + jmf_version + " found - try to download mp3plugin.jar";
	            else
	                message += "JMF" + jmf_version
	                        + " found - install mp3plugin for JMF. (re)install JMF or switch to JMF2.1.1a";

	            TTT.showMessage(message, "TTT Audio Player", JOptionPane.ERROR_MESSAGE);

			}
    }
    
    public AudioVideoPlayer(String fileName, Recording recording) throws IOException {
        // audio player
        this.recording = recording;
        try {
            fileAudio = Constants.getExistingFile(fileName, Constants.AUDIO_FILE);
            if (TTT.verbose)
                System.out.println("Audio found: " + fileAudio.getName());
            

            MediaLocator audioFile = new MediaLocator("file:" + fileAudio.getCanonicalPath());
            audioPlayer = Manager.createRealizedPlayer(audioFile);
            audio_duration = (int) (audioPlayer.getDuration().getNanoseconds() / 1000000);
            // for (Control control: audioPlayer.getControls()) {
            // System.out.println(control);
            // }
        } catch (MediaException e) {
            System.err.println("Can't play audio: " + e);
            String message = "Cannot play audio: " + e + "\n";
            String jmf_version = TTT.getJMFVersion();
            if (jmf_version.equals("NOT INSTALLED"))
                message += "JMF is not installed - (re)install JMF";
            else if (jmf_version.equals("2.1.1a"))
                message += "JMF" + jmf_version + " found - try to re-install";
            else
                message += "JMF" + jmf_version
                        + " found - install mp3plugin for JMF. (re)install JMF or switch to JMF2.1.1a";

            TTT.showMessage(message, "TTT Audio Player", JOptionPane.ERROR_MESSAGE);

        } catch (FileNotFoundException e) {
        	if(TTT.verbose)
            System.out.println("No audio found (" + e);
        }

        // video player
        try {
            fileVideo = Constants.getExistingFile(fileName, Constants.VIDEO_FILE);
            if (TTT.verbose)
                System.out.println("Video found: " + fileVideo.getName());
            MediaLocator videoFile = new MediaLocator("file:" + fileVideo.getCanonicalPath());
            videoPlayer = Manager.createRealizedPlayer(videoFile);
            video_duration = (int) (videoPlayer.getDuration().getNanoseconds() / 1000000);
        } catch (MediaException e) {
        	if(TTT.verbose)
            System.out.println("Can't play video: " + e);
        } catch (FileNotFoundException e) {
        	if(TTT.verbose)
            System.out.println("No video found (" + e);
        }

        // combine audio and video player
        player = audioPlayer;
        if (player == null)
            player = videoPlayer;
        else {
            try {
                player.addController(videoPlayer);

            } catch (IncompatibleTimeBaseException e) {
                System.out.println("Couldn't synchronize media players. - " + e);
            }
        }

        // set volume to 100 percent (default is only 40 percent)
        if (player != null && player.getGainControl() != null)
            player.getGainControl().setLevel(0.4f);
    }

    public String getAudioFilename() throws IOException {
    	if(fileAudio != null){
        return fileAudio.getCanonicalPath();
    }
    	else{
    		return null;
    	}
    		
    }

    public String getVideoFilename() throws IOException {
    	if(fileVideo != null){
        return fileVideo.getCanonicalPath();}
    	else{
    		return null;}
    }

    public void close() {
        player.close();
    }

    // returns a component displaying the video
    public Component getVideo() {
        if (videoPlayer != null)
            return videoPlayer.getVisualComponent();
        else
            return null;
    }

    // TODO: fix durations
    // private int duration;
    // private int desktop_duration;
    private int audio_duration;
    private int video_duration;

    // playback duration
    public int getDuration() {
        // return Math.min(audio_duration,video_duration);
        try {
            return (int) (player.getDuration().getNanoseconds() / 1000000l);
        } catch (Exception e) {
            System.out.println("Cannot determine duration");
            e.printStackTrace();
            return 0;
        }
    }

    // playback duration
    public int getAudioDuration() {
        // return Math.min(audio_duration,video_duration);
        return audio_duration;
    }

    // playback duration
    public int getVideoDuration() {
        // return Math.min(audio_duration,video_duration);
        return video_duration;
    }

    private double replayRatio = 1.0d;
    private int replayOffset = 0;

    public void setReplayOffset(int msec) {
        // System.out.println("Audio replay offset: " + msec+" msec");
        int time = getTime();
        replayOffset = msec;
        setTime(time);
    }

    public int getReplayOffset() {
        return replayOffset;
    }

    public void setReplayRatio(double ratio) {
    	if(TTT.verbose){
        System.out.println("Audio replay ratio: " + ratio);
    	}
        replayRatio = ratio;
    }

    // actual playback time
    public int getTime() {
        // linear sync of audio preplay
        if (recording.desktop_replay_factor_sync)
            return (int) ((player.getMediaNanoseconds() / 1000000l) * replayRatio) - replayOffset;
        else
            return (int) ((player.getMediaNanoseconds() / 1000000l));
    }

    // set playback to new time
    public void setTime(int time) {

    	// linear sync of audio preplay
        if (recording.desktop_replay_factor_sync)
            player.setMediaTime(new Time((long) ((time + replayOffset) / replayRatio) * 1000000l));
        else
            player.setMediaTime(new Time(time * 1000000l));

    }

    // playback control
    public void pause() {
        player.stop();
    }

    public void play() {
        player.start();
    }

    // //////////////////////////////////////////////////////////////////
    // volume control
    // //////////////////////////////////////////////////////////////////
    public int getVolumeLevel() {
        if (player != null && player.getGainControl() != null)
            return (int) (100 * player.getGainControl().getLevel());
        else
            return 0;
    }

    public void setVolumeLevel(int volume) {
    	float newlevel = volume/0.8f ; //there is a bug in jmf max newlevel is 0.8f and not 1.0f
    	newlevel=(newlevel>0.8f)?0.8f:newlevel;
        if (player != null && player.getGainControl() != null)
            player.getGainControl().setLevel(newlevel);
    }

    public boolean getMute() {
        if (player != null && player.getGainControl() != null)
            return player.getGainControl().getMute();
        else
            return true;
    }

    public void setMute(boolean mute) {
        if (player != null && player.getGainControl() != null)
            player.getGainControl().setMute(mute);
    }
}
