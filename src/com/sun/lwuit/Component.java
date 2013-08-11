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

import com.sun.lwuit.util.EventDispatcher;
import com.sun.lwuit.geom.Rectangle;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.animations.Animation;
import com.sun.lwuit.animations.Motion;
import com.sun.lwuit.events.FocusListener;
import com.sun.lwuit.events.StyleListener;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.LookAndFeel;
import com.sun.lwuit.plaf.UIManager;
import java.util.Hashtable;

/**
 * Base class for all the widgets in the toolkit using the composite pattern in 
 * a similar way to the AWT Container/Component relationship. All components are
 * potentially animated (need to be registered in {@link Display}). 
 * 
 * @author Chen Fishbein
 */
public class Component implements Animation, StyleListener {
    private String selectText = UIManager.getInstance().localize("select", "Select");
    private boolean alwaysTensile;
    private int tensileLength = -1;

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the down key
     */
    private Component nextFocusDown;
    private Component nextFocusUp;
    /**
     * Indicates whether component is enabled or disabled
     */
    private boolean enabled = true;
    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the right key
     */
    private Component nextFocusRight;
    private Component nextFocusLeft;
    private String name;
    boolean hasLead;

    /**
     * This property is useful for blocking in z-order touch events, sometimes we might want to grab touch events in
     * a specific component without making it focusable.
     */
    private boolean grabsPointerEvents;

    /**
     * Indicates whether tensile drag (dragging beyond the boundry of the component and
     * snapping back) is enabled for this component.
     */
    private boolean tensileDragEnabled = true;

    /**
     * Indicates whether the component should "trigger" tactile touch when pressed by the user
     * in a touch screen UI.
     */
    private boolean tactileTouch;

    /**
     * Baseline resize behavior constant used to properly align components. 
     * Indicates as the size of the component
     * changes the baseline remains a fixed distance from the top of the
     * component.
     * @see #getBaselineResizeBehavior
     */
    public static final int BRB_CONSTANT_ASCENT = 1;
    /**
     * Baseline resize behavior constant used to properly align components. Indicates as the size of the component
     * changes the baseline remains a fixed distance from the bottom of the 
     * component.
     * @see #getBaselineResizeBehavior
     */
    public static final int BRB_CONSTANT_DESCENT = 2;
    /**
     * Baseline resize behavior constant used to properly align components. Indicates as the size of the component
     * changes the baseline remains a fixed distance from the center of the
     * component.
     * @see #getBaselineResizeBehavior
     */
    public static final int BRB_CENTER_OFFSET = 3;
    /**
     * Baseline resize behavior constant used to properly align components. Indicates as the size of the component
     * changes the baseline can not be determined using one of the other
     * constants.
     * @see #getBaselineResizeBehavior
     */
    public static final int BRB_OTHER = 4;
    private boolean visible = true;
    /**
     * Used as an optimization to mark that this component is currently being
     * used as a cell renderer
     */
    private boolean cellRenderer;
    private Rectangle bounds = new Rectangle(0, 0, new Dimension(0, 0));
    private Rectangle painterBounds;
    private int scrollX;
    private int scrollY;
    private boolean sizeRequestedByUser = false;
    private Dimension preferredSize;
    private boolean scrollSizeRequestedByUser = false;
    private Dimension scrollSize;
    private Style unSelectedStyle;
    private Style pressedStyle;
    private Style selectedStyle;
    private Style disabledStyle;
    private Container parent;
    private boolean focused = false;
    private EventDispatcher focusListeners = new EventDispatcher();
    private boolean handlesInput = false;
    boolean shouldCalcPreferredSize = true;
    boolean shouldCalcScrollSize = true;
    private boolean focusable = true;
    private boolean isScrollVisible = true;
    private boolean repaintPending;
    private boolean snapToGrid;

    private int scrollOpacity = 0xff;

    /**
     * Indicates the decrement units for the scroll opacity
     */
    private int scrollOpacityChangeSpeed = 5;

    /**
     * Indicates that moving through the component should work as an animation
     */
    private boolean smoothScrolling;
    /**
     * Animation speed in milliseconds allowing a developer to slow down or accelerate
     * the smooth animation mode
     */
    private int animationSpeed;
    private Motion animationMotion;
    private Motion draggedMotion;

    /**
     * Allows us to flag a drag operation in action thus preventing the mouse pointer
     * release event from occurring.
     */
    private boolean dragActivated;
    private int initialScrollY = -1;
    private int destScrollY = -1;
    private int lastScrollY;
    private int lastScrollX;

    /**
     * Indicates if the component is in the initialized state, a component is initialized
     * when its initComponent() method was invoked. The initMethod is invoked before showing the
     * component to the user.
     */
    private boolean initialized;
    /**
     * Indicates a Component center alignment
     */
    public static final int CENTER = 4;
    /** 
     * Box-orientation constant used to specify the top of a box.
     */
    public static final int TOP = 0;
    /** 
     * Box-orientation constant used to specify the left side of a box.
     */
    public static final int LEFT = 1;
    /** 
     * Box-orientation constant used to specify the bottom of a box.
     */
    public static final int BOTTOM = 2;
    /** 
     * Box-orientation constant used to specify the right side of a box.
     */
    public static final int RIGHT = 3;
    private Hashtable clientProperties;
    private Rectangle dirtyRegion = null;
    private final Object dirtyRegionLock = new Object();
    private Label componentLabel;
    private String id;

    /**
     * Is the component a bidi RTL component
     */
    private boolean rtl;
    private boolean flatten;

    private Object paintLockImage;

    private boolean draggable;
    private boolean dragAndDropInitialized;
    private boolean dropTarget;
    private Image dragImage;
    private Component dropTargetComponent;

    boolean isDragAndDropInitialized() {
        return dragAndDropInitialized;
    }

    /** 
     * Creates a new instance of Component 
     */
    protected Component() {
        LookAndFeel laf = UIManager.getInstance().getLookAndFeel();
        animationSpeed = laf.getDefaultSmoothScrollingSpeed();
        rtl = laf.isRTL();
        tactileTouch = isFocusable();
        tensileDragEnabled = laf.isDefaultTensileDrag();
        snapToGrid = laf.isDefaultSnapToGrid();
        alwaysTensile = laf.isDefaultAlwaysTensile();
    }

    private void initStyle() {
        unSelectedStyle = UIManager.getInstance().getComponentStyle(getUIID());
        if (unSelectedStyle != null) {
            unSelectedStyle.addStyleListener(this);
            if (unSelectedStyle.getBgPainter() == null) {
                unSelectedStyle.setBgPainter(new BGPainter());
            }
        }
        if(disabledStyle != null) {
            disabledStyle.addStyleListener(this);
            if (disabledStyle.getBgPainter() == null) {
                disabledStyle.setBgPainter(new BGPainter());
            }
        }
    }

    /**
     * Returns the current component x location relatively to its parent container
     * 
     * @return the current x coordinate of the components origin
     */
    public int getX() {
        return bounds.getX();
    }

    /**
     * Returns the component y location relatively to its parent container
     * 
     * @return the current y coordinate of the components origin
     */
    public int getY() {
        return bounds.getY();
    }

    /**
     * Returns whether the component is visible or not
     * 
     * @return true if component is visible; otherwise false 
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Client properties allow the association of meta-data with a component, this
     * is useful for some applications that construct GUI's on the fly and need
     * to track the connection between the UI and the data. 
     * 
     * @param key the key used for putClientProperty
     * @return the value set to putClientProperty or null if no value is set to the property
     */
    public Object getClientProperty(String key) {
        if (clientProperties == null) {
            return null;
        }
        return clientProperties.get(key);
    }

    Component getLeadComponent() {
        Container p = getParent();
        if(p != null) {
            return p.getLeadComponent();
        }
        return null;
    }

    /**
     * Client properties allow the association of meta-data with a component, this
     * is useful for some applications that construct GUI's on the fly and need
     * to track the connection between the UI and the data. Setting the value to
     * null will remove the client property from the component.
     * 
     * @param key arbitrary key for the property
     * @param value the value assigned to the given client property
     */
    public void putClientProperty(String key, Object value) {
        if (clientProperties == null) {
            if (value == null) {
                return;
            }
            clientProperties = new Hashtable();
        }
        if (value == null) {
            clientProperties.remove(key);
            if (clientProperties.size() == 0) {
                clientProperties = null;
            }
        } else {
            clientProperties.put(key, value);
        }
    }

    /**
     * gets the Component dirty region,  this method is for internal use only and SHOULD NOT be invoked by user code.
     * Use repaint(int,int,int,int)
     * 
     * @return returns the region that needs repainting or null for the whole component
     */
    public final Rectangle getDirtyRegion() {
        return dirtyRegion;
    }

    /**
     * sets the Component dirty region, this method is for internal use only and SHOULD NOT be invoked by user code.
     * Use repaint(int,int,int,int)
     * 
     * @param dirty the region that needs repainting or null for the whole component
     */
    public final void setDirtyRegion(Rectangle dirty) {
        synchronized (dirtyRegionLock) {
            this.dirtyRegion = dirty;
        }

    }

    /**
     * Toggles visibility of the component
     * 
     * @param visible true if component is visible; otherwise false 
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Returns the component width
     * 
     * @return the component width
     */
    public int getWidth() {
        return bounds.getSize().getWidth();
    }

    /**
     * Returns the component height
     * 
     * @return the component height
     */
    public int getHeight() {
        return bounds.getSize().getHeight();
    }

    /**
     * Sets the Component x location relative to the parent container, this method
     * is exposed for the purpose of external layout managers and should not be invoked
     * directly.
     * 
     * @param x the current x coordinate of the components origin
     */
    public void setX(int x) {
        bounds.setX(x);
    }

    /**
     * Sets the Component y location relative to the parent container, this method
     * is exposed for the purpose of external layout managers and should not be invoked
     * directly.
     * 
     * @param y the current y coordinate of the components origin
     */
    public void setY(int y) {
        bounds.setY(y);
    }

    /**
     * The baseline for the component text according to which it should be aligned
     * with other components for best visual look.
     * 
     * 
     * @param width the component width
     * @param height the component height
     * @return baseline value from the top of the component
     */
    public int getBaseline(int width, int height) {
        return height - getStyle().getPadding(false, BOTTOM);
    }

    /**
     * Returns a constant indicating how the baseline varies with the size
     * of the component.
     *
     * @return one of BRB_CONSTANT_ASCENT, BRB_CONSTANT_DESCENT,
     *         BRB_CENTER_OFFSET or BRB_OTHER
     */
    public int getBaselineResizeBehavior() {
        return BRB_OTHER;
    }

    /**
     * Sets the Component Preferred Size, there is no guarantee the Component will 
     * be sized at its Preferred Size. The final size of the component may be
     * smaller than its preferred size or even larger than the size.<br>
     * The Layout manager can take this value into consideration, but there is
     * no guarantee or requirement.
     * 
     * @param d the component dimension
     */
    public void setPreferredSize(Dimension d) {
        if(d == null) {
            sizeRequestedByUser = false;
            preferredSize = null;
            shouldCalcPreferredSize = true;
            return;
        }
        Dimension dim = preferredSize();
        dim.setWidth(d.getWidth());
        dim.setHeight(d.getHeight());
        sizeRequestedByUser = true;
    }


    /**
     * Returns the Component Preferred Size, there is no guarantee the Component will 
     * be sized at its Preferred Size. The final size of the component may be
     * smaller than its preferred size or even larger than the size.<br>
     * The Layout manager can take this value into consideration, but there is
     * no guarantee or requirement.
     * 
     * @return the component preferred size
     */
    public Dimension getPreferredSize() {
        return preferredSize();
    }

    Dimension getPreferredSizeWithMargin() {
        Dimension d = preferredSize();
        Style s = getStyle();
        return new Dimension(d.getWidth() +s.getMargin(LEFT) + s.getMargin(RIGHT), d.getHeight() + s.getMargin(TOP) + s.getMargin(BOTTOM));
    }

    /**
     * Returns the Components dimension in scrolling, this is very similar to the
     * preferred size aspect only it represents actual scrolling limits.
     * 
     * @return the component actual size with all scrolling
     */
    public Dimension getScrollDimension() {
        if (!scrollSizeRequestedByUser && (scrollSize == null || shouldCalcScrollSize)) {
            scrollSize = calcScrollSize();
            shouldCalcScrollSize = false;
        }
        return scrollSize;
    }

    /**
     * Method that can be overriden to represent the actual size of the component 
     * when it differs from the desireable size for the viewport
     * 
     * @return scroll size, by default this is the same as the preferred size
     */
    protected Dimension calcScrollSize() {
        return calcPreferredSize();
    }

    /**
     * Set the size for the scroll area
     * 
     * @param d dimension of the scroll area
     */
    public void setScrollSize(Dimension d) {
        scrollSize = d;
        scrollSizeRequestedByUser = true;
    }

    /**
     * Helper method to set the preferred width of the component.
     * 
     * @param preferredW the preferred width of the component
     * @see #setPreferredSize
     */
    public void setPreferredW(int preferredW) {
        setPreferredSize(new Dimension(preferredW, getPreferredH()));
    }

    /**
     * Helper method to set the preferred height of the component.
     * 
     * @param preferredH the preferred height of the component
     * @see #setPreferredSize
     */
    public void setPreferredH(int preferredH) {
        setPreferredSize(new Dimension(getPreferredW(), preferredH));
    }

    /**
     * Helper method to retrieve the preferred width of the component.
     * 
     * @return preferred width of the component
     * @see #getPreferredSize
     */
    public int getPreferredW() {
        return getPreferredSize().getWidth();
    }

    /**
     * Helper method to retrieve the preferred height of the component.
     * 
     * @return preferred height of the component
     * @see #getPreferredSize
     */
    public int getPreferredH() {
        return getPreferredSize().getHeight();
    }

    /**
     * Sets the Component width, this method is exposed for the purpose of 
     * external layout managers and should not be invoked directly.<br>
     * If a user wishes to effect the component size setPreferredSize should
     * be used.
     * 
     * @param width the width of the component
     * @see #setPreferredSize
     */
    public void setWidth(int width) {
        bounds.getSize().setWidth(width);
    }

    /**
     * Sets the Component height, this method is exposed for the purpose of 
     * external layout managers and should not be invoked directly.<br>
     * If a user wishes to effect the component size setPreferredSize should
     * be used.
     * 
     * @param height the height of the component
     * @see #setPreferredSize
     */
    public void setHeight(int height) {
        bounds.getSize().setHeight(height);
    }

    /**
     * Sets the Component size, this method is exposed for the purpose of 
     * external layout managers and should not be invoked directly.<br>
     * If a user wishes to effect the component size setPreferredSize should
     * be used.
     * 
     * @param d the component dimension
     * @see #setPreferredSize
     */
    public void setSize(Dimension d) {
        Dimension d2 = bounds.getSize();
        d2.setWidth(d.getWidth());
        d2.setHeight(d.getHeight());
    }

    /**
     * Unique identifier for a component.
     * This id is used to retrieve a suitable Style.
     * 
     * @return unique string identifying this component for the style sheet
     */
    public String getUIID() {
        return id;
    }

    /**
     * This method sets the Component the Unique identifier.
     * This method should be used before a component has been initialized
     * 
     * @param id UIID unique identifier for component type
     */
    public void setUIID(String id) {
        this.id = id;
        unSelectedStyle = null;
        selectedStyle = null;
        disabledStyle = null;
        pressedStyle = null;
        if(!sizeRequestedByUser) {
            preferredSize = null;
        }
    }

    /**
     * Returns the container in which this component is contained
     * 
     * @return the parent container in which this component is contained
     */
    public Container getParent() {
        return parent;
    }

    /**
     * Sets the Component Parent.
     * This method should not be called by the user.
     * 
     * @param parent the parent container
     */
    void setParent(Container parent) {
        this.parent = parent;
    }

    /**
     * Registers interest in receiving callbacks for focus gained events, a focus event 
     * is invoked when the component accepts the focus. A special case exists for the
     * Form which sends a focus even for every selection within the form.
     * 
     * @param l listener interface implementing the observable pattern
     */
    public void addFocusListener(FocusListener l) {
        focusListeners.addListener(l);
    }

    /**
     * Deregisters interest in receiving callbacks for focus gained events
     * 
     * @param l listener interface implementing the observable pattern
     */
    public void removeFocusListener(FocusListener l) {
        focusListeners.removeListener(l);
    }

    /**
     * When working in 3 softbutton mode "fire" key (center softbutton) is sent to this method
     * in order to allow 3 button devices to work properly. When overriding this method
     * you should also override isSelectableInteraction to indicate that a command is placed
     * appropriately on top of the fire key for 3 soft button phones. 
     */
    protected void fireClicked() {
    }

    /**
     * This method allows a component to indicate that it is interested in an "implicit" select
     * command to appear in the "fire" button when 3 softbuttons are defined in a device.
     * 
     * @return true if this is a selectable interaction
     */
    protected boolean isSelectableInteraction() {
        return false;
    }

    /**
     * Fired when component gains focus
     */
    void fireFocusGained() {
        fireFocusGained(this);
    }

    /**
     * Fired when component lost focus
     */
    void fireFocusLost() {
        fireFocusLost(this);
    }

    /**
     * Fired when component gains focus
     */
    void fireFocusGained(Component cmp) {
        if (cmp.isCellRenderer()) {
            return;
        }

        focusListeners.fireFocus(cmp);
        focusGainedInternal();
        focusGained();
        if (isSelectableInteraction()) {
            Form f = getComponentForm();
            if (f != null) {
                f.getMenuBar().addSelectCommand(getSelectCommandText());
            }
        }
    }

    /**
     * Allows determining the text for the select command used in the 3rd softbutton
     * mode.
     *
     * @param selectText text for the interaction with the softkey
     */
    public void setSelectCommandText(String selectText) {
        this.selectText = selectText;
    }

    /**
     * Allows determining the text for the select command used in the 3rd softbutton
     * mode.
     *
     * @return text for the interaction with the softkey
     */
    public String getSelectCommandText() {
        return selectText;
    }

    /**
     * Fired when component lost focus
     */
    void fireFocusLost(Component cmp) {
        if (cmp.isCellRenderer()) {
            return;
        }
        if (isSelectableInteraction()) {
            Form f = getComponentForm();
            if (f != null) {
                f.getMenuBar().removeSelectCommand();
            }
        }

        focusListeners.fireFocus(cmp);
        focusLostInternal();
        focusLost();
    }

    /**
     * This method allows us to detect an action event internally without 
     * implementing the action listener interface.
     */
    void fireActionEvent() {
    }

    /**
     * Allows us to indicate the label associated with this component thus providing
     * visual feedback related for this component e.g. starting the ticker when 
     * the component receives focus.
     * 
     * @param componentLabel a label associated with this component
     */
    public void setLabelForComponent(Label componentLabel) {
        this.componentLabel = componentLabel;
    }

    /**
     * Allows us to indicate the label associated with this component thus providing
     * visual feedback related for this component e.g. starting the ticker when
     * the component receives focus.
     *
     * @return the label associated with this component
     */
    public Label getLabelForComponent() {
        return componentLabel;
    }

    /**
     * This method is useful since it is not a part of the public API yet
     * allows a component within this package to observe focus events
     * without implementing a public interface or creating a new class
     */
    void focusGainedInternal() {
        startComponentLableTicker();
    }


    void startComponentLableTicker() {
        if (componentLabel != null && componentLabel.isTickerEnabled()) {
            if (componentLabel.shouldTickerStart()) {
                componentLabel.startTicker(UIManager.getInstance().getLookAndFeel().getTickerSpeed(), true);
            }
        }
    }

    void stopComponentLableTicker() {
        if (componentLabel != null && componentLabel.isTickerEnabled() && componentLabel.isTickerRunning()) {
            componentLabel.stopTicker();
        }
    }

    /**
     * Callback allowing a developer to track wheh the component gains focus
     */
    protected void focusGained() {
    }

    /**
     * Callback allowing a developer to track wheh the component loses focus
     */
    protected void focusLost() {
    }

    /**
     * This method is useful since it is not a part of the public API yet
     * allows a component within this package to observe focus events
     * without implementing a public interface or creating a new class
     */
    void focusLostInternal() {
        stopComponentLableTicker();
    }

    /**
     * This method paints all the parents Components Background.
     * 
     * @param g the graphics object
     */
    public void paintBackgrounds(Graphics g) {
        drawPainters(g, this.getParent(), this, getAbsoluteX() + getScrollX(),
                getAbsoluteY() + getScrollY(),
                getWidth(), getHeight());
    }

    /**
     * Returns the absolute X location based on the component hierarchy, this method
     * calculates a location on the screen for the component rather than a relative
     * location as returned by getX()
     * 
     * @return the absolute x location of the component
     * @see #getX
     */
    public int getAbsoluteX() {
        int x = getX() - getScrollX();
        Container parent = getParent();
        if (parent != null) {
            x += parent.getAbsoluteX();
        }
        return x;
    }

    /**
     * Returns the absolute Y location based on the component hierarchy, this method
     * calculates a location on the screen for the component rather than a relative
     * location as returned by getX()
     * 
     * @return the absolute y location of the component
     * @see #getY
     */
    public int getAbsoluteY() {
        int y = getY() - getScrollY();
        Container parent = getParent();
        if (parent != null) {
            y += parent.getAbsoluteY();
        }
        return y;
    }

    /**
     * This method performs the paint of the component internally including drawing
     * the scrollbars and scrolling the component. This functionality is hidden
     * from developers to prevent errors
     * 
     * @param g the component graphics
     */
    final void paintInternal(Graphics g) {
        paintInternal(g, true);
    }

    final void paintInternal(Graphics g, boolean paintIntersects) {
        if (!isVisible()) {
            return;
        }

        if(paintLockImage != null) {
            if(paintLockImage instanceof Image) {
                Image i = (Image)paintLockImage;
                g.drawImage(i, getX(), getY());
            } else {
                Image i = (Image)Display.getInstance().extractHardRef(paintLockImage);
                if(i == null) {
                    i = Image.createImage(getWidth(), getHeight());
                    int x = getX();
                    int y = getY();
                    setX(0);
                    setY(0);
                    paintInternalImpl(i.getGraphics(), paintIntersects);
                    setX(x);
                    setY(y);
                    paintLockImage = Display.getInstance().createSoftWeakRef(i);
                }
                g.drawImage(i, getX(), getY());
            }
            return;
        }
        paintInternalImpl(g, paintIntersects);
    }

    private void paintInternalImpl(Graphics g, boolean paintIntersects) {
        int oX = g.getClipX();
        int oY = g.getClipY();
        int oWidth = g.getClipWidth();
        int oHeight = g.getClipHeight();
        if (bounds.intersects(oX, oY, oWidth, oHeight)) {
            g.clipRect(getX(), getY(), getWidth(), getHeight());
            paintBackground(g);

            if (isScrollable()) {
                int scrollX = getScrollX();
                int scrollY = getScrollY();
                g.translate(-scrollX, -scrollY);
                paint(g);
                g.translate(scrollX, scrollY);
                if (isScrollVisible) {
                    paintScrollbars(g);
                }
            } else {
                paint(g);
            }
            if (isBorderPainted()) {
                paintBorder(g);
            }

            //paint all the intersecting Components above the Component
            if (paintIntersects && parent != null) {
                paintIntersectingComponentsAbove(g);
            }

            g.setClip(oX, oY, oWidth, oHeight);
        }
    }

    private void paintIntersectingComponentsAbove(Graphics g) {
        Container parent = getParent();
        Component component = this;
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();

        g.translate(-tx, -ty);
        while (parent != null) {
            g.translate(parent.getAbsoluteX() + parent.getScrollX(),
                    parent.getAbsoluteY() + parent.getScrollY());
            parent.paintIntersecting(g, component, getAbsoluteX() + getScrollX(),
                    getAbsoluteY() + getScrollY(),
                    getWidth(), getHeight(), true);
            g.translate(-parent.getAbsoluteX() - parent.getScrollX(),
                    -parent.getAbsoluteY() - parent.getScrollY());
            component = parent;
            parent = parent.getParent();
        }
        g.translate(tx, ty);

    }

    /**
     * Paints the UI for the scrollbars on the component, this will be invoked only
     * for scrollable components. This method invokes the appropriate X/Y versions
     * to do all the work.
     * 
     * @param g the component graphics
     */
    protected void paintScrollbars(Graphics g) {
        if (isScrollableX()) {
            paintScrollbarX(g);
        }
        if (isScrollableY()) {
            paintScrollbarY(g);
        }
    }

    /**
     * Paints the UI for the scrollbar on the X axis, this method allows component
     * subclasses to customize the look of a scrollbar
     * 
     * @param g the component graphics
     */
    protected void paintScrollbarX(Graphics g) {
        float scrollW = getScrollDimension().getWidth();
        float block = ((float) getWidth()) / scrollW;
        float offset;
        if(getScrollX() + getWidth() == scrollW) {
            // normalize the offset to avoid rounding errors to the bottom of the screen
            offset = 1 - block;
        } else {
            offset = (((float) getScrollX() + getWidth()) / scrollW) - block;
        }
        UIManager.getInstance().getLookAndFeel().drawHorizontalScroll(g, this, offset, block);
    }

    /**
     * This method is used internally by the look and feel to implement the fading scrollbar
     * behavior.
     * 
     * @return the opacity of the scrollbar
     */
    public int getScrollOpacity() {
        if(Display.getInstance().shouldRenderSelection()) {
            scrollOpacity = 0xff;
        }
        return scrollOpacity;
    }

    /**
     * Paints the UI for the scrollbar on the Y axis, this method allows component
     * subclasses to customize the look of a scrollbar
     * 
     * @param g the component graphics
     */
    protected void paintScrollbarY(Graphics g) {
        float scrollH = getScrollDimension().getHeight();
        float block = ((float) getHeight()) / scrollH;
        float offset;
        if(getScrollY() + getHeight() == scrollH) {
            // normalize the offset to avoid rounding errors to the bottom of the screen
            offset = 1 - block;
        } else {
            offset = (((float) getScrollY() + getHeight()) / scrollH) - block;
        }
        UIManager.getInstance().getLookAndFeel().drawVerticalScroll(g, this, offset, block);
    }

    /**
     * Paints this component as a root by going to all the parent components and
     * setting the absolute translation based on coordinates and scroll status.
     * Restores translation when the painting is finished.
     * 
     * @param g the graphics to paint this Component on
     */
    final public void paintComponent(Graphics g) {
        repaintPending = false;
        paintComponent(g, true);
    }

    /**
     * Paints this component as a root by going to all the parent components and
     * setting the absolute translation based on coordinates and scroll status.
     * Restores translation when the painting is finished.
     * 
     * @param g the graphics to paint this Component on
     * @param background if true paints all parents background
     */
    final public void paintComponent(Graphics g, boolean background) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        Container parent = getParent();
        int translateX = 0;
        int translateY = 0;
        while (parent != null) {
            translateX += parent.getX();
            translateY += parent.getY();
            //if (parent.isScrollable()) {
            if (parent.isScrollableX()) {
                translateX -= parent.getScrollX();
            }
            if (parent.isScrollableY()) {
                translateY -= parent.getScrollY();
            }
            // since scrollability can translate everything... we should clip based on the
            // current scroll
            int parentX = parent.getAbsoluteX() + parent.getScrollX();
            if (isRTL()) {
                parentX += parent.getSideGap();
            }
            g.clipRect(parentX, parent.getAbsoluteY() + parent.getScrollY(),
                    parent.getWidth() - parent.getSideGap(), parent.getHeight() - parent.getBottomGap());

            parent = parent.getParent();
        }
        
        g.clipRect(translateX + getX(), translateY + getY(), getWidth(), getHeight());
        if (background) {
            paintBackgrounds(g);
        }

        g.translate(translateX, translateY);
        paintInternal(g);
        g.translate(-translateX, -translateY);

        paintGlassImpl(g);

        g.setClip(clipX, clipY, clipW, clipH);
    }

    /**
     * This method can be overriden by a component to draw on top of itself or its children
     * after the component or the children finished drawing in a similar way to the glass
     * pane but more refined per component
     *
     * @param g the graphics context
     */
    void paintGlassImpl(Graphics g) {
        if(parent != null) {
            parent.paintGlassImpl(g);
        }
    }

    private void drawPainters(com.sun.lwuit.Graphics g, Component par, Component c,
            int x, int y, int w, int h) {
        if(flatten && getWidth() > 0 && getHeight() > 0) {
            Image i = (Image)getClientProperty("$FLAT");
            int absX = getAbsoluteX() + getScrollX();
            int absY = getAbsoluteY() + getScrollY();
            if(i == null || i.getWidth() != getWidth() || i.getHeight() != getHeight()) {
                i = Image.createImage(getWidth(), getHeight());
                Graphics tg = i.getGraphics();
                //tg.translate(g.getTranslateX(), g.getTranslateY());
                drawPaintersImpl(tg, par, c, x, y, w, h);
                paintBackgroundImpl(tg);
                putClientProperty("$FLAT", i);
            }
            int tx = g.getTranslateX();
            int ty = g.getTranslateY();
            g.translate(-tx + absX, -ty + absY);
            g.drawImage(i, 0, 0);
            g.translate(tx - absX, ty - absY);
            return;
        }
        drawPaintersImpl(g, par, c, x, y, w, h);
    }

    private void drawPaintersImpl(com.sun.lwuit.Graphics g, Component par, Component c,
            int x, int y, int w, int h) {
        if (par == null) {
            return;
        } else {
            if (par.getStyle().getBgTransparency() != ((byte) 0xFF)) {
                drawPainters(g, par.getParent(), par, x, y, w, h);
            }
        }

        if (!par.isVisible()) {
            return;
        }

        int transX = par.getAbsoluteX() + par.getScrollX();
        int transY = par.getAbsoluteY() + par.getScrollY();

        g.translate(transX, transY);

        ((Container) par).paintIntersecting(g, c, x, y, w, h, false);

        if (par.isBorderPainted()) {
            Border b = par.getBorder();
            if (b.isBackgroundPainter()) {
                g.translate(-par.getX(), -par.getY());
                b.paintBorderBackground(g, par);
                b.paint(g, par);
                g.translate(par.getX() - transX, par.getY() - transY);
                return;
            }
        }
        Painter p = par.getStyle().getBgPainter();
        if (p != null) {
            Rectangle rect;
            if (painterBounds == null) {
                painterBounds = new Rectangle(0, 0, par.getWidth(), par.getHeight());
                rect = painterBounds;
            } else {
                rect = painterBounds;
                rect.getSize().setWidth(par.getWidth());
                rect.getSize().setHeight(par.getHeight());
            }
            p.paint(g, rect);
        }
        g.translate(-transX, -transY);
    }

    /**
     * Normally returns getStyle().getBorder() but some subclasses might use this 
     * to programmatically replace the border in runtime e.g. for a pressed border effect
     * 
     * @return the border that is drawn according to the current component state
     */
    protected Border getBorder() {
        return getStyle().getBorder();
    }

    /**
     * Paints the background of the component, invoked with the clipping region
     * and appropriate scroll translation.
     * 
     * @param g the component graphics
     */
    protected void paintBackground(Graphics g) {
        if(isFlatten()) {
            return;
        }
        paintBackgroundImpl(g);
    }

    private void paintBackgroundImpl(Graphics g) {
        if (isBorderPainted()) {
            Border b = getBorder();
            if (b != null && b.isBackgroundPainter()) {
                b.paintBorderBackground(g, this);
                return;
            }
        }
        if (getStyle().getBgPainter() != null) {
            getStyle().getBgPainter().paint(g, bounds);
        }
    }

    /**
     * This method paints the Component on the screen, it should be overriden
     * by subclasses to perform custom drawing or invoke the UI API's to let
     * the PLAF perform the rendering.
     * 
     * @param g the component graphics
     */
    public void paint(Graphics g) {
    }

    /**
     * Indicates whether the component should/could scroll by default a component
     * is not scrollable.
     * 
     * @return whether the component is scrollable
     */
    protected boolean isScrollable() {
        return isScrollableX() || isScrollableY();
    }

    /**
     * Indicates whether the component should/could scroll on the X axis
     * 
     * @return whether the component is scrollable on the X axis
     */
    public boolean isScrollableX() {
        return false;
    }

    /**
     * Indicates whether the component should/could scroll on the Y axis
     * 
     * @return whether the component is scrollable on the X axis
     */
    public boolean isScrollableY() {
        return false;
    }

    /**
     * Indicates the X position of the scrolling, this number is relative to the
     * component position and so a position of 0 would indicate the x position
     * of the component.
     * 
     * @return the X position of the scrolling
     */
    public int getScrollX() {
        return scrollX;
    }

    /**
     * Indicates the Y position of the scrolling, this number is relative to the
     * component position and so a position of 0 would indicate the x position
     * of the component.
     * 
     * @return the Y position of the scrolling
     */
    public int getScrollY() {
        return scrollY;
    }

    /**
     * Indicates the X position of the scrolling, this number is relative to the
     * component position and so a position of 0 would indicate the x position
     * of the component.
     * 
     * @param scrollX the X position of the scrolling
     */
    protected void setScrollX(int scrollX) {
        // the setter must always update the value regardless...
        this.scrollX = scrollX;
        if(!isSmoothScrolling() || !isTensileDragEnabled()) {
            this.scrollX = Math.min(this.scrollX, getScrollDimension().getWidth() - getWidth());
            this.scrollX = Math.max(this.scrollX, 0);
        }
        if (isScrollableX()) {
            repaint();
        }
    }

    /**
     * Indicates the X position of the scrolling, this number is relative to the
     * component position and so a position of 0 would indicate the x position
     * of the component.
     * 
     * @param scrollY the Y position of the scrolling
     */
    protected void setScrollY(int scrollY) {
        // the setter must always update the value regardless... 
        this.scrollY = scrollY;
        if(!isSmoothScrolling() || !isTensileDragEnabled()) {
            this.scrollY = Math.min(this.scrollY, getScrollDimension().getHeight() - getHeight());
            this.scrollY = Math.max(this.scrollY, 0);
        } 
        
        if (isScrollableY()) {
            repaint();
        }
    }

    /**
     * Returns the gap to be left for the bottom scrollbar on the X axis. This
     * method is used by layout managers to determine the room they should
     * leave for the scrollbar
     * 
     * @return the gap to be left for the bottom scrollbar on the X axis
     */
    public int getBottomGap() {
        if (isScrollableX() && isScrollVisible()) {
            return UIManager.getInstance().getLookAndFeel().getHorizontalScrollHeight();
        }
        return 0;
    }

    /**
     * Returns the gap to be left for the side scrollbar on the Y axis. This
     * method is used by layout managers to determine the room they should
     * leave for the scrollbar. (note: side scrollbar rather than left scrollbar
     * is used for a future version that would support bidi).
     * 
     * @return the gap to be left for the side scrollbar on the Y axis
     */
    public int getSideGap() {
        if (isScrollableY() && isScrollVisible()) {
            return UIManager.getInstance().getLookAndFeel().getVerticalScrollWidth();
        }
        return 0;
    }

    /**
     * Returns true if the given absolute coordinate is contained in the Component
     * 
     * @param x the given absolute x coordinate
     * @param y the given absolute y coordinate
     * @return true if the given absolute coordinate is contained in the 
     * Component; otherwise false
     */
    public boolean contains(int x, int y) {
        int absX = getAbsoluteX() + getScrollX();
        int absY = getAbsoluteY() + getScrollY();
        return (x >= absX && x < absX + getWidth() && y >= absY && y < absY + getHeight());
    }

    /**
     * Calculates the preferred size based on component content. This method is
     * invoked lazily by getPreferred size.
     * 
     * @return the calculated preferred size based on component content
     */
    protected Dimension calcPreferredSize() {
        Dimension d = new Dimension(0, 0);
        return d;
    }

    private Dimension preferredSize() {

        if (!sizeRequestedByUser && (shouldCalcPreferredSize || preferredSize == null)) {
            shouldCalcPreferredSize = false;
            preferredSize = calcPreferredSize();
        }
        return preferredSize;

    }

    /**
     * Returns the component bounds which is sometimes more convenient than invoking
     * getX/Y/Width/Height. Bounds are relative to parent container.<br>
     * Changing values within the bounds can lead to unpredicted behavior.
     * 
     * @see #getX
     * @see #getY
     * @return the component bounds
     */
    protected Rectangle getBounds() {
        return bounds;
    }

    /**
     * Returns the component bounds for scrolling which might differ from the getBounds for large components
     * e.g. list.
     *
     * @see #getX
     * @see #getY
     * @return the component bounds
     */
    protected Rectangle getVisibleBounds() {
        return bounds;
    }

    /**
     * Returns true if this component can receive focus and is enabled
     * 
     * @return true if this component can receive focus; otherwise false
     */
    public boolean isFocusable() {
        return focusable && enabled && isVisible();
    }

    /**
     * A simple setter to determine if this Component can get focused
     * 
     * @param focusable indicate whether this component can get focused
     */
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    /**
     * Indicates the values within the component have changed and preferred 
     * size should be recalculated
     * 
     * @param shouldCalcPreferredSize indicate whether this component need to 
     * recalculate his preferred size
     */
    public void setShouldCalcPreferredSize(boolean shouldCalcPreferredSize) {
        if (!shouldCalcScrollSize) {
            this.shouldCalcScrollSize = shouldCalcPreferredSize;
        }
        if (shouldCalcPreferredSize != this.shouldCalcPreferredSize) {
            this.shouldCalcPreferredSize = shouldCalcPreferredSize;
            this.shouldCalcScrollSize = shouldCalcPreferredSize;
            if (shouldCalcPreferredSize && getParent() != null) {
                this.shouldCalcPreferredSize = shouldCalcPreferredSize;
                getParent().setShouldLayout(shouldCalcPreferredSize);
            }
        }
    }


    /**
     * Prevents key events from being grabbed for focus traversal. E.g. a list component
     * might use the arrow keys for internal navigation so it will switch this flag to
     * true in order to prevent the focus manager from moving to the next component.
     * 
     * @return true if key events are being used for focus traversal
     * ; otherwise false
     */
    public boolean handlesInput() {
        return handlesInput;
    }

    /**
     * Prevents key events from being grabbed for focus traversal. E.g. a list component
     * might use the arrow keys for internal navigation so it will switch this flag to
     * true in order to prevent the focus manager from moving to the next component.
     * 
     * @param handlesInput indicates whether key events can be grabbed for 
     * focus traversal
     */
    public void setHandlesInput(boolean handlesInput) {
        this.handlesInput = handlesInput;
    }

    /**
     * Returns true if the component has focus
     * 
     * @return true if the component has focus; otherwise false
     * @see #setFocus
     */
    public boolean hasFocus() {
        return focused;
    }

    /**
     * This flag doesn't really give focus, its a state that determines
     * what colors from the Style should be used when painting the component.
     * Actual focus is determined by the parent form
     * 
     * @param focused sets the state that determines what colors from the 
     * Style should be used when painting a focused component
     * 
     * @see #requestFocus
     */
    public void setFocus(boolean focused) {
        this.focused = focused;
    }

    /**
     * Returns the Component Form or null if this Component
     * is not added yet to a form
     * 
     * @return the Component Form
     */
    public Form getComponentForm() {
        Form retVal = null;
        Component parent = getParent();
        if (parent != null) {
            retVal = parent.getComponentForm();
        }
        return retVal;
    }

    /**
     * Repaint the given component to the screen
     * 
     * @param cmp the given component on the screen
     */
    void repaint(Component cmp) {
        if (isCellRenderer() || cmp.getWidth() <= 0 || cmp.getHeight() <= 0 || paintLockImage != null) {
            return;
        }
        // null parent repaint can happen when a component is removed and modified which
        // is common for a popup
        Component parent = getParent();
        if (parent != null) {
            parent.repaint(cmp);
        }
    }

    /**
     * Repaint this Component, the repaint call causes a callback of the paint
     * method on the event dispatch thread.
     * 
     * @see Display
     */
    public void repaint() {
        repaintPending = true;
        if (dirtyRegion != null) {
            setDirtyRegion(null);
        }
        repaint(this);
    }

    /**
     * Repaints a specific region within the component
     * 
     * @param x boundry of the region to repaint
     * @param y boundry of the region to repaint
     * @param w boundry of the region to repaint
     * @param h boundry of the region to repaint
     */
    public void repaint(int x, int y, int w, int h) {
        Rectangle rect;
        synchronized (dirtyRegionLock) {
            if (dirtyRegion == null) {
                if(repaintPending) {
                    return;
                }
                rect = new Rectangle(x, y, w, h);
                setDirtyRegion(rect);
            } else if (dirtyRegion.getX() != x || dirtyRegion.getY() != y ||
                    dirtyRegion.getSize().getWidth() != w || dirtyRegion.getSize().getHeight() != h) {
                rect = new Rectangle(dirtyRegion);
                Dimension size = rect.getSize();

                int x1 = Math.min(rect.getX(), x);
                int y1 = Math.min(rect.getY(), y);

                int x2 = Math.max(x + w, rect.getX() + size.getWidth());
                int y2 = Math.max(y + h, rect.getY() + size.getHeight());

                rect.setX(x1);
                rect.setY(y1);
                size.setWidth(x2 - x1);
                size.setHeight(y2 - y1);
                setDirtyRegion(rect);
            }
        }

        repaint(this);
    }

    /**
     * If this Component is focused this method is invoked when the user presses
     * and holds the key
     * 
     * @param keyCode the key code value to indicate a physical key.
     */
    protected void longKeyPress(int keyCode) {
    }

    /**
     * If this Component is focused, the key pressed event
     * will call this method
     * 
     * @param keyCode the key code value to indicate a physical key.
     */
    public void keyPressed(int keyCode) {
    }

    /**
     * If this Component is focused, the key released event
     * will call this method
     * 
     * @param keyCode the key code value to indicate a physical key.
     */
    public void keyReleased(int keyCode) {
    }

    /**
     * If this Component is focused, the key repeat event
     * will call this method.
     * 
     * @param keyCode the key code value to indicate a physical key.
     */
    public void keyRepeated(int keyCode) {
        keyPressed(keyCode);
        keyReleased(keyCode);
    }

    /**
     * Allows defining the physics for the animation motion behavior directly 
     * by plugging in an alternative motion object
     * 
     * @param motion new motion object
     */
    private void setAnimationMotion(Motion motion) {
        animationMotion = motion;
    }

    /**
     * Allows defining the physics for the animation motion behavior directly 
     * by plugging in an alternative motion object
     * 
     * @return the component motion object
     */
    private Motion getAnimationMotion() {
        return animationMotion;
    }

    /**
     * Scroll animation speed in milliseconds allowing a developer to slow down or accelerate
     * the smooth animation mode
     * 
     * @return scroll animation speed in milliseconds
     */
    public int getScrollAnimationSpeed() {
        return animationSpeed;
    }

    /**
     * Scroll animation speed in milliseconds allowing a developer to slow down or accelerate
     * the smooth animation mode
     * 
     * @param animationSpeed scroll animation speed in milliseconds
     */
    public void setScrollAnimationSpeed(int animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    /**
     * Indicates that scrolling through the component should work as an animation
     * 
     * @return whether this component use smooth scrolling
     */
    public boolean isSmoothScrolling() {
        return smoothScrolling;
    }

    /**
     * Indicates that scrolling through the component should work as an animation
     * 
     * @param smoothScrolling indicates if a component uses smooth scrolling
     */
    public void setSmoothScrolling(boolean smoothScrolling) {
        this.smoothScrolling = smoothScrolling;
    }

    /**
     * Invoked for devices where the pointer can hover without actually clicking
     * the display. This is true for PC mouse pointer as well as some devices such
     * as the BB storm.
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerHover(int[] x, int[] y) {
        pointerDragged(x, y);
    }

    void clearDrag() {
        draggedMotion = null;
        Component parent = getParent();
        if(parent != null){
            parent.clearDrag();
        }
    }

    /**
     * Invoked for devices where the pointer can hover without actually clicking
     * the display. This is true for PC mouse pointer as well as some devices such
     * as the BB storm.
     *
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerHoverReleased(int[] x, int[] y) {
        pointerReleaseImpl(x[0], y[0]);
    }

    /**
     * Invoked for devices where the pointer can hover without actually clicking
     * the display. This is true for PC mouse pointer as well as some devices such
     * as the BB storm.
     *
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerHoverPressed(int[] x, int[] y) {
        dragActivated = false;
        clearDrag();
    }

    /**
     * If this Component is focused, the pointer dragged event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerDragged(int[] x, int[] y) {
        pointerDragged(x[0], y[0]);
    }

    /**
     * This method returns an image representing the dragged component, it can be overriden by subclasses to customize the look
     * of the image, the image will be overlayed on top of the form during a drag and drop operation
     * 
     * @return an image
     */
    protected Image getDragImage() {
        Image draggedImage = Image.createImage(getWidth(), getHeight());
        Graphics g = draggedImage.getGraphics();

        // choose a rare color
        g.setColor(0xff7777);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.translate(-getX(), -getY());
        paint(g);
        g.translate(getX(), getY());

        // remove all occurences of the rare color
        draggedImage = draggedImage.modifyAlpha((byte)0x55, 0xff7777);
        return draggedImage;
    }

    /**
     * Invoked on the focus component to let it know that drag has started on the parent container
     * for the case of a component that doesn't support scrolling
     */
    protected void dragInitiated() {
    }

    void drawDraggedImage(Graphics g) {
        if(dragImage == null) {
            dragImage = getDragImage();
        }
        drawDraggedImage(g, dragImage, lastScrollX, lastScrollY);
    }

    /**
     * Draws the given image at x/y, this method can be overriden to draw additional information such as positive
     * or negative drop indication
     *
     * @param g the graphics context
     * @param img the image
     * @param x x position
     * @param y y position
     */
    protected void drawDraggedImage(Graphics g, Image img, int x, int y) {
        g.drawImage(img, x - getWidth() / 2, y - getHeight() / 2);
    }

    
    /**
     * This method allows a component to indicate if it is a drop target for the given component at the given x/y location
     * (in component coordiate space). This method can also update the drop tagets appearance to indicate the
     * drop location.
     * 
     * @param dragged the component being dragged
     * @param x the x location over the component
     * @param y the y location over the component
     * @return true if a drop at this location will be successful
     */
    protected boolean draggingOver(Component dragged, int x, int y) {
        return dropTarget;
    }

    /**
     * This callback method indicates that a component drag has just entered this component
     *
     * @param dragged the component being dragged
     */
    protected void dragEnter(Component dragged) {
    }

    /**
     * This callback method provides an indication for a drop target that a drag operation is exiting the bounds of
     * this component and it should clear all relevant state if such state exists. E.g. if a component provides
     * drop indication visuaization in draggingOver this visualization should be cleared..
     *
     * @param dragged the component being dragged
     */
    protected void dragExit(Component dragged) {
    }

    /**
     * Performs a drop operation of the component at the given X/Y location in coordinate space, this method
     * should be overriden by subclasses to perform all of the logic related to moving a component, by default
     * this method does nothing and so dragging a component and dropping it has no effect
     *
     * @param dragged the component being dropped
     * @param x the x coordinate of the drop
     * @param y the y coordinate of the drop
     */
    public void drop(Component dragged, int x, int y) {
    }

    /**
     * Finds the drop target in the given screen coordinates
     * 
     * @param source  the component being dragged
     * @param x the screen x coordinate
     * @param y the screen y coordinate
     * @return a component drop target or null if no drop target is available at that coordinate
     */
    private Component findDropTarget(Component source, int x, int y) {
        Component c = getComponentForm().getComponentAt(x, y);
        while(c != null) {
            if(c.isDropTarget() && c.draggingOver(source, x - c.getAbsoluteX() - c.getScrollX(), y- c.getAbsoluteY() - c.getScrollY())) {
                return c;
            }
            c = c.getParent();
        }
        return null;
    }

    /**
     * If this Component is focused, the pointer dragged event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerDragged(int x, int y) {
        if(dragAndDropInitialized) {
            Form p = getComponentForm();
            if (!dragActivated) {
                dragActivated = true;
                setVisible(false);
                p.setDraggedComponent(this);
            }
            Component dropTo = findDropTarget(this, x, y);
            if(dropTargetComponent != dropTo) {
                if(dropTargetComponent != null) {
                    dropTargetComponent.dragExit(this);
                }
                dropTargetComponent = dropTo;
                if(dropTargetComponent != null) {
                    dropTargetComponent.dragEnter(this);
                }
            }

            // we repaint twice to create an intersection of the old and new position
            p.repaint(lastScrollX - getWidth() / 2, lastScrollY - getHeight() / 2, getWidth(), getHeight());
            p.repaint(x - getWidth() / 2, y - getHeight() / 2, getWidth(), getHeight());
            lastScrollX = x;
            lastScrollY = y;
            getParent().scrollRectToVisible(x, y, getWidth(), getHeight(), getParent());
            return;
        }
        if (isScrollable() && isSmoothScrolling()) {
            if (!dragActivated) {
                dragActivated = true;
                lastScrollY = y;
                lastScrollX = x;
                Form p = getComponentForm();
                p.setDraggedComponent(this);
                p.registerAnimatedInternal(this);
                Component fc = p.getFocused();
                if(fc != null && fc != this) {
                    fc.dragInitiated();
                }
            }

            // we drag inversly to get a feel of grabbing a physical screen
            // and pulling it in the reverse direction of the drag
            if (isScrollableY()) {
                int tl;
                if(getTensileLength() > -1) {
                    tl = getTensileLength();
                } else {
                    tl = getHeight() / 2;
                }
                if(!isSmoothScrolling() || !isTensileDragEnabled()) {
                    tl = 0;
                }
                int scroll = getScrollY() + (lastScrollY - y);
                if(isAlwaysTensile() && getScrollDimension().getHeight() <= getHeight()) {
                    if (scroll >= -tl && scroll < getHeight() + tl) {
                        setScrollY(scroll);
                    }
                } else {
                    if (scroll >= -tl && scroll < getScrollDimension().getHeight() - getHeight() + tl) {
                        setScrollY(scroll);
                    }
                }
            }
            if (isScrollableX()) {
                int tl;
                if(getTensileLength() > -1) {
                    tl = getTensileLength();
                } else {
                    tl = getWidth() / 2;
                }
                if(!isSmoothScrolling() || !isTensileDragEnabled()) {
                    tl= 0;
                }
                int scroll = getScrollX() + (lastScrollX - x);
                if (scroll >= -tl && scroll < getScrollDimension().getWidth() - getWidth() + tl) {
                    setScrollX(scroll);
                }
            }
            lastScrollY = y;
            lastScrollX = x;
        } else {
            //try to find a scrollable element until you reach the Form
            Component parent = getParent();
            if (!(parent instanceof Form)) {
                parent.pointerDragged(x, y);
            }
        }
    }

    private void initScrollMotion() {
        // the component might not be registered for animation if it started off 
        // as smaller than the screen and grew (e.g. by adding components to the container
        // once it is visible).
        Form f = getComponentForm();
        if (f != null) {
            f.registerAnimatedInternal(this);
        }

        Motion m = Motion.createLinearMotion(initialScrollY, destScrollY, getScrollAnimationSpeed());
        setAnimationMotion(m);
        m.start();
    }

    /**
     * If this Component is focused, the pointer pressed event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerPressed(int[] x, int[] y) {
        dragActivated = false;
        pointerPressed(x[0], y[0]);
        scrollOpacity = 0xff;
    }

    /**
     * This method allows a developer to define only a specific portion of a component as draggable
     * by default it returns true if the component is defined as "draggable"
     * 
     * @param x the x coordinate relative to the component
     * @param y the y coordinate relative to the component
     * @return true if a press in this point might indicate the desire to begin a drag operation
     */
    protected boolean isDragAndDropOperation(int x, int y) {
        return draggable;
    }

    /**
     * If this Component is focused, the pointer pressed event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerPressed(int x, int y) {
        clearDrag();
    }

    void initDragAndDrop(int x, int y) {
        dragAndDropInitialized = isDragAndDropOperation(x, y);
    }

    /**
     * If this Component is focused, the pointer released event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerReleased(int[] x, int[] y) {
        pointerReleased(x[0], y[0]);
    }

    /**
     * If this Component is focused this method is invoked when the user presses
     * and holds the pointer on the Component
     * 
     */
    protected void longPointerPress(int x, int y) {
    }

    /**
     * If this Component is focused, the pointer released event
     * will call this method
     * 
     * @param x the pointer x coordinate
     * @param y the pointer y coordinate
     */
    public void pointerReleased(int x, int y) {
        pointerReleaseImpl(x, y);
        scrollOpacity = 0xff;
    }

    /**
     * Indicates whether tensile drag (dragging beyond the boundry of the component and
     * snapping back) is enabled for this component.
     *
     * @param tensileDragEnabled true to enable tensile drag
     */
    public void setTensileDragEnabled(boolean tensileDragEnabled) {
        this.tensileDragEnabled = tensileDragEnabled;
    }

    /**
     * Indicates whether tensile drag (dragging beyond the boundry of the component and
     * snapping back) is enabled for this component.
     *
     * @return true when tensile drag is enabled
     */
    public boolean isTensileDragEnabled() {
        return tensileDragEnabled;
    }

    void startTensile(int offset, int dest) {
        if(tensileDragEnabled) {
            draggedMotion = Motion.createSplineMotion(offset, dest, 150);
            draggedMotion.start();
        } else {
            draggedMotion = Motion.createLinearMotion(offset, dest, 0);
            draggedMotion.start();
        }
        // just to be sure, there are some cases where this doesn't work as expected
        Form p = getComponentForm();
        if(p != null) {
            p.registerAnimatedInternal(this);
        }
    }

    private boolean chooseScrollXOrY(int x , int y) {
        boolean ix = isScrollableX();
        boolean iy = isScrollableY();
        if(ix && iy) {
            Form parent = getComponentForm();
            return Math.abs(parent.initialPressX - x) > Math.abs(parent.initialPressY - y);
        }
        if(ix) {
            return true;
        }
        return false;
    }

    void dragFinished(int x, int y) {
        if(dragAndDropInitialized && dragActivated) {
            Form p = getComponentForm();
            p.setDraggedComponent(null);
            Component dropTo = findDropTarget(this, x, y);
            if(dropTargetComponent != dropTo) {
                if(dropTargetComponent != null) {
                    dropTargetComponent.dragExit(this);
                }
                dropTargetComponent = dropTo;
                if(dropTargetComponent != null) {
                    dropTargetComponent.dragEnter(this);
                }
            }
            if(dropTargetComponent != null) {
                p.repaint(x, y, getWidth(), getHeight());
                getParent().scrollRectToVisible(x, y, getWidth(), getHeight(), getParent());
                dropTargetComponent.drop(this, x, y);
            } else {
                p.repaint();
            }
            setVisible(true);
            dragImage = null;
            dropTargetComponent = null;
        }
        if(UIManager.getInstance().getLookAndFeel().isFadeScrollBar() && isScrollable()) {
            Form frm = getComponentForm();
            if(frm != null) {
                frm.registerAnimatedInternal(this);
            }
        }
    }

    private void pointerReleaseImpl(int x, int y) {
        if (dragActivated) {
            int scroll = scrollY;
            dragActivated = false;
            boolean shouldScrollX = chooseScrollXOrY(x, y);
            if(shouldScrollX){
                scroll = scrollX;
                if (scroll < 0) {
                    startTensile(scroll, 0);
                    return;
                } else {
                    if(scroll > getScrollDimension().getWidth() - getWidth()) {
                        startTensile(scroll, getScrollDimension().getWidth() - getWidth());
                        return;
                    }
                }
            } else {
                if (scroll < 0) {
                    startTensile(scroll, 0);
                    return;
                } else {
                    if(scroll > getScrollDimension().getHeight() - getHeight()) {
                        startTensile(scroll, getScrollDimension().getHeight() - getHeight());
                        return;
                    }
                }
            }
            float speed = Display.getInstance().getDragSpeed(!shouldScrollX);
            int tl;
            if(getTensileLength() > -1) {
                tl = getTensileLength();
            } else {
                tl = getWidth() / 2;
            }
            if(!isTensileDragEnabled()) {
                tl = 0;
            }
            if(!shouldScrollX) {
                if(speed < 0) {
                    draggedMotion = Motion.createFrictionMotion(scroll, -tl, speed, 0.0004f);
                } else {
                    draggedMotion = Motion.createFrictionMotion(scroll, getScrollDimension().getHeight() - 
                            getHeight() + tl, speed, 0.0004f);
                }
            } else {
                if(speed < 0) {
                    draggedMotion = Motion.createFrictionMotion(scroll, -tl, speed, 0.0004f);
                } else {
                    draggedMotion = Motion.createFrictionMotion(scroll, getScrollDimension().getWidth() -
                            getWidth() + tl, speed, 0.0004f);
                }
            }
 
            draggedMotion.start();
        }
    }

    /**
     * Returns the Component Style allowing us to manipulate the look of the 
     * component
     * 
     * @return the component Style object
     */
    public Style getStyle() {
        if (unSelectedStyle == null) {
            initStyle();
        }

        if(hasLead) {
            Component lead = getLeadComponent();
            if(lead != null) {
                if(!lead.isEnabled()) {
                    return getDisabledStyle();
                }

                if(lead.isPressedStyle()) {
                    return getPressedStyle();
                }

                if (lead.hasFocus() && Display.getInstance().shouldRenderSelection(this)) {
                    return getSelectedStyle();
                }
            }
            return unSelectedStyle;
        }

        if(!isEnabled()) {
            return getDisabledStyle();
        }

        if(isPressedStyle()) {
            return getPressedStyle();
        }

        if (hasFocus() && Display.getInstance().shouldRenderSelection(this)) {
            return getSelectedStyle();
        }
        return unSelectedStyle;
    }

    boolean isPressedStyle() {
        return false;
    }

    /**
     * Returns the Component Style for the pressed state allowing us to manipulate
     * the look of the component when it is pressed
     *
     * @return the component Style object
     */
    public Style getPressedStyle() {
        if (pressedStyle == null) {
            pressedStyle = UIManager.getInstance().getComponentCustomStyle(getUIID(), "press");
            pressedStyle.addStyleListener(this);
            if(pressedStyle.getBgPainter() == null){
                pressedStyle.setBgPainter(new BGPainter());
            }
        }
        return pressedStyle;
    }

    /**
     * Sets the Component Style for the pressed state allowing us to manipulate
     * the look of the component when it is pressed
     *
     * @param style the component Style object
     */
    public void setPressedStyle(Style style) {
        if (pressedStyle != null) {
            pressedStyle.removeStyleListener(this);
        }
        pressedStyle = style;
        pressedStyle.addStyleListener(this);
        if (pressedStyle.getBgPainter() == null) {
            pressedStyle.setBgPainter(new BGPainter());
        }
        setShouldCalcPreferredSize(true);
        checkAnimation();
    }

    /**
     * Returns the Component Style for the unselected mode allowing us to manipulate
     * the look of the component
     *
     * @return the component Style object
     */
    public Style getUnselectedStyle() {
        if (unSelectedStyle == null) {
            initStyle();
        }
        return unSelectedStyle;
    }

    /**
     * Returns the Component Style for the selected state allowing us to manipulate
     * the look of the component when it owns focus
     *
     * @return the component Style object
     */
    public Style getSelectedStyle() {
        if (selectedStyle == null) {
            selectedStyle = UIManager.getInstance().getComponentSelectedStyle(getUIID());
            selectedStyle.addStyleListener(this);
            if (selectedStyle.getBgPainter() == null) {
                selectedStyle.setBgPainter(new BGPainter());
            }
        }
        return selectedStyle;
    }

    /**
     * Returns the Component Style for the disabled state allowing us to manipulate
     * the look of the component when its disabled
     *
     * @return the component Style object
     */
    public Style getDisabledStyle() {
        if (disabledStyle == null) {
            disabledStyle = UIManager.getInstance().getComponentCustomStyle(getUIID(), "dis");
            disabledStyle.addStyleListener(this);
            if (disabledStyle.getBgPainter() == null) {
                disabledStyle.setBgPainter(new BGPainter());
            }
        }
        return disabledStyle;
    }


    /**
     * Changes the Component Style by replacing the Component Style with the given Style
     * 
     * @param style the component Style object
     */
    public void setUnselectedStyle(Style style) {
        if (this.unSelectedStyle != null) {
            this.unSelectedStyle.removeStyleListener(this);
        }
        this.unSelectedStyle = style;
        this.unSelectedStyle.addStyleListener(this);
        if (this.unSelectedStyle.getBgPainter() == null) {
            this.unSelectedStyle.setBgPainter(new BGPainter());
        }
        setShouldCalcPreferredSize(true);
        checkAnimation();
    }

    /**
     * Changes the Component selected Style by replacing the Component Style with the given Style
     *
     * @param style the component Style object
     */
    public void setSelectedStyle(Style style) {
        if (this.selectedStyle != null) {
            this.selectedStyle.removeStyleListener(this);
        }
        this.selectedStyle = style;
        this.selectedStyle.addStyleListener(this);
        if (this.selectedStyle.getBgPainter() == null) {
            this.selectedStyle.setBgPainter(new BGPainter());
        }
        setShouldCalcPreferredSize(true);
        checkAnimation();
    }

    /**
     * Changes the Component disalbed Style by replacing the Component Style with the given Style
     *
     * @param style the component Style object
     */
    public void setDisabledStyle(Style style) {
        if (this.disabledStyle != null) {
            this.disabledStyle.removeStyleListener(this);
        }
        this.disabledStyle = style;
        this.disabledStyle.addStyleListener(this);
        if (this.disabledStyle.getBgPainter() == null) {
            this.disabledStyle.setBgPainter(new BGPainter());
        }
        setShouldCalcPreferredSize(true);
        checkAnimation();
    }

    /**
     * Allows subclasses to create their own custom style types and install the background painter into them
     * 
     * @param s the custom style
     */
    protected void installDefaultPainter(Style s) {
        if(s.getBgPainter() == null) {
            s.setBgPainter(new BGPainter(s));
        }
    }

    /**
     * Changes the current component to the focused component, will work only
     * for a component that belongs to a parent form.
     */
    public void requestFocus() {
        Form rootForm = getComponentForm();
        if (rootForm != null) {
            rootForm.requestFocus(this);
        }
    }

    /**
     * Overriden to return a useful value for debugging purposes
     * 
     * @return a string representation of this component
     */
    public String toString() {
        String className = getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        return className + "[" + paramString() + "]";
    }

    /**
     * Returns a string representing the state of this component. This 
     * method is intended to be used only for debugging purposes, and the 
     * content and format of the returned string may vary between 
     * implementations. The returned string may be empty but may not be 
     * <code>null</code>.
     * 
     * @return  a string representation of this component's state
     */
    protected String paramString() {
        return "x=" + getX() + " y=" + getY() + " width=" + getWidth() + " height=" + getHeight();
    }

    /**
     * Makes sure the component is up to date with the current style object
     */
    public void refreshTheme() {
        refreshTheme(getUIID());
    }

    /**
     * Makes sure the component is up to date with the given UIID
     * 
     * @param id The Style Id to update the Component with
     */
    protected void refreshTheme(String id) {
        Style unSelected = getUnselectedStyle();
        setUnselectedStyle(mergeStyle(unSelected, UIManager.getInstance().getComponentStyle(id)));

        if (selectedStyle != null) {
            setSelectedStyle(mergeStyle(selectedStyle, UIManager.getInstance().getComponentSelectedStyle(id)));
        }
        if (disabledStyle != null) {
            setDisabledStyle(mergeStyle(disabledStyle, UIManager.getInstance().getComponentCustomStyle(id, "dis")));
        }
        if(pressedStyle != null) {
            setPressedStyle(mergeStyle(pressedStyle, UIManager.getInstance().getComponentCustomStyle(id, "press")));
        }
        checkAnimation();
        UIManager.getInstance().getLookAndFeel().bind(this);
    }

    Style mergeStyle(Style toMerge, Style newStyle) {
        if (toMerge.isModified()) {
            toMerge.merge(newStyle);
            return toMerge;
        } else {
            return newStyle;
        }

    }

    /**
     * Indicates whether we are in the middle of a drag operation, this method allows
     * developers overriding the pointer released events to know when this is a drag
     * operaton.
     * 
     * @return true if we are in the middle of a drag; otherwise false
     */
    protected boolean isDragActivated() {
        return dragActivated;
    }

    void setDragActivated(boolean dragActivated) {
        this.dragActivated = dragActivated;
    }

    void checkAnimation() {
        Image bgImage = getStyle().getBgImage();
        if (bgImage != null && bgImage.isAnimation()) {
            Form pf = getComponentForm();
            if (pf != null) {
                // animations are always running so the internal animation isn't
                // good enough. We never want to stop this sort of animation
                pf.registerAnimated(this);
            }
        } else {
            Painter p = getStyle().getBgPainter();
            if(p.getClass() != BGPainter.class && p instanceof Animation) {
                Form pf = getComponentForm();
                if (pf != null) {
                    pf.registerAnimated(this);
                }
            } else {
                if (scrollOpacity == 0xff && isScrollable() && UIManager.getInstance().getLookAndFeel().isFadeScrollBar()) {
                    // trigger initial fade process on a fresh view.
                    Form pf = getComponentForm();
                    if (pf != null) {
                        pf.registerAnimatedInternal(this);
                    }
                }
            }
        } 
    }

    void deregisterAnimatedInternal() {
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimatedInternal(this);
        }
    }
    
    /**
     * This method should be implemented correctly by subclasses to make snap to grid functionality work
     * as expected. Returns the ideal grid Y position closest to the current Y position.
     * 
     * @return a valid Y position in the grid
     */
    protected int getGridPosY() {
        return getScrollY();
    }

    /**
     * This method should be implemented correctly by subclasses to make snap to grid functionality work
     * as expected. Returns the ideal grid X position closest to the current X position.
     *
     * @return a valid Y position in the grid
     */
    protected int getGridPosX() {
        return getScrollX();
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        Image bgImage = getStyle().getBgImage();
        boolean animateBackground = bgImage != null && bgImage.isAnimation() && bgImage.animate();
        Motion m = getAnimationMotion();

        // perform regular scrolling
        if (m != null && destScrollY != -1 && destScrollY != getScrollY()) {
            // change the variable directly for efficiency both in removing redundant
            // repaints and scroll checks
            setScrollY(m.getValue());
            if (destScrollY == scrollY) {
                destScrollY = -1;
                deregisterAnimatedInternal();
            }
            return true;
        }

        // perform the dragging motion if exists
        if (draggedMotion != null) {
            // change the variable directly for efficiency both in removing redundant
            // repaints and scroll checks
            int dragVal = draggedMotion.getValue();

            // this can't be a part of the parent if since we need the last value to arrive
            if(draggedMotion.isFinished()) {
                if(dragVal < 0) {
                    startTensile(dragVal, 0);
                } else {
                    if (isScrollableY()) {
                        int edge = (getScrollDimension().getHeight() - getHeight());
                        if(dragVal > edge && edge > 0) {
                            startTensile(dragVal, getScrollDimension().getHeight() - getHeight());
                        } else {
                            if(snapToGrid && getScrollY() < edge && getScrollY() > 0) {
                                int dest = getGridPosY();
                                int scroll = getScrollY();
                                if(dest != scroll) {
                                    startTensile(scroll, dest);
                                } else {
                                    draggedMotion = null;
                                }
                            } else {
                                draggedMotion = null;
                            }
                        }
                    } else {
                        int edge = getScrollDimension().getWidth() - getWidth();
                        if(dragVal > edge) {
                            startTensile(dragVal, getScrollDimension().getWidth() - getWidth());
                        } else {
                            if(snapToGrid && getScrollX() < edge && getScrollX() > 0) {
                                int dest = getGridPosX();
                                int scroll = getScrollX();
                                if(dest != scroll) {
                                    startTensile(scroll, dest);
                                } else {
                                    draggedMotion = null;
                                }
                            } else {
                                draggedMotion = null;
                            }
                        }
                    }
                }
            }

            if (isScrollableY()) {
                scrollY = dragVal;
                return true;
            } else {
                scrollX = dragVal;
                return true;
            }
        }

        if (animateBackground) {
            if(bgImage instanceof StaticAnimation) {
                Rectangle dirty = ((StaticAnimation) bgImage).getDirtyRegion();
                if (dirty != null) {
                    dirty.setX(getAbsoluteX());
                    dirty.setY(getAbsoluteY() + dirty.getY());
                }
                setDirtyRegion(dirty);
            }
        } else {
            Painter bgp = getStyle().getBgPainter();
            animateBackground = bgp != null && bgp.getClass() != BGPainter.class && bgp instanceof Animation && (bgp != this) && ((Animation)bgp).animate();
        }

        if(UIManager.getInstance().getLookAndFeel().isFadeScrollBar()) {
            if(scrollOpacity > 0 && !dragActivated) {
                scrollOpacity = Math.max(0, scrollOpacity - scrollOpacityChangeSpeed);
                return true;
            }
        }

        if(!animateBackground && (destScrollY == -1 || destScrollY == scrollY) &&
                !animateBackground && m == null && draggedMotion == null &&
                !dragActivated) {
            tryDeregisterAnimated();
        }

        return animateBackground;
    }

    /**
     * Removes the internal animation. This method may be overriden by sublcasses to block automatic removal
     */
    void tryDeregisterAnimated() {
        deregisterAnimatedInternal();
    }

    /**
     * Makes sure the component is visible in the scroll if this container 
     * is scrollable
     * 
     * @param rect the rectangle that need to be visible
     * @param coordinateSpace the component according to whose coordinates 
     * rect is defined. Rect's x/y are relative to that component 
     * (they are not absolute).
     */
    protected void scrollRectToVisible(Rectangle rect, Component coordinateSpace) {
        scrollRectToVisible(rect.getX(), rect.getY(), 
                rect.getSize().getWidth(), rect.getSize().getHeight(), coordinateSpace);
    }

    /**
     * Makes sure the component is visible in the scroll if this container 
     * is scrollable
     *
     * @param x 
     * @param y 
     * @param width 
     * @param height  
     * @param coordinateSpace the component according to whose coordinates 
     * rect is defined. Rect's x/y are relative to that component 
     * (they are not absolute).
     */
    public void scrollRectToVisible(int x, int y, int width, int height, Component coordinateSpace) {
        if (isScrollable()) {
            int scrollPosition = getScrollY();
            Style s = getStyle();
            int w = getWidth() - s.getPadding(isRTL(), LEFT) - s.getPadding(isRTL(), RIGHT);
            int h = getHeight() - s.getPadding(false, TOP) - s.getPadding(false, BOTTOM);

            Rectangle view;
            if (isSmoothScrolling() && destScrollY > -1) {
                view = new Rectangle(getScrollX(), destScrollY, w, h);
            } else {
                view = new Rectangle(getScrollX(), getScrollY(), w, h);
            }

            int relativeX = x;
            int relativeY = y;

            // component needs to be in absolute coordinates...
            Container parent = null;
            if (coordinateSpace != null) {
                parent = coordinateSpace.getParent();
            }
            if (parent == this) {
                if (view.contains(x, y, width, height)) {
                    return;
                }
            } else {
                while (parent != this) {
                    // mostly a special case for list
                    if (parent == null) {
                        relativeX = x;
                        relativeY = y;
                        break;
                    }
                    relativeX += parent.getX();
                    relativeY += parent.getY();
                    parent = parent.getParent();
                }
                if (view.contains(relativeX, relativeY, width, height)) {
                    return;
                }
            }
            if (isScrollableX()) {
                if (getScrollX() > relativeX) {
                    setScrollX(relativeX);
                }
                int rightX = relativeX + width - 
                        s.getPadding(LEFT) - s.getPadding(RIGHT);
                if (getScrollX() + w < rightX) {
                    setScrollX(getScrollX() + (rightX - (getScrollX() + w)));
                } else {
                    if (getScrollX() > relativeX) {
                        setScrollX(relativeX);
                    }
                }
            }

            if (isScrollableY()) {
                if (getScrollY() > relativeY) {
                    scrollPosition = relativeY;
                }
                int bottomY = relativeY + height - 
                        s.getPadding(TOP) - s.getPadding(BOTTOM);
                if (getScrollY() + h < bottomY) {
                    scrollPosition = getScrollY() + (bottomY - (getScrollY() + h));
                } else {
                    if (getScrollY() > relativeY) {
                        scrollPosition = relativeY;
                    }
                }
                if (isSmoothScrolling()) {
                    initialScrollY = getScrollY();
                    destScrollY = scrollPosition;
                    initScrollMotion();
                } else {
                    setScrollY(scrollPosition);
                }
            }
            repaint();
        } else {
            //try to move parent scroll if you are not scrollable
            Container parent = getParent();
            if (parent != null) {
                parent.scrollRectToVisible(getAbsoluteX() - parent.getAbsoluteX() + x,
                        getAbsoluteY() - parent.getAbsoluteY() + y, 
                        width, height, parent);
            }
        }
    }

    /**
     * Indicates whether a border should be painted
     *
     * @return if the border will be painted
     * @deprecated use getStyle().getBorder() != null 
     */
    private boolean isBorderPainted() {
        return getStyle().getBorder() != null;
    }

    /**
     * Draws the component border if such a border exists. The border unlike the content
     * of the component will not be affected by scrolling for a scrollable component.
     * 
     * @param g graphics context on which the border is painted
     */
    protected void paintBorder(Graphics g) {
        Border b = getBorder();
        if (b != null) {
            g.setColor(getStyle().getFgColor());
            b.paint(g, this);
        }
    }

    /**
     * Used as an optimization to mark that this component is currently being
     * used as a cell renderer
     * 
     * @param cellRenderer indicate whether this component is currently being
     * used as a cell renderer
     */
    public void setCellRenderer(boolean cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

    /**
     * Used as an optimization to mark that this component is currently being
     * used as a cell renderer
     * 
     * @return rtue is this component is currently being used as a cell renderer
     */
    boolean isCellRenderer() {
        return cellRenderer;
    }

    /**
     * Indicate whether this component scroll is visible
     * 
     * @return true is this component scroll is visible; otherwise false
     */
    public boolean isScrollVisible() {
        return isScrollVisible;
    }

    /**
     * Set whether this component scroll is visible
     *
     * @param isScrollVisible Indicate whether this component scroll is visible
     */
    public void setScrollVisible(boolean isScrollVisible) {
        this.isScrollVisible = isScrollVisible;
    }

    /**
     * Set whether this component scroll is visible
     * 
     * @param isScrollVisible Indicate whether this component scroll is visible
     * @deprecated replaced by setScrollVisible to match the JavaBeans spec
     */
    public void setIsScrollVisible(boolean isScrollVisible) {
        this.isScrollVisible = isScrollVisible;
    }

    /**
     * Invoked internally to initialize and bind the component
     */
    void initComponentImpl() {
        if (!initialized) {
            initialized = true;
            Style stl = getStyle();
            Image i = stl.getBgImage();
            if(i != null) {
                i.lock();
            } else {
                Border b = stl.getBorder();
                if(b != null) {
                    b.lock();
                }
            }
            UIManager.getInstance().getLookAndFeel().bind(this);
            checkAnimation();
            if(isRTL() && isScrollableX()){
                setScrollX(getScrollDimension().getWidth() - getWidth());
            }
            initComponent();
        }
    }

    /**
     * Cleansup the initialization flags in the hierachy, notice that paint calls might
     * still occur after deinitilization mostly to perform transitions etc.
     * <p>However interactivity, animation and event tracking code can and probably
     * should be removed by this method.
     */
    void deinitializeImpl() {
        if (isInitialized()) {
            paintLockRelease();
            setInitialized(false);
            setDirtyRegion(null);
            Style stl = getStyle();
            Image i = stl.getBgImage();
            if(i != null) {
                i.unlock();
            } else {
                Border b = stl.getBorder();
                if(b != null) {
                    b.unlock();
                }
            }
            Painter p = stl.getBgPainter();
            if(p instanceof BGPainter) {
                ((BGPainter)p).radialCache = null;
            }
            deinitialize();
        }
    }

    /**
     * This is a callback method to inform the Component when it's been laidout
     * on the parent Container
     */
    protected void laidOut() {
        if (isScrollableY() && getScrollY() > 0 && getScrollY() + getHeight() > 
                getScrollDimension().getHeight()) {
            setScrollY(getScrollDimension().getHeight() - getHeight());
        }
        if (isScrollableX() && getScrollX() > 0 && getScrollX() + getWidth() > 
                getScrollDimension().getWidth()) {
            setScrollX(getScrollDimension().getWidth() - getWidth());
        }
        if(!isScrollableY()){
            setScrollY(0);
        }
        if(!isScrollableX()){
            setScrollX(0);
        }
    }

    /**
     * Invoked to indicate that the component initialization is being reversed
     * since the component was detached from the container hierarchy. This allows
     * the component to deregister animators and cleanup after itself. This
     * method is the opposite of the initComponent() method.
     */
    protected void deinitialize() {
    }

    /**
     * Allows subclasses to bind functionality that relies on fully initialized and
     * "ready for action" component state
     */
    protected void initComponent() {
    }

    /**
     * Indicates if the component is in the initialized state, a component is initialized
     * when its initComponent() method was invoked. The initMethod is invoked before showing the
     * component to the user.
     * 
     * @return true if the component is in the initialized state
     */
    protected boolean isInitialized() {
        return initialized;
    }

    /**
     * Indicates if the component is in the initialized state, a component is initialized
     * when its initComponent() method was invoked. The initMethod is invoked before showing the
     * component to the user.
     * 
     * @param initialized Indicates if the component is in the initialized state
     */
    protected void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * @inheritDoc
     */
    public void styleChanged(String propertyName, Style source) {
        //changing the Font, Padding, Margin may casue the size of the Component to Change
        //therefore we turn on the shouldCalcPreferredSize flag
        if ((!shouldCalcPreferredSize &&
                source == getStyle()) &&
                (propertyName.equals(Style.FONT) ||
                propertyName.equals(Style.MARGIN) ||
                propertyName.equals(Style.PADDING))) {
            setShouldCalcPreferredSize(true);
            Container parent = getParent();
            if (parent != null && parent.getComponentForm() != null) {
                parent.revalidate();
            }
        }
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the down key
     * 
     * @return the next focus component
     */
    public Component getNextFocusDown() {
        return nextFocusDown;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the down key
     * 
     * @param nextFocusDown the next focus component
     */
    public void setNextFocusDown(Component nextFocusDown) {
        this.nextFocusDown = nextFocusDown;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the up key. 
     * 
     * @return the nxt focus component
     */
    public Component getNextFocusUp() {
        return nextFocusUp;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the up key, this method doesn't affect the general focus behavior.
     * 
     * @param nextFocusUp next focus component
     */
    public void setNextFocusUp(Component nextFocusUp) {
        this.nextFocusUp = nextFocusUp;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the left key. 
     * 
     * @return the next focus component
     */
    public Component getNextFocusLeft() {
        return nextFocusLeft;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the left key, this method doesn't affect the general focus behavior.
     * 
     * @param nextFocusLeft the next focus component
     */
    public void setNextFocusLeft(Component nextFocusLeft) {
        this.nextFocusLeft = nextFocusLeft;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the right key
     * 
     * @return the next focus component
     */
    public Component getNextFocusRight() {
        return nextFocusRight;
    }

    /**
     * Allows us to determine which component will receive focus next when traversing 
     * with the right key
     * 
     * @param nextFocusRight the next focus component
     */
    public void setNextFocusRight(Component nextFocusRight) {
        this.nextFocusRight = nextFocusRight;
    }

    /**
     * Indicates whether component is enabled or disabled thus allowing us to prevent
     * a component from receiving input events and indicate so visually
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Used to reduce coupling between the TextArea component and display/implementation
     * classes thus reduce the size of the hello world MIDlet
     * 
     * @param text text after editing is completed
     */
    void onEditComplete(String text) {
    }

    /**
     * Indicates whether component is enabled or disabled thus allowing us to prevent
     * a component from receiving input events and indicate so visually
     * 
     * @param enabled true to enable false to disable
     */
    public void setEnabled(boolean enabled) {
        if(this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        repaint();
    }

    /**
     * A component name allows us to easily identify the component within a dynamic
     * UI.
     *
     * @return name of the component
     */
    public String getName() {
        return name;
    }

    /**
     * A component name allows us to easily identify the component within a dynamic
     * UI.
     *
     * @param name a name for the component
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Allows components to create a style of their own, this method binds the listener
     * to the style and installs a bg painter
     *
     * @param s style to initialize
     */
    protected void initCustomStyle(Style s) {
        s.addStyleListener(this);
        if (s.getBgPainter() == null) {
            s.setBgPainter(new BGPainter());
        }
    }

    /**
     * Allows components to create a style of their own, this method cleans up
     * state for the given style
     *
     * @param s style no longer used
     */
    protected void deinitializeCustomStyle(Style s) {
        s.removeStyleListener(this);
    }

    /**
     * Is the component a bidi RTL component
     *
     * @return true if the component is working in a right to left mode
     */
    public boolean isRTL() {
        return rtl;
    }

    /**
     * Is the component a bidi RTL component
     *
     * @param rtl true if the component should work in a right to left mode
     */
    public void setRTL(boolean rtl) {
        this.rtl = rtl;
    }

    /**
     * Elaborate components might not provide tactile feedback for all their areas (e.g. Lists)
     * this method defaults to returning the value of isTactileTouch
     * 
     * @param x the x position
     * @param y the y position
     * @return True if the device should vibrate
     */
    protected boolean isTactileTouch(int x, int y) {
        return isTactileTouch();
    }

    /**
     * Indicates whether the component should "trigger" tactile touch when pressed by the user
     * in a touch screen UI.

     * @return the tactileTouch
     */
    public boolean isTactileTouch() {
        return tactileTouch;
    }

    /**
     * Indicates whether the component should "trigger" tactile touch when pressed by the user
     * in a touch screen UI.
     *
     * @param tactileTouch true to trigger vibration when the component is pressed
     */
    public void setTactileTouch(boolean tactileTouch) {
        this.tactileTouch = tactileTouch;
    }

    /**
     * A component may expose mutable property names for a UI designer to manipulate, this
     * API is designed for usage internally by the GUI builder code
     * 
     * @return the property names allowing mutation
     */
    public String[] getPropertyNames() {
        return null;
    }

    /**
     * Matches the property names method (see that method for further details).
     *
     * @return the types of the properties
     */
    public Class[] getPropertyTypes() {
       return null;
    }

    /**
     * Returns the current value of the property name, this method is used by the GUI builder
     *
     * @param name the name of the property
     * @return the value of said property
     */
    public Object getPropertyValue(String name) {
        return null;
    }


    /**
     * Sets a new value to the given property, returns an error message if failed
     * and null if successful. Notice that some builtin properties such as "$designMode" might be sent
     * to components to indicate application state.
     *
     * @param name the name of the property
     * @param value new value for the property
     * @return error message or null
     */
    public String setPropertyValue(String name, Object value) {
        return "Unknown: " + name;
    }

    /**
     * Releases the paint lock image to allow paint to work as usual, see paintLock(boolean)
     * for details
     */
    public void paintLockRelease() {
        paintLockImage = null;
    }

    /**
     * This method locks the component so it will always paint the given image
     * instead of running through its paint logic. This is useful when running
     * transitions that might be quite expensive on the device. A lock should 
     * be released using paintLockRelease(), it is implicitly released when 
     * a component is deinitialized although a component doesn't need to be initialized
     * to be locked!<br>
     * If the component is not opaque null is always returned!
     * <p>Duplicate calls to this method won't produce duplicate locks, in case of 
     * a soft lock the return value will always be null.
     * 
     * @param hardLock indicates whether the lock uses a hard or a soft reference to the image
     * @return the image in case of a hard lock
     */
    public Image paintLock(boolean hardLock) {
        if((getStyle().getBgTransparency() & 0xff) != 0xff) {
            return null;
        }
        if(paintLockImage == null) {
            paintLockImage = Image.createImage(getWidth(), getHeight());
            int x = getX();
            int y = getY();
            setX(0);
            setY(0);
            paintInternalImpl(((Image)paintLockImage).getGraphics(), false);
            setX(x);
            setY(y);
            if(hardLock) {
                return (Image)paintLockImage;
            } else {
                paintLockImage = Display.getInstance().createSoftWeakRef(paintLockImage);
            }
        } else {
            if(hardLock) {
                return (Image)paintLockImage;
            }
        }
        return null;
    }

    /**
     * Indicates whether scrolling this component should jump to a specific location
     * in a grid
     *
     * @return the snapToGrid
     */
    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    /**
     * Indicates whether scrolling this component should jump to a specific location
     * in a grid
     * @param snapToGrid the snapToGrid to set
     */
    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }

    /**
     * A component that might need side swipe such as the slider
     * could block it from being used for some other purpose when
     * on top of said component.
     */
    boolean shouldBlockSideSwipe() {
        return false;
    }

    /**
     * Makes the component effectively opaque by blending the backgrounds into an image in memory so the layer of underlying components
     * is only drawn once when this component is repainted. This does have a significant memory overhead.
     *
     * @return the flatten property
     */
    public boolean isFlatten() {
        return flatten;
    }

    /**
     * Makes the component effectively opaque by blending the backgrounds into an image in memory so the layer of underlying components
     * is only drawn once when this component is repainted. This does have a significant memory overhead.
     *
     * @param flatten the flatten value
     */
    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }


    /**
     * Recommended length for the tensile, -1 for default
     * @return Recommended length for the tensile, -1 for default
     */
    public int getTensileLength() {
        return tensileLength;
    }

    /**
     * Recommended length for the tensile, -1 for default
     *
     * @param tensileLength length for tensile drag
     */
    public void setTensileLength(int tensileLength) {
        this.tensileLength = tensileLength;
    }

    Label getHintLabelImpl() {
        return null;
    }

    void setHintLabelImpl(Label hintLabel) {
    }

    boolean shouldShowHint() {
        return false;
    }

    void paintHint(Graphics g) {
        Label hintLabel = getHintLabelImpl();
        if (hintLabel != null && shouldShowHint()) {
            hintLabel.setX(getX());
            hintLabel.setY(getY());
            hintLabel.setWidth(getWidth());
            hintLabel.setHeight(getHeight());
            hintLabel.paint(g);
        }
    }

    String getHint() {
        Label hintLabel = getHintLabelImpl();
        if(hintLabel != null) {
            return hintLabel.getText();
        }
        return null;
    }

    /**
     * Returns the hint icon
     *
     * @return the hint icon
     */
    Image getHintIcon() {
        Label hintLabel = getHintLabelImpl();
        if(hintLabel != null) {
            return hintLabel.getIcon();
        }
        return null;
    }

    /**
     * Sets the hint text and Icon, the hint text and icon are
     * displayed on the component when it is empty
     *
     * @param hint the hint text to display
     * @param icon the hint icon to display
     */
    void setHint(String hint, Image icon){
        Label hintLabel = getHintLabelImpl();
        if(hintLabel == null){
            hintLabel = new Label(hint);
            hintLabel.setUIID("TextHint");
            setHintLabelImpl(hintLabel);
        }else{
            hintLabel.setText(hint);
        }
        hintLabel.setIcon(icon);
    }

    /**
     * This property is useful for blocking in z-order touch events, sometimes we might want to grab touch events in
     * a specific component without making it focusable.
     *
     * @return the grabsPointerEvents
     */
    public boolean isGrabsPointerEvents() {
        return grabsPointerEvents;
    }

    /**
     * This property is useful for blocking in z-order touch events, sometimes we might want to grab touch events in
     * a specific component without making it focusable.
     * 
     * @param grabsPointerEvents the grabsPointerEvents to set
     */
    public void setGrabsPointerEvents(boolean grabsPointerEvents) {
        this.grabsPointerEvents = grabsPointerEvents;
    }

    /**
     * Indicates the decrement units for the scroll opacity
     * @return the scrollOpacityChangeSpeed
     */
    public int getScrollOpacityChangeSpeed() {
        return scrollOpacityChangeSpeed;
    }

    /**
     * Indicates the decrement units for the scroll opacity
     * @param scrollOpacityChangeSpeed the scrollOpacityChangeSpeed to set
     */
    public void setScrollOpacityChangeSpeed(int scrollOpacityChangeSpeed) {
        this.scrollOpacityChangeSpeed = scrollOpacityChangeSpeed;
    }

    /**
     * Grows or shrinks this component to its new preferred size, this method
     * essentially takes a component whose preferred size has changed and creates a "growing"
     * effect that lasts for the duration. Notice that some components (such as text areas)
     * don't report proper preferred size untill they are laid out once. Hence the first time
     * around a text area (or container containing a text area) will not produce the expected
     * effect. This can be solved by invoking revalidate before the call to this method only the
     * first time around!
     *
     * @param duration the duration in milliseconds for the grow/shrink animation
     */
    public void growShrink(int duration) {
        Motion wMotion = Motion.createSplineMotion(getWidth(), getPreferredW(), duration);
        Motion hMotion = Motion.createSplineMotion(getHeight(), getPreferredH(), duration);
        wMotion.start();
        hMotion.start();
        setPreferredSize(new Dimension(getWidth(), getHeight()));
        // we are using bgpainter just to save the cost of creating another class
        getComponentForm().registerAnimated(new BGPainter(wMotion, hMotion));
        getComponentForm().revalidate();
    }

    /**
     * Enable the tensile drag to work even when a component doesn't have a scroll showable (scrollable flag still needs to be set to true)
     * @return the alwaysTensile
     */
    public boolean isAlwaysTensile() {
        return alwaysTensile && !isScrollableX();
    }

    /**
     * Enable the tensile drag to work even when a component doesn't have a scroll showable (scrollable flag still needs to be set to true)
     * @param alwaysTensile the alwaysTensile to set
     */
    public void setAlwaysTensile(boolean alwaysTensile) {
        this.alwaysTensile = alwaysTensile;
    }

    /**
     * Indicates whether this component can be dragged in a drag and drop operation rather than scroll the parent
     * @return the draggable state
     */
    public boolean isDraggable() {
        return draggable;
    }

    /**
     * Indicates whether this component can be dragged in a drag and drop operation rather than scroll the parent
     * @param draggable the draggable to set
     */
    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    /**
     * Indicates whether this component can receive dropped components into it, notice that when dropping on a component
     * or container the parents will be checked recursively to find a valid drop target
     * @return the dropTarget state
     */
    public boolean isDropTarget() {
        return dropTarget;
    }

    /**
     * Indicates whether this component can receive dropped components into it, notice that when dropping on a component
     * or container the parents will be checked recursively to find a valid drop target
     * 
     * @param dropTarget the dropTarget to set
     */
    public void setDropTarget(boolean dropTarget) {
        this.dropTarget = dropTarget;
    }

    class BGPainter implements Painter, Animation {
        private Motion wMotion, hMotion;
        private Form previousTint;
        private Painter painter;
        Image radialCache;
        private int cachedRadialBackgroundGradientStartColor;
        private int cachedRadialBackgroundGradientEndColor;
        private int cachedRadialWidth;
        private int cachedRadialHeight;
        private float cachedRadialBackgroundGradientRelativeX;
        private float cachedRadialBackgroundGradientRelativeY;
        private float cachedRadialBackgroundGradientRelativeSize;
        private Style constantStyle;

        public BGPainter(Motion wMotion, Motion hMotion) {
            this.wMotion = wMotion;
            this.hMotion = hMotion;
        }

        public BGPainter() {
        }

        public BGPainter(Style s) {
            constantStyle = s;
        }

        public BGPainter(Form parent, Painter p) {
            this.painter = p;
        }

        public void setPreviousForm(Form previous) {
            previousTint = previous;
        }

        public Form getPreviousForm() {
            return previousTint;
        }

        private void drawGradientBackground(Style s, Graphics g, int x, int y, int width, int height) {
            switch (s.getBackgroundType()) {
                case Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
                    g.fillLinearGradient(s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                            x, y, width, height, true);
                    return;
                case Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
                    g.fillLinearGradient(s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                            x, y, width, height, false);
                    return;
                case Style.BACKGROUND_GRADIENT_RADIAL:
                    if(isInitialized()) {
                        if(radialCache == null || cachedRadialBackgroundGradientStartColor != s.getBackgroundGradientStartColor() ||
                                cachedRadialBackgroundGradientEndColor != s.getBackgroundGradientEndColor() ||
                                cachedRadialWidth != width || cachedRadialHeight != height ||
                                cachedRadialBackgroundGradientRelativeX != s.getBackgroundGradientRelativeX() ||
                                cachedRadialBackgroundGradientRelativeY != s.getBackgroundGradientRelativeY() ||
                                cachedRadialBackgroundGradientRelativeSize != s.getBackgroundGradientRelativeSize()) {
                            cachedRadialBackgroundGradientStartColor = s.getBackgroundGradientStartColor();
                            cachedRadialBackgroundGradientEndColor = s.getBackgroundGradientEndColor();
                            cachedRadialWidth = width;
                            cachedRadialHeight = height;
                            cachedRadialBackgroundGradientRelativeX = s.getBackgroundGradientRelativeX();
                            cachedRadialBackgroundGradientRelativeY = s.getBackgroundGradientRelativeY();
                            cachedRadialBackgroundGradientRelativeSize = s.getBackgroundGradientRelativeSize();
                            radialCache = Image.createImage(width, height);
                            radialCache.getGraphics().fillRectRadialGradient(s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                                0, 0, width, height, s.getBackgroundGradientRelativeX(), s.getBackgroundGradientRelativeY(),
                                s.getBackgroundGradientRelativeSize());
                        }
                        g.drawImage(radialCache, x, y);
                    } else {
                        g.fillRectRadialGradient(s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                                x, y, width, height, s.getBackgroundGradientRelativeX(), s.getBackgroundGradientRelativeY(),
                                s.getBackgroundGradientRelativeSize());
                    }
                    return;
            }
            g.setColor(s.getBgColor());
            g.fillRect(x, y, width, height, s.getBgTransparency());
        }

        public void paint(Graphics g, Rectangle rect) {
            if (painter != null) {
                if (previousTint != null) {
                    previousTint.paint(g);
                }
            } else {
                Style s;
                if(constantStyle != null) {
                    s = constantStyle;
                } else {
                    s = getStyle();
                }
                int x = rect.getX();
                int y = rect.getY();
                int width = rect.getSize().getWidth();
                int height = rect.getSize().getHeight();
                if (width <= 0 || height <= 0) {
                    return;
                }
                Image bgImage = s.getBgImage();
                if (bgImage == null) {
                    if(s.getBackgroundType() >= Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL) {
                        drawGradientBackground(s, g, x, y, width, height);
                        return;
                    }
                    g.setColor(s.getBgColor());
                    g.fillRect(x, y, width, height, s.getBgTransparency());
                } else {
                    int iW = bgImage.getWidth();
                    int iH = bgImage.getHeight();
                    switch (s.getBackgroundType()) {
                        case Style.BACKGROUND_IMAGE_SCALED:
                            if (iW != width || iH != height) {
                                bgImage = bgImage.scaled(width, height);
                                s.setBgImage(bgImage, true);
                            }
                            g.drawImage(s.getBgImage(), x, y);
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_BOTH:
                            for (int xPos = 0; xPos <= width; xPos += iW) {
                                for (int yPos = 0; yPos <= height; yPos += iH) {
                                    g.drawImage(s.getBgImage(), x + xPos, y + yPos);
                                }
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            for (int xPos = 0; xPos <= width; xPos += iW) {
                                g.drawImage(s.getBgImage(), x + xPos, y);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            for (int xPos = 0; xPos <= width; xPos += iW) {
                                g.drawImage(s.getBgImage(), x + xPos, y + (height / 2 - iH / 2));
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            for (int xPos = 0; xPos <= width; xPos += iW) {
                                g.drawImage(s.getBgImage(), x + xPos, y + (height - iH));
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            for (int yPos = 0; yPos <= height; yPos += iH) {
                                g.drawImage(s.getBgImage(), x, y + yPos);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            for (int yPos = 0; yPos <= height; yPos += iH) {
                                g.drawImage(s.getBgImage(), x + (width / 2 - iW / 2), y + yPos);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            for (int yPos = 0; yPos <= height; yPos += iH) {
                                g.drawImage(s.getBgImage(), x + width - iW, y + yPos);
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            switch (s.getBackgroundAlignment()) {
                                case Style.BACKGROUND_IMAGE_ALIGN_BOTTOM:
                                    g.drawImage(s.getBgImage(), x + width - iW, y + (height - iH));
                                    return;
                                case Style.BACKGROUND_IMAGE_ALIGN_TOP:
                                    g.drawImage(s.getBgImage(), x + width - iW, y);
                                    return;
                                case Style.BACKGROUND_IMAGE_ALIGN_LEFT:
                                    g.drawImage(s.getBgImage(), x, y + (height / 2 - iH / 2));
                                    return;
                                case Style.BACKGROUND_IMAGE_ALIGN_RIGHT:
                                    g.drawImage(s.getBgImage(), x + width - iW, y + (height / 2 - iH / 2));
                                    return;
                                case Style.BACKGROUND_IMAGE_ALIGN_CENTER:
                                    g.drawImage(s.getBgImage(), x + (width / 2 - iW / 2), y + (height / 2 - iH / 2));
                                    return;
                            }
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_TOP:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(s.getBgImage(), x + (width / 2 - iW / 2), y);
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(s.getBgImage(), x + (width / 2 - iW / 2), y + (height - iH));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_LEFT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(s.getBgImage(), x, y + (height / 2 - iH / 2));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_RIGHT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(s.getBgImage(), x + width - iW, y + (height / 2 - iH / 2));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_CENTER:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(s.getBgImage(), x + (width / 2 - iW / 2), y + (height / 2 - iH / 2));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(s.getBgImage(), x, y);
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(s.getBgImage(), x + width - iW, y);
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(s.getBgImage(), x, y + (height - iH));
                            return;
                        case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT:
                            g.setColor(s.getBgColor());
                            g.fillRect(x, y, width, height, s.getBgTransparency());
                            g.drawImage(s.getBgImage(), x + width - iW, y + (height - iH));
                            return;
                    }
                }
            }
        }

        public boolean animate() {
            if(wMotion.isFinished() && hMotion.isFinished()) {
                getComponentForm().deregisterAnimated(this);
                setPreferredSize(null);
                getComponentForm().revalidate();
                return false;
            }
            setPreferredSize(new Dimension(wMotion.getValue(), hMotion.getValue()));
            getComponentForm().revalidate();
            return false;
        }

        public void paint(Graphics g) {
        }
    }
}
