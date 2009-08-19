package ttt.messaging.client;

import ttt.helper.Base64Codec;
import ttt.messages.Annotation;
import ttt.messaging.poll.Vote;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Connection to the messaging server via Socket. Reads from socket stream and calls
 * {@link ClientMessageParser#parseMessage(String)} for complete XML messages.
 * Provides methods for the communication to the server. These methods do the conversion
 * to XML.
 * @author Thomas Doehring
 */
public class ClientConnection extends Thread {
	
	private Socket sckt;
	private boolean finished;
	
	private OutputStreamWriter out = null;
	private BufferedReader in = null;
	
	private ClientMessageParser parser = null;
		
	public ClientConnection(Socket s, ClientController cc)
	{
		this.setName("ClConn Thrd");
		sckt = s;
		cc.setConnection(this);
		parser = new ClientMessageParser(cc);
		try {
			out = new OutputStreamWriter(sckt.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sckt.getInputStream()));
		} catch (IOException ioe) {
			finished = true;
			try { cleanUp(); } catch (IOException ioe2) { /* ignore */ }
		}
	}
	
	public void run() {
		StringBuilder sb = new StringBuilder();
		
		try {
			while(!finished){
				String line = in.readLine();
				if (line == null)
					finished = true;
				else {
					sb.append(line);
					if(line.endsWith("</tttmessage>")) {
						// process message and get response
						String response = parser.parseMessage(sb.toString());
						if(response.equals("CLOSE")) {
							finished = true;
						} else if (response.length() > 0) {
							// send response
							sendMessage(response);
						}
						sb = new StringBuilder();
					}
				}
			}
		} catch (IOException ioe) {
			finished = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { cleanUp(); } catch (IOException ioe2) { /* ignore */ }
		}
	}
	
	private synchronized void sendMessage(String msg) {
		if (msg != null && msg.length() > 0) {
			try {
				out.write(msg);
				if(msg.charAt(msg.length() -1) != '\n') out.write("\n");
				out.flush();
			} catch (IOException ioe){
				finished = true;
			}
		}
	}
	
	// private final String CLOSE_CONNECTION = "<tttmessage type=\"command\"><closeConnection /></tttmessage>\n";
	public void close() {
		this.finished = true;
		// sendMessage(CLOSE_CONNECTION);
		try { cleanUp(); } catch (IOException ioe) { /* ignore */ }
	}
	
	private void cleanUp() throws IOException
	{
		if(sckt != null) sckt.close();
		if(in != null) {
			in.close();
		}
		if(out != null) out.close();
	}
	
	/**
	 * sends a text message to server.
	 * @param txt the message
	 */
	public void sendTextMessage(String txt) {
		StringBuilder sb = new StringBuilder("<tttmessage type=\"content\">\n");
		sb.append("<text>").append(txt).append("</text>");
		sb.append("</tttmessage>");
		sendMessage(sb.toString());
	}
	
	/**
	 * sends a sheet to server. Each parameter may be null, if message should not contain
	 * the accordant message part.
	 * @param imgData  sheet image
	 * @param annots  annotations
	 * @param txt  text message
	 */
	public void sendSheet(byte[] imgData, Annotation[] annots, String txt) {
		StringBuilder sb = new StringBuilder("<tttmessage type=\"content\">\n<sheet>\n");
		if (imgData != null) {
			sb.append("<bgimage data=\"");
			sb.append(Base64Codec.encodeToString(imgData));
			sb.append("\" />\n");
		}
		if (annots != null) {
			sb.append("<annotations>\n");
			for (Annotation ann : annots) {
				sb.append(ann.getXMLString());
			}
			sb.append("</annotations>\n");
		}
		if (txt != null && txt.length() > 0) {
			sb.append("<text>").append(txt).append("</text>\n");
		}
		sb.append("</sheet>\n</tttmessage>");
		sendMessage(sb.toString());
	}
	
	/**
	 * fetch the current sheet with or without annotations from server
	 * @param annotated  with or without annotations?
	 */
	public void fetchCurrentSheet(boolean annotated) {
		if (annotated) sendMessage("<tttmessage type=\"command\"><getCurrentSheet annotated=\"true\" /></tttmessage>\n");
		else sendMessage("<tttmessage type=\"command\"><getCurrentSheet annotated=\"false\" /></tttmessage>");
	}

	/**
	 * fetch all open polls 
	 */
	public void queryPolls() {
		String queryPolls = "<tttmessage type=\"command\"><queryPolls/></tttmessage>\n";
		sendMessage(queryPolls);
	}
	
	/**
	 * send votes to server
	 * @param llVotes  LinkedList containing the votes
	 */
	public void castVotes(LinkedList<Vote> llVotes) {
		StringBuilder sb = new StringBuilder("<tttmessage type=\"command\"><castVotes>\n");
		for (Vote vote : llVotes) {
			sb.append(vote.toXML());
		}
		sb.append("</castVotes></tttmessage>\n");
		sendMessage(sb.toString());
	}
	
	/**
	 * fetch size of VNC session from server
	 */
	public void querySize() {
		String querySize = "<tttmessage type=\"command\"><queryScreenSize/></tttmessage>\n";
		sendMessage(querySize);
	}
	
	/**
	 * send the name of user to server
	 * @param name  user name
	 */
	public void sendName(String name) {
		String sendName = "<tttmessage type=\"command\"><name>" + name
		+ "</name></tttmessage>\n";
		sendMessage(sendName);
	}
}
