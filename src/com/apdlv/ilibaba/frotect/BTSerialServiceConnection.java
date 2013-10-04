package com.apdlv.ilibaba.frotect;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.apdlv.ilibaba.strip.BTSerialService;
import com.apdlv.ilibaba.strip.BTSerialService.BTSerialBinder;

public class BTSerialServiceConnection implements ServiceConnection
{
    private Handler         mHandler;
    private BTSerialService mService;

    public BTSerialServiceConnection(Handler handler)
    {
        super();
        this.mHandler = handler;
    }
    
    public void onServiceConnected(ComponentName className, IBinder service) 
    {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        BTSerialBinder  btBinder   = (BTSerialBinder) service;
        BTSerialService mBTService = btBinder.getService();
        Log.d(FrotectActivity.TAG, "Got BT service " + mBTService);
        mBTService.setHandler(mHandler, true);
        Log.d(FrotectActivity.TAG, "Registered handler with BT service ");
    }

    public void onServiceDisconnected(ComponentName arg0) 
    {
	synchronized (this)
        {
	    if (null!=mService)
	    {
		mService.setHandler(null, true);
		mService = null;
	    }
        }
    }
    
    public void sendString(String s)
    {
	synchronized (this)
        {
	    if (null!=mService)
	    {
		mService.write(s.getBytes());
	    }
        }
    }
}