package ttt.messaging.gui;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;
import ttt.messaging.*;

/**
 * The Model for the message list. It provides methods for changing the list of messages. 
 * @author Thomas Doehring
 */
public final class TTTMessageListModel extends AbstractListModel {
	
	public final static long serialVersionUID = 1L;

	private ArrayList<TTTMessage> msgList;
	private LinkedList<TTTMessage> toBeAddedList;
	
	// index to last non deferred message in list
	private int lastNonDeferredIdx = 0;
	
	public TTTMessageListModel() {
		msgList = new ArrayList<TTTMessage>();
		toBeAddedList = new LinkedList<TTTMessage>();
	}
	
	// @Override
	public Object getElementAt(int idx) {
		return msgList.get(idx);
	}

	// @Override
	public int getSize() {
		return msgList.size();
	}
	
	/**
	 * adds a TTTMessage to the list.
	 * If it isn't called from EDT, message is queued and insertion
	 * is triggered.
	 * @param msg TTTMessage to be added
	 */
	public void addMessage(TTTMessage msg) {
		// if EDT add message direct
		if (SwingUtilities.isEventDispatchThread()) {
			msgList.add(lastNonDeferredIdx, msg);
			lastNonDeferredIdx++;
			fireIntervalAdded(this, msgList.size() - 1, msgList.size() - 1);
		} else {
			// other thread -> schedule insertion
			synchronized(toBeAddedList) {
				toBeAddedList.offer(msg);
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					insertPendingMessages();
				}
			});
		}
	}
	
	/**
	 * deletes message with given index from list
	 * must be called from EDT
	 * @param idx  idx of message to be deleted
	 */
	public void deleteMessage(int idx) {
		if (SwingUtilities.isEventDispatchThread()) {
			msgList.remove(idx);
			if(idx < lastNonDeferredIdx) lastNonDeferredIdx--;
			fireIntervalRemoved(this, idx, idx);
		}
	}
	
	/**
	 * deletes all messages which are not deferred.
	 * must be called from EDT
	 */
	public void deleteAllNonDeferred() {
		if (SwingUtilities.isEventDispatchThread()) {
			int oldSize = msgList.size();
			if (oldSize > 0) {
				boolean[] deferred = new boolean[oldSize];
				for(int idx = 0; idx < msgList.size(); idx++) {
					deferred[idx] = msgList.get(idx).isDeferred();
				}
				for(int i = deferred.length - 1; i > -1; i--) {
					if(!deferred[i]) msgList.remove(i);
				}
				lastNonDeferredIdx = 0;
				fireIntervalRemoved(this, 0, oldSize);
			}
		}
	}
	
	/**
	 * deletes all messages from list
	 * must be called from EDT
	 */
	public void deleteAllMessages() {
		if (SwingUtilities.isEventDispatchThread()) {
			int oldSize = msgList.size();
			if (oldSize > 0) {
				msgList.clear();
				lastNonDeferredIdx = 0;
				fireIntervalRemoved(this, 0, oldSize - 1);
			}
		}
	}
	
	public void deleteAllFromUser(java.net.InetAddress ip) {
		if(SwingUtilities.isEventDispatchThread()) {
			int oldSize = msgList.size();
			if (oldSize > 0) {
				boolean[] fromUser = new boolean[oldSize];
				for(int idx = 0; idx < msgList.size(); idx++) {
					fromUser[idx] = msgList.get(idx).getClientIP().equals(ip);
				}
				for(int i = fromUser.length - 1; i > -1; i--) {
					if(fromUser[i]) {
						msgList.remove(i);
						if (i <= lastNonDeferredIdx) lastNonDeferredIdx--;
					}
				}
				fireIntervalRemoved(this, 0, oldSize);
			}
		}
	}
	
	/**
	 * deferred message is moved to back of list
	 * @param idx
	 */
	public void deferMessage(int idx) {
		TTTMessage msg = msgList.remove(idx);
		msg.setDeferred(true);
		msgList.add(msg);
		lastNonDeferredIdx--;
		// fireIntervalRemoved(this, idx, idx);
		fireContentsChanged(this, 0, msgList.size());
	}
	
	/**
	 * Inserts all messages from pendingQueue into list.
	 * It is triggered if addMessage is called from other thread
	 * than EDT.
	 */
	private void insertPendingMessages() {
		if (SwingUtilities.isEventDispatchThread()) {
			int insertCount = 0;
			synchronized(toBeAddedList) {
				TTTMessage msg = toBeAddedList.poll();
				while (msg != null) {
					msgList.add(lastNonDeferredIdx, msg);
					lastNonDeferredIdx++;
					insertCount++;
					msg = toBeAddedList.poll();
				}
			}
			if (insertCount > 0) {
				fireIntervalAdded(this, msgList.size() - insertCount, msgList.size() - 1);
			}
		}
	}

}
