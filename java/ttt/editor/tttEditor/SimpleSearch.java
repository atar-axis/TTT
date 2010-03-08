package ttt.editor.tttEditor;
/*
 * Created on 31.03.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author Peter Ziewer
 */
public class SimpleSearch {
	static boolean umlaut_reduction = true;

	private String original_text;
	private String searchableText;

	public SimpleSearch(String text) {
		setText(text);
	}

	public void setText(String text) {
		if (text == null)
			text = "";
		original_text = text;
		this.searchableText = reduce(original_text);
	}

	public String getText() {
		return original_text;
	}

	public boolean contains(String text) {
		if(text==null) text="";
		return searchableText.indexOf(reduce(text)) >= 0;
	}

	String reduce(String text) {
		// removes special characters
		// shifts all characters to lower case
		//
		StringBuffer stringBuffer = new StringBuffer();
		boolean blank = false;
		for (int pos = 0; pos < text.length(); pos++) {
			int character = text.charAt(pos);
			char ch = Character.toLowerCase((char) character);
			if (Character.isLetterOrDigit(ch)) {
				if (umlaut_reduction) {
					switch (ch) {
						case 'ä' :
							stringBuffer.append("ae");
							break;
						case 'ö' :
							stringBuffer.append("oe");
							break;
						case 'ü' :
							stringBuffer.append("ue");
							break;
						case 'ß' :
							stringBuffer.append("ss");
							break;
						default :
							stringBuffer.append(ch);
							break;
					}
				} else
					stringBuffer.append(ch);
				blank = false; // character added
			} else if (!blank) {
				stringBuffer.append(' ');
				blank = true; // only append one blank
			}
		}
		return new String(stringBuffer);
	}

	public static void main(String[] args) {
		System.out.println(args[0]);
		SimpleSearch search = new SimpleSearch(args[0]);
		System.out.println(search.searchableText);
		System.out.println(search.contains(args[1]));
	}
}
