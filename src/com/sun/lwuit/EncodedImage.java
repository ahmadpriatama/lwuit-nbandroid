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
package com.sun.lwuit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An image that only keeps the binary data of the source file used to load it
 * in permanent memory. This allows the bitmap to get collected while the binary
 * data remains, a weak reference is used for caching.
 *
 * @author Shai Almog
 */
public class EncodedImage extends Image {
    private byte[] imageData;
    private int width = -1;
    private int height = -1;
    private boolean opaqueChecked = false;
    private boolean opaque = false;
    private Object cache;
    private Image hardCache;
    private boolean locked;
    
    private EncodedImage(byte[] imageData) {
        super(null);
        this.imageData = imageData;
    }

    /**
     * Allows subclasses to create more advanced variations of this class that
     * lazily store the data in an arbitrary location.
     *
     * @param width -1 if unknown ideally the width/height should be known in advance
     * @param height  -1 if unknown ideally the width/height should be known in advance
     */
    protected EncodedImage(int width, int height) {
        super(null);
        this.width = width;
        this.height = height;
    }

    /**
     * A subclass might choose to load asynchroniously and reset the cache when the image is ready.
     */
    protected void resetCache() {
        cache = null;
    }

    /**
     * Returns the byte array data backing the image allowing the image to be stored
     * and discarded completely from RAM.
     * 
     * @return byte array used to create the image, e.g. encoded PNG, JPEG etc.
     */
    public byte[] getImageData() {
        return imageData;
    }

    /**
     * Creates an image from the given byte array
     * 
     * @param data the data of the image
     * @return newly created encoded image
     */
    public static EncodedImage create(byte[] data) {
        if(data == null) {
            throw new NullPointerException();
        }
        return new EncodedImage(data);
    }

    /**
     * Creates an image from the input stream 
     * 
     * @param i the input stream
     * @return newly created encoded image
     * @throws java.io.IOException if thrown by the input stream
     */
    public static EncodedImage create(InputStream i) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int size = i.read(buffer);
        while(size > -1) {
            bo.write(buffer, 0, size);
            size = i.read(buffer);
        }
        bo.close();
        return new EncodedImage(bo.toByteArray());
    }

    private Image getInternalImpl() {
        if(hardCache != null) {
            return hardCache;
        }
        Image i = getInternal();
        if(locked) {
            hardCache = i;
        }
        return i;
    }

    /**
     * Returns the actual image represented by the encoded image, this image will
     * be cached in a weak/soft reference internally. This method is useful to detect
     * when the system actually created an image instance. You shouldn't invoke this
     * method manually!
     *
     * @return drawable image instance
     */
    protected Image getInternal() {
        if(cache != null) {
            Image i = (Image)Display.getInstance().extractHardRef(cache);
            if(i != null) {
                return i;
            }
        }
        Image i;
        try {
            byte[] b = getImageData();
            i = Image.createImage(b, 0, b.length);
        } catch(Exception err) {
            err.printStackTrace();
            i = Image.createImage(5, 5);
        }
        cache = Display.getInstance().createSoftWeakRef(i);
        return i;
    }

    /**
     * @inheritDoc
     */
    public void lock() {
        locked = true;
        if(cache != null) {
            hardCache = (Image)Display.getInstance().extractHardRef(cache);
        }
    }

    /**
     * @inheritDoc
     */
    public void unlock() {
        if(hardCache != null) {
            if(cache == null || Display.getInstance().extractHardRef(cache) == null) {
                cache = Display.getInstance().createSoftWeakRef(hardCache);
            }
        }
        locked = false;
    }

    /**
     * Creates an image from the input stream 
     * 
     * @param i the resource
     * @return newly created encoded image
     * @throws java.io.IOException if thrown by the input stream
     */
    public static EncodedImage create(String i) throws IOException {
        return create(Display.getInstance().getResourceAsStream(EncodedImage.class, i));
    }

    /**
     * @inheritDoc
     */
    public Image subImage(int x, int y, int width, int height, boolean processAlpha)  {
        return getInternalImpl().subImage(x, y, width, height, processAlpha);
    }

    /**
     * @inheritDoc
     */
    public Image rotate(int degrees) {
        return getInternalImpl().rotate(degrees);
    }
    
    /**
     * @inheritDoc
     */
    public Image modifyAlpha(byte alpha) {
        return getInternalImpl().modifyAlpha(alpha);
    }
    
    /**
     * @inheritDoc
     */
    public Image modifyAlpha(byte alpha, int removeColor) {
        return getInternalImpl().modifyAlpha(alpha, removeColor);
    }

    /**
     * @inheritDoc
     */
    public Graphics getGraphics() {        
        return null;
    }

    /**
     * @inheritDoc
     */
    public int getWidth() {
        if(width > -1) {
            return width;
        }
        width = getInternalImpl().getWidth();
        return width;
    }

    /**
     * @inheritDoc
     */
    public int getHeight() {
        if(height > -1) {
            return height;
        }
        height = getInternalImpl().getHeight();
        return height;
    }

    /**
     * @inheritDoc
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        getInternalImpl().drawImage(g, nativeGraphics, x, y);
    }

    /**
     * @inheritDoc
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        getInternalImpl().drawImage(g, nativeGraphics, x, y, w, h);
    }

    /**
     * @inheritDoc
     */
    void getRGB(int[] rgbData,
            int offset,
            int x,
            int y,
            int width,
            int height) {
        getInternalImpl().getRGB(rgbData, offset, x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void toRGB(RGBImage image,
            int destX,
            int destY,
            int x,
            int y,
            int width,
            int height) {
        getInternalImpl().toRGB(image, destX, destY, x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public Image scaledWidth(int width) {
        return getInternalImpl().scaledWidth(width);
    }

    /**
     * @inheritDoc
     */
    public Image scaledHeight(int height) {
        return getInternalImpl().scaledHeight(height);
    }

    /**
     * @inheritDoc
     */
    public Image scaledSmallerRatio(int width, int height) {
        return getInternalImpl().scaledSmallerRatio(width, height);
    }

    /**
     * @inheritDoc
     */
    public Image scaled(int width, int height) {
        return getInternalImpl().scaled(width, height);
    }

    /**
     * @inheritDoc
     */
    public void scale(int width, int height) {
        getInternalImpl().scale(width, height);
    }

    /**
     * @inheritDoc
     */
    public boolean isAnimation() {
        return false;
    }

    /**
     * @inheritDoc
     */
    public boolean isOpaque() {
        if(opaqueChecked) {
            return opaque;
        }
        opaque = getInternalImpl().isOpaque();
        return opaque;
    }
}
