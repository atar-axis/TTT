package ttt.messaging;

/**
 * Represents a text message. Just contains the text as string.
 * @author Thomas Doehring
 */
public class TTTTextMessage extends TTTMessage {

	private String text = "";
	
	public TTTTextMessage(String txt) {
		this.text = txt;
	}
	
	/**
	 * get text of message
	 * @return the text
	 */
	public String getText() { return this.text; }
}
