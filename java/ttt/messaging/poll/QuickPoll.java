package ttt.messaging.poll;

/**
 * A Quick poll is defined by a color and the number of answers.
 * It is used when a lecturer wants to quickly create a poll by writing the question
 * and the answers with colored FreehandAnnotations on the sheet and by creating the
 * quick poll with the same color.  
 * @author Thomas Doehring
 */
public class QuickPoll extends Poll {

	int color;
	int answerCount;
	
	public QuickPoll(int id, int answerCount, int color) {
		this.id = id;
		this.color = color;
		this.answerCount = answerCount;
		this.votes = new int[answerCount];
		this.votesPromille = new int[answerCount];
	}
	
	public int getColor() { return this.color; }
	
	@Override
	public int getAnswerCount() {
		return this.answerCount;
	}
	
	@Override
	public String getXML() {
		StringBuilder sb = new StringBuilder("<quickpoll id=\"");
		sb.append(id).append("\" color=\"").append(color).append("\" answerCount=\"");
		sb.append(answerCount).append("\" />\n");
		return sb.toString();
	}
}
