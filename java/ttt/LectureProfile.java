// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universit�t M�nchen
// 
//    This file is part of TeleTeachingTool.
//
//    TeleTeachingTool is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    TeleTeachingTool is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with TeleTeachingTool.  If not, see <http://www.gnu.org/licenses/>.

/*
 * Created on 09.05.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.Preferences;

// the lecture profile contains information about the lecture and the recording process
public class LectureProfile {
    // the primary key for the database is <lecture>
    // TODO: maybe <lecture + teacher> would be more suitable for many users
    private String lecture = "";
    private String teacher = "";
    private String host = "localhost";
    private int port = 5900;
    private boolean record = true;
    private boolean recordVideo = true;
    private boolean recordLinearAudio = true; // linear = wav
    private boolean loopbackRecorder = false;
    private int colorDepth = Constants.defaultColorDepth;
    private String RecordingCamera;
    private ttt.videoRecorder.TTTVideoFormat Format;
    private float VideoQuality;
    
    // used whether to show info box or not
    // stored in profile because other profile might be used by other person
    //
    // show record info for loopback recorder
    private boolean showRecordControlsInfo = true;
    // show warning that record may fail after play on some systems
    private boolean showRecordPlayRecordWarning = true;

    static private Preferences profileRoot = TTT.userPrefs.node("profiles");

    public LectureProfile(String lecture) {
        this.lecture = lecture;
    }

    // tests if a profile with given name exists
    static public boolean profileExists(String lecture) {
        try {
            if (lecture.length() > 0)
                return profileRoot.nodeExists(lecture);
        } catch (Exception e) {
            System.out.println("failed to read Lecture Profile: " + e);
        }
        return false;
    }

    // get profile or create new if not existent
    // NOTE: default values if lecture.equals("")
    static public LectureProfile getProfile(String lecture) {
        if (lecture == null)
            lecture = "";

        try {
            // get profile values
            if (profileRoot.nodeExists(lecture)) {
                // node
                Preferences preferences = profileRoot.node(lecture);

                // new profile
                LectureProfile profile = new LectureProfile(lecture);

                // load values
                profile.teacher = preferences.get("teacher", "");
                profile.host = preferences.get("host", "localhost");
                profile.port = preferences.getInt("port", 5900);
                profile.colorDepth = preferences.getInt("color_depth", Constants.defaultColorDepth);
                profile.record = preferences.getBoolean("record", true);
                profile.recordVideo = preferences.getBoolean("record_video", true);
                profile.recordLinearAudio = preferences.getBoolean("record_linear_audio", true);
                profile.loopbackRecorder = preferences.getBoolean("loopback_recorder", false);
                profile.showRecordControlsInfo = preferences.getBoolean("show_record_controls_info", true);
                profile.showRecordPlayRecordWarning = preferences.getBoolean("show_record_play_record_warning", true);
                profile.RecordingCamera = preferences.get("RecordingCamera", "");
                profile.Format = new ttt.videoRecorder.TTTVideoFormat(preferences.getInt("FormatWidth", 160),  preferences.getInt("FormatHeight", 120));
                profile.VideoQuality = preferences.getFloat("VideoQuality", 0.1f);
                return profile;
            }
        } catch (Exception e) {
            System.out.println("failed to read Lecture Profile: " + e);
        }

        // new defaults
        if (lecture.length() == 0)
            return new LectureProfile("");

        // not found
        else
            return null;
    }

    // store profile permanent
    // NOTE: default values if lecture.equals("")
    public void storeProfile() {
        try {
            // store under lecture name
            Preferences preferences = profileRoot.node(lecture);

            // store values
            if (lecture != null)
                preferences.put("lecture", lecture);
            if (teacher != null)
                preferences.put("teacher", teacher);
            if (host != null)
                preferences.put("host", host);
            preferences.putInt("port", port);
            preferences.putInt("color_depth", colorDepth);
            preferences.putBoolean("record", record);
            preferences.putBoolean("record_video", recordVideo);
            preferences.putBoolean("record_linear_audio", recordLinearAudio);
            preferences.putBoolean("loopback_recorder", loopbackRecorder);
            preferences.putBoolean("show_record_controls_info", showRecordControlsInfo);
            preferences.putBoolean("show_record_play_record_warning", showRecordPlayRecordWarning);
           if(RecordingCamera != null)
            preferences.put("RecordingCamera", RecordingCamera);
            preferences.putFloat("VideoQuality", VideoQuality);
            if(Format != null){
            preferences.putInt("FormatWidth", Format.getWidth());
            preferences.putInt("FormatHeight", Format.getHeight());}
            preferences.flush();
        } catch (Exception e) {
            System.out.println("failed to write Lecture Profile: " + e);
        }
    }

    // returns names of profiles (=lectures)
    static public String[] getLectures() {
        try {
            // get lecture names
            String[] children = profileRoot.childrenNames();

            // add empty lecture name (default)
            String[] result = new String[children.length + 1];
            System.arraycopy(children, 0, result, 1, children.length);
            result[0] = "";

            return result;
        } catch (Exception e) {
            System.out.println("failed to read Lecture Profiles: " + e);
            return new String[0];
        }
    }

    // returns sorted teacher names
    static public String[] getTeachers() {
        try {
            // Create the sorted set
            SortedSet<String> set = new TreeSet<String>();

            // get profile names
            String[] names = profileRoot.childrenNames();

            // add empty name
            set.add("");

            // get teacher names
            for (int i = 0; i < names.length; i++) {
                String teacher = profileRoot.node(names[i]).get("teacher", null);
                if (teacher != null)
                    set.add(teacher);
            }

            // Create an array containing the elements in a set in order.
            return set.toArray(new String[set.size()]);

        } catch (Exception e) {
            System.out.println("failed to read Lecture Profiles: " + e);
            return new String[0];
        }
    }

    static public void clearProfiles() {
        try {
            // get profile names
            String[] names = profileRoot.childrenNames();

            // remove children
            for (int i = 0; i < names.length; i++) {
                Preferences node = profileRoot.node(names[i]);
                node.removeNode();
                node.flush();
            }
        } catch (Exception e) {
            System.out.println("failed to remove Lecture Profiles: " + e);
        }
    }

    static public void exportProfiles(File file) {
        try {
            profileRoot.exportSubtree(new FileOutputStream(file));
        } catch (Exception e) {
            System.out.println("exporting Lecture Profiles failed: " + e);
        }
    }

    static public void importProfiles(File file) {
        try {
            Preferences.importPreferences(new FileInputStream(file));
        } catch (Exception e) {
            System.out.println("importing Lecture Profiles failed: " + e);
        }
    }

    public String toString() {
        return "LectureProfile: [Lecture=" + lecture + ", Teacher=" + teacher + ", Host=" + host + ", Port=" + port
                + "]";
    }

    // get suitable title concerning lecture name, teacher name and date
    static public String getDefaultTitle(String lecture, String teacher) {
        return getDefaultTitle(lecture, teacher, System.currentTimeMillis());
    }

    // get suitable title concerning lecture name, teacher name and date
    static public String getDefaultTitle(String lecture, String teacher, long timestamp) {
        String title = "";

        // last name of teacher
        if (teacher != null) {
            teacher = teacher.trim();
            title = teacher.substring(teacher.lastIndexOf(" ") + 1);
            if (title.length() > 0)
                title += ": ";
        }

        // plus lecture title
        if (lecture != null)
            title += lecture.trim() + " ";

        // plus date
        title += getFormattedTime(timestamp);

        return title;
    }

    // get suitable title concerning lecture name and date
    static public String getDefaultFilename(String lecture) {
        String filename = "";

        // lecture title
        if (lecture != null)
            filename += lecture.trim().replace(' ', '_');
        if (filename.length() > 0)
            filename += "_";

        // plus date
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        filename += new SimpleDateFormat("yyyy_MM_dd").format(calendar.getTime());

        return filename + Constants.desktopEndings[0];
    }

    /*******************************************************************************************************************
     * Helpers for automatic title detection *
     ******************************************************************************************************************/

    private static SimpleDateFormat fmt = new SimpleDateFormat("(dd.MM.yyyy)");

    private static String getFormattedTime(long time) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(time);
        return fmt.format(calendar.getTime());
    }

    public static String getTitle(long time, String desktopName) {
        if (true)
            return desktopName;

        // auto filenames for older recordings
        if (!desktopName.equals("seidl's X desktop (atseidl8:42)"))
            return desktopName;

        System.out.println("\nCHECK TITLE");
        System.out.println("    Title: " + desktopName);

        String title = null;
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(time);

        // SS 2006
        if (within(calendar, Calendar.APRIL, 2006, Calendar.JULY, 2006, Calendar.MONDAY, 11, 14))
            title = "Seidl: Compilerbau " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2006, Calendar.JULY, 2006, Calendar.WEDNESDAY, 9, 12))
            title = "Seidl: Compilerbau " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2006, Calendar.JULY, 2006, Calendar.THURSDAY, 9, 12))
            title = "Seidl: Abstrakte Maschinen " + fmt.format(calendar.getTime());

        // WS 2005/06
        else if (within(calendar, Calendar.OCTOBER, 2005, Calendar.FEBRUARY, 2006, Calendar.MONDAY, 9, 12))
            title = "Schlichter: Informatik III " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2005, Calendar.FEBRUARY, 2006, Calendar.TUESDAY, 9, 12))
            title = "Schlichter: Informatik III " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2005, Calendar.FEBRUARY, 2006, Calendar.WEDNESDAY, 13, 15))
            title = "Berlea: Programmiersprachen " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2005, Calendar.FEBRUARY, 2006, Calendar.MONDAY, 12, 14))
            title = "Bode: Einf�hrung in die techn. Informatik " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2005, Calendar.FEBRUARY, 2006, Calendar.THURSDAY, 8, 10))
            title = "Bode: Einf�hrung in die techn. Informatik " + fmt.format(calendar.getTime());

        // SS 2005
        else if (within(calendar, Calendar.APRIL, 2005, Calendar.JULY, 2005, Calendar.TUESDAY, 11, 14))
            title = "Seidl: Informatik II " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2005, Calendar.JULY, 2005, Calendar.FRIDAY, 11, 14))
            title = "Seidl: Informatik II " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2005, Calendar.JULY, 2005, Calendar.TUESDAY, 14, 16))
            title = "Zentral-Uebung: Informatik II " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2005, Calendar.JULY, 2005, Calendar.MONDAY, 11, 14))
            title = "Seidl: Compilerbau " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2005, Calendar.JULY, 2005, Calendar.WEDNESDAY, 9, 11))
            title = "Seidl: Compilerbau " + fmt.format(calendar.getTime());

        // WS 2004/05
        else if (within(calendar, Calendar.OCTOBER, 2004, Calendar.FEBRUARY, 2005, Calendar.MONDAY, 11, 14))
            title = "Seidl: Programm-Optimierung " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2004, Calendar.FEBRUARY, 2005, Calendar.TUESDAY, 12, 14))
            title = "Seidl: Programm-Optimierung " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2004, Calendar.FEBRUARY, 2005, Calendar.THURSDAY, 8, 12))
            title = "Seidl: Informatik I " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2004, Calendar.FEBRUARY, 2005, Calendar.FRIDAY, 8, 12))
            title = "Seidl: Informatik I " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2004, Calendar.FEBRUARY, 2004, Calendar.TUESDAY, 10, 12))
            title = "Bucher: Medienwissenschaft I " + fmt.format(calendar.getTime());

        // SS 2004
        else if (within(calendar, Calendar.APRIL, 2004, Calendar.JULY, 2004, Calendar.WEDNESDAY, 13, 15))
            title = "Seidl: Abstrakte Maschinen " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2004, Calendar.JULY, 2004, Calendar.MONDAY, 12, 14))
            title = "Seidl: Compilerbau " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2004, Calendar.JULY, 2004, Calendar.WEDNESDAY, 10, 12))
            title = "Seidl: Compilerbau " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2004, Calendar.JULY, 2004, Calendar.TUESDAY, 10, 12))
            title = "Bucher: Medienwissenschaft II " + fmt.format(calendar.getTime());

        // WS 2003/04
        else if (within(calendar, Calendar.OCTOBER, 2003, Calendar.FEBRUARY, 2004, Calendar.MONDAY, 13, 15))
            title = "Seidl: Programm-Optimierung " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2003, Calendar.FEBRUARY, 2004, Calendar.THURSDAY, 10, 12))
            title = "Seidl: Programm-Optimierung " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2003, Calendar.FEBRUARY, 2004, Calendar.TUESDAY, 10, 12))
            title = "Bucher: Medienwissenschaft I " + fmt.format(calendar.getTime());

        // SS 2003
        else if (within(calendar, Calendar.APRIL, 2003, Calendar.JULY, 2003, Calendar.TUESDAY, 14, 16)
                || time == 1056525338219l || time == 1058343421449l)
            title = "Seidl: Abstrakte Maschinen " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2003, Calendar.JULY, 2003, Calendar.THURSDAY, 12, 14)
                || time == 1055495763229l)
            title = "Seidl/Wilhelm: Dokumentenverarbeitung " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2003, Calendar.JULY, 2003, Calendar.MONDAY, 10, 12))
            title = "Sturm: Systemsoftware I " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2003, Calendar.JULY, 2003, Calendar.TUESDAY, 12, 14))
            title = "Sturm: Systemsoftware I " + fmt.format(calendar.getTime());

        // WS 2002/03
        else if (within(calendar, Calendar.OCTOBER, 2002, Calendar.FEBRUARY, 2003, Calendar.TUESDAY, 8, 10))
            title = "Seidl: Informatik I " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2002, Calendar.FEBRUARY, 2003, Calendar.FRIDAY, 8, 10))
            title = "Seidl: Informatik I " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2002, Calendar.FEBRUARY, 2003, Calendar.TUESDAY, 11, 13)
                || time == 1043344092919l || time == 1044370228821l)
            title = "Bucher: Medienwissenschaft II " + fmt.format(calendar.getTime());

        // SS 2002
        else if (within(calendar, Calendar.APRIL, 2002, Calendar.JULY, 2002, Calendar.TUESDAY, 14, 16))
            title = "Seidl: Abstract Machines " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.APRIL, 2002, Calendar.JULY, 2002, Calendar.TUESDAY, 9, 11))
            title = "Bucher: Medienwissenschaft I " + fmt.format(calendar.getTime());

        // WS 2001/02
        else if (within(calendar, Calendar.OCTOBER, 2001, Calendar.FEBRUARY, 2002, Calendar.TUESDAY, 8, 10))
            title = "Seidl: Informatik 1 " + fmt.format(calendar.getTime());
        else if (within(calendar, Calendar.OCTOBER, 2001, Calendar.FEBRUARY, 2002, Calendar.FRIDAY, 8, 10)
                || time == 1010509116006l)
            title = "Seidl: Informatik 1 " + fmt.format(calendar.getTime());

        if (title == null) {
            System.out.println("    Cannot find a title matching the recording time");
            System.out.println("    " + new Date(time) + " = " + time + " msecs");
            title = fmt.format(calendar.getTime());
            System.out.println("    Changing title:");
            System.out.println("    from: " + desktopName);
            System.out.println("    to:   " + title);
        } else if (desktopName.equals(title)) {
            System.out.println("    keep name:" + desktopName);
            title = desktopName;
        } else {
            System.out.println("    Changing title:");
            System.out.println("    from: " + desktopName);
            System.out.println("    to:   " + title);
        }

        return title;
    }

    private static boolean within(Calendar calendar, int from_month, int from_year, int to_month, int to_year,
            int day_of_week, int from_hour, int to_hour) {
        // range check
        Calendar calendar_from = new GregorianCalendar();
        calendar_from.set(Calendar.YEAR, from_year);
        calendar_from.set(Calendar.MONTH, from_month);
        calendar_from.set(Calendar.DAY_OF_MONTH, calendar_from.getActualMinimum(Calendar.DAY_OF_MONTH));

        Calendar calendar_to = new GregorianCalendar();
        calendar_to.set(Calendar.YEAR, to_year);
        calendar_to.set(Calendar.MONTH, to_month);
        calendar_to.set(Calendar.DAY_OF_MONTH, calendar_to.getActualMaximum(Calendar.DAY_OF_MONTH));

        return (calendar.after(calendar_from) && calendar.before(calendar_to)
                && calendar.get(Calendar.HOUR_OF_DAY) >= from_hour && calendar.get(Calendar.HOUR_OF_DAY) < to_hour)
                && calendar.get(Calendar.DAY_OF_WEEK) == day_of_week;
    }

    /*******************************************************************************************************************
     * Getter and Setter *
     ******************************************************************************************************************/

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getHost() {
        return host;
    }

    public String getLecture() {
        return lecture;
    }

    public int getPort() {
        return port;
    }

    public String getTeacher() {
        return teacher;
    }

    public String getTitle() {
        return getDefaultTitle(lecture, teacher);
    }

    public String getFilename() {
        return getDefaultFilename(lecture);
    }

    public boolean isRecordEnabled() {
        return record;
    }

    public void setRecordEnabled(boolean record) {
        this.record = record;
    }

    public boolean isRecordVideoEnabled() {
        return recordVideo;
    }


    public void setRecordingFormat (ttt.videoRecorder.TTTVideoFormat format) {
        this.Format = format;
    }
    
    public String getRecordingCamera(){
    	return RecordingCamera;
    }
    
    public void setRecordingCamera(String recordCameraID) {
        this.RecordingCamera = recordCameraID;
    }
    
    
    
    public ttt.videoRecorder.TTTVideoFormat getVideoFormat(){
    	return Format;
    }
    
    public float getVideoQuality(){
    	return VideoQuality;
    }
    
    public void setVideoQualiy(float Quality){
    	VideoQuality = Quality;
    }
    
    
    public void setRecordVideoEnabled(boolean recordVideo) {
        this.recordVideo = recordVideo;
    }
     
    public boolean isRecordLinearAudioEnabled() {
        return recordLinearAudio;
    }

    public void setRecordLinearAudioEnabled(boolean recordLinearAudio) {
        this.recordLinearAudio = recordLinearAudio;
    }

    public boolean isLoopbackRecorder() {
        return loopbackRecorder;
    }

    public void setLoopbackRecorder(boolean loopbackRecorder) {
        this.loopbackRecorder = loopbackRecorder;
    }

    public int getColorDepth() {
        return colorDepth;
    }

    public void setColorDepth(int colorDepth) {
        this.colorDepth = colorDepth;
    }

    public boolean isShowRecordControlsInfo() {
        return showRecordControlsInfo;
    }

    public void setShowRecordControlsInfo(boolean showRecordControlsInfo) {
        this.showRecordControlsInfo = showRecordControlsInfo;
    }

    public boolean isShowRecordPlayRecordWarning() {
        return showRecordPlayRecordWarning;
    }

    public void setShowRecordPlayRecordWarning(boolean showRecordPlayRecordWarning) {
        this.showRecordPlayRecordWarning = showRecordPlayRecordWarning;
    }
}
