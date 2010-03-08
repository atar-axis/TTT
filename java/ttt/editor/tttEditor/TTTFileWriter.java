package ttt.editor.tttEditor;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;



/**
 * Class providing helper methods when writing TTT files without 
 * additional processing.
 */
public class TTTFileWriter {
    
    /**
     * Creates a new TTT file from given data.
     * @param fileData the data to be written to the new file
     * @param file the output file
     * @throws java.io.IOException 
     */
    public static void writeFile(TTTFileData fileData, File file) throws IOException {
        OutputStream out_raw = new FileOutputStream(file);
        DataOutputStream out = new DataOutputStream(out_raw);
        
        //write header
        fileData.header.writeVersionMessageToOutputStream(out);
        out = new DataOutputStream(new DeflaterOutputStream(out_raw));
        fileData.header.writeServerInitToOutputStream(out);
        
        //write extensions
        //check if user selected option
        if (Parameters.saveIndex)
            fileData.index.writeIndexExtension(out, Parameters.saveThumbs);
        if (fileData.extensions != null)
            for (int i = 0; i < fileData.extensions.size(); i++) {
                byte[] extension = (byte[])fileData.extensions.get(i);
                switch(extension[0]) {
                    //skip index (has been written anyway)
                    case ProtocolConstants.EXTENSION_INDEX_TABLE:
                        break;
                    //write any other extension
                    default:
                        out.writeInt(extension.length);
                        out.write(extension);
                        break;
                }
            }
        out.writeInt(0);
        
        //write start time
        out.writeLong(fileData.header.startTime);
        
        //write messages
        fileData.index.writeAllMessages(out);
        
        out.close();
    }
    
    
    /**
     * Ensures file has correct suffix.  If older suffix present, this is
     * removed - otherwise the correct suffix is simply appended to file name.
     * If the suffix already valid, the file is simply returned unchanged.
     * @param file the original file
     * @return a file with the default TTT ending.  This may be the same
     * as the original file.
     */
    public static File forceDesktopFileToDefaultEnding(File file) {
        if (!file.getAbsolutePath().endsWith(Parameters.desktopEndings[0])) {
            String name = file.getAbsolutePath();
            //if file has alternative legal suffix (i.e. it's an old desktop file)
            //remove this before adding desired suffix
            for (int i = 1; i < Parameters.desktopEndings.length; i++) {
                if (name.endsWith(Parameters.desktopEndings[i])) {
                    name = name.substring(0, name.length() - Parameters.desktopEndings[i].length());
                }
            }
            String newName = name + Parameters.desktopEndings[0];
            return new File(newName);
        }
        return file;
    }
    
    
}
