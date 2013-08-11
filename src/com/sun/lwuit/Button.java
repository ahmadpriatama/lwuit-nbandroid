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
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.plaf.UIManager;


/**
 * Button is the base class for several UI widgets allowing clickability.
 * It has 3 states: rollover, pressed and the default state it 
 * can also have ActionListeners that react when the Button is clicked.
 * 
 * @author Chen Fishbein
 */
public class Button extends Label {
    /**
     * Indicates the rollover state of a button which is equivalent to focused for
     * most uses
     */
    public static final int STATE_ROLLOVER = 0;
    
    /**
     * Indicates the pressed state of a button 
     */
    public static final int STATE_PRESSED = 1;
    
    /**
     * Indicates the default state of a button which is neither pressed nor focused
     */
    public static final int STATE_DEFAULT = 2;
    
    private EventDispatcher dispatcher = new EventDispatcher();
    
    private int state = STATE_DEFAULT;
    
    private Image pressedIcon;
    
    private Image rolloverIcon;
    private Image rolloverPressedIcon;
  
    private Image disabledIcon;
    private Command cmd;

    private boolean toggle;
    
    /** 
     * Constructs a button with an empty string for its text.
     */
    public Button() {
        this("");
    }
    
    /**
     * Constructs a button with the specified text.
     * 
     * @param text label appearing on the button
     */
    public Button(String text) {
        this(text, null);
    }
    
    /**
     * Allows binding a command to a button for ease of use
     * 
     * @param cmd command whose text would be used for the button and would recive action events
     * from the button
     */
    public Button(Command cmd) {
        this(cmd.getCommandName(), cmd.getIcon());
        addActionListener(cmd);
        this.cmd = cmd;
        setEnabled(cmd.isEnabled());
        updateCommand();
    }

    private void updateCommand() {
        setRolloverIcon(cmd.getRolloverIcon());
        setDisabledIcon(cmd.getDisabledIcon());
        setPressedIcon(cmd.getPressedIcon());
    }

    /**
     * Applies the given command to this button
     *
     * @param  cmd the command on the button
     */
    public void setCommand(Command cmd) {
        if(this.cmd != null) {
            removeActionListener(this.cmd);
        }
        this.cmd = cmd;
        setText(cmd.getCommandName());
        setIcon(cmd.getIcon());
        setEnabled(cmd.isEnabled());
        updateCommand();
        addActionListener(cmd);
    }

    /**
     * Constructs a button with the specified image.
     * 
     * @param icon appearing on the button
     */
    public Button(Image icon) {
        this("", icon);
    }
    
    /**
     * Constructor a button with text and image
     * 
     * @param text label appearing on the button
     * @param icon image appearing on the button
     */
    public Button(String text, Image icon) {
        super(text);
        setUIID("Button");
        setFocusable(true);
        setIcon(icon);
        this.pressedIcon = icon;
        this.rolloverIcon = icon;
    }

    /**
     * @inheritDoc
     */
    void focusGainedInternal() {
        super.focusGainedInternal();
        if(state != STATE_PRESSED) {
            state = STATE_ROLLOVER;
        }
    }
    
    /**
     * @inheritDoc
     */
    void focusLostInternal() {
        super.focusLostInternal();
        state = STATE_DEFAULT;
    }
    
    /**
     * Returns the button state
     * 
     * @return One of STATE_ROLLOVER, STATE_DEAFULT, STATE_PRESSED
     */
    public int getState() {
        return state;
    }
    
    /**
     * Indicates the icon that is displayed on the button when the button is in 
     * pressed state
     * 
     * @return icon used
     * @see #STATE_PRESSED
     */
    public Image getPressedIcon() {
        return pressedIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * pressed state and is selected. This is ONLY applicable to toggle buttons
     *
     * @return icon used
     */
    public Image getRolloverPressedIcon() {
        return rolloverPressedIcon;
    }
    
    /**
     * Indicates the icon that is displayed on the button when the button is in
     * pressed state and is selected. This is ONLY applicable to toggle buttons
     *
     * @param rolloverPressedIcon icon used
     */
    public void setRolloverPressedIcon(Image rolloverPressedIcon) {
        this.rolloverPressedIcon = rolloverPressedIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * the disabled state
     *
     * @return icon used
     */
    public Image getDisabledIcon() {
        return disabledIcon;
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in 
     * rolled over state
     * 
     * @return icon used
     * @see #STATE_ROLLOVER
     */
    public Image getRolloverIcon() {
        return rolloverIcon;
    }
    
    /**
     * Indicates the icon that is displayed on the button when the button is in 
     * rolled over state
     * 
     * @param rolloverIcon icon to use
     * @see #STATE_ROLLOVER
     */
    public void setRolloverIcon(Image rolloverIcon) {
        this.rolloverIcon = rolloverIcon;
        setShouldCalcPreferredSize(true);
        checkAnimation();
        repaint();        
    }
    
    /**
     * Indicates the icon that is displayed on the button when the button is in 
     * pressed state
     * 
     * @param pressedIcon icon used
     * @see #STATE_PRESSED
     */
    public void setPressedIcon(Image pressedIcon) {
        this.pressedIcon = pressedIcon;
        setShouldCalcPreferredSize(true);
        checkAnimation();
        repaint();
    }

    /**
     * Indicates the icon that is displayed on the button when the button is in
     * the disabled state
     *
     * @param disabledIcon icon used
     */
    public void setDisabledIcon(Image disabledIcon) {
        this.disabledIcon = disabledIcon;
        setShouldCalcPreferredSize(true);
        checkAnimation();
        repaint();
    }

    void checkAnimation() {
        super.checkAnimation();
        if((pressedIcon != null && pressedIcon.isAnimation()) || 
            (rolloverIcon != null && rolloverIcon.isAnimation()) ||
            (disabledIcon != null && disabledIcon.isAnimation())) {
            Form parent = getComponentForm();
            if(parent != null) {
                // animations are always running so the internal animation isn't
                // good enough. We never want to stop this sort of animation
                parent.registerAnimated(this);
            }
        }
    }
    
    /**
     * Adds a listener to the button which will cause an event to dispatch on click
     * 
     * @param l implementation of the action listener interface
     */
    public void addActionListener(ActionListener l){
        dispatcher.addListener(l);
    }
    
    /**
     * Removes the given action listener from the button
     * 
     * @param l implementation of the action listener interface
     */
    public void removeActionListener(ActionListener l){
        dispatcher.removeListener(l);
    }

    /**
     * Returns the icon for the button based on its current state
     *
     * @return the button icon based on its current state
     */
    public Image getIconFromState() {
        Image icon = getIcon();
        if(!isEnabled() && getDisabledIcon() != null) {
            return getDisabledIcon();
        }
        if(isToggle() && isSelected()) {
            icon = rolloverPressedIcon;
            if(icon == null) {
                icon = getPressedIcon();
                if (icon == null) {
                    icon = getIcon();
                }
            }
            return icon;
        }
        switch (getState()) {
            case Button.STATE_DEFAULT:
                break;
            case Button.STATE_PRESSED:
                icon = getPressedIcon();
                if (icon == null) {
                    icon = getIcon();
                }
                break;
            case Button.STATE_ROLLOVER:
                if(Display.getInstance().shouldRenderSelection(this)) {
                    icon = getRolloverIcon();
                    if (icon == null) {
                        icon = getIcon();
                    }
                }
                break;
        }
        return icon;
    }

    /**
     * @inheritDoc
     */
    void fireActionEvent(int x, int y){
        super.fireActionEvent();
        if(cmd != null) {
            ActionEvent ev = new ActionEvent(cmd, this, x, y);
            dispatcher.fireActionEvent(ev);
            if(!ev.isConsumed()) {
                Form f = getComponentForm();
                if(f != null) {
                    f.actionCommandImpl(cmd, ev);
                }
            }
        } else {
            dispatcher.fireActionEvent(new ActionEvent(this, x, y));
        }
        Display d = Display.getInstance();
        if(d.isBuiltinSoundsEnabled()) {
            d.playBuiltinSound(Display.SOUND_TYPE_BUTTON_PRESS);
        }
    }
    
    /**
     * Invoked to change the state of the button to the pressed state
     */
    public void pressed(){
        state=STATE_PRESSED;
        repaint();
    }
    
    /**
     * Invoked to change the state of the button to the released state
     */
    public void released() {
        released(-1, -1);
    }
    
    /**
     * Invoked to change the state of the button to the released state
     *
     * @param x the x position if a touch event triggered this, -1 if this isn't relevant
     * @param y the y position if a touch event triggered this, -1 if this isn't relevant
     */
    public void released(int x, int y) {
        state=STATE_ROLLOVER;
        fireActionEvent(x, y);
        repaint();
    }
    
    /**
     * @inheritDoc
     */
    public void keyPressed(int keyCode) {
        if (Display.getInstance().getGameAction(keyCode) == Display.GAME_FIRE){
            pressed();
        }
    }
    
    /**
     * @inheritDoc
     */
    public void keyReleased(int keyCode) {
        if (Display.getInstance().getGameAction(keyCode) == Display.GAME_FIRE){
            released();
        }
    }
    
    /**
     * @inheritDoc
     */
    public void keyRepeated(int keyCode) {
    }
    
    /**
     * @inheritDoc
     */
    protected void fireClicked() {
        pressed();
        released();
    }
    
    /**
     * @inheritDoc
     */
    protected boolean isSelectableInteraction() {
        return true;
    }

    /**
     * @inheritDoc
     */
    public void pointerHover(int[] x, int[] y) {
        requestFocus();
    }
    
    /**
     * @inheritDoc
     */
    public void pointerHoverReleased(int[] x, int[] y) {
        requestFocus();
    }

    /**
     * @inheritDoc
     */
    public void pointerPressed(int x, int y) {
        clearDrag();
        setDragActivated(false);
        pressed();
    }
    
    /**
     * @inheritDoc
     */
    public void pointerReleased(int x, int y) {
        // button shouldn't fire an event when a pointer is dragged into it
        if(state == STATE_PRESSED) {
            released(x, y);
        }
    }

    /**
     * @inheritDoc
     */
    protected void dragInitiated() {
        if(Display.getInstance().shouldRenderSelection(this)) {
            state=STATE_ROLLOVER;
        } else {
            state=STATE_DEFAULT;
        }
        repaint();
    }

    /**
     * @inheritDoc
     */
    public void pointerDragged(int x, int y) {
        if(Display.getInstance().shouldRenderSelection(this)) {
            if(state != STATE_ROLLOVER) {
                state=STATE_ROLLOVER;
                repaint();
            }
        } else {
            state = STATE_DEFAULT;
            repaint();
        }
        super.pointerDragged(x, y);
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        UIManager.getInstance().getLookAndFeel().drawButton(g, this);
    }
    
    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize(){
        return UIManager.getInstance().getLookAndFeel().getButtonPreferredSize(this);
    }
    
    /**
     * @inheritDoc
     */
    protected Border getBorder() {
        return getStyle().getBorder();
    }

    boolean isPressedStyle() {
        // if a toggle button has focus we should draw the selected state not the pressed state
        // however if shouldRenderSelection is false the selected state won't be painted so
        // we should draw the pressed state
        if(toggle && isSelected()) {
            if(hasFocus()) {
                return !Display.getInstance().shouldRenderSelection(this);
            }
            return true;
        }
        return state == STATE_PRESSED;
    }

    /**
     * This method return the Button Command if exists
     * 
     * @return Command Object or null if a Command not exists
     */
    public Command getCommand() {
        return cmd;
    }

    /**
     * Returns true if the button is selected for toggle buttons,
     * throws an exception if this is not a toggle button
     *
     * @return true if the button is selected
     */
    public boolean isSelected() {
        throw new RuntimeException();
    }

    /**
     * @inheritDoc
     * @deprecated use the Style alignment instead
     */
    public void setAlignment(int align){
        super.setAlignment(align);
        getPressedStyle().setAlignment(align);
    }

    /**
     * Toggle button mode is only relevant for checkboxes/radio buttons. When pressed
     * a toggle button stays pressed and when pressed again it moves to releleased state.
     *
     * @return the toggle
     */
    public boolean isToggle() {
        return toggle;
    }

    /**
     * Toggle button mode is only relevant for checkboxes/radio buttons. When pressed
     * a toggle button stays pressed and when pressed again it moves to releleased state.
     * Setting toggle implicitly changes the UIID to "ToggleButton"
     *
     * @param toggle the toggle to set
     */
    public void setToggle(boolean toggle) {
        this.toggle = toggle;
        if(toggle && getUIID().equals("CheckBox") || getUIID().equals("RadioButton")) {
            setUIID("ToggleButton");
        }
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        boolean a = super.animate();
        if(!isEnabled() && disabledIcon != null) {
            a = disabledIcon.isAnimation() && disabledIcon.animate() ||
                    a;
        } else {
            switch(state) {
                case STATE_ROLLOVER:
                    a = rolloverIcon != null && rolloverIcon.isAnimation() && rolloverIcon.animate() ||
                            a;
                    break;
                case STATE_PRESSED:
                    a = pressedIcon != null && pressedIcon.isAnimation() && pressedIcon.animate() ||
                            a;
                    break;
            }
        }
        return a;
    }

    /**
     * Places the check box or radio button on the opposite side at the far end
     *
     * @return the oppositeSide
     */
    public boolean isOppositeSide() {
        return false;
    }
}
