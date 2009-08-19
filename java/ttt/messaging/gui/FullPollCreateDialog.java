package ttt.messaging.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This panel is used for the creation of {@link ttt.messaging.poll.FullPoll}s.
 * It contains 9 text boxes for the question and up to 8 answers. Only non-empty
 * answer text boxes are added to the answer list. 
 * @author Thomas Doehring
 */
public class FullPollCreateDialog extends JPanel {

	static final long serialVersionUID = 1L;
	
	static final int TEXT_COL_COUNT = 40;
	
	private JTextField txtQuestion;
	private JTextField[] txtAnswers;
	
	public FullPollCreateDialog() {
		super();
		
		setLayout(new GridBagLayout());
		GridBagConstraints cLabel = new GridBagConstraints();
		cLabel.weightx = 0;
		GridBagConstraints cText = new GridBagConstraints();
		cText.weightx = 1;
		
		JLabel lblQuestion = new JLabel("Question:");
		cLabel.gridx = 0;
		cLabel.gridy = 0;
		add(lblQuestion, cLabel);
		txtQuestion = new JTextField(TEXT_COL_COUNT);
		cText.gridx = 1;
		cText.gridy = 0;
		add(txtQuestion, cText);
		
		txtAnswers = new JTextField[8];
		for(int i = 0; i < txtAnswers.length; i++) {
			JLabel lbl = new JLabel("Answer " + i + ":");
			cLabel.gridy = i+1;
			add(lbl,cLabel);
			txtAnswers[i] = new JTextField(TEXT_COL_COUNT);
			cText.gridy = i+1;
			add(txtAnswers[i],cText);
		}
	}
	
	/**
	 * @return  text in question text box
	 */
	public String getQuestion() {
		return txtQuestion.getText();
	}
	
	/**
	 * get all entered answers. 
	 * @return  answers as String-Array
	 */
	public String[] getAnswers() {
		String[] tmp = new String[8];
		int count = 0;
		for(int i = 0; i < txtAnswers.length; i++) {
			String answer = txtAnswers[i].getText().trim();
			if(answer.length() > 0) tmp[count++] = answer;
		}
		if (count > 1) {
			String[] answers = new String[count];
			for(int i = 0; i < count; i++) {
				answers[i] = tmp[i];
			}
			return answers;
		}
		return null;
	}
	
}
