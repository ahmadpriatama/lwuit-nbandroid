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

import android.view.KeyEvent;
import android.view.MotionEvent;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.geom.Dimension;
import java.io.IOException;
import java.io.InputStream;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.text.ClipboardManager;
import android.text.InputType;
import android.text.TextPaint;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TextKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.sun.lwuit.Command;

import com.sun.lwuit.Component;
import com.sun.lwuit.Display;
import com.sun.lwuit.Font;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.PeerComponent;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.VideoComponent;
import com.sun.lwuit.geom.Rectangle;
import com.sun.lwuit.impl.LWUITImplementation;
import com.sun.lwuit.impl.VirtualKeyboardInterface;
import com.sun.lwuit.plaf.UIManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Vector;

public class AndroidImplementation extends LWUITImplementation  {

    /**
     * see description of LWUITInputConnection.java
     */
    public static boolean USE_LWUIT_INPUT_CONNECTION = false;
    
    /**
     * use a native text field that hides itself behind the surface
     * view.
     */
    public static boolean USE_INVISIBLE_TEXT_INPUT_CONNECTION = true;
    
    /**
     * make sure these important keys have a negative value when passed
     * to LWUIT or they might be interpreted as characters.
     */
    public static final int DROID_IMPL_KEY_LEFT = -23456;
    public static final int DROID_IMPL_KEY_RIGHT = -23457;
    public static final int DROID_IMPL_KEY_UP = -23458;
    public static final int DROID_IMPL_KEY_DOWN = -23459;
    public static final int DROID_IMPL_KEY_FIRE = -23450;
    public static final int DROID_IMPL_KEY_MENU = -23451;
    public static final int DROID_IMPL_KEY_BACK = -23452;
    public static final int DROID_IMPL_KEY_BACKSPACE = -23453;
    public static final int DROID_IMPL_KEY_CLEAR = -23454;
    public static final int DROID_IMPL_KEY_SYMBOL = -23455;
    static int[] leftSK = new int[]{DROID_IMPL_KEY_MENU};
    private AndroidView myView = null;
    private AndroidFont defaultFont;
    private final char[] tmpchar = new char[1];
    private final RectF tmprectF = new RectF();
    private final Rect tmprect = new Rect();
    private final Path tmppath = new Path();
    private boolean keyboardShowing = false;
    private boolean editing = false;
    private int lastSizeChangeW = -1;
    private int lastSizeChangeH = -1;
    private final char[] tmpDrawChar = new char[1];
    private Vibrator v = null;
    private boolean vibrateInitialized = false;
    private Activity activity;
    private AndroidLayout androidLayout;
    private int lastX, lastY;
    //private int skipped;
    
    @Override
    public void init(Object m) {
        if (m instanceof Activity) {
            this.activity = (Activity) m;
        } else {
            throw new IllegalArgumentException("pass your activity instance to the Display.init(..) method.");
        }

        this.defaultFont = (AndroidFont) this.createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        Display.getInstance().setTransitionYield(-1);
        this.initSurface();
        /**
         * devices are extemely sensitive so dragging should start
         * a little later than suggested by default implementation.
         */
        this.setDragStartPercentage(6);
        VirtualKeyboardInterface vkb = new AndroidKeyboard(activity, myView);
        Display.getInstance().registerVirtualKeyboard(vkb);
        Display.getInstance().setDefaultVirtualKeyboard(vkb);
    }

    public int translatePixelForDPI(int pixel) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, pixel,
                activity.getResources().getDisplayMetrics());
    }

    // @Override
    public int getDeviceDensity() {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        switch (metrics.densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                return Display.DENSITY_LOW;
            case DisplayMetrics.DENSITY_HIGH:
                return Display.DENSITY_HIGH;
            default:
                return Display.DENSITY_MEDIUM;
        }
    }

    @Override
    public void fillLinearGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        if (width > 0 && height > 0) {
            final boolean aa = isAntiAliased(graphics);
            if (aa) {
                setAntiAliased(graphics, false);
            }
            GradientDrawable gd = new GradientDrawable(
                    horizontal ? GradientDrawable.Orientation.LEFT_RIGHT : GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xff000000 | startColor, 0xff000000 | endColor});
            gd.setBounds(x, y, x + width, y + height);
            gd.draw(((AndroidGraphics) graphics).getCanvas());
            if (aa) {
                setAntiAliased(graphics, true);
            }
        }
    }

    /**
     * If you run the display initialization for some reason from a thread other
     * than the main thread, then this method will outsource initialization to the
     * UI thread and wait for it to complete.
     * 
     * If you already are on the UI thread then this should run immediately.
     */
    void initSurface() {
        runOnAndroidUIThreadAndWait(new Runnable() {

            /**
             * create views on UI thread!
             */
            @Override
            public void run() {
                myView = new AndroidView(activity,
                        AndroidImplementation.this);
                androidLayout = new AndroidLayout(activity, myView);
                androidLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.FILL_PARENT,
                        RelativeLayout.LayoutParams.FILL_PARENT));
                androidLayout.setFocusable(false);
                myView.setVisibility(View.VISIBLE);
                activity.setContentView(androidLayout);
                myView.requestFocus();
            }
        });
    }

    @Override
    public void confirmControlView() {
        activity.runOnUiThread(new Runnable() {

            public void run() {
                myView.setVisibility(View.VISIBLE);
            }
        });
    }

    AndroidLayout getAndroidLayout() {
        return androidLayout;
    }

    protected boolean editInProgress() {
        return this.editing;
    }

    public void hideNotifyPublic() {
        super.hideNotify();
    }

    public void showNotifyPublic() {
        super.showNotify();
    }

    @Override
    public boolean isMinimized() {
        return myView == null || myView.getVisibility() != View.VISIBLE;
    }

    @Override
    public boolean minimizeApplication() {
        activity.runOnUiThread(new Runnable() {

            public void run() {
                myView.setVisibility(View.INVISIBLE);
            }
        });
        return true;
    }

    @Override
    public void restoreMinimizedApplication() {
        activity.runOnUiThread(new Runnable() {

            public void run() {
                myView.setVisibility(View.VISIBLE);
            }
        });
    }

    //@Override
    public void editString(Component cmp, int maxSize, int constraint, String text) {
        editString(cmp, maxSize, constraint, text, 0);
    }

    
    //@Override
    public void editString(final Component cmp, int maxSize, final int constraint,
            final String text, int initiatingKeycode) {

        /**
         * softkeyboard should open on a short click on the native text area.
         */
        final boolean[] editingComplete = new boolean[]{false};
        this.editing = true;

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                UIManager m = UIManager.getInstance();
                final FrameLayout frameLayout = new FrameLayout(
                        activity);
                final EditText editText = new EditText(
                        activity);
                editText.setGravity(Gravity.CENTER | Gravity.FILL_HORIZONTAL);
                editText.setSingleLine(true);

                switch (constraint) {
                    case TextArea.NUMERIC:
                        editText.setKeyListener(DigitsKeyListener.getInstance(true, false));
                        break;
                    case TextArea.DECIMAL:
                        editText.setKeyListener(DigitsKeyListener.getInstance(true, true));
                        break;
                    case TextArea.PASSWORD:
                        // turns out the setKeyListener(..) and setTransformationMethod(..) methods alone
                        // would not disable the input hints, causing the device to attempt to save passwords.
                        // not good!  using setInputType(..) works.
                        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        //editText.setKeyListener(TextKeyListener.getInstance(false, TextKeyListener.Capitalize.NONE));
                        //editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        break;
                    case TextArea.INITIAL_CAPS_WORD:
                        editText.setKeyListener(TextKeyListener.getInstance(false, TextKeyListener.Capitalize.WORDS));
                        break;
                    case TextArea.INITIAL_CAPS_SENTENCE:
                        editText.setKeyListener(TextKeyListener.getInstance(false, TextKeyListener.Capitalize.SENTENCES));
                        break;
                    default:
                        editText.setKeyListener(TextKeyListener.getInstance(false, TextKeyListener.Capitalize.NONE));
                        break;
                }

                frameLayout.addView(editText, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.FILL_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));
                editText.setText(text == null ? "" : text);
                AlertDialog.Builder bob = new AlertDialog.Builder(
                        activity);
                frameLayout.setMinimumWidth(Math.min(
                        AndroidImplementation.this.translatePixelForDPI(250),
                        Math.min(AndroidImplementation.this.getDisplayHeight(),
                        AndroidImplementation.this.getDisplayWidth())));
                bob.setView(frameLayout);
                bob.setTitle("");
                bob.setPositiveButton(m.localize("ok", "OK"),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface d, int which) {
                                Display.getInstance().onEditingComplete(cmp,
                                        editText.getText().toString());
                                d.dismiss();
                            }
                        });
                bob.setNegativeButton(m.localize("cancel", "Cancel"),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface d, int which) {
                                d.dismiss();
                            }
                        });
                AlertDialog editDialog = bob.create();
                editDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    public void onDismiss(DialogInterface arg0) {
                        synchronized (editingComplete) {
                            editingComplete[0] = true;
                            editingComplete.notify();
                        }
                    }
                });
                editDialog.show();
            }
        });

        synchronized (editingComplete) {
            if (!editingComplete[0]) {
                try {
                    editingComplete.wait();
                } catch (Exception ignored) {
                    ;
                }
            }
        }

        this.editing = false;

        if (this.lastSizeChangeH != -1 && this.lastSizeChangeW != -1) {
            this.myView.handleSizeChange(this.lastSizeChangeW, this.lastSizeChangeH);
        }
        this.lastSizeChangeH = -1;
        this.lastSizeChangeW = -1;
    }

    protected void setLastSizeChangedWH(int w, int h) {
        this.lastSizeChangeW = w;
        this.lastSizeChangeH = h;
    }

    @Override
    public boolean handleEDTException(final Throwable err) {

        final boolean[] messageComplete = new boolean[]{false};

        Log.e("LWUIT", "Err on EDT", err);

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                UIManager m = UIManager.getInstance();
                final FrameLayout frameLayout = new FrameLayout(
                        activity);
                final TextView textView = new TextView(
                        activity);
                textView.setGravity(Gravity.CENTER);
                frameLayout.addView(textView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.FILL_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));
                textView.setText("An internal application error occurred: " + err.toString());
                AlertDialog.Builder bob = new AlertDialog.Builder(
                        activity);
                bob.setView(frameLayout);
                bob.setTitle("");
                bob.setPositiveButton(m.localize("ok", "OK"),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface d, int which) {
                                d.dismiss();
                                synchronized (messageComplete) {
                                    messageComplete[0] = true;
                                    messageComplete.notify();
                                }
                            }
                        });
                AlertDialog editDialog = bob.create();
                editDialog.show();
            }
        });

        synchronized (messageComplete) {
            if (messageComplete[0]) {
                return true;
            }
            try {
                messageComplete.wait();
            } catch (Exception ignored) {
                ;
            }
        }
        return true;
    }

    @Override
    public int getDragPathTime(){
        // return a value slightly higher than the default.
        return 250;
    }

    public float getDragSpeed(float[] points, long[] dragPathTime,
            int dragPathOffset, int dragPathLength) {

        // will remove this method in the future once the lwuit fix is in place
        // for a while.
        // http://java.net/jira/browse/LWUIT-452

        float speed = super.getDragSpeed(points, dragPathTime, dragPathOffset, dragPathLength);
        Log.d("LWUIT", "drag speed: " + speed);
        final float MAX = 1.3f;
        if (speed < -MAX) {
            return -MAX;
        } else if (speed > MAX) {
            return MAX;
        } else {
            return speed;
        }
    }


    //@Override
    public InputStream getResourceAsStream(Class cls, String resource) {
        try {
            if (resource.startsWith("/")) {
                resource = resource.substring(1);
            }
            return activity.getAssets().open(resource);
        } catch (IOException ex) {
            Log.e("LWUIT", "Failed to load resource: " + resource, ex);
            return null;
        }
    }

    @Override
    protected void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        lastX = x;
        lastY = y;
    }

    @Override
    protected void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
    }

    @Override
    protected void pointerDragged(int x, int y) {
        int diffX = Math.abs(lastX - x);
        int diffY = Math.abs(lastY - y);
        if (diffX <= 2 && diffY <= 2) {
            //
            // some device display fire a huge amount
            // of drag events. skip the small ones here. 
            //
            //   Log.d("LWUIT", "skipped: " + skipped++);
            return;
        }
        // Log.d("LWUIT", "drag diff: " + diffX + "," + diffY); 
        super.pointerDragged(x, y);
        lastX = x;
        lastY = y;
    }

    @Override
    protected int getDragAutoActivationThreshold() {
        return 1000000;
    }

    @Override
    public void flushGraphics() {
        if (myView != null) {
            myView.flushGraphics(androidLayout);
        }
        //androidLayout.flushGraphics();
    }

    @Override
    public void flushGraphics(int x, int y, int width, int height) {
        this.tmprect.set(x, y, x + width, y + height);
        if (myView != null) {
            myView.flushGraphics(this.tmprect, androidLayout);
        }
        //androidLayout.flushGraphics(this.tmprect);
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        this.tmpchar[0] = ch;
        float w = (nativeFont == null ? ((AndroidFont) this.defaultFont).textPaint
                : ((AndroidFont) nativeFont).textPaint).measureText(this.tmpchar, 0, 1);
        if (w - (int) w > 0) {
            return (int) (w + 1);
        }
        return (int) w;
    }

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        float w = (nativeFont == null ? ((AndroidFont) this.defaultFont).textPaint
                : ((AndroidFont) nativeFont).textPaint).measureText(ch, offset, length);
        if (w - (int) w > 0) {
            return (int) (w + 1);
        }
        return (int) w;
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        float w = (nativeFont == null ? ((AndroidFont) this.defaultFont).textPaint
                : ((AndroidFont) nativeFont).textPaint).measureText(str);
        if (w - (int) w > 0) {
            return (int) (w + 1);
        }
        return (int) w;
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
        if (font == null) {
            font = this.defaultFont;
        }
        ((AndroidGraphics) graphics).setFont(((AndroidFont) font));
    }

    @Override
    public int getHeight(Object nativeFont) {
        return ((AndroidFont)(nativeFont == null ? this.defaultFont : nativeFont)).getHeight();
    }

    @Override
    public int getSize(Object nativeFont) {
        return ((AndroidFont)(nativeFont == null ? this.defaultFont : nativeFont)).getSize();
    }

    @Override
    public int getStyle(Object nativeFont) {
        return ((AndroidFont)(nativeFont == null ? this.defaultFont : nativeFont)).getStyle();
    }

    @Override
    public Object createFont(int face, int style, int size) {
        return new AndroidFont(this, face, style, size);
    }

    /**
     * Loads a native font based on a lookup for a font name and attributes. Font lookup
     * values can be separated by commas and thus allow fallback if the primary font
     * isn't supported by the platform.
     *
     * @param lookup string describing the font
     * @return the native font object
     */
    public Object loadNativeFont(String lookup) {
        try {
            return new AndroidFont(lookup);
        } catch (Exception e) {
            Log.d("LWUIT", "font lookup fail: " + lookup, e);
            return null;
        }
    }

    /**
     * Indicates whether loading a font by a string is supported by the platform
     *
     * @return true if the platform supports font lookup
     */
    public boolean isLookupFontSupported() {
        return true;
    }

    @Override
    public boolean isAntiAliasedTextSupported() {
        return true;
    }

    @Override
    public void setAntiAliasedText(Object graphics, boolean a) {
        ((AndroidGraphics) graphics).getFont().textPaint.setAntiAlias(a);
    }

    @Override
    public Object getDefaultFont() {
        //TextPaint paint = new TextPaint();
        //paint.set(this.defaultFont);
        //return paint;
        return this.defaultFont;
    }

    @Override
    public Object getNativeGraphics() {
        return this.myView.getGraphics();
    }

    @Override
    public Object getNativeGraphics(Object image) {
        return new AndroidGraphics(this, new Canvas((Bitmap) image));
    }

    @Override
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y,
            int width, int height) {
        ((Bitmap) nativeImage).getPixels(arr, offset, width, x, y, width,
                height);
    }

    @Override
    public Object createImage(String path) throws IOException {
        InputStream in = this.getResourceAsStream(null, path);
        if (in == null) {
            throw new IOException("Resource not found. " + path);
        }
        try {
            return this.createImage(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                    ;
                }
            }
        }
    }

    @Override
    public Object createImage(InputStream i) throws IOException {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        // http://www.droidnova.com/2d-sprite-animation-in-android-addendum,505.html
        opts.inPurgeable = true;
        return BitmapFactory.decodeStream(i, null, opts);
    }

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        // http://www.droidnova.com/2d-sprite-animation-in-android-addendum,505.html
        opts.inPurgeable = true;
        return BitmapFactory.decodeByteArray(bytes, offset, len, opts);
    }

    @Override
    public Object createImage(int[] rgb, int width, int height) {
        return Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
    }

    @Override
    public boolean isAlphaMutableImageSupported() {
        return true;
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        return Bitmap.createScaledBitmap((Bitmap) nativeImage, width, height,
                false);
    }

    @Override
    public Object rotate(Object image, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap((Bitmap) image, 0, 0, ((Bitmap) image).getWidth(), ((Bitmap) image).getHeight(), matrix, true);
    }

    @Override
    public boolean isRotationDrawingSupported() {
        return false;
    }

    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        AndroidGraphics graphics = (AndroidGraphics) this.getNativeGraphics(bitmap);
        graphics.setColor(fillColor);
        //this.setColor(graphics, fillColor);
        this.fillRect(graphics, 0, 0, width, height);
        return bitmap;
    }

    @Override
    public int getImageHeight(Object i) {
        return ((Bitmap) i).getHeight();
    }

    @Override
    public int getImageWidth(Object i) {
        return ((Bitmap) i).getWidth();
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
        ((AndroidGraphics) graphics).getCanvas().drawBitmap((Bitmap) img, x, y, null);
    }

    @Override
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        ((AndroidGraphics) graphics).getPaint().setStyle(Style.STROKE);
        ((AndroidGraphics) graphics).getCanvas().drawLine(x1, y1, x2, y2,
                ((AndroidGraphics) graphics).getPaint());
    }

    @Override
    public boolean isAntiAliasingSupported() {
        return true;
    }

    @Override
    public void setAntiAliased(Object graphics, boolean a) {
        ((AndroidGraphics) graphics).getPaint().setAntiAlias(a);
    }

    @Override
    public void drawPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints <= 1) {
            return;
        }
        this.tmppath.rewind();
        this.tmppath.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            this.tmppath.lineTo(xPoints[i], yPoints[i]);
        }
        ((AndroidGraphics) graphics).getPaint().setStyle(Style.STROKE);
        ((AndroidGraphics) graphics).getCanvas().drawPath(this.tmppath, ((AndroidGraphics) graphics).getPaint());
    }

    @Override
    public void fillPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints <= 1) {
            return;
        }
        this.tmppath.rewind();
        this.tmppath.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            this.tmppath.lineTo(xPoints[i], yPoints[i]);
        }
        ((AndroidGraphics) graphics).getPaint().setStyle(Style.FILL);
        ((AndroidGraphics) graphics).getCanvas().drawPath(this.tmppath, ((AndroidGraphics) graphics).getPaint());
    }

    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x,
            int y, int w, int h, boolean processAlpha) {
        //Bitmap tmp = Bitmap.createBitmap(rgbData, w, h, Bitmap.Config.ARGB_8888);
        //((AndroidGraphics) graphics).drawBitmap(tmp, x, y, null);
        ((AndroidGraphics) graphics).getCanvas().drawBitmap(rgbData, offset, w, x, y, w, h,
                processAlpha, null);
    }

    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        ((AndroidGraphics) graphics).getPaint().setStyle(Style.STROKE);
        ((AndroidGraphics) graphics).getCanvas().drawRect(x, y, x + width, y + height,
                ((AndroidGraphics) graphics).getPaint());
    }

    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width,
            int height, int arcWidth, int arcHeight) {
        ((AndroidGraphics) graphics).getPaint().setStyle(Style.STROKE);
        this.tmprectF.set(x, y, x + width, y + height);
        ((AndroidGraphics) graphics).getCanvas().drawRoundRect(this.tmprectF, arcWidth,
                arcHeight, ((AndroidGraphics) graphics).getPaint());
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
        ((AndroidGraphics) graphics).getCanvas().drawText(str, x, y - 
                ((AndroidGraphics) graphics).getFont().textPaint.getFontMetricsInt().ascent,
                ((AndroidGraphics) graphics).getFont().textPaint);
    }

    
    /**
     * @deprecated
     */
    public void drawChar(Object graphics, char c, int x, int y) {
        tmpDrawChar[0] = c;
        ((AndroidGraphics) graphics).getCanvas().drawText(tmpDrawChar, 0, 1, x, y - 
                ((AndroidGraphics) graphics).getFont().textPaint.getFontMetricsInt().ascent,
                ((AndroidGraphics) graphics).getFont().textPaint);
    }

    /**
     * @deprecated
     */
    public void drawChars(Object graphics, char[] c, int offset, int length, int x, int y) {
        ((AndroidGraphics) graphics).getCanvas().drawText(c, offset, length, x, y - 
                ((AndroidGraphics) graphics).getFont().textPaint.getFontMetricsInt().ascent,
                ((AndroidGraphics) graphics).getFont().textPaint);
    }

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        ((AndroidGraphics) graphics).getPaint().setStyle(Style.STROKE);
        this.tmprectF.set(x, y, x + width, y + height);
        ((AndroidGraphics) graphics).getCanvas().drawArc(this.tmprectF, startAngle,
                arcAngle, false, ((AndroidGraphics) graphics).getPaint());
    }

    @Override
    public void fillArc(Object graphics, int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        ((AndroidGraphics) graphics).getPaint().setStyle(Style.FILL);
        this.tmprectF.set(x, y, x + width, y + height);
        ((AndroidGraphics) graphics).getCanvas().drawArc(this.tmprectF, startAngle,
                arcAngle, false, ((AndroidGraphics) graphics).getPaint());
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int width, int height) {
        ((AndroidGraphics) graphics).getPaint().setStyle(Style.FILL);
        ((AndroidGraphics) graphics).getCanvas().drawRect(x, y, x + width, y + height,
                ((AndroidGraphics) graphics).getPaint());
    }

    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width,
            int height, int arcWidth, int arcHeight) {
        ((AndroidGraphics) graphics).getPaint().setStyle(Style.FILL);
        this.tmprectF.set(x, y, x + width, y + height);
        ((AndroidGraphics) graphics).getCanvas().drawRoundRect(this.tmprectF, arcWidth,
                arcHeight, ((AndroidGraphics) graphics).getPaint());
    }

    @Override
    public int getAlpha(Object graphics) {
        return ((AndroidGraphics) graphics).getPaint().getAlpha();
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        ((AndroidGraphics) graphics).getPaint().setAlpha(alpha);
    }

    @Override
    public boolean isAlphaGlobal() {
        return true;
    }

    @Override
    public void setColor(Object graphics, int RGB) {
        ((AndroidGraphics) graphics).setColor(0xff000000 | RGB);
    }

    @Override
    public int getBackKeyCode() {
        return DROID_IMPL_KEY_BACK;
    }

    @Override
    public int getBackspaceKeyCode() {
        return DROID_IMPL_KEY_BACKSPACE;
    }

    @Override
    public int getClearKeyCode() {
        return DROID_IMPL_KEY_CLEAR;
    }

    @Override
    public int getClipHeight(Object graphics) {
        ((AndroidGraphics) graphics).getCanvas().getClipBounds(this.tmprect);
        return this.tmprect.height();
    }

    @Override
    public int getClipWidth(Object graphics) {
        ((AndroidGraphics) graphics).getCanvas().getClipBounds(this.tmprect);
        return this.tmprect.width();
    }

    @Override
    public int getClipX(Object graphics) {
        ((AndroidGraphics) graphics).getCanvas().getClipBounds(this.tmprect);
        return this.tmprect.left;
    }

    @Override
    public int getClipY(Object graphics) {
        ((AndroidGraphics) graphics).getCanvas().getClipBounds(this.tmprect);
        return this.tmprect.top;
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        ((AndroidGraphics) graphics).getCanvas().clipRect(x, y, x + width, y + height, Region.Op.REPLACE);
    }

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        ((AndroidGraphics) graphics).getCanvas().clipRect(x, y, x + width, y + height);
    }

    @Override
    public int getColor(Object graphics) {
        return ((AndroidGraphics) graphics).getPaint().getColor();
    }

    @Override
    public int getDisplayHeight() {
        return this.myView.getViewHeight();
    }

    @Override
    public int getDisplayWidth() {
        return this.myView.getViewWidth();
    }

    @Override
    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case DROID_IMPL_KEY_DOWN:
                return Display.GAME_DOWN;
            case DROID_IMPL_KEY_UP:
                return Display.GAME_UP;
            case DROID_IMPL_KEY_LEFT:
                return Display.GAME_LEFT;
            case DROID_IMPL_KEY_RIGHT:
                return Display.GAME_RIGHT;
            case DROID_IMPL_KEY_FIRE:
                return Display.GAME_FIRE;
            default:
                return 0;
        }
    }

    @Override
    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case Display.GAME_DOWN:
                return DROID_IMPL_KEY_DOWN;
            case Display.GAME_UP:
                return DROID_IMPL_KEY_UP;
            case Display.GAME_LEFT:
                return DROID_IMPL_KEY_LEFT;
            case Display.GAME_RIGHT:
                return DROID_IMPL_KEY_RIGHT;
            case Display.GAME_FIRE:
                return DROID_IMPL_KEY_FIRE;
            default:
                return 0;
        }
    }

    @Override
    public int[] getSoftkeyCode(int index) {
        if (index == 0) {
            return leftSK;
        }
        return null;
    }

    @Override
    public int getSoftkeyCount() {
        /**
         * one menu button only.  we may have to stuff some code here
         * as soon as there are devices that no longer have only a single
         * menu button.
         */
        return 1;
    }

    @Override
    public void vibrate(int duration) {
        if (!this.vibrateInitialized) {
            try {
                v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            } catch (Throwable e) {
                Log.e("LWUIT", "problem with virbrator(0)", e);
            } finally {
                this.vibrateInitialized = true;
            }
        }
        if (v != null) {
            try {
                v.vibrate(duration);
            } catch (Throwable e) {
                Log.e("LWUIT", "problem with virbrator(1)", e);
            }
        }
    }

    @Override
    public boolean isTouchDevice() {
        Configuration c = myView.getResources().getConfiguration();
        return c.touchscreen != Configuration.TOUCHSCREEN_NOTOUCH;
    }

    @Override
    public boolean hasPendingPaints() {
        //if the view is not visible make sure the edt won't wait.
        if (myView.getVisibility() != View.VISIBLE) {
            return true;
        } else {
            return super.hasPendingPaints();
        }
    }

    public void revalidate() {
        myView.setVisibility(View.VISIBLE);
        getCurrentForm().revalidate();
        flushGraphics();
    }

    //@Override
    public boolean isPortrait() {
        int orientation = activity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_UNDEFINED
                || orientation == Configuration.ORIENTATION_SQUARE) {
            return super.isPortrait();
        }
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }
    
    //@Override
    public boolean canForceOrientation() {
        return true;
    }
    
    //@Override
    public void lockOrientation(final boolean portrait) {
        this.runOnAndroidUIThreadAndWait(new Runnable() {

            public void run() {
                activity.setRequestedOrientation(portrait
                        ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        });
    }

    //@Override
    public void copyToClipboard(final Object obj) {
        // at some point move to
        // http://developer.android.com/reference/android/content/ClipboardManager.html
        // (api level 11)
        runOnAndroidUIThreadAndWait(new Runnable() {

            public void run() {
                ClipboardManager cl = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
                if (!(obj instanceof String)) {
                    AndroidImplementation.super.copyToClipboard(obj);
                    cl.setText(null);
                } else {
                    AndroidImplementation.super.copyToClipboard(null);
                    cl.setText((String) obj);
                }
            }
        });
    }

    //@Override
    public Object getPasteDataFromClipboard() {
        // at some point move to
        // http://developer.android.com/reference/android/content/ClipboardManager.html
        // (api level 11)
        final Object[] result = new Object[1];
        runOnAndroidUIThreadAndWait(new Runnable() {

            public void run() {
                ClipboardManager cl = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
                if (cl.hasText()) {
                    result[0] = cl.getText().toString();
                } else {
                    result[0] = AndroidImplementation.super.getPasteDataFromClipboard();
                }
            }
        });
        return result[0];
    }

    @Override
    public int getKeyboardType() {
        if (Display.getInstance().getDefaultVirtualKeyboard().isVirtualKeyboardShowing()) {
            return Display.KEYBOARD_TYPE_VIRTUAL;
        }
        /**
         * can we detect this?  but even if we could i think
         * it is best to have this fixed to qwerty.  we pass unicode
         * values to lwuit in any case.  check AndroidView.onKeyUpDown()
         * method.  and read comment below.
         */
        return Display.KEYBOARD_TYPE_QWERTY;
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
    }

    // @Override
    public Object createSoftWeakRef(Object o) {
        return new SoftReference(o);
    }

    // @Override
    public Object extractHardRef(Object o) {
        return o == null ? null : ((SoftReference) o).get();
    }

    /**
     * @deprecated
     */
    public boolean isVirtualKeyboardShowing() {
        return this.keyboardShowing;
    }

    /**
     * @deprecated
     */
    public boolean isVirtualKeyboardShowingSupported() {
        return this.isTouchDevice();
    }

    /**
     * @deprecated
     */
    public void setShowVirtualKeyboard(boolean show) {

        /**
         * to properly handle the virtual keyboard i see no other way than implementing
         * a customized input method service.  crap.
         *
         * http://www.mail-archive.com/android-developers@googlegroups.com/msg37058.html
         * http://groups.google.com/group/android-developers/browse_thread/thread/0da0d2eff53a7b55
         * https://groups.google.com/group/android-developers/browse_thread/thread/e23040e899eb0a0a
         * http://www.go-android.de/externe-news/creating-input-method
         */
        if (this.keyboardShowing != show) {
            // http://android-developers.blogspot.com/2009/04/updating-applications-for-on-screen.html
            InputMethodManager manager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.toggleSoftInputFromWindow(myView.getWindowToken(), 0, 0);
            this.keyboardShowing = show;

            /*
             * suggested alternative from the forums, could not test it yet.
            InputMethodManager manager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (show) {
            manager.showSoftInput(myView, 0);
            } else {
            manager.hideSoftInputFromWindow(myView.getWindowToken(), 0);
            }
            this.keyboardShowing = show;
             *
             */
        }
    }

    /**
     * @deprecated
     */
    protected void clearVirtualKeyboard() {
        this.keyboardShowing = false;
    }

    // @Override
    public void showNativeScreen(Object nativeFullScreenPeer) {
        if (!(nativeFullScreenPeer instanceof View)) {
            throw new IllegalArgumentException(nativeFullScreenPeer.getClass().getName());
        }
        // todo
    }

    /**
     * Places the following commands on the native menu system
     *
     * @param commands the LWUIT commands to use
     */
    // @Override
    public void setNativeCommands(Vector commands) {
        /**
         * I see no way to control this from the view, hence
         * we have to use the activity.
         */
        if (activity instanceof LWUITActivity) {
            ((LWUITActivity) activity).refreshOptionsMenu(commands);
        }
    }

    @Override
    public void notifyCommandBehavior(int commandBehavior) {
        if (commandBehavior == Display.COMMAND_BEHAVIOR_NATIVE) {
            if (!(activity instanceof LWUITActivity)) {
                Log.e("LWUIT", "Activity must extend LWUITActivity to use the native menu feature.");
            }
        }
    }

    /**
     * @inheritDoc
     */
    public String getProperty(String key, String defaultValue) {
        if ("OS".equals(key)) {
            return "Android";
        }
        if ("AppName".equals(key)) {
            return activity.getApplicationInfo().name;
        }
        if ("AppVersion".equals(key)) {
            try {
                PackageInfo i = activity.getPackageManager().getPackageInfo(activity.getApplicationInfo().packageName, 0);
                return i.versionName;
            } catch (NameNotFoundException ex) {
                ex.printStackTrace();
            }
            return null;
        }
        if ("Platform".equals(key)) {
            return System.getProperty("platform");
        }
        if ("User-Agent".equals(key)) {
            String userAgent;
            WebView wv = new WebView(activity);
            userAgent = wv.getSettings().getUserAgentString();
            wv.destroy();
            return userAgent;
        }
        if ("IMEI".equals(key)) {
            TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getDeviceId();
        }
        if ("MSISDN".equals(key)) {
            TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getLine1Number();
        }
        //these keys/values are from the Application Resources (strings values)
        try {
            int id = activity.getResources().getIdentifier(key, "string", activity.getApplicationInfo().packageName);
            String val = activity.getResources().getString(id);
            return val;

        } catch (Exception e) {
        }
        return System.getProperty(key, defaultValue);
    }

    /**
     * @inheritDoc
     */
    public void exitApplication() {
        super.exitApplication();
        System.exit(0);
    }
    
    /**
     * @inheritDoc
     */
    public void execute(String url) {
        try {
            Intent i = new Intent();
            //ComponentName comp = new ComponentName(
            //        "com.google.android.browser",
            //        "com.google.android.browser.BrowserActivity");
            //i.setComponent(comp);
            i.setAction(Intent.ACTION_VIEW);
            i.addCategory(Intent.CATEGORY_BROWSABLE);
            i.setData(Uri.parse(url));
            activity.startActivity(i);
        } catch (Exception e) {
            Log.e("LWUIT", "problem launching URL: " + url, e);
        }
    }

    /**
     * @inheritDoc
     */
    public void playBuiltinSound(String soundIdentifier) {
        if(soundIdentifier != null && Display.SOUND_TYPE_BUTTON_PRESS.equals(soundIdentifier)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    if(myView != null) {
                        myView.playSoundEffect(AudioManager.FX_KEY_CLICK);
                    }
                }
            });
        }
    }

    /**
     * @inheritDoc
     */
    protected void playNativeBuiltinSound(Object data) {
    }

    /**
     * @inheritDoc
     */
    public boolean isBuiltinSoundAvailable(String soundIdentifier) {
        return true;
    }

    /**
     * @inheritDoc
     */
    public Object createAudio(String uri, Runnable onCompletion) throws IOException {
        return new Audio().createAudio(activity, uri, onCompletion);
    }

    /**
     * @inheritDoc
     */
    public Object createAudio(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return new Audio().createAudio(stream, mimeType, onCompletion);
    }

    /**
     * @inheritDoc
     */
    public void cleanupAudio(Object handle) {
        ((Audio)handle).cleanupAudio();
    }

    /**
     * @inheritDoc
     */
    public void playAudio(Object handle) {
        ((Audio)handle).playAudio();
    }

    /**
     * @inheritDoc
     */
    public void pauseAudio(Object handle) {
        ((Audio)handle).pauseAudio();
    }

    /**
     * @inheritDoc
     */
    public int getAudioTime(Object handle) {
        return ((Audio)handle).getAudioTime();

    }

    /**
     * @inheritDoc
     */
    public void setAudioTime(Object handle, int time) {
        ((Audio)handle).setAudioTime(time);
    }

    /**
     * @inheritDoc
     */
    public int getAudioDuration(Object handle) {
        return ((Audio)handle).getAudioDuration();
    }

    /**
     * @inheritDoc
     */
    public void setVolume(Object handle, int vol) {
        ((Audio)handle).setVolume(vol);
    }

    /**
     * @inheritDoc
     */
    public int getVolume(Object handle) {
        return ((Audio)handle).getVolume(activity);
    }

    static class Audio implements Runnable {

        private MediaPlayer player;
        private Runnable onComplete;
        private InputStream stream;
        private int lastTime;
        private int lastDuration;

        public Audio() {
        }

        private void cleanVars() {
            if (player != null) {
                try {
                    player.release();
                } catch (Throwable t) {
                }
                player = null;
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Throwable t) {
                    }
                    stream = null;
                }
                if (onComplete != null) {
                    onComplete.run();
                    onComplete = null;
                }
                System.gc();
            }
        }

        public void run() {
            if (player != null) {
                cleanupAudio();
                cleanVars();
            }
        }

        /**
         * @inheritDoc
         */
        public Object createAudio(Activity activity, String uri, Runnable onComplete) throws IOException {
            this.onComplete = onComplete;
            if (uri.startsWith("file://")) {
                return createAudio(activity, uri.substring(7), onComplete);
            }
            if (uri.indexOf(':') < 0) {
                // use a file object to play to try and workaround this issue:
                // http://code.google.com/p/android/issues/detail?id=4124
                FileInputStream fi = new FileInputStream(uri);
                player = new MediaPlayer();
                player.setDataSource(fi.getFD());
                player.prepare();
                stream = fi;
                bindPlayerCleanupOnComplete();
                return this;
            }
            player = MediaPlayer.create(activity, Uri.parse(uri));
            bindPlayerCleanupOnComplete();
            return this;
        }

        private void bindPlayerCleanupOnComplete() {
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                public void onCompletion(MediaPlayer arg0) {
                    run();
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                    run();
                    return false;
                }
            });
        }

        public Object createAudio(InputStream stream, String mimeType, Runnable onComplete) throws IOException {
            this.onComplete = onComplete;
            if (stream instanceof FileInputStream) {
                this.stream = stream;
                player = new MediaPlayer();
                player.setDataSource(((FileInputStream) stream).getFD());
                player.prepare();
                bindPlayerCleanupOnComplete();
                return this;
            }

            // not the best thing to do but do we really have a choice???
            File tmp = File.createTempFile("tempMedia", "tmp");
            tmp.deleteOnExit();
            FileOutputStream fo = new FileOutputStream(tmp);
            byte[] buffer = new byte[8192];
            int size = stream.read(buffer);
            while (size > -1) {
                fo.write(buffer, 0, size);
                size = stream.read(buffer);
            }
            fo.close();
            FileInputStream fi = new FileInputStream(tmp);
            return createAudio(fi, mimeType, onComplete);
        }

        public void cleanupAudio() {
            try {
                if (player != null) {
                    if (player.isPlaying()) {
                        player.stop();
                    }
                    cleanVars();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public void playAudio() {
            player.start();
        }

        public void pauseAudio() {
            if (player != null) {
                player.pause();
            }
        }

        public int getAudioTime() {
            if (player == null) {
                return lastTime;
            }
            try {
                lastTime = player.getCurrentPosition();
                return lastTime;
            } catch (IllegalStateException err) {
                // no idea???
                //err.printStackTrace();
                return lastTime;
            }
        }

        public void setAudioTime(int time) {
            if (player == null) {
                return;
            }
            final boolean[] flag = new boolean[1];
            player.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {

                public void onSeekComplete(MediaPlayer arg0) {
                    flag[0] = true;
                }
            });
            if (player.isPlaying()) {
                player.seekTo(time);
            } else {
                player.start();
                player.seekTo(time);
                player.pause();
            }
        }

        public int getAudioDuration() {
            if (player == null) {
                return lastDuration;
            }

            lastDuration = player.getDuration();
            return lastDuration;
        }

        public void setVolume(int vol) {
            float v = ((float) vol) / 100.0f;
            player.setVolume(v, v);
        }

        public int getVolume(Activity a) {
            AudioManager am = (AudioManager) a.getSystemService(Context.AUDIO_SERVICE);
            return am.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
    }

    /**
     * Create a video component
     *
     * @param uri the platform specific location for the sound
     * @return a VideoComponent that can be used to control the playback of the
     * video
     * @throws java.io.IOException if the allocation fails
     */
    public VideoComponent createVideoPeer(final String url) throws IOException {
        final Object[] result = new Object[1];
        runOnAndroidUIThreadAndWait(new Runnable() {

            public void run() {
                try {
                    result[0] = new AndroidVideo(url, myView);
                } catch (Exception e) {
                    result[0] = e.getMessage();
                    Log.e("LWUIT", "problem creating video peer.", e);
                }
            }
        });
        if (result[0] instanceof String) {
            throw new IOException("Failed to create Video: " + (String) result[0]);
        }
        return (VideoComponent) result[0];
    }


    // @Override
    public PeerComponent createNativePeer(Object nativeComponent) {
        if (!(nativeComponent instanceof View)) {
            throw new IllegalArgumentException(nativeComponent.getClass().getName());
        }
        return new AndroidPeer((View) nativeComponent);
    }

    /**
     * inner class that wraps the native peers.
     * this is a useful thingy to handle a paint buffer
     * that can then be painted along with the normal
     * flushGraphics() calls.  any other aproach that
     * i tested looks bad because the native peers will
     * 'lag' a little behind when scrolling.
     * 
     * only surface view peers (videoview) will not
     * be painted to an internal buffer.
     * 
     */
    class PeerWrapper extends RelativeLayout {

        private AndroidPeer peer;
        private Bitmap nativeBuffer;
        private Rect bounds;
        private Canvas canvas;
        private Image image;
        private Paint clear = new Paint();

        public PeerWrapper(Context activity, AndroidPeer peer) {
            super(activity);

            clear.setColor(0x00000000);
            clear.setStyle(Style.FILL);

            this.peer = peer;
            this.setLayoutParams(createMyLayoutParams(peer.getAbsoluteX(), peer.getAbsoluteY(),
                    peer.getWidth(), peer.getHeight()));
            this.addView(peer.v, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT));
            this.setDrawingCacheEnabled(false);
            this.setAlwaysDrawnWithCacheEnabled(false);
            this.setFocusable(false);
            this.setFocusableInTouchMode(false);
            this.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        }

        /**
         * create a layout parameter object that holds the native component's position.
         * @return
         */
        private RelativeLayout.LayoutParams createMyLayoutParams(int x, int y, int width, int height) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.leftMargin = x;
            layoutParams.topMargin = y;
            return layoutParams;
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {

            if(child instanceof SurfaceView){
                // don't cache surface views.
                return super.drawChild(canvas, child, drawingTime);
            }

            Canvas c = getBuffer();

            boolean result;
            synchronized(c){

                /**
                 * the EDT might draw the cache bitmap from within the flushGraphics()
                 * methods. synchronizing here to avoid half painted bitmaps or whatever
                 * might happen in the background if the EDT is reading and we are drawing.
                 */
                result = super.drawChild(c, child, drawingTime);
            }

            /**
             * now that this native component has been painted we certainly need a repaint.
             * notify the EDT.
             */
            peer.repaint();

            return result;
        }

        private Canvas getBuffer(){
            if (nativeBuffer == null || getWidth() != nativeBuffer.getWidth()
                    || getHeight() != nativeBuffer.getHeight()) {
                this.nativeBuffer = Bitmap.createBitmap(
                        getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                image = new NativeImage(nativeBuffer);
                bounds = new Rect(0, 0, getWidth(), getHeight());
                canvas = new Canvas(nativeBuffer);
            }
            //clear the canvas
            canvas.drawRect(bounds, clear);
            return canvas;
        }

        void edtPaint(Graphics g) {
            if (canvas != null && !(peer.v instanceof SurfaceView)) {
                //copy the native drawing to a different image
                synchronized (canvas) {
                    g.drawImage(image, peer.getX(), peer.getY());
                }
            }
        }

        public void release(){
            deinitialize();
        }

        class NativeImage extends Image {
            public NativeImage(Bitmap nativeImage) {
                super(nativeImage);
            }
        }
    }

    /**
     * wrapper component that capsules a native view object in a LWUIT component. this
     * involves A LOT of back and forth between the LWUIT EDT and the Android UI thread.
     *
     *
     * To use it you would:
     *
     * 1) create your native Android view(s). Make sure to work on the Android UI thread when constructing
     *    and modifying them.
     * 2) create a LWUIT peer component (on EDT) by calling:
     *
     *         com.sun.lwuit.PeerComponent.create(myAndroidView);
     *
     *
     */
    class AndroidPeer extends PeerComponent {

        private final View v;
        final PeerWrapper peerWrapper;

        public AndroidPeer(View v) {
            super(v);
            this.v = v;
            this.peerWrapper = new PeerWrapper(activity, this);
        }

        @Override
        public void setVisible(boolean visible) {
            // EDT
            super.setVisible(visible);
            this.doSetVisibility(visible);
        }

        void doSetVisibility(final boolean visible) {
            runOnAndroidUIThreadAndWait(new Runnable() {

                public void run() {
                    v.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
                    if (visible) {
                        v.bringToFront();
                    }
                }
            });
        }

        @Override
        protected void deinitialize() {
            // EDT
            super.deinitialize();
            runOnAndroidUIThreadAndWait(new Runnable() {

                public void run() {
                    if (v != null) {
                        androidLayout.removePeer(AndroidPeer.this);
                    }
                }
            });
        }

        @Override
        protected void initComponent() {
            // EDT
            super.initComponent();
            runOnAndroidUIThreadAndWait(new Runnable() {

                public void run() {
                    v.setFocusable(AndroidPeer.this.isFocusable());
                    v.setFocusableInTouchMode(AndroidPeer.this.isFocusable());
                    if (v.isFocusable() || v.isFocusableInTouchMode()) {
                        if (AndroidPeer.this.hasFocus()) {
                            if (v.isInTouchMode()) {
                                v.requestFocusFromTouch();
                            } else {
                                v.requestFocus();
                            }
                        }
                    }
                    androidLayout.addPeer(AndroidPeer.this);
                }
            });

            // need to re-calculate size AFTER adding view to layout.
            // unless there is no parent form, yet. then we could run into
            // a nullpointer exception within PeerComponent.java. 
            if (this.getComponentForm() != null) {
                this.invalidate();
            }
        }

        @Override
        protected void onPositionSizeChange() {

            // called by LWUIT EDT to position the native component.

            //   Log.d("LWUIT", "on position size change");

            runOnAndroidUIThreadAndWait(new Runnable() {

                public void run() {
                    if (v.getVisibility() == View.VISIBLE) {
                        androidLayout.layoutPeer(AndroidPeer.this);
                    }
                }
            });
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            // the native peer's drawing has been cached and is now
            // painted on the lwuit bitmap. 
            this.peerWrapper.edtPaint(g);
        }

        Rectangle clipOnLWUITBounds() {
            Rectangle lwuitBounds = new Rectangle();
            lwuitBounds.setX(this.getAbsoluteX() + this.getScrollX());
            lwuitBounds.setY(this.getAbsoluteY() + this.getScrollY());
            lwuitBounds.getSize().setWidth(this.getWidth());
            lwuitBounds.getSize().setHeight(this.getHeight());
            Component parent = this;
            while (parent != null) {
                lwuitBounds = lwuitBounds.intersection(parent.getAbsoluteX() + parent.getScrollX(),
                        parent.getAbsoluteY() + parent.getScrollY(), parent.getWidth(), parent.getHeight());
                parent = parent.getParent();
            }
            lwuitBounds.getSize().setWidth(Math.max(0, lwuitBounds.getSize().getWidth()));
            lwuitBounds.getSize().setHeight(Math.max(0, lwuitBounds.getSize().getHeight()));
            return lwuitBounds;
        }

        @Override
        protected Dimension calcPreferredSize() {
            final Dimension d = new Dimension();
            runOnAndroidUIThreadAndWait(new Runnable(){

                public void run() {
                    d.setWidth(v.getMeasuredWidth());
                    d.setHeight(v.getMeasuredHeight());
                }
            });
            return d;
        }

        @Override
        public boolean isFocusable() {
            // EDT
            if (v != null) {
                return v.isFocusableInTouchMode() || v.isFocusable();
            } else {
                return super.isFocusable();
            }
        }

        @Override
        public void setFocusable(final boolean focusable) {
            // EDT
            super.setFocusable(focusable);
            runOnAndroidUIThreadAndWait(new Runnable() {

                public void run() {
                    v.setFocusable(focusable);
                    v.setFocusableInTouchMode(focusable);
                }
            });
        }

        @Override
        public void setFocus(final boolean focused) {
            // EDT
            if (hasFocus() == focused) {
                return;
            }

            super.setFocus(focused);
            if(!isInitialized()){
                return;
            }

            runOnAndroidUIThreadAndWait(new Runnable() {

                public void run() {
                    View vv = focused ? v : myView;
                    if (vv.isInTouchMode()) {
                        vv.requestFocusFromTouch();
                    } else {
                        vv.requestFocus();
                    }
                }
            });
        }
    }

    private void runOnAndroidUIThreadAndWait(final Runnable r) {
        runOnAndroidUIThreadAndWait(activity, r);
    }

    public static void runOnAndroidUIThreadAndWait(Activity activity, final Runnable r) {

        final boolean[] complete = new boolean[]{false};
        activity.runOnUiThread(new Runnable() {

            public void run() {
                try {
                    r.run();
                } finally {
                    synchronized (complete) {
                        complete[0] = true;
                        complete.notify();
                    }
                }
            }
        });

        synchronized (complete) {
            while (!complete[0]) {
                try {
                    complete.wait();
                } catch (Exception ignored) {
                    ;
                }
            }
        }
    }
 
}
