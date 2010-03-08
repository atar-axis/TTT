package ttt.messaging.gui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import ttt.messages.Annotation;

/**
 * this class is used in conjunction with {@code JOptionPane}
 * to display a dialog in which a user can choose a color and a number.
 */
public class QuickPollCreateDialog extends JPanel {
	
	static final long serialVersionUID = 1L;

	private JRadioButton[] colorButtons;
	private JRadioButton[] countButtons;
	
	/**
	 * creates a new instance of the dialog. Use it as parameter for
	 * {@code JOptionPane..showConfirmDialog()}.
	 */
	public QuickPollCreateDialog() {
		
		super();
		
		setLayout(new BorderLayout());
		
		JPanel colorPanel = new JPanel();
		colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.X_AXIS));

		// -----------  create color choices ------------- //
		
		JLabel lblColor = new JLabel("Select Color:");
		colorPanel.add(lblColor);
		
		colorPanel.add(Box.createGlue());
		
        colorButtons = new JRadioButton[5];
        for (int i = 0; i < colorButtons.length; i++) {
            colorButtons[i] = new JRadioButton();
        }

        // setBackground() does not work in MAC OS Look-and-Feel, so must use
        // separate image files for each color
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
        for (int i = 0; i < colorButtons.length; i++) {
            colorGroup.add(colorButtons[i]);
            colorPanel.add(colorButtons[i]);
        }
        add(colorPanel, BorderLayout.NORTH);
        
        
        // --------------  create number choices -------------- //
        
        JPanel countPanel = new JPanel();
        countPanel.setLayout(new BoxLayout(countPanel, BoxLayout.X_AXIS));
        
        JLabel lblCount = new JLabel("Select nr of answers:");
        countPanel.add(lblCount);
        
        countPanel.add(Box.createGlue());
        
        countButtons = new JRadioButton[7];
        ButtonGroup countGroup = new ButtonGroup();
        for(int i = 2; i < 9; i++) {
        	JRadioButton btn = new JRadioButton(String.valueOf(i));
        	btn.setToolTipText("select number of answers");
        	if (i == 3) btn.setSelected(true);
        	countButtons[i-2] = btn;
        	countGroup.add(btn);
        	countPanel.add(btn);
        }
        
        add(countPanel, BorderLayout.SOUTH);
	}
	
	/**
	 * @return the color, the user has chosen
	 */
	public int getColor() {
		for(int i = 0; i < colorButtons.length; i++) {
			if(colorButtons[i].isSelected()) {
				switch(i) {
				case 0: return Annotation.Red;
				case 1: return Annotation.Blue;
				case 2: return Annotation.Green;
				case 3: return Annotation.Yellow;
				case 4: return Annotation.Black;
				}
			}
		}
		return Annotation.Black;
	}
	
	/**
	 * @return the number the user has chosen
	 */
	public int getNumber() {
		for(int i = 0; i < countButtons.length; i++) {
			if(countButtons[i].isSelected()) return i+2;
		}
		return 3;
	}
}
