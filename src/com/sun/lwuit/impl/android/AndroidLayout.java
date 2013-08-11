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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.sun.lwuit.Display;
import com.sun.lwuit.geom.Rectangle;
import java.util.Vector;

/**
 * This layout holds the surface view (AndroidView), all native peers and an invisible
 * EditText (if enabled).
 */
public class AndroidLayout extends RelativeLayout {

    AndroidView androidView;
    int lastDirectionalKeyCode;
    boolean lastDirectionalKeyCodeAvailable;
    private boolean dirty = false;
    private final Vector peers = new Vector();
    private InvisibleTextEdit editText;
    
    
    
    AndroidLayout(Context context, AndroidView androidView) {
        super(context);
        this.androidView = androidView;
        this.setFocusable(false);
        this.setFocusableInTouchMode(false);
        this.setClipChildren(false);
        this.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        
        if (AndroidImplementation.USE_INVISIBLE_TEXT_INPUT_CONNECTION) {
            // add invisible (and most of the time not focusable) text field
            // behind the surface view.
            this.editText = new InvisibleTextEdit(context, androidView);
            RelativeLayout.LayoutParams editTextLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            this.addView(editText, editTextLayoutParams);
        }
        
        RelativeLayout.LayoutParams surfaceLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        this.addView(androidView, surfaceLayoutParams);
        
         
        androidView.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            public void onFocusChange(View view, boolean bln) {
                if (bln && lastDirectionalKeyCodeAvailable) {
                    Display.getInstance().keyPressed(lastDirectionalKeyCode);
                    Display.getInstance().keyReleased(lastDirectionalKeyCode);
                }
            }
        });
        
    }

    public InvisibleTextEdit getInvisibleTextEdit() {
        return editText;
    }
    
    void removePeer(AndroidImplementation.AndroidPeer peer){
        this.removeView(peer.peerWrapper);
        synchronized(peers){
            peers.removeElement(peer);
        }
    }

    void addPeer(AndroidImplementation.AndroidPeer peer) {
        this.addView(peer.peerWrapper, createMyLayoutParams(peer.getAbsoluteX() + peer.getScrollX(), 
                peer.getAbsoluteY() + peer.getScrollY(),
                peer.getWidth(), peer.getHeight()));
        synchronized(peers){
            peers.addElement(peer);
        }
    }
    
    void layoutPeer(AndroidImplementation.AndroidPeer peer){
        RelativeLayout.LayoutParams newLayoutParams = createMyLayoutParams(peer.getAbsoluteX() + peer.getScrollX(), 
                peer.getAbsoluteY() + peer.getScrollY(),
                peer.getWidth(), peer.getHeight());
        RelativeLayout.LayoutParams oldLayoutParams = (RelativeLayout.LayoutParams) peer.peerWrapper.getLayoutParams();
        
        if (newLayoutParams.width != oldLayoutParams.width || newLayoutParams.height != oldLayoutParams.height) {
            // set layout parameters results in layout.
            peer.peerWrapper.setLayoutParams(newLayoutParams);
        } else {
            // only adjusting the offsets is a LOT faster than applying new layout parameters.
    
            // reset to zero
            peer.peerWrapper.offsetLeftAndRight(-peer.peerWrapper.getLeft());
            peer.peerWrapper.offsetTopAndBottom(-peer.peerWrapper.getTop());
            // apply new offset
            peer.peerWrapper.offsetLeftAndRight(newLayoutParams.leftMargin);
            peer.peerWrapper.offsetTopAndBottom(newLayoutParams.topMargin);
            
        }
        
        //peer.v.setLayoutParams(layoutParams);
        //Log.d("LWUIT", "new peer size: " + newLayoutParams.leftMargin + " " + newLayoutParams.rightMargin
          //                    + " " + newLayoutParams.width + " " + newLayoutParams.height);
    }
    

//    public void flushGraphics(Rect rect) {
//        dirty = true;
//        this.postInvalidate();
//        long start = System.currentTimeMillis();
//        while (dirty && (System.currentTimeMillis() - start) < 150) {
//            Thread.yield();
//        }
//    }

//    public void flushGraphics() {
//        dirty = true;
//        this.postInvalidate();
//        long start = System.currentTimeMillis();
//        while (dirty && (System.currentTimeMillis() - start) < 150) {
//            Thread.yield();
//        }
//    }

    /**
     * create a layout parameter object that holds the native component's position.
     * @return
     */
    RelativeLayout.LayoutParams createMyLayoutParams(int x, int y, int width, int height) {
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
    public boolean dispatchTrackballEvent(MotionEvent event) {
        return super.dispatchTrackballEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        lastDirectionalKeyCodeAvailable = false;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public View focusSearch(View focused, int direction) {
        if (focused != androidView
                && (androidView.isFocusable() || androidView.isFocusableInTouchMode())) {
            // try to keep native focus on the base surface. focus traversal
            return androidView;
        }
        if (focused != editText && editText != null
                && (editText.isFocusable() || editText.isFocusableInTouchMode())) {
            // if we use the invisible edit field and if it is focusable it
            // can grab focus, too.
            return editText;
        }
        // now fall back to default focus handling.
        return super.focusSearch(focused, direction);
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                lastDirectionalKeyCode = AndroidView.internalKeyCodeTranslate(event.getKeyCode());
                lastDirectionalKeyCodeAvailable = true;
                break;
            default:
                lastDirectionalKeyCodeAvailable = false;
                break;
        }
        return super.dispatchKeyEvent(event);
    }
}
