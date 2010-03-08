package ttt.video;

import java.util.logging.Logger;

/**
 * 
 * @author ken lti-Civil
 * 
 */
// detects the OS (linux/mac/windows)
public final class OSUtils {
	@SuppressWarnings("deprecation")
	private static final Logger logger = Logger.global;
	public static class CameraException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Exception internal;
		public CameraException(Exception e){
			internal=e;
		}
		@Override
		public void printStackTrace() {
			System.err.println("CameraException occured because of ");
			internal.printStackTrace();
		}
	}
	public static WebCamControl obtainWebcam() throws CameraException{
		WebCamControl WBC = null;
		String cam = null;
		if (OSUtils.isLinux()) {
			cam = "ttt.video.LinuxCam";
		} else {
			cam = "ttt.video.WindowsCam";
		}
		try {
			Class<?> clazz = Class.forName(cam);
			WBC = (WebCamControl) clazz.getConstructors()[0].newInstance();
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println("Exception "+e+" occured while trying to create a "+cam);			
			throw new CameraException(e);			
		}
		return WBC;
	}

	private OSUtils() {
		super();
		logger.fine("OS: " + System.getProperty("os.name"));
	}

	public static final boolean isLinux() {
		return System.getProperty("os.name").equals("Linux");
	}

	public static final boolean isMacOSX() {
		return System.getProperty("os.name").equals("Mac OS X");
	}

	public static final boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	public static final boolean isSolaris() {
		return System.getProperty("os.name").equals("SunOS");
	}

}
