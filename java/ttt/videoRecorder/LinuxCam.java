package ttt.videoRecorder;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.io.File;
import java.lang.Thread;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.ImageFormat;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.ResolutionInfo.DiscreteResolution;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

public class LinuxCam implements WebCamControl, Runnable {

	private CaptureInterface CI;
	private String v4lSysfsPath = "/sys/class/video4linux/";
	private String RecordPath;
	private List<VideoDevice> system = new LinkedList<VideoDevice>();
	private FrameGrabber fg;
	private Thread captureThread;
	int w = 160, h = 120;
	private boolean CamFound = false;
	private float CompressionQuality = 0.1f;
	public boolean isRecording = false;
	String dev = "/dev/video0";
	int currentdev = 0;

	public LinuxCam() throws V4L4JException {
		initializeDevices();
	}

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
				if (isRecording) {
					bb = fg.getFrame();
					image = new byte[bb.limit()];
					bb.get(image);
					CI.onNewImage(image, RecordPath, CompressionQuality);
				}
			}
		} catch (V4L4JException e) {
			e.printStackTrace();
			System.out.println("Failed to capture image");
		}
	}

	private Object[] listV4LDeviceFiles() {
		Vector<String> sdev = new Vector<String>();
		File dir = new File(v4lSysfsPath);
		String[] files = dir.list();

		for (String file : files)
			// the following test the presence of "video" in
			// each file name - not very portable - relying on HAL
			// would be much better ...
			if (file.indexOf("video") != -1)
				sdev.add("/dev/" + file);
		if (sdev.size() != 0) {
			CamFound = true;
		}

		return sdev.toArray();
	}

	private void initFrameGrabber(int std, int channel, int qty)
			throws V4L4JException {
		fg = system.get(currentdev)
				.getJPEGFrameGrabber(w, h, channel, std, qty);
	}

	@Override
	public void release() {
		for (VideoDevice x : system) {
			x.releaseFrameGrabber();
			x.release();
		}
	}

	@Override
	public boolean Start() {
		if (CamFound)
			try {
				initFrameGrabber(0, 0, 1);
				if (!isRecording) {
					fg.startCapture();
					captureThread = new Thread(this, "Capture Thread");
					isRecording = true;
					captureThread.start();
				}
			} catch (V4L4JException e) {

				e.printStackTrace();
			}
		return false;
	}

	@Override
	public boolean Stop() {
		if (isRecording) {
			captureThread = null;
			isRecording = false;
			fg.stopCapture();
			fg = null;

			system.get(currentdev).releaseFrameGrabber();

		}
		return false;
	}

	@Override
	public String getDeviceID(int Device) {
		if (CamFound)
			return "/dev/video" + Device;

		return null;

	}

	@Override
	public List<String> getDeviceList() {
		List<String> deviselist = new LinkedList<String>();
		if (CamFound)
			for (VideoDevice i : system) {
				try {
					deviselist.add(i.getDeviceInfo().getName());
					return deviselist;
				} catch (V4L4JException e) {

					e.printStackTrace();
				}
			}
		return null;
	}

	@Override
	public List<TTTVideoFormat> getSupportedFormats(int Device) {
		currentdev = Device;
		List<TTTVideoFormat> SupportedFormats = new LinkedList<TTTVideoFormat>();
		if (CamFound)
			try {
				for (ImageFormat i : system.get(Device).getDeviceInfo()
						.getFormatList().getJPEGEncodableFormats()) {
					if (i.getResolutionInfo().getType().toString().equals(
							"DISCRETE")) {
						for (DiscreteResolution j : i.getResolutionInfo()
								.getDiscreteResolutions()) {
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
	public void setSelectedCam(String DeviceID) {
		dev = DeviceID;
	}

	@Override
	public void setRecordPath(String Path) {
		RecordPath = Path;
	}

	@Override
	public void setCaptureInterface(CaptureInterface OnPic) {
		CI = OnPic;
	}

	@Override
	public boolean CameraFound() {
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
