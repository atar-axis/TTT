/* LineInVolumePeak.java
 * Created on 11. Mai 2007, 11:48
 * @author Christian Gruber Bakk.techn.
 */

package ttt.gui.wizard.recorder;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * Gets the volume peak of a specified targetline and displays it on a VolumeLevelComponent
 */
public class LineInVolumePeak extends Thread{
    
    private TargetDataLine m_targetDataLine;
    private float m_MeanSampleValue = 0;
    private VolumeLevelComponent m_VolumeLevelComponent;
    private byte[] buffer;
    
    /**
     * Starts reading volume from the targetline
     */
    public void run(){
        while (true) {
            if (isInterrupted()) {
                break;
            }
            
             m_targetDataLine.read(buffer,0,buffer.length);
                int offset = 0;
                while(offset < buffer.length) { 
                    // caculates the frame samples from the given byte array
                    // http://www.jsresources.org/faq_audio.html#reconstruct_samples
                    float sample = (  (buffer[offset + 0] & 0xFF) | (buffer[offset + 1] << 8) ) / 32768.0F;
                    if(sample < 0) sample *= (-1);
                    m_MeanSampleValue += sample;
                    offset += 2;
                }
                m_MeanSampleValue /= (buffer.length/2);
                
                //TODO fire event instead of this
                m_VolumeLevelComponent.setPeakPercentage(m_MeanSampleValue*2);
        }
    }
    
    /**
     * @param targetDataLine the line from which the volume peak is taken
     * @param volumeLevelComponent the component which visualise the volume peak
     * @throws javax.sound.sampled.LineUnavailableException if the targetline cannot be found
     */
    public LineInVolumePeak(TargetDataLine targetDataLine, VolumeLevelComponent volumeLevelComponent) 
        throws LineUnavailableException {
        
        m_targetDataLine = targetDataLine;
        m_VolumeLevelComponent = volumeLevelComponent;
        
        try {
            
            m_targetDataLine.open();
        } catch (LineUnavailableException ex) {
           throw ex;
        }
        
        m_targetDataLine.start();
        buffer = new byte[m_targetDataLine.getBufferSize()/5];   
    }
}
