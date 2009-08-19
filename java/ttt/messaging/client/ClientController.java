package ttt.messaging.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ttt.messages.Annotation;
import ttt.messaging.poll.Poll;

/**
 * Controls the interaction between user and GUI. It's a action listener for the
 * GUI's buttons and provides methods for incoming messages/answers to
 * {@link ClientMessageParser}.
 * @author Thomas Doehring
 */
public class ClientController implements ActionListener {

	private ClientConnection conn;
	private JAnnotationPanel annotPanel;
	private TTTMessengerClient clGUI;
	
	
	public ClientController(JAnnotationPanel anP, TTTMessengerClient clGUI) 
	{
		this.annotPanel = anP;
		this.clGUI = clGUI;
	}
	
	/**
	 * called by {@link ClientConnection}, because connection is not yet available
	 * at creation time of {@code ClientController}
	 * @param conn
	 */
	void setConnection(ClientConnection conn) {
		this.conn = conn;
	}
	
	// listen for the GUI buttons' actions
	// @Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if(cmd.equals("send text")) {
			String txt = clGUI.getMessageText();
			if (txt.trim().length() > 0) conn.sendTextMessage(txt);
			
		} else if (cmd.equals("send text+sheet")) {
			byte[] imgData = annotPanel.getSheetImage();
			Annotation[] annots = annotPanel.getAllAnnotations();
			String txt = clGUI.getMessageText();
			conn.sendSheet(imgData, annots, txt);			
			
		} else if (cmd.equals("send sheet")) {
			byte[] imgData = annotPanel.getSheetImage();
			Annotation[] annots = annotPanel.getAllAnnotations();
			conn.sendSheet(imgData, annots, null);
			
		} else if (cmd.equals("getSheet")) {
			conn.fetchCurrentSheet(false);
			
		} else if (cmd.equals("getSheetAnn")) {
			conn.fetchCurrentSheet(true);
			
		} else if (cmd.equals("vote")) {
			conn.queryPolls();
		}
	}
	
	
	public void showSheet(BufferedImage bfImage, byte[] bImgData, Annotation[] annots) {
		annotPanel.showSheet(bfImage, bImgData, annots);
	}

	public void setScreenSize(int width, int height) {
		annotPanel.setScreenSize(width, height);
	}

	
	private LinkedList<Poll> tmpPolls;

	/**
	 * temporarily stores the received polls and trigger the viewing of them on EDT.
	 * @param polls  the polls
	 */
	public void showPolls(LinkedList<Poll> polls) {
		tmpPolls = polls;
		SwingUtilities.invokeLater(new Runnable() {
			// @Override
			public void run() {
				showPollDialog();
			}
		});
	}
	
	/**
	 * show the stored polls.
	 */
	private void showPollDialog() {
		if (tmpPolls.size() == 0) {
			JOptionPane.showMessageDialog(annotPanel, "No polls open.", "Voting", JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			VotePanelDialog dlgVote = new VotePanelDialog(tmpPolls);
			int result = dlgVote.showDialog(annotPanel);
			if (result == JOptionPane.OK_OPTION) {
				conn.castVotes(dlgVote.getVotes());
			}
		}
    }
}
