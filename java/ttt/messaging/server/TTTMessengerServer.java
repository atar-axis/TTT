package ttt.messaging.server;

import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

import java.util.LinkedList;

import ttt.messaging.server.MessagingController;

/**
 * waits for incoming connections and dispatches a new receiving thread for each
 * new connection
 * @author Thomas Doehring
 */
public class TTTMessengerServer {

	private boolean finished = false;	
	private LinkedList<TTTMessengerConnection> connections;
	// private Thread srvThread;
	private ServerSocket serv;
	
	private InetAddress serverIP = null;
	
	// save controller instance for distribution to the connections
	MessagingController controller;
	
	public TTTMessengerServer(MessagingController ctrl, java.net.InetAddress ip)
	{
		this.controller = ctrl;
		this.serverIP = ip;
		this.connections = new LinkedList<TTTMessengerConnection>();
	}
	
	public String start() {
		try {
			serv = new ServerSocket(7777, 50, serverIP);
		} catch (IOException ioe) {
			return "Could not create Server Socket. " + ioe.toString();
		}
		
		new Thread(new Runnable() {
			public void run() {
				serverLoop();
			}
		}).start();
		
		return null;
	}

	public void stop()
	{
		finished = true;
		// close all connections
		for (TTTMessengerConnection conn : connections) {
			conn.close();
		}
		if (serv != null) {
			try { serv.close(); } catch (IOException ioe) { /* ignore */ }
		}
	}

	/**
	 * loop waits for incoming connections and creates new socket and connection instance
	 * for it.
	 */
	private void serverLoop()
	{
		try {
			while(!finished){
				Socket s = serv.accept();
				TTTMessengerConnection conn = new TTTMessengerConnection(s,controller);
				conn.start();
				connections.add(conn);
			}
		} catch (IOException ioe) {
			finished = true;
			try { serv.close(); } catch (IOException ioe2) { /*ignore*/ }
		}
	}
		
}
