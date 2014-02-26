/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed timeout in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apdlv.ilibaba.bt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class SPPService  extends Service
{
    public static final String KEY_DEVICE_NAME = "KEY_DEVICE_NAME";
    public static final String KEY_DEVICE_ADDR = "KEY_DEVICE_ADDR";

    public interface OnBluetoothListener
    {
	void onBTStateChanged(int state, String msg);
	void onBTDataReceived(byte data[], int len);
    }

    public SPPService()
    {
	Log.d(TAG, "instantiated");
    }

    @Override
    public IBinder onBind(Intent intent) 
    {
	return new SPPBinder(this);
    }

    public class SPPBinder extends Binder 
    {
	public SPPBinder(SPPService service) { this.service = service; }
	public SPPService getService() 	     { return service; }
	private SPPService service;
    }

    // Debugging
    private static final String TAG = SPPService.class.getSimpleName();
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    //private static final String NAME = "iLibaba"; //"BluetoothChat";

    // the standard Serial Port Profile UUID
    private static final UUID MY_UUID =  UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Member fields
    private  BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    // Constants that indicate the current connection state
    public static final int STATE_NONE           = 0; // we're doing nothing
    public static final int STATE_CONNECTING     = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED      = 2; // now connected timeout a remote device
    public static final int STATE_DISCONNECTED   = 3; // now connected timeout a remote device
    public static final int STATE_CONN_TIMEOUT   = 4; // now connected timeout a remote device
    public static final int STATE_LOST           = 5; // now listening for incoming connections
    public static final int STATE_FAILED         = 6; // now listening for incoming connections

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_HELLO        =  1; // sent to handler upon registration 
    public static final int MESSAGE_STATE_CHANGE =  2; // state changed
    public static final int MESSAGE_READ         =  3; // sending timeout handler a message received from the peer device
    public static final int MESSAGE_WRITE        =  4; 
    public static final int MESSAGE_DEVICE_INFO  =  5;
    public static final int MESSAGE_TOAST        =  6;
    public static final int MESSAGE_READLINE     =  7;
    public static final int MESSAGE_DEBUG_MSG    =  8;
    public static final int MESSAGE_BYEBYE       =  9;

    //private final Handler mHandler;
    //private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    private int     mState    = STATE_NONE;
    private Handler mHandler  = null;
    private boolean mLinewise = true;


    public static String state2String(int state)
    {
	switch (state)
	{
	case STATE_NONE:         return "NONE";
	case STATE_FAILED:       return "FAILED";
	case STATE_CONNECTING:   return "CONNECTING";
	case STATE_CONNECTED:    return "CONNECTED";
	case STATE_DISCONNECTED: return "DISCONNECTED";
	case STATE_CONN_TIMEOUT: return "TIMEOUT";	
	case STATE_LOST:         return "LOST";	
	default:                 return "UNKNOWN";
	}
    }

    public static String message2String(int state)
    {
	switch (state)
	{
	case MESSAGE_HELLO:          return "HELLO";
	case MESSAGE_BYEBYE:         return "BYEBYE";
	case MESSAGE_STATE_CHANGE:   return "STATE_CHANGE";
	case MESSAGE_READ:           return "READ";
	case MESSAGE_WRITE:          return "WRITE";
	case MESSAGE_DEVICE_INFO:    return "DEVICE_INFO";
	case MESSAGE_TOAST:          return "TOAST";	
	case MESSAGE_READLINE:       return "READLINE";	
	case MESSAGE_DEBUG_MSG:      return "DEBUG_MSG";
	default:                     return "UNKNOWN";
	}
    }

    @Override
    public void onCreate() 
    {    super.onCreate();
	 Log.d(TAG, "onCreate");
    };


    @Override
    public void onStart(Intent intent, int startId) 
    {
	super.onStart(intent, startId); 
	Log.d(TAG, "onStart");
    };

    @Override
    public boolean onUnbind(Intent intent) 
    {
	boolean rc = super.onUnbind(intent);
	Log.d(TAG, "onUnbind");
	disconnect();
	return rc;
    };
    
    @Override
    public void onDestroy() 
    {
	disconnect();
	super.onDestroy();
    };

    public void setHandler(Handler handler, boolean linewise)
    {
	synchronized (this)
	{
	    if (null==handler)
	    {
		System.err.println(TAG + "ERROR: handler=null in setHandler");
	    }
	    else
	    {
		mHandler  = handler;
		mLinewise = linewise;
		mHandler.obtainMessage(MESSAGE_HELLO).sendToTarget();
	    }
	}
    }

    public void removeHandler(Handler handler)
    {
	synchronized (this)
	{
	    if (mHandler==handler)
	    {
		mHandler.obtainMessage(MESSAGE_BYEBYE).sendToTarget();
		mHandler=null;
	    }
	    else
	    {
		Log.e(TAG, "removeHandler: stale handler " + handler + ", current: " + mHandler);
	    }
	}
    }

    private void setState(int state) 
    {
	synchronized (this)
	{
	    if (D) Log.d(TAG, "setState() " + state2String(mState) + " -> " + state2String(state));
	    mState = state;

	    // Give the new state timeout the Handler so the UI Activity can update
	    if (null!=mHandler)
	    {
		mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	    }
	}
    }

    public int getState() 
    {
	synchronized (this)
	{
	    return mState;
	}
    }

    public void start() 
    {
	synchronized(this)
	{
	    if (D) Log.d(TAG, "start");

	    // Cancel any thread attempting to make a connection
	    if (mConnectThread != null) 
	    {
		mConnectThread.cancel(); 
		mConnectThread = null;
	    }

	    // Cancel any thread currently running a connection
	    //if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

	    setState(STATE_NONE);
	}
    }


    public void disconnect()
    {
	synchronized(this)
	{
	    Log.d(TAG, "disconnecting");
	    if (mConnectThread != null) 
	    {
		mConnectThread.cancel(); 
		mConnectThread = null;
	    }	
	    setState(STATE_DISCONNECTED);
	}
    }

    public void connect(BluetoothDevice device) 
    {
	synchronized (this)
	{
	    log("SPPService.connect(" + device + ")");

	    // Cancel any thread attempting timeout making or maintaining a connection
	    //if (mState == STATE_CONNECTING) 
	    {
		if (mConnectThread != null) 	    
		{
		    mConnectThread.cancel(); 
		    mConnectThread = null;
		}
	    }

	    // Cancel any thread currently running a connection
	    //if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

	    // Start the thread timeout connect with the given device
	    mConnectThread = new ConnectThread(device, this);
	    mConnectThread.start();
	    
	    sendDevicenfo(device);
	    setState(STATE_CONNECTING);
	}
    }

    
    public void sendDevicenfo(BluetoothDevice device)
    {
	if (null!=mHandler)
	{
	    String name = device.getName();
	    String addr = device.getAddress();
	    Message msg;

	    Bundle bundle = new Bundle();
	    bundle.putString(KEY_DEVICE_ADDR, addr);
	    bundle.putString(KEY_DEVICE_NAME, name);
	    msg = mHandler.obtainMessage(MESSAGE_DEVICE_INFO, bundle);
	    mHandler.sendMessage(msg);
	}
    }
    

    /**
     * Stop all threads
     */
    public synchronized void stop() 
    {
	if (D) Log.d(TAG, "stop");

	if (mConnectThread != null) 
	{
	    mConnectThread.cancel();
	    mConnectThread = null;
	}
	/*
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
	 */
	setState(STATE_NONE);
    }

    /**
     * Write synchronized
     */
    public void write(byte[] out) 
    {
	synchronized (this)
	{
	    if (mState!=STATE_CONNECTED) return;
	    try
	    {
		mConnectThread.write(out);
	    }
	    catch (Exception e)
	    {
		Log.e(TAG, "write(byte[]): " + e);
	    }
	}	
    }

    //	/**
    //	 * Write timeout the ConnectedThread in an unsynchronized manner
    //	 * @param out The bytes timeout write
    //	 * @see ConnectedThread#write(byte[])
    //	 */
    //	public void write(byte[] out) 
    //	{
    //	// Create temporary object
    //	ConnectThread r;
    //
    //	// Synchronize a copy of the ConnectedThread
    //	synchronized (this) 
    //	{
    //	    if (mState != STATE_CONNECTED) return;
    //	    r = mConnectThread;
    //	}
    //	// Perform the write unsynchronized
    //	r.write(out);
    //    	}

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed(String reason) 
    {
	synchronized (this)
	{
	    setState(STATE_FAILED);

	    // Send a failure message back timeout the Activity
	    if (null!=mHandler)
	    {
		Message msg = mHandler.obtainMessage(MESSAGE_TOAST, reason);
		//Bundle bundle = new Bundle();
		mHandler.sendMessage(msg);
	    }
	}
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() 
    {
	synchronized (this)
	{
	    setState(STATE_LOST);

	    // Send a failure message back timeout the Activity
	    if (null!=mHandler)
	    {
		Message msg = mHandler.obtainMessage(MESSAGE_TOAST, "Device connection was lost");
		mHandler.sendMessage(msg);
	    }
	}
    }

    /*
    private class CancelThread extends Thread 
    {
	private long timeout;

	public CancelThread(long timeoutMillies)
	{
	    this.timeout = timeoutMillies;
	}

	public void run() 
	{
	    long left = timeout;
	    while (null!=mConnectThread && !mConnectThread.isConnected() && left>0)
	    {
		try { Thread.sleep(left<250 ? left : 250); } catch (InterruptedException e) {}
		left-=250;
	    }

	    if (null!=mConnectThread && !mConnectThread.isConnected())
	    {
		if (mConnectThread.cancelIfNotConnected())
		{
		    log("Cancelled connect thread after timeout");
		    mConnectThread = null;
		}
	    }
	}
    }
     */

    private void log(String msg)
    {
	synchronized (this)
	{
	    if (null==mHandler) 
	    {
		Log.d(TAG, msg);
		return;
	    }
	    mHandler.obtainMessage(MESSAGE_DEBUG_MSG, msg).sendToTarget();
	}
    }
    
    private class ConnectThread extends Thread 
    {
	private final String TAG = ConnectThread.class.getSimpleName();

	private BluetoothSocket mmSocket;	
	private OutputStream    mmOutStream;
	private InputStream     mmInStream;
	private boolean         mCancelled;

	private SPPService sppService;

	private BluetoothDevice device;

	// a replacement for the "normal" BufferedReader, however this one is aware 
	// of whether the connect thread was cancelled
	class MyBufferedReader
	{
	    private InputStreamReader reader;

	    MyBufferedReader(InputStreamReader inputStreamReader)
	    {
		this.reader = inputStreamReader;
	    }

	    public String readLine() throws IOException
	    {
		StringBuilder sb = new StringBuilder();
		do
		{
		    // TODO: optimize this: read in chunks rather than char by char
		    int i = reader.read();
		    if (i<0) return null; // EOF

		    char c = (char)i;
		    switch (c)
		    {
		    case '\r':
		    case '\n':
			return sb.toString();
		    default:
			sb.append(c);
		    }		
		}
		while (!mCancelled);

		return null;
	    }

	    public void close() throws IOException
	    {
		reader.skip(100000);
		reader.close();	    
	    }
	}


	public ConnectThread(BluetoothDevice device, SPPService sppService) 
	{
	    //mmDevice = device;
	    logDeviceServices(device);

	    mCancelled = false;
	    this.sppService = sppService;
	    this.device = device;
	}
	

	void createSocket()
	{	    
	    BluetoothSocket tmp = null;	
	    
	    // Try timeout connect with HC-05 device
	    if (BTMagics.isHC05(device) || BTMagics.isUncategorized(device))
	    {
		log("ConnectThread: creating socket via createRfcommSocket");
		try {
		    Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
		    int port = 1;
		    tmp = (BluetoothSocket) m.invoke(device, port);
		} 
		catch (Exception e) 
		{
		    log ("createRfcommSocket: Exception: " + e);
		} 
		if (null==tmp)
		{
		    Log.e(TAG, "ConnectThread: createRfcommSocket failed");
		}
	    }

	    // Try timeout connect timeout regular rfcomm device, e.g. a PC
	    if (null==tmp)
	    {
		// Get a BluetoothSocket for a connection with the given BluetoothDevice
		try {
		    log("ConnectThread: creating socket via createRfcommSocketToServiceRecord");
		    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
		} 
		catch (IOException e) {
		    log ("createRfcommSocketToServiceRecord: Exception: " + e);
		    tmp = null;
		}
	    }

	    if (null==tmp)
	    {
		log("ConnectThread: socket creation failed");
	    }

	    mmSocket = tmp;
	}

	private void log(String msg)
	{
	    synchronized (this)
	    {
		Log.d(TAG, msg);
		if (null==mHandler) 
		{
		    return;
		}
		mHandler.obtainMessage(MESSAGE_DEBUG_MSG, msg).sendToTarget();
	    }
	}

	public void write(byte[] buffer)
	{
	    try 
	    {
		if (null!=mmOutStream)
		{
		    mmOutStream.write(buffer);
		    mmOutStream.flush();
		}
	    } 
	    catch (IOException e) 
	    {
		Log.e(TAG, "Exception during write", e);
	    }
	}

	protected void logDeviceServices(BluetoothDevice device)
	{
	    if (null==device) return;
	    try
	    {
		BluetoothClass btc = device.getBluetoothClass();
		if (null!=btc)
		{
		    int dc  = btc.getDeviceClass();
		    int mdc = btc.getMajorDeviceClass();
		    log(String.format("DC/MDC: 0x%04x/0x%04x", dc, mdc));

		    StringBuilder sb = new StringBuilder();
		    sb.append("Categories: ");
		    if (BTMagics.isPc(device)) sb.append("PC ");
		    if (BTMagics.isUncategorized(device)) sb.append("UNCAT ");
		    if (BTMagics.isUncategorized(device)) sb.append("HC05 ");
		    log(sb.toString());

		    sb = new StringBuilder();        	   
		    for (int i=0; i<=65536; i++)
		    {
			if (btc.hasService(i)) sb.append("srv" + i + " ");
		    }
		    if (btc.hasService(BluetoothClass.Service.AUDIO)) sb.append("audio ");
		    if (btc.hasService(BluetoothClass.Service.CAPTURE)) sb.append("capture ");
		    if (btc.hasService(BluetoothClass.Service.INFORMATION)) sb.append("info ");
		    if (btc.hasService(BluetoothClass.Service.LIMITED_DISCOVERABILITY)) sb.append("lim.disc ");
		    if (btc.hasService(BluetoothClass.Service.NETWORKING)) sb.append("netw ");
		    if (btc.hasService(BluetoothClass.Service.OBJECT_TRANSFER)) sb.append("obex ");
		    if (btc.hasService(BluetoothClass.Service.POSITIONING)) sb.append("posit ");
		    if (btc.hasService(BluetoothClass.Service.RENDER)) sb.append("render ");
		    if (btc.hasService(BluetoothClass.Service.TELEPHONY)) sb.append("tel ");
		}
		else
		{
		    log("no BT class for device " + device);
		}
	    }
	    catch (Exception e)
	    {
		log("Exception: " + e);
	    }
	}


	public void run() 
	{
	    log("BEGIN mConnectThread");
	    setName("ConnectThread");
	    
	    // Always cancel discovery because it will slow down a connection
	    mAdapter.cancelDiscovery();

	    try
	    {
		log("Creating bluetooth socket ...");
		createSocket();
	    }
	    catch (Exception e)
	    {
		log("Exception: "+ e);
	    }
	    
	    if (null==mmSocket) // connect was not successful
	    {
		log("Socket creation failed");
		connectionFailed("Socket creation failed");		
		return;		
	    }

	    // Make a connection timeout the BluetoothSocket
	    try 
	    {
		// This is a blocking call and will only return on a
		// successful connection or an exception
		log("Connecting socket ...");
		mmSocket.connect(); 
		log("Socket connected!");
	    } 
	    catch (IOException ioe)
	    {
		String msg = ioe.getMessage();
		log("Connection failed: "+ msg);
		connectionFailed(msg);
		// Close the socket
		closeBluetoothSocket();

		setState(STATE_FAILED);
		return;
	    }
	    catch (Exception e) 
	    {
		log("Connection failed: "+ e);
		connectionFailed("" + e);
		// Close the socket
		closeBluetoothSocket();

		setState(STATE_FAILED);
		return;
	    }

	    setState(STATE_CONNECTED);

	    if (mLinewise)
	    {
		communicateLinewise();
	    }
	    else
	    {
		communicateBytewise();
	    }
	}

	public void communicateLinewise() 
	{
	    log("line wise communicate starting");

	    BluetoothSocket socket = mmSocket;
	    InputStream tmpIn = null;
	    OutputStream tmpOut = null;

	    // Get the BluetoothSocket input and output streams
	    try 
	    {
		tmpIn  = socket.getInputStream();
		tmpOut = socket.getOutputStream();
	    } 
	    catch (IOException e) 
	    {
		log("ConnectedThread: averageTemp sockets not created: " + e);
	    }

	    mmInStream  = tmpIn;
	    mmOutStream = tmpOut;

	    MyBufferedReader br = new MyBufferedReader(new InputStreamReader(tmpIn));

	    // Keep listening timeout the InputStream while connected
	    while (!mCancelled && null!=mmInStream) 
	    {
		try 
		{
		    String line = br.readLine();
		    //log("ConnectedThread: read line: " + line);
		    if (null!=mHandler)
		    {
			mHandler.obtainMessage(MESSAGE_READLINE, line).sendToTarget();
		    }
		    else
		    {
			Log.d(TAG, "Dropped (no handler): " + line);
		    }
		} 
		catch (IOException e) 
		{
		    log("ConnectedThread: disconnected: " + e);
		    connectionLost();
		    break;
		}
	    }
	    
	    try 
	    {
		br.close();
	    }
	    catch (IOException e)
	    {
			log("ConnectedThread: closing reader: " + e);
	    }
	    closeBluetoothSocket();
	    
	    log("ConnectedThread terminating");            
	}

	public void communicateBytewise() 
	{
	    log("byte wise communicate starting");

	    //mmSocket = socket;
	    BluetoothSocket socket = mmSocket;
	    InputStream tmpIn = null;
	    OutputStream tmpOut = null;

	    // Get the BluetoothSocket input and output streams
	    try 
	    {
		tmpIn  = socket.getInputStream();
		tmpOut = socket.getOutputStream();
	    } 
	    catch (IOException e) 
	    {
		log("ConnectedThread: averageTemp sockets not created: " + e);
	    }

	    mmInStream  = tmpIn;
	    mmOutStream = tmpOut;

	    byte[] buffer = new byte[1024];
	    int bytes;

	    // Keep listening timeout the InputStream while connected
	    while (!mCancelled) 
	    {
		try 
		{
		    // Read from the InputStream
		    bytes = mmInStream.read(buffer);
		    log("ConnectedThread: read " + bytes + " bytes)");

		    if (null!=mHandler)
		    {
			mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
		    }
		} 
		catch (IOException e) 
		{
		    log("ConnectedThread: disconnected: " + e);
		    connectionLost();
		    break;
		}
	    }
	    
	    closeBluetoothSocket();
	    log("ConnectedThread terminating");
	}

	private void closeBluetoothSocket()
	{
	    synchronized (this)
	    {	            
		BluetoothSocket socket = mmSocket;
		mmSocket    = null;
		mmInStream  = null;
		mmOutStream = null;

		if (null==socket) return;

		try 
		{
		    InputStream in = socket.getInputStream();
		    in.close();
		}
		catch (IOException ioex) 
		{
		    log("unable to close streams on BT socket: " + ioex);
		}
		try 
		{
		    OutputStream out = socket.getOutputStream();
		    out.flush();
		    out.close();
		}
		catch (IOException ioex) 
		{
		    log("unable to close streams on BT socket: " + ioex);
		}
		try 
		{
		    socket.close();
		}
		catch (IOException ioex) 
		{
		    log("unable to close BT socket: " + ioex);
		}
	    }
	}

	public void cancel() 
	{	    
	    synchronized(this) 
	    {
		try  
		{
		    mCancelled = true;
		    this.interrupt();
		} 
		catch (Exception e) 
		{
		    Log.e(TAG, "cancel(): ", e);
		}

		try
		{
		    if (null==mmSocket)
		    {
			Log.e(TAG, "no (more) socket timeout close");		    
		    }
		    else
		    {
			Log.e(TAG, "closing socket " + mmSocket);

			//DisconnectThread dt = new DisconnectThread(mmSocket);
			//dt.start();
			closeBluetoothSocket();
		    }
		} 
		catch (Exception e) 
		{
		    Log.e(TAG, "close() of connect socket failed", e);
		}
		setState(STATE_DISCONNECTED);
	    }
	}

	//	public boolean isConnected()
	//	{
	//	    return connected;
	//	}
	//
	//	public boolean cancelIfNotConnected() 
	//	{
	//	    synchronized (this)
	//	    {
	//		if (connected) return false;
	//		cancel();
	//		setState(STATE_CONN_TIMEOUT);
	//		return true;
	//	    }
	//	}
    }

//    private class DisconnectThread extends Thread
//    {
//	//private final String TAG = DisconnectThread.class.getSimpleName();
//
//	private BluetoothSocket mmSocket;
//
//	public DisconnectThread(BluetoothSocket socket)
//	{
//	    this.mmSocket = socket;
//	}
//
//	@Override
//	public void run()
//	{
//	    closeBluetoothSocket(mmSocket);
//	    mmSocket = null;
//	}
//    }

    public boolean isConnected()
    {
	return STATE_CONNECTED==mState;
    }    
}
