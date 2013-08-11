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

import android.graphics.Region;
import com.sun.lwuit.Display;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import com.sun.lwuit.Command;
import com.sun.lwuit.Component;
import com.sun.lwuit.Form;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import java.lang.reflect.Method;

/**
 * this class can be changed to use a plain android.view.View parent if required.
 * the performance will be worse, though.
 */
public class AndroidView extends SurfaceView implements SurfaceHolder.Callback {

    private int width = 1;
    private int height = 1;
    private Bitmap bitmap;
    private AndroidGraphics buffy = null;
    private Canvas canvas;
    private AndroidImplementation implementation = null;
    private final Rect bounds = new Rect();
    private boolean fireKeyDown = false;
    private int deadkey = 0;
    private SurfaceHolder surfaceHolder = null;
    private volatile boolean created = false;
    
    
    public AndroidView(Activity activity, AndroidImplementation implementation) {
        super(activity);
        this.implementation = implementation;
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        setFocusable(true);
        setFocusableInTouchMode(true);
        setEnabled(true);
        setClickable(true);
        setLongClickable(false);

        /**
         * tell the system that we do our own caching and it does not
         * need to use an extra offscreen bitmap.
         */
        setWillNotCacheDrawing(false);

        this.buffy = new AndroidGraphics(implementation, null);

        /**
         * From the docs:
         * "Change whether this view is one of the set of scrollable containers in its window.
         * This will be used to determine whether the window can resize or must pan when a soft
         * input area is open -- scrollable containers allow the window to use resize mode since the
         * container will appropriately shrink. "
         */
        setScrollContainer(true);

        surfaceHolder = getHolder();
        
        // http://www.curious-creature.org/2010/12/08/bitmap-quality-banding-and-dithering/
        // http://groups.google.com/group/android-developers/browse_thread/thread/a8c2a29bc8b6b23d/29d04df00109b645?lnk=gst&q=gradient+#29d04df00109b645
        // http://groups.google.com/group/android-developers/browse_thread/thread/25972e2c70f1b8b7/a37f901d133c59d8?lnk=gst&q=rgbx_8888#a37f901d133c59d8
        // looks like RGBA_8888 is supported everywhere and even the default on OS 2.3+.  
        // now using it as a default through this implementation, too.  this avoids
        // drawing difficulties of images and gradients. 
        surfaceHolder.setFormat(PixelFormat.RGBA_8888);
        surfaceHolder.addCallback(this);
        
        android.view.Display androidDisplay = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        width = androidDisplay.getWidth();
        height = androidDisplay.getHeight();
        initBitmaps(width, height);
        Log.d("LWUIT", "view created");
    }

    //@Override
    public boolean isOpaque() {
        // Since: API Level 7
        return true;
    }

    AndroidImplementation getImplementation() {
        return implementation;
    }
    
    private void initBitmaps(int w, int h) {
        this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final boolean apiLevel_12 = android.os.Build.VERSION.SDK_INT >= 12;
        if (apiLevel_12) {
            try {
                Method m = bitmap.getClass().getMethod("setHasAlpha", Boolean.class);
                m.invoke(bitmap, Boolean.FALSE);
                Log.e("LWUIT", "setHasAlpha FALSE applied.");
            } catch (Exception e) {
                Log.e("LWUIT", "problem with setHasAlpha ", e);
            }
        }
        this.canvas = new Canvas(this.bitmap);
        this.buffy.setCanvas(this.canvas);
    }
    
    private void visibilityChangedTo(boolean visible) {
        Log.d("LWUIT", " visibilityChangedTo " + visible);
        if (this.implementation.getCurrentForm() != null) {
            if (visible) {
                this.implementation.showNotifyPublic();
                Display.getInstance().callSerially(new Runnable() {

                    public void run() {
                        // if the repaint() call is not placed on the EDT
                        // it might be lost it seems.
                        implementation.getCurrentForm().repaint();
                    }
                });
                //android.os.Debug.startMethodTracing("calc");
            } else {
                this.implementation.hideNotifyPublic();
                //android.os.Debug.stopMethodTracing();
            }
        }
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        created = true;
        this.visibilityChangedTo(true);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        created = false;
        this.visibilityChangedTo(false);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (this.implementation.editInProgress()) {
            /**
             * while edit is in progress a virtual keyboard might
             * resize everything.  this is problematic because the
             * editing (as it is implemented now) blocks the EDT.
             * so we just drop these resize events for now.  once
             * editing is complete we might apply the last state.
             */
            this.implementation.setLastSizeChangedWH(w, h);
            return;
        }

        this.handleSizeChange(w, h);
    }

    // for use when not using surface view as parent. keeping as a reference
    // in case you want to revert to a previous behavior of this class which
    // did not use the surface view parent.
//    @Override
//    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        super.onSizeChanged(w, h, oldw, oldh);
//
//        if (this.implementation.editInProgress()) {
//            /**
//             * while edit is in progress a virtual keyboard might
//             * resize everything.  this is problematic because the
//             * editing (as it is implemented now) blocks the EDT.
//             * so we just drop these resize events for now.  once
//             * editing is complete we might apply the last state.
//             */
//            this.implementation.setLastSizeChangedWH(w, h);
//            return;
//        }
//
//        this.handleSizeChange(w, h);
//    }
//    @Override
//    protected void onWindowVisibilityChanged(int visibility) {
//        super.onWindowVisibilityChanged(visibility);
//        Log.d("LWUIT", "visibility: " + visibility + " " + View.VISIBLE);
//        this.visibilityChangedTo(visibility == View.VISIBLE);
//    }

    public void flushGraphics(Rect rect, AndroidLayout androidLayout) {
        //Log.d("LWUIT", "flush graphics: " + created);
        this.realFlush(rect);
        //androidLayout.flushGraphics(rect);
    }

    public void flushGraphics(AndroidLayout androidLayout) {
        //Log.d("LWUIT", "flush graphics: " + created);
        this.realFlush(null);
        //androidLayout.flushGraphics();
    }
    
    void realFlush(Rect rect) {
        if (created) {
            Canvas c = null;
            try {
                if (rect == null) {
                    c = this.surfaceHolder.lockCanvas();
                } else {
                    c = this.surfaceHolder.lockCanvas(rect);
                }
                if (c != null) {
                    this.onDraw(c);
                }
            } catch (Throwable e) {
                Log.e("LWUIT", "paint problem.", e);
            } finally {
                if (c != null) {
                    this.surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }
    
    public void handleSizeChange(int w, int h) {

        /**
         * if the size is reduced AND if we have the native virtual
         * keyboard showing we should make sure that the currently focused
         * component (most likely a TextField) is visible.  doing this here is the best way that
         * i can see to detect the delayed opening of a native virtual keyboard.
         */
        final boolean makeFocusedComponentVisible = this.height > h && Display.getInstance().isVirtualKeyboardShowing()
                && Display.getInstance().getDefaultVirtualKeyboard() instanceof AndroidKeyboard;
          
        if (this.width < w || this.height < h) {
            this.initBitmaps(w, h);
        }
        this.width = w;
        this.height = h;
        Log.d("LWUIT", "sizechanged: " + width + " " + height);
        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send
             * events to the EDT.  if we send events before the
             * form has been set we might deadlock!
             */
            return;
        }
        Display.getInstance().sizeChanged(w, h);
        
        if (makeFocusedComponentVisible) {
            /**
             * ruequest the scroll animation with a delay to give the LWUIT UI time to
             * layout first.
             */
            this.postDelayed(new Runnable() {

                public void run() {
                    Display.getInstance().callSerially(new Runnable() {

                        public void run() {
                            Form current = Display.getInstance().getCurrent();
                            if (current != null) {
                                Component focused = current.getFocused();
                                if (focused != null) {
                                    current.scrollComponentToVisible(current.getFocused());
                                }
                            }
                        }
                    });
                }
            }, 600L);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d("LWUIT", "onDraw");
        boolean empty = canvas.getClipBounds(bounds);
        if (empty) {
            // ??
            canvas.drawBitmap(bitmap, 0, 0, null);
        } else {
            bounds.intersect(0, 0, width, height);
            canvas.drawBitmap(bitmap, bounds, bounds, null);
        }
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_UNKNOWN){
            Log.d("LWUIT", "multiple input: '" + event.getCharacters() + "'");
        }
        return true;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && Display.getInstance().getDefaultVirtualKeyboard() instanceof AndroidKeyboard) {
            /**
             * if the IME consumes the back command we might not find out about
             * the closed keyboard at all. so we trigger a close here on our own.
             */
            if (Display.getInstance().getDefaultVirtualKeyboard().isVirtualKeyboardShowing()) {
                // consume for keyboard close.
                Display.getInstance().getDefaultVirtualKeyboard().showKeyboard(false);
                return true;
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return this.onKeyUpDown(true, keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return this.onKeyUpDown(false, keyCode, event);
    }
    
    /**
     * some info from the MIDP docs about keycodes:
     *
     *  "Applications receive keystroke events in which the individual keys are named within a space of key codes.
     * Every key for which events are reported to MIDP applications is assigned a key code. The key code values are
     * unique for each hardware key unless two keys are obvious synonyms for each other. MIDP defines the following
     * key codes: KEY_NUM0, KEY_NUM1, KEY_NUM2, KEY_NUM3, KEY_NUM4, KEY_NUM5, KEY_NUM6, KEY_NUM7, KEY_NUM8, KEY_NUM9,
     * KEY_STAR, and KEY_POUND. (These key codes correspond to keys on a ITU-T standard telephone keypad.) Other
     * keys may be present on the keyboard, and they will generally have key codes distinct from those list above.
     * In order to guarantee portability, applications should use only the standard key codes.
     *
     * The standard key codes' values are equal to the Unicode encoding for the character that represents the key.
     * If the device includes any other keys that have an obvious correspondence to a Unicode character, their key
     * code values should equal the Unicode encoding for that character. For keys that have no corresponding Unicode
     * character, the implementation must use negative values. Zero is defined to be an invalid key code."
     *
     * Because the MIDP implementation is our reference and that implementation does not interpret the given keycodes
     * we behave alike and pass on the unicode values.
     */
    static int internalKeyCodeTranslate(int keyCode) {
        /**
         * make sure these important keys have a negative value when passed
         * to LWUIT or they might be interpreted as characters.
         */
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return AndroidImplementation.DROID_IMPL_KEY_DOWN;
            case KeyEvent.KEYCODE_DPAD_UP:
                return AndroidImplementation.DROID_IMPL_KEY_UP;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return AndroidImplementation.DROID_IMPL_KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return AndroidImplementation.DROID_IMPL_KEY_RIGHT;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                return AndroidImplementation.DROID_IMPL_KEY_FIRE;
            case KeyEvent.KEYCODE_MENU:
                return AndroidImplementation.DROID_IMPL_KEY_MENU;
            case KeyEvent.KEYCODE_CLEAR:
                return AndroidImplementation.DROID_IMPL_KEY_CLEAR;
            case KeyEvent.KEYCODE_DEL:
                return AndroidImplementation.DROID_IMPL_KEY_BACKSPACE;
            case KeyEvent.KEYCODE_BACK:
                return AndroidImplementation.DROID_IMPL_KEY_BACK;
            case KeyEvent.KEYCODE_SYM:
                return AndroidImplementation.DROID_IMPL_KEY_SYMBOL;
            default:
                return keyCode;
        }
    }

    private final boolean onKeyUpDown(boolean down, int keyCode, KeyEvent event) {
        if (event.getRepeatCount() > 0) {
            // skip repeats
            return true;
        }

        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send
             * events to the EDT.  if we send events before the
             * form has been set we might deadlock!
             */
            return true;
        }

        keyCode = internalKeyCodeTranslate(keyCode);

        if (keyCode == AndroidImplementation.DROID_IMPL_KEY_FIRE) {
            this.fireKeyDown = down;
        } else if (keyCode == AndroidImplementation.DROID_IMPL_KEY_DOWN
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_UP
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_LEFT
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_RIGHT) {
            if (this.fireKeyDown) {
                /**
                 * we keep track of trackball press/release.  while it is pressed we drop directional
                 * movements.  these movements are most likely not intended.  if the device has no
                 * trackball i see no situation where this additional behavior could hurt.
                 */
                return true;
            }
        }

        if (keyCode == AndroidImplementation.DROID_IMPL_KEY_MENU) {
            if (Display.getInstance().getCommandBehavior()
                    == Display.COMMAND_BEHAVIOR_NATIVE) {
                // let the platform handle this to trigger the menu.
                return false;
            }
        }
        
        switch (keyCode) {
            case AndroidImplementation.DROID_IMPL_KEY_BACK:
            case AndroidImplementation.DROID_IMPL_KEY_DOWN:
            case AndroidImplementation.DROID_IMPL_KEY_UP:
            case AndroidImplementation.DROID_IMPL_KEY_LEFT:
            case AndroidImplementation.DROID_IMPL_KEY_RIGHT:
            case AndroidImplementation.DROID_IMPL_KEY_FIRE:
            case AndroidImplementation.DROID_IMPL_KEY_MENU:
            case AndroidImplementation.DROID_IMPL_KEY_CLEAR:
            case AndroidImplementation.DROID_IMPL_KEY_BACKSPACE:
            case AndroidImplementation.DROID_IMPL_KEY_SYMBOL:
                // directly pass to display.
                if (down) {
                    Display.getInstance().keyPressed(keyCode);
                } else {
                    Display.getInstance().keyReleased(keyCode);
                }
                return true;
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                // skip
                return true;
            default:
                /**
                 * LWUIT's TextField does not seem to work well if two keyup-keydown
                 * sequences of different keys are not strictly sequential.  so we
                 * pass the up event of a character right after the down event.  this is
                 * exactly the behavior of the BlackBerry implementation from this repository
                 * and has worked well for me.  i guess this should be changed as soon as
                 * the TextField changes.
                 */
                if (down) {
                    int nextchar = event.getUnicodeChar();
                    if(nextchar != 0){
                        if((nextchar & KeyCharacterMap.COMBINING_ACCENT) != 0){
                            deadkey = nextchar;
                            return true;
                        } else {
                            if(deadkey != 0){
                                nextchar = KeyEvent.getDeadChar(deadkey, keyCode);
                                deadkey = 0;
                            }
                        }
                           
                        if (nextchar == '\n' && Display.getInstance().isVirtualKeyboardShowing()
                                && Display.getInstance().getDefaultVirtualKeyboard() instanceof AndroidKeyboard) {
                            /**
                             * this is annoying. the enter key on a soft keyboard does not seem to trigger calls
                             * to LWUITInputConnection.performEditorAction(..) even if the InputConnection has
                             * been created with EditorInfo.IME_ACTION_DONE.  so we should close the IME here.
                             * 
                             * if using BaseInputConnection this works, too.
                             */
                            Form current = Display.getInstance().getCurrent();
                            if (current != null) {
                                Component focused = current.getFocused();
                                if (focused != null && focused instanceof TextField 
                                        && ((TextField)focused).isSingleLineTextArea()) {
                                    Display.getInstance().setShowVirtualKeyboard(false);
                                    return true;
                                }
                            }
                        }

                        Display.getInstance().keyPressed(nextchar);
                        Display.getInstance().keyReleased(nextchar);
                    } else {
                        Log.d("LWUIT", "unrecognized keyCode: " + keyCode);
                    }
                }
                return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send
             * events to the EDT.  if we send events before the
             * form has been set we might deadlock!
             */
            return true;
        }
        
        if(!this.hasFocus()){
            this.requestFocusFromTouch();
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.implementation.pointerPressed((int) event.getX(), (int) event.getY());
                break;
            case MotionEvent.ACTION_UP:
                this.implementation.pointerReleased((int) event.getX(), (int) event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                this.implementation.pointerDragged((int) event.getX(), (int) event.getY());
                break;
        }

        return true;
    }

    public AndroidGraphics getGraphics() {
        return buffy;
    }

    public int getViewHeight() {
        return height;
    }

    public int getViewWidth() {
        return width;
    }


    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {

        if (!Display.isInitialized() || Display.getInstance().getCurrent() == null) {
            return super.onCreateInputConnection(editorInfo);
        }

        final int constraints;
        final Component txtCmp = Display.getInstance().getCurrent().getFocused();
        final boolean textFieldFocus = txtCmp != null && txtCmp instanceof TextField;
        
        if(textFieldFocus){
            constraints = ((TextField) txtCmp).getConstraint();
        } else {
            constraints = 0;
        }
        
        editorInfo.inputType = InvisibleTextEdit.configureEditorInfoInputTypes(constraints);
        editorInfo.imeOptions = InvisibleTextEdit.configureEditorInfoIMEOptions(textFieldFocus ? ((TextField)txtCmp) : null);
    
        final boolean sensitive = (constraints & TextArea.SENSITIVE) != 0 || (constraints & TextArea.PASSWORD) != 0;

        if (AndroidImplementation.USE_LWUIT_INPUT_CONNECTION && textFieldFocus) {
            return new LWUITInputConnection(this, ((TextField) txtCmp));
        } else {
            final BaseInputConnection b = new BaseInputConnection(this, false);
            return new InputConnectionWrapper(b, false) {

                @Override
                public boolean commitText(CharSequence text, int newCursorPosition) {
                    if (!sensitive) {
                        Log.d("LWUIT", "commitText ");
                    }
                    return super.commitText(text, newCursorPosition);
                }

                @Override
                public boolean endBatchEdit() {
                    if (!sensitive) {
                        Log.d("LWUIT", "endBatchEdit ");
                    }
                    return super.endBatchEdit();
                }

                @Override
                public boolean finishComposingText() {
                    if (!sensitive) {
                        Log.d("LWUIT", "finishComposingText ");
                    }
                    return super.finishComposingText();
                }

                @Override
                public CharSequence getTextAfterCursor(int n, int flags) {
                    if (!sensitive) {
                        Log.d("LWUIT", "getTextAfterCursor ");
                    }
                    return super.getTextAfterCursor(n, flags);
                }

                @Override
                public CharSequence getTextBeforeCursor(int n, int flags) {
                    if (!sensitive) {
                        Log.d("LWUIT", "getTextBeforeCursor ");
                    }
                    return super.getTextBeforeCursor(n, flags);
                }

                @Override
                public boolean performEditorAction(int editorAction) {
                    if (!sensitive) {
                        Log.d("LWUIT", "performEditorAction ");
                    }
                    return super.performEditorAction(editorAction);
                }

                @Override
                public boolean sendKeyEvent(KeyEvent event) {
                    if (!sensitive) {
                        Log.d("LWUIT", "sendKeyEvent " /* + event.toString() */);
                    }

                    // joy! sometimes there is just no ACTION_UP event and only a single
                    // ACTION_DOWN.
                    //if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    switch (event.getAction()) {
                        case KeyEvent.ACTION_UP:
                            // ignore and only respond to ACTION_DOWN.
                            return false;
                        case KeyEvent.ACTION_MULTIPLE:
                            if (event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
                                String chars = event.getCharacters();
                                if (chars != null) {
                                    for (int i = 0; i < chars.length(); i++) {
                                        super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, chars.charAt(i)));
                                        super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, chars.charAt(i)));
                                    }
                                }
                            }
                            return false;
                        default:
                            super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, event.getKeyCode()));
                            super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, event.getKeyCode()));
                            return false;
                    }
                    //}
                    //return super.sendKeyEvent(event);
                }

                @Override
                public boolean setComposingText(CharSequence text, int newCursorPosition) {
                    if (!sensitive) {
                        Log.d("LWUIT", "setComposingText ");
                    }
                    return super.setComposingText(text, newCursorPosition);
                }

                @Override
                public boolean setSelection(int start, int end) {
                    if (!sensitive) {
                        Log.d("LWUIT", "setSelection ");
                    }
                    return super.setSelection(start, end);
                }

                @Override
                public boolean deleteSurroundingText(int leftLength, int rightLength) {
                    if (!sensitive) {
                        Log.d("LWUIT", "deleteSurroundingText " + leftLength + " " + rightLength);
                    }
                    // some extra code to handle swype backspacing. narf.
                    if (rightLength == 0 && leftLength == 0) {
                        this.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                        this.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
                    } else {
                        for (int i = 0; i < leftLength; i++) {
                            this.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                            this.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
                        }
                    }
                    return true;
                }
            };
        }
    }

    @Override
    public boolean onCheckIsTextEditor() {
        if(!Display.isInitialized() || Display.getInstance().getCurrent() == null){
            return false;
        }
        Component txtCmp = Display.getInstance().getCurrent().getFocused();
        if (txtCmp != null && txtCmp instanceof TextField) {
            return AndroidImplementation.USE_LWUIT_INPUT_CONNECTION;
        }
        return false;
    }


}
