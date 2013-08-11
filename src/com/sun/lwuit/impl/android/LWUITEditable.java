/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.sun.lwuit.impl.android;

import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.animations.Animation;

/**
 *  By default not used. See description in LWUITInputConnection.java
 */
public class LWUITEditable extends SpannableStringBuilder implements TextWatcher {

    private TextArea textArea;
    private boolean stopWatchLWUITCursor;

    public LWUITEditable(TextArea t) {
        this.textArea = t;
        String text = t.getText();
        if (text == null) {
            text = "";
        }
        this.append(text);
        setCursorAt(Math.max(0, t.getCursorPosition()));
        this.setSpan(this, 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE /*| (PRIORITY << Spanned.SPAN_PRIORITY_SHIFT)*/);
    }

    void stopWatchLWUITCursor() {
        this.stopWatchLWUITCursor = true;
    }

    void watchLWUITCursor(final AndroidView view) {
        
        Log.d("LWUIT", "watch lwuit cursor");

        // register animation on EDT once. here we monitor the cursor position of the
        // lwuit textfield and apply it to the android editable if required.
        Display.getInstance().callSerially(new Runnable() {

            public void run() {
                final Form current = textArea.getComponentForm();
                if (current != null) {
                    current.registerAnimated(new Animation() {

                        int lwuitCursorLastChecked = textArea.getCursorPosition();

                        public boolean animate() {     
                            boolean active = !stopWatchLWUITCursor
                                    && textArea.hasFocus()
                                    && Display.getInstance().getDefaultVirtualKeyboard() != null
                                    && Display.getInstance().getDefaultVirtualKeyboard().isVirtualKeyboardShowing();

                            if (active) {
                                // request LWUIT cursor position on EDT
                                final int currentCursor = textArea.getCursorPosition();
                                if (currentCursor != lwuitCursorLastChecked) {
                                    lwuitCursorLastChecked = currentCursor;
                                    // change android cursor on android UI thread.
                                    view.post(new Runnable() {

                                        public void run() {
                                            LWUITEditable.this.setCursorAt(currentCursor);
                                        }
                                    });
                                }
                            } else {
                                // cleanup self.
                                current.deregisterAnimated(this);
                                Log.d("LWUIT", "stop watch lwuit cursor");
                            }
                            return active;
                        }

                        public void paint(Graphics g) {
                            // no paint, all work in animate().
                        }
                    });
                }
            }
        });
    }
    
    void setCursorAt(int index) {
        Selection.setSelection(this, Math.max(0, Math.min(index, this.length())));
    }

    int getCursor() {
        return Selection.getSelectionStart(this);
    }

    public void beforeTextChanged(CharSequence cs, int i, int i1, int i2) {
    }

    public void onTextChanged(CharSequence cs, int i, int i1, int i2) {
    }

    public void afterTextChanged(Editable edtbl) {
        if (edtbl == this) {
            final String text = this.toString();
            final int cursor = Selection.getSelectionEnd(this);
            
            // monitor the current text for changes.
            this.removeSpan(this);
            this.setSpan(this, 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE /*| (PRIORITY << Spanned.SPAN_PRIORITY_SHIFT)*/);
            
            Display.getInstance().callSerially(new Runnable() {

                public void run() {
                    textArea.setText(text);
                    if (cursor >= 0 && textArea instanceof TextField) {
                        ((TextField) textArea).setCursorPosition(cursor);
                    }
                }
            });
        }
    }
}
