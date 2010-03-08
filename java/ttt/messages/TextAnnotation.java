package ttt.messages;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.anotherbigidea.flash.movie.FontDefinition;
import com.anotherbigidea.flash.movie.FontLoader;
import com.anotherbigidea.flash.movie.Instance;
import com.anotherbigidea.flash.movie.Text;
import com.anotherbigidea.flash.movie.Font.NoGlyphException;

import ttt.Constants;
import ttt.postprocessing.flash.FlashContext;

/**
 * Annotation which displays a text string.
 * 
 * @author Thomas Doehring
 */
public final class TextAnnotation extends Annotation{

	private String text = "";
	private byte[] bText;
	private int color;
	private int posX;
	private int posY;
	private int maxWidth;
	
	private Rectangle bounds;
	
	final static Font FONT = new Font("SansSerif", Font.BOLD, 16);
	
	final static Color COL_BACKGROUND = new Color(240,240,255,160);
	
	public TextAnnotation(int timestamp, int col, int pX, int pY, int maxWidth, String txt) {
		this.timestamp = timestamp;
		this.color = col;
		this.posX = pX;
		this.posY = pY;
		this.maxWidth = maxWidth;
		this.text = txt;
		getTextBytes();
		
		calculateBounds();
	}
	
	
	
	public TextAnnotation(int timestamp, DataInputStream in) throws IOException {
		this.timestamp = timestamp;
		this.color = in.readUnsignedByte();
		this.posX = in.readUnsignedShort();
		this.posY = in.readUnsignedShort();
		this.maxWidth = in.readUnsignedShort();
		
		int length = in.readUnsignedShort();
		bText = new byte[length];
		int bytesRead = 0;
		while(bytesRead < length) {
			bytesRead += in.read(bText, bytesRead, bText.length - bytesRead);
		}
		this.text = new String(bText, "UTF-8");
		
		calculateBounds();
	}
	
	public TextAnnotation(Element xmlNode) {
		posX = Integer.parseInt(xmlNode.getAttribute("posX"));
		posY = Integer.parseInt(xmlNode.getAttribute("posY"));
		maxWidth = Integer.parseInt(xmlNode.getAttribute("width"));
		color = Integer.parseInt(xmlNode.getAttribute("color"));
		NodeList nlLines = xmlNode.getElementsByTagName("line");
		for(int i = 0; i < nlLines.getLength(); i++) {
			text += ((Element)nlLines.item(i)).getTextContent();
			if (i+1 != nlLines.getLength()) text += "\n";
		}
		getTextBytes();

		calculateBounds();
	}
	
	@Override
	public boolean contains(int x, int y) {
		return bounds.contains(x,y);
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	@Override
	public int getEncoding() {
		return Constants.AnnotationText;
	}

	@Override
	public String getXMLString() {
		StringBuilder sb = new StringBuilder("<TextAnnotation color=\"");
		sb.append(color).append("\" posX=\"").append(posX).append("\" ");
		sb.append("width=\"").append(maxWidth).append("\" ");
		sb.append("posY=\"").append(posY).append("\" >\n");
		String[] lines = text.split("\n");
		for (String line : lines) {
			sb.append("  <line>").append(line).append("</line>\n");			
		}
		sb.append("</TextAnnotation>\n");
		return sb.toString();
	}

	@Override
	public void paint(Graphics2D g) {

		FontMetrics fm = g.getFontMetrics(FONT);

		if (text.length() == 0) {
			g.setColor(Color.red);
			g.drawRect(posX, posY, 10, fm.getHeight());
			
		} else {
			if (maxWidth > 0) {
				// background
				g.setColor(COL_BACKGROUND);
				g.fillRect(posX, posY, bounds.width, bounds.height);

				AttributedString attrString = new AttributedString(text);
				attrString.addAttribute(TextAttribute.FONT, FONT);
				attrString.addAttribute(TextAttribute.FOREGROUND, Annotation.annotationColors[color]);

				AttributedCharacterIterator charIt = attrString.getIterator();
				FontRenderContext fontRenderContext = g.getFontRenderContext();
				LineBreakMeasurer measurer = new LineBreakMeasurer(charIt, fontRenderContext);

				int y = posY;
				
				while(measurer.getPosition() < charIt.getEndIndex()) {
					TextLayout textLayout = measurer.nextLayout(maxWidth);
					y += textLayout.getAscent();

					textLayout.draw(g, posX, y);
					y += textLayout.getDescent() + textLayout.getLeading();
				}
				
			} else {

				String[] lines = text.split("\n",10);
				int y = posY;

				g.setFont(FONT);
				
				g.setColor(COL_BACKGROUND);
				g.fillRect(posX, posY, bounds.width, bounds.height);

				g.setColor(annotationColors[color]);

//				int width = 0;
				for (String line : lines) {
					if (line.length() > 0) {
//						AttributedString attrString = new AttributedString(line);
//						attrString.addAttribute(TextAttribute.FONT, FONT);
//						attrString.addAttribute(TextAttribute.FOREGROUND, Annotation.annotationColors[color]);
//
//						width = Math.max(width, (int)fm.getStringBounds(line,g).getWidth());
//						g.drawString(attrString.getIterator(), posX, y + fm.getAscent());
						g.drawString(line, posX, y + fm.getAscent());
					}
					y += fm.getHeight();
				}
			}
		}
				
		if(temporary) {
			g.setColor(Color.red);
			g.drawRect(posX, posY, bounds.width, bounds.height);
		}
	}

	@Override
	public int getSize() {
		return bText.length + 8;
	}

	@Override
	public void write(DataOutputStream out, int writeTimestamp)
			throws IOException {
		writeHeader(out, writeTimestamp);
			
		out.writeByte(color);
		out.writeShort(posX);
		out.writeShort(posY);
		out.writeShort(maxWidth);
		out.writeShort(bText.length);
		out.write(bText);
	}
	
	private void calculateBounds() {
		BufferedImage bi = new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		FontMetrics fm = g.getFontMetrics(FONT);

		int width = 0;
		int height = 0;
		
		if (text.length() == 0) {
			width = 10;
			height = fm.getHeight();
			
		} else if (this.maxWidth > 0) {
			AttributedString attrString = new AttributedString(text);
			attrString.addAttribute(TextAttribute.FONT, FONT);
			attrString.addAttribute(TextAttribute.FOREGROUND, Color.black);

			AttributedCharacterIterator charIt = attrString.getIterator();
			FontRenderContext fontRenderContext = g.getFontRenderContext();
			LineBreakMeasurer measurer = new LineBreakMeasurer(charIt, fontRenderContext);
			int y = 0;
			while(measurer.getPosition() < charIt.getEndIndex()) {
				TextLayout textLayout = measurer.nextLayout(maxWidth);
				y += textLayout.getAscent() + textLayout.getDescent() + textLayout.getLeading();
				width = Math.max(((int)textLayout.getBounds().getWidth())+5, width);
			}
			height = y;
			
		} else {

			String[] lines = text.split("\n",10);

			for (String line : lines) {
				Rectangle2D r2d = fm.getStringBounds(line, g);
				width = Math.max((int)r2d.getWidth(), width);
				height += fm.getHeight();
			}
		}
		
		bounds = new Rectangle(posX, posY, width, height);
	}
	
	
	public void addChar(char c) {
		text += c;
		getTextBytes();
		calculateBounds();
	}
	
	public void deleteLastChar() {
		if (text.length() > 0) {
			text = text.substring(0, text.length() - 1);
			getTextBytes();
			calculateBounds();
		}
	}
	
	public boolean isEmpty() { return this.text.length() == 0; }
	
	/**
	 * deletes ending whitespace and line feed.
	 * Called by {@link ttt.messaging.client.JAnnotationPanel}
	 */
	public void trim() {
		if (text.trim().length() != text.length()) {
			text = text.trim();
			calculateBounds();
		}
	}

	@Override
	public void writeToFlash(FlashContext flashContext) throws IOException {
		flashContext.checkNextFrame(this.timestamp);
		
		FontDefinition fontDef = FontLoader.loadFont(this.getClass().getResourceAsStream("../../resources/VerdanaFont.swf"));
		Color txtColor = annotationColors[color];
		com.anotherbigidea.flash.structs.Color flashColor = new com.anotherbigidea.flash.structs.Color(txtColor.getRed(), txtColor.getGreen(), txtColor.getBlue());
		com.anotherbigidea.flash.movie.Font font = new com.anotherbigidea.flash.movie.Font(fontDef);
				
		try {
			if (this.maxWidth > 0) {
				
				flashContext.symbolCount++;
				Text flashText = new Text(null);
				flashText.row(font.chars(text,16), flashColor, 0, 0, false, false);
				Instance instanceText = flashContext.frame.placeSymbol(flashText, this.posX, this.posY+16, flashContext.annotationsDepth++);				
				flashContext.addAnnotations(this, instanceText);

			} else {
				String[] lines = text.split("\n",50);
				int y = 16;
				for (String line : lines) {
					flashContext.symbolCount++;
					Text flashText = new Text(null);
					flashText.row(font.chars(line,16), flashColor, 0, 0, false, true);
					Instance instanceText = flashContext.frame.placeSymbol(flashText, this.posX, this.posY+y, flashContext.annotationsDepth++);					
					flashContext.addAnnotations(this, instanceText);

					y += 20;
				}
			}
			
		} catch (NoGlyphException ex) {
			System.err.println(ex.toString());
			flashContext.symbolCount--;
		}

		// the solution with EditFields does not seem to work, maybe there's a mistake?
		// wanted to use it for automatic word wrapping, but
		// the text is not displayed in the flash file!?
		
//		EditField flashEdit = new EditField("foo", "ein etwas l�ngerer Text zum Testen des Umbruchs", font, 16, 0, 0, 200, 200);
//		flashEdit.setProperties(false, true, false, false, true, true, false, false);
//		flashEdit.setTextColor(new AlphaColor(255,0,0,0));
	}
	
	private void getTextBytes() {
		try {
			bText = text.getBytes("UTF-8");
		} catch (UnsupportedEncodingException uee) {
			bText = text.getBytes();
		}
	}

}
