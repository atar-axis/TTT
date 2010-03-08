package ttt.video;

//'general' video format for the different linux(v4l4j/windows(lti-civil) formats
public class TTTVideoFormat {

	private int Height;
	private int Width;

	public int getHeight() {
		return Height;
	}

	public int getWidth() {
		return Width;
	}

	public TTTVideoFormat(int Width, int Height) {
		this.Height = Height;
		this.Width = Width;
	}
}
