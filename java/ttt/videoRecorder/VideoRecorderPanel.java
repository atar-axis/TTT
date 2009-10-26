/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.videoRecorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.File;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;

import javax.swing.JPanel;

import ttt.videoRecorder.OSUtils.CameraException;

public class VideoRecorderPanel implements WindowListener, ActionListener {
	//MyWindow is a nested class within VideoRecroderPanel
	private MyWindow frame = new MyWindow();

	private JPanel panel = new JPanel();

	private JButton butHide = new JButton("hide");

	public JLabel myLabel = new JLabel();

	private long startTime;

	private String RecordPath;

	public boolean isRecording = false;

	public WebCamControl WBC;

	private TTTVideoFormat tttFormat = new TTTVideoFormat(160, 120);

	public VideoRecorderPanel(String RecordingCamera, TTTVideoFormat Format, float Quality, String RecordPath) {

		try {
			WBC = OSUtils.obtainWebcam();
		} catch (CameraException e) {
			e.printStackTrace();
		}

		//MyCapture is a nested class within VideoRecroderPanel
		WBC.setCaptureInterface(new MyCapture());

		if (WBC.CameraFound()) {
			setRecordingCamera(RecordingCamera);
			setRecordingFormat(Format);
			setRecordpath(RecordPath);
			setVideoQuality(Quality);
			butHide.setSize(20, 15);
			butHide.addActionListener(this);

			panel.add(butHide);

		} else {
			myLabel.setText("No Camera Found");
		}
		//
		// frame
		//
		frame.addWindowListener(this);
		this.setSize();
		panel.add(myLabel);
		frame.add(panel);

		frame.setVisible(true);
	}// End Constructor

	private void setSize() {
		frame.setSize(tttFormat.getWidth(), tttFormat.getHeight() + 40);
	}

	public void Start() {
		if (WBC.CameraFound())
			if (!isRecording) {
				WBC.setFormat(tttFormat.getWidth(), tttFormat.getHeight());
				setSize();
				try {
					WBC.Start();
				} catch (CameraStartException e) {				
					e.printStackTrace();
				}
				startTime = System.nanoTime();
				isRecording = true;
			}
	}

	public void Stop() {
		if (isRecording) {
			try {
				WBC.Stop();
			} catch (CameraStopException e) {	
				e.printStackTrace();
			}
			WBC.release();
			isRecording = false;
			saveelapsedTime();
			this.frame.dispose();
		}
	}

	protected void saveelapsedTime() {

		long elapsed = System.nanoTime() - startTime;
		File TimeWriter = new File(RecordPath + ".bjpg");
		FileWriter fw;
		try {
			fw = new FileWriter(TimeWriter, true);
			fw.write(elapsed + " ElapsedTime");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		Stop();
		frame.dispose();
	}

	public void setRecordingCamera(String recordCameraID) {
		try{
		if (recordCameraID != null) {
			WBC.setSelectedCam(recordCameraID);
		} else {
			WBC.setSelectedCam(WBC.getDeviceName(0));
		}}
		catch(SetCameraException e)	{
			System.out.println("Couldn't select Camera. Closing Record Window.");
			this.close();
		}
	}

	public void setRecordingFormat(ttt.videoRecorder.TTTVideoFormat format) {
		if (format != null) {
			tttFormat = format;
		}
	}

	public void setRecordpath(String Path) {
		RecordPath = Path;
		WBC.setRecordPath(Path);
	}

	public void setVideoQuality(float Quality) {
		WBC.setQuality(Quality);
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		frame.dispose();
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == butHide) {
			if (butHide.getText() == "hide") {
				myLabel.setVisible(false);
				butHide.setText("show");
				frame.setSize(70, 40);
			} else {
				butHide.setText("hide");
				myLabel.setSize(160, 120);
				this.setSize();
				myLabel.setVisible(true);
			}
		}
	}

	class MyCapture extends CaptureHandler {

		@Override
		public void onNewImage(byte[] image, String RecordPath, float Quality) {
			File ImageFile = new File(RecordPath + ".bjpg");
			try {
				FileOutputStream fos = new FileOutputStream(ImageFile, true);

				image = compressJpegFile(image, Quality);
				fos.write(image);
				fos.close();

				FileWriter fw = new FileWriter(ImageFile, true);
				fw.write(" FileEnd");
				fw.close();

				myLabel.setIcon(new ImageIcon(image));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}		

	}
	
	


@SuppressWarnings("serial")
class MyWindow extends JWindow {

	private int X = 0;
	private int Y = 0;

	public MyWindow() {

		setBounds(60, 60, 100, 100);

		// Print (X,Y) coordinates on Mouse Click
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				X = e.getX();
				Y = e.getY();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				setLocation(getLocation().x + (e.getX() - X), getLocation().y
						+ (e.getY() - Y));
			}
		});

		setVisible(true);
	}
}
}