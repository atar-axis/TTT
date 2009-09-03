/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.videoRecorder;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.lang.Thread;

import javax.swing.ImageIcon;

import com.lti.civil.Image;

import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

//This classs set and get methods are completly unfished!
public class LinuxCam implements WebCamControl, Runnable {
	public static String RecordPath = "C:\\TTTImplement\\";
    private static VideoDevice vd;   
    private static FrameGrabber fg;
    private Thread captureThread;
    public static boolean isRecording = false;
    
   public LinuxCam() throws V4L4JException{   
    String dev = "/dev/video0";
        
    int w=160, h=120, std=0, channel = 0, qty = 60;
  
    initFrameGrabber(dev, w, h, std, channel, qty);
    }
	
  /* Shouldn't be needed but untetested...
   public void setImage(byte[] b) {
      GUIMain.myLabel.setIcon(new ImageIcon(b));
   }*/
   
   /**
    * is called in start by the new capture thread stuff 
    */
   public void run(){
       ByteBuffer bb;
       byte[] b;
       try {       
     		//OnCapture.RecordPath = RecordPath;
               while(isRecording){
                       bb = fg.getFrame();
                       b = new byte[bb.limit()];
                       bb.get(b);
               		OnCapture SafePic = new OnCapture();     
             
            		SafePic.onNewImage((Image) new ImageIcon(b).getImage(),RecordPath);
                      // setImage(b);
               }
       } catch (V4L4JException e) {
               e.printStackTrace();
               System.out.println("Failed to capture image");
       }
   }
   
   
   private void initFrameGrabber(String dev, int w, int h, int std, int channel, int qty) throws V4L4JException{
      vd = new VideoDevice(dev);
       fg = vd.getJPEGFrameGrabber(w, h, channel, std, qty);
   }
   
	@Override
	public boolean Start() {
		try {
			if(!isRecording){
			fg.startCapture();
			   captureThread = new Thread(this, "Capture Thread");
			    captureThread.start();
			isRecording = true;			
			}
		} catch (V4L4JException e) {			
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public boolean Stopp() {
	if(isRecording){
		fg.stopCapture();
	 isRecording =false;	
	// OnCapture.ZipOnStop();
	}
	
		return false;
	}

	//returns the intern device id
	@Override
	public String getDeviceID(int Device) {	
		try {
			return vd.getDeviceInfo().toString();
		} catch (V4L4JException e) {			
			e.printStackTrace();
		}
		return null;
	}

	//it can currently handle only the default cam
	@SuppressWarnings("unchecked")
	@Override
	public List getDeviceList() {
		List lista = new LinkedList();
		lista.add(vd.getDevicefile());
		return lista;
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public List getSupportedFormats(int Device) {
		List empty = new LinkedList();
		empty.add("Width=160 Height=120");
		return empty;
	}

	//TODO Implement set Format
	@Override
	public void setFormat(int Width, int Height) {
	}

	@Override
	public void setSelectedCam(String DeviceID) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void SetRecordPath(String path) {
		RecordPath = path;		
	}
	
	
}
