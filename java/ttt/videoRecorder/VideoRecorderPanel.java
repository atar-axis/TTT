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
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import java.io.File;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JButton;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;

import javax.swing.JPanel;

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

	public VideoRecorderPanel() {

		WBC = OSUtils.obtainWebcam();

		//MyCapture is a nested class within VideoRecroderPanel
		WBC.setCaptureInterface(new MyCapture());

		if (WBC.CameraFound()) {
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
				WBC.Start();
				startTime = System.nanoTime();
				isRecording = true;
			}
	}

	public void Stop() {
		if (isRecording) {
			WBC.Stop();
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
		if (recordCameraID != null) {
			WBC.setSelectedCam(recordCameraID);
		} else {
			WBC.setSelectedCam(WBC.getDeviceID(0));
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

	class MyCapture implements CaptureInterface {

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

		public byte[] compressJpegFile(byte[] inbytes,
				float compressionQuality) {
			//1.0f means no compression
			if (Float.compare(compressionQuality, 1.0f) != 0) { 
				try {

					BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(
							inbytes));

					File outfile = File.createTempFile("pattern", ".suffix");
					RenderedImage rendImage = bimg;// ImageIO.read(infile);

					ImageWriter writer = null;
					Iterator<?> iter = ImageIO.getImageWritersByFormatName("jpg");
					if (iter.hasNext()) {
						writer = (ImageWriter) iter.next();
					}
					ImageOutputStream ios = ImageIO
							.createImageOutputStream(outfile);
					writer.setOutput(ios);
					JPEGImageWriteParam iwparam = new JPEGImageWriteParam(Locale
							.getDefault());

					iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					iwparam.setCompressionQuality(compressionQuality);
					writer
							.write(null, new IIOImage(rendImage, null, null),
									iwparam);
					ios.flush();
					ios.close();
					writer.dispose();

					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					ImageIO.write(ImageIO.read(outfile), "jpeg", baos);

					byte[] bytesOut = baos.toByteArray();

					baos.close();
					return bytesOut;
				} catch (IOException ioe) {
					System.out.println("write error: " + ioe.getMessage());
					return null;
				}
			}
			return inbytes;
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