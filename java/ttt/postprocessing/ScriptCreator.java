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
 * Created on 21.03.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.postprocessing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;


import javax.imageio.ImageIO;

import ttt.Constants;
import ttt.TTT;
import ttt.audio.Exec;
import ttt.postprocessing.podcast.ImageCreator;
import ttt.record.Recording;


public class ScriptCreator {

    public final static int HTML_SCRIPT = 1;
    public final static int OCR_OPTIMIZED = 2;
    public final static int THUMBNAILS = 4;
    public final static int PDF_SCRIPT = 8;

    private Recording recording;
    private String index_file_base;
    private String file_base;
    private String ocr_path;

    private boolean html4 = true;

    public ScriptCreator(Recording recording, int mode) throws IOException {
        this.recording = recording;

        // create directory for html index
        boolean generate_script = true;

        String directory = recording.getDirectory();
        file_base = recording.getFileBase();

        ocr_path = directory + file_base + ".ocr";
        if ((mode & OCR_OPTIMIZED) != 0)
            generate_script &= createDirectory(ocr_path);

        index_file_base = directory + file_base + ".html";
        if ((mode & HTML_SCRIPT) != 0) {
            generate_script &= createDirectory(index_file_base);
            generate_script &= createDirectory(index_file_base + File.separator + "thumbs");
            generate_script &= createDirectory(index_file_base + File.separator + "html");
            generate_script &= createDirectory(index_file_base + File.separator + "images");
            if (generate_script) {
                writeMainIndexAndThumbnailOverview();
            } else {
                System.out.println("\nERROR: Script generation failed. Could not create directories.\n");
            }
        }
        
    }

    public ScriptCreator(Recording recording, int mode, String ocrPath) throws IOException {
        this.recording = recording;

        // create directory for html index
        boolean generate_script = true;

        String directory = recording.getDirectory();
        file_base = recording.getFileBase();

        ocr_path = ocrPath;//directory + file_base + ".ocr";
        if ((mode & OCR_OPTIMIZED) != 0)
            generate_script &= createDirectory(ocr_path);

        index_file_base = directory + file_base + ".html";
        if ((mode & HTML_SCRIPT) != 0) {
            generate_script &= createDirectory(index_file_base);
            generate_script &= createDirectory(index_file_base + File.separator + "thumbs");
            generate_script &= createDirectory(index_file_base + File.separator + "html");
            generate_script &= createDirectory(index_file_base + File.separator + "images");
            if (generate_script) {
                writeMainIndexAndThumbnailOverview();
            } else {
                System.out.println("\nERROR: Script generation failed. Could not create directories.\n");
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length == 0)
            usage();
        else
            TTT.verbose = false;
        for (String arg : args) {
            File dir = new File(arg);
            if (dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    if (file.toString().toLowerCase().endsWith(".ttt")) {
                        System.out.print(file + "\t");
                        Recording recording = new Recording(file.getCanonicalPath(), false);
                        int mode = HTML_SCRIPT |  PDF_SCRIPT;
                        if (recording.index.searchbaseFormatStored==recording.index.NO_SEARCHBASE) mode = mode | OCR_OPTIMIZED;  
                        recording.createScript(mode);
                        if (recording.index.getSearchbaseFormat()!=recording.index.searchbaseFormatStored) recording.store();
                        System.out.println();
                    }
                }
            } else {
                System.out.print(arg + "\t");
                Recording recording = new Recording(arg, false);
                int mode = HTML_SCRIPT |  PDF_SCRIPT;
                if (recording.index.searchbaseFormatStored==recording.index.NO_SEARCHBASE) mode = mode | OCR_OPTIMIZED;  
                recording.createScript(mode);
                if (recording.index.getSearchbaseFormat()!=recording.index.searchbaseFormatStored) recording.store();
            }
        }
        System.exit(0);
    }

    static void usage() {
        System.out.println();
        System.out.println("TeleTeachingTool Script Creator:");
        System.out.println();
        System.out.println("java [-cp <classpath>] ttt.ScriptCreator <file1> [<file2>] [<file3>] [...]");
        System.out.println();
        System.out.println("\t- creates HTML script for all given recordings file1, file2, file3, ...");
        System.out
                .println("\t  HTML script is written to subfolder filebase.html (where filebase is the filename without ending)");
        System.out.println("\t  progress to subpath if <fileX> is an directory");
        System.out.println();
        System.out
                .println("\t- additionally a subfolder filebase.ocr is written containing index sceenshots without annotations");
        System.out
                .println("\t  used for Optical Character Recognition to generate a searchbase for the full text search feature of TTT");
        System.out.println();
    }

    /*******************************************************************************************************************
     * html script generation * *
     ******************************************************************************************************************/
    // html thumbnail overview
    void writeMainIndexAndThumbnailOverview() {
        // style sheet
        writeStyleSheet();
        // index.html as entry point
        String fileName = index_file_base + "/" + "index.html";
        try {
            File file = new File(fileName);
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            // hack for nicer html title
            writeHtmlHead(-1, out, true);
            writeRecordingInformation(out);
            out.println("<p><h2><a href=\"html" + "/" + file_base + ".html\"><b>Overview</b></a></h2>");
            writeHtmlTail(out);
        } catch (IOException e) {
            System.out.println("Couldn't write html: " + e);
        }

        // thumbnail overview
        fileName = index_file_base + "/" + "html" + "/" + file_base + ".html";
        try {
            File file = new File(fileName);
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            // hack for nicer html title
            writeHtmlHead(-1, out);
            out.println("<a href=\"../index.html\" style=\"float:right;\">Main Index</a>");
            writeRecordingInformation(out);
            writeThumbnailTable(out);
            writeHtmlTail(out);
        } catch (IOException e) {
            System.out.println("Couldn't write html: " + e);
        }
    }

    void writeRecordingInformation(PrintWriter out) {
        // header
        out.println("<table cellpadding=\"5\">");
        out.println("<tr><td>name: </td><td><b>" + recording.prefs.name + "</b></td></tr>");
        out.println("<tr><td>recorded: </td><td><b>" + new Date(recording.prefs.starttime) + "</b></td></tr>");
        out.println("<tr><td>length: </td><td><b>" + Constants.getStringFromTime(recording.getDuration(), false)
                + " min.</b></td></tr>");
        out.println("</table>\n\n<p>");
    }

    void writeThumbnailTable(PrintWriter out) {
        // add thumbnails
        for (int i = 0; i < recording.index.size(); i++) {
            String time = Constants.getStringFromTime(recording.index.get(i).getTimestamp(), false);
            String html = file_base + "." + ((i + 1) < 10 ? "0" : "") + (i + 1) + ".html";
            String thumbnail = ".." + "/" + "thumbs" + "/" + file_base + ".thumb." + ((i + 1) < 10 ? "0" : "")
                    + (i + 1) + ".png";

            // open index entry

            // index label with number and timestamp
            if (html4)
                out.println("<fieldset><legend>");
            out.println("<a name=\"" + (i + 1) + "\">");
            out.println("<b>#" + (i + 1) + ":</b> " + time + " min.");
            out.println("</a>");
            if (html4)
                out.println("</legend>");
            else
                out.println("<br>");

            // thumbnail
            out.println("<a href=\"" + html + "\"><img src=\"" + thumbnail + "\" title=\"#" + (i + 1) + ": " + time
                    + " min.\"></a>");

            // close index entry
            if (html4)
                out.println("</fieldset>");
        }
    }

    /**
     * OCR screenshots and processing with tesseract
     * @param index Index of page to generate and read
     * @param screenshot screenshot data
     * @return name of the generated image file
     */
    public String writeOCRScreenshot(int index, Image screenshot) {
    	String filename = ocr_path + File.separator + file_base + "." + ((index + 1) < 10 ? "0" : "")
                + ((index + 1) < 100 ? "0" : "") + (index + 1) + ".png";
        ImageCreator.writeImage(screenshot, filename);
        String os = System.getProperty("os.name");
        if (Exec.getCommand("tesseract")!=null){
      	  Exec exec = new Exec();
      	  try {
      		  exec.exec(new String[] {
      				  "tesseract",
      				  filename,
      				  filename+".hocr",
      				  "hocr"
      		  });
      	  } catch (Exception e) {
      		  // TODO Auto-generated catch block
      		  e.printStackTrace();
      		  return null;
      	  }

        }
		return filename;        
    }

    // create html, screenshot and thumbnail for current index
    public void writeIndex(int nr, Image screenshot) {
        // write screenshot
        String fileName = index_file_base + File.separator + "images" + File.separator + file_base + "."
                + ((nr + 1) < 10 ? "0" : "") + (nr + 1) + ".png";
        writeImage(screenshot, fileName);

        // write thumbnail
        fileName = index_file_base + File.separator + "thumbs" + File.separator + file_base + ".thumb."
                + ((nr + 1) < 10 ? "0" : "") + (nr + 1) + ".png";

        writeImage(getScaledInstance(screenshot, recording.prefs.framebufferWidth
                / recording.index.getThumbnailScaleFactor(), recording.prefs.framebufferHeight
                / recording.index.getThumbnailScaleFactor()), fileName);

        // write html
        try {
            fileName = index_file_base + File.separator + "html" + File.separator + file_base + "."
                    + ((nr + 1) < 10 ? "0" : "") + (nr + 1) + ".html";

            File file = new File(fileName);
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            writeHtmlHead(nr, out);
            writeHtmlBody(nr, out);
            writeHtmlTail(out);
        } catch (IOException e) {
            System.out.println("Couldn't write html: " + e);
        }
    }

    void writeHtmlBody(int nextIndex, PrintWriter out) {
        // name of image to display
        // NOTE: must use "/" instead of File.separator here because this is HTML and not a OS dependent file name
        String imageName = ".." + "/" + "images" + "/" + file_base + "." + ((nextIndex + 1) < 10 ? "0" : "")
                + (nextIndex + 1) + ".png";
        // link to index html file
        String index = file_base + ".html";
        // link to previous html file
        String prev = nextIndex > 0 ? file_base + "." + ((nextIndex) < 10 ? "0" : "") + (nextIndex) + ".html" : null;
        // link to next html file
        String next = nextIndex + 1 < recording.index.size() ? file_base + "." + ((nextIndex + 2) < 10 ? "0" : "")
                + (nextIndex + 2) + ".html" : null;

        // body
        //
        // prev - index - next
        out.println("<center><b>");
        if (prev != null)
            out.println("<a href=\"" + prev + "\">prev</a> - ");
        else
            out.println("<font color=\"white\">prev - </font>");
        out.println("<a href=\"" + index + "#" + (nextIndex + 1) + "\">overview</a>");
        if (next != null)
            out.println(" - <a href=\"" + next + "\">next</a>");
        else
            out.println("<font color=\"white\"> - next</font>");
        out.println("<br>");

        // image
        String time = Constants.getStringFromTime(recording.index.get(nextIndex).getTimestamp(), false);
        if (next != null)
            out.println("<a href=\"" + next + "\">");
        else
            out.println("<a href=\"" + index + "\">");
        out.println("<img src=\"" + imageName + "\" title=\"" + "#" + (nextIndex + 1) + ": " + time + " min.\"></a>");
        out.println("<br>");

        // prev - index - next
        if (prev != null)
            out.println("<a href=\"" + prev + "\">prev</a> - ");
        else
            out.println("<font color=\"white\">prev - </font>");
        out.println("<a href=\"" + index + "#" + (nextIndex + 1) + "\">overview</a>");
        if (next != null)
            out.println(" - <a href=\"" + next + "\">next</a>");
        else
            out.println("<font color=\"white\"> - next</font>");
        out.println("</b></center>");
    }

    // html head
    void writeHtmlHead(int nextIndex, PrintWriter out) {
        writeHtmlHead(nextIndex, out, false);
    }

    void writeHtmlHead(int nextIndex, PrintWriter out, boolean style_in_subdirectory) {
        // header
        out.println("<html>\n" + "<head>\n" + "<title>" + recording.prefs.name
                + (nextIndex > 0 ? " [" + (nextIndex + 1) + "]" : "") + "</title>\n");
        // index.html is located in parent directory, all other htmls & style.css in subdirectory 'html'
        out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + (style_in_subdirectory ? "html" + "/" : "")
                + "style.css\">");
        out.println("</head>\n" + "<body>\n");
    }

    // html end
    void writeHtmlTail(PrintWriter out) {
        // close html
        out.println("</body>\n" + "<html>");
        out.flush();
        out.close();
    }

      
    
    
    // css style sheet
    void writeStyleSheet() {
        try {
            String fileName = index_file_base + "/" + "html" + "/" + "style.css";
            File file = new File(fileName);
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));

            out.println("BODY { background-color:white;font-family: Arial,Helvetica,Sans-Serif;\n"
                    + "font-style: normal; color:#222222; text-align: left;}\n"
                    + "a {font-weight:bold; text-decoration:none}\n" + "a:link,a:visited   { color:#0075BA; }\n"
                    + "a:link img,a:visited img   { border: thin solid #0075BA; }\n"
                    + "a:hover,a:active,a:focus  { color:red; }\n"
                    + "a:hover img,a:active img,a:focus img  { border: thin solid red; }\n"
                    + "fieldset { padding:5px; display:inline }\n" + "legend  { font-size:x-small; }");
            out.flush();
            out.close();
        } catch (IOException e) {
            System.out.println("Couldn't write html: " + e);
        }
    }

    static private boolean createDirectory(String path) {
        File directory = new File(path);

        if (directory.exists())
            return directory.isDirectory();
        else
            try {
            	if(TTT.verbose)
                System.out.print("create directory: " + directory.getCanonicalPath());
                if (!directory.mkdir()){
                	if(TTT.verbose)
                    System.out.println(" - FAILED");
                }
                else
                {
                	if(TTT.verbose)
                    System.out.println();
                }
            } catch (IOException e) {}
        return directory.exists();
    }

    // ////////////////////////////////////////////
    // Screenshot
    // ////////////////////////////////////////////

    static void writeImage(Image image, String fileName) {
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
    static BufferedImage toBufferedImage(Image image) {
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

    public static Image getScaledInstance(Image img, int targetWidth, int targetHeight) {
        // // Image.getScaledInstance() is pretty lame
        // // NOTE: unscaled image resists in memory until drawn - draw to new image to free memory
        // return img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        // that is about 30% faster under Linux
        // TODO: Image.getScaledInstance() is suposed to be improved in Java 7 - check again
        return getScaledInstance(toBufferedImage(img), targetWidth, targetHeight,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@code BufferedImage}.
     * 
     * from http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
     * 
     * @param img
     *            the original image to be scaled
     * @param targetWidth
     *            the desired width of the scaled instance, in pixels
     * @param targetHeight
     *            the desired height of the scaled instance, in pixels
     * @param hint
     *            one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality
     *            if true, this method will use a multi-step scaling technique that provides higher quality than the
     *            usual one-step technique (only useful in downscaling cases, where {@code targetWidth} or
     *            {@code targetHeight} is smaller than the original dimensions, and generally only when the
     *            {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint,
            boolean higherQuality) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
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
}
