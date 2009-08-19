// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universität München
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
 * Created on 01.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Preferences {
    // TODO: switch to java.util.prefs.Preferences
    private static Properties preferences = getDefaultPreferences();

    private static String preferencesFile = System.getProperty("user.home") + File.separatorChar + ".ttt.preferences";

    private static String preferencesFileHeader = "Preferences file for the TeleTeachingTool. Only edit from hand if you know what you are doing.";

    private static Properties getDefaultPreferences() {
        preferences = new Properties();
        return preferences;
    }

    // getter / setter
    public static void set(String key, String value) {
        preferences.setProperty(key, value);
    }

    public static String get(String key) {
        return preferences.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return preferences.getProperty(key, defaultValue);
    }

    // reads Properties from disk
    public static void loadPreferences() {
        try {
            preferences = getDefaultPreferences();
            preferences.load(new FileInputStream(preferencesFile));
        } catch (FileNotFoundException e) {
            System.out.println("No preferences file found. Using defaults.");
            System.out.println("\tFile not found: " + preferencesFile);
        } catch (IOException e) {
            System.out.println("Error while reading preferences file. Using defaults.");
            System.out.println("\tFile : " + preferencesFile);
        }
    }

    // stores properties to disk
    public static void storePreferences() {
        try {
            if (preferences != null) {
                preferences.store(new FileOutputStream(preferencesFile), preferencesFileHeader);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not open file to write preferences.");
            System.out.println("\tFile: " + preferencesFile);
        } catch (IOException e) {
            System.out.println("Error while writing preferences.");
            System.out.println("\tFile : " + preferencesFile);
        }
    }
}
