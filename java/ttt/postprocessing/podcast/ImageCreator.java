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
 * Created on Jul 11, 2003
 */
package ttt.postprocessing.podcast;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

/**
 * @author ziewer
 */
public class ImageCreator {

    public static void writeImage(Image image, String fileName) {
        // Save as PNG
        try {
            if (!fileName.endsWith(".png"))
                fileName += ".png";
            File file = new File(fileName);
            ImageIO.write(toBufferedImage(image, 0), "png", file);
        } catch (IOException e) {
            System.out.println("Couldn't write image: " + e);
        }
    }

    public static void writeImageOLD(Image image, String fileName) {
        // Save as PNG
        try {
            // create directory
            File file = new File(fileName.substring(0, fileName.length() - 3) + "_script");
            file.mkdir();

            // remove path
            int i = fileName.lastIndexOf(File.separatorChar);
            if (i >= 0)
                fileName = fileName.substring(i);
            // build new path
            fileName = file.getAbsolutePath() + File.separator + fileName;
            // write screenshot
            file = new File(fileName + ".png");
            ImageIO.write(toBufferedImage(image, 0), "png", file);
        } catch (IOException e) {
            System.out.println("Couldn't write image: " + e);
        }
    }

    // This method returns a buffered image with the contents of an image
    public static BufferedImage toBufferedImage(Image image, int mode) {
        if (mode == 0 && image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        BufferedImage bimage = null;

        // Create a buffered image using the default color model
        int type;
        switch (mode) {
        case 1:
            type = BufferedImage.TYPE_BYTE_GRAY;
            break;
        case 2:
            type = BufferedImage.TYPE_BYTE_BINARY;
            break;
        default:
            type = BufferedImage.TYPE_INT_ARGB;
            break;
        }

        bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }

    static void listFormats() {

        // Get list of unique supported read formats
        String[] formatNames = ImageIO.getReaderFormatNames();
        formatNames = unique(formatNames);
        // e.g. png jpeg gif jpg
        System.out.println("Read formats:");
        for (int i = 0; i < formatNames.length; i++) {
            System.out.println(formatNames[i]);
        }

        // Get list of unique supported write formats
        formatNames = ImageIO.getWriterFormatNames();
        formatNames = unique(formatNames);
        // e.g. png jpeg jpg
        System.out.println("\nWrite formats:");
        for (int i = 0; i < formatNames.length; i++) {
            System.out.println(formatNames[i]);
        }

        // Get list of unique MIME types that can be read
        formatNames = ImageIO.getReaderMIMETypes();
        formatNames = unique(formatNames);
        // e.g image/jpeg image/png image/x-png image/gif
        System.out.println("\nReader MIME types:");
        for (int i = 0; i < formatNames.length; i++) {
            System.out.println(formatNames[i]);
        }

        // Get list of unique MIME types that can be written
        formatNames = ImageIO.getWriterMIMETypes();
        formatNames = unique(formatNames);
        // e.g. image/jpeg image/png image/x-png
        System.out.println("\nWriter MIME types:");
        for (int i = 0; i < formatNames.length; i++) {
            System.out.println(formatNames[i]);
        }
    }

    // Converts all strings in 'strings' to lowercase
    // and returns an array containing the unique values.
    // All returned values are lowercase.
    public static String[] unique(String[] strings) {
        Set<String> set = new HashSet<String>();
        for (int i = 0; i < strings.length; i++) {
            String name = strings[i].toLowerCase();
            set.add(name);
        }
        return set.toArray(new String[0]);
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@code
     * BufferedImage}.
     *
     * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
     *
     * @param image
     *            the original image to be scaled
     * @param targetWidth
     *            the desired width of the scaled instance, in pixels
     * @param targetHeight
     *            the desired height of the scaled instance, in pixels
     * @param hint
     *            one of the rendering hints that corresponds to {@code
     *            RenderingHints.KEY_INTERPOLATION} (e.g. {@code
     *            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, {@code
     *            RenderingHints.VALUE_INTERPOLATION_BILINEAR}, {@code
     *            RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality
     *            if true, this method will use a multi-step scaling technique
     *            that provides higher quality than the usual one-step technique
     *            (only useful in downscaling cases, where {@code targetWidth}
     *            or {@code targetHeight} is smaller than the original
     *            dimensions, and generally only when the {@code BILINEAR} hint
     *            is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
     public static BufferedImage getScaledInstance(BufferedImage image,
                     int targetWidth, int targetHeight, Object hint,
                     boolean higherQuality)
     {
             int type = (image.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                             : BufferedImage.TYPE_INT_ARGB;
             BufferedImage ret = (BufferedImage) image;
             int w, h;
             if (higherQuality)
             {
                     // Use multi-step technique: start with original size, then
                     // scale down in multiple passes with drawImage()
                     // until the target size is reached
                     w = image.getWidth();
                     h = image.getHeight();
             } else
             {
                     // Use one-step technique: scale directly from original
                     // size to target size with a single drawImage() call
                     w = targetWidth;
                     h = targetHeight;
             }
	     
	     if (w < targetWidth || h < targetHeight){
                     BufferedImage tmp = new BufferedImage(targetWidth, targetHeight, type);
                     Graphics2D g2 = tmp.createGraphics();
                     g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
                     g2.drawImage(ret, 0, 0, targetWidth, targetHeight, null);
                     g2.dispose();
                     return tmp;
	     }

             do
             {

                     if (higherQuality && w > targetWidth)
                     {
                             w /= 2;
                             if (w < targetWidth)
                             {
                                     w = targetWidth;
                             }
                     }
    
                     if (higherQuality && h > targetHeight)
                     {
                             h /= 2;
                             if (h < targetHeight)
                             {
                                     h = targetHeight;
                             }
                     }
    
                     BufferedImage tmp = new BufferedImage(w, h, type);
                     Graphics2D g2 = tmp.createGraphics();
                     g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
                     g2.drawImage(ret, 0, 0, w, h, null);
                     g2.dispose();
    
                     ret = tmp;
             } while (w != targetWidth || h != targetHeight);
    
             return ret;
     }
     
    public static void main(String args[]) {
        listFormats();
    }
    
}
