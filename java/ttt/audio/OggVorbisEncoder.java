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


package ttt.audio;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.ProgressMonitor;
import javax.swing.Timer;

import ttt.Constants;
import ttt.TTT;


/**
 * Helper class for using the ogg vorbis encoder via system calls, based on LameEncoder
 * Created on 03-23-2012
 * @author Manuel Thurner
 */
public class OggVorbisEncoder {

	private static final String OGGVORBIS = "oggenc";	
	
	public static void main(String[] args) throws Exception{		
		
		String[] oggVorbisArgs = Arrays.copyOf(args, 3);
		if (oggVorbisArgs[0] == null || oggVorbisArgs[1] == null) {
			System.out.println("OggVorbisEncoder inputFile outputFile [options]");
			return;
		}
		OggVorbisEncoder.convertAudioFile(new File(oggVorbisArgs[0]),new File(oggVorbisArgs[1]),oggVorbisArgs[2] == null ? "" : oggVorbisArgs[2],true);
	}
	
	
	/**Checks whether oggVorbis is available
	 * @return	True if oggVorbis is available
	 * */
	public static boolean isOggVorbisAvailable() {
		return Exec.getCommand(OGGVORBIS) != null;
	}
	
	
	/**Allows converting audio files using oggenc
	 * @return True: Conversion succeeded.<br>False: Canceled by user 
	 */
	public static boolean convertAudioFile(File inFile, File outFile, String options, boolean batch) throws Exception {
		if(TTT.verbose){
		System.out.println("----------------------------------------------");
		System.out.println("OggEncoder");
		System.out.println("----------------------------------------------");
		System.out.println("Encoding oggvorbis file");
		}
		long startTime = System.currentTimeMillis();	//time measurement
		String oggencCmd = Exec.getCommand(OGGVORBIS);	//get oggenc command
		if (oggencCmd == null) {
			throw new IOException("oggenc not found");
		}				
		outFile.delete();	//delete outFile in order to test success after encoding
		
		final Exec exec = new Exec();
		int i;
		if (!batch) {
			final ProgressMonitor progressMonitor = new ProgressMonitor(TTT.getRootComponent(),null,"encoding ogg vorbis file",0,100);
			progressMonitor.setMillisToDecideToPopup(100);
			progressMonitor.setMillisToPopup(100);
			//the progress depends on the current outFile size in comparison to the expected outFile size
			progressMonitor.setMaximum((int)(inFile.length()>>10)/12);	//>>10 to get the file size in kb and 12 as the expected compression rate
			final File outFileFinal = outFile;
			Timer timer = new Timer(250, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (progressMonitor.isCanceled()) {
						exec.abort();
					}
					int length = (int)outFileFinal.length()>>10;
					while (length >= progressMonitor.getMaximum()) {
						length = progressMonitor.getMaximum()-1;
					}
					progressMonitor.setProgress(length);
				}
			});				
			timer.start();
			
			//call oggenc						
			exec.createListenerStream();
			i = exec.exec(new String[] {oggencCmd, options, "-o"+outFile.getPath(), inFile.getPath()});
			
			timer.stop();
			if (progressMonitor.isCanceled()) {
				progressMonitor.close();				
				outFile.delete();	//delete the  partly encoded outFile
				if(TTT.verbose){
				System.out.println("Canceled by user");
				}
				return false;
			}
			progressMonitor.close();
		} else {
			
			//call oggenc
			exec.createListenerStream();
			i = exec.exec(new String[] {oggencCmd, options, "-o"+outFile.getPath(), inFile.getPath()});
			
		}
				
		if (i != 0 || outFile.length() == 0) {	//check success
			if(TTT.verbose){
			System.out.println("Unable to encode audio file using oggenc:");
			}
			System.out.println(exec.getListenerStream());
			outFile.delete();
			throw new IOException("unable to encode audio file using oggenc, i: "+i+" outFile.length: "+outFile.length()+" outFile.getPath(): "+outFile.getPath());
		}		
		if(TTT.verbose){
		System.out.println("Done in " + Constants.getStringFromTime((int)(System.currentTimeMillis()-startTime)));
		System.out.println("----------------------------------------------");
		}
		return true;
	}
		
	
	/**Allows converting audio files using oggenc determining suitable options automatically
	 * @return True: Conversion succeeded.<br>False: Canceled by user
	 */
	public static boolean convertAudioFile(File inFile, File outFile, boolean batch) throws Exception {
		
		//define quality depending on the file size
		int length = (int)inFile.length()>>20;	//file size in mb
		int q;
		if (length < 20) {
			q = 6;	//high quality
		} else if (length < 40) {
			q = 4;	//average quality
		} else if (length < 80) {
			q = 2;	//acceptable quality and fast
		} else {
			q = 0;	//acceptable quality and very fast
		}
		return convertAudioFile(inFile,outFile,"-q " + q, batch);
	}

}
