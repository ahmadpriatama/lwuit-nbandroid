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

import android.net.Uri;
import android.view.SurfaceView;
import android.widget.MediaController;
import android.widget.VideoView;
import com.sun.lwuit.Display;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.PeerComponent;
import com.sun.lwuit.VideoComponent;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.geom.Rectangle;
import com.sun.lwuit.impl.android.AndroidImplementation.AndroidPeer;

/**
 * This class is used to display Video
 */
public class AndroidVideo extends VideoComponent {

    private VideoView video;
    private AndroidImplementation.AndroidPeer nativePeer;
    private boolean fullscreen = false;
    private static boolean nativeController = true;

    public AndroidVideo(String url, SurfaceView lwuitView) {
        super(url);
        video = new VideoView(lwuitView.getContext());
        video.setZOrderMediaOverlay(true);
        video.setVideoURI(Uri.parse(url));
        if (nativeController) {
            MediaController mc = new MediaController(lwuitView.getContext());
            video.setMediaController(mc);
        }
        nativePeer = (AndroidPeer) PeerComponent.create(video);
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        nativePeer.initComponent();
    }

    @Override
    protected void deinitialize() {
        super.deinitialize();
        nativePeer.deinitialize();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        nativePeer.setVisible(visible);
    }

    @Override
    protected void onPositionSizeChange() {
        super.onPositionSizeChange();
        nativePeer.onPositionSizeChange();
    }

    @Override
    public void start() {
        video.start();
    }

    @Override
    public void stop() {
        video.stopPlayback();
    }

    @Override
    public void setLoopCount(int count) {
        //video.getsetLooping(true);
    }

    @Override
    public int getMediaTimeSeconds() {
        return video.getCurrentPosition();
    }

    @Override
    public int setMediaTimeSeconds(int now) {
        video.seekTo(now);
        return getMediaTimeSeconds();
    }

    protected Dimension calcPreferredSize() {
        return nativePeer.calcPreferredSize();
    }

    @Override
    public void setWidth(final int width) {
        super.setWidth(width);
        nativePeer.setWidth(width);
    }

    @Override
    public void setHeight(final int height) {
        super.setHeight(height);
        nativePeer.setHeight(height);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        nativePeer.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        nativePeer.setY(y);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        nativePeer.paint(g);
    }
    private Rectangle bounds;

    @Override
    public void setFullScreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        if (fullscreen) {
            bounds = new Rectangle(getBounds());
            setX(0);
            setY(0);
            setWidth(Display.getInstance().getDisplayWidth());
            setHeight(Display.getInstance().getDisplayHeight());
        } else {
            if (bounds != null) {
                setX(bounds.getX());
                setY(bounds.getY());
                setWidth(bounds.getSize().getWidth());
                setHeight(bounds.getSize().getHeight());
            }
        }
        repaint();
    }

    @Override
    public boolean isFullScreen() {
        return fullscreen;
    }

    /**
     * If true the native Android video controls will be attached to the playing
     * Video
     * This method should be called before the video was created
     * 
     * @param nativeController indicates if to use the native video controls
     */
    public static void setNativeController(boolean nativeController) {
        AndroidVideo.nativeController = nativeController;
    }

    @Override
    public int getMediaTimeMS() {
        return video.getCurrentPosition();
    }

    @Override
    public int setMediaTimeMS(int now) {
        video.seekTo(now);
        return this.getMediaTimeMS();
    }

    @Override
    public int getMediaDuration() {
        return video.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return video.isPlaying();
    }
}
