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
 * Created on 09.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.player;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;


import ttt.messages.CursorPositionMessage;
import ttt.messages.DeleteAllAnnotation;
import ttt.messages.KeyEventMessage;
import ttt.messages.Message;
import ttt.messages.MessageConsumer;
import ttt.messages.PointerEventMessage;

public class RFBKeyAndMouseListener implements KeyListener, MouseListener, MouseMotionListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MessageConsumer messageConsumer;

    // create
    public RFBKeyAndMouseListener(MessageConsumer messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    // /////////////////////////////////////////////////////
    // Handle key events.
    // /////////////////////////////////////////////////////
    public void keyPressed(KeyEvent evt) {
        processLocalKeyEvent(evt);
    }

    public void keyReleased(KeyEvent evt) {
        processLocalKeyEvent(evt);
    }

    public void keyTyped(KeyEvent evt) {
        evt.consume();
    }

    // /////////////////////////////////////////////////////
    // ALT-TAB emulation
    // /////////////////////////////////////////////////////
    protected boolean alt_tab;

    // emulate ALT-TAB to switch between windows (task switch)
    private void emulate_ALT_TAB(boolean down) {
        // activate ALT-TAB emulation
        if (!alt_tab) {
            writeModifierKeyEvents(KeyEvent.ALT_MASK);
            writeMessage(new DeleteAllAnnotation(0));
        }
        alt_tab = true;

        // write TAB (switches to next windows)
        writeKeyEvent(0xff09, down);

        // send to VNC server
        writeMessageBuffer();
    }

    // leave ALT-TAB emulation
    private void leave_ALT_TAB_emulation() {
        if (alt_tab) {
            // release all modifiers (incl. ALT)
            writeModifierKeyEvents(0);
            alt_tab = false;

            // send to VNC server
            writeMessageBuffer();
        }
    }

    // /////////////////////////////////////////////////////
    // Handle mouse events.
    // /////////////////////////////////////////////////////
    public void mousePressed(MouseEvent evt) {
        processLocalMouseEvent(evt, false);
    }

    public void mouseReleased(MouseEvent evt) {
        processLocalMouseEvent(evt, false);
    }

    public void mouseMoved(MouseEvent evt) {
        processLocalMouseEvent(evt, true);
    }

    public void mouseDragged(MouseEvent evt) {
        processLocalMouseEvent(evt, true);
    }

    // Ignored events.
    public void mouseClicked(MouseEvent evt) {}

    public void mouseEntered(MouseEvent evt) {}

    public void mouseExited(MouseEvent evt) {}

    // ////////////////////////////////////////////////////////////////////////
    // Write key and mouse event message
    // ////////////////////////////////////////////////////////////////////////

    // TODO: add optional suppressing of key hold
    // immediatelly send release and only release if pressed and not released yet
    synchronized private void processLocalKeyEvent(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_F11) {
            // emulate ALT-TAB to switch between windows (task switch)
            emulate_ALT_TAB(evt.getID() == KeyEvent.KEY_PRESSED);

        } else if (alt_tab) {
            // leave ALT-TAB emultaion after any other key was pressed and released
            if (evt.getID() != KeyEvent.KEY_PRESSED)

                leave_ALT_TAB_emulation();

        } else {
            writeKeyEvent(evt);
        }
        // Don't ever pass keyboard events to AWT for default processing.
        // Otherwise, pressing Tab would switch focus to ButtonPanel etc.
        evt.consume();
    }

    private void processLocalMouseEvent(MouseEvent event, boolean moved) {
        // leave ALT-TAB emulation (if activated)
        leave_ALT_TAB_emulation();

        if (moved) {
            // send position to clients
            // TODO: only needed if using cursor shape
            messageConsumer.handleMessage(new CursorPositionMessage(0, event.getX(), event.getY()));
        }

        writePointerEvent(event);
    }

    //
    // A buffer for putting pointer and keyboard events before being sent. This
    // is to ensure that multiple RFB events generated from a single Java Event
    // will all be sent in a single network packet. The maximum possible
    // length is 4 modifier down events, a single key event followed by 4
    // modifier up events i.e. 9 key events or 72 bytes.
    // private byte[] eventBuf = new byte[72];
    // private int eventBufLen;

    // Useful shortcuts for modifier masks.
    private final static int CTRL_MASK = InputEvent.CTRL_MASK;
    private final static int SHIFT_MASK = InputEvent.SHIFT_MASK;
    private final static int META_MASK = InputEvent.META_MASK;
    private final static int ALT_MASK = InputEvent.ALT_MASK;

    //
    // Write a pointer event message. We may need to send modifier key events
    // around it to set the correct modifier state.
    private int pointerMask = 0;

    private void writePointerEvent(MouseEvent evt) {
        int modifiers = evt.getModifiers();

        int mask2 = 2;
        int mask3 = 4;

        // Note: For some reason, AWT does not set BUTTON1_MASK on left
        // button presses. Here we think that it was the left button if
        // modifiers do not include BUTTON2_MASK or BUTTON3_MASK.
        if (evt.getID() == MouseEvent.MOUSE_PRESSED) {
            if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
                pointerMask = mask2;
                modifiers &= ~ALT_MASK;
            } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
                pointerMask = mask3;
                modifiers &= ~META_MASK;
            } else {
                pointerMask = 1;
            }
        } else if (evt.getID() == MouseEvent.MOUSE_RELEASED) {
            pointerMask = 0;
            if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
                modifiers &= ~ALT_MASK;
            } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
                modifiers &= ~META_MASK;
            }
        }

        // eventBufLen = 0;
        writeModifierKeyEvents(modifiers);

        int x = evt.getX();
        int y = evt.getY();

        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;

        addMessageToMessageBuffer(new PointerEventMessage(pointerMask, x, y));

        //
        // Always release all modifiers after an "up" event
        //
        if (pointerMask == 0) {
            writeModifierKeyEvents(0);
        }

        writeMessageBuffer();
    }

    // Java on UNIX does not call keyPressed() on some keys, for example
    // swedish keys To prevent our workaround to produce duplicate
    // keypresses on JVMs that actually works, keep track of if
    // keyPressed() for a "broken" key was called or not.
    private boolean brokenKeyPressed = false;

    //
    // Write a key event message. We may need to send modifier key events
    // around it to set the correct modifier state. Also we need to translate
    // from the Java key values to the X keysym values used by the RFB protocol.
    private void writeKeyEvent(KeyEvent evt) { // throws IOException {
        int keyChar = evt.getKeyChar();

        //
        // Ignore event if only modifiers were pressed.
        //

        // Some JVMs return 0 instead of CHAR_UNDEFINED in getKeyChar().
        if (keyChar == 0)
            keyChar = KeyEvent.CHAR_UNDEFINED;

        if (keyChar == KeyEvent.CHAR_UNDEFINED) {
            int code = evt.getKeyCode();
            if (code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_META
                    || code == KeyEvent.VK_ALT)
                return;
        }

        // Key press or key release?
        boolean down = (evt.getID() == KeyEvent.KEY_PRESSED);

        int key;
        if (evt.isActionKey()) {
            // An action key should be one of the following.
            // If not then just ignore the event.
            switch (evt.getKeyCode()) {
            case KeyEvent.VK_HOME:
                key = 0xff50;
                break;
            case KeyEvent.VK_LEFT:
                key = 0xff51;
                break;
            case KeyEvent.VK_UP:
                key = 0xff52;
                break;
            case KeyEvent.VK_RIGHT:
                key = 0xff53;
                break;
            case KeyEvent.VK_DOWN:
                key = 0xff54;
                break;
            case KeyEvent.VK_PAGE_UP:
                key = 0xff55;
                break;
            case KeyEvent.VK_PAGE_DOWN:
                key = 0xff56;
                break;
            case KeyEvent.VK_END:
                key = 0xff57;
                break;
            case KeyEvent.VK_INSERT:
                key = 0xff63;
                break;
            case KeyEvent.VK_F1:
                key = 0xffbe;
                break;
            case KeyEvent.VK_F2:
                key = 0xffbf;
                break;
            case KeyEvent.VK_F3:
                key = 0xffc0;
                break;
            case KeyEvent.VK_F4:
                key = 0xffc1;
                break;
            case KeyEvent.VK_F5:
                key = 0xffc2;
                break;
            case KeyEvent.VK_F6:
                key = 0xffc3;
                break;
            case KeyEvent.VK_F7:
                key = 0xffc4;
                break;
            case KeyEvent.VK_F8:
                key = 0xffc5;
                break;
            case KeyEvent.VK_F9:
                key = 0xffc6;
                break;
            case KeyEvent.VK_F10:
                key = 0xffc7;
                break;
            case KeyEvent.VK_F11:
                key = 0xffc8;
                break;
            case KeyEvent.VK_F12:
                key = 0xffc9;
                break;
            default:
                return;
            }

        } else {
            //
            // A "normal" key press. Ordinary ASCII characters go straight through.
            // For CTRL-<letter>, CTRL is sent separately so just send <letter>.
            // Backspace, tab, return, escape and delete have special keysyms.
            // Anything else we ignore.
            //
            key = keyChar;

            if (key < 0x20) {
                if (evt.isControlDown()) {
                    key += 0x60;
                } else {
                    switch (key) {
                    case KeyEvent.VK_BACK_SPACE:
                        key = 0xff08;
                        break;
                    case KeyEvent.VK_TAB:
                        key = 0xff09;
                        break;
                    case KeyEvent.VK_ENTER:
                        key = 0xff0d;
                        break;
                    case KeyEvent.VK_ESCAPE:
                        key = 0xff1b;
                        break;
                    }
                }
            } else if (key == 0x7f) {
                // Delete
                key = 0xffff;
            } else if (key > 0xff) {
                // JDK1.1 on X incorrectly passes some keysyms straight through,
                // so we do too. JDK1.1.4 seems to have fixed this.
                // The keysyms passed are 0xff00 .. XK_BackSpace .. XK_Delete
                if ((key < 0xff00) || (key > 0xffff))
                    return;
            }
        }

        // Fake keyPresses for keys that only generates keyRelease events
        if ((key == 0xe5) || (key == 0xc5) || // XK_aring / XK_Aring
                (key == 0xe4) || (key == 0xc4) || // XK_adiaeresis / XK_Adiaeresis
                (key == 0xf6) || (key == 0xd6) || // XK_odiaeresis / XK_Odiaeresis
                (key == 0xa7) || (key == 0xbd) || // XK_section / XK_onehalf
                (key == 0xa3)) { // XK_sterling
            // Make sure we do not send keypress events twice on platforms
            // with correct JVMs (those that actually report KeyPress for all
            // keys)
            if (down)
                brokenKeyPressed = true;

            if (!down && !brokenKeyPressed) {
                // We've got a release event for this key, but haven't received
                // a press. Fake it.
                // eventBufLen = 0;
                writeModifierKeyEvents(evt.getModifiers());
                writeKeyEvent(key, true);
                // protocol.write(eventBuf, 0, eventBufLen);

                writeMessageBuffer();
            }

            if (!down)
                brokenKeyPressed = false;
        }

        // eventBufLen = 0;
        writeModifierKeyEvents(evt.getModifiers());
        writeKeyEvent(key, down);

        // Always release all modifiers after an "up" event
        if (!down)
            writeModifierKeyEvents(0);

        writeMessageBuffer();
    }

    //
    // Add a raw key event with the given X keysym to eventBuf.
    //
    private void writeKeyEvent(int keysym, boolean down) {
        addMessageToMessageBuffer(new KeyEventMessage(keysym, down));
    }

    //
    // Write key events to set the correct modifier state.
    //
    private int oldModifiers = 0;

    private void writeModifierKeyEvents(int newModifiers) {
        if ((newModifiers & CTRL_MASK) != (oldModifiers & CTRL_MASK))
            writeKeyEvent(0xffe3, (newModifiers & CTRL_MASK) != 0);

        if ((newModifiers & SHIFT_MASK) != (oldModifiers & SHIFT_MASK))
            writeKeyEvent(0xffe1, (newModifiers & SHIFT_MASK) != 0);

        if ((newModifiers & META_MASK) != (oldModifiers & META_MASK))
            writeKeyEvent(0xffe7, (newModifiers & META_MASK) != 0);

        if ((newModifiers & ALT_MASK) != (oldModifiers & ALT_MASK))
            writeKeyEvent(0xffe9, (newModifiers & ALT_MASK) != 0);

        oldModifiers = newModifiers;
    }

    // buffer message and send all together
    private ArrayList<Message> messageBuffer = new ArrayList<Message>();

    synchronized private void addMessageToMessageBuffer(Message message) {
        messageBuffer.add(message);
    }

    synchronized private void writeMessageBuffer() {
        while (!messageBuffer.isEmpty()) {
            writeMessage(messageBuffer.remove(0));
        }
    }

    synchronized protected void writeMessage(Message message) {
        messageConsumer.handleMessage(message);
    }
}
