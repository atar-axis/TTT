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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.ProgressMonitor;
import javax.swing.Timer;


/**
 * Helper class for using the mp3 encoder <a href="http://lame.sourceforge.net/">lame</a> via system calls. <br>
 * Created on 29. June 2009, 14:15
 * @author Christof Angermueller 
 */
public class LameEncoder {

	private static final String LAME = "lame";	
	
	
	public static void main(String[] args) throws Exception{		
		
		String[] lameArgs = Arrays.copyOf(args, 3);
		if (lameArgs[0] == null || lameArgs[1] == null) {
			System.out.println("LameEncoder inputFile outputFile [options]");
			return;
		}
		LameEncoder.convertAudioFile(new File(lameArgs[0]),new File(lameArgs[1]),lameArgs[2] == null ? "" : lameArgs[2],true);
	}
	
	
	/**Checks whether lame is available.
	 * @return	True if lame is available.
	 * */
	public static boolean isLameAvailable() {
		return Exec.getCommand(LAME) != null;
	}
	
	
	/**Allows converting audio files using lame.
	 * @return True: Conversion succeeded.<br>False: Canceled by user. 
	 */
	public static boolean convertAudioFile(File inFile, File outFile, String options, boolean batch) throws Exception {

		String lameCmd = Exec.getCommand(LAME);	//get lame command
		if (lameCmd == null) {
			throw new IOException("lame not found");
		}
		System.out.println("Encoding mp3 file");
		long startTime = System.currentTimeMillis();	//time measurement
		outFile.delete();	//delete outFile in order to test success after encoding
		
		final Exec exec = new Exec();
		int i;
		if (!batch) {
			final ProgressMonitor progressMonitor = new ProgressMonitor(TTT.getRootComponent(),null,"encoding mp3 file",0,100);
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
			
			//call lame
			exec.createListenerStream();
			i = exec.exec(new String[] {lameCmd,options,inFile.getPath(), outFile.getPath()});	
			
			timer.stop();
			if (progressMonitor.isCanceled()) {
				progressMonitor.close();				
				outFile.delete();	//delete the  partly encoded outFile
				System.out.println("Canceled by user");
				return false;
			}
			progressMonitor.close();
		} else {
			
			//call lame
			exec.createListenerStream();
			i = exec.exec(new String[] {lameCmd,options,inFile.getPath(), outFile.getPath()});			
		}
				
		if (i != 0 || outFile.length() == 0) {	//check success
			System.out.println("Unable to encode audio file using lame:");
			System.out.println(exec.getListenerStream());
			outFile.delete();
			throw new IOException("unable to encode audio file using lame");
		}		
		System.out.println("Done in " + Constants.getStringFromTime((int)(System.currentTimeMillis()-startTime)));			
		return true;
	}
		
	
	/**Allows converting audio files using lame determining suitable options automatically.
	 * @return True: Conversion succeeded.<br>False: Canceled by user.
	 */
	public static boolean convertAudioFile(File inFile, File outFile, boolean batch) throws Exception {
		
		//define quality depending on the file size
		int length = (int)inFile.length()>>20;	//file size in mb
		int q;
		if (length < 20) {
			q = 2;	//high quality
		} else if (length < 40) {
			q = 5;	//average quality
		} else if (length < 80) {
			q = 7;	//acceptable quality and fast
		} else {
			q = 9;	//acceptable quality and very fast
		}
		return convertAudioFile(inFile,outFile,"-q " + q, batch);
	}

}
