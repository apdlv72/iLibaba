package com.apdlv.ilibaba.frotect;

import java.util.HashSet;

import android.os.Handler;
import android.os.Message;

import com.apdlv.ilibaba.gate.BluetoothSerialService;

public class BTDataHandler extends Handler
{
    public void addDataListener(BTDataListener l)
    {
        dataListeners.add(l);
    }

    public void removeDataListener(BTDataListener l)
    {
        dataListeners.remove(l);
    }

    public void addConnectionStatusListener(BTConnectionStatusListener l)
    {
	statusListeners.add(l);
    }

    public void removeConnectionStatusListener(BTConnectionStatusListener l)
    {
	statusListeners.remove(l);
    }

    @Override
    public void handleMessage(Message msg) 
    {    
        int what = msg.what;
        //onDebugMessage("Got message " + what + "(" + BTFrotectSerialService.message2String(what) + ")");
        switch (what) 
        {

        case BTFrotectSerialService.MESSAGE_HELLO:
        	onServiceConnected();
        	break;

        case BTFrotectSerialService.MESSAGE_DEBUG_MSG:
        	onDebugMessage((String) msg.obj);
        	break;

        case BTFrotectSerialService.MESSAGE_STATE_CHANGE:
            //onDebugMessage("MESSAGE_STATE_CHANGE: " + msg.arg1 + "(" + BTFrotectSerialService.state2String(msg.arg1) + ")");                
            
            switch (msg.arg1) 
            {
            case BluetoothSerialService.STATE_CONNECTING:
                onConnectingDevice();
                break;
                
            case BluetoothSerialService.STATE_CONNECTED:
                dispatchDeviceConnect(name, addr);
                break;
                
            case BluetoothSerialService.STATE_DISCONNECTED:
                dispatchDeviceDisconnect(name, addr);
                break;
                
            case BluetoothSerialService.STATE_TIMEOUT:
                onTimeout();
                break;
            case BluetoothSerialService.STATE_LISTEN:
            case BluetoothSerialService.STATE_NONE:
                onIdle();
                break;
            }
            break;
    	
        case BTFrotectSerialService.MESSAGE_READLINE:
        	dispatchLine((String) msg.obj);
        	break;

        case BTFrotectSerialService.MESSAGE_READ:                
        	onDebugMessage("Got MESSAGE_READ "+ msg);
        	onDataReceived((byte[]) msg.obj, msg.arg1);
        	break;

        case BTFrotectSerialService.MESSAGE_DEVICE_NAME:
        	// save the connected device's name
            	this.name = (String)msg.obj;
        	onDeviceName(name);
        	break;

        case BTFrotectSerialService.MESSAGE_DEVICE_ADDR:
        	// save the connected device's addr
        	this.addr = (String)msg.obj;
        	onDeviceAddr(addr);
        	break;

        case BTFrotectSerialService.MESSAGE_TOAST:
        	onToast((String) msg.obj);
        	break;
        }
    }

    private void dispatchDeviceConnect(String name, String addr)
    {
        onDeviceConnected();
        
        for (BTConnectionStatusListener l : statusListeners)
        {
            l.onConnect(name, addr);
        }
    }

    private void dispatchDeviceDisconnect(String name, String addr)
    {
        onDeviceDisconnected();
        
        for (BTConnectionStatusListener l : statusListeners)
        {
            l.onDisconnect(name, addr);
        }
    }

    private void dispatchLine(String receivedLine)
    {
	// subclass first
        onLineReceived(receivedLine);
        
        // then the listener
        for (BTDataListener l : dataListeners)
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

    
    private HashSet<BTDataListener> dataListeners = new HashSet<BTDataListener>();
    private HashSet<BTConnectionStatusListener> statusListeners = new HashSet<BTConnectionStatusListener>();
    private String name;
    private String addr;
}