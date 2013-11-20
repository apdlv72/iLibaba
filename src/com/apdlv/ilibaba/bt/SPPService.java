/*
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

package com.apdlv.ilibaba.bt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class SPPService  extends Service
{

    public SPPService()
    {
	Log.d(TAG, "instantiated");
    }

    @Override
    public IBinder onBind(Intent intent) 
    {
	return new BTSerialBinder(this);
    }

    public class BTSerialBinder extends Binder 
    {
	public BTSerialBinder(SPPService btc)
	{
	    this.btc = btc;
	}

	public SPPService getService() 
	{
	    return btc;
	}

	private SPPService btc;
    }



    public interface OnBluetoothListener
    {
	void onBTStateChanged(int state, String msg);
	void onBTDataReceived(byte data[], int len);
    }

    // Debugging
    private static final String TAG = SPPService.class.getSimpleName();
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    //private static final String NAME = "iLibaba"; //"BluetoothChat";

    // Unique UUID for this application
    private static final UUID MY_UUID =
	    // the standard Serial Port Profile UUID
	    UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    // from original example (not sure if that matters)
    //UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields
    private  BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    //private final Handler mHandler;
    //private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    //private ConnectedThread mConnectedThread;
    private int mState = STATE_NONE;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_FAILED = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_DISCONNECTED = 4;  // now connected to a remote device
    public static final int STATE_CONN_TIMEOUT = 5;  // now connected to a remote device
    public static final int STATE_LOST = 6;     // now listening for incoming connections


    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_HELLO        = -1;
    public static final int MESSAGE_STATE_CHANGE =  1;
    public static final int MESSAGE_READ         =  2; // sending to handler a message received from the peer device
    public static final int MESSAGE_WRITE        =  3;
    public static final int MESSAGE_DEVICE_ADDR  =  4;
    public static final int MESSAGE_DEVICE_NAME  =  5;
    public static final int MESSAGE_TOAST        =  6;
    public static final int MESSAGE_READLINE     =  7;
    public static final int MESSAGE_DEBUG_MSG    =  8;

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
	case MESSAGE_STATE_CHANGE:   return "STATE_CHANGE";
	case MESSAGE_READ:           return "READ";
	case MESSAGE_WRITE:          return "WRITE";
	case MESSAGE_DEVICE_ADDR:    return "DEVICE_ADDR";
	case MESSAGE_DEVICE_NAME:    return "DEVICE_NAME";
	case MESSAGE_TOAST:          return "TOAST";	
	case MESSAGE_READLINE:       return "READLINE";	
	case MESSAGE_DEBUG_MSG:      return "DEBUG_MSG";
	default:                     return "UNKNOWN";
	}
    }

    private Handler mHandler = null;
    private boolean mLinewise;

    @Override
    public void onCreate() 
    {
	super.onCreate(); Log.d(TAG, "onCreate");
    };


    @Override
    public void onStart(Intent intent, int startId) 
    {
	super.onStart(intent, startId); Log.d(TAG, "onStart");
    };


    public void setHandler(Handler handler, boolean linewise)
    {
	if (null==handler)
	{
	    System.err.println(TAG + "ERROR: handler=null in setHandler");
	}
	else
	{
	    mHandler = handler;
	    mLinewise = linewise;
	    mHandler.obtainMessage(MESSAGE_HELLO).sendToTarget();
	}
    }

    private synchronized void setState(int state) 
    {
	if (D) Log.d(TAG, "setState() " + state2String(mState) + " -> " + state2String(state));
	mState = state;
	
	// Give the new state to the Handler so the UI Activity can update
	if (null!=mHandler)
	{
	    mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}
    }

    private synchronized void log(String msg)
    {
	if (null==mHandler) 
	{
	    Log.d(TAG, msg);
	    return;
	}
	mHandler.obtainMessage(MESSAGE_DEBUG_MSG, msg).sendToTarget();
	//Log.d(TAG, msg);
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
	return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
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


    public synchronized void disconnect()
    {
	Log.d(TAG, "disconnecting");
	if (mConnectThread != null) 
	{
	    mConnectThread.cancel(); 
	    mConnectThread = null;
	}	
	setState(STATE_DISCONNECTED);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
	log("connect to: " + device);

	// Cancel any thread attempting to make a connection
	if (mState == STATE_CONNECTING) 
	{
	    if (mConnectThread != null) 	    
	    {
		try
		{
		    mConnectThread.interrupt();
		}
		catch (Exception e)
		{
		    Log.e(TAG, "connect: " + e);
		}
		mConnectThread.cancel(); 
		mConnectThread = null;
		
	    }
	}

	// Cancel any thread currently running a connection
	//if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

	// Start the thread to connect with the given device
	mConnectThread = new ConnectThread(device);
	mConnectThread.start();

	//CancelThread cancelThread = new CancelThread(5000);
	//cancelThread.start();

	if (null!=mHandler)
	{
	    String name = device.getName();
	    String addr = device.getAddress();
	    Message msg;

	    msg = mHandler.obtainMessage(MESSAGE_DEVICE_ADDR, addr);
	    mHandler.sendMessage(msg);

	    msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME, name);
	    mHandler.sendMessage(msg);		
	}
	setState(STATE_CONNECTING);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
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
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
	// Create temporary object
	ConnectThread r;
	// Synchronize a copy of the ConnectedThread
	synchronized (this) {
	    if (mState != STATE_CONNECTED) return;
	    r = mConnectThread;
	}
	// Perform the write unsynchronized
	r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() 
    {
	setState(STATE_FAILED);

	// Send a failure message back to the Activity
	if (null!=mHandler)
	{
	    Message msg = mHandler.obtainMessage(MESSAGE_TOAST, "Unable to connect device");
	    //Bundle bundle = new Bundle();
	    mHandler.sendMessage(msg);
	}
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() 
    {
	setState(STATE_LOST);

	// Send a failure message back to the Activity
	if (null!=mHandler)
	{
	    Message msg = mHandler.obtainMessage(MESSAGE_TOAST, "Device connection was lost");
	    mHandler.sendMessage(msg);
	}
    }

    @SuppressWarnings("unused")
    private class CancelThread extends Thread {
	private long to;

	public CancelThread(long timeoutMillies)
	{
	    this.to = timeoutMillies;
	}

	public void run() 
	{
	    long left = to;
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

    
    private class DisconnectThread extends Thread
    {
	private BluetoothSocket mmSocket;

	public DisconnectThread(BluetoothSocket socket)
	{
	    this.mmSocket = socket;
	}
	
	@Override
	public void run()
	{
	    try
            {
	        this.mmSocket.close();
            } 
	    catch (IOException e)
            {
		Log.e("DisconnectThread", "mmSocket.close(): " + e);

            }
	}
    }

    
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread 
    {
	private BluetoothSocket mmSocket;
	//private final BluetoothDevice mmDevice;
	private boolean connected;
	private OutputStream mmOutStream;
	private InputStream mmInStream;

	public ConnectThread(BluetoothDevice device) 
	{
	    //mmDevice = device;
	    BluetoothSocket tmp = null;

	    logDeviceServices(device);

	    // Try to connect with HC-05 device
	    if (isHC05(device) || isUncategorized(device))
	    {
		log("ConnectThread: creating socket via createRfcommSocket");
		try {
		    Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
		    int port = 1;
		    tmp = (BluetoothSocket) m.invoke(device, port);
		} 
		catch (Exception e) {
		    log ("createRfcommSocket: Exception: " + e);
		} 
		if (null==tmp)
		{
		    Log.e(TAG, "ConnectThread: createRfcommSocket failed");
		}
	    }

	    // Try to connect to regular rfcomm device, e.g. a PC
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


	public void write(byte[] buffer)
	{
	    try 
	    {
		if (null!=mmOutStream)
		{
		    mmOutStream.write(buffer);
		    mmOutStream.flush();
		}

		// Share the sent message back to the UI Activity
		// Don'temp... why is this useful?
//		if (null!=mHandler)
//		{
//		    mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
//		}
	    } 
	    catch (IOException e) 
	    {
		Log.e(TAG, "Exception during write", e);
	    }
	}


	private boolean isPc(BluetoothDevice device)
	{
	    BluetoothClass btc = device.getBluetoothClass();
	    int mdc = btc.getMajorDeviceClass();
	    return (mdc == BluetoothClass.Device.Major.COMPUTER);
	}

	private boolean isUncategorized(BluetoothDevice device)
	{
	    BluetoothClass btc = device.getBluetoothClass();
	    int mdc = btc.getMajorDeviceClass();
	    return (mdc == BluetoothClass.Device.Major.UNCATEGORIZED);
	}

	private boolean isHC05(BluetoothDevice device)
	{
	    String addr = device.getAddress();
	    return addr.startsWith("20:13:01");
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
		    if (isPc(device)) sb.append("PC ");
		    if (isUncategorized(device)) sb.append("UNCAT ");
		    if (isUncategorized(device)) sb.append("HC05 ");
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


	public void run() {
	    log("BEGIN mConnectThread");
	    setName("ConnectThread");

	    // Always cancel discovery because it will slow down a connection
	    mAdapter.cancelDiscovery();

	    // Make a connection to the BluetoothSocket
	    try {
		// This is a blocking call and will only return on a
		// successful connection or an exception
		this.connected = false;  
		log("Connecting ...");
		mmSocket.connect(); 
		this.connected = true; 
		log("Connected!");
	    } 
	    catch (Exception e) 
	    {
		log("Connection failed!");
		log("ConnectThread::connect: " + e);
		this.connected = false;
		connectionFailed();
		// Close the socket
		try 
		{
		    if (null!=mmSocket) mmSocket.close();
		} 
		catch (IOException ioex) {
		    log("unable to close() socket during connection failure: " + ioex);
		}
		setState(STATE_DISCONNECTED);
		// Start the service over to restart listening mode
		//SPPService.this.start();
		return;
	    }

	    // no need to announce device here. already done in connect()
//	    if (null!=mHandler)
//	    {
//		String name = mmDevice.getName();
//		Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME, name);
//		mHandler.sendMessage(msg);
//	    }
	    setState(STATE_CONNECTED);

	    if (mLinewise)
		communicateLinewise();
	    else
		communicateBytewise();
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
		log("ConnectedThread: temp sockets not created: " + e);
	    }

	    mmInStream = tmpIn;
	    mmOutStream = tmpOut;

	    // do not send anytghing. derived calsses may send their own hellos in the
	    // onDeviceConnected() method
/*	    
	    //String helloCmd = "HELLO\r\n";
	    String helloCmd = "D\n";
	    
	    try 
	    {
		mmOutStream.write(helloCmd.getBytes());
		mmOutStream.write(helloCmd.getBytes());
		mmOutStream.flush();
	    }
	    catch (IOException e) 
	    {
	    }
*/

	    BufferedReader br = new BufferedReader(new InputStreamReader(tmpIn));

	    // Keep listening to the InputStream while connected
	    while (true) 
	    {
		try {


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
	    try {
		tmpIn  = socket.getInputStream();
		tmpOut = socket.getOutputStream();
	    } catch (IOException e) {
		log("ConnectedThread: temp sockets not created: " + e);
	    }

	    mmInStream = tmpIn;
	    mmOutStream = tmpOut;

	    byte[] buffer = new byte[1024];
	    int bytes;

	    // Keep listening to the InputStream while connected
	    while (true) 
	    {
		try {
		    // Read from the InputStream
		    bytes = mmInStream.read(buffer);
		    log("ConnectedThread: read " + bytes + " bytes)");

		    if (null!=mHandler)
		    {
			mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
		    }
		} catch (IOException e) {
		    log("ConnectedThread: disconnected: " + e);
		    connectionLost();
		    break;
		}
	    }
	    log("ConnectedThread terminating");
	}



	public synchronized void cancel() 
	{
	    try 
	    {
		if (null==mmSocket)
		{
		    Log.e(TAG, "no (more) socket to close");		    
		}
		else
		{
		    Log.e(TAG, "closing socket " + mmSocket);

		    DisconnectThread dt = new DisconnectThread(mmSocket);
		    mmSocket = null;
		    dt.start();

		    Log.e(TAG, "socket closed (in background)");
		}
	    } 
	    catch (Exception e) 
	    {
		Log.e(TAG, "close() of connect socket failed", e);
	    }
	    setState(STATE_DISCONNECTED);
	}

	public boolean isConnected()
	{
	    return connected;
	}

	public boolean cancelIfNotConnected() 
	{
	    if (connected) return false;
	    cancel();
	    setState(STATE_CONN_TIMEOUT);
	    return true;
	}
    }

    public void removeHandler(Handler handler)
    {
	if (mHandler==handler)
	{
	    mHandler=null;
	}
	else
	{
	    Log.e(TAG, "removeHandler invoked with stale handler " + handler);
	}
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    //    
    //    private class ConnectedThread extends Thread {
    //        private final BluetoothSocket mmSocket;
    //        private final InputStream mmInStream;
    //        private final OutputStream mmOutStream;
    //
    //        public ConnectedThread(BluetoothSocket socket) {
    //            log("ConnectedThread created");
    //            mmSocket = socket;
    //            InputStream tmpIn = null;
    //            OutputStream tmpOut = null;
    //
    //            // Get the BluetoothSocket input and output streams
    //            try {
    //                tmpIn  = socket.getInputStream();
    //                tmpOut = socket.getOutputStream();
    //            } catch (IOException e) {
    //                log("ConnectedThread: temp sockets not created: " + e);
    //            }
    //
    //            mmInStream = tmpIn;
    //            mmOutStream = tmpOut;
    //        }
    //
    //        public void run() {
    //            log("ConnectedThread starting");
    //            byte[] buffer = new byte[1024];
    //            int bytes;
    //
    //            // Keep listening to the InputStream while connected
    //            while (true) {
    //                try {
    //                    // Read from the InputStream
    //                    bytes = mmInStream.read(buffer);
    //                    log("ConnectedThread: read " + bytes + " bytes)");
    //
    //                    // Send the obtained bytes to the UI Activity
    //                    mHandler.obtainMessage(GateControlActivity.MESSAGE_READ, bytes, -1, buffer)
    //                            .sendToTarget();
    //                } catch (IOException e) {
    //                    log("ConnectedThread: disconnected: " + e);
    //                    connectionLost();
    //                    break;
    //                }
    //            }
    //            log("ConnectedThread terminating");
    //        }
    //
    //        /**
    //         * Write to the connected OutStream.
    //         * @param buffer  The bytes to write
    //         */
    //        public void write(byte[] buffer) {
    //            try {
    //                mmOutStream.write(buffer);
    //
    //                // Share the sent message back to the UI Activity
    //                mHandler.obtainMessage(GateControlActivity.MESSAGE_WRITE, -1, -1, buffer)
    //                        .sendToTarget();
    //            } catch (IOException e) {
    //                Log.e(TAG, "Exception during write", e);
    //            }
    //        }
    //        
    //
    //        public void cancel() {
    //            try {
    //        	log("ConnectedThread:cancel(). C|losing socket");
    //                mmSocket.close();
    //            } catch (IOException e) {
    //                Log.e(TAG, "close() of connect socket failed", e);
    //            }
    //        }
    //    }
}
