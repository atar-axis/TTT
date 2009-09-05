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
 * Created on 24.03.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 * based on diploma thesis of Eric Willy Tiabou
 */
package ttt;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.media.format.AudioFormat;
import javax.swing.ImageIcon;
import javax.swing.ProgressMonitor;

import ttt.messages.Annotation;
import ttt.messages.CursorMessage;
import ttt.messages.HextileMessage;
import ttt.messages.Message;
import ttt.messages.WhiteboardMessage;

import com.anotherbigidea.flash.SWFConstants;
import com.anotherbigidea.flash.movie.Actions;
import com.anotherbigidea.flash.movie.Button;
import com.anotherbigidea.flash.movie.Frame;
import com.anotherbigidea.flash.movie.ImageUtil;
import com.anotherbigidea.flash.movie.Instance;
import com.anotherbigidea.flash.movie.Movie;
import com.anotherbigidea.flash.movie.MovieClip;
import com.anotherbigidea.flash.movie.Shape;
import com.anotherbigidea.flash.movie.Symbol;
import com.anotherbigidea.flash.movie.Text;
import com.anotherbigidea.flash.movie.Transform;
import com.anotherbigidea.flash.sound.MP3Helper;
import com.anotherbigidea.flash.sound.SoundStreamHead;
import com.anotherbigidea.flash.structs.AlphaColor;
import com.anotherbigidea.flash.structs.AlphaTransform;
import com.anotherbigidea.flash.structs.Color;

public class FlashContext extends FlashActionHelper {

    public static boolean flash_debug = false;
    private boolean batch;

	/**
	 * Checks whether it is possible to create a swf file.
	 * @param recording
	 * @return True if there is a mp3 file, or wav or mp2 file which can be encoded to mp3 file using lame.
	 * @throws IOException
	 */
	public static boolean isCreationPossible(Recording recording) throws IOException {
		return recording.getExistingFileBySuffix("mp3").exists() || (LameEncoder.isLameAvailable() && recording.getExistingFileBySuffix(new String[] {"wav","mp2"}).exists());
	}
	
    static public void createFlash(Recording recording, boolean batch) throws IOException {
        FlashContext flashContext = new FlashContext(recording, batch);

        // adjust framerate for recordings with many messages
        // NOTE: more messages cause more Flash objects, but number of Flash objects is limited
        // TODO: calculate good value instead of estimating it
        if (recording.messages.size() > 100000)
            frameRate = 10;
        else if (recording.messages.size() > 90000)
            frameRate = 15;
        else if (recording.messages.size() > 75000)
            frameRate = 20;
        else
            frameRate = 30;

        flashContext.createFlash();
    }

    protected FlashContext(Recording recording, boolean batch) {
        this.recording = recording;
        this.batch = batch;
    }

    final static int FLASH_VERSION = 6;
    static int frameRate = 30;
    public static int colorDepth = SWFConstants.BITMAP_FORMAT_32_BIT;
    public Frame frame = null; // frame in movieclip (slides)
    double intervalMsec; // intervall is 1sec/framerate

    public int annotationsDepth = 30000;
    public final static int cursorDepth = 60000;

    // use to save the corresponding timestamp in the first frame of each clip
    ArrayList<Integer> bufferTimestampClip = new ArrayList<Integer>();
    // use to write timestamp on thumbnails
    ArrayList<Integer> bufferTimestamp = new ArrayList<Integer>();
    ArrayList<ImageIcon> bufferThumbnails = new ArrayList<ImageIcon>();

    // used to combine hextiles to reduce number of symbols since more than 65536 will not work
    // hextiles within same frame will be combined if the additional area of the bounding rectangle is less this
    // threshold
    public static int COMBINING_HEXTILES_THRESHOLD = 10000;

    /*******************************************************************************************************************
     * initialization
     ******************************************************************************************************************/

    int index = 0;
    public int frameNumb = 0;
    int nc = 0;
    public Message message;
    public int symbolCount = 0;
    int prevTimestamp = 0;
    int previousTimestamp = 0;

    ProgressMonitor progressMonitor;

    protected void initialize() throws IOException {
        intervalMsec = (double) 1000 / frameRate;
        System.out.println("    flash frame rate: " + frameRate);
        if (!batch)
            progressMonitor.setProgress(6);

        // retrieve thumbnailWidth and thumbnailHeight to compute the Frame size
        System.out.println("    handle thumbnails");
        if (!recording.thumbnailsAvailable()) {
            if (!batch) {
                progressMonitor.setProgress(10);
                progressMonitor.setNote("computing thumbnails");
            }
            recording.createThumbnails();
        }
        if (!batch) {
            progressMonitor.setProgress(15);
            progressMonitor.setNote("converting thumbnails");
        }

        // determine width and height of thumbnails
        IndexEntry entry = recording.index.get(0);
        ImageIcon imIcon = entry.getThumbnail();
        Image im = imIcon.getImage();
        thumbnailWidth = im.getWidth(null);
        thumbnailHeight = im.getHeight(null);

        // buffer index information
        for (int i = 0; i < recording.index.size(); i++) {
            IndexEntry ie = recording.index.get(i);
            bufferTimestamp.add(ie.getTimestamp());
            bufferTimestampClip.add(ie.getTimestamp());
            bufferThumbnails.add(ie.getThumbnail());
            if (flash_debug) {
                System.out.println("Index " + (i + 1) + ":\t" + Constants.getStringFromTime(ie.getTimestamp()));
            }
        }
    }

    /*******************************************************************************************************************
     * audio stream handling
     ******************************************************************************************************************/
    // sound blocks
    ArrayList blocks;
    // sound head
    SoundStreamHead head;

    protected void initializeSound() throws IOException {
        // initialise sound
    	File file = null;
    	try {    	
	    	file = recording.getExistingFileBySuffix(new String[] {"mp3","mp2","wav"});
	    	if (file.getName().toLowerCase().endsWith(".mp3")) {
	    		//if there is already a mp3 file no additional audio encoding is necessary 
	    		try {
	    			getSoundStreamHead(file);
	    			return;
	    		} catch (Exception e) {
	    			/*if the mp3 file header can not be retrieved the audio file was probably encoded to mpeg layer II format by an older ttt version 
	    			 * still using JMF 2.1.1a. That's why rename the audio file to *.mp2 and encode it to mpeg layer III correctly via lame.
	    			 */
	    			file.renameTo(file = new File(file.getAbsolutePath() + ".mp2"));
	    		}
	    	}
    		//try to encode audio file to mp3 format
        	if (LameEncoder.convertAudioFile(file, recording.getFileBySuffix("mp3"), batch)) {
        		if (!batch) {
                    progressMonitor.setProgress(30);
        		}
        		System.out.println("    audio converted to mpeg layer 3");
        		file = recording.getExistingFileBySuffix("mp3");
        	} else {
        		//canceled by user
        		progressMonitor = null;
        		return;
        	}
	        getSoundStreamHead(file);
    	} catch (Exception e) {
    		if (file != null && file.getName().toLowerCase().endsWith("mp3.mp2")) {
    			//if the audio file was renamed from *.mp3 to *.mp3.mp2 and the audio encoding failed undo renaming
    			file.renameTo(new File(file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-4)));
    		}
            System.out.println("    audio failed - cannot create flash movie");
            throw new IOException("Cannot transcode audio to flash");
    	}
    }

    protected void getSoundStreamHead(File file) throws IOException {
    	System.out.println("    loading audio from " + file);
        FileInputStream mp3 = new FileInputStream(file);
        blocks = new ArrayList();
        System.out.print("    transform audio to flash ... ");
        head = MP3Helper.streamingBlocks(mp3, frameRate, blocks);
        System.out.println("done");
        mp3.close();
    }
    
    /*******************************************************************************************************************
     * main processing loop
     ******************************************************************************************************************/

    private void createFlash() throws IOException {
        try {
            if (!batch) {
                // show progress
                progressMonitor = new ProgressMonitor(TTT.getRootComponent(), "Convert to Flash", "Converting...", 0,
                        100);
                progressMonitor.setMillisToDecideToPopup(100);
                progressMonitor.setMillisToPopup(100);

                progressMonitor.setProgress(5);
                progressMonitor.setNote("initializing...");
            }

            initialize();

            if (!batch) {
            	if (progressMonitor.isCanceled()) {
            		return;
            	}
                progressMonitor.setProgress(20);
                progressMonitor.setNote("preparing audio");
            }

            initializeSound();

            if (!batch) {
            	if (progressMonitor == null || progressMonitor.isCanceled()) {
            		return;
            	}
                progressMonitor.setProgress(40);
                progressMonitor.setNote("converting messages");
            }

            placedSymbolsCount = 0;

            for (int i = 0; i < recording.messages.size(); i++) {
                if (!batch) {
                	if (progressMonitor.isCanceled()) {
                		return;
                	}
                	progressMonitor.setProgress(40 + (40 * i / recording.messages.size()));
                }
                    

                message = recording.messages.get(i);
                message.writeToFlash(this);

                lastFrameOfClipExtra(message.getTimestamp(), frame);

                // for later check if the previous timestamp is the same like the actual
                previousTimestamp = message.getTimestamp();
            }

            Frame beforeLastFrame = movieClip.getFrame(frameNumb - 1);
            // remove annotations of this clip in the frame before last
            clearAnnotations(false, beforeLastFrame);
            // remove cursor's instance in the frame before last
            if (CursorMessage.cursorInstance != null) {
                beforeLastFrame.remove(CursorMessage.cursorInstance);
            }
            // remove witheboard's instance in the frame before last
            if (WhiteboardMessage.whiteBoardInstance != null) {
                beforeLastFrame.remove(WhiteboardMessage.whiteBoardInstance);
            }

            CursorMessage.cursorInstance = null;

            // stop the movieclip by its last frame to avoid movie looping
            frame.stop();
            // stop the timer clip at the last Frame of the last Movieclip
            Actions frameAction = frame.actions(FLASH_VERSION);
            stopTimeClip(frameAction);
            frameAction.end();
            frame.setActions(frameAction);
            frameAction.done();

            bufMovieClip.add(movieClip);

            Movie movie = mainMovieclip();

            // NOTE: playback fails (not visible) if more than 65536 symbols are placed
            // however each hextile generate two symbols (counted as one placed symbol)
            // increase COMBINING_HEXTILES_THRESHOLD to reduce symbols
            System.out.println("    number of ttt messages: " + recording.messages.size());
            System.out.println("    number of placed symbols: " + placedSymbolsCount
                    + (placedSymbolsCount < 32000 ? "" : " THIS MAY BE TOO MANY SYMBOLS!!! FLASH MAY NOT WORK!!!"));

            // writing Flash movie to SWF file
            if (!batch) {
            	if (progressMonitor.isCanceled()) {
            		return;
            	}
                progressMonitor.setProgress(80);
                progressMonitor.setNote("writing flash movie");
            }
            System.out.println("    writing flash movie: " + recording.getDirectory() + recording.getFileBase()
                    + ".swf");
            movie.write(recording.getDirectory() + recording.getFileBase() + ".swf");

            // writing Flash movie without any control elements
            // useful if converting flash to other formats
            if (TTT.userPrefs.getBoolean("write_flash_without_controls", false)) {
                if (!batch) {
                	if (progressMonitor.isCanceled()) {
                		return;
                	}
                    progressMonitor.setProgress(90);
                    progressMonitor.setNote("writing flash movie without controls");
                }
                movie = mainMovieclipOnly();
                System.out.println("Writing Flash Movie: " + recording.getDirectory() + recording.getFileBase()
                        + "_without_controls" + ".swf");
                movie.write(recording.getDirectory() + recording.getFileBase() + "_without_controls" + ".swf");
            }

            if (!batch) {
                progressMonitor.setProgress(100);
                progressMonitor.setNote("done");
            }
        } finally {
            // remove process monitor
            if (!batch && progressMonitor != null)
                progressMonitor.close();
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // Mapping of TTT-Messages to Flash-Frames
    // Place all messages with a timestamp inside of intervalMsec on the same frame.
    // If there are no messages inside of this intervall, add an empty frame.
    // /////////////////////////////////////////////////////////////////////////////

    double timeSec_0 = 0;
    double timeMsec_0 = 0;
    double timeMsec_Inc = 0;
    int nFrames = 0;
    int blockIndex = 0;
    double thumbnailTimestampBy16000Frames = 0;
    double thumbnailTimestampBy16000Symbols = 0;
    boolean isFrameNbEq16000_Or_SymbolNbEq16000 = false;
    public boolean isFirstFrameOfClip = false;
    public boolean isCursor;

    public Instance hextileInstance = null;
    public int hextileInstanceFrameNr = -1;
    public Frame hextileInstanceFrame = null;
    public Rectangle hextileBounds = null;
    public Shape hextileShapeImage = null;

    public void checkNextFrame(int timeStamp) throws IOException {
        check16000Symbols(thumbnailTimestampBy16000Symbols + intervalMsec);
        outWhile: while (timeSec_0 * 1000 <= timeStamp) {

            if (timeMsec_Inc <= timeStamp) {
                nFrames++;
                if (nFrames != 1) {
                    lastFrameOfClip(timeStamp, timeMsec_Inc, frame);
                }

                check16000Frames();

                frameNumb++;

                // KEYFRAME
                // place a full image on the first frame of each movieclip
                if (frameNumb == 2) {
                    int colorDepthFirstFrame;
                    // If there is no hextile rect in the frame interval, one uses 32 bits.
                    if (HextileMessage.isHextileRect) {
                        /*
                         * If a hextile rect in the frame interval contains one or more raw coded subrectangles or if
                         * the size of a hextile rect in this intervall are smaller than the framebuffer size, 32 bits
                         * are used.
                         */
                        if (HextileMessage.isRawRectFirstFrame || HextileMessage.isNotFullImage) {
                            colorDepthFirstFrame = SWFConstants.BITMAP_FORMAT_32_BIT;
                            HextileMessage.isRawRectFirstFrame = false;
                            HextileMessage.isNotFullImage = false;
                        } else {
                            colorDepthFirstFrame = colorDepth;
                        }
                        HextileMessage.isHextileRect = false;
                    } else {
                        colorDepthFirstFrame = SWFConstants.BITMAP_FORMAT_32_BIT;
                    }

                    // System.out.println("KEYFRAME AT " + Constants.getStringFromTime(timeStamp));

                    Image screenshot;
                    screenshot = ((BufferedImage) recording.graphicsContext.memImage).getSubimage(0, 0,
                            recording.prefs.framebufferWidth, recording.prefs.framebufferHeight);

                    com.anotherbigidea.flash.movie.Image.Lossless img = ImageUtil.createLosslessImage(screenshot,
                            colorDepthFirstFrame, true);
                    Shape shapeImage = ImageUtil.shapeForImage(img, (double) recording.prefs.framebufferWidth,
                            (double) recording.prefs.framebufferHeight);
                    frame.placeSymbol(shapeImage, 0, 0, HextileMessage.depthOfHextileRects++);

                    isFirstFrameOfClip = false;

                }

                // NEXT FRAME
                // add a frame to the movieclip
                // frame = movieClip.appendFrame();
                frame = movieClip.appendFrame();

                if (frameNumb == 1) {
                    // access to cursor image
                    // Image softCursor = recording.graphicsContext.softCursor;
                    isCursor = true;
                    firstFrameOfClip(timeStamp, frame);
                }

                thumbnailTimestampBy16000Frames = thumbnailTimestampBy16000Frames + intervalMsec;

                // SOUND
                // add sound data in the frame of the movieclip (the sound header are placed on the first frame on each
                // movieclip)
                if (frameNumb != 1) {
                    if (blockIndex < blocks.size()) {
                        byte[] data = (byte[]) blocks.get(blockIndex);
                        frame.setSoundData(data);
                        blockIndex++;
                    }
                }

            }// end if (timeMsec_Inc <= timeMsec )
            else {
                break outWhile;
            }
            timeMsec_Inc += intervalMsec;
            timeMsec_0 += intervalMsec;

            if (1000 <= timeMsec_0) {
                timeMsec_0 -= 1000;
                timeSec_0++;
            }
        }// end while

        // for later check if the previous timestamp is the same like the actual
        prevTimestamp = timeStamp;

        thumbnailTimestampBy16000Symbols = timeStamp;

    }

    // ////////////////////////////////////////////////////////////////
    // Annotations
    // ////////////////////////////////////////////////////////////////

    // Zuordnung zwischen Annotationen und Flash-Objekten
    private ArrayList<Annotation> annotations = new ArrayList<Annotation>();
    private ArrayList<Instance> bufferFlashAnnotations = new ArrayList<Instance>();

    // add annotations to annoation list
    synchronized public void addAnnotations(Annotation annotation, Instance instance) {
        annotations.add(annotation);
        bufferFlashAnnotations.add(instance);
    }

    // remove all annotations
    synchronized public void clearAnnotations(boolean isClearBuffer, Frame givenFrame) {

        for (int i = 0; i < annotations.size(); i++) {
            if (bufferFlashAnnotations.get(i) != null) {
                givenFrame.remove(bufferFlashAnnotations.get(i));
            }
        }
        if (isClearBuffer) {
            annotations.clear();
            bufferFlashAnnotations.clear();
        }
    }

    // replace removed annotations
    synchronized public void replaceAnnotations(Frame fr) {
        for (int i = 0; i < annotations.size(); i++) {
            Instance instance = bufferFlashAnnotations.get(i);
            if (instance != null) {
                Symbol symbol = instance.getSymbol();
                fr.placeSymbol(symbol, 0, 0, annotationsDepth++);
            }
        }
    }

    // find and remove annotations at given coordinates
    synchronized public void removeAnnotationsAt(int x, int y) {
        int i = 0;
        while (i < annotations.size()) {
            if (annotations.get(i).contains(x, y)) {
                // remove Symbol from frame
                if (bufferFlashAnnotations.get(i) != null) {
                    frame.remove(bufferFlashAnnotations.get(i));
                }
                annotations.remove(i);
                bufferFlashAnnotations.remove(i);
            } else
                i++;
        }
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Insert thumbnails into the main movieclip. Each thumbnail is represented
    // by a movieclip. Timestamp and number of the corresponding thumbnail are
    // place on it as text.
    // ///////////////////////////////////////////////////////////////////////////

    MovieClip[] createThumnails() throws IOException {
        Actions thumbnailActions[] = new Actions[bufferTimestamp.size()];
        Button thumbnailButtons[] = new Button[bufferTimestamp.size()];
        MovieClip thumbnailsClips[] = new MovieClip[bufferTimestamp.size()];

        // System.out.println("Index table entries: "+bufMovieClip.size()+" or "+ bufferTimestamp.size());
        for (int i = 0; i < Math.min(bufMovieClip.size(), bufferTimestamp.size()); i++) {

            // System.out.println("Index "+(i+1)+":\t"+Constants.getStringFromTime(bufferTimestamp.get(i)));

            thumbnailButtons[i] = new Button(false);
            ImageIcon imIcon = bufferThumbnails.get(i);
            Image imag = imIcon.getImage();
            // create a Define Bit Lossless image for thumbnails. Thumbnails are allways
            // converted with a color depth of 32. So we don't need to enter this value
            // throw the user interface
            com.anotherbigidea.flash.movie.Image.Lossless thumbn = ImageUtil.createLosslessImage(imag,
                    SWFConstants.BITMAP_FORMAT_32_BIT, true);
            com.anotherbigidea.flash.movie.Shape shapeThumbnail = shapeForThumbail(thumbn,
                    (double) imag.getWidth(null), (double) imag.getHeight(null),
                    new com.anotherbigidea.flash.structs.Color(0, 0, 0));
            // create a new clip for this thumbnail
            thumbnailsClips[i] = new MovieClip();
            // append a frame to the thumbnail's clip
            Frame thumbFrame = thumbnailsClips[i].appendFrame();

            thumbnailButtons[i].addLayer(shapeThumbnail, new Transform(), new AlphaTransform(), i, true, true, false,
                    true);

            thumbnailActions[i] = thumbnailButtons[i].addActions(SWFConstants.BUTTON2_OVERUP2OVERDOWN, 5);

            // place Button on thumbnailsClip
            thumbFrame.placeSymbol(thumbnailButtons[i], 0, 0, 1);

            // place thumbnail's number on thumbnailsClip
            Text textNum = createText("#" + (i + 1), 10);
            thumbFrame.placeSymbol(textNum, 4, 10, 2);
            // place index's timestamp on thumbnailsClip
            Text textTime = createText(Constants.getStringFromTime(bufferTimestamp.get(i), false), 12);
            thumbFrame.placeSymbol(textTime, thumbnailWidth - 40, thumbnailHeight - 4, 3);

            // make all movieclip (slide) invisilble
            for (int l = 0; l < bufMovieClip.size(); l++) {
                setMovieClipVisibility("mclip" + l, false, thumbnailActions[i]);
            }

            // _root.clip1._y = _root.clip2._y ; Set the y coordinate of "clipMarker" to the y coordinate
            // of "thumbnailsClip"+i so that the marker jump to "thumbnailsClip"+i

            thumbnailActions[i].lookupTable(new String[] { "_root", "clipMarker", "_y", "thumbnailsClip" + i });
            thumbnailActions[i].lookup(0);
            thumbnailActions[i].getVariable();
            thumbnailActions[i].lookup(1);
            thumbnailActions[i].getMember();
            thumbnailActions[i].lookup(2);
            thumbnailActions[i].lookup(0);
            thumbnailActions[i].getVariable();
            thumbnailActions[i].lookup(3);
            thumbnailActions[i].getMember();
            thumbnailActions[i].lookup(2);
            thumbnailActions[i].getMember();
            thumbnailActions[i].setMember();

            stopCurrentClipInFrameBeforeLast(thumbnailActions[i]);
            setOffsetByThumbnail(i, thumbnailActions[i]);
            isStopFalse_TimeClipPlay(thumbnailActions[i]);
            setMovieClipVisibility("mclip" + i, true, thumbnailActions[i]);
            movieClipGotoFramePlay(1, "mclip" + i, thumbnailActions[i]);
            // updateTimelabel(thumbnailActions[i]);

            thumbnailActions[i].end();
        }
        return thumbnailsClips;
    }

    // ////////////////////////////////////////////////////////////////
    // Insert rectangle
    // ////////////////////////////////////////////////////////////////
    static public Shape insertRect(int x, int y, com.anotherbigidea.flash.structs.Color lineColor,
            com.anotherbigidea.flash.structs.Color fillColor) {

        Shape shape = new Shape();
        shape.defineFillStyle(fillColor);
        shape.defineLineStyle(1.0, lineColor);
        shape.setRightFillStyle(1);
        shape.setLineStyle(1);
        shape.move(0, 0);
        shape.line(0, y);
        shape.line(x, y);
        shape.line(x, 0);
        shape.line(0, 0);

        return shape;
    }

    static public Shape shapeForThumbail(com.anotherbigidea.flash.movie.Image image, double width, double height,
            com.anotherbigidea.flash.structs.Color color) {
        Shape s = new Shape();

        Transform matrix = new Transform(SWFConstants.TWIPS, SWFConstants.TWIPS, 0.0, 0.0);

        s.defineFillStyle(image, matrix, true);
        s.setRightFillStyle(1); // use image fill
        s.defineLineStyle(1, color);
        s.setLineStyle(1);
        s.setRightFillStyle(1);
        s.line(width, 0);
        s.line(width, height);
        s.line(0, height);
        s.line(0, 0);

        return s;
    }

    // ////////////////////////////////////////////////////////////////
    // Insert actions into the first Frame of each Movieclip
    // ////////////////////////////////////////////////////////////////

    int mclipNumber = 0;

    void firstFrameOfClip(int timeStamp, Frame firstFrame) throws IOException {
        // if the number of frames other the number of Symbols in the previous clip
        // was 16000, place annotations of this clip on the first frame of the next clip
        if (isFrameNbEq16000_Or_SymbolNbEq16000) {
            replaceAnnotations(firstFrame);
            isFrameNbEq16000_Or_SymbolNbEq16000 = false;
        } else {
            annotations.clear();
            bufferFlashAnnotations.clear();
        }

        // set the sound header
        firstFrame.setSoundHeader(head);

        // add action to the first frame
        Actions actions = firstFrame.actions(FLASH_VERSION);

        // _root.mcNumber = mclipNumber
        actions.push("_root");
        actions.getVariable();
        actions.push("mcNumber");
        actions.push(mclipNumber);
        actions.setMember();

        mclipNumber++;
        // write the timestamp of this clip to indexTimeStamp
        int timestamp = bufferTimestampClip.get(0).intValue();
        actions.push("indexTimeStamp");
        actions.push(timestamp);
        actions.setVariable();

        actions.end();
        firstFrame.setActions(actions);
        actions.done();
        // remove the timestamp from bufferTimestampClip
        bufferTimestampClip.remove(0);
        isFirstFrameOfClip = true;
        /*
         * index is use to add new timestamps and new thumbnails in buffer if the number of frames or symbol reached
         * 16000
         */
        index += 1;
    }

    // ADDED 17.10.2007 by ziewer
    // buffer finished indices to avoid double finishing (which causes doubled audio replay)
    Set<Integer> finishedIndices = new HashSet<Integer>();

    // ////////////////////////////////////////////////////////////////////////
    // Insert actions into the last Frame of each movieclip if the message
    // corresponding to the index is the first in the corresponding intervall
    // /////////////////////////////////////////////////////////////////////////

    void lastFrameOfClip(int timeStamp, double timeMsec_Inc, Frame lastFrame) throws IOException {
        int timeMillisec = timeStamp;

        for (int j = 0; j < bufferTimestampClip.size(); j++) {
            int indexTime = bufferTimestampClip.get(j);

            // check if the index's timestamp is equal to message's timestamp
            //
            // MODIFIED 17.10.2007 by ziewer:
            // WAS: if (indexTime == timeMillisec && timeMillisec != prevTimestamp) {
            if (indexTime <= timeMillisec && timeMillisec != prevTimestamp) {

                if (timeMillisec - timeMsec_Inc < intervalMsec || timeMillisec - timeMsec_Inc == 0) {
                    if (flash_debug) {
                        // System.out.println("\n---------------------------------");
                        System.out.println("lastFrameOfClip: Matching Index " + (nc + 2) + " at "
                                + Constants.getStringFromTime(indexTime));
                    }

                    // ADDED 17.10.2007 by ziewer
                    // check if already processed
                    if (finishedIndices.contains(indexTime)) {
                        if (flash_debug)
                            System.out.println("-- SKIPPED --");
                        continue;
                    } else {
                        // buffer finished index
                        finishedIndices.add(indexTime);
                    }
                    // end ADDED

                    nc = nc + 1;

                    Frame beforeLastFrame = movieClip.getFrame(frameNumb - 1);
                    if (beforeLastFrame != null) {
                        // remove annotations of this clip in the frame before last
                        clearAnnotations(false, beforeLastFrame);
                        // remove cursor's instance in the frame before last
                        if (CursorMessage.cursorInstance != null) {
                            beforeLastFrame.remove(CursorMessage.cursorInstance);
                        }
                        // remove witheboard's instance in the frame before last
                        if (WhiteboardMessage.whiteBoardInstance != null) {
                            beforeLastFrame.remove(WhiteboardMessage.whiteBoardInstance);
                        }
                    }

                    // stop the movieclip by its last frame to avoid movie looping
                    if (lastFrame != null)
                        lastFrame.stop();

                    bufMovieClip.add(movieClip);

                    if (flash_debug && false) {
                        System.out.println("\nLASTFRAMEOFCLIP\n" + "Movie ends at "
                                + Constants.getStringFromTime(indexTime));
                        System.out.println("Frames: " + movieClip.getFrameCount() + "\t" + frameNumb);
                        System.out.println("Symbols: " + Symbol.count + "\t" + symbolCount + "\tPLACED "
                                + placedSymbolsCount);
                        Symbol.count = 0;
                        System.out.println("Layers: " + layers.size());
                        layers.clear();
                    }

                    // reset the number of frame and symbol
                    frameNumb = 0;
                    movieClip = null;

                    symbolCount = 0;
                    // reset detph
                    HextileMessage.depthOfHextileRects = 0;
                    annotationsDepth = 30000;
                    // create new movieclip
                    movieClip = new MovieClip();

                    if (lastFrame != null) {
                        // add Actions to the last Frame of the clip
                        Actions frameAction = lastFrame.actions(FLASH_VERSION);
                        // make the next Movieclip visible
                        setMovieClipVisibility("mclip" + nc, true, frameAction);
                        // Go to the first frame of the next Movieclip and play
                        // _root.mclip.gotoAndPlay(1);
                        movieClipGotoFramePlay(1, "mclip" + nc, frameAction);

                        scrollThumbnailUp("thumbnailsClip", frameAction, recording.index.size(), thumbnailHeight);

                        frameAction.end();
                        lastFrame.setActions(frameAction);
                        frameAction.done();
                    }
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // Insert actions into the last Frame of each movieclip if the message
    // corresponding to the index is not the first in the corresponding interval
    // /////////////////////////////////////////////////////////////////////////////

    void lastFrameOfClipExtra(int timeStamp, Frame lastFrame) throws IOException {
        int timeMillisec = timeStamp;

        for (int j = 0; j < bufferTimestampClip.size(); j++) {
            int indexTime = bufferTimestampClip.get(j);

            // check if the index's timestamp is equal to message's timestamp
            //
            // MODIFIED 17.10.2007 by ziewer:
            // WAS: if ((indexTime == timeMillisec) && (timeMillisec != previousTimestamp)) {
            if (indexTime <= timeMillisec && timeMillisec != previousTimestamp) {
                if (flash_debug) {
                    // System.out.println("\n---------------------------------");
                    System.out.println("lastFrameOfClipExtra: Matching Index " + (nc + 2) + " at "
                            + Constants.getStringFromTime(indexTime));
                }

                // ADDED 17.10.2007 by ziewer
                // check if already processed
                if (finishedIndices.contains(indexTime)) {
                    if (flash_debug)
                        System.out.println("-- SKIPPED --");
                    continue;
                } else {
                    // buffer finished index
                    finishedIndices.add(indexTime);
                }
                // end ADDED

                // write cached shape
                if (hextileBounds != null) {
                    BufferedImage bufImage = (BufferedImage) recording.graphicsContext.memImage;
                    BufferedImage subImage = bufImage.getSubimage(hextileBounds.x, hextileBounds.y,
                            hextileBounds.width, hextileBounds.height);

                    // create a Define Bit Lossless image for subImage
                    com.anotherbigidea.flash.movie.Image.Lossless img = ImageUtil.createLosslessImage(subImage,
                            colorDepth, true);

                    // create a shape that uses the image as a fill
                    // (images cannot be placed directly. They can only be used as shape fills)
                    com.anotherbigidea.flash.movie.Shape shapeImage = ImageUtil.shapeForImage(img, (double) subImage
                            .getWidth(), (double) subImage.getHeight());
                    hextileShapeImage = shapeImage;

                    // place hextile rect on the stage
                    hextileInstanceFrame.placeSymbol(hextileShapeImage, hextileBounds.x, hextileBounds.y,
                            HextileMessage.depthOfHextileRects++);
                    hextileShapeImage = null;
                    hextileBounds = null;
                    hextileInstanceFrame = null;
                    hextileInstanceFrameNr = -1;
                }

                nc = nc + 1;
                Frame beforeLastFrame = movieClip.getFrame(frameNumb - 1);
                if (beforeLastFrame != null) {
                    // remove annotations of this clip in the frame before last
                    clearAnnotations(false, beforeLastFrame);
                    // remove cursor's instance in the frame before last
                    if (CursorMessage.cursorInstance != null) {
                        beforeLastFrame.remove(CursorMessage.cursorInstance);
                    }
                    // remove witheboard's instance in the frame before last
                    if (WhiteboardMessage.whiteBoardInstance != null) {
                        beforeLastFrame.remove(WhiteboardMessage.whiteBoardInstance);
                    }
                }
                // stop the movieclip by its last frame to avoid movie looping
                if (lastFrame != null)
                    lastFrame.stop();

                bufMovieClip.add(movieClip);

                if (flash_debug && false) {
                    System.out.println("\nLASTFRAMEOFCLIPEXTRA\n" + "Movie ends at "
                            + Constants.getStringFromTime(indexTime));
                    System.out.println("Frames: " + movieClip.getFrameCount() + "\t" + frameNumb);
                    System.out.println("Symbols: " + Symbol.count + "\t" + symbolCount);
                    Symbol.count = 0;
                    System.out.println("Layers: " + layers.size());
                    layers.clear();
                }

                // reset value
                frameNumb = 0;
                movieClip = null;
                symbolCount = 0;

                // reset detph
                HextileMessage.depthOfHextileRects = 0;
                annotationsDepth = 30000;
                // creat new movieclip (slide)
                movieClip = new MovieClip();

                if (lastFrame != null) {
                    // add Actions to the last Frame of the clip
                    Actions frameAction = lastFrame.actions(FLASH_VERSION);

                    // make the next Movieclip visible
                    setMovieClipVisibility("mclip" + nc, true, frameAction);
                    // Go to the first frame of the next Movieclip and play
                    // _root.mc.gotoAndPlay(1);
                    movieClipGotoFramePlay(1, "mclip" + nc, frameAction);

                    scrollThumbnailUp("thumbnailsClip", frameAction, recording.index.size(), thumbnailHeight);

                    frameAction.end();
                    lastFrame.setActions(frameAction);
                    frameAction.done();
                }
            }
        }
    }

    // //////////////////////////////////////////////////////////////////////
    // Main MovieClip
    // Place all movieclip (slides, thumbanils, timer and marker) and control
    // elements on the main movieclip. Make all movieclip(slides)unvislible
    // except the first. Stop all movieclip(slides)on their second frame
    // except the first movieclip.
    // ///////////////////////////////////////////////////////////////////////

    static final Color backgroundColor = new Color(252, 252, 253);

    Movie mainMovieclip() throws IOException {
        // ceate the main movieclip
        Movie movie = new Movie(recording.prefs.framebufferWidth + thumbnailWidth + 3,
                recording.prefs.framebufferHeight + 40, frameRate, FLASH_VERSION, null);
        System.out.println("    output movie size: " + movie.getWidth() + "x" + movie.getHeight());

        // movie.appendFrame();
        // the main timeline contain only one frame
        Frame frameMovie = movie.appendFrame();
        frameMovie.stop();

        Shape shape = new Shape();
        shape.defineFillStyle(backgroundColor);
        shape.defineLineStyle(2.0, backgroundColor);
        shape.setRightFillStyle(1);
        shape.setLineStyle(1);
        shape.move(0, 0);
        shape.line(movie.getWidth(), 0);
        shape.line(movie.getWidth(), movie.getHeight());
        shape.line(0, movie.getHeight());
        shape.line(0, 0);
        frameMovie.placeSymbol(shape, 0, 0);
        frameMovie.placeSymbol(createText("LOADING", 32), recording.prefs.framebufferWidth / 2,
                recording.prefs.framebufferHeight / 2);

        // place control elements on the Frame (main movieclip)
        int x_dist = 0;
        x_dist += 2;
        frameMovie.placeSymbol(createButtonPreviousSlide(), x_dist, recording.prefs.framebufferHeight + 2);
        x_dist += 36 + 4;
        frameMovie.placeSymbol(createButtonBackward(), x_dist, recording.prefs.framebufferHeight + 2);
        x_dist += 36 + 12 + 4;
        frameMovie.placeSymbol(createButtonPlay(), x_dist, recording.prefs.framebufferHeight + 2);
        x_dist += 36 + 4;
        frameMovie.placeSymbol(createButtonStop(), x_dist, recording.prefs.framebufferHeight + 2);
        x_dist += 36 + 12 + 4;
        frameMovie.placeSymbol(createButtonForward(), x_dist, recording.prefs.framebufferHeight + 2);
        x_dist += 36 + 4;
        frameMovie.placeSymbol(createButtonNextSlide(), x_dist, recording.prefs.framebufferHeight + 2);
        x_dist += 36 + 12 + 4;

        // place the time clip on the main Movieclip
        frameMovie.placeMovieClip(createPlaybackTimefield(recording.prefs, movie.getVersion()), new Transform(x_dist,
                recording.prefs.framebufferHeight), null, "timeClip", null);

        // place name of recordings
        x_dist += 100;
        frameMovie.placeSymbol(createText(recording.prefs.name, 16), x_dist, recording.prefs.framebufferHeight + 24);

        // insert list of thumbnails
        MovieClip thumbnailsClips[] = createThumnails();

        // place thumbnailsClip on the main Movieclip
        // modified by ziewer
        for (int j = 0; j < Math.min(bufMovieClip.size(), thumbnailsClips.length); j++) {
            // for (int j = 0; j < bufMovieClip.size(); j++) {
            // end modified
            frameMovie.placeMovieClip(thumbnailsClips[j], new Transform(recording.prefs.framebufferWidth + 2, 24 + j
                    * thumbnailHeight + j * 4), null, "thumbnailsClip" + j, null);
        }

        // create a clip for tumbnails marker
        MovieClip clipMarker = new MovieClip();
        Frame frameMarker = clipMarker.appendFrame();
        com.anotherbigidea.flash.movie.Shape shapeMarker = insertRect(thumbnailWidth, thumbnailHeight,
                new com.anotherbigidea.flash.structs.Color(255, 0, 0), new AlphaColor(255, 255, 255, 0));
        // place the marker (rectangle) on the Clipmarker
        frameMarker.placeSymbol(shapeMarker, recording.prefs.framebufferWidth + 2, 0);
        // place the Clipmarker on the main Movieclip
        frameMovie.placeMovieClip(clipMarker, new Transform(0, 24), null, "clipMarker", null);

        // add thumbnail up/down buttons
        frameMovie.placeSymbol(createButtonScrollThumbnailUp(), recording.prefs.framebufferWidth + 1, 0);
        frameMovie.placeSymbol(createButtonScrollThumbnailDown(), recording.prefs.framebufferWidth + 1,
                recording.prefs.framebufferHeight - 22);

        // add TTT note and link
        // add solid background (overriding thumbnails)
        frameMovie.placeSymbol(insertRect(172, -200, backgroundColor, backgroundColor),
                recording.prefs.framebufferWidth, 0);
        frameMovie.placeSymbol(insertRect(172, 200, backgroundColor, backgroundColor),
                recording.prefs.framebufferWidth, recording.prefs.framebufferHeight);
        frameMovie.placeSymbol(createText("produced by TeleTeachingTool", 11), recording.prefs.framebufferWidth + 4,
                recording.prefs.framebufferHeight + 12);
        frameMovie.placeSymbol(createText("http://ttt.in.tum.de", 11), recording.prefs.framebufferWidth + 4,
                recording.prefs.framebufferHeight + 30);

        // place Movieclips (slides) on the main Movieclip
        for (int i = 0; i < bufMovieClip.size(); i++) {
            frameMovie.placeMovieClip((MovieClip) bufMovieClip.get(i), new Transform(0, 0), null, "mclip" + i, null);
        }

        // ACTIONS
        Actions act1 = frameMovie.actions(5);

        for (int l = 0; l < bufMovieClip.size(); l++) {
            setMovieClipVisibility("mclip" + l, false, act1);
            movieClipGotoFrameStop(2, "mclip" + l, act1); 
        }
        setMovieClipVisibility("mclip0", true, act1);
        movieClipGotoFramePlay(1, "mclip0", act1);

        // initialise offset to zero
        act1.push("offset");
        act1.push(0);
        act1.setVariable();
        // initialise stopTime to zero
        act1.push("stopTime");
        act1.push(0);
        act1.setVariable();
        // initialise isStop to false
        act1.push("isStop");
        act1.push(false);
        act1.setVariable();

        act1.push("mclip0_Y");
        act1.push(0);
        act1.setVariable();

        act1.end();
        frameMovie.setActions(act1);
        act1.done();

        return movie;
    }

    Movie mainMovieclipOnly() throws IOException {

        Movie movie = new Movie(recording.prefs.framebufferWidth, recording.prefs.framebufferHeight, frameRate,
                FLASH_VERSION, null);

        movie.appendFrame();

        // the main timeline contain only one frame
        Frame frameMovie = movie.appendFrame();
        frameMovie.stop();

        // place Movieclips (slides) on the main Movieclip
        for (int i = 0; i < bufMovieClip.size(); i++) {
            frameMovie.placeMovieClip((MovieClip) bufMovieClip.get(i), new Transform(0, 0), null, "mclip" + i, null);
        }

        Actions act1 = frameMovie.actions(5);

        for (int l = 0; l < bufMovieClip.size(); l++) {
            setMovieClipVisibility("mclip" + l, false, act1);
            movieClipGotoFrameStop(2, "mclip" + l, act1); 
        }
        setMovieClipVisibility("mclip0", true, act1);
        movieClipGotoFramePlay(1, "mclip0", act1);

        // initialise offset to zero
        act1.push("offset");
        act1.push(0);
        act1.setVariable();
        // initialise stopTime to zero
        act1.push("stopTime");
        act1.push(0);
        act1.setVariable();
        // initialise isStop to false
        act1.push("isStop");
        act1.push(false);
        act1.setVariable();

        act1.push("mclip0_Y");
        act1.push(0);
        act1.setVariable();

        act1.end();
        frameMovie.setActions(act1);
        act1.done();

        return movie;
    }

    // //////////////////////////////////////////////////////////////////
    // If the number of frames in a Movieclip reach 16000, create a new
    // movieclip and a new Thumbnail with a new timestamp
    // //////////////////////////////////////////////////////////////////
    public static SortedSet<Integer> layers = new TreeSet<Integer>();
    public static int placedSymbolsCount;

    void check16000Frames() throws IOException {

        if (frameNumb == 16000) {
            System.out.println("    frame limit reached - split at "
                    + Constants.getStringFromTime((int) thumbnailTimestampBy16000Frames));
            nc = nc + 1;

            Frame beforeLastFrame = movieClip.getFrame(frameNumb - 1);
            // remove annotations of this clip in the frame before last
            clearAnnotations(false, beforeLastFrame);
            // remove cursor's instance in the frame before last
            if (CursorMessage.cursorInstance != null) {
                beforeLastFrame.remove(CursorMessage.cursorInstance);
            }
            // stop the movieclip by its last frame to avoid movie looping
            frame.stop();
            bufMovieClip.add(movieClip);

            if (flash_debug) {
                System.out.println("\nCHECKFRAMES\n" + "Movie ends at "
                        + Constants.getStringFromTime((int) thumbnailTimestampBy16000Frames));
                System.out.println("Frames: " + movieClip.getFrameCount() + "\t" + frameNumb);
                System.out.println("Symbols: " + Symbol.count + "\t" + symbolCount);
                Symbol.count = 0;
                System.out.println("Layers: " + layers.size());
                layers.clear();
            }

            // reset value
            frameNumb = 0;
            movieClip = null;
            symbolCount = 0;

            // reset detphs
            HextileMessage.depthOfHextileRects = 0;
            annotationsDepth = 30000;
            movieClip = new MovieClip();

            Double doubleObj = new Double(thumbnailTimestampBy16000Frames);

            // TODO: do not add thumbnail for additional clip
            // TODO: last thumbnail seems to be lost after insertion
            // Add the new timestamp (Thumbnail) in the buffer of timestamp
            bufferTimestamp.add(index, doubleObj.intValue());

            // Add the new timestamp (Thumbnail) in the buffer of timestamp Clip
            bufferTimestampClip.add(0, doubleObj.intValue());

            Image screenshot = recording.graphicsContext.getScreenshotWithoutAnnotations();
            // Create a new Thumbnail
            Image thumbnailImage = ScriptCreator.getScaledInstance(screenshot, recording.prefs.framebufferWidth
                    / recording.index.getThumbnailScaleFactor(), recording.prefs.framebufferHeight
                    / recording.index.getThumbnailScaleFactor());
            // was:
            // Image thumnailImage = screenshot.getScaledInstance(recording.prefs.framebufferWidth
            // / recording.index.getThumbnailScaleFactor(), recording.prefs.framebufferHeight
            // / recording.index.getThumbnailScaleFactor(), Image.SCALE_SMOOTH);

            ImageIcon thumnailIcon = new ImageIcon(thumbnailImage);
            // Add the new thumbanil in the buffer of Thumbnails
            bufferThumbnails.add(index, thumnailIcon);

            // add Actions to the last Frame of the clip
            Actions frameAction = frame.actions(FLASH_VERSION);

            // make the next Movieclip visible
            setMovieClipVisibility("mclip" + nc, true, frameAction);
            // Go to the first frame of the next Movieclip and play
            // _root.mc.gotoAndPlay(1);
            movieClipGotoFramePlay(1, "mclip" + nc, frameAction);
            scrollThumbnailUp("thumbnailsClip", frameAction, recording.index.size(), thumbnailHeight);

            frameAction.end();
            frame.setActions(frameAction);
            frameAction.done();

            isFrameNbEq16000_Or_SymbolNbEq16000 = true;
        }
    }

    // //////////////////////////////////////////////////////////////////
    // If the number of symbols in a Movieclip reach 16000, create a new
    // movieclip and a new Thumbnail with a new timestamp
    // //////////////////////////////////////////////////////////////////
    void check16000Symbols(double timeStamp) throws IOException {

        if (symbolCount == 16000) {
            System.out.println("    symbol limit reached - split");
            nc = nc + 1;

            Frame beforeLastFrame = movieClip.getFrame(frameNumb - 1);
            // remove annotations of this clip in the frame before last
            clearAnnotations(false, beforeLastFrame);
            // remove cursor's instance in the frame before last
            if (CursorMessage.cursorInstance != null) {
                beforeLastFrame.remove(CursorMessage.cursorInstance);
            }
            // stop the movieclip by its last frame to avoid movie looping
            frame.stop();
            bufMovieClip.add(movieClip);

            if (flash_debug) {
                System.out.println("\nCHECKSYMBOLS\n" + "Movie ends at "
                        + Constants.getStringFromTime((int) thumbnailTimestampBy16000Frames));
                System.out.println("Frames: " + movieClip.getFrameCount() + "\t" + frameNumb);
                System.out.println("Symbols: " + Symbol.count + "\t" + symbolCount);
                Symbol.count = 0;
                System.out.println("Layers: " + layers.size());
                layers.clear();
            }

            // reset value
            frameNumb = 0;
            movieClip = null;
            symbolCount = 0;

            // reset detphs
            HextileMessage.depthOfHextileRects = 0;
            annotationsDepth = 30000;
            // create new movieclip (slide)
            movieClip = new MovieClip();
            // Add the new timestamp (Thumbnail) in the buffer of timestamp
            Double doubleObj = new Double(timeStamp);
            bufferTimestamp.add(index, doubleObj.intValue());

            // Add the new timestamp (Thumbnail) in the buffer of timestamp Clip
            bufferTimestampClip.add(0, doubleObj.intValue());

            Image screenshot = recording.graphicsContext.getScreenshotWithoutAnnotations();
            // Create a new Thumbnail
            Image thumbnailImage = ScriptCreator.getScaledInstance(screenshot, recording.prefs.framebufferWidth
                    / recording.index.getThumbnailScaleFactor(), recording.prefs.framebufferHeight
                    / recording.index.getThumbnailScaleFactor());
            // was:
            // Image thumnailImage = screenshot.getScaledInstance(recording.prefs.framebufferWidth
            // / recording.index.getThumbnailScaleFactor(), recording.prefs.framebufferHeight
            // / recording.index.getThumbnailScaleFactor(), Image.SCALE_SMOOTH);

            ImageIcon thumnailIcon = new ImageIcon(thumbnailImage);
            // Add the new thumbanil in the buffer of Thumbnails
            bufferThumbnails.add(index, thumnailIcon);

            // add Actions to the last Frame of the clip
            Actions frameAction = frame.actions(FLASH_VERSION);

            // make the next Movieclip visible
            setMovieClipVisibility("mclip" + nc, true, frameAction);
            // Go to the first frame of the next Movieclip and play
            // _root.mc.gotoAndPlay(1);
            movieClipGotoFramePlay(1, "mclip" + nc, frameAction);
            scrollThumbnailUp("thumbnailsClip", frameAction, recording.index.size(), thumbnailHeight);

            frameAction.end();
            frame.setActions(frameAction);
            frameAction.done();

            isFrameNbEq16000_Or_SymbolNbEq16000 = true;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0)
            System.out.println("TTT2SWF generates Flash movies from TTT input file(s)");
        else
            for (String arg : args) {
                System.out.println("\nopening ttt recording: " + arg);
                Recording recording = new Recording(arg, false);
                System.out.println();
                System.out.println("----------------------------------------------");
                System.out.println("TTT2Flash Converter");
                System.out.println("----------------------------------------------");
                createFlash(recording, true);
            }
        System.exit(0);
    }
}
