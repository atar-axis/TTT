package ttt.editor2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.media.MediaLocator;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import ttt.editor2.processors.*;
import ttt.messages.Annotation;
import ttt.messages.CursorMessage;
import ttt.messages.DeleteAllAnnotation;
import ttt.messages.HextileMessage;
import ttt.messages.Message;
import ttt.messages.WhiteboardMessage;
import ttt.record.Recording;

/**
 * Class used to process TTT files and also their associated media files.
 */
public class TTTProcessor {

    /**
     * Sub divide an open TTT file, and any associated media files, given the <code>FileConnector</code> which
     * currently contains it and an original output file. This original file will have a number appended to its name
     * depending upon its position in the subdivision. Start and end times for each file created are obtained from the
     * <code>MarkerList</code> held in the <code>FileConnector</code>.
     * 
     * @param fileConnector
     *            the <code>FileConnector</code> containing the desktop data and media files
     * @param outputFile
     *            the desktop output file (other media files will be created as necessary)
     * @return <code>true</code> if the file sub division was successful, <code>false</code> otherwise
     */
//    public static boolean subDivide(FileConnector fileConnector, File outputFile) {
//
//        if (outputFile == null)
//            return false;
//
//        System.out.println("Beginning sub-divide...");
//
//        MarkerList markers = fileConnector.getMarkers();
////        String basicName = TTTFileUtilities.getBasicFileName(outputFile);
//
//        List<MarkerList.Marker> legalMarkers = markers.getLegalMarkers();
//
//        // use a number of trims
//        for (int i = 0; i < legalMarkers.size() - 1; i++) {
//            File file = new File(TTTFileUtilities.getBasicFileName(outputFile) + "-" + (i + 1)
//                    + Parameters.desktopEndings[0]);
//            trim(fileConnector, file, legalMarkers.get(i).getTimestamp(), legalMarkers.get(i + 1).getTimestamp(), true);
//        }
//
//        System.out.println("Subdivision complete.");
//        return true;
//    }

    /**
     * Trim an open TTT file, and any associated media files, given the <code>FileConnector</code> which currently
     * contains it and an output file. Start and end times are obtained from the <code>MarkerList</code> held in the
     * <code>FileConnector</code>.
     * 
     * @param fileConnector
     *            the <code>FileConnector</code> containing the desktop data and media files
     * @param outputFile
     *            the desktop output file (other media files will be created as necessary)
     * @return <code>true</code> if the file trim was successful, <code>false</code> otherwise
     */
//    public static boolean trim(FileConnector fileConnector, File outputFile) {
//        MarkerList markers = fileConnector.getMarkers();
//        int startTimeMS = markers.getTrimStartTime();
//        int endTimeMS = markers.getTrimEndTime();
//        return trim(fileConnector, outputFile, startTimeMS, endTimeMS, false);
//    }

    /**
     * Trim an open TTT file, and any associated media files, given the <code>FileConnector</code> which currently
     * contains it, an output file, and start and end times (in milliseconds).
     * 
     * @param fileConnector
     *            the <code>FileConnector</code> containing the desktop data and media files
     * @param outputFile
     *            the desktop output file (other media files will be created as necessary)
     * @param startTimeMS
     *            the trim start time in milliseconds
     * @param endTimeMS
     *            the trim end time in milliseconds
     * @param forSubdivide
     *            the trim is being called for subdivide - the difference is that when subdividing,
     * @return <code>true</code> if the file trim was successful, <code>false</code> otherwise
     * @throws IOException 
     */
    public static boolean trim(Recording recording, File outputFile, int startTimeMS, int endTimeMS,
            boolean forSubdivide) throws IOException {

        if (outputFile == null)
            return false;

        System.out.println("Beginning trim...");
        System.out.println("Start time:\t" + Editor2.getStringFromTime(startTimeMS, true));
        System.out.println("End time:\t" + Editor2.getStringFromTime(endTimeMS, true));
        System.out.println();

        System.out.println("Saving video...");

        // modified by Ziewer 30.03.2006
        // use real video cutting timestamps (better sync)
        cuttedStart = startTimeMS * 1000000l;
        cuttedEnd = endTimeMS * 1000000l;

        // trim video
        boolean videoSuccessful = false;
        
        
        
      
        if (recording.getVideoFilename() != null) {
        	  File videoFile = new File(recording.getDirectory() + recording.getVideoFilename());
            File videoOutput = new File(TTTFileUtilities.getBasicFileName(outputFile) + Parameters.videoEndings[0]);
            if (videoOutput.exists() && Parameters.createBackups)
                TTTFileUtilities.renameForBackup(videoOutput);
            videoSuccessful = cutVideo(videoFile, videoOutput, cuttedStart, cuttedEnd);

            // modified by Ziewer 30.03.2006
            // use real video cutting timestamps (better sync)
            if (videoSuccessful) {
                System.out.println("\nAdjusting to video frames:");
                System.out.println("start:\t" + Editor2.getStringFromTime(cuttedStart, true) + "\t("
                        + Editor2.getStringFromTime(cuttedStart - startTimeMS * 1000000l, true) + ")");
                System.out.println("end:\t" + Editor2.getStringFromTime(cuttedEnd, true) + "\t("
                        + Editor2.getStringFromTime(cuttedEnd - endTimeMS * 1000000l, true) + ")");
                System.out.println("diff:\t" + Editor2.getStringFromTime(cuttedEnd - cuttedStart, true));
                System.out.println();
            } else {
                cuttedStart = startTimeMS;
                cuttedEnd = endTimeMS;
            }
        }

        System.out.println("Saving audio...");

        // trim audio
        boolean audioSuccessful = false;
        
        if (recording.getAudioFilename() != null) {
        	File audioFile = new File(recording.getAudioFilename());
            // ADDED 25.10.2007 by Ziewer
            // NOTE: also change in FileConnector.correctFiles()
            // if available, use same encoding as source 
            String ending = Parameters.audioEndings[0];
            for (String audioEncoding : Parameters.audioEndings) {
                if(audioFile.getName().endsWith(audioEncoding)) {
                        ending=audioEncoding;
                        break;
                }
            }
            File audioOutput = new File(TTTFileUtilities.getBasicFileName(outputFile) + ending);
            
            if (audioOutput.exists() && Parameters.createBackups)
                TTTFileUtilities.renameForBackup(audioOutput);

            // modified by Ziewer 30.03.2006
            // use real video cutting timestamps (better sync)
            audioSuccessful = cutAudio(audioFile, audioOutput, cuttedStart, cuttedEnd);
        }

        System.out.println("Saving desktop...");

        //TODO stuff
        // trim desktop
        //TTTFileData fileData = fileConnector.getFileData();
        boolean desktopSuccessful = false;
        if (outputFile.exists() && Parameters.createBackups)
            TTTFileUtilities.renameForBackup(outputFile);
		
        desktopSuccessful = cutDesktop(recording, outputFile, endTimeMS, startTimeMS);

        System.out.println("Done!");

        // modified by Ziewer 30.03.2006
        // return true
        return audioSuccessful && videoSuccessful && desktopSuccessful;
    }
    
    
    private static boolean cutDesktop(Recording recording, File outputFile, int endTimeMS, int startTimeMS){
    	
    	//get Messages as an ArrayList
		ArrayList<Message> messages = recording.getMessages().getMessages();

		
		
		
		ArrayList<Message> toDelete = new ArrayList<Message>();
		//Save the first and the last message 
		Message first = messages.get(0);
		Message last = messages.get(messages.size() - 1);		

		//Prepare to save 'first picture' Messages
		Message firstWhiteboardMessage = null;
		Message firstCursorMessage = null;
		ArrayList<Message> annotations = new ArrayList<Message>();

		//for of doom <-<
		for (int i = 0; i < messages.size(); i++) {
			Message m = messages.get(i);
			//Handling of messages that happen before the new beginning
			if (m.getTimestamp() < startTimeMS) {
				//save the most recent 'background'
				if (m instanceof HextileMessage || m instanceof WhiteboardMessage) {
					firstWhiteboardMessage = m;
				}
				//save the most recent cursor position
				else if (m instanceof CursorMessage) {
					firstCursorMessage = m;
				}
				//save annotations. TODO currently are all annotations saved. even unneeded ones. find a way to remove them
				else if (m instanceof Annotation) {
					toDelete.add(m);
					//TODO
//					annotations.add(m);
					messages.get(i).setTimestamp(
							messages.get(i).getTimestamp() - startTimeMS);
			//		if delete all happens we can throw all gathered annotations aways
					if(m instanceof DeleteAllAnnotation){
						toDelete.addAll(annotations);
						annotations = new ArrayList<Message>();
					}
					
				} 
				// delete everything 			
					toDelete.add(m);
				
			} else if(m.getTimestamp() > endTimeMS){
				//messages after the new end end can simply be thrown away
				toDelete.add(m);
			} 
			//if the message is inside the range of the new start and end, calculate it's new timestamp
			else {
				messages.get(i).setTimestamp(messages.get(i).getTimestamp() - startTimeMS);
			}
		}
		
		//remove messages marked for deletion
		for (int i = 0; i < toDelete.size(); i++) {
			messages.remove(toDelete.get(i));
		}
		
		//correct the timestamp of the saved messages
		first.setTimestamp(0);
		last.setTimestamp(endTimeMS-startTimeMS);
		messages.add(0, first);
		
		//add the saved messages
		if(firstWhiteboardMessage != null){
			firstWhiteboardMessage.setTimestamp(0);
			messages.add(1, firstWhiteboardMessage);
			if (firstCursorMessage !=null){ 
				firstCursorMessage.setTimestamp(0);
				messages.add(2, firstCursorMessage);
			}
		}
		
		messages.add(last);
		
		recording.setMessages(messages);
		
		for(Message m : messages){
			System.out.println("what's going on? " +  Editor2.getStringFromTime( m.getTimestamp()) + " . damn " + m.toString());
		}
		
		
		recording.index.computeIndex();
		
		//save the new desktopFile
		return recording.store(outputFile);
	}
    
    
    
//
//    // added by Ziewer 30.03.2006
    // use real cutting timestamps allows better synchronization
    private static long cuttedStart;
    private static long cuttedEnd;

    private static boolean cutVideo(File inputFile, File outputFile, long startTimeNanoSec, long endTimeNanoSec) {
        // cut video
        if (inputFile != null) {
            MediaLocator videoInputLocator = new MediaLocator("file:/" + inputFile.getAbsolutePath());
            MediaLocator videoOutputLocator = new MediaLocator("file:/" + outputFile.getAbsolutePath());

            Cut cut = new Cut();

            long[] startNanoS = { startTimeNanoSec };
            long[] endNanoS = { endTimeNanoSec };

            boolean result = cut.doIt(videoInputLocator, videoOutputLocator, startNanoS, endNanoS, false);

            // keep real cutting timestamps
            cuttedStart = cut.realStart;
            cuttedEnd = cut.realEnd;

            return result;
        }
        return false;
    }

    private static boolean cutAudio(File inputFile, File outputFile, long startTimeNanoSec, long endTimeNanoSec) {
        // cut audio
        if (inputFile != null) {
            MediaLocator audioInputLocator = new MediaLocator("file:/" + inputFile.getAbsolutePath());
            MediaLocator audioOutputLocator = new MediaLocator("file:/" + outputFile.getAbsolutePath());
            Cut cut = new Cut();

            long[] startNanoS = { startTimeNanoSec };
            long[] endNanoS = { endTimeNanoSec };

            return cut.doIt(audioInputLocator, audioOutputLocator, startNanoS, endNanoS, false);
        }
        return false;
    }

    // removes start / end of desktop file, and then writes the trimmed file
//    private static boolean trimDesktop(TTTFileData fileData, File outputFile, long startTimeNanoSec, long endTimeNanoSec)
//            throws Exception {
//
//        int startTimeMS = (int) (startTimeNanoSec / 1000000);
//        int endTimeMS = (int) (endTimeNanoSec / 1000000);
//
//        IndexEntry first = fileData.index.insertIndex(startTimeMS);
//        if (first == null)
//            first = fileData.index.get(fileData.index.getIndexFromTime(startTimeMS));
//
//        IndexEntry last = null;
//
//        if (endTimeMS < fileData.index.getLastMessageTimestamp()) {
//            last = fileData.index.insertIndex(endTimeMS);
//            if (last == null)
//                last = fileData.index.get(fileData.index.getIndexFromTime(endTimeMS));
//        }
//
//        fileData.index.deleteFirstIndexesCompletely(first);
//        if (last != null)
//            fileData.index.deleteLastIndexesCompletely(last);
//
//        // added by Ziewer 30.03.2006
//        // add final DeleteAll annotation with final timestamp
//        fileData.index.addFinalTimestamp(endTimeMS - startTimeMS);
//
//        TTTFileWriter.writeFile(fileData, outputFile);
//
//        return true;
//    }
//
//    // writes a desktop file trimmed with the specified times, but without changing the source file
    // so that it can still be used for further subdivisions
//    private static boolean trimDesktopForSubdivide(TTTFileData fileData, File outputFile, long startTimeNanoSec,
//            long endTimeNanoSec) throws Exception {
//
//        int startTimeMS = (int) (startTimeNanoSec / 1000000);
//        int endTimeMS = (int) (endTimeNanoSec / 1000000);
//
//        OutputStream out_raw;
//        try {
//            out_raw = new FileOutputStream(outputFile);
//        } catch (Exception e) {
//            System.out.println("Cannot create output file.");
//            return false;
//        }
//        DataOutputStream out = new DataOutputStream(out_raw);
//
//        // write header
//        fileData.header.writeVersionMessageToOutputStream(out);
//        out = new DataOutputStream(new DeflaterOutputStream(out_raw));
//        fileData.header.writeServerInitToOutputStream(out);
//
//        // insert an index at the start time (nothing will happen if already there)
//        // Means that index of created file will be correct, with thumbnail if required
//        fileData.index.insertIndex(startTimeMS);
//
//        IndexEntry startEntry = fileData.index.get(fileData.index.getIndexFromTime(startTimeMS));
//
//        // get extension
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        DataOutputStream buffer = new DataOutputStream(byteArrayOutputStream);
//        int entryCount = fileData.index.getIndexExtensionDataForBuffer(buffer, startEntry, endTimeMS, true);
//
//        // length (adding on length of extension tag and count)
//        out.writeInt(byteArrayOutputStream.size() + 3);
//        // write encoding (adding on length of extension tag and count)
//        out.writeByte(ProtocolConstants.EXTENSION_INDEX_TABLE);
//        // number of extensions
//        out.writeShort(entryCount);
//        // extension data
//        out.write(byteArrayOutputStream.toByteArray());
//
//        // do not write any extensions except index extension,
//        // because uncertain what they would do - might mess things up
//        // if an extension for one file is put in and not for the others
//        out.writeInt(0);
//
//        // write start time
//        out.writeLong(fileData.header.startTime + startTimeMS);
//
//        // write messages
//        fileData.index.writeMessages(out, startEntry, endTimeMS);
//
//        out.close();
//
//        return true;
//    }
//
    /**
     * Gets a list of files to use for concatenation.
     * 
     * @return array of files to be concatenated
     */
    public static File[] getFilesForConcat() {
        ConcatSelector selector = new ConcatSelector();

        int selection = JOptionPane.showInternalOptionDialog(Editor2.getInstance().getDesktopPane(), selector,
                "Choose files to concatenate", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null,
                null);

        if (selection == JOptionPane.CANCEL_OPTION)
            return null;

        return selector.getSelectedInput();
    }

    /**
     * Allows a number of TTT files, and associated media files, to be concatenated.
     * 
     * @param desktopFiles
     *            the TTT files to be concatenated
     * @return <code>true</code> if the file concatenation was successful, <code>false</code> otherwise
     */
    public static boolean concat(File[] desktopFiles) throws Exception {

        System.out.println("Beginning concat...");
        File[] videoFiles = new File[desktopFiles.length];
        File[] audioFiles = new File[desktopFiles.length];
        MediaLocator[] videoLocators = new MediaLocator[desktopFiles.length];
        MediaLocator[] audioLocators = new MediaLocator[desktopFiles.length];
        boolean cannotConcatAudio = false;
        boolean cannotConcatVideo = false;
        System.out.println("Searching for media files");
        for (int i = 0; i < desktopFiles.length; i++) {

            videoFiles[i] = TTTFileUtilities.checkForVideo(desktopFiles[i]);
            if (videoFiles[i] != null)
                videoLocators[i] = new MediaLocator("file:/" + videoFiles[i].getAbsolutePath());
            if (videoLocators[i] == null)
                cannotConcatVideo = true;

            audioFiles[i] = TTTFileUtilities.checkForAudio(desktopFiles[i]);
            if (audioFiles[i] != null)
                audioLocators[i] = new MediaLocator("file:/" + audioFiles[i].getAbsolutePath());
            if (audioLocators[i] == null)
                cannotConcatAudio = true;

        }
        if (cannotConcatVideo)
            System.out.println("Video files missing.  Video will be ignored.");
        if (cannotConcatAudio)
            System.out.println("Audio files missing.  Audio will be ignored.");

        File outputFile = getOutputFileName();
        if (outputFile == null)
            return false;

        File videoOutput = new File(TTTFileUtilities.getBasicFileName(outputFile) + Parameters.videoEndings[0]);
        if (videoOutput.exists() && Parameters.createBackups)
            TTTFileUtilities.renameForBackup(videoOutput);
        MediaLocator videoOutputLocator = new MediaLocator("file:/" + videoOutput.getAbsolutePath());

		File audioOutput = new File(TTTFileUtilities.getBasicFileName(outputFile) + Parameters.audioEndings[1]);
        if (audioOutput.exists() && Parameters.createBackups)
            TTTFileUtilities.renameForBackup(audioOutput);


        boolean audioSuccess = false;
        boolean videoSuccess = false;

        long audioDurations[] = null;
        long videoDurations[] = null;
		WavConcat audioConcat = null;
        
		
		try{
		 audioConcat = new WavConcat(audioFiles, audioOutput);
        }catch(NullPointerException e){
        	cannotConcatAudio = true;
        }

        Concat videoConcat = new Concat();
        if (!cannotConcatVideo) {
            System.out.println("Concatenating video");
            if (videoConcat.createProcessors(videoLocators, videoOutputLocator))
                videoDurations = videoConcat.getDurationNanoSeconds();
        }
        if (!cannotConcatAudio) {
            System.out.println("Concatenating audio");
			if (audioConcat.doIt()) {
                audioDurations = audioConcat.getDurationNanoSeconds();
				audioSuccess = true;
			}
        }

        long[] durations = null;
        if (audioDurations != null && videoDurations != null) {
            durations = new long[audioDurations.length];
            for (int i = 0; i < durations.length; i++) {
                durations[i] = Math.min(videoDurations[i], audioDurations[i]);
            }
        } else if (audioDurations != null)
            durations = audioDurations;
        else if (videoDurations != null)
            durations = videoDurations;

		//        if (!cannotConcatAudio) {
		//            audioSuccess = audioConcat.doIt();
		//        }

        if (!cannotConcatVideo) {
            videoSuccess = videoConcat.doIt();
        }

        boolean desktopSuccessful = false;
        if (outputFile.exists() && Parameters.createBackups)
            TTTFileUtilities.renameForBackup(outputFile);
        try {
            desktopSuccessful = concatDesktop(desktopFiles, outputFile, durations);
        } catch (Exception e) {
            System.out.println("Unable to concatenate desktop." + e);
            throw e;
        }

        System.out.println("Desktop successful:    " + desktopSuccessful + "\nVideo successful:  " + videoSuccess
                + "\nAudio successful:  " + audioSuccess);

        return true;
    }

    private static boolean concatDesktop(File[] desktopFiles, File outputFile, long[] mediaDurations) throws Exception {

    	Recording FinalRecord;
    	
    	//saves all the messages and extenions
    	 ArrayList<Message> finalMessages = new ArrayList<Message>();
    	 ArrayList<byte[]> finalExtensions = new ArrayList<byte[]>();
    	 //used to correct the timestamp
    	 int duration = 0;
    	Recording[] placeholderRecording = new Recording[desktopFiles.length];
    	
    	
        for (int i = 0; i < desktopFiles.length; i++) {
            try {
            	placeholderRecording[i] = new Recording(desktopFiles[i]);
            	if (duration != 0){
            		System.out.println("Calculating new timestamps for the " + (i+1) + "th record");
            		for(int j = 0;placeholderRecording[i].getMessages().getMessages().size() > j;j++){
            			placeholderRecording[i].getMessages().get(j).setTimestamp(
            					duration +	placeholderRecording[i].getMessages().getMessages().get(j).getTimestamp());
            			}
            	}
            	//finalExtensions.addAll(placeholderRecording[i].getExtensions());
            	finalMessages.addAll(placeholderRecording[i].getMessages().getMessages());
		System.out.println("Extended Desktop recording by "+ (placeholderRecording[i].getMessages().get(placeholderRecording[i].getMessages().size()-1).getTimestamp())/60000l +" minutes");
            	
            	duration = (placeholderRecording[i].getMessages().get(placeholderRecording[i].getMessages().size()-1).getTimestamp());
    
            } catch (Exception e) {
                System.out.println("Error reading file: " + e);
                return false;
            }
        }
        
       FinalRecord = placeholderRecording[0];
       FinalRecord.resetIndex();
       FinalRecord.setMessages(finalMessages);
       FinalRecord.setfileDesktop(outputFile);
       FinalRecord.setExtensions(finalExtensions);
       FinalRecord.store();
        
        // compare headers
//        Header[] headers = new Header[desktopData.length];
//        for (int i = 0; i < desktopData.length; i++)
//            headers[i] = desktopData[i].header;
//        if (!compareHeaders(headers)) {
//            System.out.println("!Cannnot concatenate files: headers are incompatible");
//            return false;
//        }

//        OutputStream out_raw;
//        try {
//            out_raw = new FileOutputStream(outputFile);
//        } catch (Exception e) {
//            System.out.println("Cannot create output file.");
//            return false;
//        }
//        DataOutputStream out = new DataOutputStream(out_raw);

        // write header of first file
      //  headers[0].writeVersionMessageToOutputStream(out);
     //   out = new DataOutputStream(new DeflaterOutputStream(out_raw));
    //    headers[0].writeServerInitToOutputStream(out);

        // write index extension

        // if any one file does not have thumbnails, do not include any thumbs
//        boolean outputThumbs = true;
//        for (int i = 0; i < desktopData.length; i++) {
//            if (!desktopData[i].index.thumbnailsAvailable()) {
//                outputThumbs = false;
//                break;
//            }
//        }
//        int entryCount = 0;
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        DataOutputStream buffer = new DataOutputStream(byteArrayOutputStream);
        // use offset to adjust timestamps to cope with a new position in a longer file
//        int offset = 0;
//        for (int i = 0; i < desktopData.length; i++) {
//            entryCount += desktopData[i].index.getIndexExtensionDataForBuffer(buffer, offset, outputThumbs);
//            if (mediaDurations != null)
//                offset += Math.max(desktopData[i].index.getLastMessageTimestampWithoutSync(),
//                        (int) (mediaDurations[i] / 1000));
//            else
//                offset += desktopData[i].index.getLastMessageTimestampWithoutSync();
//        }

//        // length
//        out.writeInt(byteArrayOutputStream.size() + 3);
//        // write encoding
//        out.writeByte(ProtocolConstants.EXTENSION_INDEX_TABLE);
//        // number of entries
//        out.writeShort(entryCount);
//        // extension data
//        out.write(byteArrayOutputStream.toByteArray());

        // do not write any extensions except index extension,
        // because uncertain what they would do - it might mess things up
        // if an extension for one file is put in and not for the others
//        out.writeInt(0);
//
//        // write start time
//        out.writeLong(headers[0].startTime);
//
//        // write messages
//        offset = 0;
//        for (int i = 0; i < desktopData.length; i++) {
//            // added by Peter Ziewer in 22.05.2009
//            // deactivate whiteboard at concat points
//            if (i>0) {
//                (new BlankPageMessage(offset, ProtocolConstants.EncodingBlankPage, 0, headers[i])).writeMessage(out);
//            }
//            // end added
//            desktopData[i].index.writeAllMessages(out, offset);
//            if (mediaDurations != null)
//                offset += Math.max(desktopData[i].index.getLastMessageTimestampWithoutSync(),
//                        (int) (mediaDurations[i] / 1000));
//            else
//                offset += desktopData[i].index.getLastMessageTimestampWithoutSync();
//        }
//
//        out.close();

        return true;
    }

    
    
  
    
    
    /**
     * Helper method which compares two headers to see if they contain compatible data. This is useful in determining
     * whether two files may be successfully concatenated.<br>
     * NOTE:- This method is strict, and will return false if the framebuffer dimensions are even slightly different.
     * 
     * @param headers
     *            an array of <code>Header</code>s
     * @return <code>true</code> if the headers are similar, <code>false</code> otherwise
     */
//    public static boolean compareHeaders(Header[] headers) {
//        // if less than 2 headers, cannot be incompatible
//        if (headers.length < 2)
//            return true;
//
//        // consider important elements of header
//        // should consider fewer elements...?
//        Header header1 = headers[0];
//        for (int i = 1; i < headers.length; i++) {
//            if (header1.framebufferHeight != headers[i].framebufferHeight
//                    || header1.framebufferWidth != headers[i].framebufferWidth
//                    || header1.bitsPerPixel != headers[i].bitsPerPixel || header1.bigEndian != headers[i].bigEndian
//                    || header1.trueColour != headers[i].trueColour || header1.redMax != headers[i].redMax
//                    || header1.greenMax != headers[i].greenMax || header1.blueMax != headers[i].blueMax
//                    || header1.redShift != headers[i].redShift || header1.greenShift != headers[i].greenShift
//                    || header1.blueShift != headers[i].blueShift || !header1.colorModel.equals(headers[i].colorModel))
//                return false;
//        }
//        // headers are similar
//        return true;
//    }

    // SHOULD CHECK FOR FILE OVERWRITE?
    private static File getOutputFileName() {

        File newFile = TTTFileUtilities.showSaveFileInternalDialog();
        if (newFile == null)
            return null;

        // make sure the file ends with the proper suffix
        String fileName = newFile.getAbsolutePath();
        if (!fileName.endsWith(Parameters.desktopEndings[0]))
            fileName += Parameters.desktopEndings[0];
        File file = new File(fileName);
        return file;
    }

    // panel which allows user to select files to concatenate
    private static class ConcatSelector extends JPanel implements ListCellRenderer, ListDataListener {

        JList selectedList;
        JList activeList;
        DefaultListModel activeListModel = new DefaultListModel();
        DefaultListModel selectedListModel = new DefaultListModel();

        String instructionText = "Concatenate";

        Border noBorder = new EmptyBorder(1, 1, 1, 1);

        ConcatSelector() {
            super();

            BorderLayout layout = new BorderLayout();
            layout.setVgap(10);
            setLayout(layout);

            activeListModel.addListDataListener(this);
            selectedListModel.addListDataListener(this);

            JPanel mainPanel = new JPanel(new BorderLayout());

            Dimension listSize = new Dimension(300, 300);

            Component buttonBox = createButtonBox();

            Component activePanel = createActiveFilePanel();
            activePanel.setPreferredSize(listSize);

            Component selectedPanel = createSelectedFilePanel();
            selectedPanel.setPreferredSize(listSize);

            mainPanel.add(activePanel, BorderLayout.WEST);
            mainPanel.add(buttonBox, BorderLayout.CENTER);
            mainPanel.add(selectedPanel, BorderLayout.EAST);

            JLabel instructionLabel = new JLabel(instructionText);
            add(instructionLabel, BorderLayout.NORTH);
            add(mainPanel, BorderLayout.CENTER);
        }

        private JPanel createActiveFilePanel() {
            JPanel activePanel = new JPanel(new BorderLayout());
            activePanel.setBorder(new TitledBorder("Previously selected files"));

            activeList = new JList(activeListModel);
            activeList.setCellRenderer(this);

            JScrollPane activeScrollPane = new JScrollPane();
            activeScrollPane.setViewportView(activeList);
            activePanel.add(activeScrollPane);

            return activePanel;
        }

        JButton addButton = new JButton("Add >>>");
        JButton removeButton = new JButton("<<< Remove");
        JButton upButton = new JButton("Move up");
        JButton downButton = new JButton("Move down");

        private Component createButtonBox() {
            Box buttonBox = new Box(BoxLayout.Y_AXIS);
            buttonBox.add(Box.createGlue());

            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    switchBetweenLists(activeList, selectedList);
                }
            });
            buttonBox.add(addButton);
            addButton.setEnabled(false); // nothing initially in active list before initialization, so nothing can be
            // added
            addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonBox.add(Box.createVerticalStrut(10));

            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    switchBetweenLists(selectedList, activeList);
                }
            });
            buttonBox.add(removeButton);
            removeButton.setEnabled(false); // nothing initially in selected list, so nothing can be removed
            removeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonBox.add(Box.createGlue());

            JButton searchButton = new JButton("Search...");
            searchButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    File file = TTTFileUtilities.showOpenFileInternalDialog();
                    if (file == null)
                        return;
                    if (!selectedListModel.contains(file)) {
                        selectedListModel.addElement(file);
                        selectedList.setSelectedIndex(selectedListModel.size() - 1);
                        if (activeListModel.contains(file))
                            activeListModel.removeElement(file);
                    }
                }
            });
            buttonBox.add(searchButton);
            searchButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonBox.add(Box.createGlue());

            upButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    int index = selectedList.getSelectedIndex();
                    // do nothing if no index selected, or top index selected
                    if (index < 1)
                        return;
                    Object object = selectedListModel.getElementAt(index);
                    selectedListModel.setElementAt(selectedListModel.getElementAt(index - 1), index);
                    selectedListModel.setElementAt(object, index - 1);
                    selectedList.setSelectedIndex(index - 1);
                }
            });
            buttonBox.add(upButton);
            upButton.setEnabled(false); // nothing in list to be moved
            upButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonBox.add(Box.createVerticalStrut(10));

            downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    int index = selectedList.getSelectedIndex();
                    // do nothing if no index selected, or bottom index selected
                    if (index < 0 || index >= selectedListModel.size() - 1)
                        return;
                    Object object = selectedListModel.getElementAt(index);
                    selectedListModel.setElementAt(selectedListModel.getElementAt(index + 1), index);
                    selectedListModel.setElementAt(object, index + 1);
                    selectedList.setSelectedIndex(index + 1);
                }
            });
            buttonBox.add(downButton);
            downButton.setEnabled(false); // nothing in list to be moved
            downButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonBox.add(Box.createGlue());

            return buttonBox;
        }

        private void switchBetweenLists(JList sourceList, JList destList) {
            int index = sourceList.getSelectedIndex();
            if (index < 0)
                return;
            DefaultListModel sourceListModel = (DefaultListModel) sourceList.getModel();
            DefaultListModel destListModel = (DefaultListModel) destList.getModel();

            Object object = sourceListModel.remove(index);
            // highlight next entry in the list if available
            if (sourceListModel.size() > 0)
                if (index < sourceListModel.size())
                    sourceList.setSelectedIndex(index);
                else
                    sourceList.setSelectedIndex(sourceListModel.size() - 1);
            // add entry to selected list and highlight it
            destListModel.addElement(object);
            destList.setSelectedIndex(destListModel.size() - 1);
        }

        // used because general toString() method of File
        // returns a string which is too long
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {

            File file = (File) list.getModel().getElementAt(index);

            JLabel label = new JLabel(file.getName());
            label.setOpaque(true);
            label.setEnabled(list.isEnabled());
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }

            label.setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : noBorder);

            return label;
        }

        public void intervalAdded(ListDataEvent event) {
            if (event.getSource() == activeListModel) {
                if (activeListModel.size() == 1) {
                    addButton.setEnabled(true);
                }
            } else {
                if (selectedListModel.size() == 1) {
                    removeButton.setEnabled(true);
                    upButton.setEnabled(true);
                    downButton.setEnabled(true);
                }
            }
        }

        public void intervalRemoved(ListDataEvent event) {
            if (event.getSource() == activeListModel) {
                if (activeListModel.size() < 1) {
                    addButton.setEnabled(false);
                }
            } else {
                if (selectedListModel.size() < 1) {
                    removeButton.setEnabled(false);
                    upButton.setEnabled(false);
                    downButton.setEnabled(false);
                }
            }
        }

        public void contentsChanged(ListDataEvent event) {
        }

        private JPanel createSelectedFilePanel() {
            JPanel selectedPanel = new JPanel(new BorderLayout());
            selectedPanel.setBorder(new TitledBorder("Selected files"));
            selectedList = new JList(selectedListModel);
            selectedList.setCellRenderer(this);

            JScrollPane selectedScrollPane = new JScrollPane();
            selectedScrollPane.setViewportView(selectedList);

            selectedPanel.add(selectedScrollPane, BorderLayout.CENTER);
            return selectedPanel;
        }

        File[] getSelectedInput() {
            Object[] objects = selectedListModel.toArray();
            File[] files = new File[objects.length];
            for (int i = 0; i < objects.length; i++)
                files[i] = (File) objects[i];
            return files;
        }
    }

}
