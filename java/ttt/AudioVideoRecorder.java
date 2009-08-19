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
 * Created on 11.04.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.Format;
import javax.media.IncompatibleTimeBaseException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Processor;
import javax.media.StartEvent;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.SourceCloneable;
import javax.swing.JOptionPane;

public class AudioVideoRecorder implements ControllerListener {
    static public int MPEG_AUDIO_RECORD_MODE = 1;
    static public int WAV_RECORD_MODE = 2;

    private DataSink audioSink, videoSink, aviSink;
    private Processor audioProcessor, videoProcessor, aviProcessor;
    private StateHelper aviStateHelper;
    private boolean avi_recorder = !true;
    private boolean video_recording = true;

    // AVI
    // NOT WORKING: THERE IS NO SYNC DONE BY JMF - THAT SUCKS!!!
    private Format aviAudioFormat = new AudioFormat(AudioFormat.IMA4_MS, 22050, AudioFormat.NOT_SPECIFIED, 1);
    private String fileTypeDescriptor = FileTypeDescriptor.MSVIDEO;

    // Quicktime
    // Format aviAudioFormat = new AudioFormat(AudioFormat.GSM, 22050,
    // AudioFormat.NOT_SPECIFIED, 1);
    // String fileTypeDescriptor = FileTypeDescriptor.QUICKTIME;

    static int mpeg_audio_layer = 0;

    // WAV Audio Recording
    private int audioMode;
    private IAudioRecorder wavAudioRecorder;
    private AudioMonitorPanel volumeLevelComponent;

    public AudioVideoRecorder(LectureProfile lectureProfile) throws IOException {
        // public AudioVideoRecorder(boolean video_recording, int audioMode) throws IOException {

        video_recording = lectureProfile.isRecordVideoEnabled();
        audioMode = lectureProfile.isRecordLinearAudioEnabled() ? WAV_RECORD_MODE
                : AudioVideoRecorder.MPEG_AUDIO_RECORD_MODE;

        // NOTE: JMF2.1.1a does only support AVI-replay up to 35:47 due to an int overflow bug
        if (avi_recorder) {
            // recording in one AVI file
            System.out.println("DEBUG: USING AVI RECORDER");
            if (!createAVIProcessor(aviAudioFormat))
                JOptionPane.showMessageDialog(null, "Can't create audio/video processor");
        } else {
            // recording audio and video to separate files

            if ((audioMode & WAV_RECORD_MODE) > 0) {
                System.out.println("\nINITIALIZING AUDIO DEVICE:");
                System.out.println("    format: linear audio / WAV");
                try {
                    volumeLevelComponent = new AudioMonitorPanel(false);
                    wavAudioRecorder = new JSAAudioRecorder(volumeLevelComponent);
                    // monitoring only
                    wavAudioRecorder.startRecording(null);
                } catch (Exception e) {
                    System.out.println("    FORMAT FAILED.\n");
                    e.printStackTrace();
                    throw new IOException(e.getMessage());
                }
                System.out.println("Audio ready.");
            }

            if ((audioMode & MPEG_AUDIO_RECORD_MODE) > 0) {
                if (audioSourceStatic != null && !(audioSourceStatic instanceof SourceCloneable))
                    audioSourceStatic = Manager.createCloneableDataSource(audioSourceStatic);

                try {
                    System.out.println("\nINITIALIZING AUDIO DEVICE:");
                    Format format = new AudioFormat(AudioFormat.MPEGLAYER3, 22050.0, 16, 1, AudioFormat.BIG_ENDIAN,
                            AudioFormat.SIGNED);
                    System.out.println("    Trying format: " + format);
                    format = createAudioProcessor(format);

                    if (format != null) {
                        mpeg_audio_layer = 3;
                    } else {
                        System.out.println("    FORMAT FAILED.\n");

                        format = new AudioFormat(AudioFormat.MPEG, 22050.0, 16, 1, AudioFormat.BIG_ENDIAN,
                                AudioFormat.SIGNED);
                        System.out.println("    Trying format: " + format);
                        format = createAudioProcessor(format);
                        if (format != null) {
                            mpeg_audio_layer = 2;
                            TTT
                                    .showMessage(
                                            "WARNING:\nNo codec for MPEG Audio Layer 3 (mp3) found.\nRecording MPEG Audio Layer 2 (mp2) instead.",
                                            "TTT Audio Recorder", JOptionPane.WARNING_MESSAGE);
                        } else
                            System.out.println("    FORMAT FAILED.\n");
                    }
                    if (format == null)
                        audioProcessor = null;

                    if (audioProcessor == null && getRecordAudio()) {
                        System.out.println("AUDIO FAILED.");
                        JOptionPane.showMessageDialog(null, "Cannot create audio recorder");
                    } else
                        System.out.println("Audio ready.");

                } catch (IncompatibleTimeBaseException e) {
                    throw new IOException(e.getMessage());
                }
            }

            if (video_recording)
                try {
                    createVideoProcessor();
                } catch (IncompatibleTimeBaseException e) {
                    throw new IOException(e.getMessage());
                }
            else
                videoProcessor = null;
        }
    }

    public AudioMonitorPanel getVolumeLevelComponent() {
        return volumeLevelComponent;
    }

    private boolean closing;

    public void close() {
        closing = true;
        stopRec();

        if (audioProcessor != null)
            audioProcessor.close();
        audioProcessor = null;

        if (videoProcessor != null)
            videoProcessor.close();
        videoProcessor = null;

        if (aviProcessor != null)
            aviProcessor.close();
        aviProcessor = null;

        audioSink = videoSink = aviSink = null;
    }

    private boolean createAVIProcessor(Format audioFormat) {
        // JMF sucks! There is no sync of audio and video recording
        try {
            Format videoSourceFormat = null;
            System.out.println("Video source: " + getVideoSource());
            System.out.println("Audio source: " + getAudioSource());
            Format audioSourceFormat = new AudioFormat(AudioFormat.LINEAR, 22050, AudioFormat.NOT_SPECIFIED, 2);
            DataSource dataSource = JMFUtils.createCaptureDataSource(new MediaLocator(getVideoSource()),
                    videoSourceFormat, new MediaLocator(getAudioSource()), audioSourceFormat);
            if (dataSource == null)
                System.out.println("FAILED: Couldn't initialize DataSource");

            Processor processor = Manager.createProcessor(dataSource);

            StateHelper stateHelper = new StateHelper(processor);
            stateHelper.configure();
            processor.setContentDescriptor(new FileTypeDescriptor(fileTypeDescriptor));

            // set formats
            TrackControl[] trackControls = processor.getTrackControls();
            Format videoFormat = new VideoFormat(VideoFormat.H263, getRecordVideoSize(), VideoFormat.NOT_SPECIFIED,
                    null, 1);
            // Format videoFormat = new H263Format();
            for (int i = 0; i < trackControls.length; i++) {
                TrackControl trackControl = trackControls[i];
                Format[] supported = trackControl.getSupportedFormats();
                if (supported != null)
                    // set video format
                    for (int j = 0; j < supported.length; j++) {
                        Format matchedFormat = JMFUtils.matches(videoFormat, supported);
                        if (matchedFormat != null) {
                            String formatString = trackControl.setFormat(matchedFormat).toString();
                            System.out.println("set track[" + (i + 1) + "] format: " + formatString);
                            break;
                        }
                    }
                // set audio format
                for (int j = 0; j < supported.length; j++) {
                    Format matchedFormat = JMFUtils.matches(audioFormat, supported);
                    if (matchedFormat != null) {
                        String formatString = trackControl.setFormat(matchedFormat).toString();
                        System.out.println("set track[" + (i + 1) + "] format: " + formatString);
                        break;
                    }
                }
            }
            stateHelper.realize();
            this.aviProcessor = processor;
            this.aviStateHelper = stateHelper;

            return true;
        } catch (Exception e) {
            System.out.println("Audio/Video Processor failed: " + e);
            e.printStackTrace();
        }
        this.aviProcessor = null;
        this.aviStateHelper = null;
        return false;
    }

    // reuse audiosource, because opening multiple times can block on some systems
    private Format createAudioProcessor(Format format) throws IOException, IncompatibleTimeBaseException {

        // Audio DataSource
        DataSource audioSource = null;

        // use clone if source clonable
        if (audioSourceStatic instanceof SourceCloneable)
            audioSource = ((SourceCloneable) audioSourceStatic).createClone();

        if (audioSource == null) {
            if (getAudioSource() != null) {
                System.out.println("    Audio source: " + getAudioSource());
                try {
                    audioSource = Manager.createDataSource(new MediaLocator(getAudioSource()));
                } catch (Exception e) {
                    System.err.println("Couldn't open audio: " + getAudioSource() + " - " + e);
                    JOptionPane.showMessageDialog(null, "Warning: Audio recording failed:\n" + "Couldn't open audio: "
                            + getAudioSource() + " - " + e);
                    e.printStackTrace();
                }
            } else {
                System.out.println("WARNING: No Audio !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                JOptionPane.showMessageDialog(null, "Warning: Audio recording disabled!");
            }
        }

        // Audio Processor
        if (audioSource != null && getRecordAudio()) {
            try {
                audioProcessor = Manager.createProcessor(audioSource);
                StateHelper stateHelper = new StateHelper(audioProcessor);
                stateHelper.configure();
                System.out.println("    Content: "
                        + audioProcessor.setContentDescriptor(new ContentDescriptor(FileTypeDescriptor.MPEG_AUDIO)));

                // NOTE: AudioFormat.MPEG offers better compatibility with JMFe
                // but appears as MP2 and makes trouble if converting TTT to Flash
                // return new AudioFormat(AudioFormat.MPEG, 22050.0, 16, 1, AudioFormat.BIG_ENDIAN, AudioFormat.SIGNED);
                // return new AudioFormat(AudioFormat.MPEGLAYER3, 22050.0, 16, 1, AudioFormat.BIG_ENDIAN,
                // AudioFormat.SIGNED);

                // set formats
                TrackControl[] trackControls = audioProcessor.getTrackControls();
                for (int i = 0; i < trackControls.length; i++) {
                    TrackControl trackControl = trackControls[i];
                    // System.out.println(i+" "+trackControl);
                    // Format audioFormat = new
                    // AudioFormat(AudioFormat.MPEG);
                    Format audioFormat = format;
                    // Format audioFormat = getDefaultAudioRecordFormat();
                    Format[] supported = trackControl.getSupportedFormats();
                    if (supported != null) {
                        // set audio format
                        for (int j = 0; j < supported.length; j++) {
                            Format matchedFormat = JMFUtils.matches(audioFormat, supported);
                            if (matchedFormat != null) {
                                format = trackControl.setFormat(matchedFormat);
                                System.out.println("    Format: " + format);
                                break;
                            } else
                                return null;
                        }
                    }
                }

                if (!stateHelper.realize())
                    return null;

                if (!stateHelper.prefetch())
                    return null;

                return format;
                // }
            } catch (NoPlayerException npe) {
                System.err.println("Cannot open audio device: " + npe);
                JOptionPane.showMessageDialog(null, "Can't create recorder for '" + getAudioSource() + "'");
                npe.printStackTrace();
            }
        }
        return null;
    }

    // videosource must be newly opened for each recording
    // but output is only shown once
    private boolean firstVideo = true;

    private void createVideoProcessor() throws IOException, IncompatibleTimeBaseException {

        if (firstVideo)
            System.out.println("\nINITIALIZING VIDEO DEVICE:");

        // Video DataSource
        DataSource videoSource = null;

        // use clone if source clonable
        if (videoSourceStatic != null && !(videoSourceStatic instanceof SourceCloneable))
            videoSourceStatic = Manager.createCloneableDataSource(videoSourceStatic);
        if (videoSourceStatic instanceof SourceCloneable)
            videoSource = ((SourceCloneable) videoSourceStatic).createClone();
        if (videoSource == null && getRecordVideo()) {
            if (getVideoSource() != null && !getVideoSource().equals("")) {
                if (firstVideo)
                    System.out.println("    Video source: " + getVideoSource());
                try {
                    videoSource = Manager.createDataSource(new MediaLocator(getVideoSource()));
                } catch (Exception e) {
                    System.err.println("Couldn't open video: " + getVideoSource() + " - " + e);
                }
            } else {
                if (firstVideo)
                    System.out.println("\nNO VIDEO RECORDING\n");
            }
        }

        // Video Processor
        if (videoSource != null) {
            try {
                videoProcessor = Manager.createProcessor(videoSource);
                videoProcessor.configure();
                while (videoProcessor.getState() != Processor.Configured)
                    Thread.sleep(100);
                String str = "    Content: "
                        + videoProcessor.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.QUICKTIME));
                if (firstVideo)
                    System.out.println(str);

                TrackControl[] trackControl = videoProcessor.getTrackControls();
                VideoFormat formatWithSize = new VideoFormat(null, getRecordVideoSize(), Format.NOT_SPECIFIED, null,
                        Format.NOT_SPECIFIED);
                Format format = getDefaultVideoRecordFormat().intersects(formatWithSize);

                str = "    Format: " + trackControl[0].setFormat(format);
                if (firstVideo)
                    System.out.println(str);

                videoProcessor.prefetch();
                videoProcessor.addControllerListener(this);
                if (firstVideo)
                    System.out.println("Video ready");

            } catch (NoPlayerException npe) {
                System.err.println("Can't open video: " + npe);
                JOptionPane.showMessageDialog(null, "Can't create player for '" + getVideoSource() + "'");
            } catch (InterruptedException ie) {}
        }
        firstVideo = false;

        if (videoProcessor == null && getRecordVideo())
            JOptionPane.showMessageDialog(null, "Can't create video recorder");
    }

    public void stopRec() {
        try {
            if (avi_recorder) {
                System.out.println("close avi processor");
                if (aviProcessor != null)
                    aviProcessor.close();
                System.out.println("closed");
                System.out.println("close data sink");
                if (aviSink != null)
                    aviSink.close();
                System.out.println("closed");

            } else {
                if ((audioMode & WAV_RECORD_MODE) > 0) {
                    if (wavAudioRecorder != null)
                        wavAudioRecorder.stopRecording();
                    wavAudioRecorder = null;
                    if (!closing) {
                        // re-open for monitoring only
                        wavAudioRecorder = new JSAAudioRecorder(volumeLevelComponent);
                        wavAudioRecorder.startRecording(null);
                    }
                }

                if ((audioMode & MPEG_AUDIO_RECORD_MODE) > 0)
                    if (audioProcessor != null)
                        // only close if finished
                        // otherwise just stop audio processor, because some systems will hang during reopening
                        if (closing)
                            audioProcessor.close();
                        else
                            audioProcessor.stop();

                if (videoProcessor != null)
                    // must close video processor
                    videoProcessor.close();

                // close files
                if ((audioMode & MPEG_AUDIO_RECORD_MODE) > 0)
                    if (audioSink != null)
                        audioSink.close();
                if (videoSink != null)
                    videoSink.close();
            }

            if (!closing) {
                if (avi_recorder)
                    if (!createAVIProcessor(aviAudioFormat))
                        JOptionPane.showMessageDialog(null, "Can't re-create audio/video processor");
                // reset video processor
                if (videoProcessor != null)
                    createVideoProcessor();
            }
        } catch (Exception e) {
            System.out.println("\nRecorder Error:");
            e.printStackTrace();
        }
    }

    public void startRec(String file) {
        try {
            // TODO: check file name / auto file names
            if (file.endsWith(Constants.desktopEndings[0]))
                file = file.substring(0, file.length() - 4);

            if (avi_recorder) {
                while (aviStateHelper != null && !aviStateHelper.prefetched)
                    aviStateHelper.prefetch(50);
                aviSink = Manager.createDataSink(aviProcessor.getDataOutput(), new MediaLocator("file:" + file
                        + (fileTypeDescriptor.matches(FileTypeDescriptor.MSVIDEO) ? ".avi" : ".mov")));
                aviSink.open();
                aviProcessor.start();
                aviSink.start();
                System.out.println("    Recording audio and video to '" + file
                        + (fileTypeDescriptor.matches(FileTypeDescriptor.MSVIDEO) ? ".avi" : ".mov") + "'.");
            } else {
                String file_name;
                switch (mpeg_audio_layer) {
                case 3:
                    file_name = file + ".mp3";
                    break;
                case 2:
                    file_name = file + ".mp2";
                    break;
                default:
                    file_name = file + Constants.audioEndings[0];
                    break;
                }

                if ((audioMode & MPEG_AUDIO_RECORD_MODE) > 0)
                    if (audioProcessor != null) {
                        while (audioProcessor.getState() != Processor.Prefetched)
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {}
                        audioSink = Manager.createDataSink(audioProcessor.getDataOutput(), new MediaLocator("file:"
                                + file_name));
                        audioSink.open();
                    }

                if (videoProcessor != null) {
                    while (videoProcessor.getState() != Processor.Prefetched)
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {}
                    videoSink = Manager.createDataSink(videoProcessor.getDataOutput(), new MediaLocator("file:" + file
                            + Constants.videoEndings[0]));
                    videoSink.open();
                }

                if (videoProcessor != null) {
                    videoProcessor.start();
                    videoSink.start();
                    System.out.println("    Recording video to '" + file + Constants.videoEndings[0] + "'.");
                }

                if ((audioMode & WAV_RECORD_MODE) > 0) {
                    if (wavAudioRecorder != null)
                        wavAudioRecorder.stopRecording();
                    wavAudioRecorder = new JSAAudioRecorder(volumeLevelComponent);
                    if (wavAudioRecorder != null) {
                        wavAudioRecorder.startRecording(new File(file + ".wav"));
                        System.out.println("    Recording audio to '" + file + ".wav" + "'.");
                    } else {
                        System.out.println("    Recording audio to '" + file + ".wav" + "' - FAILED");
                    }
                }
                if ((audioMode & MPEG_AUDIO_RECORD_MODE) > 0)
                    if (audioProcessor != null) {
                        if (videoProcessor != null && videoProcessor.getState() != Processor.Started) {
                            synchronized (this) {
                                wait();
                            }
                        }
                        audioProcessor.start();
                        audioSink.start();
                        System.out.println("    Recording audio to '" + file_name + "'.");
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
            TTT.showMessage("Audio/Video recording failed", "TTT Recorder Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void controllerUpdate(ControllerEvent ce) {
        if (ce instanceof StartEvent) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    private static DataSource audioSourceStatic, videoSourceStatic;

    private static Format getDefaultVideoRecordFormat() {
        return new VideoFormat(VideoFormat.H263);
    }

    // TODO: user input
    private String getVideoSource() {
        try {
            return ((CaptureDeviceInfo) CaptureDeviceManager.getDeviceList(new VideoFormat(null)).get(0)).getLocator()
                    .toExternalForm();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean getRecordVideo() {
        return video_recording;
    }

    private Dimension getRecordVideoSize() {
        return new Dimension(176, 144);
    }

    // TODO: user input
    private String audioSource = null;

    private String getAudioSource() {
        if (audioSource != null)
            return audioSource;
        // TODO: add javasound://22050 for old linux kernel sounddriver not supporting 44100
        try {
            // take first device
            // NOTE: will fail if no device available (or not found)
            audioSource = ((CaptureDeviceInfo) CaptureDeviceManager.getDeviceList(new AudioFormat(null)).get(0))
                    .getLocator().toExternalForm();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return audioSource;
    }

    private boolean getRecordAudio() {
        return true;
    }

    // /////////////////////////////////////////////////////////////
    // helper
    // /////////////////////////////////////////////////////////////

    public static Vector getVideoSources() {
        Vector deviceList = CaptureDeviceManager.getDeviceList(null); // new VideoFormat(null));
        int i = 0;
        while (i < deviceList.size()) {
            System.out.println();
            System.out.println(i);
            System.out.println("Name:\t" + ((CaptureDeviceInfo) deviceList.get(i)).getName());
            System.out.println("toString\t" + ((CaptureDeviceInfo) deviceList.get(i)).toString());
            System.out.println(("locator\t" + ((CaptureDeviceInfo) deviceList.get(i)).getLocator()));
            System.out.println("external\t" + ((CaptureDeviceInfo) deviceList.get(i)).getLocator().toExternalForm());
            Source dev = new Source(((CaptureDeviceInfo) deviceList.get(i)).getName());
            deviceList.set(i++, dev);
        }
        return deviceList;
    }

}

class Source extends MediaLocator {
    Source(URL url) {
        super(url);
    }

    Source(String locatorString) {
        super(locatorString);
    }

    public String toString() {
        String str = toExternalForm();
        if (str.equals(""))
            return "( none )";
        else if (str.equals("other"))
            return "( specify other )";
        else if (str.equals("file:"))
            return "( choose file )";
        else if (str.startsWith("file:"))
            return str.substring(str.lastIndexOf(File.separatorChar) + 1);
        else
            return str;
    }

    public boolean equals(Object object) {
        if (!(object instanceof MediaLocator))
            return false;
        else
            return this.toExternalForm().equals(((MediaLocator) object).toExternalForm());
    }
}
