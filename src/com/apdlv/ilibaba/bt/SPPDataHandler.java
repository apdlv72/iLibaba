package com.apdlv.ilibaba.bt;

import java.util.HashSet;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SPPDataHandler extends Handler
{
    private boolean mConnected;
    
    final static String TAG = SPPDataHandler.class.getSimpleName();
    
    public void addDataListener(SPPDataListener l)
    {
        if (null!=l)
        {
            dataListeners.add(l);
        }
    }

    public void removeDataListener(SPPDataListener l)
    {
        if (null!=l)
        {
            dataListeners.remove(l);
        }
    }

    public void addStatusListener(SPPStatusListener l)
    {
        if (null!=l)
        {
            statusListeners.add(l);
            l.setStatus(mConnected);
        }
    }

    public void removeStatusListener(SPPStatusListener l)
    {
        if (null!=l)
        {
            statusListeners.remove(l);
        }
    }

    @Override
    public void handleMessage(Message msg) 
    {    
        int what = msg.what;
        //onDebugMessage("Got message " + what + "(" + SPPService.message2String(what) + ")");
        switch (what) 
        {

        case SPPService.MESSAGE_HELLO:
        	onServiceConnected();
        	break;

        case SPPService.MESSAGE_DEBUG_MSG:
        	onDebugMessage((String) msg.obj);
        	break;

        case SPPService.MESSAGE_STATE_CHANGE:
            //onDebugMessage("MESSAGE_STATE_CHANGE: " + msg.arg1 + "(" + SPPService.state2String(msg.arg1) + ")");
            
            Log.e(TAG, "MESSAGE_STATE_CHANGE: " + SPPService.state2String(msg.arg1));
            
            switch (msg.arg1) 
            {
            case SPPService.STATE_CONNECTING:
                onConnectingDevice();
                break;
                
            case SPPService.STATE_CONNECTED:
                mConnected = true;
                dispatchDeviceConnect(name, addr);
                break;
                
            case SPPService.STATE_DISCONNECTED:
                mConnected = false;
                dispatchDeviceDisconnect(name, addr);
                break;
                
            case SPPService.STATE_CONN_TIMEOUT:
                mConnected = false;
                onTimeout();
                break;
                
            case SPPService.STATE_FAILED:
            case SPPService.STATE_LOST:
        	mConnected = false;
        	dispatchDeviceDisconnect(name, addr);
        	break;
        	
            case SPPService.STATE_NONE:
                onIdle();
                break;
            }
            break;
    	
        case SPPService.MESSAGE_READLINE:
        	dispatchLine((String) msg.obj);
        	break;

        case SPPService.MESSAGE_READ:                
        	onDebugMessage("Got MESSAGE_READ "+ msg);
        	onDataReceived((byte[]) msg.obj, msg.arg1);
        	break;

        case SPPService.MESSAGE_DEVICE_NAME:
        	// save the connected device's name
            	this.name = (String)msg.obj;
        	onDeviceName(name);
        	break;

        case SPPService.MESSAGE_DEVICE_ADDR:
        	// save the connected device's addr
        	this.addr = (String)msg.obj;
        	onDeviceAddr(addr);
        	break;

        case SPPService.MESSAGE_TOAST:
        	onToast((String) msg.obj);
        	break;
        }
    }

    private void dispatchDeviceConnect(String name, String addr)
    {
        onDeviceConnected();
        
        for (SPPStatusListener l : statusListeners)
        {
            l.onConnect(name, addr);
        }
    }

    private void dispatchDeviceDisconnect(String name, String addr)
    {
        onDeviceDisconnected();
        
        for (SPPStatusListener l : statusListeners)
        {
            l.onDisconnect(name, addr);
        }
    }

    private void dispatchLine(String receivedLine)
    {
	// subclass first
        onLineReceived(receivedLine);
        
        // then the listener
        for (SPPDataListener l : dataListeners)
        {
            l.onLineReceived(receivedLine);
        }
    }

    // methods to be overridden by subclasses in order to receive various events:
    protected void onServiceConnected() {}
    protected void onDeviceAddr(String deviceAddr) {}
    protected void onDeviceName(String deviceName) {}
    protected void onConnectingDevice() {}
    protected void onDeviceConnected() {}
    protected void onDeviceDisconnected() {}
    protected void onTimeout() {}
    protected void onIdle() {}
    protected void onDataReceived(byte[] readBuf, int len) {}
    protected void onLineReceived(String receivedLine) {}
    protected void onToast(String msg) {}
    protected void onDebugMessage(String msg) {}

    
    private HashSet<SPPDataListener> dataListeners = new HashSet<SPPDataListener>();
    private HashSet<SPPStatusListener> statusListeners = new HashSet<SPPStatusListener>();
    private String name;
    private String addr;
}