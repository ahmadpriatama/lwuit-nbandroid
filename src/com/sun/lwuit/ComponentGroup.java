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

import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.plaf.UIManager;

/**
 * <p>A component group is a container that applies the given UIID to a set of components within it
 * and gives the same UIID with "First"/"Last" prepended to the first and last components. E.g.
 * by default the  GroupElement UIID is applied so the first and last elements would have the
 * GroupElementFirst/GroupElementLast UIID's applied to them. If a group has only one element
 * the word "Only" is appended to the element UIID as in GroupElementOnly.
 * <p><b>Important!!!</b> A component group does nothing by default unless explicitly activated by
 * the theme by enabling the ComponentGroupBool constant (by default, this can be customized via the groupFlag property).
 * This allows logical grouping without changing the UI for themes that don't need grouping.
 * <p>This container uses box X/Y layout (defaults to Y), other layout managers shouldn't be used
 * since this container relies on the specific behavior of the box layout.
 *
 * @author Shai Almog
 */
public class ComponentGroup extends Container {
    private String elementUIID = "GroupElement";
    private String groupFlag = "ComponentGroupBool";
    private boolean uiidsDirty;
 
    /**
     * Default constructor
     */
    public ComponentGroup() {
        super(new BoxLayout(BoxLayout.Y_AXIS));
        setUIID("ComponentGroup");
    }

    private void reverseRadio(Component cmp) {
        if(cmp instanceof ComboBox) {
            ((ComboBox)cmp).setActAsSpinnerDialog(uiidsDirty);
        }
    }

    void insertComponentAt(int index, Component cmp) {
        super.insertComponentAt(index, cmp);
        updateUIIDs();
    }

    /**
     * @inheritDoc
     */
    public void refreshTheme() {
        super.refreshTheme();
        if(!UIManager.getInstance().isThemeConstant(groupFlag, false)) {
            if(uiidsDirty) {
                uiidsDirty = false;
                int count = getComponentCount();
                for(int iter = 0 ; iter < count ; iter++) {
                    restoreUIID(getComponentAt(iter));
                }
            }
        } else {
            updateUIIDs();
        }
    }

    void removeComponentImpl(Component cmp) {
        super.removeComponentImpl(cmp);
        
        // restore original UIID
        Object o = cmp.getClientProperty("$origUIID");
        if(o != null) {
            cmp.setUIID((String)o);
        }
        updateUIIDs();
    }

    private void updateUIIDs() {
        if(!UIManager.getInstance().isThemeConstant(groupFlag, false)) {
            return;
        }
        int count = getComponentCount();
        if(count > 0) {
            uiidsDirty = true;
            if(count == 1) {
                updateUIID(elementUIID + "Only", getComponentAt(0));
            } else {
                updateUIID(elementUIID + "First", getComponentAt(0));
                if(count > 1) {
                    updateUIID(elementUIID + "Last", getComponentAt(count - 1));
                    for(int iter = 1 ; iter < count - 1 ; iter++) {
                        updateUIID(elementUIID, getComponentAt(iter));
                    }
                }
            }
        }
    }

    private void updateUIID(String newUIID, Component c) {
        Object o = c.getClientProperty("$origUIID");
        if(o == null) {
            c.putClientProperty("$origUIID", c.getUIID());
        }
        c.setUIID(newUIID);
        reverseRadio(c);
    }

    private void restoreUIID(Component c) {
        String o = (String)c.getClientProperty("$origUIID");
        if(o != null) {
            c.setUIID(o);
        }
        reverseRadio(c);
    }

    /**
     * Indicates that the component group should be horizontal by using the BoxLayout Y
     * @return the horizontal
     */
    public boolean isHorizontal() {
        return getLayout() instanceof BoxLayout && ((BoxLayout)getLayout()).getAxis() == BoxLayout.X_AXIS;
    }

    /**
     * Indicates that the component group should be horizontal by using the BoxLayout Y
     * @param horizontal the horizontal to set
     */
    public void setHorizontal(boolean horizontal) {
        if(horizontal) {
            setLayout(new BoxLayout(BoxLayout.X_AXIS));
        } else {
            setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        }
    }

    /**
     * The UIID to apply to the elements within this container
     *
     * @return the elementUIID
     */
    public String getElementUIID() {
        return elementUIID;
    }

    /**
     * The UIID to apply to the elements within this container
     * @param elementUIID the elementUIID to set
     */
    public void setElementUIID(String elementUIID) {
        this.elementUIID = elementUIID;
    }

    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"elementUIID", "displayName", "horizontal", "groupFlag"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {String.class, String.class, Boolean.class, String.class};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("elementUIID")) {
            return getElementUIID();
        }
        if(name.equals("horizontal")) {
            if(isHorizontal()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("groupFlag")) {
            return groupFlag;
        }
        return null;
    }


    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("elementUIID")) {
            setElementUIID((String)value);
            return null;
        }
        if(name.equals("horizontal")) {
            setHorizontal(((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("groupFlag")) {
            setGroupFlag(groupFlag);
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * The group flag allows changing the flag that activates this group, from ComponentGroupBool to any
     * arbitrary flag. This allows a developer/designer to enable grouping for a specific type of components
     * (e.g. for horizontal Toggle Buttons) yet disable it for vertical lists of components.
     *
     * @return the groupFlag
     */
    public String getGroupFlag() {
        return groupFlag;
    }

    /**
     * The group flag allows changing the flag that activates this group, from ComponentGroupBool to any
     * arbitrary flag. This allows a developer/designer to enable grouping for a specific type of components
     * (e.g. for horizontal Toggle Buttons) yet disable it for vertical lists of components.
     * 
     * @param groupFlag the groupFlag to set
     */
    public void setGroupFlag(String groupFlag) {
        this.groupFlag = groupFlag;
    }
}
