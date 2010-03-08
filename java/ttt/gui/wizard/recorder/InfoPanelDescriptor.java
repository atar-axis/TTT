/* InfoPanelDescriptor.java
 * Created on 24. Mai 2007, 13:33
 * @author Christian Gruber Bakk.techn.
 */

package ttt.gui.wizard.recorder;

import ttt.gui.wizard.framework.WizardController;
import ttt.gui.wizard.framework.WizardPanelDescriptor;

public class InfoPanelDescriptor extends WizardPanelDescriptor {
    
    private InfoPanel m_InfoPanel;
    
    private String m_ResolutionWarning_EN = "During recording do not change your screen resolution! This might " +
            "corrupt your current records!";
    
    private String m_ResolutionWarning_DE = "Die Bildschirmauflösung darf während der Aufzeichnung nicht " +
            "geändert werden. Wenn doch, kann die Aufnahme unter Umständen nicht " +
            "mehr wiedergegeben werden.";
    
    private String m_DeviceWarning_EN = "If you use an external device " +
            "like a beamer plug in NOW and then start recording! So you avoid " +
            "switching the resolution by your system during recording.";
    
    private String m_DeviceWarning_DE = "Wenn externe Ausgabegeräte wie Beamer verwendet werden, sollten Sie " +
            "diese JETZT anschließen. Dies verhindert eine automatische Änderung der Bildschirmauflösung " +
            "während der Aufnahme.";
    
    /** Creates a new instance of InfoPanelDescriptor */
    public InfoPanelDescriptor(String language) {
        
        m_FinishEnable = false;
        m_InfoPanel = new InfoPanel();
        this.setObjectIdentifier("INFOPANEL");
        this.setComponent(m_InfoPanel);
        
        if(language == "DE"){
            m_InfoPanel.ResolutionWarningTextArea.setText(m_ResolutionWarning_DE);
            m_InfoPanel.DeviceWarningTextArea.setText(m_DeviceWarning_DE);
        } else{
            m_InfoPanel.ResolutionWarningTextArea.setText(m_ResolutionWarning_EN);
            m_InfoPanel.DeviceWarningTextArea.setText(m_DeviceWarning_EN);
        }
    }
    
    public Object getLastComponentDescriptor(){
        return "OPTIONPANEL";
    }
    
    public Object getNextComponentDescriptor() {
        if (!m_FinishEnable)
            return "ADVICEPANEL";
        else
            return FINISH;
    }
    
    public void beforeDisplayingPanel() {
        WizardController controller = getWizardController();
        if (controller.getProfile().isShowRecordControlsInfo())
            m_FinishEnable = false;
        else
            m_FinishEnable = true;
        this.restoreButtonsToPanelState();
    }
    
    public void displayingPanel() {
        
    }
    
}
