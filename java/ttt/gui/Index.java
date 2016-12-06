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
 * Created on 27.02.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import ttt.Constants;
import ttt.TTT;
import ttt.audio.Exec;
import ttt.helper.LibraryChecker;
import ttt.messages.Annotation;
import ttt.messages.DeleteAllAnnotation;
import ttt.messages.FramebufferUpdateMessage;
import ttt.messages.Message;
import ttt.messages.WhiteboardMessage;
import ttt.postprocessing.ScriptCreator;
import ttt.record.Recording;

public class Index {

    // TODO: visibility
    public ArrayList<IndexEntry> index = new ArrayList<IndexEntry>();
    ArrayList<IndexEntry> search_index = new ArrayList<IndexEntry>();

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // constructors
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Recording recording;

    int getWidth() {
        return recording.prefs.framebufferWidth;
    }

    public Index(Recording recording) {
        this.recording = recording;

        // must contain at least one entry
        // TODO: needed?
        index.add(new IndexEntry(this));
    }

    public void close() {
        recording = null;
        index.clear();
        index = null;
        search_index.clear();
        search_index = null;
    }

    // is this index filled with data - computed or read from extension
    private boolean valid;

    public boolean isValid() {
        return valid;
    }

    // check if thumbnail is available for each index
    public boolean thumbnailsAvailable() {
        for (int i = 0; i < index.size(); i++) {
            if (index.get(i).getThumbnail() == null)
                return false;
        }
        return true;
    }

    // create index
    public void computeIndex() {
        computeIndex_regarding_length_of_sequence();
    }

    // create index
    void computeIndex_regarding_duration_of_sequence() {
        // build slide index

        if (TTT.verbose)
            System.out.println("\ncompute index table:\n");

        // delete index
        index.clear();

        // TODO: set as option
        // possible slide should at least cover 20% of maximum size
        int minSlideArea = recording.prefs.framebufferWidth * recording.prefs.framebufferHeight / 5;

        // TODO: set as option
        // there should be at least 5 or 10 sec between two slides
        int minSlideDiffMsecs = 10000;
        int minAnimationDuration = 5000;

        // count sequence with gaps less than minSlideDiffMsecs
        int animationCount = 0;

        int previous_timestamp = -1;
        int area = 0;
        boolean sequence = false;

        // build index based on covered area
        for (int i = 0; i < recording.messages.size(); i++) {
            Message message = recording.messages.get(i);

            // only FramebufferUpdates are useful
            if (!(message instanceof FramebufferUpdateMessage))
                continue;

            // cumulate areas of same timestamp
            if (i + 1 < recording.messages.size()
                    && message.getTimestamp() == recording.messages.get(i + 1).getTimestamp()) {
                area += ((FramebufferUpdateMessage) message).getCoveredArea();
                continue;
            }
            area += ((FramebufferUpdateMessage) message).getCoveredArea();

            // check size
            if (area > minSlideArea || message instanceof WhiteboardMessage) {

                if (!sequence) {
                    // beginning of sequence
                    sequence = true;
                    // set index
                    index.add(new IndexEntry(this, message.getTimestamp()));
                    previous_timestamp = message.getTimestamp();
                    if (TTT.verbose)
                        System.out.print("\nIndex " + (index.size() < 10 ? " " : "") + (index.size()) + ": "
                                + Constants.getStringFromTime(message.getTimestamp()));

                } else if (message.getTimestamp() - previous_timestamp < minSlideDiffMsecs) {
                    // within sequence
                    previous_timestamp = message.getTimestamp();
                    if (TTT.verbose)
                        System.out.print("\t" + Constants.getStringFromTime(message.getTimestamp(), true) + "("
                                + (++animationCount) + ")");

                } else {
                    // end of sequence AT PREVIOUS POTENTIAL INDEX
                    sequence = false;
                    if (animationCount > 0
                            && previous_timestamp - index.get(index.size() - 1).getTimestamp() < minAnimationDuration) {
                        // no animation - reset index
                        index.set(index.size() - 1, new IndexEntry(this, previous_timestamp));
                        if (TTT.verbose)
                            System.out.print(" RESET");
                    }
                    animationCount = 0;

                    // beginning of next sequence
                    sequence = true;
                    // set index
                    index.add(new IndexEntry(this, message.getTimestamp()));
                    previous_timestamp = message.getTimestamp();
                    if (TTT.verbose)
                        System.out.print("\nIndex " + (index.size() < 10 ? " " : "") + (index.size()) + ": "
                                + Constants.getStringFromTime(message.getTimestamp()));
                }
            }
            // reset cumulated area
            area = 0;
        }

        // // fix last index if needed
        sequence = false;
        if (animationCount > 0
                && previous_timestamp - index.get(index.size() - 1).getTimestamp() < minAnimationDuration) {
            // no animation - reset index
            index.set(index.size() - 1, new IndexEntry(this, previous_timestamp));
            if (TTT.verbose)
                System.out.print(" RESET");
        }
        animationCount = 0;

        // remove last index if useless
        if ((index.size() > 0)
                && (index.get(index.size() - 1).getTimestamp() >= recording.messages.get(recording.messages.size() - 1)
                        .getTimestamp())) {
            index.remove(index.size() - 1);
            if (TTT.verbose)
                System.out.print(" - Removing last index, because it uses timestamp of last message.");
        }

        // add index at beginning if needed
        if (index.size() == 0 || index.get(0).getTimestamp() > 2000) {
            index.add(0, new IndexEntry(this, 0));
            if (TTT.verbose)
                System.out.println("\nIndex added index at beginning.");
        }

        if (TTT.verbose)
            System.out.println("\n\nGenerated index with " + index.size() + " entries.\n");

    }

    // create index
    void computeIndex_regarding_length_of_sequence() {
        // build slide index
        // containing all message which area covers minSlideArea and a time interval of at least minSlideDiffMsecs

        if (TTT.verbose)
            System.out.println("\ncompute index table:\n");

        // delete index
        index.clear();

        // TODO: set as option
        // possible slide should at least cover 20% of maximum size
        int minSlideArea = recording.prefs.framebufferWidth * recording.prefs.framebufferHeight / 5;

        // TODO: set as option
        // there should be at least 5 or 10 sec between two slides
        int minSlideDiffMsecs = 10000;
        int minSequenceLength = 5;

        // count sequence with gaps less than minSlideDiffMsecs
        int animationCount = 0;

        int timestamp = Integer.MIN_VALUE + 1;
        int previous_timestamp = -1;
        int area = 0;

        // build index based on covered area
        for (int i = 0; i < recording.messages.size(); i++) {
            Message message = recording.messages.get(i);

            // sum up area(s)
            if (message instanceof FramebufferUpdateMessage)
                area += ((FramebufferUpdateMessage) message).getCoveredArea();
            else if (area == 0)
                // only FramebufferUpdates are useful - skip others
                // Note: do not skip if same timestamp as previous framebufferupdate
                continue;

            // cumulate areas of same timestamp
            if (i + 1 < recording.messages.size()
                    && message.getTimestamp() == recording.messages.get(i + 1).getTimestamp())
                continue;

            // check size
            if (area > minSlideArea) {
                // no animation or first index
                if ((message.getTimestamp() - timestamp > minSlideDiffMsecs) || index.size() == 0) {
                    if (animationCount > 0 && animationCount < minSequenceLength && previous_timestamp >= 0) {
                        // no animation, take last message of sequence
                        // (animations take first message of sequence as index)
                        if (index.size() > 0)
                            index.set(index.size() - 1, new IndexEntry(this, previous_timestamp));
                        else
                            // first index
                            index.add(new IndexEntry(this, previous_timestamp));

                        if (TTT.verbose)
                            System.out.print(" RESET");
                    }

                    animationCount = 0;

                    if (TTT.verbose)
                        System.out.print("\nIndex " + (index.size() < 9 ? " " : "") + (index.size() + 1) + ": "
                                + Constants.getStringFromTime(message.getTimestamp()));
                    index.add(new IndexEntry(this, message.getTimestamp()));
                } else {
                    // distinguish animations from multiple slide changes
                    animationCount++;
                    previous_timestamp = message.getTimestamp();

                    if (TTT.verbose)
                        System.out.print("\t" + Constants.getStringFromTime(message.getTimestamp(), true) + "("
                                + animationCount + ")");
                }
                timestamp = message.getTimestamp();
            }

            // reset cumulated area
            area = 0;
        }

        // fix last index if needed
        if (animationCount > 0 && animationCount < minSequenceLength && previous_timestamp >= 0 && index.size() > 0) {
            // no animation, take last message of sequence
            // (animations take first message of sequence as index)
            index.set(index.size() - 1, new IndexEntry(this, previous_timestamp));
            if (TTT.verbose)
                System.out.print(" RESET");
        }

        if ((index.size() > 0)
                && (index.get(index.size() - 1).getTimestamp() >= recording.messages.get(recording.messages.size() - 1)
                        .getTimestamp())) {
            index.remove(index.size() - 1);
            if (TTT.verbose)
                System.out.print(" - Removing last index, because it uses timestamp of last message.");
        }

        // add index at beginning if needed
        if (index.size() == 0 || index.get(0).getTimestamp() > 2000) {
            index.add(0, new IndexEntry(this, 0));
            if (TTT.verbose)
                System.out.println("\nIndex added index at beginning.");
        }

        if (TTT.verbose)
            System.out.println("\n\nGenerated index with " + index.size() + " entries.\n");
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // file I/O
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TODO: move to own class IndexExtension

    // read from extension
    public void readIndexExtension(DataInputStream in) throws IOException {
        // remove current index
        index.clear();

        // NOTE: assumes header tag already read

        // header
        int number_of_table_entries = in.readShort();

        if (TTT.verbose)
            System.out.println("Index with " + number_of_table_entries + " entries.");
        // index table
        for (int i = 0; i < number_of_table_entries; i++) {
            // timestamp
            int timestamp = in.readInt();
            // title
            int titelLength = in.readByte();
            byte[] titleArray = new byte[titelLength];
            in.readFully(titleArray);
            String title = new String(titleArray);
            // searchable text
            int searchableLength = in.readInt();
            if (searchableLength > 0)
                searchbaseFormat = searchbaseFormatStored = ASCII_SEARCHBASE;
            byte[] searchableArray = new byte[searchableLength];
            in.readFully(searchableArray);
            String searchable = new String(searchableArray);
            // thumbnail
            BufferedImage image = readThumbnail(in);

            // add index entry
            index.add(new IndexEntry(this, title, timestamp, searchable, image));

            if (TTT.verbose)
                System.out.println("Index " + (i < 9 ? " " : "") + (i + 1) + ": "
                        + Constants.getStringFromTime(timestamp, true)
                        + (image == null ? " - no thumb" : " -    thumb")
                        + (searchable.length() > 0 ? " -    search" : " - no search") + " - Title: " + title);
        }

        // check if valid
        valid = index.size() > 0;
        if (!valid)
            index.add(new IndexEntry(this));
    }

    // write index entries as extension
    public void writeIndexExtension(DataOutputStream out) throws IOException {
        if (index != null) {
            // buffer output to determine its length
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream buffer = new DataOutputStream(byteArrayOutputStream);

            // header
            buffer.writeByte(Constants.EXTENSION_INDEX_TABLE);
            buffer.writeShort(index.size());
            // index table
            for (int i = 0; i < index.size(); i++) {
                IndexEntry indexEntry = index.get(i);
                // timestamp
                buffer.writeInt(indexEntry.getTimestamp());
                // title
                buffer.writeByte(indexEntry.getTitle().length());
                buffer.writeBytes(indexEntry.getTitle());

                // write searchbase text (if exist)
                String searchableText = indexEntry.getSearchbase();
                if (searchableText == null || searchableText.length() == 0)
                    buffer.writeInt(0);
                else {
                    searchbaseFormatStored = Index.XML_SEARCHBASE;
                    // NOTE: String.length() not always equals String.getBytes().length
                    // depending on the system's character encoding (Umlauts may fail)
                    buffer.writeInt(searchableText.getBytes().length);
                    buffer.write(searchableText.getBytes());
                }

                // thumbnail
                writeThumbnail(indexEntry, buffer);
            }
            buffer.flush();

            // write length of extension
            int tmp = byteArrayOutputStream.size();
            out.writeInt(tmp);
            // write extension
            out.write(byteArrayOutputStream.toByteArray());
        } // else no index available
    }

    // read one image representing thumbnail
    private BufferedImage readThumbnail(DataInputStream in) throws IOException {
        int image_size = in.readInt();
        if (image_size == 0) {
            // thumbnail not available
            return null;
        } else {
            // thumbnail available
            byte[] image_array = new byte[image_size];
            in.readFully(image_array);
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image_array));
            thumbnail_scale_factor = recording.prefs.framebufferHeight / bufferedImage.getHeight();
            return bufferedImage;
        }
    }

    private void writeThumbnail(IndexEntry indexEntry, DataOutputStream out) throws IOException {
        // TODO: write annotated thumbnail for HTML script

        // write thumbnail image
        ImageIcon thumbnail = indexEntry.getThumbnail();
        if (thumbnail == null)
            // no thumbnail available
            out.writeInt(0);
        else {
            // buffer to determine size of image
            BufferedImage bufferedImage = new BufferedImage(thumbnail.getIconWidth(), thumbnail.getIconHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = bufferedImage.createGraphics();
            g.drawImage(thumbnail.getImage(), 0, 0, null);
            ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", imageOut);
            imageOut.flush();
            // write size + image
            out.writeInt(imageOut.size());
            out.write(imageOut.toByteArray());
        }
    }

    // ///////////////////////////////////////////
    // thumbnail defaults
    // ///////////////////////////////////////////

    private int thumbnail_scale_factor = Constants.default_thumbnail_scale_factor;

    public int getThumbnailScaleFactor() {
        return thumbnail_scale_factor;
    }

    private Image defaultThumbnail;

    public Image getDefaultThumbnail() {
        if (defaultThumbnail == null)
            createDefaultThumbnail();       
        return defaultThumbnail;
    }

    private void createDefaultThumbnail() {
        // define default thumbnail icon
      	
            // get image
            Image image = Constants.getIcon("77.kitty.jpg").getImage();

            // wait until image is fully loaded and image dimensions can be determined
            while (!Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, null))
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}

            // scale image to thumbnail size
            image = ScriptCreator.getScaledInstance(image, 1024 / thumbnail_scale_factor, 768 / thumbnail_scale_factor);

            // was:
            // image = image.getScaledInstance(1024 / thumbnail_scale_factor, 768 / thumbnail_scale_factor,
            // Image.SCALE_DEFAULT);

            defaultThumbnail = image;
        
    }

    // //////////////////////////////////////////////
    // Annotations
    // //////////////////////////////////////////////

    // displaying annotations on thumbnails
    static final public int PAINT_ALL_ANNOTATIONS = 0;
    static final public int PAINT_NO_ANNOTATIONS = 1;
    static final public int PAINT_NO_HIGHLIGHT_ANNOTATIONS = 2;
    int annotationsPaintMode = PAINT_NO_HIGHLIGHT_ANNOTATIONS;

    public int getAnnotationsPaintMode() {
        return annotationsPaintMode;
    }

    public void setAnnotationsPaintMode(int annotationsPaintMode) {
        this.annotationsPaintMode = annotationsPaintMode;
    }

    // gather annotations for each index entry
    // useful for annotated thumbnails and script
    public void extractAnnotations() {
        // Buffer for annotations
        ArrayList<Annotation> annotations = new ArrayList<Annotation>();

        // message counter
        int message_nr = 0;

        // get annotations for each index
        for (int i = 0; i < recording.index.size(); i++) {
            // get end time of index
            int end;
            if (i + 1 == recording.index.size())
                // last index ends with last message
                end = recording.messages.get(recording.messages.size() - 1).getTimestamp();
            else
                // all other end with the beginning of the following index
                end = recording.index.get(i + 1).getTimestamp();

            // read message
            Message message = recording.messages.get(message_nr);
            int start = message.getTimestamp();

            // delete flag
            boolean marked_to_be_deleted = false;

            // gather annotations until end of index
            while (message.getTimestamp() < end) {
                // stop if switching to another whiteboard (or to desktop) after the start
                // NOTE: otherwise anotations will not belong to thumbnail screenshot
                if (message instanceof WhiteboardMessage && message.getTimestamp() > start)
                    break;

                // delete events
                if (message instanceof DeleteAllAnnotation || message instanceof WhiteboardMessage) {
                    // Note: typically a new index is preceded by a DeleteAllMessage
                    // therefore only register delete events here
                    // and only clear buffer if it was not the last one for this index
                    marked_to_be_deleted = true;
                }
                // other annotations
                else if (message instanceof Annotation) {
                    // delete buffered annotations of needed
                    // Note: previous annotations are only deleted if the delete event is suceeded by other annotations
                    if (marked_to_be_deleted) {
                        annotations.clear();
                        marked_to_be_deleted = false;
                    }
                    // add annotations to buffer
                    annotations.add((Annotation) message);
                }
                // read next message
                message_nr++;
                if (message_nr == recording.messages.size())
                    break;
                message = recording.messages.get(message_nr);
            }

            // set collected annotations
            recording.index.get(i).setAnnotations(annotations);

            // clear buffer if index ended with a delete event, which was not performed yet
            if (marked_to_be_deleted) {
                // delete buffered annotations
                annotations.clear();
                marked_to_be_deleted = false;
            }
        }
    }

    public void extractAnnotations_old() {
        int i = index.size() - 1;
        ArrayList<Annotation> annotations = new ArrayList<Annotation>();

        for (int m = recording.messages.size() - 1; m >= 0; m--) {
            Message message = recording.messages.get(m);

            // computation for this index has finished - set annotations
            if (message.getTimestamp() < index.get(i).getTimestamp() || m == 0) {
                index.get(i).setAnnotations(annotations);
                annotations.clear();

                // all done if first index reached
                if (--i < 0)
                    break;
            }

            // only gather annotations up to first deleteAll
            // annotations after first deleteAll are drown away
            // NOTE: new ttt recorder does not write (explicit) initial deleteAll for Whiteboard (implicit clear)
            // TODO: introduce sub-indices
            if (message instanceof DeleteAllAnnotation || message instanceof WhiteboardMessage) {
                // ignore initial DeleteAll-Event of whitepage
                if (message.getTimestamp() != index.get(i).getTimestamp()) {
                    annotations.clear();
                }
            }

            // add annotations to beginning (because of reverse searching)
            else if (message instanceof Annotation) {
                annotations.add(0, (Annotation) message);
            }
        }
    }

    // //////////////////////////////////////////////////////////////////
    // thumbnails
    // //////////////////////////////////////////////////////////////////
    
    ScriptCreator scriptCreator;
    public boolean computeScreenshots(int mode, boolean batch, boolean ShowProgressMonitor, String ocrPath){
    	
    	try {
    		scriptCreator = new ScriptCreator(recording, mode, ocrPath);
			return createScreenshots(mode, batch, ShowProgressMonitor);
		} catch (IOException e) {
			return false;
		}
    }
    
    public boolean computeScreenshots(int mode, boolean batch, boolean ShowProgressMonitor) {
    	try {
    	scriptCreator = new ScriptCreator(recording, mode);
    	
			return createScreenshots(mode, batch, ShowProgressMonitor);
		} catch (IOException e) {
				return false;
		}
    }
    
    /**
     * Actually computes the Screenshots
     * @param mode
     * @param batch
     * @param ShowProgressMonitor
     * @return
     * @throws IOException
     */
    private boolean createScreenshots(int mode, boolean batch, boolean ShowProgressMonitor) throws IOException {
    
        if (mode == 0)
            return true;
        if(TTT.verbose){
        // print information
        System.out.print("Computing ");
        if ((mode & ScriptCreator.THUMBNAILS) != 0)
            System.out.print("thumbnails . ");
        if ((mode & ScriptCreator.PDF_SCRIPT) != 0)
            System.out.print("PDF script . ");
        if ((mode & ScriptCreator.HTML_SCRIPT) != 0)
            System.out.print("HTML script . ");
        if ((mode & ScriptCreator.OCR_OPTIMIZED) != 0)
            System.out.print("OCR input . ");
        System.out.println();
        }
        // print file names
        String name = recording.getDirectory() + recording.getFileBase();
        if(TTT.verbose){
        if ((mode & ScriptCreator.PDF_SCRIPT) != 0)
            System.out.println(name + ".pdf");
        if ((mode & ScriptCreator.HTML_SCRIPT) != 0)
            System.out.println(name + ".html");
        if ((mode & ScriptCreator.OCR_OPTIMIZED) != 0)
            System.out.println(name + ".ocr");
        }
        ProgressMonitor progressMonitor = null;
        if (!batch && ShowProgressMonitor) {
            // show progress
        	String text = mode == ScriptCreator.THUMBNAILS ? "Computing thumbnails" : "Computing screenshots";
            progressMonitor = new ProgressMonitor(TTT.getRootComponent(), text, null, 0, index.size());
            progressMonitor.setMillisToDecideToPopup(100);
            progressMonitor.setMillisToPopup(100);
        }

        // TODO: should be done with other GraphicsContext to avoid stopping playback
        recording.pause();
        int previousTime = recording.getTime();

        if(scriptCreator != null)
         scriptCreator = new ScriptCreator(recording, mode);
     
        PDFHelper pdfHelper = null;
        if ((mode & ScriptCreator.PDF_SCRIPT) != 0) {
        
            // generate PDF helper
            // check whether the iText-library is installed or not
            if (LibraryChecker.isITextPdfLibraryInstalled()) {
                try {
                    pdfHelper = new PDFHelper(recording);
                } catch (Exception e) {
                	if(TTT.verbose){
                    System.out.println("Cannot create PDF:");
                	}
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Cannot generate pdf script: " + e, "Error...",
                            JOptionPane.ERROR_MESSAGE);
                    pdfHelper = null;
                    mode = mode - ScriptCreator.PDF_SCRIPT;
                }
            } else {
                // iText PDF Library is not installed - disable pdf generation
                pdfHelper = null;
                mode = mode - ScriptCreator.PDF_SCRIPT;
            }
        }

        // measure time
        long t = System.currentTimeMillis();
        boolean isCanceled = false;
    
        // compute
        if (mode != 0)
            for (int i = 0; i < index.size(); i++) {
                if (progressMonitor != null) {
                    // Check if the progress monitor has been cancelled
                    if (progressMonitor.isCanceled()) {
                        // Stop task
                        if ((mode & ScriptCreator.PDF_SCRIPT) != 0) {
                        	new File(name+".pdf").delete();
                        	pdfHelper = null;
                        }
                        if ((mode & ScriptCreator.HTML_SCRIPT) != 0)
                        	Constants.deleteDirectory(new File(name+".html"));
                        if ((mode & ScriptCreator.OCR_OPTIMIZED) != 0)
                        	Constants.deleteDirectory(new File(name+".ocr"));
                        if(TTT.verbose){
                        System.out.println("Cancelled by user");
                        }
                        isCanceled = true;
                        break;
                    } else if (TTT.verbose) {
                        // show progress
                        progressMonitor.setProgress(i);
                    }
                }
                if(TTT.verbose){
                	System.out.print(".");
                }
                // set time of index
                IndexEntry indexEntry = index.get(i);
                int timestamp = indexEntry.getTimestamp();
            	if ((mode & ScriptCreator.OCR_OPTIMIZED) == 0)
                	if (i+1<index.size()){
                		timestamp += (index.get(i+1).getTimestamp()-timestamp)*.95;
                	}
               
                recording.setTime(timestamp,false);

                // create screenshot
                Image screenshot = recording.graphicsContext.getScreenshotWithoutAnnotations();

                // set thumbnail
                if ((mode & ScriptCreator.THUMBNAILS) != 0) {                    


                    // faster image scaling
                	//Image thumbnail = recording.graphicsContext.getThumbnailWithoutAnnotations(thumbnail_scale_factor);
                    Image                 	thumbnail = ScriptCreator.getScaledInstance(screenshot, recording.prefs.framebufferWidth
                            / thumbnail_scale_factor, recording.prefs.framebufferHeight / thumbnail_scale_factor);
                    indexEntry.setThumbnail(thumbnail);
                }

                // write input for Optical Character Recognition
                if ((mode & ScriptCreator.OCR_OPTIMIZED) != 0){
                  String filename = scriptCreator.writeOCRScreenshot(i, screenshot);
                  readSearchBaseFromHOCRFile(filename+".hocr.hocr", i);
                  // cleanup mess
                  {
                	  File f = new File(filename);
                	  f.delete();
                	  f = new File(filename+".hocr.hocr");
                	  f.delete();
                  }
                }

                if (((mode & ScriptCreator.HTML_SCRIPT) != 0) || ((mode & ScriptCreator.PDF_SCRIPT) != 0)) {
                    // add (future) annotations of index
                    int annotationMode = getAnnotationsPaintMode();
                    setAnnotationsPaintMode(PAINT_NO_HIGHLIGHT_ANNOTATIONS);
                    index.get(i).paintAnnotations((Graphics2D) screenshot.getGraphics(), false);

                    // reset mode
                    setAnnotationsPaintMode(annotationMode);

                    // write html, screenshot and thumbnail
                    if ((mode & ScriptCreator.HTML_SCRIPT) != 0)
                        scriptCreator.writeIndex(i, screenshot);

                    // write screenshot to pdf
                    if (((mode & ScriptCreator.PDF_SCRIPT) != 0) && pdfHelper != null) {
                        try {
                            pdfHelper.writeNextIndex(i, screenshot);
                        } catch (Exception e) {
                        	if(TTT.verbose) {
                            System.out.println("Cannot create PDF:");
                        }
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Cannot generate pdf script: " + e, "Error...",
                                    JOptionPane.ERROR_MESSAGE);
                            pdfHelper = null;
                            mode = mode - ScriptCreator.PDF_SCRIPT;
                        }
                    }
                }
            }
        // close pdf document
        if (((mode & ScriptCreator.PDF_SCRIPT) != 0) && pdfHelper != null) {
            try {
                pdfHelper.close();
            } catch (Exception e) {
            	if(TTT.verbose){
                System.out.println("Cannot create PDF:");
            	}
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Cannot generate pdf script: " + e, "Error...",
                        JOptionPane.ERROR_MESSAGE);
                pdfHelper = null;
                mode = mode - ScriptCreator.PDF_SCRIPT;
            }
        }

        t = System.currentTimeMillis() - t;
        if(TTT.verbose){
        System.out.println(" done in " + Constants.getStringFromTime((int) t));
        }
        // remove process monitor
        if (progressMonitor != null)
            progressMonitor.close();

        // reset playback
        // TODO: reset mode pause/play
        recording.setTime(previousTime);
        return isCanceled == false;
    }

    // //////////////////////////////////////////////////////////////////
    // keyframes
    // //////////////////////////////////////////////////////////////////

    static final int PAINT_TO_OFFSCREEN_IMAGE = 0;
    static final int RAW_IN_ARRAY = 2;
    static final int RAW_IN_OFFSCREEN_IMAGE = 3;
    static final int COLLECT_MESSAGES = 4;

    public void computeKeyframes(int mode) {
        // disable painting, because it's slow
        recording.graphicsContext.enableRefresh(false);
        recording.graphicsContext.paint_to_offscreen_image = mode == PAINT_TO_OFFSCREEN_IMAGE;

        long t = System.currentTimeMillis();

        if (mode == COLLECT_MESSAGES)
            collectMessagesForKeyframes2();

        else {

            int nextIndex = 0;
            for (int i = 0; i < recording.messages.size(); i++) {
                Message message = recording.messages.get(i);
                // if(i%1000==0) System.out.print(".");

                if (message.getTimestamp() > index.get(nextIndex).getTimestamp()) {
                    switch (mode) {
                    case RAW_IN_OFFSCREEN_IMAGE:
                        recording.graphicsContext.handleUpdatedPixels(0, 0, recording.prefs.framebufferWidth,
                                recording.prefs.framebufferHeight);
                    case PAINT_TO_OFFSCREEN_IMAGE:
                        index.get(nextIndex).setKeyframe(recording.graphicsContext.getScreenshotOld());
                        break;
                    case RAW_IN_ARRAY:
                        int[] pixels = new int[recording.graphicsContext.pixels.length];
                        System.arraycopy(recording.graphicsContext.pixels, 0, pixels, 0, pixels.length);
                        // index.get(nextIndex).setKeyframe(pixels);
                        break;
                    default:
                        break;
                    }
                    // System.out.println("write " + nextIndex);
                    // ImageCreator.writeImage(index.get(nextIndex-1).getKeyframe(),"/netzlaufwerk/frame"+nextIndex);
                    nextIndex++;

                    if (nextIndex == index.size())
                        break;
                }

                // recording.deliverMessage(message);
                message.paint(recording.graphicsContext);
            }
        }
        t = System.currentTimeMillis() - t;
        if(TTT.verbose){
        System.out.println(Constants.getStringFromTime((int) t));
        }
        recording.graphicsContext.paint_to_offscreen_image = true;
        recording.graphicsContext.enableRefresh(true);
    }

    private void collectMessagesForKeyframes2() {
        boolean[][] set = new boolean[recording.prefs.framebufferWidth][recording.prefs.framebufferHeight];
        int setCount = 0;
        int full = recording.prefs.framebufferWidth * recording.prefs.framebufferHeight;

        ArrayList<Message> collected = new ArrayList<Message>();

        // TODO: what if index is empty??
        int previousIndexNumber = index.size() - 1;
        int previousIndexTime = index.get(previousIndexNumber--).getTimestamp();

        int count = 0;

        for (int i = recording.messages.size() - 1; i >= 0; i--) {
            Message message = recording.messages.get(i);
            count++;
            if (message instanceof FramebufferUpdateMessage) {
                if (previousIndexTime > message.getTimestamp()) {
                    // System.out.println("Index " + (previousIndexNumber + 2) + ":\t" + collected.size()
                    // + "(of "+count+") messages - covered " + (setCount==full));

                    if (previousIndexNumber == 0)
                        break;
                    previousIndexTime = index.get(previousIndexNumber--).getTimestamp();
                    collected.clear();
                    count = 0;

                    set = new boolean[recording.prefs.framebufferWidth][recording.prefs.framebufferHeight];
                    setCount = 0;
                }
                if (setCount != full) {
                    // message needed for keyframe, because it covers unset pixels
                    collected.add(message);

                    Rectangle rectangle = ((FramebufferUpdateMessage) message).getBounds();
                    if (message instanceof WhiteboardMessage)
                        rectangle = new Rectangle(0, 0, recording.prefs.framebufferWidth,
                                recording.prefs.framebufferHeight);

                    for (int y = 0; y < rectangle.height; y++)
                        for (int x = 0; x < rectangle.width; x++)
                            if (!set[rectangle.x + x][rectangle.y + y]) {
                                setCount++;
                                set[rectangle.x + x][rectangle.y + y] = true;
                            }
                }
            }
        }
    }

    // //////////////////////////////////////////////////////////////////
    // controlling
    // //////////////////////////////////////////////////////////////////

    private int nowPlayingIndex_startingAtZero;

    synchronized public IndexEntry getCurrentIndex() {
        return index.get(nowPlayingIndex_startingAtZero);
    }

    synchronized public IndexEntry getCorrespondingIndex(int time) {
        // find corresponding index
        int i = 1;
        while (i < index.size() && time >= index.get(i).getTimestamp())
            i++;

        return index.get(--i);
    }

    synchronized public void setCorrespondingIndex(int time) {
        // find corresponding index
        int i;
        for (i = index.size() - 1; i > 0; i--)
            if (index.get(i).getTimestamp() <= time)
                break;

        // set index
        nowPlayingIndex_startingAtZero = i;
        recording.fireIndexChangedEvent(nowPlayingIndex_startingAtZero + 1);
    }

    public IndexEntry get(int i) {
        try {
            return index.get(i);
        } catch (Exception e) {
            return null;
        }
    }

    public int size() {
        return index.size();
    }

    synchronized public IndexEntry getNextIndex() {
        if (nowPlayingIndex_startingAtZero + 1 < index.size())
            return index.get(nowPlayingIndex_startingAtZero + 1);
        else
            // TODO: think about this - maybe return null
            return index.get(0);
    }

    synchronized public IndexEntry getPreviousIndex() {
        if (nowPlayingIndex_startingAtZero > 0)
            return index.get(nowPlayingIndex_startingAtZero - 1);
        else
            return index.get(0);
    }

    public void updateRunningIndex(int timestamp) {
        // set index marker if needed
        for (int i = 0; i < index.size(); i++)
            if (index.get(i).getTimestamp() == timestamp) {
                nowPlayingIndex_startingAtZero = i;

                // fire event (index event starting at one)
                recording.fireIndexChangedEvent(nowPlayingIndex_startingAtZero + 1);

                // update search result highligting
                recording.graphicsContext.refresh();
                break;
            }
    }

    // //////////////////////////////////////////////////////////////////
    // search
    // //////////////////////////////////////////////////////////////////

    final public static int NO_SEARCHBASE = 0;
    final public static int ASCII_SEARCHBASE = 1;
    final public static int XML_SEARCHBASE = 2;

    // TOD:; visibility
    public int searchbaseFormatStored = NO_SEARCHBASE;
    public int searchbaseFormat = NO_SEARCHBASE;

    public int getSearchbaseFormat() {
        return searchbaseFormat;
    }

    public int getSearchbaseFormatStored() {
        return searchbaseFormatStored;
    }

    // search for given keyword
    public void search(String searchword) {
        // clear old results
        search_index.clear();

        // perform search
        for (int i = 0; i < index.size(); i++)
            // add to search results
            if (index.get(i).contains(searchword))
                search_index.add(index.get(i));

        // force repaint to highlight results
        recording.graphicsContext.refresh();
    }

    public void highlightSearchResultsOfCurrentIndex(Graphics2D g) {
        getCurrentIndex().highlightSearchResults(g);
    }

    /*******************************************************************************************************************
     * reading search base
     ******************************************************************************************************************/
    /***
     * read hOCR/xhtml generated by tesseract-OCR
     * @param fileName
     * @return
     */
    public boolean readSearchBaseFromHOCRFile(String fileName,int pageindex) {
        File file = new File(fileName);
        if (TTT.verbose) System.out.print("Reading search base: "+fileName);
        if (file.exists()) {
            // Use an instance of ourselves as the SAX event handler

        	hOCRHandler handler = new hOCRHandler();
            try {
            	XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            	xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            	xmlReader.setContentHandler(handler);
            	xmlReader.parse(new InputSource(new FileReader(file)));
                ArrayList<ArrayList<SearchBaseEntry>> pages = handler.getResult();

                // check size
                if (pages.size() != 1 && pageindex >= this.index.size()) {
                    // TODO: modify dialog
                    TTT.showMessage("Missmatch in OCR/index","expected pages: "+index.size()+" requested page: "+pageindex+" paket size: "+pages.size(), JOptionPane.WARNING_MESSAGE);

                } else {
                    // set searchbase
                    index.get(pageindex).setSearchbase(pages.get(0));
                    searchbaseFormat = XML_SEARCHBASE;
                    return true;
                }

            } catch (Throwable t) {
                t.printStackTrace();
                TTT.showMessage("XML file '" + file + "' contains errors.\nError: " + t,
                        "TTT: Reading hOCR Searchbase File", JOptionPane.ERROR_MESSAGE);
            }
        } else if (TTT.verbose)
            System.out.println("not found.");
        return false;
    }
    
    public // read XML generated by OCR
    // double ratioXMLSearchBase;
    // Dimension resolutionXMLSearchBase;
    boolean readSearchBaseFromFileXML(String fileName) {
        if (TTT.verbose)
            System.out.print("Reading search base: ");

        File file = new File(fileName);
        if (TTT.verbose)
            System.out.print(fileName + " ");

        if (file.exists()) {
            // Use an instance of ourselves as the SAX event handler
            XMLHandler handler = new XMLHandler();
            // set desktop resolution
            // NOTE: desktop resolution is not stored within OmniPage 14 XML file but within OmniPage 15 XML files
            handler.setDesktopResolution(recording.prefs.framebufferWidth, recording.prefs.framebufferHeight);
            // Use the default (non-validating) parser
            SAXParserFactory factory = SAXParserFactory.newInstance();
            try {
                // Parse the input
                SAXParser saxParser = factory.newSAXParser();
                saxParser.parse(file, handler);

                // TODO: maybe ratio should be index-dependent and not word-dependent
                // resolutionXMLSearchBase = handler.getResolution();
                // ratioXMLSearchBase = recording.prefs.framebufferWidth / resolutionXMLSearchBase.getWidth();
                // System.out.println("RATIO: "+ratio);

                ArrayList<ArrayList<SearchBaseEntry>> pages = handler.getResult();

                // check size
                if (pages.size() != this.index.size()) {
                    // TODO: modify dialog
                    TTT.showMessage("Searchbase " + (pages.size() < this.index.size() ? "incomplete" : "not suitable")
                            + "\nFound " + pages.size() + " entries (expected " + +this.index.size() + ")",
                            "Searchbase incomplete", JOptionPane.WARNING_MESSAGE);

                } else {
                    if (TTT.verbose)
                        System.out.println(" ok");

                    // set searchbase
                    for (int i = 0; i < pages.size(); i++) {
                        index.get(i).setSearchbase(pages.get(i));
                    }
                    searchbaseFormat = XML_SEARCHBASE;
                    return true;
                }

            } catch (Throwable t) {
                t.printStackTrace();
                TTT.showMessage("XML file '" + file + "' contains errors.\nError: " + t,
                        "TTT: Reading XML Searchbase File", JOptionPane.ERROR_MESSAGE);
            }
        } else if (TTT.verbose)
            System.out.println("not found.");
        return false;
    }

    public double getRatio() {
        double ratio = 0;
        // TODO: ugly hack
        // NOTE: ratio of 1 might be returned for pages without searchable text
        for (int i = 0; ((ratio == 0) || (ratio == 1)) && (i < size()); i++)
            ratio = get(i).getRatio();
        return ratio;
    }

    public static void main(String[] args) {
        File file = new File(args[0]);

        if (file.exists())
            try {
                // read file
                BufferedReader in = new BufferedReader(new FileReader(file));
                char[] characters = new char[(int) file.length()];
                in.read(characters);

                boolean last_was_blank = false;
                if(TTT.verbose){
                for (int i = 0; i < characters.length; i++) {
                    // page devider
                    if (characters[i] == 12) {
                        System.out.println("\n------------------------------");
                    } else if (characters[i] == '\n') {
                        if (!last_was_blank)
                            System.out.print(" ");
                        last_was_blank = true;
                    } else if (characters[i] == '\r') {
                        if (!last_was_blank)
                            System.out.print(" ");
                        last_was_blank = true;
                    } else if (characters[i] == ' ') {
                        if (!last_was_blank)
                            System.out.print(" ");
                        last_was_blank = true;
                    } else if (characters[i] == '\t') {
                        if (!last_was_blank)
                            System.out.print(" ");
                        last_was_blank = true;
                    } else {
                        last_was_blank = false;
                        System.out.print(characters[i]);
                    }
                }
                }
            } catch (IOException e) {
                System.out.println("failed (" + e + ")");
            }
        else
            System.out.println("not found.");
    }

    // read ASCII text generated by OCR
    public boolean readSearchBaseFromFileTXT(String fileName) {
        if (TTT.verbose)
            System.out.print("Reading search base: ");

        File file = new File(fileName);
        if (TTT.verbose)
            System.out.print(fileName + " ");

        if (file.exists())
            try {
                // read file
                BufferedReader in = new BufferedReader(new FileReader(file));
                char[] characters = new char[(int) file.length()];
                in.read(characters);

                ArrayList<String> pages = new ArrayList<String>();

                int begin = 0;
                for (int i = 0; i < characters.length; i++) {
                    // page devider
                    if (characters[i] == 12) {
                        if (TTT.verbose)
                            System.out.print(".");
                        pages.add(new String(characters, begin, i + 1 - begin));
                        begin = i + 1;
                    }
                }
                if (pages.size() != index.size()) {
                    // number of pages does not match
                    // TODO: modify dialog
                    TTT.showMessage("Searchbase " + (pages.size() < index.size() ? "incomplete" : "not suitable")
                            + "\nFound " + pages.size() + " entries (expected " + +index.size() + ")",
                            "Searchbase incomplete", JOptionPane.WARNING_MESSAGE);
                    return false;
                }

                boolean updating = false;

                for (int i = 0; i < pages.size(); i++) {
                    if (index.get(i).getSearchbase() == null)
                        index.get(i).setSearchbase(pages.get(i));
                    else if (!index.get(i).getSearchbase().equals(pages.get(i))) {
                        // TODO: maybe equals can fail - test this (recording read - write - read again)
                        if (!updating) {
                            if (TTT.verbose)
                                System.out.print("updating ");
                            updating = true;
                        }
                        if (TTT.verbose)
                            System.out.print("[" + (i + 1) + "] ");
                        index.get(i).setSearchbase(pages.get(i));
                    }
                }

                if (TTT.verbose)
                    System.out.println(" ok");

                searchbaseFormat = ASCII_SEARCHBASE;
                return true;

            } catch (IOException e) {
                System.out.println("failed (" + e + ")");
            }
        else if (TTT.verbose)
            System.out.println("not found.");
        return false;
    }
}
