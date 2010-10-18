/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.video;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;

import com.lti.civil.CaptureDeviceInfo;
import com.lti.civil.CaptureException;
import com.lti.civil.CaptureObserver;
import com.lti.civil.CaptureStream;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;
import com.lti.civil.Image;
import com.lti.civil.VideoFormat;
import com.lti.civil.awt.AWTImageConverter;

public class WindowsCam implements WebCamControl {
	private CaptureHandler CI;
	private boolean isRecording = false;
	private String RecordPath;
	private String selectedCameraID; // Remembers the currently selected cam
	private int SelectedFormat; // Remembers the currently selected format
	private CaptureStream StartcaptureStream; // The recording stream
	private CaptureSystemFactory factory = DefaultCaptureSystemFactorySingleton.instance();
    private	CaptureSystem system = factory.createCaptureSystem();
	private boolean CamFound = false;
	private float CompressionQuality = 0.1f;

	@SuppressWarnings("unchecked")
	public WindowsCam() throws CaptureException {
		system.init();
		List list = system.getCaptureDeviceInfoList();
		if (list.size() > 0) { // check if any camera found
			CamFound = true;
		}

		for (int i = 0; i < list.size(); ++i) {
			CaptureDeviceInfo info = (CaptureDeviceInfo) list.get(i);		
			selectedCameraID = info.getDeviceID();
		}
	}

	public List<TTTVideoFormat> getSupportedFormats(int Device) {
		List<TTTVideoFormat> SupportedFormats = new LinkedList<TTTVideoFormat>();
		if (CamFound)
			try {
				StartcaptureStream = system
						.openCaptureDeviceStream(getDeviceName(Device));
				for (VideoFormat format : StartcaptureStream.enumVideoFormats()) {
					// check if formats are valid
					if (format.getWidth() > 0 && format.getHeight() > 0)
						SupportedFormats.add(new TTTVideoFormat(format
								.getWidth(), format.getHeight()));
				}
				if(!SupportedFormats.isEmpty())
				return SupportedFormats;
			} catch (CaptureException e) {
				e.printStackTrace();
			}
		return null;
	}

	public String videoFormatToString(VideoFormat f) {
		return "Width=" + f.getWidth() + " Height=" + f.getHeight() + " FPS="
				+ f.getFPS();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getDeviceName(int Device) {
		if (CamFound)
			try {
				List<CaptureDeviceInfo> z = (List<CaptureDeviceInfo>) system
						.getCaptureDeviceInfoList();
				return z.get(Device).getDeviceID();

			} catch (CaptureException e) {
				e.printStackTrace();
			}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List getDeviceNames() {
		if (CamFound)
			try {
				List<CaptureDeviceInfo> z = (List<CaptureDeviceInfo>) system
						.getCaptureDeviceInfoList();
				List<String> DeviceList = new LinkedList();
				for (CaptureDeviceInfo x : z) {
					DeviceList.add(x.getDescription());
				}
				if (!DeviceList.isEmpty()) {
					return DeviceList;
				} else {
					return null;
				}
			} catch (CaptureException e) {
				e.printStackTrace();
			}
		return null;
	}

	public String getSelectedCam() {
		return selectedCameraID;
	}

	@Override
	public boolean start() {
		if (CamFound)
			if (!isRecording) {
				try {
					system.init();

					StartcaptureStream = system
							.openCaptureDeviceStream(selectedCameraID);

					StartcaptureStream.setObserver(new MyCaptureObserver());

					StartcaptureStream.setVideoFormat(StartcaptureStream
							.enumVideoFormats().get(SelectedFormat));

					System.out.println("Capturing");
					isRecording = true;
					StartcaptureStream.start();

				} catch (CaptureException e1) {
					e1.printStackTrace();
				}
			}
		return false;
	}

	@Override
	public boolean stop() {
		try {
			if (isRecording) {
				StartcaptureStream.stop();
				StartcaptureStream.dispose();
			
				isRecording = false;
			}
		} catch (CaptureException e1) {
			e1.printStackTrace();
		}
		return false;
	}

	@Override
	public void setSelectedCam(String DeviceID) {
		selectedCameraID = DeviceID;
	}

	@Override
	public void setFormat(int Width, int Height) {
		int i = 0;
		if (CamFound)
			try {
				CaptureStream captureStream;
				captureStream = system.openCaptureDeviceStream(selectedCameraID);

				for (VideoFormat format : captureStream.enumVideoFormats()) {
					if (format.getWidth() == Width
							&& format.getHeight() == Height) {
						System.out.println("Choosing format: "
								+ videoFormatToString(format));
						SelectedFormat = i;
						break;
					}
					i++;
				}
			} catch (CaptureException e) {
				e.printStackTrace();
			}
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
	public void release() {
	} // not necessary for WindowsCam

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

	//
	// gets called when a new picture is made. it calls OnCapture
	//
	class MyCaptureObserver implements CaptureObserver {
		public void onError(CaptureStream sender, CaptureException e) {
			System.err.println("onError " + sender);
			e.printStackTrace();
		}

		public void onNewImage(CaptureStream sender, Image image) {
			BufferedImage bimg;
			try {
				bimg = AWTImageConverter.toBufferedImage(image);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				ImageIO.write(bimg, "jpeg", baos);

				byte[] bytesOut = baos.toByteArray();

				baos.close();

				CI.onNewImage(bytesOut, RecordPath, CompressionQuality);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean close() throws CameraStopException {
		
		try {
			StartcaptureStream.stop();
			StartcaptureStream.dispose();
			isRecording = false;
		} catch (CaptureException e) {
			System.out.println("Something went wrong closing the Camerastream!");
			e.printStackTrace();
		}
		
		return true;
	}
}
