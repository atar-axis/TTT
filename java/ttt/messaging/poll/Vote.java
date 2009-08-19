package ttt.messaging.poll;

import java.net.InetAddress;

/**
 * helper class combining the ID of a poll and 
 * the index of an answer
 */
public class Vote {

	// TODO: change index to id
	
	public Vote(InetAddress ip, int id, int answer) {
		this.ClientIP = ip;
		this.PollID = id;
		this.Answer = answer;
	}
	
	public Vote(int id, int answer) {
		this.PollID = id;
		this.Answer = answer;
	}
	
	InetAddress ClientIP = null;
	int PollID = -1;
	int Answer = -1;

	public String toXML() {
		return "<vote id=\"" + PollID + "\" answer=\"" + Answer + "\" />\n";
	}
}
