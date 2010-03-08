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
 * Created on 03.05.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.helper;

import javax.swing.JOptionPane;

import ttt.TTT;

// check whether a dedicated library is installed or not
public class LibraryChecker {

    private static boolean isJavaMediaFramworkInstalled = false;

    public static boolean isJavaMediaFramworkInstalled() {
        if (!isJavaMediaFramworkInstalled)
            try {
                ClassLoader.getSystemClassLoader().loadClass("javax.media.Manager");

                // allows movies to be rendered in swing containers
                javax.media.Manager.setHint(javax.media.Manager.LIGHTWEIGHT_RENDERER, new Boolean(true));

                isJavaMediaFramworkInstalled = true;
            } catch (ClassNotFoundException e) {
                System.out.println("Java Media Framework: NOT INSTALLED");
                JOptionPane
                        .showMessageDialog(
                                TTT.ttt,
                                "Java Media Framework is not installed\nCannot record/replay any audio or video files/streams\n\n(Re)Install Java Media Framework",
                                "Error...", JOptionPane.ERROR_MESSAGE);
            }
        return isJavaMediaFramworkInstalled;
    }

    private static boolean isITextPdfLibraryInstalled = false;

    public static boolean isITextPdfLibraryInstalled() {
        if (!isITextPdfLibraryInstalled)
            try {
                ClassLoader.getSystemClassLoader().loadClass("com.lowagie.text.Document");
                isITextPdfLibraryInstalled = true;
            } catch (ClassNotFoundException e) {
                System.out.println("iText - Free Java-PDF library: NOT INSTALLED");
                JOptionPane.showMessageDialog(TTT.ttt,
                        "iText - Free Java-PDF library is not installed:\nCannot generate PDF scripts", "Warning...",
                        JOptionPane.WARNING_MESSAGE);
            }
        return isITextPdfLibraryInstalled;
    }

    private static boolean isSwingLayoutExtensionsLibraryInstalled = false;

    public static boolean isSwingLayoutExtensionsLibraryInstalled() {
        if (!isSwingLayoutExtensionsLibraryInstalled)
            try {
                ClassLoader.getSystemClassLoader().loadClass("org.jdesktop.layout.GroupLayout");
                isSwingLayoutExtensionsLibraryInstalled = true;
            } catch (ClassNotFoundException e) {
                System.out.println("Swing Layout Extensions Library: NOT INSTALLED");
                JOptionPane.showMessageDialog(TTT.ttt,
                        "Swing Layout Extensions Library is not installed:\nCannot open post processing dialog",
                        "Error...", JOptionPane.ERROR_MESSAGE);
            }
        return isSwingLayoutExtensionsLibraryInstalled;
    }

    private static boolean isJSchInstalled = false;

    public static boolean isJSchInstalled() {
        if (!isJSchInstalled)
            try {
                ClassLoader.getSystemClassLoader().loadClass("com.jcraft.jsch.JSch");
                isJSchInstalled = true;
            } catch (ClassNotFoundException e) {
                System.out.println("JSch - Java Secure Channel Library: NOT INSTALLED");
                JOptionPane.showMessageDialog(TTT.ttt,
                        "JSch - Java Secure Channel Library is not installed:\nCannot copy data to file server",
                        "Warning...", JOptionPane.WARNING_MESSAGE);
            }
        return isJSchInstalled;
    }
}
