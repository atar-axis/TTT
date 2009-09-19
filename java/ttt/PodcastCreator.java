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


import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

	private static final int RESOLUTION_WIDTH = 480;
	private static final int RESOLUTION_HEIGTH = 320;
	private static final String FFMPEG = "ffmpeg";
	private static final String MP4BOX = "MP4Box";

	private static final double FRAMES_PER_SEC = 1;
	
	
	public static void main(String[] args) throws Exception {
		
		if (args.length == 0) {
			System.out.println("PodcastCreator recording");
			return;
		}
		Recording recording = new Recording(new File(args[0]), false);
		PodcastCreator.createPodcast(recording, true);
	}
	
	
	/**
	 * Checks whether it is possible to create a podcast
	 * @param recording
	 * @return True if ffmpeg, MP4Box, and an audio file is available for creating a podcast
	 * @throws IOException
	 */
	public static boolean isCreationPossible(Recording recording) throws IOException {
		return Exec.getCommand(FFMPEG) != null && Exec.getCommand(MP4BOX) != null && (recording.getExistingFileBySuffix(new String[] {"wav","mp3","mp2"}).exists());
	}

	/**
	 * Creates podcast using default parameters
	 * @param recording
	 * @param batch
	 * @return True: Podcast created successfully.<br>False: Canceled by user.
	 * @throws Exception
	 */
	public static boolean createPodcast(Recording recording, boolean batch) throws Exception {
		return createPodcast(recording, RESOLUTION_WIDTH, RESOLUTION_HEIGTH, FRAMES_PER_SEC, batch);
	}
	
	/**
	 * Creates podcast
	 * @param recording
	 * @param resolutionWidth Podcast width
	 * @param resolutionHeight Podcast heigth
	 * @param framesPerSec Frames per second
	 * @param batch
	 * @return True: Podcast created successfully.<br>False: Canceled by user
	 * @throws Exception
	 */
	public static boolean createPodcast(Recording recording, int resolutionWidth, int resolutionHeight, double framesPerSec, boolean batch) throws Exception {
		
		System.out.println("----------------------------------------------");
		System.out.println("PodcastCreator");
		System.out.println("----------------------------------------------");
		System.out.println("Creating mp4 podcast");
		
		//check whether the necessary applications are available
		String ffmpegCmd = Exec.getCommand(FFMPEG);
		if (ffmpegCmd == null) {
			throw new IOException("ffmpeg not found");
		}
		String mp4BoxCmd = Exec.getCommand(MP4BOX);
		if (mp4BoxCmd == null) {
			throw new IOException("MP4Box not found");
		}
		//get audio file
		File audioFile = recording.getExistingFileBySuffix(new String[] {"wav","mp3","mp2"});
		if (audioFile.exists() == false) {
			throw new IOException("No audio file found");
		}
		
		//initialization
		long startTime = System.currentTimeMillis();
		File outMovieFile = File.createTempFile("tmpOutMovie",".mp4");	//final output
		File outMovieTmpFile = File.createTempFile("tmpOutMovie",".mp4");	//temporary output for joined window movies
		File windowMovieFile = File.createTempFile("tmpWindowMovie", ".mp4");	//window movie created from png file		
		File windowImageFile = File.createTempFile("tmpWindowImage", ".png");	//image of window movie
		double frameLength = (double)1000/framesPerSec;
		double outMovieLength = 0;	//current length of outMovieFile
		int vFrames;	//number of video frames of window movie
		int i = 0;
		int j;
		
		final ProgressMonitor progressMonitor = new ProgressMonitor(TTT.getRootComponent(), null, "building podcast video stream", 0, recording.messages.size());	//time per frame is roughly the same for video and audio encoding
		final Exec exec = new Exec();
		System.out.println("Building podcast video stream from messages");
		recording.whiteOut();
		while (i < recording.messages.size()) {
			
			//draw all messages of the next frame
			while (i < recording.messages.size() && recording.messages.get(i).getTimestamp() - outMovieLength <= frameLength) {
				recording.deliverMessage(recording.messages.get(i++));
			}
			//the number of video frames depends on the timestamp of the succeeding message
			//if the next message occurs in x frames relative to the current frame, the next window lasts x-1 frames because nothing happens   
			if (i < recording.messages.size()) {
				vFrames = (int)((recording.messages.get(i).getTimestamp() - outMovieLength) / frameLength); 
			} else {
				vFrames = (int)((recording.getDuration() - outMovieLength) / frameLength);
				if (vFrames == 0) {
					vFrames = 1;
				}
			}
			outMovieLength += vFrames * frameLength;
			
			if (!batch && i < recording.messages.size()) {
				progressMonitor.setProgress(i);
			}
			System.out.println("   Message (" + i + "/" + recording.messages.size() + ")");
			
			//create window movie using ffmpeg
			//write scaled window image
			ImageIO.write(ImageCreator.getScaledInstance(recording.getGraphicsContext().getScreenshot(), resolutionWidth, resolutionHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true), "png", windowImageFile);
			windowMovieFile.delete();
			exec.createListenerStream();
			j = exec.exec(new String[] {"ffmpeg", "-loop_input", "-r", String.valueOf(framesPerSec), "-i", windowImageFile.getPath(), "-pix_fmt", "rgb24", "-vcodec", "mpeg4", "-vframes", String.valueOf(vFrames), "-s", resolutionWidth + "x" + resolutionHeight,"-y", windowMovieFile.getPath()});
			if (j != 0 || windowMovieFile.length() == 0) {
				//error while creating window movie
				windowMovieFile.delete();
				outMovieFile.delete();
				outMovieTmpFile.delete();
				windowImageFile.delete();
				System.out.println("Unable to create window movie using ffmpeg:");
				System.out.println(exec.getListenerStream());
				throw new IOException("unable to create window movie using ffmpeg");
			}
			if (!batch && progressMonitor.isCanceled()) {
				//canceled by user
				windowMovieFile.delete();
				outMovieFile.delete();
				outMovieTmpFile.delete();
				windowImageFile.delete();
				progressMonitor.close();
				System.out.println("Canceled by user");
				windowMovieFile.delete();
				outMovieFile.delete();
				outMovieTmpFile.delete();
				return false;
			}
			if (outMovieFile.length() == 0) {
				//the first window movie can renamed directly to output movie.
				//NOTE: MP4Box uses fps=1 for the container format when vFrames=1 whereby the container frame rate and codec frame rate can differ when using frameRate != 1. That causes a wrong synchronized video and audio stream
				windowMovieFile.renameTo(outMovieTmpFile);
			} else {
				//append the created window movie (windowMovieFile) to the output movie (outMovieFile) using MP4Box
				//NOTE: appending slideMovieFile to outMovieFile directly via "MP4Box -cat slideMovieFile.getPath() outMovieFile.getPath()" causes renaming problems in some cases. Thus outMovieTmpFile is used
				exec.createListenerStream();
				j = exec.exec(new String[] { MP4BOX, "-cat", windowMovieFile.getPath(), outMovieFile.getPath(), "-out", outMovieTmpFile.getPath()});
				if (j != 0 || outMovieTmpFile.length() == 0) {
					//error while appending the slideMovie to the output file
					windowMovieFile.delete();
					outMovieFile.delete();
					outMovieTmpFile.delete();
					windowImageFile.delete();
					System.out.println("Unable join slide movies using MP4Box:");
					System.out.println(exec.getListenerStream());
					throw new IOException("unable join slide movies using MP4Box");
				}				
			}
			//replace outMovieFile by outMovieFileTmp
			outMovieFile.delete();
			if (i < recording.messages.size()) {
				outMovieTmpFile.renameTo(outMovieFile);
			}
		}
		windowMovieFile.delete();
		outMovieFile.delete();
		windowImageFile.delete();
		
		//audio encoding with ffmpeg. The audio stream must be converted via aac to achieve ipod compatibility
		System.out.println("Adding audio stream to podcast");
		Timer timer = null;
		if (!batch) {			
			//the progress of the progress monitor is determined by the frame value ("frame= ") of the ffmpeg output
			final int nFrames = recording.getDuration()/1000;
			progressMonitor.setNote("adding audio stream to podcast");
			progressMonitor.setMaximum(nFrames);
			progressMonitor.setProgress(0);
			timer = new Timer(1000, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (progressMonitor.isCanceled()) {
						exec.abort();
					}
					//get the value after "frame=" of the last line
					String[] lines = exec.getListenerStream().toString().split("\n");
					Scanner scanner = new Scanner(lines[lines.length-1]);
					scanner.useDelimiter("[ ]+");
					if (scanner.findInLine("frame=") != null && scanner.hasNextInt()){
						int i = scanner.nextInt();
						System.out.println("   Frame (" + i + "/" + nFrames + ")");
						progressMonitor.setProgress(i);
					}
					exec.getListenerStream().reset();
				}
			});				
			timer.start();
		}
		exec.createListenerStream();
		outMovieFile = recording.getFileBySuffix("mp4");
		j = exec.exec(new String[] {ffmpegCmd,"-i",audioFile.getPath(),"-i",outMovieTmpFile.getPath(),"-acodec","libfaac","-ab" ,"128" ,"-ar","44100","-vcodec","copy","-y", outMovieFile.getPath()});
		outMovieTmpFile.delete();	
		if (!batch) {
			timer.stop();			
			if (progressMonitor.isCanceled()) {
				//canceled by user
				windowMovieFile.delete();
				outMovieFile.delete();
				outMovieTmpFile.delete();
				windowImageFile.delete();
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
		System.out.println("----------------------------------------------");
		return true;
	}	
	
} 
