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
 * Created on 15.06.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

public class ColorHistogram {
    public static void main(String[] args) throws IOException {
        listColorHistogramOccurrences(args);
    }

    static PrintWriter out;

    public static void listColorHistogramOccurrences(String[] args) throws IOException {
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter("histograms.html")));
            // html header
            out.println("<html>\n" + "<head>\n" + "<title>" + "Color Histograms" + "</title>\n");
            out.println("</head>\n" + "<body>\n");

            for (String arg : args) {
                File dir = new File(arg);
                if (dir.isDirectory()) {
                    System.out.println("\n\nDir: " + arg);
                    File subdir = new File(dir, "images");
                    if (subdir.isDirectory())
                        dir = subdir;
                    File[] files = dir.listFiles();
                    TreeSet<String> filenames = new TreeSet<String>();
                    for (File file : files) {
                        filenames.add(file.getAbsolutePath());
                    }
                    analyzeColorHistogramOccurrences(filenames.toArray(new String[filenames.size()]));
                } else
                    analyzeColorHistogramOccurrences(args);
            }

        } finally {
            // close html
            out.println("</body>\n" + "<html>");
            out.flush();
            out.close();
        }
    }

    public static void analyzeColorHistogramOccurrences(String[] args) {
        int count = 0;
        for (String arg : args) {
            count++;

            // System.out.println("image: " + arg);

            String thumb = arg.replaceFirst("images", "thumbs");
            int pos = thumb.substring(0, thumb.length() - 6).lastIndexOf('.');
            String statistics = new File(thumb.substring(0, pos) + ".stat" + thumb.substring(pos)).getName();
            
            // local filesystem names
            thumb = thumb.substring(0, pos) + ".thumb" + thumb.substring(pos);            
            String href = arg;
            
            // web names
            href = "http://teleteaching.uni-trier.de/search/images/"+new File(arg).getName();
            thumb = "http://teleteaching.uni-trier.de/search/thumbs/"+new File(thumb).getName();
        
            System.out.print("#" + count + ":\t");
            BufferedImage image = loadImage(arg);
            if (image != null) {
                int[] pixels = getPixels(image);
                int[][] histogram = getColorHistogram(pixels);
                
                
                out.println("<a href="+href+"><img border=1 src=" + thumb + "></a>");
                out.println("<img src=" + statistics + "><br>");
                out.flush();

                writeImage(createStatisticsImage(histogram, pixels.length), statistics);

                // for (int i = 0; i < histogram.length; i++) {
                // int percentage = ((int) (((double) (histogram[i][1]) / (pixels.length)) * 10000));
                // if (percentage < 100)
                // break;
                // System.out.print(percentage / 100d + "%\t");
                // }
                // System.out.println();
            }
        }
    }

    public static Image createStatisticsImage(int[][] histogram, int pixelnumber) {
        // Create a buffered image using the default color model
        int width = 500;
        int height = 100;
        BufferedImage image = new BufferedImage(width + 300, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.createGraphics();

        // background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // scale lines
        g.setColor(Color.BLACK);
        for (int i = 0; i < 10; i++) {
            // // horizontal
            // g.drawLine(0, i * height / 10, width, i * height / 10);
            // vertical
            g.drawLine(i * width / 10, 0, i * width / 10, height);
        }

        // percentage bars of most frequently used colors
        for (int i = 0; i < Math.min(10, histogram.length); i++) {
            int percentage = ((int) (((double) (histogram[i][1]) / (pixelnumber)) * 10000));
            // if (percentage < 100)
            // break;
            // System.out.print(percentage / 100d + "%\t");

            int color = percentage / 100;
            if (color >= 85)
                g.setColor(Color.MAGENTA);
            else if (color >= 80)
                g.setColor(Color.YELLOW.darker());
            else if (color >= 60)
                g.setColor(Color.BLUE);
            else if (color >= 40)
                g.setColor(Color.ORANGE);
            else if (color >= 20)
                g.setColor(Color.CYAN);
            else if (color >= 10)
                g.setColor(Color.PINK);
            else if (color >= 5)
                g.setColor(Color.BLUE);
            else if (color >= 1)
                g.setColor(Color.GREEN);
            else
                g.setColor(Color.RED);

            // // horizontal
            // int y = (10000 - percentage) / 100;
            // g.fillRect(i * 10 + 1, y, 8, height - y);

            // vertical
            int x = percentage / 100 * width / 100;
            g.fillRect(0, i * height / 10 + 1, x, 8);
            g.setColor(g.getColor().darker());
            g.drawString((percentage / 100d) + "%", width + 5, i * height / 10 + 10);
        }
        // System.out.println();

        // border
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, width - 1, height - 1);

        // analyzis
        double most_freq = 100d * histogram[0][1] / pixelnumber;
        double second_most_freq = 100d * histogram[1][1] / pixelnumber;

        String classified_as;
        if ( most_freq >= 85)
            classified_as = "simple slide";
        else if ( most_freq >= 50) {
            if(second_most_freq > 5)
                classified_as = "complex slide [table/diagram]";
            else
                classified_as = "complex slide [image]";
        } else if (most_freq>=30)
            classified_as = "desktop or application";
        else if (most_freq < 5)
            classified_as = "full colored image/video";
        else
            classified_as = "unclassified";

        g.setColor(Color.BLACK);
        g.drawString(classified_as, width + 100, 10);
        System.out.println(most_freq + "\t" + classified_as);

        return image;
    }

    public static void writeImage(Image image, String fileName) {
        // Save as PNG
        try {
            if (!fileName.endsWith(".png"))
                fileName += ".png";
            File file = new File(fileName);
            ImageIO.write(toBufferedImage(image), "png", file);
        } catch (IOException e) {
            System.out.println("Couldn't write image: " + e);
        }
    }

    // This method returns a buffered image with the contents of an image
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage)
            return (BufferedImage) image;

        // Create a buffered image using the default color model
        BufferedImage bimage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
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
