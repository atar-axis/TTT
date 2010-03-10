package ttt.editor.tttEditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.LinkedList;

import ttt.messages.Annotation;


/**Annotation message.
 */
public class AnnotationMessage extends Message {
        
    private ColoredShape shape = null;

    private int x, y;
    
    //REVISE THIS SO DOESN'T STORE DATA ARRAY AS WELL AS INTEGERS??!?
    //Need to store co-ordinates then, or else just take from shape?
    //(will then also need own write method)
    
    /**
     * Class constructor
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param data byte array containing the main data for the <code>Message</code>
     * @param header object containing data specific to the TTTFile, and
     * necessary for processing any message
     */
    public AnnotationMessage(int timestamp, int encoding, byte[] data, Header header) {
        super(timestamp, encoding, data, header);
        
        switch(encoding) {
            case ProtocolConstants.LINE:
                handleLine();
                break;
            case ProtocolConstants.RECT:
                handleRectangle();
                break;
            case ProtocolConstants.HIGHLIGHT:
                handleHighlight();
                break;
            case ProtocolConstants.FREE:
                handleFreehand();
                break;
            case ProtocolConstants.REMOVE:
                x = getUnsignedShort();
                y = getUnsignedShort();
                break;
            case ProtocolConstants.TEXT:
            	break;
             }
    }
        
	 byte[] bText;
	 String text;
	 int maxWidth;
	 int c;
	 final static Font FONT = new Font("SansSerif", Font.BOLD, 16);
	 
	 
		public void writeTextAnnotaion(DataOutputStream out)
				throws IOException {
							
			out.writeByte(c);
			out.writeShort(x);
			out.writeShort(y);
			out.writeShort(maxWidth);
			out.writeShort(bText.length);
			out.write(bText);
		}
	 
	
		
		//Constructor for TextAnnotations
    public AnnotationMessage(int timestamp, int col, int pX, int pY, int maxWidth, String txt, int encoding, byte[] data, Header header) {
        super(timestamp, encoding, data, header);
    	this.timestamp = timestamp;
		this.c = col;
		this.x = pX;
		this.y = pY;
		
	this.maxWidth = maxWidth;
		 text = txt;
		try {
			bText = txt.getBytes("UTF-8");
		} catch (UnsupportedEncodingException uee) {
			bText = txt.getBytes();
		}
	
		calculateBounds();
	}
  
 
	final static Color COL_BACKGROUND = new Color(240,240,255,160);
	
    private void calculateBounds() {
		BufferedImage bi = new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		FontMetrics fm = g.getFontMetrics(FONT);

		int width = 0;
		int height = 0;
		
		if (text.length() == 0) {
			width = 15;
			height = fm.getHeight();
			
		} else if (this.maxWidth > 0) {
			AttributedString attrString = new AttributedString(text);
			attrString.addAttribute(TextAttribute.FONT, FONT);
			attrString.addAttribute(TextAttribute.FOREGROUND, Annotation.annotationColors[c]);
		
				
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
		
		shape = new TextShape(x, y, x+width, y-height,  COL_BACKGROUND, new Color(136, 136, 136, 200), 1, text, c);
		  
	}
    
    /**
     * Get the shape contained within this <code>Message</code>.  Will return
     * <code>null</code> if no shape is available (e.g. the encoding dictates
     * that the message should remove shapes rather than add one)
     * @return the <code>ColoredShape</code> stored by this <code>AnnotationMessage</code>,
     * if present.
     */
    public ColoredShape getShape() {
        return shape;
    }
    
    
    /**
     * If <code>AnnotationMessage</code> encoding is REMOVE, removes applicable shapes from
     * a linked list of <code>AnnotationMessage</code>s.
     * Otherwise does nothing.
     * @param annotations A <code>LinkedList</code> containing <code>AnnotationMessage</code>s.
     */
    public void processRemove(LinkedList<AnnotationMessage> annotations) {
        if (encoding != ProtocolConstants.REMOVE)
            return;
        AnnotationMessage[] s = overShape(x, y, annotations);
        if (s != null) {
            for (int i = 0; i < s.length; i++) {
                annotations.remove(s[i]);
            }
        }
    }
    
    
     /**
     * Queries whether this message requests that all previous annotations be removed from the screen.
     * @return <code>true</code> if the <code>AnnotationMessage</code> encoding is REMOVE_ALL
     */
    public boolean isRemoveAll() {
        return (encoding == ProtocolConstants.REMOVE_ALL);
    }
    
    
    /**
     * Queries whether this message requests that all annotations at a point 
     * on the screen be removed.
     * @return <code>true</code> if the <code>AnnotationMessage</code> encoding is REMOVE
     */
    public boolean isRemove() {
        return (encoding == ProtocolConstants.REMOVE);
    }
    
    /**
     * Gets the point on the screen specified if the message encoding is REMOVE.
     * When a remove message is processed, all annotations shown at a certain
     * point on the screen should be removed.
     * @return a <code>Point</code> object for the point on the screen affected
     * by the remove message, otherwise <code>null</code> if the message
     * encoding is not REMOVE
     */
    public Point getRemoveCoordinates() {
        if (isRemove()) {
            return new Point(x,y);}
        
            return null;
    }
    
    
    /**
     * Queries whether the message contains a highlight.
     * @return <code>true</code> if the <code>AnnotationMessage</code> encoding is HIGHLIGHT
     */
    public boolean isHighlight() {
        return (encoding == ProtocolConstants.HIGHLIGHT);
    }
    
    
      /**
     * Paints any included shape onto the <code>Graphics2D</code> context passed as a parameter.
     * Any scaling can be applied to the <code>Graphics2D</code> object before it is passed
     * to this method, thereby allowing annotations to be painted at different sizes.
     * @param g2 The Graphics context upon which to paint
     */
    public void paint(Graphics2D g2, Stroke penStyle) {
        if (shape != null)
            shape.paintShape(g2, penStyle);
    }
    
    
    private void handleLine() {
        Color c = Parameters.colors[getUnsignedByte()];
        handleLine(getUnsignedShort(), getUnsignedShort(), getUnsignedShort(), getUnsignedShort(), c);
    }

    private void handleLine(int x1, int y1, int x2, int y2, Color c) {
        shape = new SimpleColoredShape(penStyle.createStrokedShape(new Line2D.Double(x1, y1, x2, y2)), c);
    }

    private void handleRectangle() {
        Color c = Parameters.colors[getUnsignedByte()];
        handleRectangle(getUnsignedShort(), getUnsignedShort(), getUnsignedShort(), getUnsignedShort(),
                c);
    }

    private void handleRectangle(int x1, int y1, int x2, int y2, Color c) {
        shape = new SimpleColoredShape(getRectangleStroke(x1, y1, x2, y2), c);
    }

    private void handleHighlight() {
        Color c = Parameters.colors[getUnsignedByte()];
        handleHighlight(getUnsignedShort(), getUnsignedShort(), getUnsignedShort(), getUnsignedShort(),
                c);
    }

    private void handleHighlight(int x1, int y1, int x2, int y2, Color c) {
        shape = new HighlightShape(x1, y1, x2, y2, c, new Color(136, 136, 136, 200), 1);
    }

    private void handleFreehand() {
        Color c = Parameters.colors[getUnsignedByte()];

        int count = getUnsignedShort();

        GeneralPath gp = new GeneralPath();

        gp.moveTo(getUnsignedShort(), getUnsignedShort());
        for (int j = 0; j < count - 1; j++) {
            gp.lineTo(getUnsignedShort(), getUnsignedShort());
        }
        handleFreehand(gp, c);
    }

    private void handleFreehand(GeneralPath gp, Color c) {
        shape = new SimpleColoredShape(penStyle.createStrokedShape(gp), c);
    }

    
    /**
     * The pencil width used for drawing.
     */
    protected float pencilWidth = Parameters.penSize;

    /**
     * The pen style used for drawing.
     */
    protected Stroke penStyle = new BasicStroke(pencilWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);



    private Shape getRectangleStroke(int startx, int starty, int endx, int endy) {
        if (startx < endx) {
            if (starty < endy)
                return penStyle
                        .createStrokedShape(new Rectangle2D.Double(startx, starty, endx - startx, endy - starty));
            else
                return penStyle.createStrokedShape(new Rectangle2D.Double(startx, endy, endx - startx, starty - endy));
        } else {
            if (starty < endy)
                return penStyle.createStrokedShape(new Rectangle2D.Double(endx, starty, startx - endx, endy - starty));
            else
                return penStyle.createStrokedShape(new Rectangle2D.Double(endx, endy, startx - endx, starty - endy));
        }
    }


    private AnnotationMessage[] overShape(int x, int y, LinkedList<AnnotationMessage> shapes) {
        LinkedList<AnnotationMessage> t = new LinkedList<AnnotationMessage>();
        AnnotationMessage[] ret = null;
        Shape s;

        for (int i = 0; i < shapes.size(); i++) {
            s = shapes.get(i).getShape();
            if (s.contains(x, y))
                t.add(shapes.get(i));
        }

        if (t.size() > 0) {
            ret = new AnnotationMessage[t.size()];
            for (int i = 0; i < t.size(); i++)
                ret[i] = t.get(i);
        }

        return ret;
    }
    
    
    /**
     * Gets aformatted string containing the name of the type of <code>Message</code>
     * (the shape contained, or a type such as 'Remove All'), and the timestamp
     * of the message.
     * @return string representation of <code>AnnotationMessage</code>
     */
    @Override
	public String toString() {
        switch(encoding) {
            case ProtocolConstants.LINE:
                return ("Line:\t  " + TTTEditor.getStringFromTime(timestamp,true));
            case ProtocolConstants.RECT:
                return ("Rect:\t  " + TTTEditor.getStringFromTime(timestamp,true));
            case ProtocolConstants.HIGHLIGHT:
                return ("Highlight:\t  " + TTTEditor.getStringFromTime(timestamp,true));
            case ProtocolConstants.FREE:
                return ("Freehand:\t  " + TTTEditor.getStringFromTime(timestamp,true));
            case ProtocolConstants.REMOVE:
                return ("Remove (" + x + "," + y + "):\t  " + TTTEditor.getStringFromTime(timestamp,true));
            case ProtocolConstants.REMOVE_ALL:
                return ("Remove All:\t  " + TTTEditor.getStringFromTime(timestamp,true));
        }
        return ("Annotation:\t  " + TTTEditor.getStringFromTime(timestamp,true));
    }
    
}
