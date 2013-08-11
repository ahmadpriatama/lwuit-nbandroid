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
package com.sun.lwuit.layouts;

import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.plaf.UIManager;

/**
 * Flows elements in a row so they can spill over when reaching line end
 *
 * @author Nir Shabi
 */
public class FlowLayout extends Layout{
    private boolean fillRows;

    private int orientation = Component.LEFT;
    private int valign = Component.TOP;


    /**
     * Creates a new instance of FlowLayout with left alignment
     */
    public FlowLayout() {
    }

    /**
     * Creates a new instance of FlowLayout with the given orientation one of
     * LEFT, RIGHT or CENTER
     *
     * @param orientation the orientation value
     */
    public FlowLayout(int orientation) {
        this.orientation = orientation;
    }

    /**
     * @inheritDoc
     */
    public void layoutContainer(Container parent) {
        int x = parent.getStyle().getPadding(parent.isRTL(), Component.LEFT);
        int width = parent.getLayoutWidth() - parent.getSideGap() - parent.getStyle().getPadding(parent.isRTL(), Component.RIGHT) - x;

        boolean rtl = parent.isRTL();
        if(rtl) {
        	x += parent.getSideGap();
        }
        int initX = x;

        int y = parent.getStyle().getPadding(false, Component.TOP);
        int rowH=0;
        int start=0;

        int maxComponentWidth = width;

        int numOfcomponents = parent.getComponentCount();
        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            Style style = cmp.getStyle();
            int marginX = style.getMargin(false, Component.LEFT) + style.getMargin(false, Component.RIGHT);
            cmp.setWidth(Math.min(maxComponentWidth - marginX, cmp.getPreferredW()));
            cmp.setHeight(cmp.getPreferredH());

            if((x == parent.getStyle().getPadding(rtl, Component.LEFT)) || ( x+ cmp.getPreferredW() <= width) ) {
                // We take the actual LEFT since drawing is done in reverse
                x += cmp.getStyle().getMargin(false, Component.LEFT);
            	if(rtl) {
                	cmp.setX(Math.max(width + initX - (x - initX) - cmp.getPreferredW(), style.getMargin(false, Component.LEFT)));
            	} else {
            		cmp.setX(x);
            	}

                cmp.setY(y + cmp.getStyle().getMargin(cmp.isRTL(), Component.TOP));

                x += cmp.getPreferredW() + cmp.getStyle().getMargin(false, Component.RIGHT);
                rowH = Math.max(rowH, cmp.getPreferredH() + cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM));
            } else {
                moveComponents(parent, parent.getStyle().getPadding(rtl, Component.LEFT), y, width - x, rowH, start, i);
                fillRow(parent, width, start, i);
                x = initX+cmp.getStyle().getMargin(false, Component.LEFT);
                y += rowH;

                if(rtl) {
                	cmp.setX(Math.max(width + initX - (x - initX) - cmp.getPreferredW(), style.getMargin(false, Component.LEFT)));
                } else {
                	cmp.setX(x);
                }

                cmp.setY(y + cmp.getStyle().getMargin(false, Component.TOP));
                rowH = cmp.getPreferredH()+ cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM);
                x += cmp.getPreferredW()+ cmp.getStyle().getMargin(false, Component.RIGHT);
                start = i;

            }
        }
        moveComponents(parent, parent.getStyle().getPadding(parent.isRTL(), Component.LEFT), y, width - x, rowH, start, numOfcomponents);
        fillRow(parent, width, start, numOfcomponents);
    }

    private void fillRow(Container target, int width, int start, int end) {
        if(fillRows) {
            int available = width;
            for(int iter = start ; iter < end ; iter++) {
                Component c = target.getComponentAt(iter);
                available -= c.getWidth();
            }
            if(available > 0 && end - start > 0) {
                int perComponent = available / (end - start);
                int lastComponent = perComponent + available % (end - start);
                if(perComponent > 0) {
                    int addOffset = 0;
                    boolean rtl = target.isRTL();
                    for(int iter = start ; iter < end - 1 ; iter++) {
                        Component c = target.getComponentAt(iter);
                        c.setWidth(c.getWidth() + perComponent);
                        if(rtl) {
                            addOffset += perComponent;
                            c.setX(c.getX() - addOffset);
                        } else {
                            c.setX(c.getX() + addOffset);
                            addOffset += perComponent;
                        }
                    }
                    Component c = target.getComponentAt(end - 1);
                    if(rtl) {
                        addOffset += lastComponent;
                        c.setX(c.getX() - addOffset);
                    } else {
                        c.setX(c.getX() + addOffset);
                    }
                    c.setWidth(c.getWidth() + lastComponent);
                } else {
                    Component c = target.getComponentAt(end - 1);
                    c.setWidth(c.getWidth() + lastComponent);
                }
            }
        }
    }

    private void moveComponents(Container target, int x, int y, int width, int height, int rowStart, int rowEnd ) {
        switch (orientation) {
            case Component.CENTER:
                // this will remove half of last gap
                if (target.isRTL()) {
                	x -= (width) / 2;
                } else {
                	x += (width) / 2;
                }
                break;
            case Component.RIGHT:
                x+=width;  // this will remove the last gap
                break;
        }
        Style parentStyle = target.getStyle();
        int parentPadding = parentStyle.getPadding(Component.LEFT) + parentStyle.getPadding(Component.RIGHT);


        for (int i = rowStart ; i < rowEnd ; i++) {
            Component m = target.getComponentAt(i);
            Style style = m.getStyle();
            int marginX = style.getMargin(false, Component.LEFT) + style.getMargin(false, Component.RIGHT);
            if(m.getWidth() + marginX < target.getWidth() - parentPadding){
                m.setX(m.getX()+ x);
            }
            int marginTop = m.getStyle().getMargin(false, Component.TOP);
            switch(valign) {
                case Component.BOTTOM:
                    m.setY(y + Math.max(marginTop, target.getHeight() - m.getHeight()));
                    break;
                case Component.CENTER:
                    m.setY(y + Math.max(marginTop, (target.getHeight() - m.getHeight()) / 2));
                    break;
                default:
                    m.setY(y + marginTop);
                    break;
            }
        }
    }

    /**
     * @inheritDoc
     */
    public  Dimension getPreferredSize(Container parent) {
        int parentWidth = parent.getWidth();
        if(parentWidth == 0){
            parent.invalidate();
        }
        int width = 0;
        int height = 0;
        int w = 0;
        int numOfcomponents = parent.getComponentCount();
        Style parentStyle = parent.getStyle();
        int parentPadding = parentStyle.getPadding(Component.LEFT) + parentStyle.getPadding(Component.RIGHT);

        for(int i=0; i< numOfcomponents; i++){
            Component cmp = parent.getComponentAt(i);
            height = Math.max(height, cmp.getPreferredH() + cmp.getStyle().getMargin(false, Component.TOP)+ cmp.getStyle().getMargin(false, Component.BOTTOM));
            int prefW = cmp.getPreferredW()+ cmp.getStyle().getMargin(false, Component.RIGHT)+ cmp.getStyle().getMargin(false, Component.LEFT);
            w += prefW;
            //we need to break a line
            if (parentWidth > parentPadding && w >= parentWidth && i > 0) {
                height += cmp.getPreferredH() + cmp.getStyle().getMargin(false, Component.TOP) + cmp.getStyle().getMargin(false, Component.BOTTOM);
                width = Math.max(w, width);
                w = prefW;
            }
        }

        width = Math.max(w, width);

        return new Dimension(width + parent.getStyle().getPadding(false, Component.LEFT)+ parent.getStyle().getPadding(false, Component.RIGHT),
            height + parent.getStyle().getPadding(false, Component.TOP)+ parent.getStyle().getPadding(false, Component.BOTTOM));
    }


    /**
     * @inheritDoc
     */
    public String toString() {
        return "FlowLayout";
    }

    /**
     * Indicates whether the layout manager should try to fill up the available space
     * in the row
     *
     * @return the fillRows
     */
    public boolean isFillRows() {
        return fillRows;
    }

    /**
     * Indicates whether the layout manager should try to fill up the available space
     * in the row
     *
     * @param fillRows the fillRows to set
     */
    public void setFillRows(boolean fillRows) {
        this.fillRows = fillRows;
    }

    /**
     * Indicates vertical alignment within the flow layout
     *
     * @return Component.TOP/BOTTOM/CENTER
     */
    public int getValign() {
        return valign;
    }

    /**
     * Indicates vertical alignment within the flow layout
     *
     * @param valign one of Component.TOP/BOTTOM/CENTER
     */
    public void setValign(int valign) {
        this.valign = valign;
    }

    /**
     * Alignment of the flow layout, defaults to LEFT
     *
     * @return the orientation
     */
    public int getAlign() {
        return orientation;
    }

    /**
     * Alignment of the flow layout, defaults to LEFT
     *
     * @param orientation the orientation to set
     */
    public void setAlign(int orientation) {
        this.orientation = orientation;
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object o) {
        return super.equals(o) && ((FlowLayout)o).orientation == orientation &&
                ((FlowLayout)o).valign == valign &&
                ((FlowLayout)o).fillRows == fillRows;
    }
}