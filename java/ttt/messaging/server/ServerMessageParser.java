package ttt.messaging.server;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.LinkedList;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ttt.messages.*;
import ttt.helper.Base64Codec;
import ttt.helper.XMLHelper;
import ttt.messaging.*;

/**
 * Parses the XML messages with a DOM parser and does the action needed according to the
 * content of the messages.
 * @author Thomas Doehring
 */
public class ServerMessageParser {

	private DocumentBuilder parser = null;
	private TTTMessengerConnection client = null;
	private MessagingController ctrl = null;
	private InetAddress clIP;
	
	public ServerMessageParser(InetAddress ip, TTTMessengerConnection cl, MessagingController ctrl)
	{
		this.clIP = ip;
		this.client = cl;
		this.ctrl = ctrl;
		
		// create DOM Parser
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setValidating(false);
		
		try {
			parser = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce){
			parser = null;
		}
	}
	
	public String parseMessage(String msg) {
		try {
			Document dom = parser.parse(new InputSource(new StringReader(msg)));
			return handleMessage(dom);
		} catch (IOException ioe) {
		} catch (SAXException se) {
		}
		return "";
	}
	
	/**
	 * @param dom root node of tttmessage xml
	 * @return Message for client as finished tttmessage
	 */
	private String handleMessage(Document dom){
		Element root = dom.getDocumentElement();
		// check if really tttmessage
		if(root.getNodeName().equals("tttmessage")){
			String msgType = root.getAttribute("type");

			if (msgType == null) return null;
			
			// *** COMMANDS *** //
			if (msgType.equals("command")) {
				
				Element cmd = XMLHelper.getFirstElementChild(root);
				if(cmd != null){
					String cmdName = cmd.getNodeName();
					
					if(cmdName.equals("closeConnection")) {
						return "CLOSE";
						
					} else if (cmdName.equals("getCurrentSheet")) {
						boolean annotated = ((cmd.getAttribute("annotated") != null) &&
								cmd.getAttribute("annotated").equals("true"));
						return ctrl.getCurrentSheet().toXMLString(annotated);
						
					} else if (cmdName.equals("queryPolls")) {
						return ctrl.getOpenPollsForUserAsXML(clIP);
					
					} else if (cmdName.equals("castVotes")) {
						castVotes(cmd);
					}
					else if (cmdName.equals("queryScreenSize")) {
						return ctrl.getScreenSizeAsXML();
					}
					else if (cmdName.equals("name")) {
						if (cmd.getTextContent() != null) {
							client.setUserName(cmd.getTextContent());
						}
					}
				}
			
			// *** CONTENT *** //
			} else if (msgType.equals("content")) {
				TTTMessage msg = null;
				
				Element content = XMLHelper.getFirstElementChild(root);
				if(content.getNodeName().equals("text")) {
					msg = new TTTTextMessage(content.getTextContent());
					
				} else if (content.getNodeName().equals("sheet")) {
					// get the contents of the sheet message
					
					NodeList nlImages = content.getElementsByTagName("bgimage");
					if (nlImages.getLength() > 0) {
						Element elImage = (Element)nlImages.item(0);
						byte[] imgData = Base64Codec.decode(elImage.getAttribute("data"));
						msg = new TTTSheetMessage(imgData);
					}
					
					NodeList nlAnnots = content.getElementsByTagName("annotations");
					if (nlAnnots.getLength() > 0) {
						Element elAnnots = (Element)nlAnnots.item(0);
						
						LinkedList<Element> llElAnnots = XMLHelper.getChildElements(elAnnots);
						Annotation[] annotations = new Annotation[llElAnnots.size()];
						int pos = 0;
						for (Element elAnnot : llElAnnots) {
							String name = elAnnot.getNodeName();
							if(name.equals("FreehandAnnotation")) {
								annotations[pos++] = new FreehandAnnotation(elAnnot);
							} else if (name.equals("LineAnnotation")) {
								annotations[pos++] = new LineAnnotation(elAnnot);
							} else if (name.equals("HighlightAnnotation")) {
								annotations[pos++] = new HighlightAnnotation(elAnnot);
							} else if (name.equals("RectangleAnnotation")) {
								annotations[pos++] = new RectangleAnnotation(elAnnot);
							} else if (name.equals("ImageAnnotation")) {
								annotations[pos++] = new ImageAnnotation(elAnnot);
							} else if (name.equals("TextAnnotation")) {
								annotations[pos++] = new TextAnnotation(elAnnot);
							}
						}
						
						if (msg == null) {
							msg = new TTTSheetMessage(annotations);
						} else {
							((TTTSheetMessage)msg).setAnnotations(annotations);
						}
					}
					
					if(content.getElementsByTagName("text").getLength() > 0) {
						Element elText = (Element)content.getElementsByTagName("text").item(0);
						((TTTSheetMessage)msg).setText(elText.getTextContent());
					}
					
					if(msg != null) ((TTTSheetMessage)msg).setSheetSize(ctrl.getScreenSize());
				}
				
				if(msg != null) {
					msg.setClientIP(clIP);
					if(client.userNameAvailable()) msg.setUserName(client.getUserName());
					ctrl.addMessage(msg);
				}
				
			// *** RESPONSE *** //
			} else if (msgType.equals("response")) {
				// no responses from clients expected
			}
			
		}
		
		return "";
	}
		
	private void castVotes(Element elVotes) {
		NodeList nlVotes = elVotes.getElementsByTagName("vote");
		
		for(int i = 0; i < nlVotes.getLength(); i++) {
			Element elVote = (Element)nlVotes.item(i);
			int id = Integer.parseInt(elVote.getAttribute("id"));
			int answer = Integer.parseInt(elVote.getAttribute("answer"));
			ctrl.castVote(client.getIP(), id, answer);
		}		
	}

}
