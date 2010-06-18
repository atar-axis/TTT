package ttt.video;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;

import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import ttt.TTT;
import ttt.video.OSUtils.CameraException;


//GUI for setting Camera options (like quality size etc.)
public class VideoSettingPanel implements ActionListener {

	final JInternalFrame frame = new JInternalFrame("Camera Settings");

	JComboBox boxCameras = new JComboBox();
	JComboBox boxFormat = new JComboBox();
	JComboBox boxQuality = new JComboBox();
	JLabel lblQuality = new JLabel(
			"Set recording Quality. The higher the better.");
	JLabel lblnotice = new JLabel(); // this text is set in current camera
										// format.
	
	boolean CamFound = false;
	JButton butStart = new JButton("preview");
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
		frame.pack();
	}

	public JInternalFrame getFrame() {
		return frame;
	}

	@SuppressWarnings("unchecked")
	public VideoSettingPanel() {

		TTT.getInstance().addInternalFrameCentered(frame);

		try {
			WBC= OSUtils.obtainWebcam();
			//MySettings is a nested class within VideoSettingsPanel
			WBC.setCaptureInterface(new MySettings());
		
		if (WBC.CameraFound()) {
			CamFound = true;
			list = WBC.getDeviceNames();

			WBC.setSelectedCam(WBC.getDeviceName(0));
			
			System.out.println("Chosen Camera: " + WBC.getDeviceName(0));
						
			CurrentCameraformat(0);
			if(list != null)
			for (int i = 0; i < list.size(); ++i) {
				boxCameras.addItem(list.get(i));
			}
			
			for (int i = 1; i < 11; i++){
				boxQuality.addItem(i);
			}

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
		}
		else {
			lblnotice.setText("No Camera Found");
		}
		}catch(CameraException e){
			lblnotice.setText("No Camera Found");
		}catch(SetCameraException e){
			
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
		frame.add(lblnotice, c);
		c.gridy = c.gridy + 1;		
		frame.add(myLabel, c);
		c.anchor = GridBagConstraints.CENTER;
		frame.pack();	
	frame.setVisible(false);
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
		return CamFound;
	}

	public void CurrentCameraformat(int DeviceID) {
		// updates the CameraModeBox
		boxFormat.removeAllItems();
		try {
			formats = WBC.getSupportedFormats(DeviceID);
		} catch (CameragetFormatsException e) {			
			e.printStackTrace();
		}

		if (formats == null) {
			formats = new LinkedList<TTTVideoFormat>();
			lblnotice.setText("No supported Formats found. Setting Resolution to 160x120.");
			formats.add(new TTTVideoFormat(160, 120));
		} else {
			lblnotice
					.setText("It is recommended to use 160x120 as resultion and 10 as Quality.");
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
			IDs.add(WBC.getDeviceName(i));
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
		if(1 <= index && index <= 10 ){
		boxQuality.setSelectedIndex(index);}
	
	}

	public void setRecordingFormat(TTTVideoFormat format) {
		AppliedFormat = format;
		boxFormat.setSelectedItem(videoFormatToString(format));
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
		butStop.setEnabled(bool);
	}

	public void release(){
		if (CamFound)
		WBC.release();
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == boxCameras) {
			try {
				WBC.setSelectedCam(WBC.getDeviceName(boxCameras.getSelectedIndex()));
			CurrentCameraformat(boxCameras.getSelectedIndex());
			} catch (SetCameraException e1) {
				e1.printStackTrace();
			}			
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
			try {
				WBC.Stop();
			} catch (CameraStopException e1) {				
				e1.printStackTrace();
			}
			setEnabled(true);
		}
		
		if (e.getSource() == butStart) {	
			try {
				if(WBC.Start()){
					setEnabled(true);
					frame.pack();
				}		
				else{
					lblnotice.setText("Something went wrong. Couldn't start Preview.");
				}
			} catch (CameraStartException e1) {				
				e1.printStackTrace();
			}
		}
		
		if (e.getSource() == butClose) {
			show(false);
			setEnabled(true);
			try {
				WBC.Stop();
			} catch (CameraStopException e1) {				
				e1.printStackTrace();
			}
		}
		
		if (e.getSource() == butApply) {
			setEnabled(true);
			try {
				WBC.Stop();
			} catch (CameraStopException e1) {				
				e1.printStackTrace();
			}
			AppliedCamera = WBC.getDeviceName(boxCameras.getSelectedIndex());
			AppliedFormat = formats.get(boxFormat.getSelectedIndex());
			AppliedQuality = WBC.getQuality();
			show(false);
		}
	}
	
	class MySettings extends CaptureHandler {
		@Override
		public void onNewImage(byte[] image, String RecordPath, float Quality) {
			myLabel.setIcon(new ImageIcon(compressJpegFile(image, Quality)));
			frame.pack();
		}
		
		
	}
}
