/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.videoRecorder;


import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;

import javax.swing.JPanel;

import au.edu.jcu.v4l4j.exceptions.V4L4JException;

import com.lti.civil.CaptureException;


public class VideoRecorder  implements WindowListener{
	
	JFrame frame = new JFrame("TTTCam");

	JPanel panel = new JPanel ();
		
	long start; 
	
	public static JLabel myLabel = new JLabel();

	public boolean isRecording = false;
	
	public WebCamControl WBC;
  	
	
	public VideoRecorder() {

		
		if(OSUtils.isWindows())	{
			try {
				WBC = new WindowsCam();
			} catch (CaptureException e) {
			
				e.printStackTrace();
			}			
		}
		if(OSUtils.isLinux()){
			try {
				WBC = new LinuxCam();
			} catch (V4L4JException e) {
					e.printStackTrace();
			}
		}
						
		WBC.setSelectedCam (WBC.getDeviceID(0));
							
		WBC.setFormat(160, 120);
		
		panel.add(myLabel);
		//
		//frame
		//
		frame.addWindowListener(this);		
		frame.setSize(160, 160);		
		frame.add (panel);		
		
		frame.setVisible(true);
		}// End Constructor
			
		public void Start(){
			if(!isRecording){
			WBC.Start();
			start = System.nanoTime();
			isRecording = true;}
		}
		public void Stop() {
			if(isRecording){
			WBC.Stopp();
			isRecording = false;
			}
		}
		
		public void saveelapsedTime(){
			long elapsed = System.nanoTime() - start;
			File TimeWriter = new File(OnCapture.getRecordPath()+".bjpg");
			FileWriter fw;
			try {
				fw = new FileWriter(TimeWriter, true);
				fw.write(elapsed+"ElapsedTime");
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		public void close(){
			frame.dispose();
		}
		
		public void setRecordpath(String Path){
			WBC.SetRecordPath(Path);
		}

		@Override
		public void windowClosing(WindowEvent arg0) {
				Stop();			
		}
		@Override
		public void windowActivated(WindowEvent arg0) {	}
		@Override
		public void windowClosed(WindowEvent arg0) {}
		@Override
		public void windowDeactivated(WindowEvent arg0) {}
		@Override
		public void windowDeiconified(WindowEvent arg0) {}
		@Override
		public void windowIconified(WindowEvent arg0) {}
		@Override
		public void windowOpened(WindowEvent arg0) {}	
	}
	




	



