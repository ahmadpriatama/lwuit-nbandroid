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
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.sun.lwuit.Component;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.TextField;
import com.sun.lwuit.impl.VirtualKeyboardInterface;

/**
 * 
 */
public class AndroidKeyboard implements VirtualKeyboardInterface {

    private boolean keyboardShowing;
    private Context context;
    private AndroidView myView;
    private long lastKeyboardToggle = 0;

    public AndroidKeyboard(Context context, AndroidView myView) {
        this.context = context;
        this.myView = myView;
    }

    public String getVirtualKeyboardName() {
        return "Android Keyboard";
    }

    public void showKeyboard(final boolean show) {
        Log.d("LWUIT", "showKeyboard " + show + "|" + (keyboardShowing != show));
        if (keyboardShowing != show) {

            /**
             * when opening and closing the keyboard repeatedly and in a fast sequence, 
             * for example when switching between textfields, the inputmethodmanager
             * might ignore some requests or get messed up for some other reason. so we
             * track the keyboard state immediately and post the imput method requests
             * with a short delay and on the UI thread.
             */
            long delay = (System.currentTimeMillis() - this.lastKeyboardToggle) < 600L ? 600L : 10L;
            lastKeyboardToggle = System.currentTimeMillis();
            this.keyboardShowing = show;


            this.myView.postDelayed(new Runnable() {

                public void run() {
                    // by default use our surface as the editable.
                    View view = myView;
                    
                    // if a TextField is focused and we are configured to use the
                    // invisible text field then we need to call the inputmethodmanager
                    // with the native edittext to ensure this native view receives
                    // the proper callbacks: onCheckIsTextEditor() and onCreateInputConnection()
                    Form current = Display.getInstance().getCurrent();
                    Component focusedComponent = current == null ? null : current.getFocused();
                    TextField tf = focusedComponent instanceof TextField ? (TextField) focusedComponent : null;
                    if (AndroidImplementation.USE_INVISIBLE_TEXT_INPUT_CONNECTION && tf != null) {
                        InvisibleTextEdit editText = myView.getImplementation().getAndroidLayout().getInvisibleTextEdit();
                        view = editText;
                        editText.startInvisibleEdit(tf);
                    }
                    
                    InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    manager.restartInput(view);
                    //  manager.toggleSoftInputFromWindow(myView.getWindowToken(), 0, 0);
                    //     this.keyboardShowing = show;
                    if (show) {
                        manager.showSoftInput(view, 0);
                    } else {
                        manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            }, delay);
        }
    }

    public boolean isVirtualKeyboardShowing() {
        return keyboardShowing;
    }

    public void setInputType(int inputType) {
        // todo
    }
}
