/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.video;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Vector;


import javax.media.MediaLocator;
import javax.swing.ImageIcon;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import javax.swing.JProgressBar;
import ttt.editor.tttEditor.FileConnector;

//Convert bjpg files into mov and show progress.
public class VideoCreator  implements PropertyChangeListener{
	
	private JLabel lblProgress;		
	private JFrame frameProgress;	
	private JProgressBar progressBar;
	private task Task;
	private long length;
		
	public VideoCreator() {
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);

		lblProgress = new JLabel("");

		frameProgress = new JFrame("Converting...");
		frameProgress.add(progressBar);
		frameProgress.add(lblProgress);
		lblProgress.setText("0%");
		frameProgress.pack();
		frameProgress.setLayout(new GridLayout(3, 1));
		frameProgress.setVisible(true);		
	}
	
	//needed to notify the FileConnector when the vid is finished
	FileConnector Caller;	
	public void setCallingFileConnector(FileConnector x){
		Caller = x;
	}
	
	public void create(String FilePath) {
		File lengthgetter = new File(FilePath);
		frameProgress.setLocation(50,50);
		frameProgress.setVisible(true);	
		frameProgress.pack();
		frameProgress.setEnabled(true);
		length = lengthgetter.length();
		Task = new task(FilePath, lengthgetter.length());
		Task.addPropertyChangeListener(this);		
		Task.execute();		
	}
		
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if ("progress" == arg0.getPropertyName()) {
			int progress = (Integer) arg0.getNewValue();
			progressBar.setValue(progress);
			lblProgress.setText("Processing Video.");
			frameProgress.repaint();
		}
	}
	
	
	class task extends SwingWorker<Void, Void> {
		String Path;

		task(String bjpgPath, long size) {
			Path = bjpgPath;
			length = size;
		}

		@Override
		protected void done() {
			setProgress(100);
			lblProgress.setText("Done");
			frameProgress.dispose();
			if(Caller != null){
				Caller.setVideo();
			}
		}

		@Override
		protected Void doInBackground() throws Exception {
			try {
				// looks for the 'end' of a jpg
				FileInputStream fis = new FileInputStream(Path);

				// actually fills the buffer
				InputStream fis2 = new FileInputStream(Path);
				int i;

				String v; // looks for the escape strings
				Vector<byte[]> InFiles = new Vector<byte[]>();
				byte[] buffer;
				StringBuffer strContent = new StringBuffer("");
				long FrameRate = 15;
				long ElapsedTime = 0;
				Boolean first = true;
				int j = 0;
				while ((i = fis.read()) != -1) {

					strContent.append((char) i);
					v = strContent.toString();

					if (v.contains("FileEnd")) {
						buffer = new byte[v.length() - 7];
						if (!first) {

							// skipping fileEnd
							fis2.read(buffer, 0, 7);
							j = j + buffer.length + 8;
							System.out.println((j / (length / 100)) + "% done");

							setProgress((int) (j / (length / 100)));
						}
						fis2.read(buffer, 0, buffer.length);
						first = false;

						InFiles.add(buffer);
						strContent = new StringBuffer("");
					}
					// calculates the framerate
					if (v.contains("ElapsedTime")) {
						v = v.substring(0, v.length() - 12);
						ElapsedTime = Long.parseLong(v);
						strContent = new StringBuffer("");
					}
				}

				if (ElapsedTime != 0) {
					System.out.println(ElapsedTime);
					ElapsedTime = java.util.concurrent.TimeUnit.NANOSECONDS
							.toSeconds(ElapsedTime);

					FrameRate = InFiles.size() / ElapsedTime;
					System.out.println(ElapsedTime);
					System.out.println("Framerate: " + FrameRate
							+ " Number of Files: " + InFiles.size()
							+ " ElapsedNanoTime: " + ElapsedTime);

				}
				Path = Path.substring(0, Path.length() - 4); // remove the
																// ".bjpg" from
																// path

				MediaLocator outML = new MediaLocator("file:" + Path + "mov");

				ImageIcon imageformat = new ImageIcon(InFiles.get(0));
				JpegImagesToMovie test = new JpegImagesToMovie();
				test.doIt(imageformat.getIconWidth(), imageformat
						.getIconHeight(), (int) FrameRate, InFiles, outML);
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
					
			return null;
		}
	}
	
	
	
}
