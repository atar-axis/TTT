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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;
import javax.swing.Timer;

/**
 * Creates MP4 podcast from recording object using <a href="http://ffmpeg.org">ffmpeg</a> and <a href="http://gpac.sourceforge.net">MP4Box</a>. <br>  
 * Created on 16. August 2009, 17:25
 * @author Christof Angermueller 
 * 
 */
public class PodcastCreator {

	private static final String RESOLUTION = "480x320";	//ouput resolution.
	private static final String FFMPEG = "ffmpeg";
	private static final String MP4BOX = "MP4Box";

	
	public static void main(String[] args) throws Exception {
		
		if (args.length == 0) {
			System.out.println("PodcastCreator recording");
			return;
		}
		Recording recording = new Recording(new File(args[0]), false);
		PodcastCreator.createPodcast(recording, true);
	}
	
	
	/**
	 * Checks whether it is possible to create a podcast.
	 * @param recording
	 * @return True if ffmpeg, MP4Box, and an audio file is available for creating a podcast.
	 * @throws IOException
	 */
	public static boolean isCreationPossible(Recording recording) throws IOException {
		return Exec.getCommand(FFMPEG) != null && Exec.getCommand(MP4BOX) != null && (recording.getExistingFileBySuffix("mp3").exists() || recording.getExistingFileBySuffix("wav").exists());
	}

	
	/**
	 * Creates podcast.
	 * @param recording
	 * @param batch
	 * @return True: Podcast created successfully.<br>False: Canceled by user.
	 * @throws Exception
	 */
	public static boolean createPodcast(Recording recording, boolean batch) throws Exception {

		long startTime = System.currentTimeMillis();
		System.out.println("Creating mp4 podcast");
		
		//Check whether the necessary applications are available.
		String ffmpegCmd = Exec.getCommand(FFMPEG);
		if (ffmpegCmd == null) {
			throw new IOException("ffmpeg not found");
		}
		String mp4BoxCmd = Exec.getCommand(MP4BOX);
		if (mp4BoxCmd == null) {
			throw new IOException("MP4Box not found");
		}
		//Create html script containing images for creating the podcast if not available.
		if (new File(recording.getDirectory() + recording.getFileBase() + ".html" + File.separator).exists() == false) {
			if (recording.createScript(ScriptCreator.HTML_SCRIPT, batch) == false) {
				return false;
			}
		}
		//Get audio file
		File audioFile = recording.getExistingFileBySuffix("wav");
		if (audioFile.exists() == false) {
			audioFile = recording.getExistingFileBySuffix("mp3");
			if (audioFile.exists() == false) {
				throw new IOException("No audio file found");
			}
		}
		
		//Initialization
		File outMovieFile = recording.getFileBySuffix("mp4");	//final output
		outMovieFile.delete();
		File outMovieTmpFile = recording.getFileBySuffix("tmp.mp4");	//temporary output for joined slide movies
		File slideMovieFile = File.createTempFile("tmpSlideMovie", ".mp4");	//slide movie created from png file
		long slideMovieLength;	//length of the slide movie
		Index index = recording.index;
		final ProgressMonitor progressMonitor = new ProgressMonitor(TTT.getRootComponent(), null, "building podcast from screenshots", 0, recording.getDuration()/1000*2);	//time per frame is roughly the same for video and audio encoding
		if (!batch) {
			progressMonitor.setMillisToDecideToPopup(0);
			progressMonitor.setMillisToPopup(0);
		}
		final Exec exec = new Exec();
		int j;
		
		//Video encoding
		for (int i = 0; i < index.size(); ++i) {	//loop through all slides
			System.out.println("Creating slide movie (" + (i+1) + "/" + index.size() + ")");
			if (!batch) {
				progressMonitor.setProgress(index.get(i).getTimestamp()/1000);
			}
			File imageFile = new File(recording.getDirectory() + recording.getFileBase() + ".html" + File.separator + "images" + File.separator + recording.getFileBase() + "." + ((i + 1) < 10 ? "0" : "") + (i + 1) + ".png");
			if (imageFile.exists() == false) {	//create png file from recording object if not available
				recording.setTime(index.get(i).getTimestamp());
				BufferedImage image = recording.graphicsContext.getScreenshotWithoutAnnotations();
				ImageIO.write(image, "png", imageFile);
			}
			//get length of the slide movie
			if (i == index.size() - 1) {
				slideMovieLength = recording.getDuration()-index.get(i).getTimestamp();
			} else {
				slideMovieLength = index.get(i + 1).getTimestamp()-index.get(i).getTimestamp();
			}
			slideMovieLength = Math.round((double) slideMovieLength / 1000);
			//create the slide movie using ffmpeg
			slideMovieFile.delete();
			exec.createListenerStream();
			j = exec.exec(new String[] {"ffmpeg", "-loop_input", "-r", "1", "-i", imageFile.getPath(), "-pix_fmt", "rgb24", "-vcodec", "mpeg4", "-vframes", String.valueOf(slideMovieLength), "-s", RESOLUTION,"-y", slideMovieFile.getPath()});
			if (j != 0 || slideMovieFile.length() == 0) {
				//error while creating the slide movie
				System.out.println("Unable to create slide movie using ffmpeg:");
				System.out.println(exec.getListenerStream());
				slideMovieFile.delete();
				outMovieFile.delete();
				throw new IOException("unable to create slide movie using ffmpeg");
			}
			if (!batch && progressMonitor.isCanceled()) {
				//canceled by user
				slideMovieFile.delete();
				outMovieFile.delete();
				progressMonitor.close();
				System.out.println("Canceled by user");
				slideMovieFile.delete();
				outMovieFile.delete();
				outMovieTmpFile.delete();
				return false;
			}
			//append the created slide movie (slideMovieFile) to the output file (outMovieFile) using MP4Box
			//appending slideMovieFile to outMovieFile directly via "MP4Box -cat slideMovieFile.getPath() outMovieFile.getPath()" causes renaming problems in some cases. Thus outMovieTmpFile is used.
			exec.createListenerStream();
			j = exec.exec(new String[] { MP4BOX, "-cat", slideMovieFile.getPath(), outMovieFile.getPath(), "-out", outMovieTmpFile.getPath()});
			if (j != 0 || outMovieTmpFile.length() == 0) {
				//error while appending the slideMovie to the output file
				System.out.println("Unable join slide movies using MP4Box:");
				System.out.println(exec.getListenerStream());
				outMovieFile.delete();
				throw new IOException("unable join slide movies using MP4Box");
			}
			//replace outMovieFile by outMovieFileTmp
			outMovieFile.delete();
			if (i < index.size() - 1) {
				outMovieTmpFile.renameTo(outMovieFile);
			}
		}
		slideMovieFile.delete();	//delete temporary file
		
		//Audio encoding with ffmpeg. The audio stream must be converted via aac to achieve ipod compatibility
		System.out.println("Adding audio stream to podcast");
		Timer timer = null;
		if (!batch) {			
			//The progress of the progress monitor is determined by the frame value ("frame= ") of the ffmpeg output
			progressMonitor.setNote("adding audio stream to podcast");		
			final int nFrames = recording.getDuration()/1000;
			timer = new Timer(1000, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (progressMonitor.isCanceled()) {
						exec.abort();
					}
					//Get the value after "frame=" of the last line
					String[] lines = exec.getListenerStream().toString().split("\n");
					Scanner scanner = new Scanner(lines[lines.length-1]);
					scanner.useDelimiter("[ ]+");
					if (scanner.findInLine("frame=") != null && scanner.hasNextInt()){
						int i = scanner.nextInt();
						System.out.println("Adding audio stream on frame (" + i + "/" + nFrames + ")");
						i+= nFrames;
						if (i < progressMonitor.getMaximum()) {
							progressMonitor.setProgress(i);
						}
					}
					exec.getListenerStream().reset();
				}
			});				
			timer.start();
		}
		exec.createListenerStream();
		j = exec.exec(new String[] {ffmpegCmd, "-i",audioFile.getPath(),"-i",outMovieTmpFile.getPath(),"-acodec","libfaac","-ab" ,"128" ,"-ar","44100","-vcodec","copy","-y", outMovieFile.getPath()});
		outMovieTmpFile.delete();	//delete temporary file
		if (!batch) {
			timer.stop();			
			if (progressMonitor.isCanceled()) {
				outMovieFile.delete();
				System.out.println("Canceled by user");
				return false;
			}
			progressMonitor.close();
		}
		if (j != 0 || outMovieFile.length() == 0) {
			//error while adding audio stream
			outMovieFile.delete();
			System.out.println("Unable add audio stream using ffmpeg:");
			System.out.println(exec.getListenerStream());
			throw new IOException("unable to add audio stream using ffmpeg");
		}
		System.out.println("Podcast created in " + Constants.getStringFromTime((int)(System.currentTimeMillis()-startTime)));
		return true;
	}	
}
