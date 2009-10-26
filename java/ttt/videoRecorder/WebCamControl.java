/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.videoRecorder;
import java.util.List;


public interface WebCamControl {
	//the capture interface handles the incoming pictures (must be set)
	public void setCaptureInterface(CaptureHandler OnPic);
		
	public void setQuality(float Quality);//only numbers from 0.1(incl.) to 1(incl.) are valid.
	
	public void setSelectedCam(String DeviceID) throws SetCameraException;
	
	public void setFormat(int Width, int Height);
	
	public void setRecordPath(String Path);
	
	@SuppressWarnings("unchecked")
	public List getDeviceNames(); 
	//returns null if no camera found. else: returns the name/description of the cameras
	
	public List<TTTVideoFormat> getSupportedFormats(int Device) throws CameragetFormatsException;
	//returns null if no supported format found: else returns the formats supported by the specified camera.
	
	public String getDeviceName(int Device);
	//returns null if device not found	
	
	public float getQuality(); //default quality is 0,1f
	
	public boolean Start() throws CameraStartException; //Starts Capturing images
	
	public boolean Stop() throws CameraStopException; //Ends Capturing images
		
	public void release();
	
	public boolean CameraFound();
	 //true if at least one camera detected.
	
	
}
final class CameraStartException extends Exception{
	private static final long serialVersionUID = 1L;
	Exception internal;
	public void CameraException(Exception e){
		internal=e;
	}
	@Override
	public void printStackTrace() {
		System.err.println("CameraStartException occured because of ");
		internal.printStackTrace();
	}
}

final class SetCameraException extends Exception{
	private static final long serialVersionUID = 1L;
	Exception internal;
	public void CameraException(Exception e){
		internal=e;
	}
	@Override
	public void printStackTrace() {
		System.err.println("Couldn't select Camera because of ");
		internal.printStackTrace();
	}
	
}

final class CameraStopException extends Exception{
	private static final long serialVersionUID = 1L;
	Exception internal;
	public void CameraException(Exception e){
		internal=e;
	}
	@Override
	public void printStackTrace() {
		System.err.println("CameraStopException occured because of ");
		internal.printStackTrace();
	}
}
final class CameragetFormatsException extends Exception{
	private static final long serialVersionUID = 1L;
	Exception internal;
	public void CameraException(Exception e){
		internal=e;
	}
	@Override
	public void printStackTrace() {
		System.err.println("CameragetFormatsException occured because of ");
		internal.printStackTrace();
	}
}
final class CameragetDevicelistException extends Exception{
	private static final long serialVersionUID = 1L;
	Exception internal;
	public void CameraException(Exception e){
		internal=e;
	}
	@Override
	public void printStackTrace() {
		System.err.println("CameragetDevicelistException occured because of ");
		internal.printStackTrace();
	}
}

