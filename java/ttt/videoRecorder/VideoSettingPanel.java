package ttt.videoRecorder;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;

import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import ttt.TTT;

public class VideoSettingPanel implements ActionListener {

	final JInternalFrame frame = new JInternalFrame("Camera Settings");

	JComboBox boxCameras = new JComboBox();
	JComboBox boxFormat = new JComboBox();
	JComboBox boxQuality = new JComboBox();
	JLabel lblQuality = new JLabel(
			"Set recording Quality. The higher the better.");
	JLabel lblnotice = new JLabel(); // this text is set in current camera
										// format.
	JButton butStart = new JButton("start");
	JButton butStop = new JButton("stop");
	JButton butApply = new JButton("apply");
	JButton butClose = new JButton("close");
	JPanel panelControll = new JPanel();
	JPanel panelstartstop = new JPanel();
	JPanel panelquality = new JPanel();
	JPanel panelbox = new JPanel();

	public JLabel myLabel = new JLabel();
	JEditorPane Image = new JEditorPane();
	List<TTTVideoFormat> formats;

	List<String> list;

	String AppliedCamera;
	TTTVideoFormat AppliedFormat;
	float AppliedQuality;

	public boolean isRecording = false;

	public WebCamControl WBC;

	public void show(boolean i) {
		frame.setVisible(i);
	}

	public JInternalFrame getFrame() {
		return frame;
	}

	@SuppressWarnings("unchecked")
	public VideoSettingPanel() {

		TTT.getInstance().addInternalFrameCentered(frame);

		WBC = OSUtils.obtainWebcam();
		//MySettings is a nested class within VideoSettingsPanel
		WBC.setCaptureInterface(new MySettings());

		if (WBC.CameraFound()) {
			list = WBC.getDeviceList();

			WBC.setSelectedCam(WBC.getDeviceID(0));
			CurrentCameraformat(0);
			for (int i = 0; i < list.size(); ++i) {
				boxCameras.addItem(list.get(i));
			}
			for (int i = 1; i < 11; i++)
				boxQuality.addItem(i);

			boxCameras.addActionListener(this);

			butStart.addActionListener(this);

			butApply.addActionListener(this);

			myLabel.setVisible(true);

			boxFormat.addActionListener(this);

			boxQuality.addActionListener(this);

			butStop.addActionListener(this);

			panelbox.add(boxCameras);
			panelbox.add(boxFormat);

			panelstartstop.add(butStart);
			panelstartstop.add(butStop);

			panelquality.add(lblQuality);
			panelquality.add(boxQuality);

			panelControll.add(butApply);
		} else {
			lblnotice.setText("No Camera Found");
		}
		butClose.addActionListener(this);

		panelControll.add(butClose);

		frame.setLayout(new GridBagLayout());
		c.fill = GridBagConstraints.HORIZONTAL;
		addPanel(panelbox);
		addPanel(panelstartstop);
		addPanel(panelquality);
		addPanel(panelControll);

		c.gridy = c.gridy + 1;
		c.anchor = GridBagConstraints.PAGE_START;
		frame.add(lblnotice, c);

		c.gridy = c.gridy + 1;
		frame.add(myLabel, c);

		frame.setSize(320, 200);

		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setEnabled(true);

	}// End Constructor

	GridBagConstraints c = new GridBagConstraints();
	int gridx = 0;
	int gridy = 0;

	private void addPanel(JPanel panel) {
		c.gridx = gridx;
		c.gridy = gridy++;
		frame.add(panel, c);
	}

	public boolean CamerasFound() {
		return WBC.CameraFound();
	}

	public void CurrentCameraformat(int DeviceID) {
		// updates the CameraModeBox
		boxFormat.removeAllItems();
		formats = WBC.getSupportedFormats(DeviceID);

		if (formats.isEmpty()) {
			lblnotice
					.setText("No supported Formats found. Setting Resolution to 160x120.");
			formats.add(new TTTVideoFormat(160, 120));
		} else {
			lblnotice
					.setText("It is recommended to use 160x120 as resultion and 1 as Quality.");
		}
		for (TTTVideoFormat format : formats) {
			boxFormat.addItem(videoFormatToString(format));
		}
	}

	public String videoFormatToString(TTTVideoFormat f) {
		return "Width=" + f.getWidth() + " Height=" + f.getHeight();
	}

	public TTTVideoFormat getRecordingFormat() {
		return AppliedFormat;
	}

	public List<String> getCameraIDs() {
		List<String> IDs = new LinkedList<String>();
		for (int i = 0; i < list.size(); i++) {
			IDs.add(WBC.getDeviceID(i));
		}
		return IDs;
	}

	public void setRecordingCamera(String CameraID) {
		AppliedCamera = CameraID;
	}

	public void setQuality(float Quality) {
		AppliedQuality = Quality;
		WBC.setQuality(AppliedQuality);
		int index = (int) (Quality * 10) - 1;
		boxQuality.setSelectedIndex(index);
	}

	public void setRecordingFormat(TTTVideoFormat format) {
		AppliedFormat = format;
	}

	public String getRecordingCamera() {
		return AppliedCamera;
	}

	public float getQuality() {
		return AppliedQuality;
	}

	private void setEnabled(boolean bool) {
		boxFormat.setEnabled(bool);
		boxCameras.setEnabled(bool);
		boxQuality.setEnabled(bool);
		butStart.setEnabled(bool);
		butStop.setEnabled(!bool);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == boxCameras) {
			WBC.setSelectedCam(WBC.getDeviceID(boxCameras.getSelectedIndex()));
			CurrentCameraformat(boxCameras.getSelectedIndex());
		}
		if (e.getSource() == boxQuality) {
			float x = ((float) boxQuality.getSelectedIndex() + 1) / 10;
			WBC.setQuality(x);
		}

		// when a new camera in cameraBox gets selected update the cameraModeBox
		if (e.getSource() == boxFormat) {
			if (boxFormat.getSelectedIndex() != -1)
				WBC.setFormat(formats.get(boxFormat.getSelectedIndex())
						.getWidth(), formats.get(boxFormat.getSelectedIndex())
						.getHeight());
		}

		if (e.getSource() == butStop) {
			WBC.Stop();
			setEnabled(true);
		}
		if (e.getSource() == butStart) {
			setEnabled(false);
			if (formats.get(boxFormat.getSelectedIndex()).getWidth() >= 320)
				frame.setSize(formats.get(boxFormat.getSelectedIndex())
						.getWidth(), formats.get(boxFormat.getSelectedIndex())
						.getHeight() + 200);
			else
				frame.setSize(370, 380);
			WBC.Start();
		}
		if (e.getSource() == butClose) {
			show(false);
			WBC.Stop();
			WBC.release();
		}
		if (e.getSource() == butApply) {
			WBC.Stop();
			AppliedCamera = WBC.getDeviceID(boxCameras.getSelectedIndex());
			AppliedFormat = formats.get(boxFormat.getSelectedIndex());
			AppliedQuality = WBC.getQuality();
			show(false);
			WBC.release();
		}
	}
	
	class MySettings implements CaptureInterface {

		@Override
		public void onNewImage(byte[] image, String RecordPath, float Quality) {
			myLabel.setIcon(new ImageIcon(compressJpegFile(image, Quality)));
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
}
