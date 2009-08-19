package ttt.helper;

import java.util.LinkedList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * provides convenience methods for working with DOM
 * @author Thomas Doehring
 */
public final class XMLHelper {

	/**
	 * returns all childs of given DOM node which are ELEMENT nodes
	 * @param parent  the parent node of the searched childs
	 * @return  LinkedList of all found ELEMENT child nodes
	 */
	public static LinkedList<Element> getChildElements(Node parent) {
		LinkedList<Element> llElements = new LinkedList<Element>();
		NodeList childs = parent.getChildNodes();
		for(int i = 0; i < childs.getLength(); i++) {
			if (childs.item(i).getNodeType() == Node.ELEMENT_NODE)
				llElements.add((Element)childs.item(i));
		}
		
		return llElements;
	}
	
	/**
	 * searches the first child node which is an ELEMENT
	 * @param parent  the parent node of the searched child
	 * @return  the first element child (or null if parent does not contain element nodes)
	 */
	public static Element getFirstElementChild(Node parent){
		NodeList childs = parent.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			if (childs.item(i).getNodeType() == Node.ELEMENT_NODE) {
				return (Element)childs.item(i);
			}
		}
		return null;
	}

}
