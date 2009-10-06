/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.videoRecorder;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import ttt.videoRecorder.OSUtils.CameraException;

public class Videotest implements ActionListener {
	
	public static void main(String args[]) throws Exception {
		Videotest kf = new Videotest();
		kf.gui();
	}
	
	// testclass no real relevance
	String path = System.getProperty("user.dir");
	JFrame vidtest = new JFrame();
	JPanel panelbuttons = new JPanel();
	JButton butVideoCreator = new JButton("VideoCreator");
	JButton butVideoRecorder = new JButton("VideoRecorder");
	JButton butVideoSettings = new JButton("VideoSettings");

	public void gui() {
		butVideoCreator.addActionListener(this);
		butVideoRecorder.addActionListener(this);
		butVideoSettings.addActionListener(this);
		panelbuttons.add(butVideoCreator);
		panelbuttons.add(butVideoRecorder);
		panelbuttons.add(butVideoSettings);
		vidtest.add(panelbuttons);
		vidtest.setEnabled(true);
		vidtest.setVisible(true);
		vidtest.setSize(1000, 700);
		vidtest.setLayout(new GridBagLayout());
		vidtest.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		try {
			// k.getFrame().setSelected(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean recording = false;

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == (butVideoCreator)) {
			try {
				videocreator();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
		if (arg0.getSource() == (butVideoSettings)) {
			videosettings();
		}
		if (arg0.getSource() == (butVideoRecorder)) {
			try {
				videorecorder();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}



	final VideoSettingPanel k;
	public Videotest() throws CameraException{
		k= new VideoSettingPanel(OSUtils.obtainWebcam());
	}

	public void videosettings() {
		if (!recording) {
			k.show(true);
			vidtest.add(k.getFrame());
			System.out.println("cam found " + k.CamerasFound());
		}
	}

	public TTTVideoFormat format = new TTTVideoFormat(160, 120);
	public String recordCameraID = null;
	public float Quality = 0.1f;
	VideoRecorderPanel Test;

	int cam = 0;
	
	public void videorecorder() throws IOException {
		if (!recording) {
			if (k != null) {
				recordCameraID = k.getRecordingCamera();
				format = k.getRecordingFormat();
				Quality = k.getQuality();
			}

			Test = new VideoRecorderPanel();
			System.out.println(path + ".bjpg");
			File del = new File(path + ".bjpg");
			System.out.println("Deletion: " + del.delete());
			Test.setRecordpath(path);
		
			//Test.setRecordingCamera(recordCameraID);
			Test.setRecordingFormat(format);
			Test.setVideoQuality(Quality);
			Test.Start();
			//vidtest.add( Test.getFrame());
			cam++;
		//	recording = true;
		}
		if (recording) {
			Test.Stop();
			recording = false;
		}
	}

	public void videocreator() throws IOException {
		VideoCreator e = new VideoCreator();
		vidtest.add(e.getFrame());

		e.create(path + ".bjpg");
	}

}
