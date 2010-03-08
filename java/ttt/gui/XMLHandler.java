package ttt.gui;

/*
 * MODIFIED VERSION OF
 * 
 * @(#)Echo01.java 1.5 99/02/09
 * 
 * Copyright (c) 1998 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use, modify and redistribute this software in
 * source and binary code form, provided that i) this copyright notice and license appear on all copies of the software;
 * and ii) Licensee does not utilize the software in a manner which is disparaging to Sun.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS
 * AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL
 * OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY
 * TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * This software is not designed or intended for use in on-line control of aircraft, air traffic, aircraft navigation or
 * aircraft communications; or in the design, construction, operation or maintenance of any nuclear facility. Licensee
 * represents and warrants that it will not use or redistribute the Software for such purposes.
 */

import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLHandler extends DefaultHandler {

    public static void main(String argv[]) {
        if (argv.length < 1) {
            System.err.println("Usage: cmd [-txt] <xml-serachbase>");
            System.err.println("            -txt: create ASCII searchbase");
            System.exit(1);
        }

        boolean createASCII = argv[0].equals("-txt");
        System.arraycopy(argv, 1, argv, 0, argv.length - 1);

        // Use an instance of ourselves as the SAX event handler
        XMLHandler handler = new XMLHandler();
        handler.output = !createASCII;

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            // Parse the input
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new File(argv[0]), handler);

            if (createASCII) {
                // create output file
                String filename = argv[0];
                if (filename.endsWith(".xml"))
                    filename = filename.substring(0, filename.length() - 4) + ".txt";
                File file = new File(filename);

                System.out.println("create ASCII searchbase: '" + file.getCanonicalPath() + "'");
                // backup existing file
                if (file.exists()) {
                    System.out.println("    backup exiting file");
                    file.renameTo(new File(filename + "bak"));
                    file = new File(filename);
                }

                Writer out = new BufferedWriter(new FileWriter(filename));
                ArrayList<ArrayList<SearchBaseEntry>> pages = handler.getResult();
                for (ArrayList<SearchBaseEntry> words : pages) {
                    System.out.print(".");
                    boolean first = true;
                    for (SearchBaseEntry entry : words) {
                        out.write((first ? "" : " ") + entry.searchTextOriginal);
                        first = false;
                    }
                    // pagebreak
                    out.write(12);
                }
                out.flush();
                out.close();
                System.out.println(" done");
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.exit(0);
    }

    public boolean output = false;

    // ===========================================================
    // SAX DocumentHandler methods
    // ===========================================================

    public void startDocument() throws SAXException {}

    public void endDocument() throws SAXException {
    // System.out.println("Page size (" + width + "," + height + ")");
    // for (int i = 0; i < searchBase.size(); i++) {
    // System.out.println("Page " + (i + 1));
    // ArrayList page = (ArrayList) searchBase.get(i);
    // for (int j = 0; j < page.size(); j++) {
    // System.out.println("\t" + page.get(j));
    // }
    // }
    }

    int page = 0;
    boolean wdFlag;
    int left, right, top, bottom;
    String searchText = null;

    int width, height;

    // list of pages - each containing a list of words
    ArrayList<ArrayList<SearchBaseEntry>> pages = new ArrayList<ArrayList<SearchBaseEntry>>();
    // temporary list of words for current page
    ArrayList<SearchBaseEntry> words;

    public ArrayList<ArrayList<SearchBaseEntry>> getResult() {
        return pages;
    }

    // ratio used to translate coordinates from OmniPage values to pixel values
    // NOTE: OmniPage does not specify coordinates in pixels
    double ratio = 1;

    // specify desktop resolution (used to calculate ratio)
    // NOTE: desktop resolution is not store within OmniPage 14 XML file but within OmniPage 15 XML files
    Dimension desktopResulotion;

    public void setDesktopResolution(int width, int height) {
        desktopResulotion = new Dimension(width, height);
    }

    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes attrs)
            throws SAXException {

        String elementName = localName; // element name
        if ("".equals(elementName))
            elementName = qualifiedName; // namespaceAware = false

        // beginning of page
        if (elementName.equals("page")) {
            // debug output
            if (output) {
                System.out
                        .println("\n---------------------------------------------------------------------------------\n"
                                + "\t\t\t\tPage Nr."
                                + (pages.size() + 1)
                                + "\n---------------------------------------------------------------------------------");
            }

            // new page - new page list
            words = new ArrayList<SearchBaseEntry>();
            pages.add(words);
        }
        // read OmniPage's page resolution
        // NOTE: OmniPage 14 uses width and height attributes of page-tag
        // NOTE: OmniPage 15 uses width and height attributes of theoreticalPage-tag
        // TODO: check if theoreticalPage-tag is a son of a description-tag, which should be a son of a page-tag
        if (elementName.equals("page") || elementName.equals("theoreticalPage")) {
            // read page dimensions
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    String attributeName = attrs.getLocalName(i); // Attr name
                    if ("".equals(attributeName))
                        attributeName = attrs.getQName(i);
                    if (attributeName.equals("width")) {
                        int w = Integer.parseInt(attrs.getValue(i));
                        if (width == 0) {
                            width = w;
                            ratio = desktopResulotion == null ? 1 : desktopResulotion.getWidth() / width;
                            // debug output
                            if (output && desktopResulotion != null)
                                System.out.println("\n******** RATIO SET TO " + ratio + "******** \n");
                        } else if (width != w) {
                            System.out.println("ERROR VARIABLE PAGE WIDTH");
                            System.out.println("\tpage: " + pages.size());
                            System.out.print("\twidth = " + w + " (expected:" + width + ")");
                        }
                    }
                    if (attributeName.equals("height")) {
                        int h = Integer.parseInt(attrs.getValue(i));
                        if (height == 0) {
                            height = h;
                            ratio = desktopResulotion == null ? 1 : desktopResulotion.getHeight() / height;
                            // debug output
                            if (output && desktopResulotion != null)
                                System.out.println("\n******** RATIO SET TO " + ratio + "******** \n");
                        } else if (height != h) {
                            System.out.println("ERROR VARIABLE PAGE HEIGHT");
                            System.out.println("\tpage: " + pages.size());
                            System.out.print("\theight = " + h + " (expected:" + height + ")");
                        }
                    }
                    // System.out.print(attributeName + "=\"" + attrs.getValue(i) + "\" ");
                }
            }
        }
        // read pixel resolution
        // NOTE: OmniPage 14 NOT AVAILABLE (must by specified by caller via setDesktopResolution(width, height))
        // NOTE: OmniPage 15 uses sizex and sizey attributes of source-tag
        if (elementName.equals("source")) {
            // read page dimensions
            if (attrs != null) {
                int sizex = 0;
                int sizey = 0;
                for (int i = 0; i < attrs.getLength(); i++) {
                    String attributeName = attrs.getLocalName(i); // Attr name
                    if ("".equals(attributeName))
                        attributeName = attrs.getQName(i);
                    // set pixel resolution
                    // NOTE: OminPage 15 in
                    if (attributeName.equals("sizex")) {
                        sizex = Integer.parseInt(attrs.getValue(i));
                        if (sizex != 0 && sizey != 0)
                            setDesktopResolution(sizex, sizey);
                    }
                    if (attributeName.equals("sizey")) {
                        sizey = Integer.parseInt(attrs.getValue(i));
                        if (sizex != 0 && sizey != 0)
                            setDesktopResolution(sizex, sizey);
                    }
                    // System.out.print(attributeName + "=\"" + attrs.getValue(i) + "\" ");
                }
            }
        }

        // beginning of word - read attributes: left, right, top, bottom
        if (elementName.equals("wd")) {
            wdFlag = true;

            // reset searchtext
            searchText = null;

            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    String attributeName = attrs.getLocalName(i); // Attr name
                    if ("".equals(attributeName))
                        attributeName = attrs.getQName(i);
                    if (attributeName.equals("l"))
                        left = Integer.parseInt(attrs.getValue(i));
                    else if (attributeName.equals("t"))
                        top = Integer.parseInt(attrs.getValue(i));
                    else if (attributeName.equals("r"))
                        right = Integer.parseInt(attrs.getValue(i));
                    else if (attributeName.equals("b"))
                        bottom = Integer.parseInt(attrs.getValue(i));
                }
                if (output) {
                    System.out.print("(left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom
                            + ") ");
                }
            }
        }
    }

    static boolean flag = true;

    public void endElement(String namespaceURI, String simpleName, String qualifiedName) throws SAXException {
        // word completed
        if (qualifiedName.equals("wd")) {
            wdFlag = false;
            if (searchText == null)
                searchText = "";
            if (flag)
                flag = false;

            SearchBaseEntry entry = new SearchBaseEntry(searchText, left, top, right - left, bottom - top, ratio);
            if (output)
                System.out.println("########## RATIO " + ratio);
            // System.out.println("\tset:\t" + searchText);
            words.add(entry);
        }
        // if (qualifiedName.equals("page")) {
        // System.out.println("\n------------------------------");
        // }
    }

    public void characters(char buf[], int offset, int len) throws SAXException {
        // buffer current word
        if (wdFlag) {
            // if (output)
            // System.out.println("\tcurrent[" + searchText + "]");
            if (searchText == null) {
                // set
                searchText = new String(buf, offset, len);
            } else {
                // concatenate
                searchText += new String(buf, offset, len);
            }
            if (output)
                System.out.println("\t" + searchText.trim());
            // System.out.println("\t[" + len + " chars]\t" + searchText);
        }
    }
}