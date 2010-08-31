package ttt.video;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.io.File;
import java.lang.Thread;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.ImageFormat;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.ResolutionInfo.DiscreteResolution;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

public class LinuxCam implements WebCamControl, Runnable {

    private	CaptureHandler CI;	
	private String RecordPath;
	private List<VideoDevice> system = new LinkedList<VideoDevice>();
	private FrameGrabber fg;
	private Thread captureThread;
    private	int w = 160, h = 120;
	private boolean CamFound = false;
    private	float CompressionQuality = 0.1f;
	private boolean isRecording = false;
	private int selectedCameraID = 0;

	public LinuxCam() throws V4L4JException {
		initializeDevices();
	}

	//all cameras are initialized in order to gather information about them
	protected void initializeDevices() throws V4L4JException {
		for (Object i : listV4LDeviceFiles()) {		
			system.add(new VideoDevice(i.toString()));
		}
	}

	/**
	 * Implements the capture thread: get a frame from the FrameGrabber, and
	 * display it
	 */
	public void run() {
		ByteBuffer bb;
		byte[] image;
		try {
			while (isRecording) {
					bb = fg.getFrame();
					
					image = new byte[bb.limit()];
				
					bb.get(image);
				
					CI.onNewImage(image, RecordPath, CompressionQuality);				
			}
		} catch (V4L4JException e) {
			e.printStackTrace();
			System.out.println("Failed to capture image");
		}
	}
		
	private Object[] listV4LDeviceFiles() {
		Vector<String> sdev = new Vector<String>();
		String v4lSysfsPath = "/sys/class/video4linux/";
		File dir = new File(v4lSysfsPath);
		String[] files = dir.list();
		if (files!=null)
		{
			for (String file : files)

			// the following test the presence of "video" in
			// each file name - not very portable - relying on HAL
			// would be much better ...
			if (file.indexOf("video") != -1)
				sdev.add("/dev/" + file);
			if (sdev.size() != 0) 
				CamFound = true;
			return sdev.toArray();
		}
		else return new Object[0];
	}

	private void initFrameGrabber(int std, int channel)
			throws V4L4JException { 
		fg = system.get(selectedCameraID).getJPEGFrameGrabber(w, h, channel, std, V4L4JConstants.MAX_JPEG_QUALITY);
	}

	@Override
	public void release() {//all initialized systems have to be released.
		for (VideoDevice x : system) {
			x.releaseFrameGrabber();
			x.release();
		}
	}

	@Override
	public boolean start() {
		boolean check = false;
		if (CamFound)
			try {
				
			//checking if camera (still) exists
			for(Object i: listV4LDeviceFiles()){
				if(	i.toString().equals(system.get(selectedCameraID).getDevicefile()))	{
				check = true;
				}
			}
			
			if(check){
				initFrameGrabber(0, 0);
				if (!isRecording) {
					fg.startCapture();
					captureThread = new Thread(this, "Capture Thread");
					isRecording = true;
					captureThread.start();
					return true;
					}
				}
			} catch (V4L4JException e) {
				e.printStackTrace();
			}
		return false;
	}

	@Override
	public boolean stop() {
		if (isRecording) {
			captureThread = null;
			isRecording = false;
			fg.stopCapture();
			fg = null;
			system.get(selectedCameraID).releaseFrameGrabber();
			return true;
		}
		return false;
	}

	@Override
	public String getDeviceName(int DeviceID) {		
		if (CamFound)
			try {
				return system.get(DeviceID).getDeviceInfo().getName();
			} catch (V4L4JException e) {				
				e.printStackTrace();
			}
		return null;
	}

	@Override
	public List<String> getDeviceNames() {
		List<String> devicelist = new LinkedList<String>();
		if (CamFound){
			for (VideoDevice i : system) {
				try {
					devicelist.add(i.getDeviceInfo().getName());					
				} catch (V4L4JException e) {
					e.printStackTrace();
				}
			}
			if(!devicelist.isEmpty())
		return devicelist;
		}
		return null;
	}

	@Override
	public List<TTTVideoFormat> getSupportedFormats(int Device) {		
		List<TTTVideoFormat> SupportedFormats = new LinkedList<TTTVideoFormat>();
		if (CamFound)
			try {
				for (ImageFormat i : system.get(Device).getDeviceInfo().getFormatList().getJPEGEncodableFormats()) {
					if (i.getResolutionInfo().getType().toString().equals("DISCRETE")) {						
						for (DiscreteResolution j : i.getResolutionInfo().getDiscreteResolutions()) {
							SupportedFormats.add(new TTTVideoFormat(j
									.getWidth(), j.getHeight()));
						}
					}
				}
				if(!SupportedFormats.isEmpty())
				return SupportedFormats;
			} catch (V4L4JException e) {
				e.printStackTrace();
			}
		return null;
	}

	@Override
	public void setFormat(int Width, int Height) {
		w = Width;
		h = Height;
	}

	@Override
	public void setSelectedCam(String DeviceID) throws SetCameraException {	
		for(int i = 0; i < system.size();i++){
			try {
				if(system.get(i).getDeviceInfo().getName().equals(DeviceID)){
					selectedCameraID = i;
				break; 
				}
			} catch (V4L4JException e) {
				throw new SetCameraException();			
			}
		}//for closing
	}	

	@Override
	public void setRecordPath(String Path) {
		RecordPath = Path;
	}

	@Override
	public void setCaptureInterface(CaptureHandler OnPic) {
		CI = OnPic;
	}

	@Override
	public boolean cameraFound() {
		return CamFound;
	}

	@Override
	public void setQuality(float Quality) {
		if (Quality <= 1.0f && Quality >= 0.1f)
			CompressionQuality = Quality;
	}

	@Override
	public float getQuality() {
		return CompressionQuality;
	}

}
