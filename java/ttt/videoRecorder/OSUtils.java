package ttt.videoRecorder;

import java.util.logging.Logger;

/**
 * 
 * @author ken lti-Civil
 * 
 */
public final class OSUtils {
	@SuppressWarnings("deprecation")
	private static final Logger logger = Logger.global;

	public static WebCamControl obtainWebcam() {
		WebCamControl WBC = null;
		String cam = null;
		if (OSUtils.isWindows()) {
			cam = "WindowsCam";
		} else
			cam = "LinuxCam";
		try {
			Class clazz = Class.forName(cam);
			WBC = (WebCamControl) clazz.getConstructors()[0].newInstance(null);
		} catch (Exception e) {
			e.printStackTrace();
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
