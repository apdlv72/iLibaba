/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apdlv.ilibaba.strip;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.apdlv.ilibaba.R;

public class LampListDialog extends Dialog 
{    
    private int numberLamps;
    private StripControlActivity activity;
    private String[] lampNames;

    public LampListDialog(StripControlActivity activity, String lampNames[])
    {
	super(activity);
	this.activity = activity;
	this.numberLamps = lampNames.length;
	this.lampNames = lampNames;
    }

    // Debugging
    //private static final String TAG = "LampListDialog";
    //private static final boolean D = true;

    // Member fields
    private ArrayAdapter<String> mLampArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.lamp_list);
        
        Context context = getContext();
        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mLampArrayAdapter = new ArrayAdapter<String>(context, R.layout.lamp_name);
        //mNewDevicesArrayAdapter = new ArrayAdapter<String>(context, R.layout.lamp_name);

        // Find and set up the ListView for paired devices
        ListView lampListView = (ListView) findViewById(R.id.available_lamps);
        lampListView.setAdapter(mLampArrayAdapter);
        lampListView.setOnItemClickListener(mClickListener);

        if (numberLamps>1)
        {
            for (int i=0; i<numberLamps; i++)
            {        	
                String desc = i<lampNames.length ? lampNames[i] : "no description";
                desc = desc.replaceAll("_", " ");
                mLampArrayAdapter.add("Switch " + (i+1) + "\n" + desc);
            }
        }
        else
        {
            mLampArrayAdapter.add("\nOff\n");
            mLampArrayAdapter.add("\nOn\n");
            mLampArrayAdapter.add("\nToggle\n");
        }        
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mClickListener = new OnItemClickListener() 
    {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) 
        {
                if (numberLamps>1)
            {
                int lampNo = arg2;
        	activity.setCmd("P" + lampNo + "=2"); // send toggle command
            }
            else
            {
        	int state = arg2; // 0=off, 1=on, 2=toggle
        	activity.setCmd("P=" + state); // send toggle command
            }            
        }
    };

}
