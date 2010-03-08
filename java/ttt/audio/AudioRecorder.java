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
 * Created on 11.04.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.audio;

import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;

import ttt.Constants;
import ttt.TTT;
import ttt.record.LectureProfile;

public class AudioRecorder  {
    static public int WAV_RECORD_MODE = 2;

    // WAV Audio Recording
    private int audioMode;
    private IAudioRecorder wavAudioRecorder;
    private AudioMonitorPanel volumeLevelComponent;

    public AudioRecorder(LectureProfile lectureProfile) throws IOException {
        // public AudioVideoRecorder(boolean video_recording, int audioMode) throws IOException {

        audioMode = WAV_RECORD_MODE;

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
    }

    public AudioMonitorPanel getVolumeLevelComponent() {
        return volumeLevelComponent;
    }

    private boolean closing;

    public void close() {
        closing = true;
        stopRec();

    }




    public void stopRec() {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            TTT.showMessage("Audio/Video recording failed", "TTT Recorder Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}

