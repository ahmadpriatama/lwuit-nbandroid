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

package com.sun.lwuit.spinner;

import com.sun.lwuit.Component;
import com.sun.lwuit.List;
import com.sun.lwuit.list.DefaultListCellRenderer;
import java.util.Calendar;
import java.util.Date;

/**
 * A renderer that can represent values for Date and time, time is represented as an integer
 * for seconds since midnight. This is formatted accordingly by the renderer
 *
 * @author Shai Almog
 */
class DateTimeRenderer extends DefaultListCellRenderer {
    private boolean date;
    private int type;
    private char separatorChar;
    private boolean twentyFourHours;
    private boolean showSeconds;

    private DateTimeRenderer() {
        super(false);
    }

    boolean isShowSeconds() {
        return showSeconds;
    }

    /**
     * Construct a time renderer
     *
     * @param twentyFourHours show the value as 24 hour values or AM/PM
     * @param showSeconds show the value of the seconds as well or hide it
     */
    public static DateTimeRenderer createTimeRenderer(boolean twentyFourHours, boolean showSeconds) {
        DateTimeRenderer d = new DateTimeRenderer();
        d.twentyFourHours = twentyFourHours;
        d.showSeconds = showSeconds;
        return d;
    }

    /**
     * Constructs a date renderer
     *
     * @param separatorChar char separating the entries within the renderer such as /, - etc.
     * @param format the date, one of the constant values in this class
     */
    public static DateTimeRenderer createDateRenderer(char separatorChar, int format) {
        DateTimeRenderer d = new DateTimeRenderer();
        d.date = true;
        d.separatorChar = separatorChar;
        d.type = format;
        return d;
    }

    private String twoDigits(int i) {
        if(i < 10) {
            return "0" + i;
        }
        return "" + i;
    }

    /**
     * @inheritDoc
     */
    public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
        if(date) {
            Calendar c = Calendar.getInstance();
            c.setTime((Date)value);
            int day = c.get(Calendar.DAY_OF_MONTH);
            int month = c.get(Calendar.MONTH) + 1;
            int year = c.get(Calendar.YEAR);
            switch(type) {
                case Spinner.DATE_FORMAT_DD_MM_YYYY:
                    value = twoDigits(day) + separatorChar + twoDigits(month) + separatorChar + year;
                    break;
                case Spinner.DATE_FORMAT_MM_DD_YYYY:
                    value = twoDigits(month) + separatorChar + twoDigits(day) + separatorChar + year;
                    break;
                case Spinner.DATE_FORMAT_DD_MM_YY:
                    value = twoDigits(day) + separatorChar + twoDigits(month) + separatorChar + (year % 100);
                    break;
                case Spinner.DATE_FORMAT_MM_DD_YY:
                    value = twoDigits(month) + separatorChar + twoDigits(day) + separatorChar + (year % 100);
                    break;
            }
        } else {
            int v = ((Integer)value).intValue();
            int seconds = v % 60;
            v /= 60;
            int minutes = v % 60;
            v /= 60;
            int hours;
            String amPm = "";
            if(twentyFourHours) {
                hours = v % 24;
            } else {
                hours = v % 12;
                if(v >= 12) {
                    amPm = "PM";
                } else {
                    amPm = "AM";
                }
            }
            if(showSeconds) {
                value = twoDigits(hours) + ":" + twoDigits(minutes) + ":" + twoDigits(seconds) + amPm;
            } else {
                value = twoDigits(hours) + ":" + twoDigits(minutes) + amPm;
            }
        }
        return super.getListCellRendererComponent(list, value, index, isSelected);
    }

    /**
     * @return the twentyFourHours
     */
    public boolean isTwentyFourHours() {
        return twentyFourHours;
    }


}
