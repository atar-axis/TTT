package ttt.messaging.client;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;

import ttt.messaging.poll.*;

/**
 * Displays the open polls and the user can cast his/her votes. It uses 
 * {@code JOptionPane.showOptionDialog()} to display itself as a dialog.
 * Each poll has it's own panel and the poll panels are arranged with a
 * vertical BoxLayout.
 * @author Thomas Doehring
 */
public class VotePanelDialog extends JPanel {
	
	final static long serialVersionUID = 1L;

	JRadioButton[][] rbVotes;
	
	private LinkedList<Poll> polls;
	
	// unused colors default to white
	// (can't use Annotation.colors b/c of bug when using transparent
	// colors with RadioButtons
	final static Color[] QUICK_POLL_COLOR = new Color[] {
		Color.white, Color.white, Color.white,
		Color.white, new Color(180,180,180), Color.white,
		Color.white, new Color(180,180,255), new Color(255,180,180),
		new Color(180,255,180), Color.white, new Color(255,255,180),
		Color.white
	};
	
	public VotePanelDialog(LinkedList<Poll> polls) {
		super();
		this.polls = polls;
		
		setLayout(new GridLayout(polls.size(),1));
		
		rbVotes = new JRadioButton[polls.size()][];
		
		int voteNr = 0;
		for (Poll poll : polls) {
			add(createPollPanel(voteNr++, poll));
		}
	}
	
	/**
	 * creates the panel for a poll
	 * @param voteNr  number of the poll
	 * @param poll  the poll
	 * @return  the panel with all controls for the poll
	 */
	private final JPanel createPollPanel(int voteNr, Poll poll) {
		JPanel pollPanel = new JPanel();
		pollPanel.setLayout(new BoxLayout(pollPanel, BoxLayout.Y_AXIS));
		pollPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		if (poll instanceof FullPoll) {			
			FullPoll fp = (FullPoll)poll;
			rbVotes[voteNr] = new JRadioButton[fp.getAnswerCount() + 1];

			JLabel question = new JLabel(fp.getQuestion());
			pollPanel.add(question);
			
			ButtonGroup grp = new ButtonGroup();
			
			JRadioButton rb = new JRadioButton("abstain from vote");
			rb.setSelected(true);
			grp.add(rb);
			rbVotes[voteNr][0] = rb;
			pollPanel.add(rb);
			
			String[] answers = fp.getAnswers();
			for (int i = 0; i < answers.length; i++) {
				rb = new JRadioButton(answers[i]);
				grp.add(rb);
				pollPanel.add(rb);
				rbVotes[voteNr][i+1] = rb;
			}
		}
		else if (poll instanceof QuickPoll) {
			QuickPoll qp = (QuickPoll)poll;
			pollPanel.setBackground(QUICK_POLL_COLOR[qp.getColor()/4]);
			pollPanel.setOpaque(true);
			rbVotes[voteNr] = new JRadioButton[qp.getAnswerCount()+1];
			
			JLabel question = new JLabel(" -- see lecturer sheet for question -- ");
			question.setOpaque(false);
			pollPanel.add(question);
			
			ButtonGroup grp = new ButtonGroup();
			
			JRadioButton rb = new JRadioButton("abstain from vote");
			rb.setSelected(true);
			rb.setOpaque(false);
			grp.add(rb);
			pollPanel.add(rb);
			rbVotes[voteNr][0] = rb;
			
			for(int i = 0; i < qp.getAnswerCount(); i++) {
				rb = new JRadioButton(" - answer " + i + " - ");
				rb.setOpaque(false);
				grp.add(rb);
				pollPanel.add(rb);
				rbVotes[voteNr][i+1] = rb;
			}
		}
		
		return pollPanel;
	}
	
	/**
	 * get the votes, the user has cast
	 * @return LinkedList with the Votes
	 */
	public LinkedList<Vote> getVotes() {
		LinkedList<Vote> votes = new LinkedList<Vote>();
		
		// check for each poll, which answer (=radioButton) was selected by user
		int voteNr = 0;
		for (Poll p : polls) {
			if(!rbVotes[voteNr][0].isSelected()) {
				for(int i = 1; i < p.getAnswerCount()+1; i++) {
					if(rbVotes[voteNr][i].isSelected()) {
						votes.add(new Vote(p.getID(), i-1));
					}
				}
			}
			voteNr++;
		}
		
		return votes;
	}
	
	public int showDialog(java.awt.Component parent) {
		return JOptionPane.showOptionDialog(parent, this, "Voting", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] { "Vote", "Cancel" }, "Vote");
	}
}
