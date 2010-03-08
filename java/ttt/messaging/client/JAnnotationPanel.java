package ttt.messaging.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import ttt.Constants;
import ttt.messages.*;

/**
 * A swing component for displaying and drawing annotations. Code for painting the annotations
 * is copied from the one in TTT, but there are some extensions
 * and differences compared to the annotation drawing in TTT:
 * <ul>
 * <li>an image can be displayed beneath the annotations (i.e. the sheet image)</li>
 * <li>there're two fields with annotations: one for the annotations that the user created,
 *   and one for the annotations which came from the server as part of a sheet</li>
 * <li>supports creation of text annotations</li>
 * <li>the mouse and keyboard events are handled directly in this class, not by listener
 *   instances</li>
 * <li>the delete tool does not create delete annotations, but directly deletes them</li>
 * </ul>
 * 
 * @author Thomas Doehring
 */
public final class JAnnotationPanel extends JComponent {
	
	public final static long serialVersionUID = 1L;

	protected boolean whiteboardEnabled = false;
	private BufferedImage sheetImage = null;
	private byte[] bImgData = null;
	private Annotation[] sheetAnnotations = null;
	private ArrayList<Annotation> userAnnotations;
	// currently drawn annotation
	private Annotation annotation;
	
	private int paintMode = Constants.AnnotationFreehand;
	// true when text is edited
	private boolean textMode = false;
	private int color = Annotation.Black;
	
	private int screenWidth = 800;
	private int screenHeight = 600;
	
	public JAnnotationPanel() {
		
		sheetImage = null;
		sheetAnnotations = null;
		userAnnotations = new ArrayList<Annotation>();
		
		setVisible(true);
		setEnabled(true);
		setFocusable(true);
		
		// process mouse and keyboard events in this class
		enableEvents(MouseEvent.MOUSE_EVENT_MASK | MouseEvent.MOUSE_MOTION_EVENT_MASK | KeyEvent.KEY_EVENT_MASK);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
	
		g.setColor(Color.BLACK);
		
		// draw sheet image or white background (aka white board)
		if(sheetImage == null) {
			g2d.setColor(Color.white);
			g2d.fillRect(0, 0, screenWidth, screenHeight);
		} else {
			g2d.drawImage(sheetImage, 0, 0, null);
		}
		
		g.setColor(Color.black);
		g.drawRect(0, 0, screenWidth, screenHeight);
		
		paintAnnotations(g2d);
	}
	
	protected void paintAnnotations(Graphics2D g) {
		// paint lecturer's annotations first ...
		if(sheetAnnotations != null) {
			for (Annotation ann : sheetAnnotations) {
				ann.paint(g);
			}
		}
		// ... then those of the user ...
		for (Annotation ann : this.userAnnotations) {
			ann.paint(g);
		}
		// ... and finally the annotation which is currently drawn by user
		if (annotation != null) annotation.paint(g);
	}
	
	@Override
	protected void processMouseEvent(MouseEvent e) {
		if (!areCoordinatesValid(e)) return;
		
		// only left mouse clicks handled
		if (e.getButton() == MouseEvent.BUTTON1) {

			// needed to get keyboard events for text annotations
			requestFocusInWindow();
			
			switch (e.getID()) {
			
			case MouseEvent.MOUSE_PRESSED:
				if(textMode) finishPainting();  // mouse press while text editing ends it
				else set(e.getX(), e.getY());
				break;
				
			case MouseEvent.MOUSE_RELEASED:
				if (!textMode) finishPainting();
				break;
			}
		}
		
		super.processMouseEvent(e);
	}
	
	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		if (!areCoordinatesValid(e)) return;
		
		// process only if left mouse button is pressed while dragging
		if (e.getID() == MouseEvent.MOUSE_DRAGGED && 
				((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)) {
			
			reset(e.getX(), e.getY());
		}
		super.processMouseMotionEvent(e);
	}
	
	@Override
	protected void processKeyEvent(KeyEvent e) {			
		if (textMode && e.getID() == KeyEvent.KEY_TYPED) {
			if(e.getKeyChar() != KeyEvent.CHAR_UNDEFINED &&
					(e.getKeyChar() > 31 || e.getKeyChar() == KeyEvent.VK_ENTER)) {
				// add non special characters to text annotation
				((TextAnnotation)annotation).addChar(e.getKeyChar());
				repaint();
				e.consume();
			}
			if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
				// delete last character when backspace pressed
				((TextAnnotation)annotation).deleteLastChar();
				repaint();
				e.consume();
			}
		}
		super.processKeyEvent(e);
	}
	
    // initiates new temporary annotation
    private void set(int x, int y) {

        switch (paintMode) {
        case Constants.AnnotationHighlight:
            annotation = new HighlightAnnotation(0, color + 3, x, y, x, y);
            annotation.temporary = true;
            break;
        case Constants.AnnotationRectangle:
            annotation = new RectangleAnnotation(0, color, x, y, x, y);
            annotation.temporary = true;
            break;
        case Constants.AnnotationLine:
            annotation = new LineAnnotation(0, color, x, y, x, y);
            annotation.temporary = true;
            break;
        case Constants.AnnotationFreehand:
            annotation = new FreehandAnnotation(0, color);
            ((FreehandAnnotation) annotation).addPoint(x, y);
            annotation.temporary = true;
            break;
        case Constants.AnnotationDelete:
            removeAnnotationsAt(x, y);
            break;
        case Constants.AnnotationText:
        	annotation = new TextAnnotation(0, color, x, y, 0, "");
        	annotation.temporary = true;
        	this.requestFocusInWindow();
        	textMode = true;
        	break;
        }
        
        repaint();

    }

    // updates temporary annotation
    private void reset(int x, int y) {
        // fix if startpoint was outside (can happen during fullscreen)
        if (annotation == null) {
            set(x, y);
            return;
        }

        // cut by framebuffer size
        // not needed because areCoordinatesValid() already limits the coordinate range!?
//        x = Math.max(0, Math.min(x, screenWidth));
//        y = Math.max(0, Math.min(y, screenHeight));

        switch (paintMode) {
        case Constants.AnnotationHighlight:
            ((HighlightAnnotation) annotation).setEndPoint(x, y);
            break;
        case Constants.AnnotationRectangle:
            ((RectangleAnnotation) annotation).setEndPoint(x, y);
            break;
        case Constants.AnnotationLine:
            if (annotation instanceof LineAnnotation) {
                Point point = ((LineAnnotation) annotation).getStartPoint();
                // set horizontal or vertical line depending on x-delta and y-delta
                if (Math.abs(point.x - x) > Math.abs(point.y - y))
                    // reset y
                    y = point.y;
                else
                    // reset x
                    x = point.x;
            }
            ((LineAnnotation) annotation).setEndPoint(x, y);
            break;
        case Constants.AnnotationFreehand:
            ((FreehandAnnotation) annotation).addPoint(x, y);
            break;
        case Constants.AnnotationDelete:
            removeAnnotationsAt(x, y);
        }
        
        repaint();
    }

    private void finishPainting() {
    	if(textMode) {
    		textMode = false;
    		// if text annotation is still empty, discard it
    		if (((TextAnnotation)annotation).isEmpty()) {
    			annotation = null;
    		} else {
    			((TextAnnotation)annotation).trim();
    		}
    	}
        if (annotation != null) {
            // NOTE: reset last coordinates not needed - same as last dragged event
            annotation.temporary = false;
            // force consumer to reset timestamp
            annotation.setTimestamp(0);
            // writeMessage(annotation);
            userAnnotations.add(annotation);
            annotation = null;
            
        }
        repaint();
    }
    
    /**
     * checks if coordinates are outside the bounds of the drawing area
     * @param event  event which contains coordinates
     * @return  true if coordinates are within drawing area
     */
    private boolean areCoordinatesValid(MouseEvent event) {
        boolean areCoordinatesValid = !(event.getX() < 0 || event.getX() > screenWidth
        		|| event.getY() < 0 || event.getY() > screenHeight);

        if (!areCoordinatesValid) {
            // terminate painting
            finishPainting();
        }
        
        return areCoordinatesValid;
    }

    
    /**
     *  find and remove annotations at given coordinates.
     */
    private void removeAnnotationsAt(int x, int y) {
        int i = 0;
        while (i < userAnnotations.size()) {
            if (userAnnotations.get(i).contains(x, y))
                userAnnotations.remove(i);
            else
                i++;
        }
    }
    
    /**
     * delete all user annotations.
     */
    public void clearUserAnnotations() {
    	userAnnotations.clear();
    	repaint();
    }
    
    /**
     * clear all sheet annotations (i.e. lecturer's annotations).
     */
    public void clearSheetAnnotations() {
    	sheetAnnotations = null;
    	repaint();
    }
    
    /**
     * delete the sheet image.
     */
    public void clearBackgroundImage() {
    	this.bImgData = null;
    	this.sheetImage = null;
    	repaint();
    }
    
    /**
     * get all annotations (user annotations and sheet annotations).
     * @return Array containing all annotations
     */
    public Annotation[] getAllAnnotations() {
    	int count = 0;
    	if (sheetAnnotations != null) count += sheetAnnotations.length;
    	if (userAnnotations != null) count += userAnnotations.size();
    	Annotation[] allAnnots = new Annotation[count];
    	int pos = 0;
    	if (sheetAnnotations != null) {
    		for(int i = 0; i < sheetAnnotations.length; i++) {
    			allAnnots[pos++] = sheetAnnotations[i];
    		}
    	}
    	if (userAnnotations != null) {
    		for (Annotation ann : userAnnotations) {
				allAnnots[pos++] = ann; 
			}
    	}
    	return allAnnots;
    }
    
    public void setPaintMode(int paintMode) {
    	// end text input if not yet finished
    	if(textMode) finishPainting();
    	this.paintMode = paintMode;
    }
    
    public void setColor(int color) {
    	this.color = color;
    }
    
    /**
     * get sheet image in its byte representation, i.e. the JPG which came with
     * a sheet message from server.
     * @return the byte representation of the sheet image
     */
    public byte[] getSheetImage() {
    	return this.bImgData;
    }
    
    // temporary store for new drawing size
    private int[] sizeTmp = new int[2];
    
    /**
     * set new size for the drawing area. The action has to be redirected onto EDT, if
     * method is called not from EDT (most probably it's called by message receiver thread). 
     * @param width  new width
     * @param height  new height
     */
    public void setScreenSize(int width, int height) {
    	if (SwingUtilities.isEventDispatchThread()) {
        	this.screenWidth = width;
        	this.screenHeight = height;
        	Dimension d = new Dimension(width, height);
        	setMaximumSize(d);
        	setPreferredSize(d);
        	setMinimumSize(d);    		
    	} else {
    		sizeTmp[0] = width;
    		sizeTmp[1] = height;
    		SwingUtilities.invokeLater(new Runnable() {
    			// @Override
    			public void run() {
    				triggerScreenSizeUpdate();
    			}
    		});
    	}
    }
    /**
     * needed for redirection of new screen size action onto EDT.
     */
    private void triggerScreenSizeUpdate() {
    	setScreenSize(sizeTmp[0], sizeTmp[1]);
    }

    
    // temporary objects needed for redirection of action onto EDT
    private Object syncObject = new Object();
    private BufferedImage tmpImage;
    private byte[] tmpImageBytes;
    private Annotation[] tmpAnnotation;

    /**
     * displays the new sheet including the annotations. If called by receiver thread, the 
     * the parameters are stored and the update is queued on EDT.
     * @param bi  sheet image as BufferedImage
     * @param data  sheet image as binary array (JPG)
     * @param annots  the lecturer's annotations
     */
    public void showSheet(BufferedImage bi, byte[] data, Annotation[] annots) {

    	synchronized(syncObject) {
    		tmpImage = bi;
    		tmpImageBytes = data;
    		tmpAnnotation = annots;
    	}
    	
    	if(SwingUtilities.isEventDispatchThread()) {
    		updateMessage();
    	} else {
    		SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    				updateMessage();
    			}
    		});
    	}
    }   

    private void updateMessage() {
    	synchronized(syncObject) {
    		this.sheetImage = tmpImage;
    		this.sheetAnnotations = tmpAnnotation;
    		this.bImgData = tmpImageBytes;
    	}
    	repaint();
    }
}
