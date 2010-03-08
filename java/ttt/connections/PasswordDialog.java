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
 * Created on 02.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.connections;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import ttt.TTT;

// Helper class which creates a GUI and asks user for a password
public class PasswordDialog implements ActionListener {

    // creates GUI, asks user for password and returns password
    public static String getPassword() {
        // creat GUI
        PasswordDialog passwordDialog = new PasswordDialog();

        // wait until password received
        synchronized (passwordDialog) {
            try {
                passwordDialog.wait();
            } catch (InterruptedException e) {}
        }

        // remove dialog and return user input
        String password = passwordDialog.password;
        passwordDialog.dispose();
        passwordDialog = null;
        return password;
    }

    final private static String OK = "Ok";
    final private static String CANCEL = "Cancel";

    private JInternalFrame frame;
    private JPasswordField passwordField;
    private String password;

    // create dialog frame
    private PasswordDialog() {
        if (SwingUtilities.isEventDispatchThread())
            createAndShowPasswordDialog();
        else
            // Schedule a job for the event-dispatching thread:
            // creating and showing this application's GUI.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createAndShowPasswordDialog();
                }
            });
    }

    // remove GUI
    private void dispose() {
        frame.dispose();
        frame = null;
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (OK.equals(cmd)) { // Process the password.
            password = new String(passwordField.getPassword());
        } else { // aborted
            password = null;
        }
        // wake up getPassword()
        synchronized (this) {
            notify();
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
     */
    private void createAndShowPasswordDialog() {
        // Make sure we have nice window decorations.
        // TODO: should not be handled here, but somewhere global
        // JFrame.setDefaultLookAndFeelDecorated(true);

        // Create and set up the window.
        frame = new JInternalFrame("Authentication");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create everything.
        passwordField = new JPasswordField(10);
        passwordField.setEchoChar('#');
        passwordField.setActionCommand(OK);
        passwordField.addActionListener(this);

        JLabel label = new JLabel("Enter the password: ");
        label.setLabelFor(passwordField);

        JComponent buttonPane = createButtonPanel();

        // Lay out everything.
        JPanel textPane = new JPanel();
        textPane.setLayout(new GridLayout(0, 1));

        textPane.add(label);
        textPane.add(passwordField);

        // Create and set up the content pane.
        JPanel newContentPane = new JPanel();
        newContentPane.setLayout(new FlowLayout(FlowLayout.TRAILING));
        newContentPane.add(textPane);
        newContentPane.add(buttonPane);

        newContentPane.setOpaque(true); // content panes must be opaque
        frame.setContentPane(newContentPane);

        // Make sure the focus goes to the right component
        // whenever the frame is initially given the focus.
        frame.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameActivated(InternalFrameEvent arg0) {
                // Must be called from the event-dispatching thread.
                passwordField.requestFocusInWindow();
            }

            public void internalFrameClosing(InternalFrameEvent arg0) {
                // aborted
                password = null;
                // wake up getPassword()
                synchronized (PasswordDialog.this) {
                    PasswordDialog.this.notify();
                }
            }
        });

        // Display the window.
        TTT.getInstance().addInternalFrameCentered(frame);

        passwordField.requestFocusInWindow();
    }

    // create buttons
    private JComponent createButtonPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(0, 1));
        JButton okButton = new JButton(OK);
        JButton helpButton = new JButton(CANCEL);

        okButton.setActionCommand(OK);
        helpButton.setActionCommand(CANCEL);
        okButton.addActionListener(this);
        helpButton.addActionListener(this);

        p.add(okButton);
        p.add(helpButton);

        return p;
    }
}
