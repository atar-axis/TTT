package ttt.messaging.poll;

import java.net.InetAddress;
import java.util.HashSet;

/**
 * super class for both types ({@link FullPoll} and {@link QuickPoll}).
 * Every poll gets a unique ID during a TTT session. The number of votes for each
 * answer is stored. Also the IP of every user which voted on the poll is saved to
 * block further multiple votes from same user on same poll.
 * @author Thomas Doehring
 */
public abstract class Poll {

	int id = 0;
	private boolean closed = true;
	int[] votes;
	// contains the ratios between each the number of votes for each answer and
	// the total sum of votes (updated with every vote)
	int[] votesPromille;
	HashSet<InetAddress> clientsVoted = new HashSet<InetAddress>(50);
	
	public int getID() {
		return this.id;
	}
	
	/**
	 * @return the number of answers of the poll
	 */
	public abstract int getAnswerCount();
	
	// status of poll
	public boolean isOpen() { return !this.closed; }
	public void close() {
		this.closed = true;
	}
	public void toggleStatus() {
		this.closed = !this.closed;
	}
	
	/**
	 * cast a vote on this poll.
	 * @param ip  IP of the user
	 * @param answer  number of the answer
	 */
	public void castVote(InetAddress ip, int answer) {
		if (!clientsVoted.contains(ip) && !closed) {
			if(answer < votes.length) {
				votes[answer]++;
				calculatePromille();
			}
			clientsVoted.add(ip);
		}
	}
	
	/**
	 * @return number of votes for each answer
	 */
	public int[] getResult() {
		return votes;
	}
	
	public int[] getPromilleResult() {
		return votesPromille;
	}
	
	private void calculatePromille() {
		int sumOfVotes = 0;
		for(int i = 0; i < votes.length; i++) {
			sumOfVotes += votes[i];
		}
		if (sumOfVotes > 0) {
			for(int i = 0; i < votesPromille.length; i++) {
				votesPromille[i] = 1000 * votes[i] / sumOfVotes;
			}
		}
	}
		
	public abstract String getXML();

	/**
	 * check if user has already taken part in that poll.
	 * @param ip  IP of the user
	 * @return  true if user has already cast a vote on that poll
	 */
	public boolean hasAlreadyVoted(InetAddress ip) {
		return clientsVoted.contains(ip);
	}
}
