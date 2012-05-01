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
 * Created on 20.02.2012
 *
 * Author: Manuel Thurner, TU Munich, Germany - manuel.thurner@mytum.de
 */
package ttt.postprocessing.html5;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.ProgressMonitor;

import ttt.Constants;
import ttt.TTT;
import ttt.audio.LameEncoder;
import ttt.gui.IndexEntry;
import ttt.messages.Message;
import ttt.record.Recording;
import biz.source_code.base64Coder.Base64Coder;

public class Html5Context {

    public static boolean html5_debug = false;
    public static final String thumbnailImageFormat = "png";
	public static final String HTML5_DIRECTORY = "webplayer";
	public static final String FILE_NAME = "lecture";
    private boolean batch;
    public Recording recording;
    public BufferedWriter out;

    static public void createJson(Recording recording, boolean batch) throws IOException {
        Html5Context html5Context = new Html5Context(recording, batch);
        
        html5Context.createJson();
    }
    
    public static boolean isCreationPossible(Recording recording) throws IOException {
		return recording.getExistingFileBySuffix("mp3").exists() || (LameEncoder.isLameAvailable() && recording.getExistingFileBySuffix(new String[] {"wav","mp2"}).exists());
	}

    protected Html5Context(Recording recording, boolean batch) {
        this.recording = recording;
        this.batch = batch;
    }

    // use to save the corresponding timestamp in the first frame of each clip
    ArrayList<Integer> bufferTimestampClip = new ArrayList<Integer>();
    // use to write timestamp on thumbnails
    ArrayList<Integer> bufferTimestamp = new ArrayList<Integer>();
    ArrayList<ImageIcon> bufferThumbnails = new ArrayList<ImageIcon>();
    
    /*******************************************************************************************************************
     * initialization
     ******************************************************************************************************************/

    int index = 0;
    public int frameNumb = 0;
    int nc = 0;
    public Message message;
    int prevTimestamp = 0;
    int previousTimestamp = 0;

    ProgressMonitor progressMonitor;

    protected void initialize() throws IOException {
        if (!batch)
            progressMonitor.setProgress(6);
        if(TTT.verbose){
	        // retrieve thumbnailWidth and thumbnailHeight to compute the Frame size
	        System.out.println("handle thumbnails");
        }
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

        // buffer index information
        for (int i = 0; i < recording.index.size(); i++) {
            IndexEntry ie = recording.index.get(i);
            bufferTimestamp.add(ie.getTimestamp());
            bufferTimestampClip.add(ie.getTimestamp());
            bufferThumbnails.add(ie.getThumbnail());
            if (html5_debug) {
                System.out.println("Index " + (i + 1) + ":\t" + Constants.getStringFromTime(ie.getTimestamp()));
            }
        }
    }
    
    /*******************************************************************************************************************
     * main processing loop
     ******************************************************************************************************************/

    private void createJson() throws IOException {
        try {
        	File html5dir = new File(recording.getDirectory() + HTML5_DIRECTORY);
        	if (!html5dir.isDirectory()) {
        		//create dir
        		html5dir.mkdir();
        	}
        	
        	//write file
        	String fileName = recording.getDirectory() + HTML5_DIRECTORY + "/" + FILE_NAME + ".js";
			FileWriter fstream = new FileWriter(fileName);
			this.out = new BufferedWriter(fstream);
        	
            if (!batch) {
                // show progress
                progressMonitor = new ProgressMonitor(TTT.getRootComponent(), "Convert to HTML5/Json", "Converting...", 0,
                        100);
                progressMonitor.setMillisToDecideToPopup(100);
                progressMonitor.setMillisToPopup(100);

                progressMonitor.setProgress(5);
                progressMonitor.setNote("initializing...");
            }

            this.initialize();
            
            //JSON with padding
            out.write("$(function(){controller.loadData({");
            
            //PREFS
            out.write("\"prefs\":"+this.recording.graphicsContext.prefs.toJson()+",");
            
            //THUMBNAILS
            if (!batch) {
            	if (progressMonitor == null || progressMonitor.isCanceled()) {
            		return;
            	}
                progressMonitor.setProgress(5);
                progressMonitor.setNote("writing thumbnails");
            }
            
            this.createJsonThumbnails();
            
            //MESSAGES
            if (!batch) {
            	if (progressMonitor == null || progressMonitor.isCanceled()) {
            		return;
            	}
                progressMonitor.setProgress(10);
                progressMonitor.setNote("converting messages");
            }
            
            out.write(", \"messages\": [");
            for (int i = 0; i < recording.messages.size(); i++) {
                if (!batch) {
                	if (progressMonitor.isCanceled()) {
                		return;
                	}
                	progressMonitor.setProgress(10 + (60 * i / recording.messages.size()));
                }
                    
                
                message = recording.messages.get(i);
                message.writeToJson(this);
                if (i < recording.messages.size()-1) {
                	out.write(",");
                }
            }
            out.write("]");
            
            //close padding
            out.write("})});");
            
            
            if (!batch) {
            	if (progressMonitor.isCanceled()) {
            		return;
            	}
                progressMonitor.setProgress(70);
                progressMonitor.setNote("copying audio files to player directory");
            }
            
            this.copyAudioFiles();
            
            if (!batch) {
            	if (progressMonitor.isCanceled()) {
            		return;
            	}
                progressMonitor.setProgress(100);
                progressMonitor.setNote("done");
            }
            
            
        } finally {
            // remove process monitor
            if (!batch && progressMonitor != null)
                progressMonitor.close();
            if (out != null)
            	out.close();
        }
    }
    
    private void copyAudioFiles() throws IOException {
    	//copy audio to html5 directory, if existent
        String[] audioFormatSuffix = {"mp3", "ogg"};
        for (int i = 0; i < audioFormatSuffix.length; i++) {
        	File target = new File(recording.getDirectory() + HTML5_DIRECTORY + "/" + FILE_NAME + "." + audioFormatSuffix[i]);
            if (recording.getExistingFileBySuffix(audioFormatSuffix[i]).exists() && !target.exists()) {
            	Html5PlayerCreator.copyFile(recording.getExistingFileBySuffix(audioFormatSuffix[i]), target);
            }
        }
    }
    
    public static byte[] imageToByte(Image image) {
        RenderedImage rendered = null;
        
        if (image instanceof RenderedImage) {
		    rendered = (RenderedImage)image;
		} else {
		    BufferedImage buffered = new BufferedImage(
		        image.getWidth(null),
		        image.getHeight(null),
		        BufferedImage.TYPE_INT_ARGB
		    );
		    Graphics2D g = buffered.createGraphics();
		    g.drawImage(image, 0, 0, null);
		    g.dispose();
		    rendered = buffered;
		}
    	
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(rendered, thumbnailImageFormat, byteArrayOutputStream);
            byte[] imageData = byteArrayOutputStream.toByteArray();
            return imageData;
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public void createJsonThumbnails() throws IOException {
    	out.write("\"thumbnails\": [");
    	for (int i = 0; i < bufferTimestamp.size(); i++) {
        	ImageIcon imageIcon = bufferThumbnails.get(i);
        	
        	if (i > 0) {
        		out.write(",");
        	}
        	
        	out.write("{");
        	out.write("\"time\": "+bufferTimestamp.get(i)+", ");
        	out.write("\"displayTime\": \""+Constants.getStringFromTime(bufferTimestamp.get(i), false)+"\", ");
        	out.write("\"width\": "+imageIcon.getIconWidth()+", ");
        	out.write("\"height\": "+imageIcon.getIconHeight()+", ");
        	out.write("\"data\": \"data:image/"+thumbnailImageFormat+";base64,");
           	out.write(Base64Coder.encode(imageToByte(imageIcon.getImage())));
        	out.write("\"}");
        }
    	out.write("]");
    }
    
    
    

    public static void main(String[] args) throws IOException {
        if (args.length == 0)
            System.out.println("TTT2Json generates Json messages from TTT input file(s)");
        else
            for (String arg : args) {
                System.out.println("\nopening ttt recording: " + arg);
                Recording recording = new Recording(arg, false);
                System.out.println();
                System.out.println("----------------------------------------------");
                System.out.println("TTT2Json Converter");
                System.out.println("----------------------------------------------");
                createJson(recording, true);
            }
        System.exit(0);
    }
}
