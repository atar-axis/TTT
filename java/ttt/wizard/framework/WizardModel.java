/* WizardModel.java
 * Created on 24. April 2007, 12:10
 * @author Christian Gruber Bakk.techn.
 */

package ttt.wizard.framework;

// import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * The wizard model is responsible for the state of the wizard. For example it 
 * holds the actual panel which is displayed.
 */
public class WizardModel {
    
    private WizardPanelDescriptor currentPanel;
    
    // all registered panels
    private HashMap panelHashmap;
    
    /** Creates a new instance of WizardModel */
    public WizardModel() {
        panelHashmap = new HashMap();
    }
    
    /**
     * Registers a new panel to the wizard
     * @param obj identifier which specifies the panel
     * @param wizardpaneldescriptor  the descriptor which belongs to the panel
     */
    public void registerPanel(Object obj, WizardPanelDescriptor wizardpaneldescriptor) {
        panelHashmap.put(obj, wizardpaneldescriptor);
    }
    
    
    /**
     * Sets the current wizard panel
     * @param obj object identifier which specifies the panel and the descriptor
     */
    public void setCurrentPanel(Object obj){
        WizardPanelDescriptor newWizardPanelDescriptor = (WizardPanelDescriptor)panelHashmap.get(obj);
        if(newWizardPanelDescriptor == null) {
            throw new WizardPanelNotFoundException();
        }
        currentPanel = newWizardPanelDescriptor;
    }
    
    /**
     * Returns the actual panel which is displayed
     * @return displayed panel
     */
    public WizardPanelDescriptor getCurrentPanelDescriptor() {
        return currentPanel;
    }
    

    /**
     * Returns all panel descriptors which are available
     * @return all registered panels
     */
    public WizardPanelDescriptor[] getAllPanelDescriptors(){
        Set set= panelHashmap.keySet();
        Iterator iter = set.iterator();
        WizardPanelDescriptor[] panels = new WizardPanelDescriptor[set.size()];
        int i=0;
        while (iter.hasNext()){
            panels[i] = (WizardPanelDescriptor)panelHashmap.get(iter.next());
            i++;
        }
        return panels;
    }
}
