package ttt.editor.tttEditor;
import java.util.ArrayList;


/**
 * Simple data structure which contains all of the data within a TTT file.
 */
public class TTTFileData {
    
    /**
     * The <code>Header</code> object, which contains information relating to the
     * recording of the file (e.g. start time, server message), and also 
     * information which is needed for processing <code>Message</code>s (e.g.)
     * bits-per-pixel).
     */
    protected Header header = null;
    /**
     * The <code>Index</code>, which also contains all the <code>Message</code>s
     * of the file.
     */
    protected Index index = null;
    /**
     * Any extensions which are present, including the index extension if it has been saved.
     */
    protected ArrayList<byte[]> extensions = null;
    
}
