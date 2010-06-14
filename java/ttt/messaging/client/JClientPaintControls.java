package ttt.messaging.client;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


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
       
        colorButtons = new JRadioButton[5];
        for (int i = 0; i < colorButtons.length; i++) {
            colorButtons[i] = new JRadioButton();
        }

        // color buttons
        // setBackground() does not work in MAC OS Look-andnew ImageIcon(urlRolloverDel)-Feel, so must use
        // separate image files for each button
        colorButtons[0].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[0].setToolTipText("Choose Color");
        colorButtons[0].setIcon(Constants.getIcon("color_button24_red.png"));
        colorButtons[0].setSelectedIcon(Constants.getIcon("color_button_active24_red.png"));
        colorButtons[0].setRolloverIcon(Constants.getIcon("color_button_rollover24_red.png"));

        colorButtons[1].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[1].setToolTipText("Choose Color");
        colorButtons[1].setIcon(Constants.getIcon("color_button24_blue.png"));
        colorButtons[1].setSelectedIcon(Constants.getIcon("color_button_active24_blue.png"));
        colorButtons[1].setRolloverIcon(Constants.getIcon("color_button_rollover24_blue.png"));

        colorButtons[2].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[2].setToolTipText("Choose Color");
        colorButtons[2].setIcon(Constants.getIcon("color_button24_green.png"));
        colorButtons[2].setSelectedIcon(Constants.getIcon("color_button_active24_green.png"));
        colorButtons[2].setRolloverIcon(Constants.getIcon("color_button_rollover24_green.png"));

        colorButtons[3].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[3].setToolTipText("Choose Color");
        colorButtons[3].setIcon(Constants.getIcon("color_button24_yellow.png"));
        colorButtons[3].setSelectedIcon(Constants.getIcon("color_button_active24_yellow.png"));
        colorButtons[3].setRolloverIcon(Constants.getIcon("color_button_rollover24_yellow.png"));

        colorButtons[4].setBorder(BorderFactory.createEmptyBorder());
        colorButtons[4].setToolTipText("Choose Color");
        colorButtons[4].setIcon(Constants.getIcon("color_button24_black.png"));
        colorButtons[4].setSelectedIcon(Constants.getIcon("color_button_active24_black.png"));
        colorButtons[4].setRolloverIcon(Constants.getIcon("color_button_rollover24_black.png"));
        colorButtons[4].setSelected(true);

        ButtonGroup colorGroup = new ButtonGroup();
        for (int i = 0; i < colorButtons.length; i++)
            colorGroup.add(colorButtons[i]);

        // paint modes
        textButton = new JToggleButton();
        textButton.setToolTipText("Text");
        textButton.setBorder(BorderFactory.createEmptyBorder());
        textButton.setIcon(Constants.getIcon("text24.png"));
        textButton.setSelectedIcon(Constants.getIcon("text_active24.png"));
        textButton.setRolloverIcon(Constants.getIcon("text_rollover24.png"));
        
        highlightButton = new JToggleButton();
        highlightButton.setToolTipText("Highlight");
        highlightButton.setBorder(BorderFactory.createEmptyBorder());
        highlightButton.setIcon(Constants.getIcon("Highlight24_new.gif"));
        highlightButton.setSelectedIcon(Constants.getIcon("Highlight_active24_new.gif"));
        highlightButton.setRolloverIcon(Constants.getIcon("Highlight_rollover24_new.gif"));

        freeButton = new JToggleButton();
        freeButton.setToolTipText("Freehand");
        freeButton.setBorder(BorderFactory.createEmptyBorder());
        freeButton.setIcon(Constants.getIcon("Freehand24_new.gif"));
        freeButton.setSelectedIcon(Constants.getIcon("Freehand_active24_new.gif"));
        freeButton.setRolloverIcon(Constants.getIcon("Freehand_rollover24_new.gif"));

        rectangleButton = new JToggleButton();
        rectangleButton.setToolTipText("Rectangle");
        rectangleButton.setBorder(BorderFactory.createEmptyBorder());
        rectangleButton.setIcon(Constants.getIcon("Rectangle24_new.gif"));
        rectangleButton.setSelectedIcon(Constants.getIcon("Rectangle_active24_new.gif"));
        rectangleButton.setRolloverIcon(Constants.getIcon("Rectangle_rollover24_new.gif"));

        lineButton = new JToggleButton();
        lineButton.setToolTipText("Line");
        lineButton.setBorder(BorderFactory.createEmptyBorder());
        lineButton.setIcon(Constants.getIcon("Line24.gif"));
        lineButton.setSelectedIcon(Constants.getIcon("Line_active24.gif"));
        lineButton.setRolloverIcon(Constants.getIcon("Line_rollover24.gif"));

        deleteButton = new JToggleButton();
        deleteButton.setToolTipText("Delete");
        deleteButton.setBorder(BorderFactory.createEmptyBorder());
        deleteButton.setIcon(Constants.getIcon("Delete24.gif"));
        deleteButton.setSelectedIcon(Constants.getIcon("Delete_active24.gif"));
        deleteButton.setRolloverIcon(Constants.getIcon("Delete_rollover24.gif"));

        deleteAllButton = new JButton();
        deleteAllButton.setToolTipText("Delete all your annotations");
        deleteAllButton.setBorder(BorderFactory.createEmptyBorder());
        deleteAllButton.setIcon(Constants.getIcon("Delete_all16.gif"));
        deleteAllButton.setSelectedIcon(Constants.getIcon("Delete_all_active16.gif"));

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

        btnDeleteSheetAnn = new JButton(Constants.getIcon("msgclient_delsheetann.png"));
        btnDeleteSheetAnn.setBorder(BorderFactory.createEmptyBorder());
        btnDeleteSheetAnn.setToolTipText("delete all annotations which came with sheet");
        add(btnDeleteSheetAnn);
        add(Box.createGlue());
        btnWB = new JButton(Constants.getIcon("msgclient_wb.png"));
        btnWB.setToolTipText("new whiteboard");
        add(btnWB);
        JButton btnGetSheet = new JButton(Constants.getIcon("msgclient_getsheet.png"));
        btnGetSheet.setToolTipText("get current sheet");
        btnGetSheet.setActionCommand("getSheet");
        btnGetSheet.addActionListener(cc);
        add(btnGetSheet);
        JButton btnGetSheetAnn = new JButton(Constants.getIcon("msgclient_getsheetann2.png"));
        btnGetSheetAnn.setToolTipText("get current sheet or whiteboard with annotations");
        btnGetSheetAnn.setActionCommand("getSheetAnn");
        btnGetSheetAnn.addActionListener(cc);
        add(btnGetSheetAnn);
        add(Box.createRigidArea(new Dimension(20,0)));

        btnVote = new JButton(Constants.getIcon("msgclient_vote.png"));
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
