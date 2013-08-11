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

import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.util.EventDispatcher;
import java.util.Hashtable;

/**
 * The browser component is an interface to an embeddable native platform browser on platforms
 * that support embedding the native browser in place, if you need wide compatibility and flexibility
 * you should check out the HTMLComponent which provides a lightweight 100% cross platform
 * web component.
 * This component will only work on platforms that support embedding a native browser which
 * exclude earlier versions of Blackberry devices and J2ME devices.<br>
 * Its recommended that you place this component in a fixed position (none scrollable) on the screen without other
 * focusable components to prevent confusion between focus authority and allow the component to scroll
 * itself rather than LWUIT making that decision for it.
 *
 * @author Shai Almog
 */
public class BrowserComponent extends Container {
    private Hashtable listeners;
    private PeerComponent internal;

    /**
     * This constructor will work as expected when a browser component is supported, see isNativeBrowserSupported()
     */
    public BrowserComponent() {
        setUIID("BrowserComponent");
        PeerComponent c = Display.getInstance().getImplementation().createBrowserComponent(this);
        setLayout(new BorderLayout());
        addComponent(BorderLayout.CENTER, c);
        internal = c;
    }

    /**
     * Returns true if the platform supports embedding a native browser component
     *
     * @return true if native browsing is supported
     */
    public static boolean isNativeBrowserSupported() {
        return Display.getInstance().getImplementation().isNativeBrowserComponentSupported();
    }

    /**
     * This method allows customizing the properties of a web view in various ways including platform specific settings.
     * When a property isn't supported by a specific platform it is just ignored.
     *
     * @param key see the documentation with the LWUIT Implementation for further details
     * @param value see the documentation with the LWUIT Implementation for further details
     */
    public void setProperty(String key, Object value) {
        Display.getInstance().getImplementation().setBrowserProperty(internal, key, value);
    }

    /**
     * The page title
     * @return the title
     */
    public String getTitle() {
        return Display.getInstance().getImplementation().getBrowserTitle(internal);
    }

    /**
     * The page URL
     * @return the URL
     */
    public String getURL() {
        return Display.getInstance().getImplementation().getBrowserURL(internal);
    }

    /**
     * Sets the page URL, jar: URL's must be supported by the implementation
     * @param url  the URL
     */
    public void setURL(String url) {
        Display.getInstance().getImplementation().setBrowserURL(internal, url);
    }

    /**
     * Reload the current page
     */
    public void reload() {
        Display.getInstance().getImplementation().browserReload(internal);
    }

    /**
     * Indicates whether back is currently available
     * @return true if back should work
     */
    public boolean hasBack() {
        return Display.getInstance().getImplementation().browserHasBack(internal);
    }

    /**
     * Indicates whether forward is currently available
     * @return true if forward should work
     */
    public boolean hasForward() {
        return Display.getInstance().getImplementation().browserHasForward(internal);
    }

    /**
     * Navigates back in the history
     */
    public void back() {
        Display.getInstance().getImplementation().browserBack(internal);
    }

    /**
     * Navigates forward in the history
     */
    public void forward() {
        Display.getInstance().getImplementation().browserForward(internal);
    }

    /**
     * Clears navigation history
     */
    public void clearHistory() {
        Display.getInstance().getImplementation().browserClearHistory(internal);
    }

    /**
     * Shows the given HTML in the native viewer
     *
     * @param html HTML web page
     * @param baseUrl base URL to associate with the HTML
     */
    public void setPage(String html, String baseUrl) {
        Display.getInstance().getImplementation().setBrowserPage(internal, html, baseUrl);
    }
    
    private EventDispatcher getEventDispatcher(String type, boolean autoCreate) {
        if(listeners == null) {
            if(!autoCreate) {
                return null;
            }
            listeners = new Hashtable();
            EventDispatcher ev = new EventDispatcher();
            listeners.put(type, ev);
            return ev;
        }
        EventDispatcher ev = (EventDispatcher)listeners.get(type);
        if(ev == null) {
            if(autoCreate) {
                ev = new EventDispatcher();
                listeners.put(type, ev);
            }
        }
        return ev;
    }
    
    /**
     * Adds a listener to the given event type name, event type names are platform specific but some 
     * must be fired for all platforms and will invoke the action listener when the appropriate event loads
     * 
     * @param type platform specific but must support: onLoad, onError
     * @param listener callback for the event
     */
    public void addWebEventListener(String type, ActionListener listener) {
        getEventDispatcher(type, true).addListener(listener);
    }

    /**
     * Removes the listener, see addWebEventListener for details
     *
     * @param type see addWebEventListener for details
     * @param listener see addWebEventListener for details
     */
    public void removeWebEventListener(String type, ActionListener listener) {
        EventDispatcher e = getEventDispatcher(type, false);
        if(e != null) {
            e.removeListener(listener);
            if(!e.hasListeners()) {
                listeners.remove(type);
            }
        }
    }

    /**
     * Used internally by the implementation to fire an event from the native browser widget
     * 
     * @param type the type of the event
     * @param ev the event
     */
    public void fireWebEvent(String type, ActionEvent ev) {
        EventDispatcher e = getEventDispatcher(type, false);
        if(e != null) {
            e.fireActionEvent(ev);
        }
    }

    /**
     * Executes the given JavaScript string within the current context
     * 
     * @param javaScript the JavaScript string
     */
    public void execute(String javaScript) {
        Display.getInstance().getImplementation().browserExecute(internal, javaScript);
    }

    /**
     * Allows exposing the given object to JavaScript code so the JavaScript code can invoke methods
     * and access fields on the given object. Notice that on RIM devices which don't support reflection
     * this object must implement the propriatery Scriptable interface
     * http://www.blackberry.com/developers/docs/5.0.0api/net/rim/device/api/script/Scriptable.html
     *
     * @param o the object to invoke, notice all public fields and methods would be exposed to JavaScript
     * @param name the name to expose within JavaScript
     */
    public void exposeInJavaScript(Object o, String name) {
        Display.getInstance().getImplementation().browserExposeInJavaScript(internal, o, name);
    }
}
