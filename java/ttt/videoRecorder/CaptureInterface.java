package ttt.videoRecorder;

//handles the incoming pictures for linuxcam and windowscam
public interface CaptureInterface {
	public void onNewImage(byte[] image, String RecordPath, float Quality);
}
