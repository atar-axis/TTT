/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.videoRecorder;
import java.util.List;


public interface WebCamControl {
	//the capture interface handles the incoming pictures (must be set)
	public void setCaptureInterface(CaptureInterface OnPic);
		
	public void setSelectedCam(String DeviceID);
	
	public void setFormat(int Width, int Height);
	
	public boolean Start(); //Starts Capturing images
	
	public boolean Stop(); //Ends Capturing images
	
	public void setRecordPath(String Path);
		
	public void release();
	
	public boolean CameraFound();
	 //true if at least one camera detected.
	
	@SuppressWarnings("unchecked")
	public List getDeviceList(); 
	//returns null if no camera found. else: returns the name/description of the cameras
	
	public List<TTTVideoFormat> getSupportedFormats(int Device);
	//returns null if no supported format found: else returns the formats supported by the specified camera.
	
	public String getDeviceID(int Device);
	//returns null if device not found
	
	public void setQuality(float Quality);//only numbers from 0.1(incl.) to 1(incl.) are valid.

	public float getQuality();
}
