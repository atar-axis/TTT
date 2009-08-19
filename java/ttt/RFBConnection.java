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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.JOptionPane;

public class RFBConnection implements Connection {

    // I/O
    private Socket socket;
    private ReconnectingInputStream userInputStream;
    private FailSafeOutputStream userOutputStream;

    // initialization pereferences
    private ProtocolPreferences prefs;

    public RFBConnection(InetAddress host, int port, String password) {
        prefs = new ProtocolPreferences();
        prefs.host = host;
        prefs.port = port;
        prefs.password = password;
    }

    // TODO: add switching to another host
    // TODO: catch errors, like host not found

    boolean disableCursorEncodings = false;

    // connect to given vnc server
    public synchronized void connect() throws IOException {
        connect(disableCursorEncodings);
    }

    public synchronized void connect(boolean disableCursorEncodings) throws IOException {
        this.disableCursorEncodings = disableCursorEncodings;
        // cleanup (if needed)
        if (socket != null)
            try {
                socket.close();
            } catch (IOException e) {}
        socket = null;

        // init

        // I/O stream used during initialisation
        DataInputStream initializationDataInputStream = null;
        OutputStream initializationOutputstream = null;
        InputStream initializationInputStream = null;

        // connect
        int reconnectcounter = 0;
        while (socket == null) {
            if (TTT.verbose)
                System.out.println("    Connect to " + prefs.host + " : " + prefs.port);
            // open socket and I/O streams
            try {
                // was:
                // socket = new Socket(prefs.host, prefs.port);

                // initialize socket
                socket = new Socket();
                InetSocketAddress socketAddress = new InetSocketAddress(prefs.host, prefs.port);
                // try to connect - timeout set to 20 sec
                // NOTE: default timeout is 3 min, which is far too long
                socket.connect(socketAddress, 20000);

            } catch (IOException e) {
                System.out.println(e);
                // set unconnected socket to null;
                socket = null;

                // failed: wait and retry (no dialog during automatic reconnect)
                if (reconnecting) {
                    if (++reconnectcounter % 10 == 0) {
                        TTT.showMessage("Connection lost. Reconnect failed " + reconnectcounter
                                + " times - still trying.", "Lost Connection", JOptionPane.ERROR_MESSAGE);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {}
                    continue;
                }

                // ask to retry
                else if (JOptionPane.showConfirmDialog(TTT.getInstance(), "Connection failed: " + e + "\nTry again?",
                        "Connection Failure", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    continue;

                // or quit
                throw e;
            }

            initializationInputStream = new BufferedInputStream(socket.getInputStream(), 16384);
            initializationDataInputStream = new DataInputStream(initializationInputStream);
            initializationOutputstream = socket.getOutputStream();

            // read server version
            byte[] b = new byte[12];
            initializationDataInputStream.readFully(b);
            prefs.versionMsg = new String(b);

            checkVersionMessage(prefs.versionMsg);
            // send client version
            initializationOutputstream.write(Constants.VersionMessageRFB.getBytes());

            // authentication - try again if failed
            if (!authentication(initializationDataInputStream, initializationOutputstream))
                continue;
        }

        // request shared session
        initializationOutputstream.write(Constants.Shared);

        // read server pixel format and name
        readServerInit(initializationDataInputStream);

        // set encoding and format
        prefs.setEncodings(disableCursorEncodings ? Constants.encodingsWithoutCursorEncodings : Constants.encodings);
        Constants.writeSetEncodingsMessage(initializationOutputstream, prefs);
        prefs.setDepth(prefs.preferedDepth);
        Constants.writeSetPixelFormatMessage(initializationOutputstream, prefs);

        // all done - make visible for consumers
        if (userInputStream == null)
            userInputStream = new ReconnectingInputStream(initializationInputStream);
        else
            userInputStream.setInputStream(initializationInputStream);

        if (userOutputStream == null)
            userOutputStream = new FailSafeOutputStream(initializationOutputstream);
        else
            userOutputStream.setOutputStream(initializationOutputstream);

        initializationDataInputStream = null;
        initializationOutputstream = null;
    }

    // reconnect to server
    // self repairing connection after error detection
    private boolean reconnecting;

    public synchronized void reconnect() {
        reconnecting = true;
        while (reconnecting)
            try {
                // try to connect
                connect();

                // request full framebuffer update
                Constants.writeFramebufferUpdateRequestMessage(getOutputStream(), 0, 0, prefs.framebufferWidth,
                        prefs.framebufferHeight, false);

                // reconnected
                reconnecting = false;
                System.out.println("reconnected");

            } catch (IOException e) {
                // failed - wait and retry
                try {
                    // TODO: dynamically adopt timeout
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {}
            }
    }

    // close whatever need to be closed
    public void close() {
        // stop auto reconnect (if running)
        reconnecting = false;

        if (userInputStream != null)
            try {
                userInputStream.close();
            } catch (Exception e) {}
        userInputStream = null;

        if (userOutputStream != null)
            try {
                userOutputStream.close();
            } catch (Exception e) {}
        userOutputStream = null;

        if (socket != null)
            try {
                socket.close();
            } catch (IOException e) {}
        socket = null;
    }

    /*******************************************************************************************************************
     * Initialisation *
     ******************************************************************************************************************/

    // read server version
    private void checkVersionMessage(String versionMsg) throws IOException {
        if (versionMsg == null || versionMsg.length() != 12)
            throw new IOException("Bad server version (" + versionMsg + ")");

        byte[] b = versionMsg.getBytes();

        // examine server version:
        // starting with TTT or RFB
        // followed by one blank
        // followed by 3 digits (major version number)
        // followed by one dot
        // followed by 3 digits (minor version number)
        // followed by one newline
        if ((!(((b[0] == 'T') && (b[1] == 'T') && (b[2] == 'T')) || ((b[0] == 'R') && (b[1] == 'F') && (b[2] == 'B'))))
                || (b[3] != ' ') || (b[4] < '0') || (b[4] > '9') || (b[5] < '0') || (b[5] > '9') || (b[6] < '0')
                || (b[6] > '9') || (b[7] != '.') || (b[8] < '0') || (b[8] > '9') || (b[9] < '0') || (b[9] > '9')
                || (b[10] < '0') || (b[10] > '9') || (b[11] != '\n')) {
            throw new IOException("Bad server version (" + new String(b) + ")");
        }

        // output
        if (TTT.verbose) {
            System.out.print("      Client TTT Version: " + Constants.VersionMessageTTT);
            System.out.print("      Client RFB Version: " + Constants.VersionMessageRFB);
            System.out.print("      Server Version: " + versionMsg);
        }

        if (b[11] != '\n')
            System.out.println();

        // check TTT version number
        if (versionMsg.startsWith("TTT")) {
            int serverMajor = (b[4] - '0') * 100 + (b[5] - '0') * 10 + (b[6] - '0');
            int serverMinor = (b[8] - '0') * 100 + (b[9] - '0') * 10 + (b[10] - '0');

            byte[] bb = Constants.VersionMessageTTT.getBytes();
            int clientMajor = (bb[4] - '0') * 100 + (bb[5] - '0') * 10 + (bb[6] - '0');
            int clientMinor = (bb[8] - '0') * 100 + (bb[9] - '0') * 10 + (bb[10] - '0');

            if (serverMajor > clientMajor || (serverMajor == clientMajor && serverMinor > clientMinor)) {
                JOptionPane.showMessageDialog(null, "Newer Protocol Version detected: " + versionMsg
                        + "\n\tClient Version: " + Constants.VersionMessageTTT
                        + "\n\tUpgrade your version of TeleTeachingTool at http://TTT.Uni-Trier.de",
                        "Newer Protocol Version Detected", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // authentication
    private boolean authentication(DataInputStream initializationDataInputStream,
            OutputStream initializationOutputstream) throws IOException {
        int authScheme = initializationDataInputStream.readInt();
        switch (authScheme) {
        case Constants.ConnectionFailed:
            byte[] reason = new byte[initializationDataInputStream.readInt()];
            initializationDataInputStream.readFully(reason);
            throw new IOException(new String(reason));

        case Constants.NoAuthentication:
            System.out.println("      No authentication needed");
            return true;

        case Constants.Authentication:
            byte[] challenge = new byte[16];
            initializationDataInputStream.readFully(challenge);

            if (prefs.password == null)
                prefs.password = PasswordDialog.getPassword();

            if (prefs.password == null)
                throw new IOException("Aborted by user.");

            if (prefs.password.length() > 8)
                prefs.password = prefs.password.substring(0, 8);
            // Truncate to 8 chars

            byte[] key = { 0, 0, 0, 0, 0, 0, 0, 0 };
            System.arraycopy(prefs.password.getBytes(), 0, key, 0, prefs.password.length());

            DesCipher des = new DesCipher(key);

            des.encrypt(challenge, 0, challenge, 0);
            des.encrypt(challenge, 8, challenge, 8);

            initializationOutputstream.write(challenge);

            int authResult = initializationDataInputStream.readInt();

            switch (authResult) {
            case Constants.AuthenticationOK:
                System.out.println("      Authentication succeeded");
                return true;

            case Constants.AuthenticationFailed:
                System.out.println("      Authentication failed: Wrong password");
                // clear password, close connection and try again (reconnect)
                prefs.password = null;
                if (socket != null)
                    socket.close();
                socket = null;
                return false;

            case Constants.AuthenticationTooManny:
                System.out.println("      Authentication failed: Too many tries");
                throw new IOException("Authentication failed: Too many tries");

            default:
                System.out.println("      Unknown authentication result: " + String.valueOf(authResult));
                throw new IOException("Unknown authentication result: " + String.valueOf(authResult));
            }

        default:
            System.out.println("      Unknown authentication result, authScheme: " + String.valueOf(authScheme));
            throw new IOException("Unknown authentication result, authScheme: " + String.valueOf(authScheme));
        }
    }

    // read server initialisation
    private void readServerInit(DataInputStream initializationDataInputStream) throws IOException {
        int framebufferWidth = initializationDataInputStream.readUnsignedShort();
        int framebufferHeight = initializationDataInputStream.readUnsignedShort();

        // TODO: close recorder and restart or support resolution change
        if ((prefs.framebufferHeight > 0 && prefs.framebufferHeight != framebufferHeight)
                || (prefs.framebufferWidth > 0 && prefs.framebufferWidth != framebufferWidth)) {

            TTT.showMessage("Resolution of remote desktop has changed - NOT SUPPORTED BY TTT\n"
                    + "Please close and restart TTT Recorder", "Reconnection Error", JOptionPane.ERROR_MESSAGE);

            // TODO: close recorder

            throw new IOException("Cannot reconnect: Resolution of remote desktop has changed");
        }

        prefs.framebufferWidth = framebufferWidth;
        prefs.framebufferHeight = framebufferHeight;
        prefs.bitsPerPixel = initializationDataInputStream.readUnsignedByte();
        prefs.bytesPerPixel = prefs.bitsPerPixel == 8 ? 1 : 4;
        prefs.depth = initializationDataInputStream.readUnsignedByte();
        prefs.bigEndian = (initializationDataInputStream.readUnsignedByte() != 0);
        prefs.trueColour = (initializationDataInputStream.readUnsignedByte() != 0);
        prefs.redMax = initializationDataInputStream.readUnsignedShort();
        prefs.greenMax = initializationDataInputStream.readUnsignedShort();
        prefs.blueMax = initializationDataInputStream.readUnsignedShort();
        prefs.redShift = initializationDataInputStream.readUnsignedByte();
        prefs.greenShift = initializationDataInputStream.readUnsignedByte();
        prefs.blueShift = initializationDataInputStream.readUnsignedByte();
        // padding
        initializationDataInputStream.skipBytes(3);
        int nameLength = initializationDataInputStream.readInt();
        byte[] name = new byte[nameLength];
        initializationDataInputStream.readFully(name);

        // keep previously set name
        // NOTE: required if name was specified by a lecture profile
        if (prefs.name == null || prefs.name.equals(""))
            prefs.name = new String(name);
    }

    /*******************************************************************************************************************
     * Input / Output
     ******************************************************************************************************************/

    public ProtocolPreferences getProtocolPreferences() {
        return prefs;
    }

    public InputStream getInputStream() {
        return userInputStream;
    }

    public OutputStream getOutputStream() {
        return userOutputStream;
    }

    private class ReconnectingInputStream extends InputStream {
        private InputStream inputStream;
        private boolean running = true;

        public ReconnectingInputStream(InputStream in) {
            inputStream = in;
        }

        void setInputStream(InputStream in) {
            inputStream = in;
        }

        public void close() throws IOException {
            running = false;
            super.close();
        }

        public int read() {
            while (running)
                try {
                    return inputStream.read();
                } catch (SocketException e) {
                    if (running && !reconnecting) {
                        System.out.println("Lost connection: Trying to reconnect");
                        reconnect();
                    }
                } catch (IOException e) {
                    System.out.println("Error while reading: Reading failed");
                    return -1;
                }
            return -1;
        }
    }

    // TODO: maybe change to ReconnectingOutputStream
    private class FailSafeOutputStream extends OutputStream {
        private OutputStream outputStream;

        public FailSafeOutputStream(OutputStream out) {
            outputStream = out;
        }

        void setOutputStream(OutputStream out) {
            outputStream = out;
        }

        // write to server
        public void write(byte[] buffer) { // throws IOException {
            write(buffer, 0, buffer.length);
        }

        // write to server
        public void write(byte[] buffer, int offset, int length) {// throws IOException {
            try {
                // TODO: output may be buffered even if connection is lost - detect
                outputStream.write(buffer, offset, length);
            } catch (SocketException e) {
                System.out.println("Lost connection: Writing failed");
            } catch (IOException e) {
                System.out.println("Error while writing: Writing failed");
            }
        }

        public void write(int value) {
            try {
                // TODO: output may be buffered even if connection is lost - detect
                outputStream.write(value);
            } catch (SocketException e) {
                System.out.println("Lost connection: Writing failed");
            } catch (IOException e) {
                System.out.println("Error while writing: Writing failed");
            }
        }
    }
}
