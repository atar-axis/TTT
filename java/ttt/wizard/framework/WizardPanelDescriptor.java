/* WizardPanelDescriptor.java
 * Created on 24. April 2007, 11:19
 * @author Christian Gruber Bakk.techn.
 */

package ttt.wizard.framework;

import java.awt.Component;
import javax.swing.JPanel;

/**
 * The wizard panel descriptor is responsible for all actions 
 * which take place at a specified panel
 */
public class WizardPanelDescriptor {
    
    /**
     * Shows if the next button is a finsh button
     */
    public boolean m_FinishEnable;
    /**
     * Shows if the next button is enable
     */
    public boolean m_NextButtonEnable;
    /**
     * Shows if the back button is enabable
     */
    public boolean m_BackButtonEnable;
    /**
     * Shows if the cancel button is enable
     */
    public boolean m_CancelButtonEnable;
    
    static class FinishIdentifier {
        public static final String ID = "FINISH";
        FinishIdentifier() {
        }
    }
    
    private static final String DEFAULT_PANEL_IDENTIFIER = "defaultPanelIdentifier";    
    
    
    /**
     * If a Finish identifier is returned by the method getNextComponent the wizard will be closed
     */
    public static final FinishIdentifier FINISH = new FinishIdentifier();
    
    private WizardController m_WizardContoller;
    private Component m_MyComponent;
    private Object m_MyObjectIdentifier;
    
    /** Creates a new instance of WizardPanelDescriptor; Sets default panel identifier*/
    public WizardPanelDescriptor() {
        
        m_FinishEnable = false;
        m_NextButtonEnable = true;
        m_BackButtonEnable = true;
        m_CancelButtonEnable = true;
        m_MyObjectIdentifier = DEFAULT_PANEL_IDENTIFIER;
        m_MyComponent = new JPanel();
    }
    
    
    
    /**
     * Creates a new instance of WizardPanelDescriptor with concret objects
     * @param identifier identifier of the descriptor and the component
     * @param component the component the descriptor belongs to
     * @param language language of the text in the panel
     */
    public WizardPanelDescriptor(Object identifier, Component component, String language) {
        m_MyObjectIdentifier = identifier;
        m_MyComponent = component;
    }
    
    
    /**
     * Returns component associated with
     * @return an associated component
     */
    public final Component getComponent() {
        return m_MyComponent;
    }
    
    /**
     * Sets component associated with
     * @param component component which belongs to the descriptor
     */
    public final void setComponent(Component component) {
        m_MyComponent = component;
    }
    
    /**
     * Returns object identifier associated with
     * @return 
     */
    public final Object getObjectIdentifier() {
        return m_MyObjectIdentifier;
    }
    
    /**
     * Sets object identifier associated with
     * @param identifier identifier of the component
     */
    public final void setObjectIdentifier(Object identifier) {
        m_MyObjectIdentifier = identifier;
    }
    
    /**
     * Returns the next component descriptor. If not overridden it returns null - the 
     * next button is disabled
     */
    public Object getNextComponentDescriptor() {
        return null;
    }
    
    /**
     * Returns the last component descriptor. If not overridden it 
     * returns null - the back button is disabled
     */
    public Object getLastComponentDescriptor() {
        return null;
    }
    
    /**
     * Sets the wizard controller
     */
    public void setWizardControler(WizardController wizardController) {
        m_WizardContoller = wizardController;
    }
    
    /**
     * Gets the wizard controller
     */
    public WizardController getWizardController() {
        return m_WizardContoller;
    }
    
    /** restores the controller buttons to the actual state*/
    public void restoreButtonsToPanelState(){
        m_WizardContoller.setNextButton(m_NextButtonEnable);
        m_WizardContoller.setBackButton(m_BackButtonEnable);
        m_WizardContoller.setCancelButton(m_CancelButtonEnable);
        m_WizardContoller.setNextButtonToFinsh(m_FinishEnable);
    }
    
    /**
     * Consists Code that is called before the Panel is displayed
     */
    public void beforeDisplayingPanel() {
    }
    
    /**
     * Consists Code that is called during the Panel is displayed
     */
    public void displayingPanel() {
    }
    
    /**
     * Consists Code that is called after the Panel is displayed
     */
    public void afterDisplayingPanel() {
    }
}
