package ttt.messaging.gui;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

import ttt.messaging.poll.*;

/**
 * own list model, which can be changed (standard list model is immutable).
 * Provides the needed methods by {@code AbstractListModel} and methods for changing
 * the entries in the list.
 * @author Thomas Doehring
 */
public class PollListModel extends AbstractListModel {
	
	public final static long serialVersionUID = 1L;

	private ArrayList<Poll> pollList;
	
	public PollListModel() {
		pollList = new ArrayList<Poll>(5);
	}
	
	// @Override
	public synchronized Object getElementAt(int index) {
		return pollList.get(index);
	}

	// @Override
	public synchronized int getSize() {
		return pollList.size();
	}
	
	/**
	 * add a new full poll to the list.
	 * (no forwarding to EDT needed, because method is only called when lecturer adds
	 * new full poll &rarr; GUI &rarr; already EDT)
	 * @param question  the question
	 * @param answers  the answers
	 */
	public synchronized void createNewFullPoll(String question, String[] answers) {
		int nextIdx = pollList.size();
		pollList.add(new FullPoll(nextIdx, question, answers));
		fireIntervalAdded(this, nextIdx, nextIdx);
	}
	
	/**
	 * add a new quick poll to the list.
	 * (no forwarding to EDT needed, because method is only called when lecturer adds
	 * new quick poll &rarr; GUI &rarr; already EDT)
	 * @param color  the color of the quick poll
	 * @param answerCount  number of answers
	 */
	public synchronized void createNewQuickPoll(int color, int answerCount) {
		int nextIdx = pollList.size();
		pollList.add(new QuickPoll(nextIdx, answerCount, color));
		fireIntervalAdded(this, nextIdx, nextIdx);
	}
	
	public synchronized void closePoll(int idx) {
		pollList.get(idx).close();
	}
	
	/**
	 * toggle the status of a poll between open and closed.
	 * @param idx  the index of the poll in the list
	 */
	public synchronized void togglePollStatus(int idx) {
		pollList.get(idx).toggleStatus();
		fireContentsChanged(this, idx, idx);
	}
	
	private LinkedList<Vote> votesToCast = new LinkedList<Vote>();
	
	/**
	 * cast the vote of a user.
	 * temporarily stores the votes of the user in a linked list and triggers the
	 * processing of them on the EDT (if not already called by EDT).
	 * @param ip  IP of user
	 * @param idx  the index of the poll
	 * @param answer  the number of the answer the user has chosen
	 */
	public synchronized void castVote(InetAddress ip, int idx, int answer) {
		if (SwingUtilities.isEventDispatchThread()) {
			if (idx < pollList.size()) {
				Poll p = pollList.get(idx);
				if(!p.hasAlreadyVoted(ip)) {
					pollList.get(idx).castVote(ip, answer);
					fireContentsChanged(this, idx, idx);
				}
			}
		} else {
			// save vote for casting later in EDT context
			votesToCast.add(new Vote(ip,idx,answer));
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					castPendingVotes();
				}
			});
		}
	}
	
	/**
	 * trigger method for the processing of the votes on EDT
	 */
	private synchronized void castPendingVotes() {
			for (Vote v : votesToCast) {
				if(v.PollIndex < pollList.size()) {
					Poll p = pollList.get(v.PollIndex);
					if (!p.hasAlreadyVoted(v.ClientIP)) {
						p.castVote(v.ClientIP, v.Answer);
					}
				}
			}
		fireContentsChanged(this, 0, pollList.size());
	}
	
	/**
	 * searches for all open polls in the list which the given user (identified by his IP)
	 * has not already voted on.
	 * @param ip  IP of the user
	 * @return a linked list containing the open polls for that user
	 */
	public synchronized LinkedList<Poll> getOpenPollsForUser(java.net.InetAddress ip) {
		LinkedList<Poll> llReturn = new LinkedList<Poll>();
		for (Poll p : pollList) {
			if (p.isOpen() && !p.hasAlreadyVoted(ip)) {
				llReturn.add(p);
			}
		}
		return llReturn;
	}

	/**
	 * helper class combining the index of a poll and 
	 * the index of an answer
	 */
	class Vote {
		Vote(InetAddress ip, int idx, int answer) {
			this.ClientIP = ip;
			this.PollIndex = idx;
			this.Answer = answer;
		}
		InetAddress ClientIP = null;
		int PollIndex = -1;
		int Answer = -1;
	}

}
