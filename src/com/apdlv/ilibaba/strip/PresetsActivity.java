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

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.apdlv.ilibaba.R;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class PresetsActivity extends Activity implements OnItemLongClickListener {
    // Debugging
    private static final String TAG = "PresetsActivity";
    private static final boolean D = true;

    public static final int REQUEST_CONNECT_DEVICE = 1;

    // Return Intent extra
    public static String EXTRA_PRESET_BUNDLE = "preset_bundle";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	// Setup the window
	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	setContentView(R.layout.preset_list);

	// Set result CANCELED incase the user backs out
	setResult(Activity.RESULT_CANCELED);

	// Initialize the button to perform device discovery
	Button scanButton = (Button) findViewById(R.id.button_scan);
	/*
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
	 */

	String presets[] = { "One", "Two", "Three", "Four", "Five", "Six" }; 

	// Initialize array adapters. One for already paired devices and
	// one for newly discovered devices
	mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.preset_name);
	//mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

	// Find and set up the ListView for paired devices
	ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
	pairedListView.setAdapter(mPairedDevicesArrayAdapter);
	pairedListView.setOnItemClickListener(mDeviceClickListener);

	pairedListView.setOnItemLongClickListener(this);

	for (String p : presets) {
	    mPairedDevicesArrayAdapter.add(p);
	}

    }

    @Override
    protected void onDestroy() {
	super.onDestroy();
    }


    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
	public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
	    /*
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();
	     */
	    // Get the device MAC address, which is the last 17 chars in the View
	    String name = ((TextView) v).getText().toString();
	    /*
            //String address = info.substring(info.length() - 17);
            Bundle b = new Bundle();
            b.putString("NAME", name);
            b.putInt("RND", 7);
	     */

	    // Create the result Intent and include the MAC address
	    Intent intent = new Intent();
	    intent.putExtra("ACTION", "load");
	    intent.putExtra("NAME", name);

	    // Set result and finish this Activity
	    setResult(Activity.RESULT_OK, intent);
	    finish();
	}
    };

    public boolean onItemLongClick(AdapterView<?> arg0, View view, int arg2, long arg3)
    {
	Log.d(TAG, "Long click on " + view);
	if (view instanceof TextView)
	{
	    String name = ((TextView)view).getText().toString();
	    Intent intent = new Intent();
	    intent.putExtra("ACTION", "save");
	    intent.putExtra("NAME", name);

	    // Set result and finish this Activity
	    setResult(Activity.RESULT_OK, intent);
	    finish();
	}
	return false;
    }
}