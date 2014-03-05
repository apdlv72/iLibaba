package com.apdlv.ilibaba.bt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

class ConnectThread extends Thread 
{
    // The standard Serial Port Profile UUID
    public static final UUID SPP_UUID =  UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    
    public static boolean DUMP_SERVICES = false;

    public ConnectThread(SPPService sppService, BluetoothDevice device, boolean linewise) 
    {
	if (DUMP_SERVICES)
	{
	    logDeviceServices(device);
	}

	this.mDevice          = device;
	this.mCreatorService  = sppService;
	this.mAttachedService = null;
	this.mLinewise        = linewise;
	this.mDoCancel       = false;
    }


    public void run() 
    {
        log("BEGIN mConnectThread");
        setName("ConnectThread");
        mWasCancelled = false;
    
        // Always cancel discovery because it will slow down a connection
        if (!mCreatorService.cancelDiscovery(this))
        {
            return;
        }
    
        if (mDoCancel)
        {        
            mWasCancelled = true;
            return;
        }
        
        try
        {
            log("Creating bluetooth socket ...");
            mSocket = createSocket(mDevice);
        }
        catch (Exception e)
        {
            log("Exception: "+ e);
        }
    
        if (null==mSocket) // connect was not successful
        {
            log("Socket creation failed");
            mCreatorService.connectionFailed("Socket creation failed");		
            return;		
        }

        if (mDoCancel)
        {
            mWasCancelled = true;
            closeBluetoothSocket();
            mCreatorService.connectionFailed("Connection cancelled");
            return;
        }        

        // Make a connection timeout the BluetoothSocket
        try 
        {
            // This is a blocking call and will only return on a successful connection or an exception
            log("Connecting socket ...");
            mSocket.connect(); 
            log("Socket connected!");
        } 
        catch (IOException ioe)
        {
            String msg = ioe.getMessage();
            log("Connection failed: "+ msg);
            mCreatorService.connectionFailed(msg);
            // Close the socket
            closeBluetoothSocket();
    
            mCreatorService.setState(this, SPPService.STATE_FAILED);
            return;
        }
        catch (Exception e) 
        {
            log("Connection failed: "+ e);
            mCreatorService.connectionFailed("" + e);
            // Close the socket
            closeBluetoothSocket();
    
            mCreatorService.setState(this, SPPService.STATE_FAILED);
            return;
        }
    
        if (mDoCancel)
        {
            mWasCancelled = true;
            closeBluetoothSocket();
            mCreatorService.connectionFailed("Connection cancelled");
            return;
        }        

        if (!mCreatorService.attachConnectThread(this))
        {
            cancel();
            return;
        }
    
        mCreatorService.setState(this, SPPService.STATE_CONNECTED);
    
        if (!getSocketStreams())
        {
            mAttachedService.setState(this, SPPService.STATE_FAILED);
            mAttachedService.releaseConnectThread(this);
            closeBluetoothSocket();
            return;
        }
        else if (mLinewise)
        {
            communicateLinewise();
        }
        else
        {
            communicateBytewise();
        }
        
        if (mDoCancel)
	{
	    mWasCancelled = true;
	    mAttachedService.connectionLost("Connection cancelled");
	}

	closeBluetoothSocket();
	log("ConnectedThread terminating");
    }


    public void cancel() 
    {	    
        synchronized(this) 
        {
            try  
            {
        	mDoCancel = true;
        	this.interrupt();
            } 
            catch (Exception e) 
            {
        	Log.e(TAG, "cancel(): ", e);
            }
    
            try
            {
        	if (null==mSocket)
        	{
        	    Log.e(TAG, "no (more) socket timeout close");		    
        	}
        	else
        	{
        	    Log.e(TAG, "closing socket " + mSocket);
    
        	    //DisconnectThread dt = new DisconnectThread(mmSocket);
        	    //dt.start();
        	    closeBluetoothSocket();
        	}
            } 
            catch (Exception e) 
            {
        	Log.e(TAG, "close() of connect socket failed", e);
            }
            mAttachedService.setState(this, SPPService.STATE_DISCONNECTED);
        }
    }


    // called by sspService
    public void attachToService(SPPService sppService)
    {
	this.mAttachedService = sppService;
    }

    // called by sspService
    public void releaseFromService(SPPService sppService)
    {
	this.mAttachedService = null;
    }    
    

    public void write(byte[] buffer)
    {
        try 
        {
            if (null!=mOutStream)
            {
        	mOutStream.write(buffer);
        	mOutStream.flush();
            }
        } 
        catch (IOException e) 
        {
            Log.e(TAG, "Exception during write", e);
        }
    }


    private void logDeviceServices(BluetoothDevice device)
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


    private void log(String msg)
    {
	synchronized (this)
	{
	    Log.d(TAG, msg);
	    
	    if (null==mAttachedService) 
	    {
		return;
	    }
	    mAttachedService.sendDebug(this, msg);
	}
    }

    private void communicateLinewise() 
    {
	log("linewise communicate starting");

	InterruptableBufferedReader br = new InterruptableBufferedReader(new InputStreamReader(mInputStream));

	// Keep listening timeout the InputStream while connected
	while (!mDoCancel && null!=mInputStream) 
	{
	    try 
	    {
		String line = br.readLine();
		//log("ConnectedThread: read line: " + line);
		if (null!=mAttachedService)
		{
		    mAttachedService.sendMessageString(this, line);
		}
		else
		{
		    Log.d(TAG, "Dropped (no handler): " + line);
		}
	    } 
	    catch (IOException e) 
	    {
		log("ConnectedThread: communicateLinewise: " + e);
		if (!mDoCancel)
		{
		    mAttachedService.connectionLost(e.getMessage());
		}
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
    }


    private void communicateBytewise() 
    {
	log("byte wise communicate starting");

	byte[] buffer = new byte[1024];
	int bytes;

	// Keep listening timeout the InputStream while connected
	while (!mDoCancel) 
	{
	    try 
	    {
		// Read from the InputStream
		bytes = mInputStream.read(buffer);
		log("ConnectedThread: read " + bytes + " bytes)");

		if (null!=mAttachedService)
		{
		    mAttachedService.sendMessageBytes(this, SPPService.MESSAGE_READ, bytes, buffer);
		}
	    } 
	    catch (IOException e) 
	    {
		log("ConnectedThread: communicateBytewise: " + e);
		if (!mDoCancel)
		{
		    mAttachedService.connectionLost(e.getMessage());
		}
		break;
	    }
	}
    }

    
    private BluetoothSocket createSocket(BluetoothDevice device)
    {	    
        BluetoothSocket tmp = null;	
    
        // Try timeout connect with HC-05 device
        if (BTMagics.isHC05(device) || BTMagics.isUncategorized(device))
        {
            log("ConnectThread: creating socket via createRfcommSocket");
            try 
            {
        	Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
        	int port = 1;
        	tmp = (BluetoothSocket) m.invoke(mDevice, port);
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
            try 
            {
        	log("ConnectThread: creating socket via createRfcommSocketToServiceRecord");
        	tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } 
            catch (IOException e) 
            {
        	log ("createRfcommSocketToServiceRecord: Exception: " + e);
        	tmp = null;
            }
        }
    
        if (null==tmp)
        {
            log("ConnectThread: socket creation failed");
        }
    
        return tmp;
    }


    private boolean getSocketStreams()
    {
	BluetoothSocket socket = mSocket;
	InputStream tmpIn   = null;
	OutputStream tmpOut = null;

	// Get the BluetoothSocket input and output streams
	try 
	{
	    tmpIn  = socket.getInputStream();
	    tmpOut = socket.getOutputStream();
	} 
	catch (IOException e) 
	{
	    log("ConnectedThread: Exception getting socket streams: " + e);
	    return false;
	}

	mInputStream  = tmpIn;
	mOutStream = tmpOut;
	return true;
    }


    private void closeBluetoothSocket()
    {
        synchronized (this)
        {	            
            BluetoothSocket socket = mSocket;
            mSocket       = null;
            mInputStream  = null;
            mOutStream    = null;
    
            if (null==socket)
            {
        	Log.d(TAG, "closeBluetoothSocket: socket==null, returning");
        	return;
            }
    
            try 
            {
        	InputStream in = socket.getInputStream();
        	Log.d(TAG, "closeBluetoothSocket: closing input stream");
        	in.close();
            }
            catch (IOException ioex) 
            {
        	log("unable to close streams on BT socket: " + ioex);
            }
            try 
            {
        	OutputStream out = socket.getOutputStream();
        	Log.d(TAG, "closeBluetoothSocket: closing output stream");
        	out.flush();
        	out.close();
            }
            catch (IOException ioex) 
            {
        	log("unable to close streams on BT socket: " + ioex);
            }
            try 
            {
        	Log.d(TAG, "closeBluetoothSocket: closing socket");
        	socket.close();
        	Log.d(TAG, "closeBluetoothSocket: socket closed");
            }
            catch (IOException ioex) 
            {
        	log("unable to close BT socket: " + ioex);
            }
        }
    }


    // a replacement for the "normal" BufferedReader, however this one is aware 
    // of whether the connect thread was cancelled
    private class InterruptableBufferedReader
    {
        private InputStreamReader reader;
    
        InterruptableBufferedReader(InputStreamReader inputStreamReader)
        {
            this.reader = inputStreamReader;
        }
    
        String readLine() throws IOException
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
            while (!mDoCancel);
    
            return null;
        }
    
        void close() throws IOException
        {
            reader.skip(100000);
            reader.close();	    
        }
    }


    private final String TAG = ConnectThread.class.getSimpleName();

    private BluetoothSocket mSocket;
    private OutputStream    mOutStream;
    private InputStream     mInputStream;
    private boolean         mDoCancel;
    private boolean         mWasCancelled;
    private boolean         mLinewise;
    private BluetoothDevice mDevice;
    private SPPService      mCreatorService;
    private SPPService      mAttachedService;

} 
// ConnectThread