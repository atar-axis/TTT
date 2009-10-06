/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.videoRecorder;

import java.util.LinkedList;
import java.util.List;
import com.lti.civil.CaptureDeviceInfo;
import com.lti.civil.CaptureException;
import com.lti.civil.CaptureObserver;
import com.lti.civil.CaptureStream;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;
import com.lti.civil.Image;
import com.lti.civil.VideoFormat;


public class WindowsCam implements WebCamControl {
	
	public static boolean isRecording = false;
	public static String RecordPath = "C:\\TTTImplement\\";
	public String SelectedCamID; //Remembers the currently selected cam
	public static int SelectedFormat; //Remembers the currently selected format
	CaptureStream StartcaptureStream; //The recording stream
	CaptureSystemFactory factory = DefaultCaptureSystemFactorySingleton.instance();
 	CaptureSystem system = factory.createCaptureSystem();
	
	@SuppressWarnings("unchecked")
	public WindowsCam () throws CaptureException{
		
		system.init();
		List list = system.getCaptureDeviceInfoList();
		for (int i = 0; i < list.size(); ++i) {
			CaptureDeviceInfo info = (CaptureDeviceInfo) list.get(i);
			
			SelectedCamID = info.getDeviceID();			
		}			
	}
	
	@SuppressWarnings("unchecked")
	public List getSupportedFormats(int Device){
		List<String> SupportedFormats = new LinkedList<String>();
		try {
			StartcaptureStream = system.openCaptureDeviceStream(getDeviceID(Device));
			StartcaptureStream.setObserver(new MyCaptureObserver());
			for (VideoFormat format : StartcaptureStream.enumVideoFormats()){
				SupportedFormats.add(videoFormatToString(format));				
			}		
			
			return SupportedFormats;
		} catch (CaptureException e) {		
			e.printStackTrace();
		}
		
		return null;
		}
			
	public static String videoFormatToString(VideoFormat f)
	{
		return "Type=" + formatTypeToString(f.getFormatType()) + " Width=" + f.getWidth() + " Height=" + f.getHeight() + " FPS=" + f.getFPS(); 
	}
	
	private static String formatTypeToString(int f)
	{
		switch (f)
		{
			case VideoFormat.RGB24:
				return "RGB24";
			case VideoFormat.RGB32:
				return "RGB32";
			default:
				return "" + f + " (unknown)";
		}
	}	

	@SuppressWarnings("unchecked")
	@Override
	public String getDeviceID(int Device) {
		try {
			List<CaptureDeviceInfo> z = (List<CaptureDeviceInfo>) system.getCaptureDeviceInfoList();
		return z.get(Device).getDeviceID();		
		
		
		} catch (CaptureException e) {	
			e.printStackTrace();
		}		
		return null;
	}
		
	@SuppressWarnings("unchecked")
	public List getDeviceList()  {
		try {
			List<CaptureDeviceInfo> z = (List<CaptureDeviceInfo>) system.getCaptureDeviceInfoList();
		List<String> DeviceList = new LinkedList();
			for(CaptureDeviceInfo x :  z){
			DeviceList.add( x.getDescription());
			}
			
			return  DeviceList;
		} catch (CaptureException e) {		
			e.printStackTrace();
		}
		return null;
	}
	
	public String getSelectedCam(){
		return SelectedCamID;
	}
		
	public void setCurrentCameraformat(String getDeviceID) throws CaptureException{		
		system.init(); 
		
		CaptureStream captureStream;
		try {
			captureStream = system.openCaptureDeviceStream(getDeviceID);
			captureStream.setObserver(new MyCaptureObserver());
				
		} catch (CaptureException e1) {
			e1.printStackTrace();
		}
	}
	
	@Override
	public boolean Start() {
		
		if(!isRecording){
		try {			
			system.init();
			
			StartcaptureStream = system.openCaptureDeviceStream(SelectedCamID);
			StartcaptureStream.setObserver(new MyCaptureObserver());
			StartcaptureStream.setVideoFormat(StartcaptureStream.enumVideoFormats().get(SelectedFormat));
		//	System.out.println(StartcaptureStream.enumVideoFormats().get(SelectedFormat));
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
	public boolean Stopp() {
		try {
			if(isRecording){
			StartcaptureStream.stop();
			StartcaptureStream.dispose();
	
			System.out.println("aufnahmestopp");
			isRecording = false;
			}
		} catch (CaptureException e1) {
			e1.printStackTrace();
		}
		return false;
	}

	@Override
	public void setSelectedCam(String DeviceID) {
		SelectedCamID = DeviceID;		
	}

	@Override
	public void setFormat(int Width, int Height) {	
		int i = 0;
		try {
			CaptureStream captureStream;
			captureStream = system.openCaptureDeviceStream(SelectedCamID);
			
			for (VideoFormat format : captureStream.enumVideoFormats())
			{				
				if(format.getWidth()==Width && format.getHeight()==Height){
				
				System.out.println("Choosing format: " + videoFormatToString(format));
			//	StartcaptureStream.setVideoFormat(format);
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
	public void SetRecordPath(String Path) {
		RecordPath =Path;		
	}
	
}


//
//gets called when a new picture is made. it calls OnCapture
//
class MyCaptureObserver implements CaptureObserver {
	public void onError(CaptureStream sender, CaptureException e)
	{	System.err.println("onError " + sender);
		e.printStackTrace();
	}
	public void onNewImage(CaptureStream sender, Image image)
	{	
		OnCapture SafePic = new OnCapture();
		//OnCapture.RecordPath = WindowsCam.RecordPath;
		SafePic.onNewImage(image, WindowsCam.RecordPath);
	}
	
}
