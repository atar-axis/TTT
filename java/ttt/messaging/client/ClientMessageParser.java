package ttt.messaging.client;

import ttt.helper.Base64Codec;
import ttt.helper.ImageHelper;
import ttt.helper.XMLHelper;
import ttt.messages.*;
import ttt.messaging.poll.FullPoll;
import ttt.messaging.poll.Poll;
import ttt.messaging.poll.QuickPoll;

import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.util.LinkedList;

import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

/**
 * Parses the XML strings received from messaging server.
 * @author Thomas Doehring
 */
public class ClientMessageParser {

	private DocumentBuilder parser;
	private ClientController cc;
	
	public ClientMessageParser(ClientController cc) {

		this.cc = cc;

		// use a DOM parser
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setValidating(false);
		
		try {
			parser = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			parser = null;
		}
	}
	
	/**
	 * create DOM from XML string.
	 * @param msg  the XML string message
	 * @return answer to client or empty String if no answer
	 */
	public String parseMessage(String msg) {
		try {
			Document dom = parser.parse(new InputSource(new StringReader(msg)));
			return handleMessage(dom);
		} catch (Exception e){
			e.printStackTrace();
		}
		return "";
	}
	
	// look for the order in the message
	private String handleMessage(Document dom) {
		Element root = dom.getDocumentElement();
		// check if really tttmessage
		if(root.getNodeName().equals("tttmessage")){
			String msgType = root.getAttribute("type");
			
			// **** COMMANDS **** //
			if (msgType.equals("command")) {
				Element cmd = XMLHelper.getFirstElementChild(root);
				String cmdName = cmd.getNodeName();
				
				if(cmdName.equals("closeConnection")) {
					return "CLOSE";
				}
				
				
			// **** RESPONSES **** //
			} else if (msgType.equals("response")) {
				
				NodeList nl = root.getElementsByTagName("polls");
				if(nl.getLength() > 0) {
					displayPolls((Element)nl.item(0));
				}
				
				if(root.getElementsByTagName("screenSize").getLength() > 0) {
					Element size = (Element)root.getElementsByTagName("screenSize").item(0);
					int width = Integer.parseInt(size.getAttribute("width"));
					int height = Integer.parseInt(size.getAttribute("height"));
					cc.setScreenSize(width, height);
				}
				
				
			// **** CONTENT **** //
			} else if (msgType.equals("content")) {
				Element content = XMLHelper.getFirstElementChild(root);
				
				if(content.getNodeName().equals("text")) {
					// no text messages from server
					
				} else if (content.getNodeName().equals("sheet")) {
					createSheet(content);
				}
			}
		}
		
		return "";
	}

	/**
	 * extract the content from a received sheet message and trigger displaying.
	 * @param elSheet  the root node of the message
	 */
	private void createSheet(Element elSheet)
	{
		BufferedImage img = null;
		byte[] bImgData = null;
		Annotation[] annotations = null;
		
		if (elSheet.getElementsByTagName("bgimage").getLength() > 0) {
			Element elImg = (Element)elSheet.getElementsByTagName("bgimage").item(0);
			String sImgData = elImg.getAttribute("data");
			bImgData = Base64Codec.decode(sImgData);
			img = ImageHelper.createImageFromBytes(bImgData);
		}
		if (elSheet.getElementsByTagName("annotations").getLength() > 0) {
			Node ndAnnots = elSheet.getElementsByTagName("annotations").item(0);
			LinkedList<Element> annots = XMLHelper.getChildElements(ndAnnots);
			annotations = new Annotation[annots.size()];
			int count = 0;
			for (Element el : annots) {
				// convert from XML to annotation
				String anotName = el.getNodeName();
				if(anotName.equals("FreehandAnnotation")) {
					annotations[count++] = new FreehandAnnotation(el);
					
				} else if(anotName.equals("HighlightAnnotation")) {
					annotations[count++] = new HighlightAnnotation(el);
					
				} else if(anotName.equals("LineAnnotation")) {
					annotations[count++] = new LineAnnotation(el);
					
				} else if(anotName.equals("RectangleAnnotation")) {
					annotations[count++] = new RectangleAnnotation(el);
					
				} else if(anotName.equals("ImageAnnotation")) {
					annotations[count++] = new ImageAnnotation(el);
					
				} else if(anotName.equals("TextAnnotation")) {
					annotations[count++] = new TextAnnotation(el);
				}
			}
		}
		
		cc.showSheet(img, bImgData, annotations);
	}
	
	/**
	 * create the Poll class instances from XML and trigger displaying.
	 * @param elPolls  the parent node of the polls
	 */
	private void displayPolls(Element elPolls) {
		// gather polls
		LinkedList<Poll> llPolls = new LinkedList<Poll>();
		
		NodeList nl = elPolls.getChildNodes();
		
		for(int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element elPoll = (Element)n;
				if(elPoll.getNodeName().equals("fullpoll")) {
					int id = Integer.parseInt(elPoll.getAttribute("id"));
					String question = elPoll.getAttribute("text");
					NodeList nlAnswers = elPoll.getElementsByTagName("answer");
					String[] answers = new String[nlAnswers.getLength()];
					for(int j = 0; j < nlAnswers.getLength(); j++) {
						Element elAnswer = (Element)nlAnswers.item(j);
						int idx = Integer.parseInt(elAnswer.getAttribute("id"));
						answers[idx] = elAnswer.getAttribute("text");
					}
					llPolls.add(new FullPoll(id, question, answers));
				}
				else if (elPoll.getNodeName().equals("quickpoll")) {
					int id = Integer.parseInt(elPoll.getAttribute("id"));
					int answerCount = Integer.parseInt(elPoll.getAttribute("answerCount"));
					int color = Integer.parseInt(elPoll.getAttribute("color"));
					llPolls.add(new QuickPoll(id, answerCount, color));
				}
			}
		}
		
		cc.showPolls(llPolls);
		
	}
	
}
