package ttt.messaging.server;
import java.net.Socket;
import java.io.*;

import ttt.messaging.server.MessagingController;

/**
 * A connection to a client. It's subclass of Thread and the {@link TTTMessengerServer}
 * starts it.
 * Reading is done with a BufferedReader. That is why a message must have a line feed at the end.
 * @author Thomas Doehring
 */
public class TTTMessengerConnection extends Thread {

	private Socket sckt = null;
	private OutputStreamWriter out = null;
	private BufferedReader in = null;
	
	private String userName = "";
	
	private ServerMessageParser parser;

//	private static final String CLOSE_MESSAGE = "<tttmessage type=\"command\"><closeConnection/></tttmessage>\n";

	public TTTMessengerConnection(Socket socket, MessagingController ctrl){
		this.sckt = socket;
		
		this.setName("MsgConn");
		
		this.parser = new ServerMessageParser(sckt.getInetAddress(), this, ctrl);
	}
	
	private boolean finished;
	
	/**
	 * receiver thread.
	 * read lines and put them together to a message
	 * until an ending {@literal </tttmessage>} is received, than pass it to parser
	 */
	public void run() {

		try {
			out = new OutputStreamWriter(sckt.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sckt.getInputStream()));
			
			StringBuilder sb = new StringBuilder();
			
			while(!finished) {
				String line = in.readLine();
				if (line == null)
					finished = true;
				else {
					sb.append(line);
					if(line.endsWith("</tttmessage>")) {
						// process message
						String response = parser.parseMessage(sb.toString());
						sendMessage(response);
						sb = new StringBuilder();
						if(response.equals("CLOSE")) {
							finished = true;
						}
					}
				}
			}
		} catch (IOException ioe) {
			System.err.println("foo");
			ioe.printStackTrace();
		} finally {
			try { close(); cleanUp(); } catch (IOException ioe2) { /* ignore */ }
		}
	}
	
	private void cleanUp() throws IOException {
		if (sckt != null) {
			sckt.close();
			sckt = null;
		}
		if (out != null) {
			out.close();
			out = null;
		}
		if (in != null){
			in.close();
			in = null;			
		}
	}
	
	/**
	 * sends a message
	 * @param s  complete XML message string 
	 */
	public void sendMessage(String s) {
		if (s != null && s.length() > 0) {
			try {
				out.write(s);
				if (s.charAt(s.length() - 1) != '\n') out.write('\n');
				out.flush();
			} catch (IOException ioe) { /* ignore */ }
		}
	}
	
	public void close()
	{
		// sendMessage(CLOSE_MESSAGE);
		try { cleanUp(); } catch (IOException e) { /* ignore */ }
	}
	
	/**
	 * get IP of remote machine, i.e. the IP which identifies the user.
	 * @return
	 */
	public java.net.InetAddress getIP() {
		return this.sckt.getInetAddress();
	}
	
	public void setUserName(String name) { this.userName = name; }
	public String getUserName() { return this.userName; }
	public boolean userNameAvailable() { return this.userName.length() > 0; }
}
