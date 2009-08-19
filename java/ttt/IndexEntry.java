// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universität München
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

package ttt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import ttt.messages.Annotation;
import ttt.messages.HighlightAnnotation;

public class IndexEntry extends JLabel {

    private int timestamp;
    private ImageIcon thumbnail;
    private String title = "";
    private String searchbase_of_page;
    private ArrayList<SearchBaseEntry> words;
    private Index index;

    // //////////////////////////////////////////////
    // constructors
    // //////////////////////////////////////////////
    public IndexEntry(Index index) {
        this(index, "", 0, null, null);
    }

    public IndexEntry(Index index, int timestamp) {
        this(index, "", timestamp, null, null);
    }

    public IndexEntry(Index index, String title, int timestamp, String searchableText, Image thumbnail) {
        this.index = index;
        setTimestamp(timestamp);
        setTitle(title);
        setSearchbase(searchableText);
        setThumbnail(thumbnail);
    }

    // //////////////////////////////////////////////
    // getter and setter
    // //////////////////////////////////////////////
    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public String getTitle() {
        return title;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setThumbnail(Image thumbnail) {
        if (thumbnail != null) {
            this.thumbnail = new ImageIcon(thumbnail, Constants.getStringFromTime(timestamp));
            setIcon(this.thumbnail);
        } else {
            this.thumbnail = null;
            setIcon(new ImageIcon(index.getDefaultThumbnail(), Constants.getStringFromTime(timestamp)));
        }
    }

    public ImageIcon getThumbnail() {
        return thumbnail;
    }

    private ArrayList<Annotation> annotations;

    public void setAnnotations(ArrayList<Annotation> annotations) {
        this.annotations = new ArrayList<Annotation>(annotations);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        // add annotations
        Graphics2D g = (Graphics2D) graphics;
        g.translate(getInsets().left, getInsets().top);
        g.scale(1d / index.getThumbnailScaleFactor(), 1d / index.getThumbnailScaleFactor());

        // show annoattions (if enabled)
        paintAnnotations(g, true);

        // highlight search results
        for (int i = 0; i < results.size(); i++) {
            ColoredShape shape = results.get(i);
            g.setColor(shape.color);
            if (shape.filled)
                g.fill(shape.shape);
            else
                g.draw(shape.shape);
        }

        g.scale(index.getThumbnailScaleFactor(), index.getThumbnailScaleFactor());
        g.translate(-getInsets().left, -getInsets().top);
    }

    // show annoattions (if enabled)
    void paintAnnotations(Graphics2D g, boolean thumbnail) {
        boolean paintHighlightAnnotations = index.getAnnotationsPaintMode() == Index.PAINT_ALL_ANNOTATIONS;
        if (annotations != null)
            if (index.getAnnotationsPaintMode() != Index.PAINT_NO_ANNOTATIONS)
                for (int i = 0; i < annotations.size(); i++) {
                    Annotation annotation = annotations.get(i);
                    if (paintHighlightAnnotations || !(annotation instanceof HighlightAnnotation)) {
                        if (thumbnail)
                            annotation.paintToThumbnail(g);
                        else
                            annotation.paint(g);
                    }
                }
    }

    // //////////////////////////////////////////////
    // search
    // //////////////////////////////////////////////
    public String getSearchbase() {
        return searchbase_of_page;
    }

    // set ASCII searchbase
    public void setSearchbase(String searchbase) {
        // TODO: umlaut reduction
        this.searchbase_of_page = searchbase;
    }

    // set searchbase with per word coordinates (from XML)
    public void setSearchbase(ArrayList<SearchBaseEntry> words) {
        this.words = words;

        // overwrite searchbase string for page
        String page_string = "";
        for (int i = 0; i < words.size(); i++)
            page_string += words.get(i).searchText + " ";
        setSearchbase(page_string);
    }

    // perform search
    public boolean contains(String searchword) {
        // empty search
        if (searchword == null || searchword.equals("")) {
            results.clear();
            return false;
        }

        // ASCII searchbase
        if (words == null)
            return searchbase_of_page.toLowerCase().indexOf(searchword.toLowerCase()) >= 0;

        // XML searchbase
        else
            return getSearchResults(searchword);
    }

    private ArrayList<ColoredShape> results = new ArrayList<ColoredShape>();

    // perform advanced search (XML searchbase with coordinates)
    private boolean getSearchResults(String searchword) {
        results.clear();

        if (words != null) {
            for (int i = 0; i < words.size(); i++)
                words.get(i).contains(searchword, results);
        }

        // any result found?
        return results.size() > 0;
    }

    public void highlightSearchResults(Graphics2D g) {
        if (results != null)
            for (int i = 0; i < results.size(); i++) {
                ColoredShape shape = results.get(i);
                g.setColor(shape.color);
                if (shape.filled)
                    g.fill(shape.shape);
                else
                    g.draw(shape.shape);
            }
    }

    // write all words and coordinates of this index
    public void writeSearchbase(DataOutputStream out) throws IOException {
        if (words == null)
            // empty
            out.writeShort(0);

        else {
            // number of words
            out.writeShort(words.size());

            // write words and coordinates
            for (int i = 0; i < words.size(); i++)
                words.get(i).write(out);
        }
    }

    // read words and coordinates for this index
    public void readSearchbase(DataInputStream in, double ratio) throws IOException {
        // TODO: maybe compare or overwrite

        ArrayList<SearchBaseEntry> words = new ArrayList<SearchBaseEntry>();
        int number_of_words = in.readShort();

        for (int i = 0; i < number_of_words; i++)
            words.add(SearchBaseEntry.read(in, ratio));

        // now set words
        // NOTE: setting all at once will update searchbase of Index Extension (used for backward compatibility)
        setSearchbase(words);
    }

    // TODO: ugly hack
    double getRatio() {
        if (words == null)
            return 1;
        return words.size() == 0 ? 1 : words.get(0).getRatio();
    }

    // ////////////////////////////////////////////////////////////////////
    // keyframes
    // ////////////////////////////////////////////////////////////////////

    private Image keyframe_image;

    public void setKeyframe(Image keyframe) {
        this.keyframe_image = keyframe;
    }

    public Image getKeyframe() {
        return keyframe_image;
    }

    public void paintKeyframe(GraphicsContext graphicsContext) {
        graphicsContext.memGraphics.setColor(Color.CYAN);
        graphicsContext.memGraphics.fillRect(0, 0, graphicsContext.prefs.framebufferWidth,
                graphicsContext.prefs.framebufferHeight);

        if (keyframe_image != null) {
            graphicsContext.memGraphics.drawImage(keyframe_image, 0, 0, null);
        }
    }
}