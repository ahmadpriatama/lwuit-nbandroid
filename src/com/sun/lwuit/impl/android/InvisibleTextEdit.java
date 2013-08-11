package com.sun.lwuit.impl.android;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import com.sun.lwuit.Command;
import com.sun.lwuit.Component;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.animations.Animation;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.FocusListener;

/**
 * A native text field that is hidden behind the surface view and only used
 * as an InputConnection between a lwuit TextField and the IME. 
 * 
 * If this field looses focus it 'cleans up' it's connection to the lwuit
 * text field automatically. 
 */
public class InvisibleTextEdit extends EditText implements FocusListener {

    private TextField currentLWUITText;
    private AndroidView androidView;

    public InvisibleTextEdit(Context context, AndroidView androidView) {
        super(context);
        this.androidView = androidView;
        setFocusable(false);
        setFocusableInTouchMode(false);
        watchEditText();
    }

    public void focusGained(Component cmp) {
    }

    public void focusLost(Component cmp) {
        // remove self.
        cmp.removeFocusListener(this);
        // LWUIT TextField lost focus. cleanup
        // the native component states as well.
        this.post(new Runnable() {

            public void run() {
                endInvisibleEdit();
                androidView.requestFocus();
            }
        });
    }

    void startInvisibleEdit(TextField ta) {
        currentLWUITText = ta;
        ta.addFocusListener(this);
        setImeOptions(configureEditorInfoIMEOptions(ta));
        setInputType(configureEditorInfoInputTypes(ta.getConstraint()));
        setFocusable(true);
        setFocusableInTouchMode(true);
        androidView.setFocusable(false);
        androidView.setFocusableInTouchMode(false);
        requestFocus();
    }

    private void endInvisibleEdit() {
        Log.d("LWUIT", "endInvisibleEdit ");
        // automatically give up focusability once we loose focus.
        currentLWUITText = null;
        setText("");
        androidView.setFocusable(true);
        androidView.setFocusableInTouchMode(true);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        Log.d("LWUIT", "text focus " + (focused ? "gain" : "loss"));
        if (focused) {
            if (currentLWUITText != null) {
                setText(currentLWUITText.getText());
                setSelection(currentLWUITText.getCursorPosition());
                watchLWUITCursor();
            }
        } else {
            if (currentLWUITText != null) {
                this.endInvisibleEdit();
            }
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        int code = AndroidView.internalKeyCodeTranslate(keyCode);
        switch (code) {
            // if we have focus we intercept left/right/up/down
            // keys and pass them to LWUIT. the listener in
            // this class will then track the cursor position,
            // or we loose focus and clean up in response to the
            // focus event.
            case AndroidImplementation.DROID_IMPL_KEY_LEFT:
            case AndroidImplementation.DROID_IMPL_KEY_RIGHT:
            case AndroidImplementation.DROID_IMPL_KEY_UP:
            case AndroidImplementation.DROID_IMPL_KEY_DOWN:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    Display.getInstance().keyPressed(code);
                    Display.getInstance().keyReleased(code);
                } else {
                    // ignore. 
                }
                return true;
            default:
                return super.onKeyPreIme(keyCode, event);
        }
    }

    void watchEditText() {
        addTextChangedListener(new TextWatcher() {

            public void beforeTextChanged(CharSequence cs, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence cs, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable edtbl) {
                final TextField currentLWUITText = InvisibleTextEdit.this.currentLWUITText;
                if (currentLWUITText != null) {
                    final String lwuitText = currentLWUITText.getText();
                    final String androidText = InvisibleTextEdit.this.getText().toString();
                    final int cursor = InvisibleTextEdit.this.getSelectionEnd();
                    if (!lwuitText.equals(androidText)) {
                        Display.getInstance().callSerially(new Runnable() {

                            public void run() {
                                currentLWUITText.setText(androidText);
                                currentLWUITText.setCursorPosition(cursor);
                            }
                        });
                    }
                }
            }
        });
    }

    void watchLWUITCursor() {

        Log.d("LWUIT", "watch lwuit cursor");

        // register animation on EDT once. here we monitor the cursor position of the
        // lwuit textfield and apply it to the android editable if required.
        Display.getInstance().callSerially(new Runnable() {

            public void run() {
                final TextField t = currentLWUITText;
                if (t == null) {
                    return;
                }
                final Form current = t.getComponentForm();
                if (current != null) {
                    current.registerAnimated(new Animation() {

                        int lwuitCursorLastChecked = t.getCursorPosition();

                        public boolean animate() {
                            TextField ta = currentLWUITText;
                            boolean active = ta != null
                                    && Display.getInstance().getDefaultVirtualKeyboard() != null
                                    && Display.getInstance().getDefaultVirtualKeyboard().isVirtualKeyboardShowing();

                            if (active) {
                                // request LWUIT cursor position on EDT
                                final int currentCursor = ta.getCursorPosition();
                                if (currentCursor != lwuitCursorLastChecked) {
                                    lwuitCursorLastChecked = currentCursor;
                                    // change android cursor on android UI thread.
                                    post(new Runnable() {

                                        public void run() {
                                            // set cursor, but force into range to avoid any exceptions.
                                            int c = currentCursor;
                                            c = Math.max(0, c);
                                            c = Math.min(InvisibleTextEdit.this.length(), c);
                                            InvisibleTextEdit.this.setSelection(c);
                                        }
                                    });
                                }
                                //Log.d("LWUIT", "watching lwuit cursor " + System.currentTimeMillis());
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

    static int configureEditorInfoIMEOptions(TextField textArea) {
        int imeOptions = 0;
        if (textArea != null) {
            if (textArea.isSingleLineTextArea()) {
                Command cmd = Display.getInstance().getCurrent().getDefaultCommand();
                Component nextFocus = textArea.getNextFocusDown();
                if (nextFocus instanceof TextField) {
                    imeOptions |= EditorInfo.IME_ACTION_NEXT;
                } else if (cmd != null) {
                    imeOptions |= EditorInfo.IME_ACTION_GO;
                } else {
                    imeOptions |= EditorInfo.IME_ACTION_DONE;
                }
            } else {
                imeOptions |= EditorInfo.IME_ACTION_NONE;
            }
        } else {
            imeOptions |= EditorInfo.IME_ACTION_NONE;
            imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
            if (android.os.Build.VERSION.SDK_INT >= 11 /*android 3.0*/) {
                imeOptions |= 0x02000000; //EditorInfo.IME_FLAG_NO_FULLSCREEN;
            }
            // todo: allow for constraints if the focus is not on a textarea and
            // if the virtual keyboard interface supports constraint preferences.
            // this requires a change of LWUIT, though.
            // http://java.net/jira/browse/LWUIT-438
        }
        return imeOptions;
    }

    static int configureEditorInfoInputTypes(int constraints) {
        int inputType = 0;
        if ((constraints & TextArea.NUMERIC) != 0) {
            inputType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_SIGNED;
        } else if ((constraints & TextArea.DECIMAL) != 0) {
            inputType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_DECIMAL;
        } else if ((constraints & TextArea.PASSWORD) != 0) {
            inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;
        } else if ((constraints & TextArea.PHONENUMBER) != 0) {
            inputType = EditorInfo.TYPE_CLASS_PHONE;
        } else if ((constraints & TextArea.EMAILADDR) != 0) {
            inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
        } else if ((constraints & TextArea.URL) != 0) {
            inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_URI;
        } else {
            inputType = EditorInfo.TYPE_CLASS_TEXT;
        }
        if ((!AndroidImplementation.USE_LWUIT_INPUT_CONNECTION
                && !AndroidImplementation.USE_INVISIBLE_TEXT_INPUT_CONNECTION)
                || (constraints & TextArea.NON_PREDICTIVE) != 0
                || (constraints & TextArea.SENSITIVE) != 0) {

            // these settings attempt to disable any text prediction and
            // simplify the InputConnection a lot.

            /**
             * for some reason TYPE_TEXT_FLAG_NO_SUGGESTIONS does not seem to work
             * for me, hence the use of TYPE_TEXT_VARIATION_VISIBLE_PASSWORD.
             */
            //inputType |= EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS; // minSdkVersion 5 (2.0)
            inputType |= EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        }
        return inputType;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new InputConnectionWrapper(super.onCreateInputConnection(outAttrs), false) {

            @Override
            public boolean performEditorAction(int editorAction) {
                if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
                    Component txtCmp = Display.getInstance().getCurrent().getFocused();
                    if (txtCmp != null && txtCmp instanceof TextField) {
                        if (editorAction == EditorInfo.IME_ACTION_GO
                                || editorAction == EditorInfo.IME_ACTION_DONE) {
                            Display.getInstance().setShowVirtualKeyboard(false);
                            Command cmd = Display.getInstance().getCurrent().getDefaultCommand();
                            if (cmd != null) {
                                Display.getInstance().getCurrent().dispatchCommand(cmd, new ActionEvent(cmd, null, 0, 0));
                                return true;
                            } else {
                                return false;
                            }
                        } else if (editorAction == EditorInfo.IME_ACTION_NEXT) {
                            Component next = txtCmp.getNextFocusDown();
                            if (next != null) {
                                next.requestFocus();
                            }
                            Display.getInstance().setShowVirtualKeyboard(true);
                            return true;
                        }
                    }
                }


                return super.performEditorAction(editorAction);
            }
        };
    }
}
