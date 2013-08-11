package com.priatama.versusapp.activity;

import android.os.Bundle;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Label;
import com.sun.lwuit.impl.android.LWUITActivity;
import com.sun.lwuit.io.Storage;

public class MainActivity extends LWUITActivity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);Display.init(this);
        // storage is initialized with activity as parameter!
        Storage.init(this);
        Display.getInstance().callSerially(new Runnable(){

            @Override
            public void run() {
                (new TestForm()).show();
//                (new Detikcom()).startApp();
            }
        });
    }
    
    public String getFullApplicationPath(){
        return "com.creacle.detik.Detikcom";
        // (make sure that obfuscators don't mess with the class name and path.)
    }
    
    class TestForm extends Form {
        public TestForm() {
            super("tes");
            Label TestLabel = new Label("test");
            TestLabel.getStyle().setBgTransparency(0);
            addComponent(TestLabel);
        }
    }
}
