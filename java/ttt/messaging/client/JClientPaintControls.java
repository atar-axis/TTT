package ttt.messaging.client;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.*;

import ttt.Constants;
import ttt.gui.GradientPanel;
import ttt.messages.Annotation;

/**
 * displays a toolbar with the tools the user can use to create annotations and
 * to interact with server (get sheets/annotations & polls).
 * @author Thomas Doehring
 */
public class JClientPaintControls extends GradientPanel {
	
	public final static long serialVersionUID = 1L;

    private JRadioButton[] colorButtons;

    private AbstractButton textButton;
    private AbstractButton highlightButton;
    private AbstractButton freeButton;
    private AbstractButton rectangleButton;
    private AbstractButton lineButton;
    private AbstractButton deleteButton;
    private JButton deleteAllButton;
    private JButton btnDeleteSheetAnn;
    private JButton btnWB;
    private JButton btnVote;
    
    private JAnnotationPanel anotPanel;
    private ClientController cc;
    
    public JClientPaintControls(JAnnotationPanel pnl, ClientController cc) {
     	anotPanel = pnl;
     	this.cc = cc;
     	this.setFocusable(true);
     	try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private void jbInit() throws Exception {
        URL urlRect = this.getClass().getResource("../../../resources/Rectangle24_new.gif");
        URL urlActiveRect = this.getClass().getResource("../../../resources/Rectangle_active24_new.gif");
        URL urlRolloverRect = this.getClass().getResource("../../../resources/Rectangle_rollover24_new.gif");
        URL urlLine = this.getClass().getResource("../../../resources/Line24.gif");
        URL urlActiveLine = this.getClass().getResource("../../../resources/Line_active24.gif");
        URL urlRolloverLine = this.getClass().getResource("../../../resources/Line_rollover24.gif");
        URL urlFree = this.getClass().getResource("../../../resources/Freehand24_new.gif");
        URL urlActiveFree = this.getClass().getResource("../../../resources/Freehand_active24_new.gif");
        URL urlRolloverFree = this.getClass().getResource("../../../resources/Freehand_rollover24_new.gif");
        URL urlHighlight = this.getClass().getResource("../../../resources/Highlight24_new.gif");
        URL urlActiveHighlight = this.getClass().getResource("../../../resources/Highlight_active24_new.gif");
        URL urlRolloverHighlight = this.getClass().getResource("../../../resources/Highlight_rollover24_new.gif");
        URL urlDel = this.getClass().getResource("../../../resources/Delete24.gif");
        URL urlActiveDel = this.getClass().getResource("../../../resources/Delete_active24.gif");
        URL urlRolloverDel = this.getClass().getResource("../../../resources/Delete_rollover24.gif");

        URL urlText = this.getClass().getResource("../../../resources/text24.png");
        URL urlActiveText = this.getClass().getResource("../../../resources/text_active24.png");
        URL urlRolloverText = this.getClass().getResource("../../../resources/text_rollover24.png");
        
        URL urlDelAll = this.getClass().getResource("../../../resources/Delete_all16.gif");
        URL urlActiveDelAll = this.getClass().getResource("../../../resources/Delete_all_active16.gif");

        URL urldelSheetAnn = this.getClass().getResource("../../../resources/msgclient_delsheetann.png");
        URL urlWB = this.getClass().getResource("../../../resources/msgclient_wb.png");
        URL urlgetSheet = this.getClass().getResource("../../../resources/msgclient_getsheet.png");
        URL urlgetSheetAnn = this.getClass().getResource("../../../resources/msgclient_getsheetann2.png");
        URL urlVote = this.getClass().getResource("../../../resources/msgclient_vote.png");

        colorButtons = new JRadioButton[5];
        for (int i = 0; i < colorButtons.length; i++) {
            colorButtons[i] = new JRadioButton();
        }

        // color buttons
        // setBackground() does not work in MAC OS Look-and-Feel, so must use
        // separate image files for each button
        colorButtons[0].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[0].setToolTipText("Choose Color");
        colorButtons[0].setIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button24_red.png")));
        colorButtons[0].setSelectedIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_active24_red.png")));
        colorButtons[0].setRolloverIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_rollover24_red.png")));

        colorButtons[1].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[1].setToolTipText("Choose Color");
        colorButtons[1].setIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button24_blue.png")));
        colorButtons[1].setSelectedIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_active24_blue.png")));
        colorButtons[1].setRolloverIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_rollover24_blue.png")));

        colorButtons[2].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[2].setToolTipText("Choose Color");
        colorButtons[2].setIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button24_green.png")));
        colorButtons[2].setSelectedIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_active24_green.png")));
        colorButtons[2].setRolloverIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_rollover24_green.png")));

        colorButtons[3].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[3].setToolTipText("Choose Color");
        colorButtons[3].setIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button24_yellow.png")));
        colorButtons[3].setSelectedIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_active24_yellow.png")));
        colorButtons[3].setRolloverIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_rollover24_yellow.png")));

        colorButtons[4].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[4].setToolTipText("Choose Color");
        colorButtons[4].setIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button24_black.png")));
        colorButtons[4].setSelectedIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_active24_black.png")));
        colorButtons[4].setRolloverIcon(new ImageIcon(this.getClass().getResource("../../../resources/color_button_rollover24_black.png")));
        colorButtons[4].setSelected(true);

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
        
        highlightButton = new JToggleButton();
        highlightButton.setToolTipText("Highlight");
        highlightButton.setBorder(BorderFactory.createEmptyBorder());
        highlightButton.setIcon(new ImageIcon(urlHighlight));
        highlightButton.setSelectedIcon(new ImageIcon(urlActiveHighlight));
        highlightButton.setRolloverIcon(new ImageIcon(urlRolloverHighlight));

        freeButton = new JToggleButton();
        freeButton.setToolTipText("Freehand");
        freeButton.setBorder(BorderFactory.createEmptyBorder());
        freeButton.setIcon(new ImageIcon(urlFree));
        freeButton.setSelectedIcon(new ImageIcon(urlActiveFree));
        freeButton.setRolloverIcon(new ImageIcon(urlRolloverFree));

        rectangleButton = new JToggleButton();
        rectangleButton.setToolTipText("Rectangle");
        rectangleButton.setBorder(BorderFactory.createEmptyBorder());
        rectangleButton.setIcon(new ImageIcon(urlRect));
        rectangleButton.setSelectedIcon(new ImageIcon(urlActiveRect));
        rectangleButton.setRolloverIcon(new ImageIcon(urlRolloverRect));

        lineButton = new JToggleButton();
        lineButton.setToolTipText("Line");
        lineButton.setBorder(BorderFactory.createEmptyBorder());
        lineButton.setIcon(new ImageIcon(urlLine));
        lineButton.setSelectedIcon(new ImageIcon(urlActiveLine));
        lineButton.setRolloverIcon(new ImageIcon(urlRolloverLine));

        deleteButton = new JToggleButton();
        deleteButton.setToolTipText("Delete");
        deleteButton.setBorder(BorderFactory.createEmptyBorder());
        deleteButton.setIcon(new ImageIcon(urlDel));
        deleteButton.setSelectedIcon(new ImageIcon(urlActiveDel));
        deleteButton.setRolloverIcon(new ImageIcon(urlRolloverDel));

        deleteAllButton = new JButton();
        deleteAllButton.setToolTipText("Delete all your annotations");
        deleteAllButton.setBorder(BorderFactory.createEmptyBorder());
        deleteAllButton.setIcon(new ImageIcon(urlDelAll));
        deleteAllButton.setSelectedIcon(new ImageIcon(urlActiveDelAll));

        ButtonGroup modeButtons = new ButtonGroup();
        modeButtons.add(textButton);
        modeButtons.add(lineButton);
        modeButtons.add(rectangleButton);
        modeButtons.add(freeButton);
        modeButtons.add(highlightButton);
        modeButtons.add(deleteButton);


        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
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

        btnDeleteSheetAnn = new JButton(new ImageIcon(urldelSheetAnn));
        btnDeleteSheetAnn.setBorder(BorderFactory.createEmptyBorder());
        btnDeleteSheetAnn.setToolTipText("delete all annotations which came with sheet");
        add(btnDeleteSheetAnn);
        add(Box.createGlue());
        btnWB = new JButton(new ImageIcon(urlWB));
        btnWB.setToolTipText("new whiteboard");
        add(btnWB);
        JButton btnGetSheet = new JButton(new ImageIcon(urlgetSheet));
        btnGetSheet.setToolTipText("get current sheet");
        btnGetSheet.setActionCommand("getSheet");
        btnGetSheet.addActionListener(cc);
        add(btnGetSheet);
        JButton btnGetSheetAnn = new JButton(new ImageIcon(urlgetSheetAnn));
        btnGetSheetAnn.setToolTipText("get current sheet or whiteboard with annotations");
        btnGetSheetAnn.setActionCommand("getSheetAnn");
        btnGetSheetAnn.addActionListener(cc);
        add(btnGetSheetAnn);
        add(Box.createRigidArea(new Dimension(20,0)));

        btnVote = new JButton(new ImageIcon(urlVote));
        btnVote.setToolTipText("vote on polls");
        btnVote.setBorder(BorderFactory.createEmptyBorder());
        btnVote.addActionListener(cc);
        btnVote.setActionCommand("vote");
        add(btnVote);
        add(Box.createRigidArea(new Dimension(20,0)));

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                // set paint mode
                if (event.getSource() == highlightButton) {
                    anotPanel.setPaintMode(Constants.AnnotationHighlight);
                } else if (event.getSource() == freeButton) {
                    anotPanel.setPaintMode(Constants.AnnotationFreehand);
                } else if (event.getSource() == rectangleButton) {
                	anotPanel.setPaintMode(Constants.AnnotationRectangle);
                } else if (event.getSource() == lineButton) {
                	anotPanel.setPaintMode(Constants.AnnotationLine);
                } else if (event.getSource() == deleteButton) {
                	anotPanel.setPaintMode(Constants.AnnotationDelete);
                } else if (event.getSource() == textButton) {
                	anotPanel.setPaintMode(Constants.AnnotationText);
                } else if (event.getSource() == deleteAllButton) {
                	anotPanel.clearUserAnnotations();
                } else if (event.getSource() == btnDeleteSheetAnn) {
                	anotPanel.clearSheetAnnotations();
                } else if (event.getSource() == btnWB) {
                	anotPanel.clearBackgroundImage();
                }
                
                // set color
                else if (event.getSource() == colorButtons[0])
                	anotPanel.setColor(Annotation.Red);
                else if (event.getSource() == colorButtons[1])
                	anotPanel.setColor(Annotation.Blue);
                else if (event.getSource() == colorButtons[2])
                	anotPanel.setColor(Annotation.Green);
                else if (event.getSource() == colorButtons[3])
                	anotPanel.setColor(Annotation.Yellow);
                else if (event.getSource() == colorButtons[4])
                	anotPanel.setColor(Annotation.Black);
            }
        };

        textButton.addActionListener(actionListener);
        highlightButton.addActionListener(actionListener);
        freeButton.addActionListener(actionListener);
        rectangleButton.addActionListener(actionListener);
        lineButton.addActionListener(actionListener);
        deleteButton.addActionListener(actionListener);

        deleteAllButton.addActionListener(actionListener);
        btnDeleteSheetAnn.addActionListener(actionListener);
        btnWB.addActionListener(actionListener);

        for (int i = 0; i < colorButtons.length; i++)
            colorButtons[i].addActionListener(actionListener);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

}
