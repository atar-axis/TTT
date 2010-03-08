package ttt.editor.tttEditor;

import javax.swing.JPanel;
import java.awt.Rectangle;
import java.util.LinkedList;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;




/**
 * A <code>JComponent</code> which is able to display TTT desktop data,
 * and can deal with the processing of individual <code>Message</code>s.
 */
public class DesktopPanel extends JPanel {
    
    //for zoom
    private int zoomIndex = Parameters.ZOOM_50;
    private double scaleFactor = 1;
    
    //for painting
    private Image memImage;
    private Graphics memGraphics;
    
    //cursor
    private SoftCursor softCursor = null;
    private Point cursorPosition = new Point(0,0);
    
    //annotation list
    private LinkedList<AnnotationMessage> annotations = new LinkedList<AnnotationMessage>();
    
    //whiteboard on or off
    private boolean blankPage = false;
    
    //pen (may not be correctly used / changable)
    private float pencilWidth = Parameters.penSize;
    private Stroke penStyle = new BasicStroke(pencilWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    
    //preferred desktop dimensions
    private int preferredWidth, preferredHeight;
    
    
    /**
     * Class constructor.
     * @param preferredWidth the preferred width of the panel, assuming zoom is 100%
     * @param preferredHeight the preferred height of the panel, assuming zoom is 100%
     */
    public DesktopPanel(int preferredWidth, int preferredHeight) {
        this.preferredWidth = preferredWidth;
        this.preferredHeight = preferredHeight;
        memImage = TTTEditor.getInstance().createImage(preferredWidth, preferredHeight);
        memGraphics = memImage.getGraphics();
        
    }
    
    
    /**
     * Set the zoom level to be used.  Takes an <code>int</code> as a parameter,
     * which has a value of <CODE>ZOOM_TO_FIT</CODE>, <CODE>ZOOM_25</CODE>, <CODE>ZOOM_50</CODE>, <CODE>ZOOM_75</CODE>, or <CODE>ZOOM_100</CODE>,
     * as defined in the <code>Parameters</code> class.
     * @param zoomIndex the new zoom level
     */
    public synchronized void setZoomLevel(int zoomIndex) {
        this.zoomIndex = zoomIndex;
        switch(zoomIndex) {
            case Parameters.ZOOM_25: scaleFactor = 0.5;
            break;
            case Parameters.ZOOM_50: scaleFactor = 0.75;
            break;
            case Parameters.ZOOM_75: scaleFactor = 0.875;
            break;
            case Parameters.ZOOM_100: scaleFactor = 1.0;
            break;
        }
        repaint();
        revalidate();
    }
    
    
    /**
     * Overrides the getPreferredSize() method of superclass in order to
     * take account of the zoom level.
     * <br />
     * NOTE:- in order to calculate the correct dimensions for zoom-to-fit,
     * this method searches the container hierarchy for a <code>JViewport</code>
     * and uses the width and height of it to determine the size of the area
     * in which it may be displayed.  If there is no viewport to be found,
     * the desktop panel's parent is used instead.  This approach may not work in all
     * situations.
     * @return the preferred size
     */
    public Dimension getPreferredSize() {
        if (zoomIndex == Parameters.ZOOM_100)
            return new Dimension(preferredWidth, preferredHeight);
        if (zoomIndex == Parameters.ZOOM_TO_FIT) {
            int width, height;
            Container viewport = SwingUtilities.getAncestorOfClass(JViewport.class, this);
            if (viewport != null) {
                width = viewport.getWidth();
                height = viewport.getHeight();
            }
            else {
                width = getParent().getWidth();
                height = getParent().getHeight();
            }
            scaleFactor = (double)width / preferredWidth;
            if (scaleFactor > (double)height / preferredHeight)
                scaleFactor = (double)height / preferredHeight;
        }
        int w = (int)(preferredWidth * scaleFactor + 0.5);
        int h = (int)(preferredHeight * scaleFactor + 0.5);
        return new Dimension(w, h);
    }
    
    
    /**
     * Get the current zoom level.
     * @return the current zoom level
     */
    public int getZoomIndex() {
        return zoomIndex;
    }
    
    
    /**
     * Process a single <code>Message</code>.  This does not result in a call
     * to repaint, as it may be desirable to process a number of <code>Message</code>s
     * before repainting.  Therefore repainting is left to the discretion of
     * the caller of this method.
     * @param msg the <code>Message</code> to be processed
     */
    public synchronized void processMessage(Message msg) {
        if (msg == null)
            return;
        if (msg instanceof FramebufferMessage) {
            ((FramebufferMessage)msg).paint(memGraphics);
            return;
        } else if (msg instanceof AnnotationMessage) {
            if (((AnnotationMessage)msg).isRemove())
                ((AnnotationMessage)msg).processRemove(annotations);
            else if (((AnnotationMessage)msg).isRemoveAll()) {
                annotations.clear();
            } else {
                annotations.add((AnnotationMessage)msg);
            }
        } else if (msg instanceof BlankPageMessage) {
            blankPage = ((BlankPageMessage)msg).getPageNumber() > 0;
            // added by Ziewer in 01.06.2007
            // whiteboard is impliced remove all event (for new recordings)
            annotations.clear();
            // end added
            return;
        } else if (msg instanceof CursorMoveMessage) {
            updateSoftCursorPosition(((CursorMoveMessage)msg).getCursorPosition());
        } else if (msg instanceof CursorShapeMessage) {
            updateSoftCursorShape(((CursorShapeMessage)msg).getCursor());
        }
    }
    
    
    /**
     * Remove the passed annotation from the annotations currently displayed
     * on screen.  If the annotation is not currently displayed, nothing happens.
     * @param message the annotation to remove
     */
    public synchronized void removeAnnotationFromScreen(AnnotationMessage message) {
        if (annotations.remove(message))
            repaint();
    }
    
    /**
     * Updates the position of the cursor.
     * @param newPosition the new cursor position
     */
    private synchronized void updateSoftCursorPosition(Point newPosition) {
        cursorPosition = newPosition;
        if (softCursor != null) {
            softCursor.setLocation(cursorPosition);
        }
    }
    
    
    /**
     * Updates the shape of the cursor.
     * @param newCursor new cursor shape
     */
    private synchronized void updateSoftCursorShape(SoftCursor newCursor) {
        softCursor = newCursor;
        if (softCursor != null) {
            softCursor.setLocation(cursorPosition);
        }
    }
    
    
    /**
     * Clears the list of annotations which are currently on screen, but
     * does not call repaint() to remove them visually.
     */
    public void resetAnnotationList() {
        annotations.clear();
    }
    
    /**
     * Turns the whiteboard off, but does not call repaint() to update the screen.
     * If the whiteboard was not on, nothing happens.
     */
    public void resetBlankPage() {
        blankPage = false;
    }
    
    
    
    private Rectangle crosshairBounds = null;
    private int crosshairSize = 8;
    private boolean showCrosshair = false; //means that crosshair disappears when playback resumes
    
    /**
     * Paints crosshairs on the screen at the position specified within a passed
     * remove annotation.  Removes any previous crosshair from the screen first,
     * and repaints.  The crosshairs are only added temporarily, and will disappear
     * the next time the <code>DesktopPanel</code> is repainted.
     * @param message the <code>AnnotationMessage</code> containing the remove point -
     * if any other <code>Message</code> is passed nothing happens.
     */
    public void drawRemoveCrosshairs(AnnotationMessage message) {
        if (crosshairBounds != null) {
            repaint();
//only need repaint the bounds if 100% zoom, otherwise need to calculate area and repaint that
            //(lazy way to repaint everything...)
        }
        if (message == null || !(message instanceof AnnotationMessage))
            return;
        Point crosshair = message.getRemoveCoordinates();
        crosshairBounds = new Rectangle(crosshair.x - crosshairSize,
                crosshair.y - crosshairSize, crosshairSize + crosshairSize,
                crosshairSize + crosshairSize);
        showCrosshair = true;
        repaint();
    }
    
    /**
     * Overrides <code>paint</code> method of superclass.
     * Paints the desktop including any annotations etc. which are available.
     * Also takes account of the scale factor to produce a scaled version of
     * the desktop if desired.
     * @param g the <code>Graphics</code> context upon which to paint.
     */
    public void paintComponent(Graphics g) {
        
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;
        
        if (zoomIndex == Parameters.ZOOM_TO_FIT) {
            int width = getVisibleRect().width;
            int height = getVisibleRect().height;
            scaleFactor = (double)width / preferredWidth;
            if (scaleFactor > (double)height / preferredHeight)
                scaleFactor = (double)height / preferredHeight;
        }
        
        g2.scale(scaleFactor, scaleFactor);
        
        if (!blankPage) {
            if (memImage != null) {
                synchronized (memImage) {
                    g2.drawImage(memImage, 0, 0, null);
                }
            }
        } else {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, preferredWidth, preferredHeight);
        }
        
        AnnotationMessage annotation;
        if (annotations != null) {
            for (int i = 0; i < annotations.size(); i++) {
                annotation = annotations.get(i);
                annotation.paint(g2, penStyle);
            }
        }
        
        if (softCursor != null) {
            synchronized(softCursor) {
                softCursor.paint(g2);
            }
        }
        
        if (showCrosshair && crosshairBounds != null) {
            g2.setColor(Color.BLACK);
            g2.drawLine(crosshairBounds.x, crosshairBounds.y + crosshairSize,
                    crosshairBounds.x + crosshairBounds.width, crosshairBounds.y + crosshairSize);
            g2.drawLine(crosshairBounds.x + crosshairSize, crosshairBounds.y,
                    crosshairBounds.x + crosshairSize, crosshairBounds.y + crosshairBounds.height);
            //only paint it once
            showCrosshair = false;
        }
        
        g2.dispose();
    }
}