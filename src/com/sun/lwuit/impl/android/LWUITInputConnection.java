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

import android.content.Context;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputMethodManager;
import com.sun.lwuit.Command;
import com.sun.lwuit.Component;
import com.sun.lwuit.Display;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.events.ActionEvent;

/**
 *  This is an attempt to implement proper inputconnection support. Unfortunately this
 * is a really complex task as the Android APIs are not exactly well documented in this
 * area. I just could not make it work properly to enable support for the more advanced
 * editing features. For the time being I leave it here as is. This code is by default
 * no longer used. To enable it toggle the AndroidImplementation.USE_LWUIT_INPUT_CONNECTION
 * flag to true. Good luck!
 */
public class LWUITInputConnection extends BaseInputConnection {

    private LWUITEditable edit;
    private AndroidView view;
    private final boolean sensitive;

    public LWUITInputConnection(final AndroidView view, final TextArea textArea) {
        super(view, true);
        this.view = view;
        this.sensitive = (textArea.getConstraint() & TextArea.SENSITIVE) != 0
                || (textArea.getConstraint() & TextArea.PASSWORD) != 0;
        this.edit = new LWUITEditable(textArea);
        this.edit.watchLWUITCursor(view);
    }

    @Override
    public boolean performEditorAction(int actionCode) {
        Log.d("LWUIT", "performEditorAction " + actionCode);
        if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
            Component txtCmp = Display.getInstance().getCurrent().getFocused();
            if (txtCmp != null && txtCmp instanceof TextField) {
                if (actionCode == EditorInfo.IME_ACTION_GO
                        || actionCode == EditorInfo.IME_ACTION_DONE) {
                    Display.getInstance().setShowVirtualKeyboard(false);
                    Command cmd = Display.getInstance().getCurrent().getDefaultCommand();
                    if (cmd != null) {
                        Display.getInstance().getCurrent().dispatchCommand(cmd, new ActionEvent(cmd, null, 0, 0));
                        return true;
                    } else {
                        return false;
                    }
                } else if (actionCode == EditorInfo.IME_ACTION_NEXT) {
//                    Display.getInstance().setShowVirtualKeyboard(false);
                    txtCmp.getNextFocusDown().requestFocus();
                    Display.getInstance().setShowVirtualKeyboard(true);
//                    Display.getInstance().getCurrent().getNextFocusDown().requestFocus();
                    return true;
                }
            }
        }
        return super.performEditorAction(actionCode);
    }

    @Override
    public boolean finishComposingText() {
        if (!sensitive) {
            Log.d("LWUIT", "finishComposingText ");
        }
        this.edit.stopWatchLWUITCursor();
        return super.finishComposingText();
    }

    public Editable getEditable() {
        return this.edit;
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        if (!sensitive) {
            Log.d("LWUIT", "getExtractedText ");
        }
        if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
            Component txtCmp = Display.getInstance().getCurrent().getFocused();
            if (txtCmp != null && txtCmp instanceof TextField) {
                //String txt = ((TextField) txtCmp).getText();
                String txt = this.edit.toString();
                if (txt != null) {
                    ExtractedText et = new ExtractedText();
                    et.partialStartOffset = -1;
                    et.partialEndOffset = -1;
                    et.text = txt;
                    et.flags = 0;
                    et.flags |= ExtractedText.FLAG_SINGLE_LINE;
                    et.startOffset = 0;
                    et.selectionStart = this.edit.getCursor();
                    et.selectionEnd = et.selectionStart;
                    //et.selectionStart = Selection.getSelectionStart(txt);
                    //et.selectionEnd = Selection.getSelectionEnd(txt);
                    return et;
                }
            }
        }
        return null;
    }
}
