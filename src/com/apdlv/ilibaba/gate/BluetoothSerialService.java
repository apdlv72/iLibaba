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

package com.apdlv.ilibaba.gate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

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

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothSerialService {
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
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private boolean mDoListen;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_DISCONNECTED = 4;  // now connected to a remote device
    public static final int STATE_TIMEOUT = 5;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothSerialService(Context context, Handler handler, boolean doListen) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        mDoListen = doListen;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(GateControlActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    
    
    private synchronized void log(String msg)
    {
	mHandler.obtainMessage(GateControlActivity.MESSAGE_DEBUG_MSG, msg).sendToTarget();
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
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        if (mDoListen)
        {
            // Start the thread to listen on a BluetoothServerSocket
            if (mSecureAcceptThread == null) {
        	mSecureAcceptThread = new AcceptThread();
        	mSecureAcceptThread.start();
            }
            setState(STATE_LISTEN);
        }
        else
        {
            setState(STATE_NONE);
        }
    }
    
    
    public synchronized void disconnect()
    {
        Log.d(TAG, "disconnecting");
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}	
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
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        
        //CancelThread cancelThread = new CancelThread(5000);
        //cancelThread.start();
        
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
	log("connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(GateControlActivity.MESSAGE_DEVICE_NAME, device.getName());
        // no need for a buncdle, set name as message object
//        Bundle bundle = new Bundle();
//        bundle.putString(GateControlActivity.DEVICE_NAME, device.getName());
//        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
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

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
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
        Message msg = mHandler.obtainMessage(GateControlActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GateControlActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(GateControlActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GateControlActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, 
                	MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, 
        	    "BEGIN mSecureAcceptThread" + this);
            setName("AcceptThread");
            
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    if (null!=mmServerSocket)
                    {
                	socket = mmServerSocket.accept();
                	BluetoothDevice device = socket.getRemoteDevice();
                	String addr = null==device ? null : device.getAddress();
                	String name = null==device ? null : device.getName();
                	log("AcceptThread: new incoming connection from " + addr + "(" + name + ")");
                    }
                } 
                catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothSerialService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mSecureAcceptThread");
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
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

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
	private boolean connected;

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
                BluetoothSerialService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothSerialService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
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
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            log("ConnectedThread created");
            mmSocket = socket;
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
        }

        public void run() {
            log("ConnectedThread starting");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    log("ConnectedThread: read " + bytes + " bytes)");

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(GateControlActivity.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    log("ConnectedThread: disconnected: " + e);
                    connectionLost();
                    break;
                }
            }
            log("ConnectedThread terminating");
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(GateControlActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        

        public void cancel() {
            try {
        	log("ConnectedThread:cancel(). C|losing socket");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
