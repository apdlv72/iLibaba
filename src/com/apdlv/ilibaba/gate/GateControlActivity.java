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

package com.apdlv.ilibaba.gate;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.frotect.BTFrotectSerialService;
import com.apdlv.ilibaba.shake.Shaker.Callback;
import com.apdlv.ilibaba.strip.StripControlActivity;

/**
 * This is the main Activity that displays the current chat session.
 */
public class GateControlActivity extends Activity implements Callback, OnClickListener {
    // Debugging
    private static final String TAG = "GateControlActivity";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_DEBUG_MSG = 6;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;
    private ImageButton mButtonOpen;
    private ImageButton mButtonBS;

    // Name of the connected device
    private String mConnectedDeviceName = null;   
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothSerialService mChatService = null;
    private TextView mPinArea;
    private TextView mInfoArea;
    private TextView mLogView;
    private Vibrator mVibrator;


    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

	//requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_gate);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        if (null!=mTitle) mTitle.setText(R.string.view_name_garage);        
        mTitle = (TextView) findViewById(R.id.title_right_text);

        (mPinArea = (TextView) findViewById(R.id.pinText)).setCursorVisible(false);        
        (mInfoArea = (TextView) findViewById(R.id.infoArea)).setTextColor(Color.RED);
        mLogView = (TextView) findViewById(R.id.logView);
        
        
        mInfoArea.setOnClickListener(this);
        
        mLogView.setMaxLines(1000);
        mLogView.setMovementMethod(ScrollingMovementMethod.getInstance());
        Scroller scroller = new Scroller(mLogView.getContext());
        mLogView.setScroller(scroller);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            sendToast("Bluetooth is not available");
            finish();
            return;
        }
        
        loadPeerInfo();
        updateSelectedInfo();
        
	//Shaker shaker = new Shaker(this, 1.25d, 500, this);
	//Shaker shaker = new Shaker(this, 2*1.25d, 500, this);

	mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	activityStarted = Calendar.getInstance().getTimeInMillis();
    }
    
    long activityStarted;

    
    @Override
    public void onStart() 
    {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
        //connectInAdvance();
    }

    
    @Override
    public synchronized void onPause() 
    {
        super.onPause();
        if (mChatService != null) mChatService.disconnect();
        mPinArea.setText("");
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    
    @Override
    public synchronized void onResume() 
    {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven'temp started already
            if (mChatService.getState() == BluetoothSerialService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }
/*    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) 
    {
	super.onConfigurationChanged(newConfig);
        if(D) Log.e(TAG, "+ ON CONFIG CHANGE +");
    };
*/
    
    @Override
    public void onStop() {
        super.onStop();
        if (mChatService != null) mChatService.disconnect();
        mPinArea.setText("");
        if(D) Log.e(TAG, "-- ON STOP --");
        //finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }


    private void scrollToEnd()  
    {
	if (null==mLogView) return;
	Layout layout = mLogView.getLayout();
	if (null==layout) return;
	
        final int scrollAmount = mLogView.getLayout().getLineTop(mLogView.getLineCount())-mLogView.getHeight();
        if (scrollAmount>0)
        {
            mLogView.scrollTo(0, scrollAmount);
        }
        else
        {
            mLogView.scrollTo(0,0);
        }
    }

    
    public void log(String msg)
    {
	if(D) Log.i(TAG,msg);
	mLogView.append(msg + "\n");
	scrollToEnd();
    }
    
    private class DisconnectThread extends Thread
    {
	private long ms;
	private boolean cancelled;

	public DisconnectThread(long ms)
	{
	    this.ms = ms;
	    cancelled = false;
	}
	
	   @Override 
	   public void run()
	   {
	       try { Thread.sleep(ms); } catch (InterruptedException e) {}
	       if (!cancelled)		  
	       {		   
		   Message msg = mHandler.obtainMessage(GateControlActivity.MESSAGE_TOAST);
		   Bundle bundle = new Bundle();
		   bundle.putString(GateControlActivity.TOAST, "Disconnecting");
		   msg.setData(bundle);
		   mHandler.sendMessage(msg);

		   mChatService.disconnect();
	       }
	   }
	   
	   public void cancel()
	   {
	       this.cancelled = true;
	   }
    }
    
    
    private void sendToast(String text)
    {
	   Message msg = mHandler.obtainMessage(GateControlActivity.MESSAGE_TOAST);
	   Bundle bundle = new Bundle();
	   bundle.putString(GateControlActivity.TOAST, text);
	   msg.setData(bundle);
	   mHandler.sendMessage(msg);	
    }
    
    private DisconnectThread disconnectThread = null;
    
    private void connectInAdvance()
    {
	synchronized (this)
	{
	    if (null!=disconnectThread)
	    {
		disconnectThread.cancel();
		disconnectThread = null;
	    }
	    
	    if (null!=mmSelectedAddress && !mConnected)
	    {
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mmSelectedAddress);
		mChatService.connect(device);
	    }
	}
    }
    
    private void disconnectAfter(final int ms)
    {
	synchronized (this)
	{
	    if (null!=disconnectThread)
	    {
		disconnectThread.cancel();
	    }
	    (disconnectThread = new DisconnectThread(ms)).start();		
	}	
    }    

    
    public void buttonPressed(View v)
    {
	Log.d(TAG, "buttonPressed: " + v);
	mVibrator.vibrate(10);
	
	String pin = mPinArea.getText().toString();	    
	if (v instanceof ImageButton)
	{	    
	    ImageButton b = (ImageButton)v;
	    if (mButtonBS==b)
	    {
		if (pin.length()>0)
		{
		    pin = pin.substring(0, pin.length()-1);
		}
	    }
	    else if (mButtonOpen==b)
	    {
		initOpening();
	    }
	}
	else if (v instanceof Button)
	{
	    Button b = (Button)v;
	    pin += b.getText().toString().trim();
	    if (pin.length()==1)
	    {
		connectInAdvance();
	    }
	}
	
	mPinArea.setText(pin);	
    }
    
    private void initOpening()
    {
	sendBTMessage("tartur\n");
	commandIntension = CMD_OPEN;	
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the send value with a listener that for click events
        mButtonBS   = (ImageButton) findViewById(R.id.buttonBS);
        mButtonOpen = (ImageButton) findViewById(R.id.buttonOpen);
        
        // Initialize the BluetoothChatService to perform bluetooth connections
        boolean doListen = false;
        mChatService = new BluetoothSerialService(this, mHandler, doListen);
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendBTMessage(String message) 
    {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothSerialService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            log("SENT: " + message);
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    
    public static final char[] hexchars = new char[] { '0', '1', '2', '3', '4',
	      '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String toHex(byte[] buf, int ofs, int len) {
	    StringBuffer sb = new StringBuffer();
	    int j = ofs + len;
	    for (int i = ofs; i < j; i++) {
	      if (i < buf.length) {
	        sb.append(hexchars[(buf[i] & 0xF0) >> 4]);
	        sb.append(hexchars[buf[i] & 0x0F]);
	        sb.append(' ');
	      } else {
	        sb.append(' ');
	        sb.append(' ');
	        sb.append(' ');
	      }
	    }
	    return sb.toString();
	  }
    
    public static String toAscii(byte[] buf, int ofs, int len) {
	    StringBuffer sb = new StringBuffer();
	    int j = ofs + len;
	    for (int i = ofs; i < j; i++) {
	      if (i < buf.length) {
	        if ((20 <= buf[i]) && (126 >= buf[i])) {
	          sb.append((char) buf[i]);
	        } else {
	          sb.append('.');
	        }
	      } else {
	        sb.append(' ');
	      }
	    }
	    return sb.toString();
	  }

    private static String format(byte[] buf, int width) {
	    int bs = (width - 8) / 4;
	    int i = 0;
	    StringBuffer sb = new StringBuffer();
	    do {
	      for (int j = 0; j < 6; j++) {
	        sb.append(hexchars[(i << (j * 4) & 0xF00000) >> 20]);
	      }
	      sb.append('\t');
	      sb.append(toHex(buf, i, bs));
	      sb.append(' ');
	      sb.append(toAscii(buf, i, bs));
	      sb.append('\n');
	      i += bs;
	    } while (i < buf.length);
	    return sb.toString();
	  }
    
    String receivedLine = "";
    private boolean mConnected;
    
    
    private void setTitleMsg(String msg)
    {
	if (null!=mTitle)
	{
	    mTitle.setText(msg);
	}
    }
    
    private void appendTitle(String msg)
    {
	if (null!=mTitle)
	{
	    mTitle.append(msg);
	}
    }
    
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {

	@Override
        public void handleMessage(Message msg) {
            
            Log.d(TAG, "Got message "+ msg);
            switch (msg.what) {
            case MESSAGE_DEBUG_MSG:
        	String logLine = (String)msg.obj; Log.d(TAG, logLine);
        	log(logLine);
        	break;
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                
                mConnected = false;
                
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                    log("STATE_CONNECTED");
                    setTitle(R.string.title_connected_to);
                    appendTitle(mConnectedDeviceName);
                    mConnected = true;
                    mInfoArea.setTextColor(Color.GREEN);
                    break;
                case BluetoothSerialService.STATE_CONNECTING:
                    log("STATE_CONNECTING");
                    setTitle(R.string.title_connecting);
                    mInfoArea.setTextColor(Color.YELLOW);
                    break;
                case BluetoothSerialService.STATE_DISCONNECTED:
                    log("STATE_DISCONNECTED");
                    setTitleMsg("disconnected");
                    mInfoArea.setTextColor(Color.RED);
                    break;
                case BluetoothSerialService.STATE_TIMEOUT:
                    log("STATE_TIMEOUT");
                    setTitleMsg("timeout");
                    mInfoArea.setTextColor(Color.RED);
                    break;
                case BluetoothSerialService.STATE_LISTEN:
                case BluetoothSerialService.STATE_NONE:
                    log("STATE_LISTEN/STATE_NONE");
                    setTitle(R.string.title_not_connected);
                    mInfoArea.setTextColor(Color.RED);
                    break;
                }
                break;
            case MESSAGE_WRITE:
        	Log.d(TAG, "Got MESSAGE_WRITE "+ msg);
                break;
            case MESSAGE_READ:                
        	Log.d(TAG, "Got MESSAGE_READ "+ msg);
                byte[] readBuf = (byte[]) msg.obj;
                //Log.d(TAG, "Got payload "+ format(readBuf, 128));	

                String payload = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, "Got payload "+ payload);
        	log("RCVD: " + payload);
        	synchronized (receivedLine)
        	{
        	    receivedLine += payload;
        	    if (receivedLine.endsWith("\r") || receivedLine.endsWith("\n"))
        	    {
        		log("COMMAND: " + receivedLine);
        		handleCommand(receivedLine);
        		receivedLine = "";
        	    }
        	}
                
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
		mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
		if (null==mConnectedDeviceName) mConnectedDeviceName = (String)msg.obj;
                sendToast("Connected to " + mConnectedDeviceName);
                break;
            case MESSAGE_TOAST:
        	//sendToast(msg.getData().getString(TOAST));
        	showToast(msg.getData().getString(TOAST));
                break;
            }
        }

    };

    
    static final char CMD_REGISTER = 'r';
    static final char CMD_OPEN     = 'o';
    static final char CMD_PIN      = 'p';   
    static final char CMD_NONE     = 0;
    
    char commandIntension = '?'; 
    int pin    = 1234;
    int newPin = 4321;
    private String mmSelectedAddress;
    private String mmSelectedName;
    
    private void handleCommand(String command)
    {
	try
	{
	    Log.d(TAG, "COMMAND: '"+ command +"'");
	    command = command.replaceAll("[\r\n]", "");

	    char code = command.charAt(0);
	    switch (code)
	    {
	    case 'O' : 
		logSuccess(command.substring(1));
		//disconnectAfter(2000);
		showProgress();
		mChatService.disconnect();
		mPinArea.setText("");
		break;

	    case 'E' : 
		logError(command.substring(1)); 
		break;

	    case 'D' : 
		logDebug(command.substring(1)); 
		break;

	    case 'T' :
		switch (commandIntension)
		{
		case CMD_OPEN:
		{
		    String token = computeToken(command.substring(1), pin);
		    sendBTMessage(CMD_OPEN + token + "\n");
		}
		break;
		case CMD_PIN:
		{
		    String token = computeToken(command.substring(1), pin);
		    sendBTMessage(CMD_PIN + token + "," + newPin + "\n");
		}
		break;
		case CMD_NONE:
		    break;
		}
		commandIntension = CMD_NONE;
		break;

	    default:
		logError(command); 
		break;
	    }
	}
	catch (Exception e)
	{
	    String msg = e.toString();
	    msg = msg.replaceAll("[\r\n]", "");
	    sendBTMessage("E" + msg + "\n");
	}
    }

    protected void showToast(String string)
    {
	Toast.makeText(this, string, Toast.LENGTH_LONG).show();
    }


    private String computeToken(String token, int pin)
    {
	long t = Long.parseLong(token, 36);
	t = (t+pin) % 0xffffffffL;
	return String.format("%x", t);
    }

    private void logError(String msg)
    {
	Log.e(TAG, msg);
    }

    private void logDebug(String msg)
    {
	Log.d(TAG, msg);
    }

    private void logSuccess(String msg)
    {
	Log.i(TAG, msg);
    }

    private void updateSelectedInfo()
    {
        mInfoArea.setText("" + mmSelectedName + "\n" + mmSelectedAddress);
    }
    
    private void savePeerInfo()
    {
	String FILENAME = "remote_info.txt";

	log("SELECT: " + mmSelectedName + " " + mmSelectedAddress);
	
	FileOutputStream fos;
        try
        {
	    fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
	    fos.write(mmSelectedAddress.getBytes()); fos.write('\n');
	    fos.write(mmSelectedName.getBytes()); fos.write('\n');
	    fos.close();
        } 
        catch (Exception e)
        {
            sendToast("Failed to peer info: " + e);
        } 
    }

    
    private void loadPeerInfo()
    {
	String FILENAME = "remote_info.txt";

        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(FILENAME)));
            mmSelectedAddress = reader.readLine();
            mmSelectedName = reader.readLine();
	    reader.close();	    
        } 
        catch (Exception e)
        {
            sendToast("Failed to load peer info: " + e);
        } 
    }

    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When PresetsActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                
                mmSelectedAddress = address;
                mmSelectedName = device.getName();
                savePeerInfo();
                updateSelectedInfo();
                
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_gate, menu);
        return true;
    }

        
    public class CustomizeDialog extends Dialog  
    {
	private ProgressBar mProgress;

	public CustomizeDialog(Context context)   
	{
	    super(context);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.dialog_gate);
	    (mProgress = (ProgressBar) findViewById(R.id.progressBar)).setMax(100);
	    mProgress.setProgress(0);	    
	}

	
	@Override
	public void show()
	{
	    super.show();

	    new Thread(new Runnable() 
	    {
		public void run() 
		{
		    for (int i=0; i<100; i++) 
		    {
			mProgress.incrementProgressBy(1);
			try
                        {
	                    Thread.sleep(10);
                        } 
			catch (InterruptedException e)
                        {
                        }
		    }
		    dismiss();
		}
	    }).start();
	}
    }
    
    private void showProgress()
    {
	(new CustomizeDialog(this)).show();
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) {
        
        // clik on the info area now to see the demo
//        case R.id.menu_garage_test:
//            showProgress();
//            break;
        
        case R.id.menu_garage_next:
            nextActivity();
            return true;            
        case R.id.reconnect:
            connectInAdvance();
            return true;
        case R.id.disconnect:
            mChatService.disconnect();
            return true;
        case R.id.select:
            // Launch the PresetsActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
//        case R.id.discoverable:
//            // Ensure this device is discoverable by others
//            ensureDiscoverable();
//            return true;
        }
        return false;
    }

    /**
     * 
     */
    private void nextActivity()
    {
	Intent i = new Intent(getApplicationContext(), StripControlActivity.class);
	startActivity(i);
	finish();
    }

    public void onClick(DialogInterface dialog, int which)
    {
	// TODO Auto-generated method stub
	
    }

    public void shakingStarted()
    {
	if (Calendar.getInstance().getTimeInMillis()-activityStarted>1000)
	{
	    mVibrator.vibrate(200);
	}
    }


    public void shakingStopped()
    {
	if (Calendar.getInstance().getTimeInMillis()-activityStarted>1000)
	{
	    nextActivity();
	}
    }


    public void onClick(View view)
    {
	if (mInfoArea==view)
	{
	    showProgress();
	}
    }
}