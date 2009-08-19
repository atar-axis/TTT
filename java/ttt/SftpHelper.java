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
 * Created on 12.03.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class SftpHelper {

    private Session session;
    private ChannelSftp channel;
    private String startPath;

    public static void main(String[] arg) throws Exception {
        if (arg.length != 2) {
            System.err.println("usage: java ScpTo file1 user@remotehost:file2");
            System.exit(-1);
        }

        String lfile = arg[0];
        String user = arg[1].substring(0, arg[1].indexOf('@'));
        arg[1] = arg[1].substring(arg[1].indexOf('@') + 1);
        String host = arg[1].substring(0, arg[1].indexOf(':'));
        String rfile = arg[1].substring(arg[1].indexOf(':') + 1);
        SftpHelper session = new SftpHelper(user, host);
        session.publish(lfile, rfile, true);
        session.close();
    }

    public SftpHelper(String user, String host) throws /* JSchException, */Exception {
        JSch jsch = new JSch();

        // check for private keyfile
        try {
            File keyfile = null;
            // file specified by user
            String keyfilename = TTT.userPrefs.get("ssh_private_key", null);
            if (keyfilename != null)
                keyfile = new File(keyfilename);

            // otherwise use system default file (Linux/Unix)
            if (keyfile == null || !keyfile.isFile())
                keyfile = new File(System.getProperty("user.home") + "/.ssh/id_dsa");

            // set key
            if (keyfile != null && keyfile.isFile()) {
                System.out.println("key loaded from: " + keyfile.getAbsoluteFile());
                jsch.addIdentity(keyfile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Cannot read private key file: " + e);
        }

        // check for known hosts file
        // NOTE: new hosts will not be added by JSCH/TTT
        try {
            File hostsfile = null;
            // file specified by user
            String hostsfilename = TTT.userPrefs.get("ssh_known_hosts", null);
            if (hostsfilename != null)
                hostsfile = new File(hostsfilename);

            // otherwise use system default file (Linux/Unix)
            if (hostsfile == null || !hostsfile.isFile())
                hostsfile = new File(System.getProperty("user.home") + "/.ssh/known_hosts");

            // set known hosts
            if (hostsfile != null && hostsfile.isFile()) {
                System.out.println("known hosts loaded from: " + hostsfile.getAbsoluteFile());
                jsch.setKnownHosts(hostsfile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Cannot read known hosts file: " + e);
        }

        // initialize session
        session = jsch.getSession(user, host, 22);

        // username and password will be given via UserInfo interface.
        UserInfo ui = new MyUserInfo();
        session.setUserInfo(ui);

        try {
            session.connect();
        } catch (JSchException e) {
            if (e.getMessage().contains("NoRouteToHost")) {
                // workaround for our stupid server which often fails once
                System.out.println("No route to host - try again");
                session.connect();
            } else
                throw e;
        }

        // set faster enciphering
        java.util.Hashtable config = new java.util.Hashtable();
        // config.put("cipher.s2c", "none,3des-cbc,blowfish-cbc");
        // config.put("cipher.c2s", "none,3des-cbc,blowfish-cbc");
        config.put("cipher.s2c", "none,blowfish-cbc");
        config.put("cipher.c2s", "none,blowfish-cbc");
        // config.put("cipher.s2c", "none,3des-cbc");
        // config.put("cipher.c2s", "none,3des-cbc");
        session.setConfig(config);
        session.rekey();

        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        startPath = channel.pwd();

    }

    public void close() {
        session.disconnect();
    }

    public void closeChannel() {
        channel.disconnect();
    }

    public boolean publish(String source, String destination_path) {
        return publish(source, destination_path, false);
    }

    public boolean publish(String source, String destination_path, boolean batch) {
        recursive = false;
        try {
            File file = new File(source);
            if (file.exists())
                return copy(source, destination_path, batch);
            else
                System.out.println("copy " + source + " \tNOT FOUND");
        } catch (Exception e) {
            System.out.println("\npublishing '" + source + "' failed: " + e);
            e.printStackTrace();
        }
        return false;
    }

    // change directory to given path
    // NOTE: folder(s) for given path will be created if not existent
    private boolean cd(String absolute_path) {
        try {
            // reset to default login path
            channel.cd(startPath);

            // make path absolute
            // NOTE: otherwise one must cut the already created path at the beginning of the path string
            if (!absolute_path.startsWith("/"))
                absolute_path = channel.pwd() + "/" + absolute_path;

            try {
                // try to change directory to given path
                channel.cd(absolute_path);
                return true;
            } catch (SftpException e) {
                // directory does not exists - create directory

                // extract name of parent folder
                int pos = absolute_path.length() - 1;
                if (absolute_path.endsWith("/"))
                    pos--;
                pos = absolute_path.lastIndexOf('/', pos);
                String parent = absolute_path.substring(0, pos);

                // fo to parent folder and create subfolder
                if (cd(parent)) {
                    channel.mkdir(absolute_path);
                    System.out.println("folder " + absolute_path + " \tcreated");
                    channel.cd(absolute_path);
                    return true;
                } else
                    throw new IOException("Cannot access '" + parent + "'");
            }
        } catch (Exception e) {
            System.out.println("Cannot access '" + absolute_path + "': " + e);
            e.printStackTrace();
        }
        return false;
    }

    private boolean recursive = false;

    private boolean copy(String source, String destination_path, boolean batch) throws SftpException {
        if (cd(destination_path)) {
            File file = new File(source);

            // copy directory
            if (file.isDirectory()) {
                // change directory (folder will be created if not present)
                // NOTE: assumes "/" to be separator of destination file system (may fail otherwise)
                if (!destination_path.endsWith("/"))
                    destination_path += "/";
                destination_path += file.getName();

                String children[] = file.list();
                if (children.length == 0) {
                    // no children - create directory only
                    return cd(destination_path);
                }
                // recursively copy all children
                else {
                    boolean result = true;
                    for (String child : children) {
                        try {
                            if (!recursive)
                                System.out.println("copy folder " + source);
                            recursive = true;
                            result &= copy(file.getCanonicalPath() + File.separator + child, destination_path, batch);
                        } catch (IOException e) {
                            System.out.println("Copying of '" + child + "' failed: " + e);
                            e.printStackTrace();
                        }
                    }
                    return result;
                }
            }
            // copy file
            else {
                if (recursive) {
                    channel.put(source, ".", null);
                } else {
                    System.out.print("copy " + source + " \t");
                    channel.put(source, ".", batch ? null : new MyProgressMonitor());
                    System.out.println();
                }
                return true;
            }
        }
        return false;
    }

    public static class MyProgressMonitor implements SftpProgressMonitor {
        ProgressMonitor monitor;
        long count = 0;
        long max = 0;
        long l;

        public void init(int op, String src, String dest, long max) {
            this.max = max;
            monitor = new ProgressMonitor(TTT.ttt, ((op == SftpProgressMonitor.PUT) ? "Copying" : "get") + ": "
                    + new File(src).getName(), "", 0, (int) max);
            // monitor = new ProgressMonitor(TTT.getInstance(), ((op == SftpProgressMonitor.PUT) ? "Copying" : "get")
            // + ": " + new File(src).getName(), "", 0, (int) max);
            count = 0;
            percent = -1;
            monitor.setProgress((int) this.count);
            monitor.setMillisToDecideToPopup(1000);
            l = System.currentTimeMillis();
        }

        private long percent = -1;

        public boolean count(long count) {
            this.count += count;

            if (percent >= this.count * 100 / max) {
                return true;
            }
            percent = this.count * 100 / max;

            // monitor.setNote("Completed " + this.count + "(" + percent + "%) out of " + max + ".");
            System.out.print((percent < 10 ? " " : "") + (percent < 100 ? " " : "") + percent + "%\b\b\b\b");
            monitor.setProgress((int) this.count);

            return !(monitor.isCanceled());
        }

        public void end() {
            monitor.close();
            l = System.currentTimeMillis() - l;
            // TODO: ugly hack - suppressing output of recursively copied html folder (small files -> faster than 500
            // msec)
            if (l > 500)
                System.out.print("\t" + Constants.getStringFromTime((int) l) + "\t"
                        + (int) ((max / 1024d) / (l / 1000d)) + "k/sec");
        }
    }

    public void executeCommand(String command) throws JSchException, IOException {
        System.out.println("Executing command:\n    " + command);
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        channel.setInputStream(null);

        ((ChannelExec) channel).setErrStream(System.err);

        InputStream in = channel.getInputStream();

        channel.connect();

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0)
                    break;
                System.out.print(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                System.out.println("exit-status: " + channel.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {}
        }
        channel.disconnect();
    }

    public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
        public String getPassword() {
            return passwd;
        }

        public boolean promptYesNo(String str) {
            Object[] options = { "yes", "no" };
            int foo = JOptionPane.showOptionDialog(null, str, "Warning", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            return foo == 0;
        }

        String passwd;
        JTextField passwordField = (JTextField) new JPasswordField(20);

        public String getPassphrase() {
            return null;
        }

        public boolean promptPassphrase(String message) {
            return true;
        }

        public boolean promptPassword(String message) {
            Object[] ob = { passwordField };
            int result = JOptionPane.showConfirmDialog(null, ob, message, JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                passwd = passwordField.getText();
                return true;
            } else {
                return false;
            }
        }

        public void showMessage(String message) {
            JOptionPane.showMessageDialog(null, message);
        }

        final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
        private Container panel;

        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt,
                boolean[] echo) {
            panel = new JPanel();
            panel.setLayout(new GridBagLayout());

            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.gridx = 0;
            panel.add(new JLabel(instruction), gbc);
            gbc.gridy++;

            gbc.gridwidth = GridBagConstraints.RELATIVE;

            JTextField[] texts = new JTextField[prompt.length];
            for (int i = 0; i < prompt.length; i++) {
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridx = 0;
                gbc.weightx = 1;
                panel.add(new JLabel(prompt[i]), gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weighty = 1;
                if (echo[i]) {
                    texts[i] = new JTextField(20);
                } else {
                    texts[i] = new JPasswordField(20);
                }
                panel.add(texts[i], gbc);
                gbc.gridy++;
            }

            if (JOptionPane.showConfirmDialog(null, panel, destination + ": " + name, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                String[] response = new String[prompt.length];
                for (int i = 0; i < prompt.length; i++) {
                    response[i] = texts[i].getText();
                }
                return response;
            } else {
                return null; // cancel
            }
        }
    }
}
