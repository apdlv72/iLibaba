package com.apdlv.ilibaba.frotect;

import com.apdlv.ilibaba.frotect.BTFrotectSerialService.BTSerialBinder;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


public class BTFrotectSerialServiceConnection implements ServiceConnection
{
    private Handler         mHandler;
    private BTFrotectSerialService mService = null;

    public BTFrotectSerialServiceConnection(Handler handler)
    {
	super();
	this.mHandler = handler;
    }
    
    
    public void onServiceConnected(ComponentName className, IBinder service) 
    {
	// We've bound to LocalService, cast the IBinder and get LocalService instance
	BTSerialBinder  btBinder   = (BTSerialBinder) service;
	mService = btBinder.getService();
	Log.d(FrotectActivity.TAG, "Got BT frotect serial service " + mService);
	setHandler(mHandler);
	Log.d(FrotectActivity.TAG, "Registered handler with BT service ");
    }


    public void setHandler(Handler handler)
    {
	mService.setHandler(handler, true);
    }
    

    public void onServiceDisconnected(ComponentName arg0) 
    {
	synchronized (this)
	{
	    if (null!=mService)
	    {
		mService.removeHandler(mHandler);
		mService = null;
	    }
	}
    }

    public void sendLine(String s)
    {
	synchronized (this)
	{
	    if (null!=mService)
	    {
		s = s + "\r\n";
		System.out.println("Sending string '" + s + "'");
		mService.write(s.getBytes());
	    }
	}
    }

    public void connect(BluetoothDevice device)
    {
	if (null!=mService)
	{
	    mService.connect(device);
	}
    }

    public void disconnect()
    {
	if (null!=mService)
	{
	    mService.disconnect();
	}	
    }


    static final String TAG = BTFrotectSerialServiceConnection.class.getSimpleName();
    
    public void unbind(Context applicationContext)
    {
	synchronized (this)
	{
	    if (null!=mService)
	    {
		Log.i(TAG, "unbind: removing handler and unbinding from service");
		mService.removeHandler(mHandler);
		applicationContext.unbindService(this);
		mService = null;
	    }
	    else
	    {
		Log.i(TAG, "unbind: no service available (any more?)");
	    }
	}
    }
    
    
}