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

package com.apdlv.ilibaba.water;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import com.apdlv.ilibaba.BluetoothHC05;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothConnector 
{
    static final String TOAST = "TOAST";
    public static final String DEVICE_NAME = "device_name";

    public interface OnBluetoothListener
    {
	void onBTStateChanged(int state, String msg);
	void onBTDataReceived(byte data[], int len);
    }
    
    // Debugging
    private static final String TAG = "BluetoothSerialService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "iLibaba"; //"BluetoothChat";

    // Unique UUID for this application
    private static final UUID MY_UUID =
	    // my own generated uuid::
	    // UUID.fromString("63178624-BD83-4267-A6D1-D3F25D9948CF");
	    // the standard Serial Port Profile UUID
	    UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    	    // from original example (not sure if that matters)
	    //UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    //private final Handler mHandler;
    //private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    //private ConnectedThread mConnectedThread;
    private int mState;
    private boolean mDoListen;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_DISCONNECTED = 4;  // now connected to a remote device
    public static final int STATE_TIMEOUT = 5;  // now connected to a remote device

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_DEBUG_MSG = 6;


    Handler mHandler = null;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothConnector(Context context, Handler handler) 
    {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        //mHandler = handler;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    
    
    
    private synchronized void log(String msg)
    {
	mHandler.obtainMessage(MESSAGE_DEBUG_MSG, msg).sendToTarget();
	Log.d(TAG, msg);
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
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        //if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_NONE);
    }
    
    
    public synchronized void disconnect()
    {
        Log.d(TAG, "disconnecting");
        if (mConnectThread != null) {
            mConnectThread.write("EOF\r\n".getBytes());
            mConnectThread.cancel(); mConnectThread = null;
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
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        //if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        
        //CancelThread cancelThread = new CancelThread(5000);
        //cancelThread.start();
        
        setState(STATE_CONNECTING);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
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
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
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

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread 
    {
	private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
	private boolean connected;
	private OutputStream mmOutStream;
	private InputStream mmInStream;

        public ConnectThread(BluetoothDevice device) 
        {
            mmDevice = device;
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
              }

              // Share the sent message back to the UI Activity
              mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
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
                mmSocket.connect();
                this.connected = true;
            } 
            catch (Exception e) 
            {
        	log("ConnectThread::connect: " + e);
        	this.connected = false;
                connectionFailed();
                // Close the socket
                try 
                {
                    mmSocket.close();
                } 
                catch (IOException ioex) {
                    log("unable to close() socket during connection failure: " + ioex);
                }
                setState(STATE_DISCONNECTED);
                // Start the service over to restart listening mode
                //BluetoothConnector.this.start();
                return;
            }

            // Start the connected thread
            Message msg = mHandler.obtainMessage(BluetoothHC05.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(BluetoothHC05.DEVICE_NAME, mmDevice.getName());
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            setState(STATE_CONNECTED);
            communicate();
        }
        
        public void communicate() {

            log("communicate starting");
            
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

                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    log("ConnectedThread: disconnected: " + e);
                    connectionLost();
                    break;
                }
            }
            log("ConnectedThread terminating");
        }
        
        
        
        public void cancel() 
        {
            setState(STATE_DISCONNECTED);
            try 
            {
                mmSocket.close();
            } 
            catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

        public boolean isConnected()
        {
            return connected;
        }

        public boolean cancelIfNotConnected() 
        {
            if (connected) return false;
            cancel();
            setState(STATE_TIMEOUT);
            return true;
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
//                    mHandler.obtainMessage(BluetoothHC05.MESSAGE_READ, bytes, -1, buffer)
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
//                mHandler.obtainMessage(BluetoothHC05.MESSAGE_WRITE, -1, -1, buffer)
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
