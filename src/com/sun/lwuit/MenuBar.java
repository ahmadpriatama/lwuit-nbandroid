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

import com.sun.lwuit.animations.CommonTransitions;
import com.sun.lwuit.animations.Transition;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.FlowLayout;
import com.sun.lwuit.layouts.GridLayout;
import com.sun.lwuit.list.ListCellRenderer;
import com.sun.lwuit.plaf.LookAndFeel;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.plaf.UIManager;
import java.util.Vector;

/**
 * This class represents the Form MenuBar.
 * This class is responsible to show the Form Commands and to handle device soft
 * keys, back key, clear key, etc...
 * This class can be overridden and replaced in the LookAndFeel
 * @see LookAndFeel#setMenuBarClass(java.lang.Class) 
     
 * @author Chen Fishbein
 */
public class MenuBar extends Container implements ActionListener {

    private Command selectCommand;
    private Command defaultCommand;
    /**
     * Indicates the command that is defined as the back command out of this form.
     * A back command can be used both to map to a hardware button (e.g. on the Sony Ericsson devices)
     * and by elements such as transitions etc. to change the behavior based on 
     * direction (e.g. slide to the left to enter screen and slide to the right to exit with back).
     */
    private Command backCommand;
    /**
     * Indicates the command that is defined as the clear command out of this form similar
     * in spirit to the back command
     */
    private Command clearCommand;
    /**
     * This member holds the left soft key value
     */
    static int leftSK;
    /**
     * This member holds the right soft key value
     */
    static int rightSK;
    /**
     * This member holds the 2nd right soft key value
     * this is used for different BB devices
     */
    static int rightSK2;
    /**
     * This member holds the back command key value
     */
    static int backSK;
    /**
     * This member holds the clear command key value
     */
    static int clearSK;
    static int backspaceSK;
    

    static {
        // RIM and potentially other devices reinitialize the static initializer thus overriding
        // the new static values set by the initialized display https://lwuit.dev.java.net/issues/show_bug.cgi?id=232
        if (Display.getInstance() == null || Display.getInstance().getImplementation() == null) {
            leftSK = -6;
            rightSK = -7;
            rightSK2 = -7;
            backSK = -11;
            clearSK = -8;
            backspaceSK = -8;
        }
    }
    private Command menuCommand;
    private Vector commands = new Vector();
    private Button[] soft;
    private Command[] softCommand;
    private Button left;
    private Button right;
    private Button main;
    private ListCellRenderer menuCellRenderer;
    private Transition transitionIn;
    private Transition transitionOut;
    private Component commandList;
    private Style menuStyle;
    private Command selectMenuItem;
    private Command cancelMenuItem;
    private Form parent;
    private int softkeyCount;
    private boolean thirdSoftButton;

    /**
     * Empty Constructor
     */
    public MenuBar() {
    }
    
    /**
     * Initialize the MenuBar
     * 
     * @param parent the associated Form
     */
    protected void initMenuBar(Form parent) {
        this.parent = parent;
        selectMenuItem = createMenuSelectCommand();
        cancelMenuItem = createMenuCancelCommand();
        LookAndFeel lf = UIManager.getInstance().getLookAndFeel();
        menuStyle = UIManager.getInstance().getComponentStyle("Menu");
        setUIID("SoftButton");
        menuCommand = new Command(UIManager.getInstance().localize("menu", "Menu"), lf.getMenuIcons()[2]);
        // use the slide transition by default
        if (lf.getDefaultMenuTransitionIn() != null || lf.getDefaultMenuTransitionOut() != null) {
            transitionIn = lf.getDefaultMenuTransitionIn();
            transitionOut = lf.getDefaultMenuTransitionOut();
        } else {
            transitionIn = CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, true, 300, true);
            transitionOut = CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, false, 300, true);
        }
        menuCellRenderer = lf.getMenuRenderer();
        softkeyCount = Display.getInstance().getImplementation().getSoftkeyCount();
        thirdSoftButton = Display.getInstance().isThirdSoftButton();

        int commandBehavior = getCommandBehavior();
        if (softkeyCount > 1 && commandBehavior < Display.COMMAND_BEHAVIOR_BUTTON_BAR) {
            if (thirdSoftButton) {
                setLayout(new GridLayout(1, 3));
                soft = new Button[]{createSoftButton("SoftButtonCenter"), createSoftButton("SoftButtonLeft"), createSoftButton("SoftButtonRight")};
                main = soft[0];
                left = soft[1];
                right = soft[2];
                if (parent.isRTL()) {
                    right.setUIID("SoftButtonLeft");
                    left.setUIID("SoftButtonRight");
                    addComponent(right);
                    addComponent(main);
                    addComponent(left);
                } else {
                    addComponent(left);
                    addComponent(main);
                    addComponent(right);
                }
                if (isReverseSoftButtons()) {
                    Button b = soft[1];
                    soft[1] = soft[2];
                    soft[2] = b;
                }
            } else {
                setLayout(new GridLayout(1, 2));
                soft = new Button[]{createSoftButton("SoftButtonLeft"), createSoftButton("SoftButtonRight")};
                main = soft[0];
                left = soft[0];
                right = soft[1];
                if (parent.isRTL()) {
                    right.setUIID("SoftButtonLeft");
                    left.setUIID("SoftButtonRight");
                    addComponent(right);
                    addComponent(left);
                } else {
                    addComponent(left);
                    addComponent(right);
                }
                if (isReverseSoftButtons()) {
                    Button b = soft[0];
                    soft[0] = soft[1];
                    soft[1] = b;
                }
            }
            // It doesn't make sense for softbuttons to have ... at the end
            for (int iter = 0; iter < soft.length; iter++) {
                soft[iter].setEndsWith3Points(false);
            }
        } else {
            // special case for touch screens we still want the 3 softbutton areas...
            if (thirdSoftButton) {
                setLayout(new GridLayout(1, 3));
                soft = new Button[]{createSoftButton("SoftButtonCenter"), createSoftButton("SoftButtonLeft"), createSoftButton("SoftButtonRight")};
                main = soft[0];
                left = soft[1];
                right = soft[2];
                addComponent(left);
                addComponent(main);
                addComponent(right);
                if (isReverseSoftButtons()) {
                    Button b = soft[1];
                    soft[1] = soft[2];
                    soft[2] = b;
                }
            } else {
                soft = new Button[]{createSoftButton("SoftButtonCenter")};
            }
        }

        softCommand = new Command[soft.length];
    }

    private int getCommandBehavior() {
        int i = Display.getInstance().getCommandBehavior();
        if(Display.getInstance().getImplementation().getSoftkeyCount() == 0) {
            if(i != Display.COMMAND_BEHAVIOR_BUTTON_BAR && i != Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK &&
                    i != Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT) {
                return Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK;
            }
            return i;
        }
        if(i == Display.COMMAND_BEHAVIOR_DEFAULT) {
            if(Display.getInstance().isTouchScreenDevice()) {
                return Display.COMMAND_BEHAVIOR_TOUCH_MENU;
            }
            return Display.COMMAND_BEHAVIOR_SOFTKEY;
        }
        return i;
    }

    /**
     * Default command is invoked when a user presses fire, this functionality works
     * well in some situations but might collide with elements such as navigation
     * and combo boxes. Use with caution.
     * 
     * @param defaultCommand the command to treat as default
     */
    public void setDefaultCommand(Command defaultCommand) {
        this.defaultCommand = defaultCommand;
    }

    /**
     * Default command is invoked when a user presses fire, this functionality works
     * well in some situations but might collide with elements such as navigation
     * and combo boxes. Use with caution.
     * 
     * @return the command to treat as default
     */
    public Command getDefaultCommand() {
        if (selectCommand != null) {
            return selectCommand;
        }
        return defaultCommand;
    }

    /**
     * Indicates the command that is defined as the clear command in this form.
     * A clear command can be used both to map to a "clear" hardware button 
     * if such a button exists.
     * 
     * @param clearCommand the command to treat as the clear Command
     */
    public void setClearCommand(Command clearCommand) {
        this.clearCommand = clearCommand;
    }

    /**
     * Indicates the command that is defined as the clear command in this form.
     * A clear command can be used both to map to a "clear" hardware button 
     * if such a button exists.
     * 
     * @return the command to treat as the clear Command
     */
    public Command getClearCommand() {
        return clearCommand;
    }

    private void updateTitleCommandPlacement() {
        int commandBehavior = getCommandBehavior();
        Container t = parent.getTitleArea();
        BorderLayout titleLayout = (BorderLayout)t.getLayout();
        if(commandBehavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK && !(parent instanceof Dialog)) {
            titleLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);

            Button b = null;
            for(int iter = 0 ; iter < t.getComponentCount() ; iter++) {
                Component c = t.getComponentAt(iter);
                if(c instanceof Button) {
                    b = (Button)c;
                    break;
                }
            }
            if(backCommand == null) {
                if(b != null) {
                    t.removeComponent(b);
                    BorderLayout internalLayout = new BorderLayout();
                    t.setLayout(internalLayout);
                    Component title = t.getComponentAt(0);
                    t.removeComponent(title);
                    t.addComponent(BorderLayout.CENTER, title);
                }
            } else {
                if(b == null) {
                    b = new Button(backCommand);
                    b.setUIID("BackCommand");
                    t.addComponent(BorderLayout.WEST, b);
                } else {
                    b.setCommand(backCommand);
                }
                removeCommand(backCommand);
            }
        } else {
            if(t.getComponentCount() > 1) {
                titleLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
                Label l = parent.getTitleComponent();
                t.removeAll();
                t.addComponent(BorderLayout.CENTER, l);
            }
        }
    }

    /**
     * Indicates the command that is defined as the back command out of this form.
     * A back command can be used both to map to a hardware button (e.g. on the Sony Ericsson devices)
     * and by elements such as transitions etc. to change the behavior based on 
     * direction (e.g. slide to the left to enter screen and slide to the right to exit with back).
     * 
     * @param backCommand the command to treat as the back Command
     */
    public void setBackCommand(Command backCommand) {
        this.backCommand = backCommand;
        updateTitleCommandPlacement();
    }

    /**
     * Indicates the command that is defined as the back command out of this form.
     * A back command can be used both to map to a hardware button (e.g. on the Sony Ericsson devices)
     * and by elements such as transitions etc. to change the behavior based on 
     * direction (e.g. slide to the left to enter screen and slide to the right to exit with back).
     * 
     * @return the command to treat as the back Command
     */
    public Command getBackCommand() {
        return backCommand;
    }

    /**
     * The selectCommand is the command to invoke when a Component has foucs in
     * Third Soft Button state.
     * 
     * @return the select command
     */
    public Command getSelectCommand() {
        return selectCommand;
    }

    /**
     * Sets the select command
     * 
     * @param selectCommand
     */
    public void setSelectCommand(Command selectCommand) {
        this.selectCommand = selectCommand;
    }

    /**
     * Updates the command mapping to the softbuttons
     */
    private void updateCommands() {
        int commandBehavior = getCommandBehavior();
        if(commandBehavior == Display.COMMAND_BEHAVIOR_NATIVE) {
            Display.getInstance().getImplementation().setNativeCommands(commands);
            return;
        }
        if(commandBehavior >= Display.COMMAND_BEHAVIOR_BUTTON_BAR) {
            return;
        }
        if (soft.length > 1) {
            soft[0].setText("");
            soft[1].setText("");
            soft[0].setIcon(null);
            soft[1].setIcon(null);
            int commandSize = getCommandCount();
            if (soft.length > 2) {
                soft[2].setText("");
                if (commandSize > 2) {
                    if (commandSize > 3) {
                        softCommand[2] = menuCommand;
                    } else {
                        softCommand[2] = getCommand(getCommandCount() - 3);
                    }
                    soft[2].setText(softCommand[2].getCommandName());
                    soft[2].setIcon(softCommand[2].getIcon());
                } else {
                    softCommand[2] = null;
                }
            }
            if (commandSize > 0) {
                softCommand[0] = getCommand(getCommandCount() - 1);
                soft[0].setText(softCommand[0].getCommandName());
                soft[0].setIcon(softCommand[0].getIcon());
                if (commandSize > 1) {
                    if (soft.length == 2 && commandSize > 2) {
                        softCommand[1] = menuCommand;
                    } else {
                        softCommand[1] = getCommand(getCommandCount() - 2);
                    }
                    soft[1].setText(softCommand[1].getCommandName());
                    soft[1].setIcon(softCommand[1].getIcon());
                } else {
                    softCommand[1] = null;
                }
            } else {
                softCommand[0] = null;
                softCommand[1] = null;
            }

            // we need to add the menu bar to an already visible form
            if (commandSize == 1) {
                if (parent.isVisible()) {
                    parent.revalidate();
                }
            }
            repaint();
        }
    }

    /**
     * Invoked when a softbutton is pressed
     */
    public void actionPerformed(ActionEvent evt) {
        if (evt.isConsumed()) {
            return;
        }
        Object src = evt.getSource();
        if (commandList == null) {
            Button source = (Button) src;
            for (int iter = 0; iter < soft.length; iter++) {
                if (source == soft[iter]) {
                    if (softCommand[iter] == menuCommand) {
                        showMenu();
                        return;
                    }
                    if (softCommand[iter] != null) {
                        ActionEvent e = new ActionEvent(softCommand[iter]);
                        softCommand[iter].actionPerformed(e);
                        if (!e.isConsumed()) {
                            parent.actionCommandImpl(softCommand[iter]);
                        }
                    }
                    return;
                }
            }
        } else {
            // the list for the menu sent the event
            if (src instanceof Button) {
                for (int iter = 0; iter < soft.length; iter++) {
                    if (src == soft[iter]) {
                        Container parent = commandList.getParent();
                        while (parent != null) {
                            if (parent instanceof Dialog) {
                                ((Dialog) parent).actionCommand(softCommand[iter]);
                                return;
                            }
                            parent = parent.getParent();
                        }
                    }
                }
            }
            Command c = getComponentSelectedCommand(commandList);
            if(!c.isEnabled()) {
                return;
            }
            Container p = commandList.getParent();
            while (p != null) {
                if (p instanceof Dialog) {
                    ((Dialog) p).actionCommand(c);
                    return;
                }
                p = p.getParent();
            }
        }

    }

    /**
     * Creates a soft button Component
     * @return the softbutton component
     */
    protected Button createSoftButton(String uiid) {
        Button b = new Button();
        b.setUIID(uiid);
        b.addActionListener(this);
        b.setFocusable(false);
        b.setTactileTouch(true);
        updateSoftButtonStyle(b);
        return b;
    }

    private void updateSoftButtonStyle(Button b) {
        if (softkeyCount < 2) {
            b.getStyle().setMargin(0, 0, 0, 0);
            b.getStyle().setPadding(0, 0, 0, 0);
        }
    }

    /**
     * @inheritDoc
     */
    public void setUnselectedStyle(Style style) {
        style.setMargin(Component.TOP, 0, true);
        style.setMargin(Component.BOTTOM, 0, true);
        super.setUnselectedStyle(style);
        if (soft != null) {
            for (int iter = 0; iter < soft.length; iter++) {
                updateSoftButtonStyle(soft[iter]);
            }
        }
    }

    /**
     * Prevents scaling down of the menu when there is no text on the menu bar 
     */
    protected Dimension calcPreferredSize() {
        if (soft.length > 1) {
            Dimension d = super.calcPreferredSize();
            if ((soft[0].getText() == null || soft[0].getText().equals("")) &&
                    (soft[1].getText() == null || soft[1].getText().equals("")) &&
                    soft[0].getIcon() == null && soft[1].getIcon() == null &&
                    (soft.length < 3 ||
                    ((soft[2].getText() == null || soft[2].getText().equals("")) && soft[2].getIcon() == null))) {
                d.setHeight(0);
            }
            return d;
        }
        return super.calcPreferredSize();
    }

    /**
     * Sets the menu transitions for showing/hiding the menu, can be null...
     */
    public void setTransitions(Transition transitionIn, Transition transitionOut) {
        this.transitionIn = transitionIn;
        this.transitionOut = transitionOut;
    }

    /**
     * This method shows the menu on the Form.
     * The method creates a Dialog with the commands and calls showMenuDialog.
     * The method blocks until the user dispose the dialog.
     */
    public void showMenu() {
        final Dialog d = new Dialog("Menu", "");
        d.setDisposeWhenPointerOutOfBounds(true);
        d.setMenu(true);

        d.setTransitionInAnimator(transitionIn);
        d.setTransitionOutAnimator(transitionOut);
        d.setLayout(new BorderLayout());
        d.setScrollable(false);
        //calling parent.createCommandComponent is done only for backward 
        //compatability reasons, in the next version this call be replaced with 
        //calling directly to createCommandComponent
        ((Form) d).getMenuBar().commandList = createCommandComponent(commands);
        if (menuCellRenderer != null && ((Form) d).getMenuBar().commandList instanceof List) {
            ((List) ((Form) d).getMenuBar().commandList).setListCellRenderer(menuCellRenderer);
        }
        d.getContentPane().getStyle().setMargin(0, 0, 0, 0);
        d.addComponent(BorderLayout.CENTER, ((Form) d).getMenuBar().commandList);
        if (thirdSoftButton) {
            d.addCommand(selectMenuItem);
            d.addCommand(cancelMenuItem);
        } else {
            d.addCommand(cancelMenuItem);
            if (soft.length > 1) {
                d.addCommand(selectMenuItem);
            }
        }
        d.setClearCommand(cancelMenuItem);
        d.setBackCommand(cancelMenuItem);

        if (((Form) d).getMenuBar().commandList instanceof List) {
            ((List) ((Form) d).getMenuBar().commandList).addActionListener(((Form) d).getMenuBar());
        }
        Command result = showMenuDialog(d);
        if (result != cancelMenuItem) {
            Command c = null;
            if (result == selectMenuItem) {
                c = getComponentSelectedCommand(((Form) d).getMenuBar().commandList);
                if (c != null) {
                    ActionEvent e = new ActionEvent(c);
                    c.actionPerformed(e);
                }
            } else {
                c = result;
                // a touch menu will always send its commands on its own...
                if (!UIManager.getInstance().getLookAndFeel().isTouchMenus()) {
                    c = result;
                    if (c != null) {
                        ActionEvent e = new ActionEvent(c);
                        c.actionPerformed(e);
                    }
                }
            }
            // menu item was handled internally in a touch interface that is not a touch menu
            if (c != null) {
                parent.actionCommandImpl(c);
            }
        }
        if (((Form) d).getMenuBar().commandList instanceof List) {
            ((List) ((Form) d).getMenuBar().commandList).removeActionListener(((Form) d).getMenuBar());
        }

        Form upcoming = Display.getInstance().getCurrentUpcoming();
        if (upcoming == parent) {
            d.disposeImpl();
        } else {
            parent.tint = (upcoming instanceof Dialog);
        }
    }

    Button[] getSoftButtons() {
        return soft;
    }

    /**
     * Adds a Command to the MenuBar
     * 
     * @param cmd Command to add
     */
    public void addCommand(Command cmd) {
        // prevent duplicate commands which might happen in some edge cases
        // with the select command
        if (commands.contains(cmd)) {
            return;
        }

        // special case for default commands which are placed at the end and aren't overriden later
        if (soft.length > 2 && cmd == parent.getDefaultCommand()) {
            commands.addElement(cmd);
        } else {
            commands.insertElementAt(cmd, 0);
        }

        if(!(parent instanceof Dialog)) {
            int behavior = getCommandBehavior();
            if(behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR || behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK ||
                    behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT) {
                if(behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK && cmd == parent.getBackCommand()) {
                    return;
                }
                if(parent.getBackCommand() != cmd) {
                    setLayout(new GridLayout(1, getCommandCount()));
                    addComponent(createTouchCommandButton(cmd));
                } else {
                    commands.removeElement(cmd);
                }
                return;
            }
        }

        updateCommands();
    }

    /**
     * Returns the command occupying the given index
     * 
     * @param index offset of the command
     * @return the command at the given index
     */
    public Command getCommand(int index) {
        return (Command) commands.elementAt(index);
    }

    /**
     * Returns number of commands
     * 
     * @return number of commands
     */
    public int getCommandCount() {
        return commands.size();
    }

    /**
     * Add a Command to the MenuBar
     * 
     * @param cmd Command to Add
     * @param index determines the order of the added commands
     */
    protected void addCommand(Command cmd, int index) {
        if (getCommandCount() == 0 && parent != null) {
            installMenuBar();
        }
        // prevent duplicate commands which might happen in some edge cases
        // with the select command
        if (commands.contains(cmd)) {
            return;
        }
        commands.insertElementAt(cmd, index);
        if(!(parent instanceof Dialog)) {
            int behavior = getCommandBehavior();
            if(behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR || 
                    behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK ||
                    behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT) {
                if(behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK && cmd == parent.getBackCommand()) {
                    return;
                }
                if(parent.getBackCommand() != cmd) {
                    setLayout(new GridLayout(1, getCommandCount()));
                    addComponent(index, createTouchCommandButton(cmd));
                    revalidate();
                } else {
                    commands.removeElement(cmd);
                }
                return;
            }
        }
        updateCommands();
    }

    /**
     * Adds the MenuBar on the parent Form
     */
    protected void installMenuBar() {
        if (getParent() == null) {
            int type = Display.getInstance().getCommandBehavior();
            if(type == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT) {
                parent.getTitleArea().addComponent(BorderLayout.EAST, this);
                return;
            }
            if(softkeyCount > 1 || type == Display.COMMAND_BEHAVIOR_BUTTON_BAR ||
                    type == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK) {
                parent.addComponentToForm(BorderLayout.SOUTH, this);
            }
        }
    }

    /**
     * Removes the MenuBar from the parent Form
     */
    protected void unInstallMenuBar() {
        parent.removeComponentFromForm(this);
        updateTitleCommandPlacement();
    }

    /**
     * Remove all commands from the menuBar
     */
    protected void removeAllCommands() {
        commands.removeAllElements();
        int behavior = getCommandBehavior();
        if(behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR || 
                behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK ||
                behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT) {
            removeAll();
            return;
        }
        updateCommands();
    }

    /**
     * Removes a Command from the MenuBar
     * 
     * @param cmd Command to remove
     */
    protected void removeCommand(Command cmd) {
        int behavior = getCommandBehavior();
        if(behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR || 
                behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK ||
                behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT) {
            int i = commands.indexOf(cmd);
            if(i > -1) {
                commands.removeElementAt(i);
                if(getComponentCount() > i) {
                    removeComponent(getComponentAt(i));
                }
                if(getCommandCount() > 0) {
                    setLayout(new GridLayout(1, getCommandCount()));
                }
            }
            return;
        }
        commands.removeElement(cmd);
        updateCommands();
    }

    void addSelectCommand(String selectText) {
        if (thirdSoftButton) {
            if (selectCommand == null) {
                selectCommand = createSelectCommand();
            }
            selectCommand.setCommandName(selectText);
            addCommand(selectCommand);
        }
    }

    void removeSelectCommand() {
        if (thirdSoftButton) {
            removeCommand(selectCommand);
        }
    }

    /**
     * Factory method that returns the Form select Command.
     * This Command is used when Display.getInstance().isThirdSoftButton() 
     * returns true.
     * This method can be overridden to customize the Command on the Form.
     * 
     * @return Command
     */
    protected Command createSelectCommand() {
        return new Command(UIManager.getInstance().localize("select", "Select"));
    }

    /**
     * Factory method that returns the Form Menu select Command.
     * This method can be overridden to customize the Command on the Form.
     * 
     * @return Command
     */
    protected Command createMenuSelectCommand() {
        LookAndFeel lf = UIManager.getInstance().getLookAndFeel();
        return new Command(UIManager.getInstance().localize("select", "Select"), lf.getMenuIcons()[0]);
    }

    /**
     * Factory method that returns the Form Menu cancel Command.
     * This method can be overridden to customize the Command on the Form.
     * 
     * @return Command
     */
    protected Command createMenuCancelCommand() {
        LookAndFeel lf = UIManager.getInstance().getLookAndFeel();
        return new Command(UIManager.getInstance().localize("cancel", "Cancel"), lf.getMenuIcons()[1]);
    }

    /**
     * The MenuBar default implementation shows the menu commands in a List 
     * contained in a Dialog.
     * This method replaces the menu ListCellRenderer of the Menu List.
     * 
     * @param menuCellRenderer
     */
    public void setMenuCellRenderer(ListCellRenderer menuCellRenderer) {
        this.menuCellRenderer = menuCellRenderer;
    }

    /**
     * Returns the Menu Dialog Style
     * 
     * @return Menu Dialog Style
     */
    public Style getMenuStyle() {
        return menuStyle;
    }

    static boolean isLSK(int keyCode) {
        return keyCode == leftSK;
    }

    static boolean isRSK(int keyCode) {
        return keyCode == rightSK || keyCode == rightSK2;
    }

    /**
     * This method returns true if the MenuBar should handle the given keycode.
     * 
     * @param keyCode to determine if the MenuBar is responsible for.
     * @return true if the keycode is a MenuBar related keycode such as softkey,
     * back button, clear button, ...
     */
    public boolean handlesKeycode(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        if (keyCode == leftSK || (keyCode == rightSK || keyCode == rightSK2) || keyCode == backSK ||
                (keyCode == clearSK && clearCommand != null) ||
                (keyCode == backspaceSK && clearCommand != null) ||
                (thirdSoftButton && game == Display.GAME_FIRE)) {
            return true;
        }
        return false;
    }

    /**
     * @inheritDoc
     */
    public void keyPressed(int keyCode) {
        int commandBehavior = getCommandBehavior();
        if(commandBehavior >= Display.COMMAND_BEHAVIOR_BUTTON_BAR) {
            return;
        }
        if (getCommandCount() > 0) {
            if (keyCode == leftSK) {
                if (left != null) {
                    left.pressed();
                }
            } else {
                // it might be a back command or the fire...
                if ((keyCode == rightSK || keyCode == rightSK2)) {
                    if (right != null) {
                        right.pressed();
                    }
                } else {
                    if (Display.getInstance().getGameAction(keyCode) == Display.GAME_FIRE) {
                        main.pressed();
                    }
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void keyReleased(int keyCode) {
        int commandBehavior = getCommandBehavior();
        if(commandBehavior >= Display.COMMAND_BEHAVIOR_BUTTON_BAR && keyCode != backSK && keyCode != clearSK && keyCode != backspaceSK) {
            return;
        }
        if (getCommandCount() > 0) {
            if (softkeyCount < 2 && keyCode == leftSK) {
                if (commandList != null) {
                    Container parent = commandList.getParent();
                    while (parent != null) {
                        if (parent instanceof Dialog && ((Dialog) parent).isMenu()) {
                            return;
                        }
                        parent = parent.getParent();
                    }
                }
                showMenu();
                return;
            } else {
                if (keyCode == leftSK) {
                    if (left != null) {
                        left.released();
                    }
                    return;
                } else {
                    // it might be a back command...
                    if ((keyCode == rightSK || keyCode == rightSK2)) {
                        if (right != null) {
                            right.released();
                        }
                        return;
                    } else {
                        if (Display.getInstance().getGameAction(keyCode) == Display.GAME_FIRE) {
                            main.released();
                            return;
                        }
                    }
                }
            }
        }

        // allows a back/clear command to occur regardless of whether the
        // command was added to the form
        Command c = null;
        if (keyCode == backSK) {
            // the back command should be invoked
            c = parent.getBackCommand();
        } else {
            if (keyCode == clearSK || keyCode == backspaceSK) {
                c = getClearCommand();
            }
        }
        if (c != null) {
            ActionEvent ev = new ActionEvent(c, keyCode);
            c.actionPerformed(ev);
            if (!ev.isConsumed()) {
                parent.actionCommandImpl(c);
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void refreshTheme() {
        super.refreshTheme();
        if (menuStyle.isModified()) {
            menuStyle.merge(UIManager.getInstance().getComponentStyle("Menu"));
        } else {
            menuStyle = UIManager.getInstance().getComponentStyle("Menu");
        }
        if (menuCellRenderer != null) {
            List tmp = new List();
            tmp.setListCellRenderer(menuCellRenderer);
            tmp.refreshTheme();
        }
        for (int iter = 0; iter < soft.length; iter++) {
            updateSoftButtonStyle(soft[iter]);
        }

        revalidate();
    }

    /*private void fixCommandAlignment() {
        if (left != null) {
            if (parent.isRTL()) {
                left.setAlignment(Label.RIGHT);
                right.setAlignment(Label.LEFT);
            } else {
                left.setAlignment(Label.LEFT);
                right.setAlignment(Label.RIGHT);
            }
            if(main != null && main != left && main != right) {
                main.setAlignment(CENTER);
            }
        }
    }*/

    /**
     * A menu is implemented as a dialog, this method allows you to override dialog
     * display in order to customize the dialog menu in various ways
     * 
     * @param menu a dialog containing menu options that can be customized
     * @return the command selected by the user in the dialog (not menu) Select 
     * or Cancel
     */
    protected Command showMenuDialog(Dialog menu) {
        boolean pref = UIManager.getInstance().isThemeConstant("menuPrefSizeBool", false);
        int height;
        int marginLeft;
        int marginRight = 0;
        if(pref) {
            Container dialogContentPane = menu.getDialogComponent();
            marginLeft = parent.getWidth() - (dialogContentPane.getPreferredW() +
                    menu.getStyle().getPadding(LEFT) +
                    menu.getStyle().getPadding(RIGHT));
            marginLeft = Math.max(0, marginLeft);
            if(parent.getSoftButtonCount() > 1) {
                height = parent.getHeight() - parent.getSoftButton(0).getParent().getPreferredH() - dialogContentPane.getPreferredH();
            } else {
                height = parent.getHeight() - dialogContentPane.getPreferredH();
            }
            height = Math.max(0, height);
        } else {
            float menuWidthPercent = 1 - Float.parseFloat(UIManager.getInstance().getThemeConstant("menuWidthPercent", "75")) / 100;
            float menuHeightPercent = 1 - Float.parseFloat(UIManager.getInstance().getThemeConstant("menuHeightPercent", "50")) / 100;
            height = (int) (parent.getHeight() * menuHeightPercent);
            marginLeft = (int) (parent.getWidth() * menuWidthPercent);
        }

        if (isReverseSoftButtons()) {
            marginRight = marginLeft;
            marginLeft = 0;
        }
        if (UIManager.getInstance().getLookAndFeel().isTouchMenus() && UIManager.getInstance().isThemeConstant("PackTouchMenuBool", true)) {
            return menu.showPacked(BorderLayout.SOUTH, true);
        } else {
            return menu.show(height, 0, marginLeft, marginRight, true);
        }
    }

    /**
     * Allows an individual form to reverse the layout direction of the softbuttons, this method is RTL
     * sensitive and might reverse the result based on RTL state
     * 
     * @return The value of UIManager.getInstance().getLookAndFeel().isReverseSoftButtons()
     */
    protected boolean isReverseSoftButtons() {
        LookAndFeel lf = UIManager.getInstance().getLookAndFeel();
        if (isRTL()) {
            return !lf.isReverseSoftButtons();
        }
        return lf.isReverseSoftButtons();
    }

    /**
     * Calculates the amount of columns to give to the touch commands within the 
     * grid
     * 
     * @param grid container that will be arranged in the grid containing the 
     * components
     * @return an integer representing the touch command grid size
     */
    protected int calculateTouchCommandGridColumns(Container grid) {
        int count = grid.getComponentCount();
        int maxWidth = 0;
        for (int iter = 0; iter < count; iter++) {
            Component c = grid.getComponentAt(iter);
            Style s = c.getUnselectedStyle(); 
            // bidi doesn't matter since this is just a summary of width
            maxWidth = Math.max(maxWidth, 
                    c.getPreferredW() + 
                    s.getMargin(false, LEFT) + s.getMargin(false, RIGHT));
        }
        return Math.max(2, Display.getInstance().getDisplayWidth() / maxWidth);
    }

    /**
     * Creates a touch command for use as a touch menu item
     * 
     * @param c command to map into the returned button
     * @return a button that would fire the touch command appropriately
     */
    protected Button createTouchCommandButton(Command c) {
        Button b = new Button(c);
        if(b.getIcon() == null) {
            // some themes look awful without any icon
            b.setIcon((Image)UIManager.getInstance().getThemeImageConstant("defaultCommandImage"));
        }
        b.setTactileTouch(true);
        b.setTextPosition(Label.BOTTOM);
        b.setEndsWith3Points(false);
        b.setUIID("TouchCommand");
        return b;
    }

    /**
     * Creates the component containing the commands within the given vector
     * used for showing the menu dialog, this method calls the createCommandList
     * method by default however it allows more elaborate menu creation.
     *
     * @param commands list of command objects
     * @return Component that will result in the parent menu dialog recieving a command event
     */
    protected Component createCommandComponent(Vector commands) {
        // Create a touch based menu interface
        if (UIManager.getInstance().getLookAndFeel().isTouchMenus()) {
            Container menu = new Container();
            menu.setScrollableY(true);
            for (int iter = 0; iter < commands.size(); iter++) {
                Command c = (Command)commands.elementAt(iter);
                menu.addComponent(createTouchCommandButton(c));
            }
            if(!UIManager.getInstance().isThemeConstant("touchCommandFlowBool", false)) {
                int cols = calculateTouchCommandGridColumns(menu);
                if(cols > getCommandCount()) {
                    cols = getCommandCount();
                }
                int rows = Math.max(1, getCommandCount() / cols + (getCommandCount() % cols != 0 ? 1 : 0) );
                if(rows > 1) {
                    // try to prevent too many columns concentraiting within a single row
                    int remainingColumns = (rows * cols) % getCommandCount();
                    int newCols = cols;
                    int newRows = rows;
                    while(remainingColumns != 0 && remainingColumns > 1 && newCols >= 2) {
                        newCols--;
                        newRows = Math.max(1, getCommandCount() / newCols + (getCommandCount() % newCols != 0 ? 1 : 0) );
                        if(newRows != rows) {
                            break;
                        }
                        remainingColumns = (newRows * newCols) % getCommandCount();
                    }
                    if(newRows == rows) {
                        cols = newCols;
                        rows = newRows;
                    }
                }
                GridLayout g = new GridLayout(rows, cols);
                g.setFillLastRow(UIManager.getInstance().isThemeConstant("touchCommandFillBool", true));
                menu.setLayout(g);
            } else {
                ((FlowLayout)menu.getLayout()).setFillRows(true);
            }
            menu.setPreferredW(Display.getInstance().getDisplayWidth());
            return menu;
        }
        return createCommandList(commands);
    }

    /**
     * This method returns a Vector of Command objects
     * 
     * @return Vector of Command objects
     */
    protected Vector getCommands() {
        return commands;
    }

    
    /**
     * Creates the list component containing the commands within the given vector
     * used for showing the menu dialog
     * 
     * @param commands list of command objects
     * @return List object
     */
    protected List createCommandList(Vector commands) {
        List l = new List(commands);
        l.setUIID("CommandList");
        Component c = (Component) l.getRenderer();
        c.setUIID("Command");
        c = l.getRenderer().getListFocusComponent(l);
        c.setUIID("CommandFocus");

        l.setFixedSelection(List.FIXED_NONE_CYCLIC);
        if(UIManager.getInstance().isThemeConstant("menuPrefSizeBool", false)) {
            // an entry way down in the list might be noticeably wider
            l.setListSizeCalculationSampleCount(50);
        }
        return l;
    }

    Command getComponentSelectedCommand(Component cmp) {
        if (cmp instanceof List) {
            List l = (List) cmp;
            return (Command) l.getSelectedItem();
        } else {
            cmp = cmp.getComponentForm().getFocused();
            if (cmp instanceof Button) {
                return ((Button) cmp).getCommand();
            }
        }
        // nothing to do for this case...
        return null;
    }

    /**
     * This method returns the select menu item, when a menu is opened
     * @return select Command
     */
    protected Command getSelectMenuItem() {
        return selectMenuItem;
    }

    /**
     * This method returns the cancel menu item, when a menu is opened
     * @return cancel Command
     */
    protected Command getCancelMenuItem() {
        return cancelMenuItem;
    }
    
    
}