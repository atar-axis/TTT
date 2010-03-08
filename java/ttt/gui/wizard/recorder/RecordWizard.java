/* RecordWizard.java
 * Created on 24. April 2007, 13:54
 * @author Christian Gruber Bakk.techn.
 */

package ttt.gui.wizard.recorder;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ttt.gui.wizard.framework.WizardController;

/**
 * Start point of the wizard
 */
public class RecordWizard {
    
    private static final String ENGLISH = "EN";
    private static final String GERMAN = "DE";
    private static String language;
    
    public RecordWizard() {
    }
    
    /**
     * Sets look and feel. Creates and registers all panels
     * @param args input parameters
     */
    public static void main(String[] args) {
        //javax.swing.SwingUtilities.invokeLater(new Runnable() {
        //  public void run() {
        //set system look and feel
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
            // handle exception
        } catch (ClassNotFoundException e) {
            // handle exception
        } catch (InstantiationException e) {
            // handle exception
        } catch (IllegalAccessException e) {
            // handle exception
        }
        
                
        //default language is set to english
        language = ENGLISH;
        String arg;
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];
            
            for (int j = 1; j < arg.length(); j++) {
                char flag = arg.charAt(j);
                switch (flag) {
                    case 'd':
                        language = GERMAN;
                        break;
                    case 'e':
                        language = ENGLISH;
                        break;
                    default:
                        System.out.println(
                                "Usage: java RunCommand TTT -option \n option: " +
                                "-player -d | -player -e | -recorder -d |-recorder -e | -converter"
                                );
                        System.exit(-1);
                }
            }
        }
        
        
        WizardController controller = new WizardController(language);
        
        AudioPanelDescriptor aupd = new AudioPanelDescriptor(language);
        controller.registerWizardPanel("AUDIOPANEL",aupd);
        
        OptionPanelDescriptor opd = new OptionPanelDescriptor(language);
        controller.registerWizardPanel("OPTIONPANEL",opd);
        
        VideoPanelDescriptor vpd = new VideoPanelDescriptor(language);
        controller.registerWizardPanel("VIDEOPANEL",vpd);
        
        InfoPanelDescriptor ipd = new InfoPanelDescriptor(language);
        controller.registerWizardPanel("INFOPANEL",ipd);
        
        AdvicePanelDescriptor apd = new AdvicePanelDescriptor(language);
        controller.registerWizardPanel("ADVICEPANEL", apd);
        
        controller.setCurrentPanel("AUDIOPANEL");
        controller.startWizard();
        
    }
    //});
}


