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
 * Created on 17.02.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.gui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import ttt.TTT;

public class GradientPanel extends JPanel {

    private boolean vertical;

    public GradientPanel() {
        this(SwingConstants.VERTICAL);
    }

    public GradientPanel(int orientation) {
    	if (TTT.isEnabledNativeLookAndFeel() == false) {
    		vertical = orientation == SwingConstants.VERTICAL;
    	}
    }

     protected void paintComponent(Graphics g) {

         super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (vertical) {
            // A non-cyclic gradient
            double change = 0.65;
            Color startColor = new Color(221, 232, 243);
            Color endColor = new Color(184, 207, 229);

            GradientPaint gradient = new GradientPaint(0, 0, startColor, 0, (int) (change * getHeight()), Color.white);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), (int) (change * getHeight()));

            gradient = new GradientPaint(0, (int) (change * getHeight()), Color.white, 0, getHeight(), endColor);
            g2d.setPaint(gradient);
            g2d.fillRect(0, (int) (change * getHeight()), getWidth(), getHeight());

        } else {
            // A non-cyclic gradient
            double change = 0.35;
            double change2 = 0.35;
            Color startColor = new Color(221, 232, 243);
            Color endColor = new Color(221, 232, 243);

            GradientPaint gradient = new GradientPaint(0, 0, startColor, (int) (change * getWidth()), 0, Color.white);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, (int) (change * getWidth()), getHeight());

            g2d.setPaint(Color.WHITE);
            g2d.fillRect((int) (change * getWidth()), 0, (int) (change2 * getWidth()), getHeight());

            gradient = new GradientPaint((int) (change2 * getWidth()), 0, Color.white, getWidth(), 0, endColor);
            g2d.setPaint(gradient);
            g2d.fillRect((int) (change2 * getWidth()), 0, getWidth(), getHeight());
        }
    }
}
