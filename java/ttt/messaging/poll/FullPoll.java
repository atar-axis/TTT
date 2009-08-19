package ttt.messaging.poll;

/**
 * A FullPoll consists of a question and a number of answers.
 * @author Thomas Doehring
 */
public class FullPoll extends Poll {

	String question = "";
	String[] answers;
	
	public FullPoll(int id, String question, String[] answers) {
		this.id = id;
		this.question = question;
		this.answers = answers;
		this.votes = new int[answers.length];
		this.votesPromille = new int[answers.length];
		for(int i = 0; i < votes.length; i++) {
			votes[i] = 0;
			votesPromille[i] = 0;
		}
	}
	
	public String getQuestion() {
		return this.question;
	}
	
	public String[] getAnswers() {
		return this.answers;
	}
	
	@Override
	public int getAnswerCount() {
		return this.answers.length;
	}

	@Override
	public String getXML() {
		StringBuilder sb = new StringBuilder("<fullpoll id=\"");
		sb.append(id).append("\" text=\"").append(question).append("\" >\n");
		for(int i = 0; i < answers.length; i++) {
			sb.append("<answer id=\"").append(i).append("\" text=\"");
			sb.append(answers[i]).append("\" />\n");
		}
		sb.append("</fullpoll>\n");
		return sb.toString();
	}

}
