package ttt.editor2;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/*
 * FileSearch.java
 *
 * Created on 25 August 2005, 12:14
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 * A collection of helper methods for use when searching for, opening and saving files.
 */
public class TTTFileUtilities {

    /**
     * Indicates a file with a valid desktop ending.
     */
    public static final int DESKTOP = 1;
    /**
     * Indicates a file with a valid audio ending.
     */
    public static final int AUDIO = 2;
    /**
     * Indicates a file with a valid video ending.
     */
    public static final int VIDEO = 3;

    
    /**
     * Removes the suffix of a passed file, returning its absolute path with the ending removed.
     * 
     * @param file
     *            the <code>File</code> to obtain the name of
     * @return the basic file name
     */
    public static String getBasicFileName(File file) {
        String basicFileName = file.getAbsolutePath();
        int suffixIndex = basicFileName.lastIndexOf('.');
        basicFileName = basicFileName.substring(0, suffixIndex);
        return basicFileName;
    }

    /**
     * Returns the suffix of a passed file.
     * 
     * @param file
     *            the <code>File</code> to obtain the suffix of
     * @return the suffix
     */
    private static String getFileSuffix(File file) {
        String name = file.getAbsolutePath();
        int suffixIndex = name.lastIndexOf('.');
        String suffix = name.substring(suffixIndex);
        return suffix;
    }

    /**
     * Given a file with a desktop suffix, checks whether a file with the same name but an appropriate audio suffix is
     * found.
     * 
     * @param desktopFile
     *            the desktop file
     * @return the <code>File</code> which has been found, or <code>null</code> if no such file is found
     */
    public static File checkForAudio(File desktopFile) {
        return checkForAudio(getBasicFileName(desktopFile));
    }

    // checks for file with appropriate audio suffix, returns it if found
    /**
     * Given a basic file name (without a suffix), checks whether a file with the same name but an appropriate audio
     * suffix is found.
     * 
     * @param basicFileName
     *            the file name without suffix
     * @return the <code>File</code> which has been found, or <code>null</code> if no such file is found
     */
    public static File checkForAudio(String basicFileName) {
        for (int count = 0; count < Parameters.audioEndings.length; count++) {
            File file = new File(basicFileName + Parameters.audioEndings[count]);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Given a file with a desktop suffix, checks whether a file with the same name but an appropriate video suffix is
     * found.
     * 
     * @param desktopFile
     *            the desktop file
     * @return the <code>File</code> which has been found, or <code>null</code> if no such file is found
     */
    public static File checkForVideo(File desktopFile) {
        return checkForVideo(getBasicFileName(desktopFile));
    }

    // checks for file with appropriate video suffix, returns it if found
    /**
     * Given a basic file name (without a suffix), checks whether a file with the same name but an appropriate video
     * suffix is found.
     * 
     * @param basicFileName
     *            the file name without suffix
     * @return the <code>File</code> which has been found, or <code>null</code> if no such file is found
     */
    public static File checkForVideo(String basicFileName) {
        for (int count = 0; count < Parameters.videoEndings.length; count++) {
            File file = new File(basicFileName + Parameters.videoEndings[count]);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Renames a file for backup by adding a '.bak' suffix to it, followed by a number if a file of that name already
     * exists. The numbers used increment until a file without that name exists.
     * 
     * @param file
     *            the file to be renamed
     * @return the renamed file if successful, otherwise the original file
     */
    public static File renameForBackup(File file) {
        File renameFile = new File(file.getAbsolutePath() + ".bak");
        int i = 1;
        while (renameFile.exists())
            renameFile = new File(file.getAbsolutePath() + ".bak." + i++);
        file.renameTo(renameFile);
        // return the renamed file if it exists, otherwise return the original
        if (renameFile.exists())
            return renameFile;
        else
            return file;
    }

    /**
     * Renames a file for backup as in the renameForBackup method, but appends the original suffix to the end so that
     * the file type may still be recognized.
     * 
     * @param file
     *            the file to be renamed
     * @return the renamed file if successful, otherwise the original file
     */
    public static File renameForBackupProcessing(File file) {
        String suffix = TTTFileUtilities.getFileSuffix(file);
        File renameFile = new File(file.getAbsolutePath() + ".bak");
        int i = 1;
        while (renameFile.exists())
            renameFile = new File(file.getAbsolutePath() + ".bak." + i++);

        // appends the original suffix
        renameFile = new File(renameFile.getAbsolutePath() + suffix);
        file.renameTo(renameFile);
        // return the renamed file if it exists, otherwise return the original
        if (renameFile.exists())
            return renameFile;
        else
            return file;
    }

    // JFileChooser for opening new files, and filter so that only valid files are displayed
  private  static JFileChooser fileChooser;

    // used because creating a JFileChooser can take some time -
    // this allows the file chooser to be created in advance
    /**
     * Create the file chooser for later use. This is offered because creating a file chooser can take some time, and it
     * is useful to create it in advance of actually requiring it.
     */
    private static void createFileChooser() {
        // added 19.01.2006 by Peter Ziewer
        String lastUsedPath = Editor2.userPrefs.get("last used path", null);

        fileChooser = lastUsedPath != null ? new JFileChooser(lastUsedPath) : new JFileChooser();
        fileChooser.setFileFilter(new TTTDefaultFileFilter());
        fileChooser.setAcceptAllFileFilterUsed(false);        
    }

    /**
     * Ask the user to select a file to open
     * 
     * @return the <code>File</code> which the user has selected to open
     */
    public static File showOpenFileInternalDialog() {
        return showOpenFileInternalDialog(false);
    }

    /**
     * Ask the user to select a file to open
     * 
     * @param allowOlderDesktopFiles
     *            <code>false</code> if only the most recent TTT desktop files should be offered, <code>true</code>
     *            if all should be included
     * @return the <code>File</code> which the user has selected to open
     */
    public static File showOpenFileInternalDialog(boolean allowOlderDesktopFiles) {
        if (fileChooser == null)
            createFileChooser();
        // calibrate the file chooser
        FileFilter currentFilter = fileChooser.getFileFilter();
        if (allowOlderDesktopFiles) {
            if (!(currentFilter instanceof TTTFileFilter)) {
                fileChooser.resetChoosableFileFilters();
                fileChooser.setFileFilter(new TTTFileFilter());
                fileChooser.setAcceptAllFileFilterUsed(false);
            }
        } else {
            if (!(currentFilter instanceof TTTDefaultFileFilter)) {
                fileChooser.resetChoosableFileFilters();
                fileChooser.setFileFilter(new TTTDefaultFileFilter());
                fileChooser.setAcceptAllFileFilterUsed(false);
            }
        }

        int returnValue = fileChooser.showOpenDialog(Editor2.getInstance());

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            // added 22.03.2006 by Peter Ziewer
            // use same path after restart
            Editor2.userPrefs.put("last used path", fileChooser.getSelectedFile().getAbsoluteFile().getParent());

            return fileChooser.getSelectedFile();
        }
        else
            return null;
    }

    /**
     * Ask the user to select a file to used for saving
     * 
     * @return the <code>File</code> which the user has selected to use for saving
     */
    public static File showSaveFileInternalDialog() {
        return showSaveFileInternalDialog(null);
    }

    /**
     * Ask the user to select a file to used for saving
     * 
     * @param suggestedFile
     *            the file which the file chooser should initially select
     * @return the <code>File</code> which the user has selected to use for saving
     */
    public static File showSaveFileInternalDialog(File suggestedFile) {
        if (fileChooser == null)
            createFileChooser();

        // calibrate the file chooser
        FileFilter currentFilter = fileChooser.getFileFilter();
        if (!(currentFilter instanceof TTTDefaultFileFilter)) {
            fileChooser.resetChoosableFileFilters();
            fileChooser.setFileFilter(new TTTDefaultFileFilter());
            fileChooser.setAcceptAllFileFilterUsed(false);
        }

        if (suggestedFile != null)
            fileChooser.setSelectedFile(suggestedFile);

        int returnValue = fileChooser.showSaveDialog(Editor2.getInstance());
        if (returnValue == JFileChooser.APPROVE_OPTION)
            return fileChooser.getSelectedFile();

        return null;
    }

    /**
     * FileFilter used for displaying all valid TTT files in a JFileChooser.
     * <p>
     * Endings are not explicitly stored here, but rather generated at runtime from a list of valid endings in the
     * <code>Parameters</code> class, which should aid in maintaining the list.
     */
    static final class TTTFileFilter extends FileFilter {

        private String[] validEndings;
        private String validEndingsString = "";

        /**
         * Class Constructor. Creates an array of valid file endings using those defined in the Parameters class.
         */
        public TTTFileFilter() {
            validEndings = Parameters.desktopEndings;
            for (int count = 0; count < validEndings.length; count++) {
                if (count > 0)
                    validEndingsString += ", " + validEndings[count];
                else
                    validEndingsString = validEndings[count];
            }
        }

        /**
         * @return <code>true</code> if the file selected has a valid suffix.
         */
        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            String name = f.getName().toLowerCase();
            for (int count = 0; count < validEndings.length; count++) {
                if (name.endsWith(validEndings[count]))
                    return true;
            }
            return false;
        }

        /**
         * Provides a String containing a formatted list of all valid file suffixes which can be handled by the
         * TTTEditor.
         * 
         * @return a String including the valid file endings.
         */
        public String getDescription() {
            return ("Valid files (" + validEndingsString + ")");
        }
    }

    /**
     * FileFilter used to display only files with the preferred TTT ending in a JFileChooser.
     * <p>
     * The ending is obtained from the <code>Parameters.desktopEndings</code> array. The first element in this array
     * is the preferred ending.
     */
    static final class TTTDefaultFileFilter extends FileFilter {

        /**
         * @return <code>true</code> if the file selected has a valid suffix.
         */
        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            String name = f.getName().toLowerCase();
            if (name.endsWith(Parameters.desktopEndings[0]))
                return true;
            return false;
        }

        /**
         * Provides a String containing the ending of the default file.
         * 
         * @return a String including the valid file endings.
         */
        public String getDescription() {
            return ("TTT files (" + Parameters.desktopEndings[0] + ")");
        }
    }
}