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

package ttt.gui;

import java.awt.Toolkit;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * @author Peter Ziewer (University of Trier,Germany)
 * 
 * textfield to enter port numbers
 */
public class NumberField extends JTextField {

    private int length;

    public NumberField(int length) {
        super(length);
        this.length = length;
        setHorizontalAlignment(JTextField.CENTER);
        setToolTipText("port");
    }

    public int getNumber() {
        try {
            return Integer.parseInt(getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    protected Document createDefaultModel() {
        return new NumberDocument(this);
    }

    static class NumberDocument extends PlainDocument {
        // accepts up to five numbers

        NumberField field;

        NumberDocument(NumberField field) {
            this.field = field;
        }

        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

            if (str == null) {
                return;
            }
            char[] chars = str.toCharArray();
            char[] numbers = new char[chars.length];

            int j = 0;
            for (int i = 0; i < chars.length; i++) {
                switch (chars[i]) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    numbers[j++] = chars[i];
                    break;

                default:
                    Toolkit.getDefaultToolkit().beep();
                    break;
                }
            }

            int len = field.getText().length();
            if (len + j > field.length) {
                j = field.length - len;
                Toolkit.getDefaultToolkit().beep();
            }

            super.insertString(offs, new String(numbers, 0, j), a);
        }
    }
}
