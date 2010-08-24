package ttt.editor2;

import java.awt.Color;
import javax.media.Manager;

/**
 * Parameters used by the TTTEditor, some of which may be set by the user.
 */
public final class Parameters {

    /**
     * The version message which will be used when converting an older TTT file type to a current file type
     */
    protected static final String defaultVersionMessage = "TTT 001.000\n";

    /**
     * Whether backups should be created when saving files
     */
    protected static boolean createBackups = true;

    /**
     * String text to be used in a zoom box - options should correspond to possible zoom int values
     */
    protected static final String[] zoomOptions = { "Size to fit", "25%", "50%", "75%", "100%" };

    static final int ZOOM_TO_FIT = 0, ZOOM_25 = 1, ZOOM_50 = 2, ZOOM_75 = 3, ZOOM_100 = 4;

    /**
     * Formatted text containing information about the software to be displayed in an "About" box
     */
    protected static final String ABOUT =
    // "The TTTEditor is designed for use with the\n"
    // + "TeleTeachingTool, and created by Pete Bankhead as part of a\n"
    // + "student project at the TUM.\n"
    // + "--------------------------------------------\n"+
    "   TeleTeachingTool Editor   -   Version " + editor2.version + "\n\n"+
    "   Author:  \tPete Bankhead\n\n"

    + "   Author:  \tPeter Ziewer\n" + "                Technical University of Munich\n" + "                Germany\n"
            + "   Email:    \tziewer@in.tum.de\n" + "   Web:      \thttp://TTT.Uni-Trier.de\n\n" + "   Java Version "
            + System.getProperty("java.version") + " (" + System.getProperty("java.vm.version") + ")\n"
            + "   Java Vendor: " + System.getProperty("java.vendor") + "\n" + "   JMF Version  " + getJMFVersion()
            + "\n" + "   Operating System: " + System.getProperty("os.name") + " (" + System.getProperty("os.version");
    //      + ")\n\n" + "This software may be redistributed under the terms\nof the General Public License.";

    private static String getJMFVersion() {
        try {
            return Manager.getVersion();
        } catch (NoClassDefFoundError e) {
            return "NOT INSTALLED";
        }
    }

    // variables which user can change - used for all open files
    /**
     * Whether synchronization should be used or not during playback, and also for file writing.
     */
    protected static boolean synchronize = false;
    /**
     * Whether thumbnails should be included in any index extensions which are written
     */
    protected static boolean saveThumbs = true;
    /**
     * Whether index extensions should be saved with files
     */
    protected static boolean saveIndex = true;
    /**
     * Whether index division points should be displayed on the timeline.
     */
    protected static boolean displayIndexesOnTimeline = false;
    /**
     * Whether markers should be displayed on the timeline.
     */
    protected static boolean displayMarkersOnTimeline = true;
    /**
     * Whether ticks should be displayed on the timeline.
     */
    protected static boolean displayTicksOnTimeline = true;

    /**
     * Valid desktop file suffixes (e.g. ".ttt"). The preferred suffix should always be the first. Also, preferred
     * endings should be at the beginning, since when searching for a file the first acceptable suffix will be taken.
     */
    public static final String[] desktopEndings = { ".ttt", ".vnc" };
    /**
     * Valid audio file suffixes (e.g. ".mp3"). The preferred suffix should always be the first.
     */
    public static final String[] audioEndings = { ".mp3", ".wav" };
    /**
     * Valid audio file suffixes (e.g. ".mov"). The preferred suffix should always be the first.
     */
    public static final String[] videoEndings = { ".mov",  ".avi"};

    public static final String[] unprocessedVideoEndings = {".bjpg"};
    // public static final String [] audioType = {FileTypeDescriptor.MPEG_AUDIO};
    // public static final String [] videoType = {FileTypeDescriptor.QUICKTIME};

    /**
     * Scale factor when creating thumbnails. The preferred width and height will be divided by this to get the
     * thumbnail width and height.
     */
    public static int thumbnail_scale_factor = 6;

    /**
     * Layer used by a <code>JDesktopPane</code> for <code>DesktopViewer</code>s.
     */
    public static final int DESKTOP_LAYER = 1;
    /**
     * Layer used by a <code>JDesktopPane</code> for <code>VideoViewer</code>s.
     */
    public static final int VIDEO_LAYER = 3;

    /**
     * Pen size used when painting annotations.
     */
    public static float penSize = 2.5f;

    /*
     * public final static int White = 0; public final static int DarkGray = 4; public final static int Gray = 8; public
     * final static int LightGray = 12; public final static int Black = 16; public final static int Orange = 20; public
     * final static int Pink = 24; public final static int Blue = 28; public final static int Red = 32; public final
     * static int Green = 36; public final static int Magenta = 40; public final static int Yellow = 44; public final
     * static int Cyan = 48;
     */

    /**
     * Colours, as used to paint annotations.
     */
    public static Color[] colors = { new Color(255, 255, 255, 255), // white
            new Color(255, 255, 255, 192), new Color(255, 255, 255, 128), new Color(255, 255, 255, 64),
            new Color(64, 64, 64, 255),
            // darkGray
            new Color(64, 64, 64, 192), new Color(64, 64, 64, 128), new Color(64, 64, 64, 64),
            new Color(128, 128, 128, 255),
            // gray
            new Color(128, 128, 128, 192), new Color(128, 128, 128, 128), new Color(128, 128, 128, 64),
            new Color(192, 192, 192, 255),
            // lightGray
            new Color(192, 192, 192, 192), new Color(192, 192, 192, 128), new Color(192, 192, 192, 64),
            new Color(0, 0, 0, 255),
            // black
            new Color(0, 0, 0, 192), new Color(0, 0, 0, 128), new Color(0, 0, 0, 64), new Color(255, 200, 0, 255),
            // orange
            new Color(255, 200, 0, 192), new Color(255, 200, 0, 128), new Color(255, 200, 0, 64),
            new Color(255, 175, 175, 255),
            // pink
            new Color(255, 175, 175, 192), new Color(255, 175, 175, 128), new Color(255, 175, 175, 64),
            new Color(0, 0, 255, 255),
            // blue
            new Color(0, 0, 255, 192), new Color(0, 0, 255, 128), new Color(0, 0, 255, 64), new Color(255, 0, 0, 255),
            // red
            new Color(255, 0, 0, 192), new Color(255, 0, 0, 128), new Color(255, 0, 0, 64), new Color(0, 255, 0, 255),
            // green
            new Color(0, 255, 0, 192), new Color(0, 255, 0, 128), new Color(0, 255, 0, 64),
            new Color(255, 0, 255, 255),
            // magenta
            new Color(255, 0, 255, 192), new Color(255, 0, 255, 128), new Color(255, 0, 255, 64),
            new Color(255, 255, 0, 255),
            // yellow
            new Color(255, 255, 0, 192), new Color(255, 255, 0, 128), new Color(255, 255, 0, 64),
            new Color(0, 255, 255, 255),
            // cyan
            new Color(0, 255, 255, 192), new Color(0, 255, 255, 128), new Color(0, 255, 255, 64),
            new Color(0, 0, 153, 255),
            // dark blue
            new Color(0, 0, 153, 192), new Color(0, 0, 153, 128), new Color(0, 0, 153, 64),
            new Color(102, 102, 255, 255),
            // light blue
            new Color(102, 102, 255, 192), new Color(102, 102, 255, 128), new Color(102, 102, 255, 64),
            new Color(204, 204, 255, 255),
            // very light blue
            new Color(204, 204, 255, 192), new Color(204, 204, 255, 128), new Color(204, 204, 255, 64),
            new Color(255, 102, 102, 255),
            // light red
            new Color(255, 102, 102, 192), new Color(255, 102, 102, 128), new Color(255, 102, 102, 64),
            new Color(255, 204, 204, 255),
            // very light red
            new Color(255, 204, 204, 192), new Color(255, 204, 204, 128), new Color(255, 204, 204, 64),
            new Color(0, 102, 0, 255),
            // dark green
            new Color(0, 102, 0, 192), new Color(0, 102, 0, 128), new Color(0, 102, 0, 64),
            new Color(102, 255, 102, 255),
            // light green
            new Color(102, 255, 102, 192), new Color(102, 255, 102, 128), new Color(102, 255, 102, 64),
            new Color(204, 255, 204, 255),
            // very light green
            new Color(204, 255, 204, 192), new Color(204, 255, 204, 128), new Color(204, 255, 204, 64),
            new Color(102, 0, 102, 255),
            // dark rose
            new Color(102, 0, 102, 192), new Color(102, 0, 102, 128), new Color(102, 0, 102, 64),
            new Color(255, 0, 255, 255),
            // rose
            new Color(255, 0, 255, 192), new Color(255, 0, 255, 128), new Color(255, 0, 255, 64),
            new Color(255, 102, 255, 255),
            // light rose
            new Color(255, 102, 255, 192), new Color(255, 102, 255, 128), new Color(255, 102, 255, 64),
            new Color(255, 204, 255, 255),
            // very light rose
            new Color(255, 204, 255, 192), new Color(255, 204, 255, 128), new Color(255, 204, 255, 64),
            new Color(102, 102, 0, 255),
            // dark yellow
            new Color(102, 102, 0, 192), new Color(102, 102, 0, 128), new Color(102, 102, 0, 64),
            new Color(255, 255, 102, 255),
            // light yellow
            new Color(255, 255, 102, 192), new Color(255, 255, 102, 128), new Color(255, 255, 102, 64),
            new Color(255, 255, 204, 255),
            // very light yellow
            new Color(255, 255, 204, 192), new Color(255, 255, 204, 128), new Color(255, 255, 204, 64),
            new Color(0, 0, 102, 255),
            // dark turquoise
            new Color(0, 0, 102, 192), new Color(0, 0, 102, 128), new Color(0, 0, 102, 64),
            new Color(102, 255, 255, 255),
            // light turquoise
            new Color(102, 255, 255, 192), new Color(102, 255, 255, 128), new Color(102, 255, 255, 64),
            new Color(204, 255, 255, 255),
            // very light turquoise
            new Color(204, 255, 255, 192), new Color(204, 255, 255, 128), new Color(204, 255, 255, 64),
            new Color(153, 0, 255, 255),
            // violet
            new Color(153, 0, 255, 192), new Color(153, 0, 255, 128), new Color(153, 0, 255, 64),
            new Color(102, 0, 153, 255),
            // dark violet
            new Color(102, 0, 153, 192), new Color(102, 0, 153, 128), new Color(102, 0, 153, 64),
            new Color(153, 102, 255, 255),
            // blueish light violet
            new Color(153, 102, 255, 192), new Color(153, 102, 255, 128), new Color(153, 102, 255, 64),
            new Color(204, 102, 255, 255),
            // redish light violet
            new Color(204, 102, 255, 192), new Color(204, 102, 255, 128), new Color(204, 102, 255, 64),
            new Color(204, 102, 0, 255),
            // light brown
            new Color(204, 102, 0, 192), new Color(204, 102, 0, 128), new Color(204, 102, 0, 64),
            new Color(255, 102, 51, 255),
            // dark orange
            new Color(255, 102, 51, 192), new Color(255, 102, 51, 128), new Color(255, 102, 51, 64),
            new Color(255, 204, 153, 255),
            // light orange
            new Color(255, 204, 153, 192), new Color(255, 204, 153, 128), new Color(255, 204, 153, 64),
            new Color(255, 215, 0, 255),
            // gold
            new Color(255, 215, 0, 192), new Color(255, 215, 0, 128), new Color(255, 215, 0, 64),
            new Color(240, 230, 140, 255),
            // khaki
            new Color(240, 230, 140, 192), new Color(240, 230, 140, 128), new Color(240, 230, 140, 64),
            new Color(218, 165, 32, 255),
            // goldenrod
            new Color(218, 165, 32, 192), new Color(218, 165, 32, 128), new Color(218, 165, 32, 64),
            new Color(245, 245, 220, 255),
            // beige
            new Color(245, 245, 220, 192), new Color(245, 245, 220, 128), new Color(245, 245, 220, 64),
            new Color(255, 228, 181, 255),
            // moccasin
            new Color(255, 228, 181, 192), new Color(255, 228, 181, 128), new Color(255, 228, 181, 64),
            new Color(255, 99, 71, 255),
            // tomato
            new Color(255, 99, 71, 192), new Color(255, 99, 71, 128), new Color(255, 99, 71, 64),
            new Color(255, 140, 0, 255),
            // darkorange
            new Color(255, 140, 0, 192), new Color(255, 140, 0, 128), new Color(255, 140, 0, 64),
            new Color(220, 20, 60, 255),
            // crimson
            new Color(220, 20, 60, 192), new Color(220, 20, 60, 128), new Color(220, 20, 60, 64),
            new Color(70, 130, 180, 255),
            // steelblue
            new Color(70, 130, 180, 192), new Color(70, 130, 180, 128), new Color(70, 130, 180, 64),
            new Color(65, 105, 225, 255),
            // royalblue
            new Color(65, 105, 225, 192), new Color(65, 105, 225, 128), new Color(65, 105, 225, 64),
            new Color(123, 104, 238, 255),
            // medslateblue
            new Color(123, 104, 238, 192), new Color(123, 104, 238, 128), new Color(123, 104, 238, 64),
            new Color(127, 255, 212, 255),
            // aquamarine
            new Color(127, 255, 212, 192), new Color(127, 255, 212, 128), new Color(127, 255, 212, 64),
            new Color(0, 255, 127, 255),
            // springgreen
            new Color(0, 255, 127, 192), new Color(0, 255, 127, 128), new Color(0, 255, 127, 64),
            new Color(150, 205, 50, 255),
            // yellowgreen
            new Color(150, 205, 50, 192), new Color(150, 205, 50, 128), new Color(150, 205, 50, 64),
            new Color(216, 191, 216, 255),
            // thistle
            new Color(216, 191, 216, 192), new Color(216, 191, 216, 128), new Color(216, 191, 216, 64),
            new Color(245, 222, 179, 255),
            // wheat
            new Color(245, 222, 179, 192), new Color(245, 222, 179, 128), new Color(245, 222, 179, 64),
            new Color(160, 82, 45, 255),
            // siena
            new Color(160, 82, 45, 192), new Color(160, 82, 45, 128), new Color(160, 82, 45, 64),
            new Color(233, 150, 122, 255),
            // darksalmon
            new Color(233, 150, 122, 192), new Color(233, 150, 122, 128), new Color(233, 150, 122, 64),
            new Color(165, 42, 42, 255),
            // brown
            new Color(165, 42, 42, 192), new Color(165, 42, 42, 128), new Color(165, 42, 42, 64),
            new Color(210, 105, 30, 255),
            // chocolate
            new Color(210, 105, 30, 192), new Color(210, 105, 30, 128), new Color(210, 105, 30, 64),
            new Color(244, 164, 96, 255),
            // sandybrown
            new Color(244, 164, 96, 192), new Color(244, 164, 96, 128), new Color(244, 164, 96, 64),
            new Color(255, 20, 147, 255),
            // deeppink
            new Color(255, 20, 147, 192), new Color(255, 20, 147, 128), new Color(255, 20, 147, 64),
            new Color(255, 105, 180, 255),
            // hotpink
            new Color(255, 105, 180, 192), new Color(255, 105, 180, 128), new Color(255, 105, 180, 64),
            new Color(221, 160, 221, 255),
            // plum
            new Color(221, 160, 221, 192), new Color(221, 160, 221, 128), new Color(221, 160, 221, 64),
            new Color(186, 85, 211, 255),
            // medorchid
            new Color(186, 85, 211, 192), new Color(186, 85, 211, 128), new Color(186, 85, 211, 64),
            new Color(112, 128, 144, 255),
            // slategray
            new Color(112, 128, 144, 192), new Color(112, 128, 144, 128), new Color(112, 128, 144, 64) };

}
