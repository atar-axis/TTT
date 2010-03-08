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
 * Created on 25.04.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.postprocessing;

import java.io.File;
import java.net.InetAddress;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import ttt.TTT;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

public class KeyGen {

    public static void generateKeys() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("id_dsa"));
        chooser.setDialogTitle("Choose name for key files");
        chooser.setFileHidingEnabled(false);
        int returnVal = chooser.showSaveDialog(TTT.getInstance());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String filename = chooser.getSelectedFile().getAbsolutePath();

            // create comment (typically user@host)
            String comment = "TTT";
            try {
                String username = System.getProperty("user.name");
                if (username != null)
                    comment = username;
                String localhost = InetAddress.getLocalHost().getHostName();
                if (localhost != null)
                    comment += "@" + localhost;
            } catch (Exception e) {}

            int type = KeyPair.DSA;

            JSch jsch = new JSch();

            // TODO: with or without passprhase??
            String passphrase = "";
            // JTextField passphraseField = (JTextField) new JPasswordField(20);
            // Object[] ob = { passphraseField };
            // int result = JOptionPane.showConfirmDialog(null, ob, "Enter passphrase (empty for no passphrase)",
            // JOptionPane.OK_CANCEL_OPTION);
            // if (result == JOptionPane.OK_OPTION) {
            // passphrase = passphraseField.getText();
            // }

            try {
                KeyPair kpair = KeyPair.genKeyPair(jsch, type);
                kpair.setPassphrase(passphrase);
                kpair.writePrivateKey(filename);
                kpair.writePublicKey(filename + ".pub", comment);
                System.out.println("Finger print: " + kpair.getFingerPrint());
                kpair.dispose();
            } catch (Exception e) {
                System.out.println("SSHKey generation failed: " + e);
                e.printStackTrace();
            }

            TTT.userPrefs.put("ssh_private_key", chooser.getSelectedFile().getAbsolutePath());
            System.out.println("Private ssh key file set to '" + chooser.getSelectedFile().getAbsolutePath() + "'");
        }

    }

    public static void main(String[] arg) {
        if (arg.length < 3) {
            System.err.println("usage: java KeyGen rsa output_keyfile comment\n"
                    + "       java KeyGen dsa  output_keyfile comment");
            System.exit(-1);
        }
        String _type = arg[0];
        int type = 0;
        if (_type.equals("rsa")) {
            type = KeyPair.RSA;
        } else if (_type.equals("dsa")) {
            type = KeyPair.DSA;
        } else {
            System.err.println("usage: java KeyGen rsa output_keyfile comment\n"
                    + "       java KeyGen dsa  output_keyfile comment");
            System.exit(-1);
        }
        String filename = arg[1];
        String comment = arg[2];

        JSch jsch = new JSch();

        String passphrase = "";
        JTextField passphraseField = (JTextField) new JPasswordField(20);
        Object[] ob = { passphraseField };
        int result = JOptionPane.showConfirmDialog(null, ob, "Enter passphrase (empty for no passphrase)",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            passphrase = passphraseField.getText();
        }

        try {
            KeyPair kpair = KeyPair.genKeyPair(jsch, type);
            kpair.setPassphrase(passphrase);
            kpair.writePrivateKey(filename);
            kpair.writePublicKey(filename + ".pub", comment);
            System.out.println("Finger print: " + kpair.getFingerPrint());
            kpair.dispose();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
