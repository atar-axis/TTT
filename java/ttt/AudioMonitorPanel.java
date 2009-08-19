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

/*
 * Created on 20.11.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

public class AudioMonitorPanel extends JPanel {

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Audio Volume Meter");
        frame.add(new AudioMonitorPanel(true));

        frame.pack();
        frame.setVisible(true);
    }

    private JSAAudioRecorder wavAudioRecorder;

    public AudioMonitorPanel(boolean active) throws Exception {
        setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        if (active) {
            // start own recorder for monitoring purpose
            wavAudioRecorder = new JSAAudioRecorder(this);
            wavAudioRecorder.startRecording(null);
        }
    }

    public void stop() {
        if (wavAudioRecorder != null) {
            wavAudioRecorder.stopRecording();
            wavAudioRecorder = null;
        }
    }

    // one rectangle shows the color gradient while
    // the second one is responsible to show percentage of the first
    private Rectangle2D gradientRec;
    private Rectangle2D overlappingRec;

    private Color startColor = Color.GREEN;
    private Color endColor = Color.RED;

    private double peakPercentage = 0;

    /**
     * Sets the current volume input peak of the volumebar
     * 
     * @param percent
     *            relative volume level
     */
    public void setPeakPercentage(double percent) {
        peakPercentage = percent;
        repaint();
    }

    /**
     * Paints the volumebar - depends on its component size
     * 
     * @param g
     */

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getWidth() > getHeight()) {
            gradientRec = new Rectangle2D.Float(0f, 0f, this.getWidth(), this.getHeight());

            float xCoordOverlappingRec = (float) (this.getWidth() * peakPercentage);
            float yCoordOverlappingRec = 0f;
            float widthOverlappingRec = (float) (this.getWidth() * (1 - peakPercentage));
            float heightOverlappingRec = this.getHeight();

            overlappingRec = new Rectangle2D.Float(xCoordOverlappingRec, yCoordOverlappingRec, widthOverlappingRec,
                    heightOverlappingRec);

            Graphics2D graphics = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(0, 0, startColor, 1.2f * this.getWidth(), 0, endColor, false);

            graphics.setPaint(gp);
            graphics.fill(gradientRec);
            graphics.setPaint(Color.GRAY);
            graphics.fill(overlappingRec);
        } else {
            gradientRec = new Rectangle2D.Float(0f, 0f, this.getWidth(), this.getHeight());

            float yCoordOverlappingRec = 0f; // (float) (this.getHeight() * peakPercentage);
            float xCoordOverlappingRec = 0f;
            float heightOverlappingRec = (float) (this.getHeight() * (1 - peakPercentage));
            float widthOverlappingRec = this.getWidth();

            overlappingRec = new Rectangle2D.Float(xCoordOverlappingRec, yCoordOverlappingRec, widthOverlappingRec,
                    heightOverlappingRec);

            Graphics2D graphics = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(0, 1.2f * this.getHeight(), startColor, 0, 0, endColor, false);

            graphics.setPaint(gp);
            graphics.fill(gradientRec);
            graphics.setPaint(Color.GRAY);
            graphics.fill(overlappingRec);
        }
    }

}
