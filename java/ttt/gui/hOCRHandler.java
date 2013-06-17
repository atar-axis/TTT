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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class hOCRHandler extends DefaultHandler {

    public static void main(String argv[]) {
        if (argv.length < 1) {
            System.err.println("Usage: cmd [-txt] <xml-serachbase>");
            System.err.println("            -txt: create ASCII searchbase");
            System.exit(1);
        }

        boolean createASCII = argv[0].equals("-txt");
        System.arraycopy(argv, 1, argv, 0, argv.length - 1);

        // Use an instance of ourselves as the SAX event handler
        hOCRHandler handler = new hOCRHandler();
        handler.output = !createASCII;

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
        	System.out.println("Parsing input in "+argv[0]);
        	
        	
        	XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        	xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(new FileReader(argv[0])));
        	
        	// Parse the input
        	System.out.println("Parsed input");
        	
            if (createASCII) {
                // create output file
                String filename = argv[0];
                if (filename.endsWith(".hocr"))
                    filename = filename.substring(0, filename.length() - 5) + ".txt";
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
        System.out.println("Program ended");
    }

    public boolean output = false;

    // ===========================================================
    // SAX DocumentHandler methods
    // ===========================================================

    public void startDocument() throws SAXException { 
    }

    public void endDocument() throws SAXException {
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

    // specify desktop resolution (used to calculate ratio)
    // NOTE: desktop resolution is not store within OmniPage 14 XML file but within OmniPage 15 XML files
    Dimension desktopResulotion;
    public void setDesktopResolution(int width, int height) {
//    	System.out.println("Desktop resolution is now "+width+"x"+height);
        desktopResulotion = new Dimension(width, height);
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes attrs)
            throws SAXException {
    	wdFlag=false;
    	if (localName.equals("div") && attrs.getValue("class").equals("ocr_page")){
    		String resolution = attrs.getValue("title").split(";")[1].substring(9).trim();
    		setDesktopResolution(Integer.parseInt(resolution.split(" ")[0]),Integer.parseInt(resolution.split(" ")[1]));
    	}
    	
    	if (localName.equals("span") && attrs.getValue("class").equals("ocrx_word")){
            wdFlag = true;
            // reset searchtext
            searchText = null;
            
    		String[] coords = attrs.getValue("title").substring(5).split(" ");
    		left   = Integer.parseInt(coords[0]);
    		top    = Integer.parseInt(coords[1]);
    		right  = Integer.parseInt(coords[2]);
    		bottom = Integer.parseInt(coords[3]);
    	}
    	
        if (localName.equals("div") && attrs.getValue("class").equals("ocr_page")) {
            // new page - new page list
            words = new ArrayList<SearchBaseEntry>();
            pages.add(words);
        }

    }

    static boolean flag = true;

    @Override
    public void endElement(String namespaceURI, String simpleName, String qualifiedName) throws SAXException {
    	// word completed
        if (simpleName.equals("span") && wdFlag) {
            wdFlag = false;
            if (searchText == null)
                searchText = "";
            if (flag)
                flag = false;

            SearchBaseEntry entry = new SearchBaseEntry(searchText, left, top, right - left, bottom - top, 1);
            words.add(entry);
        }
    }

    @Override
    public void characters(char buf[], int offset, int len) throws SAXException {
        // buffer current word
        if (wdFlag) {
            if (searchText == null) {
                // set
                searchText = new String(buf, offset, len);
            } else {
                // concatenate
                searchText += new String(buf, offset, len);
            }
        }
    }
}