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

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * #######################################################################
 * #######################################################################
 * 
 * Bundle one canvas and two paints to get one graphics object.
 */
class AndroidGraphics {

    private Canvas canvas;
    private Paint paint;
    private AndroidFont font;

    AndroidGraphics(AndroidImplementation impl, Canvas canvas) {
        this.canvas = canvas;
        this.paint = new Paint();
        paint.setAntiAlias(true);
        this.font = (AndroidFont) impl.getDefaultFont();
    }

    Canvas getCanvas() {
        return canvas;
    }

    void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    AndroidFont getFont() {
        return font;
    }

    void setFont(AndroidFont font) {
        this.font = font;
        this.font.textPaint.setColor(this.paint.getColor());
    }

    void setColor(int color){
        this.paint.setColor(color);
        this.font.textPaint.setColor(color);
    }

    Paint getPaint() {
        return paint;
    }

}