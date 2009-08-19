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
 * Created on 17.10.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ttt.messages.CursorMessage;
import ttt.messages.CursorPositionMessage;
import ttt.messages.FramebufferUpdateMessage;
import ttt.messages.Message;

public class Analyses {

    /**
     * @param args
     */
    public static void main(String[] args) {
        inconsistencyCheck(args);
        System.exit(0);

        if (args.length == 0) {
            usage();
            System.exit(0);
        }

        int min_kbytes = 0;
        int min_area_percentage = 0;

        while (args.length > 0 && args[0].startsWith("-")) {
            if (args[0].equalsIgnoreCase("-process")) {
                args = shift(args);
                fast_processing(args, false);
                System.exit(0);
            } else if (args[0].equalsIgnoreCase("-process_stripes")) {
                args = shift(args);
                fast_processing(args, true);
                System.exit(0);
            } else if (args[0].equalsIgnoreCase("-mpix")) {
                args = shift(args);
                megapixels(args);
                System.exit(0);
            } else if (args[0].equalsIgnoreCase("-min") && args.length > 1) {
                args = shift(args);

                if (args[0].endsWith("%"))
                    min_area_percentage = Integer.parseInt(args[0].substring(0, args[0].length() - 1));
                else
                    min_kbytes = Integer.parseInt(args[0]);

                args = shift(args);
            }
        }

        message_sizes(args, min_kbytes, min_area_percentage);
    }

    public static void usage() {
        System.out.println("Java ttt.Analyses [-min <value>] [-min <value>%] file1 [file2 ...]");
        System.out.println("Java ttt.Analyses [-process] file1 [file2 ...]");
        System.out.println("Java ttt.Analyses [-process_stripes] file1 [file2 ...]");
        System.out.println("Java ttt.Analyses [-mpix] file1 [file2 ...]");
        System.out.println("\tcalculates size and affected areas of framebuffer updates");
        System.out.println("\t-min <value> to adjust min value in bytes");
        System.out.println("\t-min <value>% to adjust min value in percentage of area");
        System.out.println("\t-process to run fast processing of all messages");
        System.out
                .println("\t-process_stripes to run fast processing of all messages regarding update stripes (2 min check)");
        System.out.println("\t-mpix delivers the amount of pixels effected by the recording (in megapixels)");
    }

    public static String[] shift(String[] args) {
        if (args != null) {
            String[] shifted = new String[args.length - 1];
            System.arraycopy(args, 1, shifted, 0, shifted.length);
            return shifted;
        }
        return null;
    }

    public static void message_sizes(String[] args, int min_kbytes, int min_areas) {
        TTT.verbose = false;

        for (String arg : args) {
            try {
                System.out.print(arg);
                Recording recording = new Recording(arg, false);
                File file_kbytes = new File("/netzlaufwerk/stats/sizes/" + recording.getFileBase() + ".kbytes");
                File file_areas = new File("/netzlaufwerk/stats/sizes/" + recording.getFileBase() + ".areas");

                file_kbytes.getParentFile().mkdirs();

                FileWriter writer_bytes = new FileWriter(file_kbytes);
                FileWriter writer_areas = new FileWriter(file_areas);

                int timestamp = 0;
                int bytes = 0;
                int kbytes_dropped = 0;
                int area = 0;
                int count_bytes = 0;
                int count_areas = 0;
                int number_of_pixels = recording.prefs.framebufferHeight * recording.prefs.framebufferWidth;
                for (int i = 0; i < recording.messages.size(); i++) {
                    Message message = recording.messages.get(i);
                    if (!(message instanceof FramebufferUpdateMessage) || (message instanceof CursorMessage)
                            || (message instanceof CursorPositionMessage)) {
                        // System.out.println(message.getSize()+"\t"+message.getClass());
                        kbytes_dropped += message.getSize();
                        continue;
                    }

                    // System.out.println(Constants.getStringFromTime(message.getTimestamp()) + "\t" + number_of_pixels
                    // + "\t" + ((FramebufferUpdateMessage) message).getCoveredArea() + "\t" + 100d
                    // * ((FramebufferUpdateMessage) message).getCoveredArea() / number_of_pixels+"\t"+message);

                    if (message.getTimestamp() != timestamp || i + 1 == recording.messages.size()) {
                        // System.out.println(Constants.getStringFromTime(timestamp) + "\t" + size);

                        if (bytes > min_kbytes) {
                            count_bytes++;
                            writer_bytes.write(timestamp / 60000d + "\t" + bytes / 1000d + "\n");
                        }

                        double percentage = 100d * area / number_of_pixels;
                        if (percentage > 100)
                            percentage = 100;
                        if (percentage > min_areas) {
                            count_areas++;
                            writer_areas.write(timestamp / 60000d + "\t" + percentage + "\n");
                            // System.out.println(message+"\t"+percentage);
                        }

                        timestamp = message.getTimestamp();
                        bytes = message.getSize();
                        area = ((FramebufferUpdateMessage) message).getCoveredArea();
                    } else {
                        bytes += message.getSize();
                        area += ((FramebufferUpdateMessage) message).getCoveredArea();
                    }

                }

                writer_bytes.flush();
                writer_bytes.close();
                writer_areas.flush();
                writer_areas.close();
                System.out.println("\tok" + "\tdropped " + kbytes_dropped / 1000 + " kbytes other msg types");
                System.out.println("\t" + count_bytes + " values exceeding " + min_kbytes + " bytes \t" + count_areas
                        + " values exceeding " + min_areas + "% of area");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }

    public static void fast_processing(String[] args, boolean stripes) {
        TTT.verbose = false;

        int number = 0;

        for (String arg : args) {
            try {
                // System.out.println();
                for (int j = 0; j < 1; j++) {
                    // fast processing
                    long time = System.currentTimeMillis();
                    Recording recording = new Recording(arg, false);
                    recording.graphicsContext.enableRefresh(false);

                    // System.out.print(arg + "\tprocessing " + recording.messages.size() + " messages in ");
                    // for (int i = 0; i < recording.messages.size(); i++) {
                    // Message message = recording.messages.get(i);
                    // message.paint(recording.graphicsContext);
                    // }
                    // time = System.currentTimeMillis() - time;
                    // System.out.println(Constants.getStringFromTime((int) time) + "\t- average: " + time * 1000000
                    // / recording.messages.size() + " nanosecs per msg");

                    // worst case - backward
                    time = System.currentTimeMillis();

                    // System.out.print(arg + "\tprocessing " + recording.messages.size() + " messages in ");
                    int count = 0;
                    for (int i = recording.getDuration(); i >= 0; i -= 60000) {
                        // recording.setTime(i);
                        if (stripes)
                            recording.messages.setTime_full_frame_check_regarding_stripes(i);
                        else
                            recording.messages.setTime_full_frame_check(i);
                        count++;
                    }
                    time = System.currentTimeMillis() - time;
                    // System.out.println(Constants.getStringFromTime((int) time) + "\t- average: " + time / count + "
                    // ");
                    String date = recording.getFileBase().substring(recording.getFileBase().indexOf('_') + 1);
                    System.out.println((++number) + "\t" + time / count + "\t" + date);

                    // int sum = 0;
                    // int prev_timestamp = recording.getDuration();
                    // count = 0;
                    // for (int i = recording.messages.size()-1; i >= 0; i--) {
                    // int timestamp = recording.messages.get(i).getTimestamp();
                    // if (timestamp != prev_timestamp) {
                    // time = System.currentTimeMillis();
                    // recording.setTime(recording.messages.get(i).getTimestamp());
                    // time = System.currentTimeMillis() - time;
                    // sum += time * (prev_timestamp - timestamp);
                    // prev_timestamp = timestamp;
                    // count++;
                    // }
                    // }
                    // System.out.println("average seek time: " + sum/recording.getDuration()+" msec \t"+count);

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void megapixels(String[] args) {
        TTT.verbose = false;

        int number = 0;

        for (String arg : args) {
            try {
                // System.out.println();
                Recording recording = new Recording(arg, false);
                recording.graphicsContext.enableRefresh(false);

                // System.out.println(Constants.getStringFromTime((int) time) + "\t- average: " + time / count + "
                // ");
                String date = recording.getFileBase().substring(recording.getFileBase().indexOf('_') + 1);
                int mpix = recording.messages.getNumberOfPixels() / (1024 * 1024);
                int kpix_per_min = recording.messages.getNumberOfPixels() / (recording.getDuration() / 60000) / 1024;
                System.out.println((++number) + "\t" + (mpix < 100 ? " " : "") + mpix + " Mpixel\t" + date + "\t"
                        + (kpix_per_min < 1000 ? " " : "") + kpix_per_min + " kpixel/min");

                // int sum = 0;
                // int prev_timestamp = recording.getDuration();
                // count = 0;
                // for (int i = recording.messages.size()-1; i >= 0; i--) {
                // int timestamp = recording.messages.get(i).getTimestamp();
                // if (timestamp != prev_timestamp) {
                // time = System.currentTimeMillis();
                // recording.setTime(recording.messages.get(i).getTimestamp());
                // time = System.currentTimeMillis() - time;
                // sum += time * (prev_timestamp - timestamp);
                // prev_timestamp = timestamp;
                // count++;
                // }
                // }
                // System.out.println("average seek time: " + sum/recording.getDuration()+" msec \t"+count);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void inconsistencyCheck(String[] args) {
        TTT.verbose = false;

        for (String arg : args) {
            try {
                System.out.println(arg);
                new Recording(arg, false);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
