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
 * Created on 16.03.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import com.sun.media.codec.video.cinepak.CineStore;

import ttt.messages.Annotation;

public class PaintControls extends GradientPanel {

    private JCheckBox activateButton;
    private JRadioButton[] colorButtons;

    private AbstractButton textButton;
    private AbstractButton highlightButton;
    private AbstractButton freeButton;
    private AbstractButton rectangleButton;
    private AbstractButton lineButton;
    private AbstractButton deleteButton;
    private JButton deleteAllButton;

    private AbstractButton whiteboardEnable;
    private JButton whiteboardNext, whiteboardPrevious;

    private PaintListener paintListener;

    public PaintControls(PaintListener paintListener) {
        this.paintListener = paintListener;
        paintListener.setPaintControls(this);

        // initialize GUI
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static final String HIGHLIGHT = "highlight";
    static final String RECTANGLE = "rectangle";
    static final String LINE = "line";
    static final String FREEHAND = "freehand";
    static final String DELETE = "delete";
    static final String DELETE_ALL = "delete all";

    private void jbInit() throws Exception {
        URL urlText = this.getClass().getResource("/ttt/resources/text24.png");
        URL urlActiveText = this.getClass().getResource("/ttt/resources/text_active24.png");
        URL urlRolloverText = this.getClass().getResource("/ttt/resources/text_rollover24.png");
        URL urlRect = this.getClass().getResource("resources/Rectangle24_new.gif");
        URL urlActiveRect = this.getClass().getResource("resources/Rectangle_active24_new.gif");
        URL urlRolloverRect = this.getClass().getResource("resources/Rectangle_rollover24_new.gif");
        URL urlLine = this.getClass().getResource("resources/Line24.gif");
        URL urlActiveLine = this.getClass().getResource("resources/Line_active24.gif");
        URL urlRolloverLine = this.getClass().getResource("resources/Line_rollover24.gif");
        URL urlFree = this.getClass().getResource("resources/Freehand24_new.gif");
        URL urlActiveFree = this.getClass().getResource("resources/Freehand_active24_new.gif");
        URL urlRolloverFree = this.getClass().getResource("resources/Freehand_rollover24_new.gif");
        URL urlHighlight = this.getClass().getResource("resources/Highlight24_new.gif");
        URL urlActiveHighlight = this.getClass().getResource("resources/Highlight_active24_new.gif");
        URL urlRolloverHighlight = this.getClass().getResource("resources/Highlight_rollover24_new.gif");
        URL urlDel = this.getClass().getResource("resources/Delete24.gif");
        URL urlActiveDel = this.getClass().getResource("resources/Delete_active24.gif");
        URL urlRolloverDel = this.getClass().getResource("resources/Delete_rollover24.gif");

        URL urlDelAll = this.getClass().getResource("resources/Delete_all16.gif");
        URL urlActiveDelAll = this.getClass().getResource("resources/Delete_all_active16.gif");
        URL urlEnable = this.getClass().getResource("resources/Enable16.gif");
        URL urlActiveEnable = this.getClass().getResource("resources/Enable_active16.gif");

        URL urlColorButton = this.getClass().getResource("resources/color_button24.gif");
        URL urlActiveColorButton = this.getClass().getResource("resources/color_button_active24.gif");
        URL urlRolloverColorButton = this.getClass().getResource("resources/color_button_rollover24.gif");

        Icon colorIcon = new ImageIcon(urlColorButton);
        Icon colorActiveIcon = new ImageIcon(urlActiveColorButton);
        Icon colorRolloverIcon = new ImageIcon(urlRolloverColorButton);

        activateButton = new JCheckBox();
        activateButton.setIcon(new ImageIcon(urlEnable));
        activateButton.setBorder(BorderFactory.createEmptyBorder());
        activateButton.setSelectedIcon(new ImageIcon(urlActiveEnable));
        activateButton.setToolTipText("Activate painting");
        activateButton.setFocusable(false);

        colorButtons = new JRadioButton[5];
        for (int i = 0; i < colorButtons.length; i++) {
            colorButtons[i] = new JRadioButton();
        }

        // color buttons
        colorButtons[0].setBackground(Color.RED);
        colorButtons[0].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[0].setToolTipText("Choose Color");
        colorButtons[0].setIcon(colorIcon);
        colorButtons[0].setSelectedIcon(colorActiveIcon);
        colorButtons[0].setRolloverIcon(colorRolloverIcon);
        colorButtons[0].setFocusable(false);

        colorButtons[1].setBackground(Color.BLUE);
        colorButtons[1].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[1].setToolTipText("Choose Color");
        colorButtons[1].setIcon(colorIcon);
        colorButtons[1].setSelectedIcon(colorActiveIcon);
        colorButtons[1].setRolloverIcon(colorRolloverIcon);
        colorButtons[1].setFocusable(false);

        colorButtons[2].setBackground(Color.GREEN);
        colorButtons[2].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[2].setToolTipText("Choose Color");
        colorButtons[2].setIcon(colorIcon);
        colorButtons[2].setSelectedIcon(colorActiveIcon);
        colorButtons[2].setRolloverIcon(colorRolloverIcon);
        colorButtons[2].setFocusable(false);

        colorButtons[3].setBackground(Color.YELLOW);
        colorButtons[3].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[3].setToolTipText("Choose Color");
        colorButtons[3].setIcon(colorIcon);
        colorButtons[3].setSelectedIcon(colorActiveIcon);
        colorButtons[3].setRolloverIcon(colorRolloverIcon);
        colorButtons[3].setFocusable(false);

        colorButtons[4].setBackground(Color.BLACK);
        colorButtons[4].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[4].setToolTipText("Choose Color");
        colorButtons[4].setIcon(colorIcon);
        colorButtons[4].setSelectedIcon(colorActiveIcon);
        colorButtons[4].setRolloverIcon(colorRolloverIcon);
        colorButtons[4].setFocusable(false);

        ButtonGroup colorGroup = new ButtonGroup();
        for (int i = 0; i < colorButtons.length; i++)
            colorGroup.add(colorButtons[i]);

        // paint modes
        textButton = new JToggleButton();
        textButton.setToolTipText("Text");
        textButton.setBorder(BorderFactory.createEmptyBorder());
        textButton.setIcon(new ImageIcon(urlText));
        textButton.setSelectedIcon(new ImageIcon(urlActiveText));
        textButton.setRolloverIcon(new ImageIcon(urlRolloverText));
        textButton.setFocusable(false);
        
        highlightButton = new JToggleButton();
        highlightButton.setToolTipText("Highlight");
        highlightButton.setBorder(BorderFactory.createEmptyBorder());
        highlightButton.setIcon(new ImageIcon(urlHighlight));
        highlightButton.setSelectedIcon(new ImageIcon(urlActiveHighlight));
        highlightButton.setRolloverIcon(new ImageIcon(urlRolloverHighlight));
        highlightButton.setFocusable(false);

        freeButton = new JToggleButton();
        freeButton.setToolTipText("Freehand");
        freeButton.setBorder(BorderFactory.createEmptyBorder());
        freeButton.setIcon(new ImageIcon(urlFree));
        freeButton.setSelectedIcon(new ImageIcon(urlActiveFree));
        freeButton.setRolloverIcon(new ImageIcon(urlRolloverFree));
        freeButton.setFocusable(false);

        rectangleButton = new JToggleButton();
        rectangleButton.setToolTipText("Rectangle");
        rectangleButton.setBorder(BorderFactory.createEmptyBorder());
        rectangleButton.setIcon(new ImageIcon(urlRect));
        rectangleButton.setSelectedIcon(new ImageIcon(urlActiveRect));
        rectangleButton.setRolloverIcon(new ImageIcon(urlRolloverRect));
        rectangleButton.setFocusable(false);

        lineButton = new JToggleButton();
        lineButton.setToolTipText("Line");
        lineButton.setBorder(BorderFactory.createEmptyBorder());
        lineButton.setIcon(new ImageIcon(urlLine));
        lineButton.setSelectedIcon(new ImageIcon(urlActiveLine));
        lineButton.setRolloverIcon(new ImageIcon(urlRolloverLine));
        lineButton.setFocusable(false);

        deleteButton = new JToggleButton();
        deleteButton.setToolTipText("Delete");
        deleteButton.setBorder(BorderFactory.createEmptyBorder());
        deleteButton.setIcon(new ImageIcon(urlDel));
        deleteButton.setSelectedIcon(new ImageIcon(urlActiveDel));
        deleteButton.setRolloverIcon(new ImageIcon(urlRolloverDel));
        deleteButton.setFocusable(false);

        deleteAllButton = new JButton();
        deleteAllButton.setToolTipText("Delete all");
        deleteAllButton.setBorder(BorderFactory.createEmptyBorder());
        deleteAllButton.setIcon(new ImageIcon(urlDelAll));
        deleteAllButton.setSelectedIcon(new ImageIcon(urlActiveDelAll));
        deleteAllButton.setFocusable(false);

        ButtonGroup modeButtons = new ButtonGroup();
        modeButtons.add(textButton);
        modeButtons.add(lineButton);
        modeButtons.add(rectangleButton);
        modeButtons.add(freeButton);
        modeButtons.add(highlightButton);
        modeButtons.add(deleteButton);

        // create whiteboard controls
        whiteboardEnable = new RollOverToggleButton(new ImageIcon(this.getClass().getResource("resources/Edit24.gif")));
        whiteboardEnable.setMargin(new Insets(0, 0, 0, 0));
        whiteboardEnable.setActionCommand("switch");
        whiteboardEnable.setToolTipText("Switch between desktop and whiteboard");
        whiteboardEnable.setFocusable(false);

        whiteboardPrevious = new RollOverButton(new ImageIcon(this.getClass().getResource("resources/Back24.gif")));
        whiteboardPrevious.setActionCommand("previous");
        whiteboardPrevious.setToolTipText("previous whiteboard page");
        whiteboardPrevious.setFocusable(false);

        whiteboardNext = new RollOverButton(new ImageIcon(this.getClass().getResource("resources/Forward24.gif")));
        whiteboardNext.setActionCommand("next");
        whiteboardNext.setToolTipText("next whiteboard page");
        whiteboardNext.setFocusable(false);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createRigidArea(new Dimension(20, 0)));
        add(activateButton);
        add(Box.createRigidArea(new Dimension(20, 0)));
        add(colorButtons[0]);
        add(colorButtons[1]);
        add(colorButtons[2]);
        add(colorButtons[3]);
        add(colorButtons[4]);
        add(Box.createRigidArea(new Dimension(20, 0)));
        add(textButton);
        add(freeButton);
        add(highlightButton);
        add(lineButton);
        add(rectangleButton);
        add(deleteButton);
        add(Box.createRigidArea(new Dimension(20, 0)));
        add(deleteAllButton);
        add(Box.createRigidArea(new Dimension(80, 0)));

        add(whiteboardPrevious);
        add(whiteboardEnable);
        add(whiteboardNext);
        add(Box.createGlue());

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // enable/disable painting
                if (event.getSource() == activateButton)
                    paintListener.setActivated(activateButton.isSelected());

                // automatically enable painting
                else if (event.getSource() != deleteAllButton) {
                    activateButton.setSelected(true);
                    paintListener.setActivated(true);
                }

                // set paint mode
                if (event.getSource() == textButton) {
                	paintListener.setPaintMode(Constants.AnnotationText);
                	highlightButton.setSelected(false);
                    freeButton.setSelected(false);
                    lineButton.setSelected(false);
                    rectangleButton.setSelected(false);
                    deleteButton.setSelected(false);
                } else if (event.getSource() == highlightButton) {
                    paintListener.setPaintMode(Constants.AnnotationHighlight);
                    textButton.setSelected(false);
                    freeButton.setSelected(false);
                    lineButton.setSelected(false);
                    rectangleButton.setSelected(false);
                    deleteButton.setSelected(false);
                } else if (event.getSource() == freeButton) {
                    paintListener.setPaintMode(Constants.AnnotationFreehand);
                    textButton.setSelected(false);
                    highlightButton.setSelected(false);
                    lineButton.setSelected(false);
                    rectangleButton.setSelected(false);
                    deleteButton.setSelected(false);
                } else if (event.getSource() == rectangleButton) {
                    paintListener.setPaintMode(Constants.AnnotationRectangle);
                    textButton.setSelected(false);
                    highlightButton.setSelected(false);
                    freeButton.setSelected(false);
                    lineButton.setSelected(false);
                    deleteButton.setSelected(false);
                } else if (event.getSource() == lineButton) {
                    paintListener.setPaintMode(Constants.AnnotationLine);
                    textButton.setSelected(false);
                    highlightButton.setSelected(false);
                    freeButton.setSelected(false);
                    rectangleButton.setSelected(false);
                    deleteButton.setSelected(false);
                } else if (event.getSource() == deleteButton) {
                    paintListener.setPaintMode(Constants.AnnotationDelete);
                    textButton.setSelected(false);
                    highlightButton.setSelected(false);
                    freeButton.setSelected(false);
                    lineButton.setSelected(false);
                    rectangleButton.setSelected(false);
                } else if (event.getSource() == deleteAllButton)
                    paintListener.setPaintMode(Constants.AnnotationDeleteAll);

                // set color
                else if (event.getSource() == colorButtons[0])
                    paintListener.setColor(Annotation.Red);
                else if (event.getSource() == colorButtons[1])
                    paintListener.setColor(Annotation.Blue);
                else if (event.getSource() == colorButtons[2])
                    paintListener.setColor(Annotation.Green);
                else if (event.getSource() == colorButtons[3])
                    paintListener.setColor(Annotation.Yellow);
                else if (event.getSource() == colorButtons[4])
                    paintListener.setColor(Annotation.Black);

                else if (event.getSource() == whiteboardEnable)
                    paintListener.enableWhiteboard(whiteboardEnable.isSelected());
                else if (event.getSource() == whiteboardPrevious)
                    paintListener.previousWhiteboard();
                else if (event.getSource() == whiteboardNext)
                    paintListener.nextWhiteboard();
            }
        };

        activateButton.addActionListener(actionListener);

        textButton.addActionListener(actionListener);
        highlightButton.addActionListener(actionListener);
        freeButton.addActionListener(actionListener);
        rectangleButton.addActionListener(actionListener);
        lineButton.addActionListener(actionListener);
        deleteButton.addActionListener(actionListener);

        deleteAllButton.addActionListener(actionListener);

        whiteboardEnable.addActionListener(actionListener);
        whiteboardPrevious.addActionListener(actionListener);
        whiteboardNext.addActionListener(actionListener);

        for (int i = 0; i < colorButtons.length; i++)
            colorButtons[i].addActionListener(actionListener);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    void setWhiteboardSelected(boolean selected) {
        whiteboardEnable.setSelected(selected);
    }

    // update GUI
    public void updateActivatedState(boolean activated) {
        activateButton.setSelected(activated);
    }

    public void selectColorButton(int color) {
        switch (color) {
        case Annotation.Red:
            colorButtons[0].setSelected(true);
            break;
        case Annotation.Blue:
            colorButtons[1].setSelected(true);
            break;
        case Annotation.Green:
            colorButtons[2].setSelected(true);
            break;
        case Annotation.Yellow:
            colorButtons[3].setSelected(true);
            break;
        case Annotation.Black:
            colorButtons[4].setSelected(true);
            break;
        default:
            break;
        }
    }

    public void selectPaintModeButton(int paintMode) {
        switch (paintMode) {
        case Constants.AnnotationText:
        	textButton.setSelected(true);
        	break;
        case Constants.AnnotationFreehand:
            freeButton.setSelected(true);
            break;
        case Constants.AnnotationHighlight:
            highlightButton.setSelected(true);
            break;
        case Constants.AnnotationLine:
            lineButton.setSelected(true);
            break;
        case Constants.AnnotationRectangle:
            rectangleButton.setSelected(true);
            break;
        case Constants.AnnotationDelete:
            deleteButton.setSelected(true);
            break;
        default:
            break;
        }
    }
}
