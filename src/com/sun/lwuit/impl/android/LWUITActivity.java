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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import com.sun.lwuit.Command;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.events.ActionEvent;
import java.lang.reflect.Method;
import java.util.Vector;

public abstract class LWUITActivity extends Activity {
    
    // There might be some fun with static variables because of their process lifetime, which
    // is potentially longer than the lifetime of an activity.
    // http://www.mail-archive.com/android-developers@googlegroups.com/msg46584.html
    //
    // Enable this feature to kill the current porcess when the onDestroy() method is
    // called.
    // 
    // I prefer to have this enabled which requires to finish the application
    // shutdown procedures BEFORE your implementation of this class calls through
    // to LWUITActivity.onDestroy(), if you override onDestroy(). But since people 
    // might override that method without much consideration I disable this feature by default. 
    public static boolean FORCE_PROCESS_END_ON_DESTROY = false;

    /**
     * keep track of current activity instance.
     */
    public static LWUITActivity currentActivity = null;
    /**
     * lwuit commands to be used as native menu items.
     */
    private Command[] commands = null;
    private boolean invalidateOptionsMenuAvailable = false;
    private boolean invalidateOptionsMenuChecked = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("LWUIT", "onCreate");
        super.onCreate(savedInstanceState);

        if (FORCE_PROCESS_END_ON_DESTROY && currentActivity != null) {
            Log.w("LWUIT", "previous activity found!!!");
            this.shootSelf();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentActivity = this;
    }

    @Override
    protected void onStart() {
        Log.d("LWUIT", "onStart");
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        // if you override this method AND if you have FORCE_PROCESS_END_ON_DESTROY
        // enabled you need to reach a safe application state BEFORE you call through
        // to this method.
        Log.d("LWUIT", "onDestroy");
        super.onDestroy();
        if (FORCE_PROCESS_END_ON_DESTROY) {
            shootSelf();
        }
    }

    @Override
    protected void onPause() {
        Log.d("LWUIT", "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d("LWUIT", "onResume");
        super.onResume();
    }

    @Override
    protected void onRestart() {
        Log.d("LWUIT", "onRestart");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.d("LWUIT", "onStop");
        super.onStop();
    }

    /**
     * idea is to make sure the process really ends after this activity
     * is gone.
     */
    private void shootSelf() {
        new Thread(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(1000);
                    android.os.Process.killProcess(android.os.Process.myPid());
                } catch (Throwable e) {
                    Log.e("LWUIT", "problem killing self.", e);
                }
            }
        }).start();
    }

    /**
     * too bad we cannot control this from the view, can we?
     */
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final Command[] commands = this.commands;
        if (commands == null) {
            // no menu.
            return false;
        }
        menu.clear();
        for (int i = 0; i < commands.length; i++) {
            String name = commands[i].getCommandName();
            MenuItem item = menu.add(Menu.NONE, i, Menu.NONE, name);
            Image icon = commands[i].getIcon();
            if (icon != null) {
                Bitmap b = (Bitmap) icon.getImage();
                BitmapDrawable d = new BitmapDrawable(b);
                item.setIcon(d);
            }
        }
        return true;
    }

    public void refreshOptionsMenu(Vector commands) {

        this.commands = null;
        if (commands != null && commands.size() > 0) {
            Command[] tmp = new Command[commands.size()];
            for (int i = 0; i < commands.size(); i++) {
                tmp[i] = (Command) commands.elementAt(i);
            }
            this.commands = tmp;
        }

        // from the android docs:

        // On Android 2.3 and lower, the system calls onPrepareOptionsMenu() 
        // each time the user opens the Options Menu.

        // On Android 3.0 and higher, you must call invalidateOptionsMenu() when you 
        // want to update the menu, because the menu is always open. The system will 
        // then call onPrepareOptionsMenu() so you can update the menu items.

        if (invalidateOptionsMenuChecked
                && !invalidateOptionsMenuAvailable) {
            return;
        }

        try {
            Method m = Activity.class.getMethod("invalidateOptionsMenu", null);
            if (m == null) {
                return;
            }
            m.invoke(this, null);
            invalidateOptionsMenuAvailable = true;
        } catch (Exception e) {
            Log.e("LWUIT", "problem with refreshOptionsMenu", e);
        } finally {
            invalidateOptionsMenuChecked = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // from the android docs:
        //Note: On Android 2.3 and lower, the system calls onCreateOptionsMenu() to create the Options Menu when the user 
        //opens it for the first time, but on Android 3.0 and greater, the system creates it as soon as the 
        //activity is created.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final Command[] commands = this.commands;
        if (commands != null && item.getItemId() >= 0 && item.getItemId() < commands.length) {
            Display.getInstance().callSerially(new Runnable() {

                public void run() {
                    Form current = Display.getInstance().getCurrent();
                    if (current != null) {
                        current.dispatchCommand(commands[item.getItemId()], new ActionEvent(commands[item.getItemId()]));
                    }
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    /**
     * utility method to query device dimensions that might be different from the dimensions
     * that the view reports.
     * @return 
     */
    public static int getDeviceDisplayWidth() {
        return ((WindowManager) LWUITActivity.currentActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
    }

    /**
     * utility method to query device dimensions that might be different from the dimensions
     * that the view reports.
     * @return 
     */
    public static int getDeviceDisplayHeight() {
        return ((WindowManager) LWUITActivity.currentActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight();
    }
    
    
    
}
