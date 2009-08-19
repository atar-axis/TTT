package ttt.messaging.server;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedList;

import ttt.RfbProtocol;
import ttt.messages.Annotation;
import ttt.messages.HighlightAnnotation;
import ttt.messages.ImageAnnotation;
import ttt.messages.TextAnnotation;
import ttt.messaging.gui.JTTTMessageList;
import ttt.messaging.gui.PollListModel;
import ttt.messaging.gui.TTTMessageListModel;
import ttt.messaging.poll.*;
import ttt.messaging.*;

/**
 * controls the interactions between {@link TTTMessageListModel}, {@link PollListModel},
 * {@link TTTMessengerConnection}s and TTT ({@link RfbProtocol}).
 * It does the conversions to the XML message format. It does also convert polls to
 * TTT annotations if the poll results should be shown in TTT.
 * @author Thomas Doehring
 */
public class MessagingController {

	private TTTMessageListModel msgListModel;
	private JTTTMessageList msgList;
	private PollListModel pollListModel;
	private RfbProtocol tttLink;

	final String responseSize;
	
	// stores the IPs of users who are blocked by lecturer
	private HashSet<InetAddress> blockedUsers = new HashSet<InetAddress>(); 
	
	public MessagingController(JTTTMessageList list,
			PollListModel pollModel, RfbProtocol ttt) {
		this.msgList = list;
		this.msgListModel = list.getModel();
		this.pollListModel = pollModel;
		this.tttLink = ttt;
		
		responseSize = "<tttmessage type=\"response\"><screenSize width=\""
			+ tttLink.prefs.framebufferWidth + "\" height=\""
			+ tttLink.prefs.framebufferHeight + "\"/></tttmessage>\n";
	}
	
	/**
	 * fetches current sheet from TTT teaching session
	 * @return TTTSheetMessage with current sheet
	 */
	public TTTSheetMessage getCurrentSheet() {
		BufferedImage sheet = null;
		Annotation[] annots = null;
		if(!tttLink.isWhiteboardEnabled()) {
			sheet = tttLink.getScreenshotWithoutAnnotations();
		}
		annots = tttLink.getCurrentAnnotationsAsArray();
		return new TTTSheetMessage(sheet, annots);
	}
	
	public void addMessage(TTTMessage msg) {
		if (!blockedUsers.contains(msg.getClientIP())) {
			msgListModel.addMessage(msg);
		}
	}
	
	// ****** POLL management ******* //
	
	public String getOpenPollsForUserAsXML(java.net.InetAddress ip) {
		// return pollManager.getOpenPolls();
		LinkedList<Poll> llPolls = pollListModel.getOpenPollsForUser(ip);
		StringBuilder sb = new StringBuilder("<tttmessage type=\"response\"><polls>\n");
		for (Poll poll : llPolls) {
			sb.append(poll.getXML());
		}
		sb.append("\n</polls></tttmessage>");
		return sb.toString();
	}
	
	public LinkedList<Poll> getOpenPollsForUser(java.net.InetAddress ip) {
		return pollListModel.getOpenPollsForUser(ip);
	}
	
	public void castVote(InetAddress ip, int vID, int answer) {
		pollListModel.castVote(ip, vID, answer);
	}
	
	public void createFullPoll(String question, String[] answers) {
		pollListModel.createNewFullPoll(question, answers);
	}
	
	public void createQuickPoll(int color, int answerCount) {
		pollListModel.createNewQuickPoll(color, answerCount);
	}
	
	/**
	 * show the poll in TTT.
	 * @param p the poll
	 */
	public void showPoll(Poll p) {
		if (p instanceof QuickPoll) {
			createQuickPollAnnotations((QuickPoll)p);
		} else if (p instanceof FullPoll) {
			createFullPollAnnotations((FullPoll)p);
		}
	}
	
	/**
	 * show the quick poll results in TTT. Convert the results to annotations.
	 * @param qp  quick poll to display in TTT
	 */
	private void createQuickPollAnnotations(QuickPoll qp) {
		int maxWidth = tttLink.prefs.framebufferWidth - 150;
		int[] votesPromille = qp.getPromilleResult();
		int[] votes = qp.getResult();
		int color = qp.getColor();
		int y = tttLink.prefs.framebufferHeight - votesPromille.length * 50;
		// draw bars at bottom
		DecimalFormat df = new DecimalFormat("##0.0");
		for(int i = 0; i < votesPromille.length; i++) {
			int barWidth = maxWidth * votesPromille[i] / 1000;
			HighlightAnnotation ha = new HighlightAnnotation(0, color+3, 75, y, 75+barWidth, y+20);
			tttLink.handleMessage(ha);
			Double prom = votesPromille[i] / 10.0;
			String sprom = df.format(prom) + "%";
			TextAnnotation ta1 = new TextAnnotation(0, Annotation.Black, 15, y, 0, sprom);
			tttLink.handleMessage(ta1);
			TextAnnotation ta2 = new TextAnnotation(0, Annotation.Black, tttLink.prefs.framebufferWidth - 40, y, 0, String.valueOf(votes[i]));
			tttLink.handleMessage(ta2);
			y += 50;
		}
	}
	
	/**
	 * show the full poll results in TTT. Converts text and results to TTT annotations.
	 * @param poll  Full Poll to display.
	 */
	private void createFullPollAnnotations(FullPoll poll) {
		tttLink.newWhiteboardPage();
		
		int maxWidth = tttLink.prefs.framebufferWidth - 150;
		TextAnnotation taQ = new TextAnnotation(0, Annotation.Black, 50, 20, maxWidth, poll.getQuestion());
		tttLink.handleMessage(taQ);

		int y = 80;
		String[] answers = poll.getAnswers();
		int[] votes = poll.getResult();
		int[] votesPromille = poll.getPromilleResult();
		DecimalFormat df = new DecimalFormat("##0.0");
		for(int i = 0; i < answers.length; i++) {
			TextAnnotation taA = new TextAnnotation(0, Annotation.Black, 50, y, maxWidth, answers[i]);
			tttLink.handleMessage(taA);
			y += 40;
			int barWidth = maxWidth * votesPromille[i] / 1000;
			HighlightAnnotation bar = new HighlightAnnotation(0, Annotation.Blue+3, 75, y, 75 + barWidth, y+30);
			tttLink.handleMessage(bar);
			Double prozent = votesPromille[i] / 10.0;
			String sprozent = df.format(prozent) + "%";
			TextAnnotation taProzent = new TextAnnotation(0, Annotation.Black, 15, y, 0, sprozent);
			tttLink.handleMessage(taProzent);
			TextAnnotation taVotes = new TextAnnotation(0, Annotation.Black, tttLink.prefs.framebufferWidth - 40, y, 0, String.valueOf(votes[i]));
			tttLink.handleMessage(taVotes);
			y += 40;
		}
	}
	
	// ****** MESSAGE management ****** //
	
	public void deleteMessage(int i) {
		msgListModel.deleteMessage(i);
	}
	
	public void deleteAllMessages() {
		msgListModel.deleteAllMessages();
	}
	
	public void deleteAllNonDeferred() {
		msgListModel.deleteAllNonDeferred();
	}
	
	/**
	 * show the message in TTT. Converts the (content) message to applicable annotations.
	 * @param msg  the message
	 */
	public void showMessage(TTTMessage msg) {
		if(msg instanceof TTTTextMessage) {
			TTTTextMessage msgText = (TTTTextMessage)msg;
			TextAnnotation ta = new TextAnnotation(0, Annotation.Black, 50, 20, tttLink.prefs.framebufferWidth - 100, msgText.getText());
			tttLink.handleMessage(ta);
			
		} else if (msg instanceof TTTSheetMessage) {
			TTTSheetMessage msgSheet = ((TTTSheetMessage)msg);
			// display sheet on new whiteboard
			tttLink.nextWhiteboardPage();
			if (!msgSheet.isWhiteboardSheet()) {
				ImageAnnotation imgAnn = new ImageAnnotation(0, 0, 0, msgSheet.getImageData());
				tttLink.handleMessage(imgAnn);
			}
			if (msgSheet.hasAnnotations()) {
				tttLink.handleMessage(msgSheet.getAnnotations());
			}
			if (msgSheet.hasText()) {
				TextAnnotation ta = new TextAnnotation(0, Annotation.Black, 50, 20, tttLink.prefs.framebufferWidth - 100, msgSheet.getText());
				tttLink.handleMessage(ta);
			}
		}
	}
	
	/**
	 * block the user which sent the currently selected message.
	 * Deletes all messages from that user and all further (content) messages from
	 * this user are discarded.
	 */
	public void blockUser() {
		TTTMessage msg = (TTTMessage)msgList.getSelectedValue();
		if (msg != null) {
			InetAddress blockIP = msg.getClientIP();
			blockedUsers.add(blockIP);
			msgListModel.deleteAllFromUser(blockIP);
		}
	}
	
	// ****** MISC ****** //
	
	public String getScreenSizeAsXML() {
		return responseSize;
	}
	
	public Dimension getScreenSize() {
		return new Dimension(tttLink.prefs.framebufferWidth, tttLink.prefs.framebufferHeight);
	}
}
