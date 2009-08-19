package ttt.messaging;

/**
 * superclass of the messages sent by the messaging.
 * the attributes client IP and user name are set by the messaging parser.
 * @author Thomas Doehring
 */
public class TTTMessage {

	// deferred set by lecturer in message list
	private boolean deferred = false;
	public void setDeferred(boolean deferred) { this.deferred = deferred; }
	public boolean isDeferred() { return this.deferred; }
	
	private java.net.InetAddress clIP;
	/**
	 * get the IP of the client from which this message originated.
	 * @return client IP
	 */
	public java.net.InetAddress getClientIP() {
		return clIP;
	}
	public void setClientIP(java.net.InetAddress ip) {
		this.clIP = ip;
	}
	
	protected String userName = "";
	public void setUserName(String name) {
		this.userName = name;
	}
	/**
	 * get the (optional) name of the messaging client user.
	 * @return  user name if entered or empty string
	 */
	public String getUserName() { return this.userName; }
	public boolean hasUserName() { return this.userName != null && (this.userName.length() > 0); }
}
