package ttt.postprocessing.html5;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class CountingBufferedWriter extends BufferedWriter {
	private long bytesWritten = 0;
	
	public CountingBufferedWriter(Writer out) {
		super(out);
	}
	
	public CountingBufferedWriter(Writer out, int sz) {
		super(out, sz);
	}
	
	public void write(int c) throws IOException {
		super.write(c);
		this.increaseBytesWritten(1);
	}
	
	public void write(char cbuf[], int off, int len) throws IOException {
		super.write(cbuf, off, len);
		this.increaseBytesWritten(len-off);
	}
	
	public void write(String s, int off, int len) throws IOException {
		super.write(s, off, len);
		this.increaseBytesWritten(s.substring(off, off+len).getBytes().length);
	}
	
	private void increaseBytesWritten(long inc) {
		this.bytesWritten += inc;
	}
	
	public long getBytesWritten() {
		return this.bytesWritten;
	}
	
}
