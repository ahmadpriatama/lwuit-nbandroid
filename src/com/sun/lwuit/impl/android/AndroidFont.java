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

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import com.sun.lwuit.Font;

/**
 *
 */
public class AndroidFont {

    final int face, style, size;
    int defaultFontHeight;
    TextPaint textPaint;
    int height;

    AndroidFont(AndroidImplementation impl, int face, int style, int size) {

        this.face = face;
        this.style = style;
        this.size = size;

        /**
         * translate our default font height depending on the screen density.
         * this is required for new high resolution devices.  otherwise everything
         * looks awfully small.
         *
         * we use our default font height value of 18 and go from there.  i thought
         * about using new Paint().getTextSize() for this value but if some new
         * version of android suddenly returns values already tranlated to the screen
         * then we might end up with too large fonts.  the documentation is not very
         * precise on that.
         */
        final int defaultFontPixelHeight = 16;
        // (int)new TextView(activity).getTextSize();
        this.defaultFontHeight = impl.translatePixelForDPI(defaultFontPixelHeight);



        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        Typeface typeface = null;
        switch (face) {
            case Font.FACE_MONOSPACE:
                typeface = Typeface.MONOSPACE;
                break;
            default:
                typeface = Typeface.DEFAULT;
                break;
        }

        int fontstyle = Typeface.NORMAL;
        if ((style & Font.STYLE_BOLD) != 0) {
            fontstyle |= Typeface.BOLD;
        }
        if ((style & Font.STYLE_ITALIC) != 0) {
            fontstyle |= Typeface.ITALIC;
        }


        height = this.defaultFontHeight;
        int diff = height / 3;

        switch (size) {
            case Font.SIZE_SMALL:
                height -= diff;
                break;
            case Font.SIZE_LARGE:
                height += diff;
                break;
        }

        textPaint.setTypeface(Typeface.create(typeface, fontstyle));
        textPaint.setUnderlineText((style & Font.STYLE_UNDERLINED) != 0);
        textPaint.setTextSize(height);
        height = textPaint.getFontMetricsInt(textPaint.getFontMetricsInt());
    }

    AndroidFont(String lookup) {
        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        int typeface = Typeface.NORMAL;
        this.face = Font.FACE_SYSTEM;
        this.size = Font.SIZE_MEDIUM;
        String familyName = lookup.substring(0, lookup.indexOf("-"));
        String style = lookup.substring(lookup.indexOf("-") + 1, lookup.lastIndexOf("-"));
        String size = lookup.substring(lookup.lastIndexOf("-") + 1, lookup.length());
        
        if (style.equals("bolditalic")) {
            typeface = Typeface.BOLD_ITALIC;
            this.style = Font.STYLE_BOLD | Font.STYLE_ITALIC;
        } else if (style.equals("italic")) {
            typeface = Typeface.ITALIC;
            this.style = Font.STYLE_ITALIC;
        } else if (style.equals("bold")) {
            typeface = Typeface.BOLD;
            this.style = Font.STYLE_BOLD; 
        } else {
            this.style = Font.STYLE_PLAIN;
        }
        textPaint.setTypeface(Typeface.create(familyName, typeface));
        textPaint.setTextSize(Integer.parseInt(size));
        height = textPaint.getFontMetricsInt(textPaint.getFontMetricsInt());
    }

    public int getStyle() {
        /*
        // http://www.java.net/forum/topic/mobile-embedded/lwuit/android-implementation-font-issue
        if (nativeFont instanceof Paint) {
        Paint new_name = (Paint) nativeFont;
        return new_name.getTypeface().getStyle();
        }
        return 0;
         * 
         */
        return style;
    }

    public int getSize() {
        return size;
    }

    public int getHeight() {
        return height;
    }
}
