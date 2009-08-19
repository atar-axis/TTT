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
 * Created on 25.07.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

public class SlideComparator {

    public static void main(String[] args) {
        slideCompare(args);
    }

    public static void slideCompare(String[] args) {
        for (String arg : args) {
            if (new File(arg).isDirectory()) {
                System.out.println("\n\nDir: " + arg);
                File[] files = new File(arg).listFiles();
                TreeSet<String> filenames = new TreeSet<String>();
                for (File file : files) {
                    filenames.add(file.getAbsolutePath());
                }
                pixelDiff(filenames.toArray(new String[filenames.size()]));
            } else
                pixelDiff(args);
        }
    }

    public static void pixelDiff(String[] args) {
        ArrayList<int[]> images = new ArrayList<int[]>();
        ArrayList<int[][]> colorHistograms = new ArrayList<int[][]>();

        int count = 0;
        for (String arg : args) {
            // System.out.println("\nLoading: " + arg);
            System.out.print("#" + (++count) + ":\t");
            BufferedImage image = loadImage(arg);
            if (image != null) {
                int[] newPic = getPixels(image);
                int[][] histogram = getColorHistogram(newPic);

                int percentage = ((int) (((double) (histogram[0][1]) / (newPic.length)) * 10000));
                System.out.println(percentage / 100d + "%");

                // diff
                for (int i = 0; i < images.size(); i++) {
                    int diff = pixelDiff(images.get(i), newPic);
                    if (diff < 10000) {
                        int[][] histogram2 = colorHistograms.get(i);
                        int colorDiff1 = Integer.MAX_VALUE;
                        if (histogram.length > 0 && histogram2.length > 0 && histogram[0][0] == histogram2[0][0])
                            colorDiff1 = Math.abs(histogram[0][1] - histogram2[0][1]);

                        int colorDiff2 = Integer.MAX_VALUE;
                        if (histogram.length > 1 && histogram2.length > 1 && histogram[1][0] == histogram2[1][0])
                            colorDiff2 = Math.abs(histogram[1][1] - histogram2[1][1]);

                        int colorDiff3 = Integer.MAX_VALUE;
                        if (histogram.length > 2 && histogram2.length > 2 && histogram[2][0] == histogram2[2][0])
                            colorDiff3 = Math.abs(histogram[2][1] - histogram2[2][1]);

                        System.out.println("   diff( " + (i + 1) + " , " + (images.size() + 1) + " )\t" + diff
                                + "\tcol diff:\t" + colorDiff1 + "\t" + colorDiff2 + "\t" + colorDiff3);
                        // System.out.println(i + 1);
                        // getColorHistogram(images.get(i), true);
                        // System.out.println(images.size() + 1);
                        // getColorHistogram(newPic, true);
                    }
                }

                images.add(newPic);
                colorHistograms.add(histogram);
            }
        }
    }

    public static int pixelDiff(int[] a, int[] b) {
        int diff = 0;
        for (int i = 0; i < a.length && i < b.length; i++) {
            if (a[i] != b[i])
                diff++;
        }
        return diff;
    }

    public static BufferedImage loadImage(String filename) {
        try {
            // Read from a file
            File file = new File(filename);
            return ImageIO.read(file);
        } catch (IOException e) {}
        return null;
    }

    // get pixel map from offscreen image
    public static int[] getPixels(BufferedImage image) {
        int[] pixel = new int[image.getWidth() * image.getHeight()];
        try {
            PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, image.getWidth(), image.getWidth(), pixel, 0,
                    image.getWidth());
            pixelGrabber.grabPixels();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return pixel;
    }

    public static int[][] getColorHistogram(int[] pixels) {
        return getColorHistogram(pixels, false);
    }

    public static int[][] getColorHistogram(int[] pixels, boolean verbose) {
        // count occurrences of color (combined RGB)
        HashMap<Integer, Integer> hash = new HashMap<Integer, Integer>();
        for (int pixel : pixels) {
            pixel &= 0x00FFFFFF;

            Integer i = hash.get(new Integer(pixel));
            if (i == null)
                hash.put(pixel, 1);
            else
                hash.put(pixel, ++i);
        }
        // System.out.println("\t" + hash.size() + " entries:");

        // sort by number of occurrences
        Set<Entry<Integer, Integer>> set = hash.entrySet();
        Comparator<Entry<Integer, Integer>> comparator = new Comparator<Entry<Integer, Integer>>() {
            public int compare(Entry<Integer, Integer> arg0, Entry<Integer, Integer> arg1) {
                return -arg0.getValue().compareTo(arg1.getValue());
            }
        };
        TreeSet<Entry<Integer, Integer>> treeSet = new TreeSet<Entry<Integer, Integer>>(comparator);
        treeSet.addAll(set);

        int[][] colorHistogram = new int[treeSet.size()][2];
        int i = 0;
        for (Entry<Integer, Integer> e : treeSet) {
            colorHistogram[i][0] = e.getKey(); // color
            colorHistogram[i++][1] = e.getValue(); // occurrences
        }

        if (verbose) {
            int total = 0;
            for (Entry<Integer, Integer> e : treeSet) {
                int percentage = ((int) (((double) (e.getValue()) / (pixels.length)) * 10000));
                if (total < pixels.length * 0.90) {
                    Color color = new Color(e.getKey());// decodeColor(e.getKey());
                    System.out.println("\t" + "[ r:" + color.getRed() + "\tg:" + color.getGreen() + "\tb:"
                            + color.getBlue() + "\t] " + percentage / 100d + "%  \tabsolut " + e.getValue());
                    total += e.getValue();
                }
            }
            System.out.println();
        }

        return colorHistogram;
    }
}
