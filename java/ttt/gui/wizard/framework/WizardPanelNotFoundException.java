/* WizardPanelNotFoundException.java
 * Created on 24. April 2007, 12:49
 * @author Christian Gruber Bakk.techn.
 */

package ttt.gui.wizard.framework;

import javax.swing.JOptionPane;

/**
 * Is thrown if the next panel is not found by the wizard controller
 */
public class WizardPanelNotFoundException extends RuntimeException{
    
    /** Creates a new instance of WizardPanelNotFoundException */
    public WizardPanelNotFoundException(){
     JOptionPane.showMessageDialog(null, "Could Not Find Next Wizard Panel - Program Will Exit!");
     System.exit(1);
    }
    
}
