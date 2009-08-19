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

package ttt;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.InflaterInputStream;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;

/*
 * Created on 29.10.2003

 */

/**
 * @author ziewer
 * 
 */
public class TTTInfo {

    static boolean simple;

    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            printUsage();
        simple = false;
        String filename = args[0];
        if (args[0].equals("-simple")) {
            if (args.length < 2)
                printUsage();
            else {
                simple = true;
                filename = args[1];
            }
        }

        File curfile = new File(filename);
        if (curfile.exists()) {

            if (curfile.isFile())
                adjustFile(filename);

            else if (curfile.isDirectory()) {

                File[] files = curfile.listFiles();
                TreeSet<String> filenames = new TreeSet<String>();
                for (int i = 0; i < files.length; i++) {

                    if (files[i].isFile() && files[i].getName().endsWith(".ttt")) {
                        filenames.add(files[i].getName().substring(0, files[i].getName().length() - 4));
                    }
                }

                Iterator it = filenames.iterator();
                while (it.hasNext()) {
                    String filetoadjust = (String) it.next();
                    System.out.println("adjusting file " + filetoadjust + "\n");
                    adjustFile(filetoadjust);
                }
            }
        }

    }

    public static void adjustFile(String file) {

        if (file.lastIndexOf('.') == file.length() - 4)
            file = file.substring(0, file.length() - 4);
        System.out.print("File: " + file + ".ttt /mov/mp3 ");
        if (!simple)
            System.out.println();

        int desktop_duration = getDesktopDuration(file + ".ttt");
        if (!simple)
            System.out.println("\tdesktop duration= " + getTimeString(desktop_duration));

        int video_duration = 0;
        File f;
        try {
            f = new File(file + ".mov");
            if (f.exists()) {
                Player player = Manager.createRealizedPlayer(new MediaLocator("file:" + file + ".mov"));
                video_duration = (int) (player.getDuration().getNanoseconds() / 1000000);
                player.close();
                if (!simple)
                    System.out.println("\tvideo duration\t= " + getTimeString(video_duration) + "\tdiff = "
                            + getTimeString(video_duration - desktop_duration));
            } else
                System.out.println("\nno video file found");
        } catch (Exception e) {
            System.out.println("\tno video");
        }

        int audio_duration = 0;
        try {
            f = new File(file + ".mp3");
            if (f.exists()) {
                Player player = Manager.createRealizedPlayer(new MediaLocator("file:" + file + ".mp3"));
                audio_duration = (int) (player.getDuration().getNanoseconds() / 1000000);
                player.close();
                if (!simple)
                    System.out.println("\taudio duration\t= " + getTimeString(audio_duration) + "\tdiff = "
                            + getTimeString(audio_duration - desktop_duration));
            } else {
                f = new File(file + ".mp2");
                if (f.exists()) {
                    Player player = Manager.createRealizedPlayer(new MediaLocator("file:" + file + ".mp2"));
                    audio_duration = (int) (player.getDuration().getNanoseconds() / 1000000);
                    player.close();
                    if (!simple)
                        System.out.println("\taudio duration\t= " + getTimeString(audio_duration) + "\tdiff = "
                                + getTimeString(audio_duration - desktop_duration));
                } else
                    System.out.println("\tno audio file found");
            }
        } catch (Exception e) {
            System.out.println("\tno audio");
        }

        System.out.println();

        // calculate differences to the audio length
        int desktop_difference = desktop_duration - audio_duration;
        int video_difference = video_duration - audio_duration;
        // int = 0;

        // System.out.println("\tdifference = " + getTimeString(difference));

        if (!simple) {
            System.out.println("desktop difference: " + desktop_difference);
            System.out.println("video difference: " + video_difference);
        }

        // decide if the file needs to be resampled
        if ((video_duration > 0) && (Math.abs(video_difference) > 200)) {
            // difference is greater than 0.2 seconds, the file needs to be resampled
            System.out.println("resample audio to video length");
            // copy the file
            String exec = "cp -f " + file + ".mp3 " + file + "_copy.mp3";
            if (!simple)
                System.out.println(exec);
            // exec(exec);
            try {
                copy(new File(file + ".mp3"), new File(file + "_resampled.mp3"));

                // calculate the the stretch factor
                float factor = (float) audio_duration / (float) video_duration;
                System.out.println(factor + "\t" + audio_duration + "\t" + video_duration);
                // call sox to do the resampling
                exec = "sox " + file + "_copy.mp3 " + file + "_resampled.mp3 speed " + factor;
                if (!simple)
                    System.out.println(exec);
                exec(exec);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if ((desktop_duration > 0) && (Math.abs(desktop_difference) > 200)) {
            // difference is greater than 0.2 seconds, the file needs to be resampled
            System.out.println("resample audio to match desktop length");
            // copy the file
            String exec = "cp -f " + file + ".mp3 " + file + "_copy.mp3";
            if (!simple)
                System.out.println(exec);
            // exec(exec);
            try {
                copy(new File(file + ".mp3"), new File(file + "_resampled.mp3"));

                // calculate the the stretch factor
                float factor = ((float) audio_duration) / ((float) desktop_duration);
                // call sox to do the resampling
                exec = "sox " + file + "_copy.mp3 " + file + "_resampled.mp3 speed " + factor;
                if (!simple)
                    System.out.println(exec);
                exec(exec);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            System.out.println("no adjustment needed");

    }

    public static String getTimeString(long nsec) {
        return getTimeString((int) (nsec / 1000000));
    }

    public static String getTimeString(int msec) { // generates nice time String
        boolean negative = msec < 0;
        if (negative)
            msec = -msec;
        int sec = msec / 1000 % 60;
        int min = msec / 60000;
        msec = msec % 1000;

        return (negative ? "-" : " ") + (min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec + "."
                + (msec < 10 ? "0" : "") + (msec < 100 ? "0" : "") + msec;
    }

    public static void exec(String command) {
        try {
            String ls_str;

            Process ls_proc = Runtime.getRuntime().exec(command);
            // get its output (your input) stream

            BufferedReader ls_in = new BufferedReader(new InputStreamReader(ls_proc.getInputStream()));

            try {
                while ((ls_str = ls_in.readLine()) != null) {
                    System.out.println(ls_str);
                }
                ls_proc.waitFor();
            } catch (IOException e) {
                System.exit(0);
            } catch (InterruptedException e) {}

        } catch (IOException e1) {
            System.err.println(e1);
            e1.printStackTrace();
            System.exit(-1);
        }
    }

    static int getDesktopDuration(String file) {
        int maxtimestamp = 0;
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            // skip version
            in.skipBytes(12);
            // read compressed data
            in = new DataInputStream(new BufferedInputStream(new InflaterInputStream(in)));

            // skip init parameters
            in.skipBytes(20);
            // name
            in.skipBytes(in.readInt());

            // skipextensions
            // new format without total length of all extensions
            int len;
            while ((len = in.readInt()) > 0) {
                in.skipBytes(len);
            }

            // read time of recording
            in.readLong();

            try {
                // read messages
                while (true) {
                    // read header
                    int size = in.readInt();
                    // totalSize += size;
                    int encoding = in.readByte();
                    size--;
                    if ((encoding & 128) != 0) {
                        // timestamp bit set -> message contains timestamp
                        int timestamp = in.readInt();
                        if (maxtimestamp < timestamp)
                            maxtimestamp = timestamp;
                        encoding &= 127;
                        // remove timestamp bit
                        size -= 4; // size - timestamp
                    } // else keep previous timestamp
                    in.skipBytes(size);
                }
            } catch (EOFException e) {}
        } catch (IOException e) {}
        return maxtimestamp;
    }

    // Copies src file to dst file.
    // If the dst file does not exist, it is created
    static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    static void printUsage() {
        System.out.println("TTTInfo <inputfile> ");

        System.exit(0);
    }
}
