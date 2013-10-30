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

/* Audiorecorder.java
 * Created on 06. April 2007, 08:37
 * @author Christian Gruber Bakk.techn.
 */

package ttt.audio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;


public class JSAAudioRecorder extends Thread implements IAudioRecorder {
    private static final AudioFormat[] audioFormats = {
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050.0F, 16, 1, 2, 22050.0F, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 11025.0F, 16, 1, 2, 11025.0F, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, 16, 1, 2, 44100.0F, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000.0F, 16, 1, 2, 8000.0F, false) };

    private File outputFile;
    private DataLine.Info info;
    private TargetDataLine targetDataLine;
    private AudioInputStream audioInputStream;
    private AudioFileFormat.Type targetType;

    public void run() {
        try {
            if (outputFile != null)
                AudioSystem.write(audioInputStream, targetType, outputFile);
            else
                try {
                    // monitoring only
                    File dummyFile = new File("/dev/null");
                    if (!dummyFile.exists())
                        dummyFile = new File("NUL");
                    AudioSystem.write(audioInputStream, targetType, dummyFile);
                } catch (Exception e) {
                    System.err.println("Cannot open audio monitor: " + e);
                }
        } catch (IOException e) {
            System.err.println("Unable to write to file: " + e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ArrayList<AudioFormat> formats = new ArrayList<AudioFormat>();
        float[] sampleRates = { 8000f, 11025f, 16000f, 22050f, 44100f };
        int[] bitsPerSample = { 8, 16 };
        int[] channels = { 1, 2 };
        for (float f : sampleRates) {
            for (int i : bitsPerSample) {
                for (int j : channels) {
                    formats.add(new AudioFormat(f, i, j, false, false));
                    formats.add(new AudioFormat(f, i, j, true, false));
                    formats.add(new AudioFormat(f, i, j, false, true));
                    formats.add(new AudioFormat(f, i, j, true, true));
                }
            }
        }

        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        System.out.println("Available mixers:");
        for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
            System.out.println("\nMIXER: " + mixerInfo[cnt]);
            Mixer mixer = AudioSystem.getMixer(mixerInfo[cnt]);
            for (AudioFormat format : formats) {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (mixer.isLineSupported(info))
                    System.out.println("\t\tFormat: " + info + "\tSUPPORTED: " + mixer.isLineSupported(info));
            }
        }// end for loop
        System.exit(0);
    }

    public static String availableInputs(){
    	String ret="[";
    	for (Mixer.Info info: AudioSystem.getMixerInfo()){
            TargetDataLine tdl = null;    		
    		for (int i = 0; i < audioFormats.length; i++) {
                DataLine.Info in = new DataLine.Info(TargetDataLine.class, audioFormats[i]);
                if (AudioSystem.isLineSupported(in)) {
                    break;
                }
            }
    		
    		ret+=info.getName()+"/\n                           ";
    	}
    	return ret+"]";
    }
    
    /** Creates a new instance of JSAAudioRecorder */
    public JSAAudioRecorder(AudioMonitorPanel volumeLevelComponent) throws Exception {
        targetDataLine = null;
      
        // Try to get a Targetline from which the audio data is read
        for (int i = 0; i < audioFormats.length; i++) {
            info = new DataLine.Info(TargetDataLine.class, audioFormats[i]);
            if (AudioSystem.isLineSupported(info)) {
                // try {
                targetDataLine = new TargetDataLineMonitor((TargetDataLine) AudioSystem.getLine(info),
                        volumeLevelComponent);
                targetDataLine.open(audioFormats[i], targetDataLine.getBufferSize());
                break;
            }
        }

        // if no available AudioFormat was found abort
        if (targetDataLine == null) {
            System.err.println("Unable to get an audio source!");
            throw new IOException("audio source unavailable");
        }
        audioInputStream = new AudioInputStream(targetDataLine);
        targetType = AudioFileFormat.Type. WAVE;
    }

    public void startRecording(File file) {
        outputFile = file;
        targetDataLine.start();
        super.start();
    }

    public void stopRecording() {
        targetDataLine.stop();
        targetDataLine.close();
    }
}