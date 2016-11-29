// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universit�t M�nchen
// 
//    This file is part of TeleTeachingTool.
//
//    TeleTeachingTool is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    TeleTeachingTool is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with TeleTeachingTool.  If not, see <http://www.gnu.org/licenses/>.

/*
 * Created on 12.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.MouseInputAdapter;

import ttt.ProtocolPreferences;
import ttt.messages.Annotation;
import ttt.messages.CursorMessage;
import ttt.messages.CursorPositionMessage;
import ttt.messages.GraphicInterface;
import ttt.messages.Message;
import ttt.messages.MessageConsumer;
import ttt.messages.MessageProducer;
import ttt.messages.WhiteboardMessage;
import ttt.postprocessing.podcast.PodcastCreator;
import ttt.record.Recording;

public class GraphicsContext extends JComponent implements GraphicInterface, MessageConsumer {

    public ProtocolPreferences prefs;
    private MessageProducer producer;

    // constructor - initialize with ProtocolPreferences
    public GraphicsContext(ProtocolPreferences prefs) {
        this.prefs = prefs;
        setDoubleBuffered(true);

        // Tab key should be delivered to VNC server
        setFocusTraversalKeysEnabled(false);

        // initialize
        initializeOffscreenImage();

        addMouseListener(new MouseInputAdapter() {
            public void mouseEntered(MouseEvent e) {
                requestFocusInWindow();
            }

            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        // resize if needed
        addComponentListener(new ComponentAdapter() {
            // TODO: sometimes doesn't work during window risizing because there is now event but scroolbars instead
            public void componentResized(ComponentEvent event) {
                super.componentResized(event);
                rescale();
            }
        });
    }

    // constructor - register to MesageProducer
    public GraphicsContext(MessageProducer producer) {
        this(producer.getProtocolPreferences());

        // register to producer
        this.producer = producer;
        producer.addMessageConsumer(this);
    }

    // return preferences
    public ProtocolPreferences getProtocolPreferences() {
        return prefs;
    }

    public void close() {
        if (producer != null)
            producer.removeMessageConsumer(this);
        producer = null;
        prefs = null;
        rawPixelsImage = null;
        pixelsSource = null;
        pixels8 = null;
        pixels = null;
    }

    // /////////////////////////////////////////////////////////////
    // offscreen images and color table
    // /////////////////////////////////////////////////////////////

    // Color models
    public ColorModel colorModel;
    public Color[] colors;

    // offscreen image
    public Image memImage = null;
    public Graphics memGraphics;

    public Image getScreenshotOld() {
        // does not allow getGraphics() :-(
        return memImage.getScaledInstance(prefs.framebufferWidth, prefs.framebufferHeight, Image.SCALE_FAST);
    }
    
    /**
     * Creates screenshot of current graphics contect including annotations, whiteboard pages, and the cursor.
     * 
     * @see PodcastCreator#createPodcast
     * @see GraphicsContext#getScreenshotWithoutAnnotations
     */
    public BufferedImage getScreenshot() {
    	//Create a buffered image using the default color model
    	BufferedImage screenshot = new BufferedImage(prefs.framebufferWidth, prefs.framebufferHeight, BufferedImage.TYPE_INT_RGB);
    	Graphics g = screenshot.getGraphics();
        if (isWhiteboardEnabled()) {
        	//Create whiteboard page
        	g.setColor(Color.WHITE);
        	g.fillRect(0, 0, prefs.framebufferWidth, prefs.framebufferHeight);
        } else {
        	//Draw desktop
        	g.drawImage(memImage, 0, 0, null);
        }
    	paintAnnotations((Graphics2D)g);	//Paint annotation
        // display cursor
        if (showSoftCursor) {
            int x0 = cursorX - hotX, y0 = cursorY - hotY;
            Rectangle r = new Rectangle(x0, y0, cursorWidth, cursorHeight);
            if (r.intersects(new Rectangle(0,0,screenshot.getWidth(),screenshot.getHeight()))) {
                g.drawImage(softCursor, x0, y0, null);
            }
        }
    	return screenshot;
    }
    
    //MODMSG : changed return type to BufferedImage
    public BufferedImage getScreenshotWithoutAnnotations() {
        BufferedImage screenshot;
        // Create a buffered image using the default color model
        screenshot = new BufferedImage(prefs.framebufferWidth, prefs.framebufferHeight, BufferedImage.TYPE_INT_RGB);

        Graphics g = screenshot.getGraphics();

        // show blank page if whiteboard activated
        if (isWhiteboardEnabled()) {
            g.setColor(Color.white);
            g.fillRect(0, 0, prefs.framebufferWidth, prefs.framebufferHeight);
            // g.setColor(Color.black);
            // g.setFont(g.getFont().deriveFont(100f));
            // g.drawString("WHITEBOARD", 200, 200);
            g.dispose();
        }

        // show desktop
        else {
            // Paint the image onto the buffered image
            g.drawImage(memImage, 0, 0, null);
            g.dispose();
        }
        return screenshot;
    }

    public Image getThumbnailWithoutAnnotations(int thumbnail_scale_factor) {
        Image screenshot;

        // show blank page if whiteboard activated
        if (isWhiteboardEnabled()) {
            screenshot = new BufferedImage(prefs.framebufferWidth/ thumbnail_scale_factor, prefs.framebufferHeight/ thumbnail_scale_factor, BufferedImage.TYPE_INT_RGB);
            Graphics g = screenshot.getGraphics();
            g.setColor(Color.white);
            g.fillRect(0, 0, prefs.framebufferWidth/ thumbnail_scale_factor, prefs.framebufferHeight/ thumbnail_scale_factor);
            // g.setColor(Color.black);
            // g.setFont(g.getFont().deriveFont(100f));
            // g.drawString("WHITEBOARD", 200, 200);
            g.dispose();
        }

        // show desktop
        else {
            // Create a buffered image using the default color model
            screenshot = new BufferedImage(prefs.framebufferWidth/ thumbnail_scale_factor, prefs.framebufferHeight/ thumbnail_scale_factor, BufferedImage.TYPE_INT_RGB);

            // Copy image to buffered image
            Graphics2D g = ((BufferedImage) screenshot).createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // Paint the image onto the buffered image
            g.drawImage(memImage, 0, 0,prefs.framebufferWidth/ thumbnail_scale_factor, prefs.framebufferHeight / thumbnail_scale_factor ,null);
            g.dispose();
        }
        return screenshot;
    }

    // offscreen images for raw data
    private Image rawPixelsImage;
    private MemoryImageSource pixelsSource;
    public byte[] pixels8;
    public int[] pixels;

    // hextile colors
    // TODO: think about reseting
    // can cause minor color errors
    public Color hextile_bg = new Color(0);
    public Color hextile_fg = new Color(0);
    public byte[] hextile_bg_encoded;
    public byte[] hextile_fg_encoded;

    public void setForeground(byte[] color, int offset) {
        System.arraycopy(color, offset, hextile_fg_encoded, 0, prefs.bytesPerPixel);
    }

    public void setBackground(byte[] color, int offset) {
        System.arraycopy(color, offset, hextile_bg_encoded, 0, prefs.bytesPerPixel);
    }

    // initialize all data objectes (e.g. color model, offscreen image)
    public void initializeOffscreenImage() {
        // TODO: 24 Bits -> 3 or 4 bytes???
        hextile_bg_encoded = new byte[prefs.bytesPerPixel];
        hextile_fg_encoded = new byte[prefs.bytesPerPixel];

        switch (prefs.bitsPerPixel) {
        case 8:
            colorModel = new DirectColorModel(prefs.bitsPerPixel, (prefs.redMax << prefs.redShift),
                    (prefs.greenMax << prefs.greenShift), (prefs.blueMax << prefs.blueShift));
            colors = new Color[256];
            for (int i = 0; i < 256; i++)
                colors[i] = new Color(colorModel.getRGB(i));
            pixels8 = new byte[prefs.framebufferWidth * prefs.framebufferHeight];
            pixels = null;
            pixelsSource = new MemoryImageSource(prefs.framebufferWidth, prefs.framebufferHeight, colorModel, pixels8,
                    0, prefs.framebufferWidth);
            break;
        case 16:
            colorModel = new DirectColorModel(prefs.bitsPerPixel, (prefs.redMax << prefs.redShift),
                    (prefs.greenMax << prefs.greenShift), (prefs.blueMax << prefs.blueShift));
            colors = new Color[65536];
            for (int i = 0; i < 65536; i++)
                colors[i] = new Color(colorModel.getRGB(i));
            pixels8 = null;
            pixels = new int[prefs.framebufferWidth * prefs.framebufferHeight];
            pixelsSource = new MemoryImageSource(prefs.framebufferWidth, prefs.framebufferHeight, colorModel, pixels,
                    0, prefs.framebufferWidth);
            break;
        case 24:
        case 32:
            // 32-bit input -> 24-bit output
            colorModel = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
            colors = null;
            pixels8 = null;
            pixels = new int[prefs.framebufferWidth * prefs.framebufferHeight];
            pixelsSource = new MemoryImageSource(prefs.framebufferWidth, prefs.framebufferHeight, colorModel, pixels,
                    0, prefs.framebufferWidth);
        }

        // Create new off-screen image either if it does not exist, or if
        // its geometry should be changed. It's not necessary to replace
        // existing image if only pixel format should be changed.
        if (memImage == null) {
            memImage = new BufferedImage(prefs.framebufferWidth, prefs.framebufferHeight, BufferedImage.TYPE_INT_RGB);
            memGraphics = memImage.getGraphics();
        }

        pixelsSource.setAnimated(true);
        rawPixelsImage = Toolkit.getDefaultToolkit().createImage(pixelsSource);
    }

    // ////////////////////////////////////////////////////////
    // Helper
    // ////////////////////////////////////////////////////////

    // decode color
    public Color decodeColor(int color) {
        byte[] colorField = new byte[4];
        colorField[3] = (byte) ((color >> 24) & 0xFF);
        colorField[2] = (byte) ((color >> 16) & 0xFF);
        colorField[1] = (byte) ((color >> 8) & 0xFF);
        colorField[0] = (byte) ((color >> 0) & 0xFF);
        return decodeColor(colorField);
    }

    public Color decodeColor(byte[] colorField) {
        int color = 0;
        for (int i = 0, shift = 0; i < prefs.bytesPerPixel; i++, shift += 8) {
            color += (colorField[i] & 0xFF) << shift;
        }

        switch (prefs.bitsPerPixel) {
        case 16:
            if (prefs.bigEndian)
                // 16 bit big endian: swap bytes
                color = (color & 0xFF) << 8 | ((color & 0xFF00) >> 8);
        case 8:
            // use color table
            return colors[color];
        default:
            // use default color
            if (prefs.bigEndian) {
                // 24 bit big endian: swap bytes
                color = (color & 0xFF) << 24 | (color >> 8 & 0xFF) << 16 | (color >> 16 & 0xFF) << 8 | color >> 24
                        & 0xFF;
            }
            return new Color(color);
        }
    }

    public int getEncodedColor(byte[] colorField) {
        int color = 0;
        for (int i = 0, shift = 0; i < prefs.bytesPerPixel; i++, shift += 8) {
            color += (colorField[i] & 0xFF) << shift;
        }

        switch (prefs.bitsPerPixel) {
        case 16:
            if (prefs.bigEndian)
                // 16 bit big endian: swap bytes
                color = (color & 0xFF) << 8 | ((color & 0xFF00) >> 8);
        case 8:
            break;
        default:
            // use default color
            if (prefs.bigEndian) {
                // 24 bit big endian: swap bytes
                color = (color & 0xFF) << 24 | (color >> 8 & 0xFF) << 16 | (color >> 16 & 0xFF) << 8 | color >> 24
                        & 0xFF;
            }
        }
        return color;
    }

    // get raw pixel map; only works for raw encoded framebuffer updates
    public int[] getPixelsFromRawImage(int x, int y, int width, int height) {
        int[] pixel = new int[width * height];
        PixelGrabber pixelGrabber = new PixelGrabber(rawPixelsImage, x, y, width, height, pixel, 0, width);
        try {
            pixelGrabber.grabPixels();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pixel;
    }

    // get pixel map from offscreen image
    public int[] getPixels(int x, int y, int width, int height) {
        int[] pixel = new int[width * height];
        PixelGrabber pixelGrabber = new PixelGrabber(memImage, x, y, width, height, pixel, 0, width);
        try {
            pixelGrabber.grabPixels();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return pixel;
    }

    void computeColorSpreading() {
        HashMap<Integer, Integer> hash = new HashMap<Integer, Integer>();
        for (int pixel : getPixels(0, 0, prefs.framebufferWidth, prefs.framebufferHeight)) {
            pixel &= 0x00FFFFFF;
            // if(pixel!=0) System.out.println(pixel);

            Integer i = hash.get(new Integer(pixel));
            if (i == null)
                hash.put(pixel, 1);
            else
                hash.put(pixel, ++i);
        }
        System.out.println("\t" + hash.size() + " entries:");

        Set<Entry<Integer, Integer>> set = hash.entrySet();
        Comparator<Entry<Integer, Integer>> comparator = new Comparator<Entry<Integer, Integer>>() {
            public int compare(Entry<Integer, Integer> arg0, Entry<Integer, Integer> arg1) {
                return -arg0.getValue().compareTo(arg1.getValue());
            }
        };
        TreeSet<Entry<Integer, Integer>> treeSet = new TreeSet<Entry<Integer, Integer>>(comparator);
        treeSet.addAll(set);

        int total = 0;
        for (Entry<Integer, Integer> e : treeSet) {
            int percentage = ((int) (((double) (e.getValue()) / (prefs.framebufferWidth * prefs.framebufferHeight)) * 10000));
            if (total < prefs.framebufferWidth * prefs.framebufferHeight * 0.95) {
                Color color = decodeColor(e.getKey());
                System.out.println("\t" + "[ r:" + color.getRed() + "\tg:" + color.getGreen() + "\tb:"
                        + color.getBlue() + "\t]   " + percentage / 100d + "%\tabsolut " + e.getValue());
                total += e.getValue();
            }
        }
        System.out.println();
    }

    // //////////////////////////////////////////////////////////
    // Painting and Updating
    // //////////////////////////////////////////////////////////

    // paint message
    public void handleMessage(Message message) {
        message.paint(this);
    }

    // update offscreen image
    public void handleUpdatedPixels(int x, int y, int w, int h) {
        // TODO: maybe call from paintComponent, but update only needed parts
        // Draw updated pixels of the off-screen image.
        pixelsSource.newPixels(x, y, w, h);
        memGraphics.setClip(x, y, w, h);
        memGraphics.drawImage(rawPixelsImage, 0, 0, null);
        memGraphics.setClip(0, 0, prefs.framebufferWidth, prefs.framebufferHeight);
    }

    // update offscreen image
    public void handleUpdatedPixels() {
        handleUpdatedPixels(0, 0, prefs.framebufferWidth, prefs.framebufferHeight);
    }

    // TODO: only for testing - get rid of this
    public boolean raw = !true;

    // All painting is performed here.
    public void paintComponent(Graphics graphics) {

        Graphics2D g = (Graphics2D) graphics;

        // scaling / zooming
        g.scale(scaleFactor, scaleFactor);

        // show whiteboard
        if (isWhiteboardEnabled()) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, prefs.framebufferWidth, prefs.framebufferHeight);
            g.setColor(Color.BLACK);
            g.drawString("#" + whiteboardPage, prefs.framebufferWidth - 30, 20);
        }

        // or desktop
        else {
            // display offscreen image
            Image image = raw ? rawPixelsImage : memImage;
            if (image != null)
                synchronized (image) {
                    // TODO: maybe move handleUpdate here, but update only needed parts
                    // handleUpdatedPixels();
                    g.drawImage(image, 0, 0, null);
                }
        }

        // display annotations
        paintAnnotations(g);

        // highlight search results
        // TODO: maybe can be added to list of annotations instead
        if (producer instanceof Recording)
            ((Recording) producer).highlightSearchResults(g);

        // display cursor
        if (showSoftCursor) {
            int x0 = cursorX - hotX, y0 = cursorY - hotY;
            Rectangle r = new Rectangle(x0, y0, cursorWidth, cursorHeight);
            if (r.intersects(g.getClipBounds())) {
                g.drawImage(softCursor, x0, y0, null);
            }
        }

        // rescaling
        g.scale(1d / scaleFactor, 1d / scaleFactor);
    }

    // enable/disable graphical output
    private boolean refreshEnabled = true;
    // TODO: change visibility
    public boolean paint_to_offscreen_image = true;

    public void enableRefresh(boolean refresh) {
        this.refreshEnabled = refresh;
    }

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    // refresh calls repaint, but respects scaling
    public void refresh(int x, int y, int width, int height) {
        if (refreshEnabled)
            super.repaint((int) (x * scaleFactor), (int) (y * scaleFactor), (int) (width * scaleFactor + 1),
                    (int) (height * scaleFactor) + 1);
    }

    public void refresh(Rectangle rectangle) {
        if (rectangle == null)
            refresh();
        else
            refresh(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    public void refresh() {
        if (refreshEnabled)
            super.repaint();
    }

    // ////////////////////////////////////////////////////////////////
    // Whiteboard
    // ////////////////////////////////////////////////////////////////

    // whiteboard (blank page for annotations)
    protected int whiteboardPage;

    public boolean isWhiteboardEnabled() {
        return whiteboardPage > 0;
    }

    // set whiteboard page and corresponding annotion buffer
    public void setWhiteboardPage(int whiteboardPage) {
        this.whiteboardPage = whiteboardPage;
        clearAnnotations();
        refresh();
    }

    // updates for late comers and recorders
    public Message getCurrentWhiteboardMessage() {
        return new WhiteboardMessage(0, whiteboardPage, prefs);
    }

    // // ////////////////////////////////////////////////////////////////
    // // Annotations
    // // ////////////////////////////////////////////////////////////////

    private ArrayList<Annotation> currentAnnotations = new ArrayList<Annotation>();

    // add annotations to annoation list
    synchronized public void addAnnotation(Annotation annotation) {
        currentAnnotations.add(annotation);
    }

    // remove all annotations
    synchronized public void clearAnnotations() {
        currentAnnotations.clear();
    }

    // find and remove annotations at given coordinates
    synchronized public void removeAnnotationsAt(int x, int y) {
        int i = 0;
        while (i < currentAnnotations.size()) {
            if (currentAnnotations.get(i).contains(x, y))
                currentAnnotations.remove(i);
            else
                i++;
        }
    }

    
    //MODMSG
    public Annotation[] getCurrentAnnotationsAsArray() {
    	Annotation[] annots = null;
    	synchronized(currentAnnotations) {
    		annots = new Annotation[currentAnnotations.size()];
    		currentAnnotations.toArray(annots);
    	}
    	return annots;
    }


    // display all annotations
    synchronized private void paintAnnotations(Graphics2D graphics) {
        for (int i = 0; i < currentAnnotations.size(); i++) {
            currentAnnotations.get(i).paint(graphics);
        }

        if (temporaryAnnotation != null)
            temporaryAnnotation.paint(graphics);
    }

    // show temporary annotation while painting
    private Annotation temporaryAnnotation;
    // keep bounds of temporary annotations to clear later
    private Rectangle temporaryAnnotationBounds;

    // set temporary annotation and clear old one
    public void setTemporaryAnnotation(Annotation temporaryAnnotation) {
        // set new
        this.temporaryAnnotation = temporaryAnnotation;

        // get new bounds and refresh
        Rectangle boundsNew = temporaryAnnotation.getBounds();
        if (boundsNew == null) {
            boundsNew = new Rectangle(0, 0, prefs.framebufferWidth, prefs.framebufferHeight);
            refresh();
        } else if (temporaryAnnotationBounds == null) {
            refresh(boundsNew);
        } else {
            temporaryAnnotationBounds.add(boundsNew);
            refresh(temporaryAnnotationBounds);
        }

        // keep bounds for later refresh
        temporaryAnnotationBounds = boundsNew;
    }

    // clear temporary annotation and refresh screen
    public void clearTemporaryAnnotation() {
        temporaryAnnotation = null;
        if (temporaryAnnotationBounds != null)
            refresh(temporaryAnnotationBounds);
        temporaryAnnotationBounds = null;
    }

    // ////////////////////////////////////////////////////////////////
    // Soft Cursor
    // ////////////////////////////////////////////////////////////////

    // Handle cursor shape updates (XCursor and RichCursor encodings).
    private boolean showSoftCursor;

    public int[] softCursorPixels;
    public MemoryImageSource softCursorSource;
    public Image softCursor;

    public int cursorX, cursorY;
    public int cursorWidth, cursorHeight;
    public int hotX, hotY;

    // Moves soft cursor into a particular location.
    synchronized public void softCursorMove(int x, int y) {
        if (showSoftCursor) {
            int old_cursorX = cursorX;
            int old_cursorY = cursorY;

            // set new position
            cursorX = x;
            cursorY = y;

            // clear old and paint new cursor
            refresh(old_cursorX - hotX, old_cursorY - hotY, cursorWidth, cursorHeight);
            refresh(x - hotX, y - hotY, cursorWidth, cursorHeight);
        } else {
            // just set new position
            cursorX = x;
            cursorY = y;
        }
    }

    // Remove soft cursor, dispose resources.
    synchronized public void softCursorFree() {

        if (showSoftCursor) {
            showSoftCursor = false;
            softCursor = null;
            softCursorSource = null;
            softCursorPixels = null;

            refresh(cursorX - hotX, cursorY - hotY, cursorWidth, cursorHeight);
        }
    }

    public void showSoftCursor(boolean show) {
        if (show) {
            // show soft cursor (if available)
            if (softCursor != null) {
                showSoftCursor = true;
                refresh(cursorX - hotX, cursorY - hotY, cursorWidth, cursorHeight);
            }

        } else {
            // disable soft cursor
            if (showSoftCursor) {
                showSoftCursor = false;
                refresh(cursorX - hotX, cursorY - hotY, cursorWidth, cursorHeight);
            }
        }
    }

    // updates for late comers and recorders
    public Message getCurrentCursorPositionMessage() {
        return new CursorPositionMessage(0, cursorX, cursorY);
    }

    // updates for late comers and recorders
    private CursorMessage bufferedCursorMessage;

    public void setCurrentCursorMessage(CursorMessage cursorMessage) {
        bufferedCursorMessage = cursorMessage;
    }

    public Message getCurrentCursorMessage() {
        return bufferedCursorMessage;
    }

    // ////////////////////////////////////////////////////////
    // size and scaling
    // ////////////////////////////////////////////////////////

    // scaling
    // adjust scaling according to component size
    private boolean scaleToFit;
    private double scaleFactor = 1;

    public boolean isScaleToFit() {
        return scaleToFit;
    }

    public void setScaleToFit(boolean scaleToFit) {
        this.scaleToFit = scaleToFit;
        if (scaleToFit) {
            rescale();
            // force resize event (for scrollpane) - size is ignored
            setSize(0, 0);
        }
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    // sets display zoom factor
    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
        // force resize event (for scrollpane) - size is ignored
        setSize(0, 0);
        pack();
    }

    private void rescale() {
        if (scaleToFit) {
            // compute scale factor
            int width, height;

            Component parent = getParent();
            if (parent != null)
                parent = parent.getParent();
            if (parent == null)
                return;

            if (parent instanceof JScrollPane) {
                // within scroll pane - use parents size minus insets
                // getParent() is JViewPort
                // getParent().getParent() is JScrollPane
                // size of viewport can be to small if scroolbars are visible - using scrollpane size minus insets
                Insets insets = ((JScrollPane) parent).getInsets();
                width = parent.getWidth() - insets.left - insets.right;
                height = parent.getHeight() - insets.top - insets.bottom;
            } else {
                // use own size
                width = getWidth();
                height = getHeight();
            }
            // scale
            scaleFactor = (double) width / prefs.framebufferWidth;
            if (scaleFactor > (double) height / prefs.framebufferHeight)
                scaleFactor = (double) height / prefs.framebufferHeight;

            refresh();
        }
    }

    public void enableDraggingIfZoomed() {
        // dragging component if zoomed
        MouseInputAdapter mouseInputAdapter = new MouseInputAdapter() {
            Point position;

            public void mousePressed(MouseEvent event) {
                // TODO: only set cursor if moving is possible
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                // store starting position
                position = event.getPoint();
            }

            public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }

            public void mouseDragged(MouseEvent event) {
                // adjust viewport
                Container parent = getParent();
                if (parent instanceof JViewport) {
                    JViewport viewport = (JViewport) parent;

                    Rectangle rectangle = viewport.getViewRect();

                    rectangle.x += -(event.getX() - position.x);
                    rectangle.y += -(event.getY() - position.y);

                    scrollRectToVisible(rectangle);
                }
            }
        };
        // add mouse listener (dragging zoomed viewer)
        addMouseMotionListener(mouseInputAdapter);
        addMouseListener(mouseInputAdapter);
    }

    // calls pack() of JInternalFrame
    private void pack() {
        Container parent = this;
        do {
            parent = parent.getParent();
            if (parent instanceof JInternalFrame) {
                JInternalFrame frame = ((JInternalFrame) parent);

                frame.pack();
                break;
            }
        } while (parent != null);
    }

    // Callback methods to determine geometry of our Component.
    public Dimension getPreferredSize() {
        return new Dimension((int) (prefs.framebufferWidth * scaleFactor),
                (int) (prefs.framebufferHeight * scaleFactor));
    }

    public Dimension getMinimumSize() {
        return new Dimension((int) (prefs.framebufferWidth * scaleFactor),
                (int) (prefs.framebufferHeight * scaleFactor));
    }

    public Dimension getMaximumSize() {
        return new Dimension((int) (prefs.framebufferWidth * scaleFactor),
                (int) (prefs.framebufferHeight * scaleFactor));
    }
}
