/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.videoRecorder;
import java.util.List;


public interface WebCamControl {

	public void setSelectedCam(String DeviceID);
	
	public void setFormat(int Width, int Height);
	
	public boolean Start(); //Starts Capturing images
	
	public boolean Stopp(); //Ends Capturing images
	
	public void SetRecordPath(String Path);
	
	
	@SuppressWarnings("unchecked")
	public List getDeviceList(); //Returns stuff about the cams 
	
	@SuppressWarnings("unchecked")
	public List getSupportedFormats(int Device);
	
	public String getDeviceID(int Device);
	
}
