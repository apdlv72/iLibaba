package com.apdlv.ilibaba.strip;


import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.HashMap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Scroller;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.color.HSVColorWheel;
import com.apdlv.ilibaba.color.OnColorSelectedListener;
import com.apdlv.ilibaba.gate.BluetoothSerialService;
import com.apdlv.ilibaba.gate.DeviceListActivity;
import com.apdlv.ilibaba.gate.GateControlActivity;
import com.apdlv.ilibaba.shake.Shaker.Callback;

public class StripControlActivity extends Activity implements OnSeekBarChangeListener, OnClickListener, Callback, OnColorSelectedListener, OnItemSelectedListener
{
    private TextView mTextCommand;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mTitle;

    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PRESET    = 3;
    private static final int REQUEST_CONNECT_DEVICE = 1;

    private BluetoothConnector mmBTConnector;
    private TextView mLogView;
    private HSVColorWheel mColorWheel;
    private Vibrator mVibrator;
    private long activityStarted;
    private Spinner mSpinnerMode;

    private String mmSelectedAddress;
    private String mmSelectedName;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);	

	//requestWindowFeature(Window.FEATURE_NO_TITLE);
	//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

	// Set up the window layout
	requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	setContentView(R.layout.activity_strip);
	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

	// Set up the custom title
	mTitle = (TextView) findViewById(R.id.title_left_text);
	if (null!=mTitle) mTitle.setText(R.string.view_name_water);

	mTitle = (TextView) findViewById(R.id.title_right_text);
	mTitle.setText("test");

	setContentView(R.layout.activity_strip);

	(mSpinnerMode = ((Spinner)findViewById(R.id.spinnerMode))).setOnItemSelectedListener(this);

	((SeekBar) findViewById(R.id.seekBright)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekAmplitude)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekSpeed)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekFade)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekStrength)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekRand)).setOnSeekBarChangeListener(this);

	mLogView = (TextView) findViewById(R.id.logView);
	mLogView.setMaxLines(1000);
	mLogView.setMovementMethod(ScrollingMovementMethod.getInstance());
	Scroller scroller = new Scroller(mLogView.getContext());
	mLogView.setScroller(scroller);	

	mTextCommand = (TextView) findViewById(R.id.textCommand);

	mColorWheel = (HSVColorWheel) findViewById(R.id.hSVColorWheel);
	mColorWheel.setListener(this);

	//enableControls(false);
	enableControls(true);

	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	// If the adapter is null, then Bluetooth is not supported
	if (mBluetoothAdapter == null) {
	    Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
	    finish();
	    return;
	}

	mmBTConnector = new BluetoothConnector(getApplicationContext(), mHandler);

	//Shaker shaker = new Shaker(this, 2*1.25d, 500, this);

	mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	activityStarted = Calendar.getInstance().getTimeInMillis();

	loadPeerInfo();
    }


    @Override
    protected void onStart()
    {
	super.onStart();

	if (!mBluetoothAdapter.isEnabled()) 
	{
	    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	    // Otherwise, setup the chat session
	} 
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.options_strip, menu);
	return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) 
    {
	switch (item.getItemId()) {
	case R.id.menu_water_reconnect:
	    if (null!=mmSelectedAddress)
	    {
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mmSelectedAddress);
		mmBTConnector.connect(device);
	    }
	    break;
	case R.id.menu_water_next:
	    nextActivity();
	    return true;
	case R.id.menu_water_on:
	    setCmd("POW=1");
	    return true;
	case R.id.menu_water_off:
	    setCmd("POW=0");
	    return true;
	case R.id.menu_water_select:
	    // Launch the PresetsActivity to see devices and do scan
	    Intent serverIntent = new Intent(this, DeviceListActivity.class);
	    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	    return true;
	case R.id.menu_water_disconnect:
	    mmBTConnector.disconnect();
	    break;
	case R.id.menu_water_presets:
	    Intent serverIntent2 = new Intent(this, PresetsActivity.class);
	    startActivityForResult(serverIntent2, REQUEST_PRESET);
	    break;
	    //	case R.id.discoverable: // Ensure this device is discoverable by others
	    //          ensureDiscoverable();
	    //          return true;
	default:
	    Toast.makeText(getApplicationContext(), "Unknown option " + item.getItemId(), Toast.LENGTH_SHORT).show();
	}
	return false;
    }


    private void nextActivity()
    {
	Intent i = new Intent(getApplicationContext(), GateControlActivity.class);
	startActivity(i);            
	finish();	
    }


    public void onProgressChanged(SeekBar s, int progress, boolean fromUser)
    {
	String command = null==s.getTag() ? null : "" + s.getTag() + "=" + s.getProgress();	
	if (null!=command)
	{
	    setCmd(command);
	}
    }

    public void onStartTrackingTouch(SeekBar seekBar)
    {
    }

    public void onStopTrackingTouch(SeekBar seekBar)
    {
    }

    @Override
    protected void onStop() 
    {
	super.onStop();
	mmBTConnector.disconnect();
    };

    @Override
    protected void onDestroy() 
    {
	super.onDestroy();
	mmBTConnector.disconnect();	
    };

    @Override
    protected void onPause() 
    {
	super.onPause();
	//mmBTConnector.disconnect();	
    };

    void setCmd(String s)
    {
	mTextCommand.setText(s);
	mmBTConnector.write((s+"\r\n").getBytes());
	doLog("SENT: " + s);
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


    private void doLog(String s)
    {
	mLogView.append(s + "\n");
	scrollToEnd();
	Log.d(TAG, s);
    }

    public void onClick(View v)
    {
	if (v instanceof Button)
	{
	    Button b = (Button)v;
	    if (b==v)
	    {
		setCmd("STA=2");
	    }
	}
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
	switch (requestCode) 
	{
	case REQUEST_PRESET:
	    Bundle b = null==data ? null : data.getExtras();
	    if (null!=b)
	    {
		String name    = b.getString("NAME");
		String action  = b.getString("ACTION");
		applyPresets(action, name);
	    }
	    break;

	case REQUEST_ENABLE_BT:
	    // When the request to enable Bluetooth returns
	    if (resultCode == Activity.RESULT_OK) {
		// Bluetooth is now enabled, so set up a chat session
		//setupChat();
	    } else {
		// User did not enable Bluetooth or an error occured
		Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
		finish();
	    }
	    break;

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
		//mChatService.connect(device);
		mmBTConnector.connect(device);
	    }
	    break;

	}
    }

    /*
    private View findViewWithTag(ViewGroup root, String tag)
    {
	for (int i=0; i<root.getChildCount(); i++)
	{
	    View child = root.getChildAt(i);
	    Object o = child.getTag();
	    if (o instanceof String)
	    {
		String found = (String)o;
		if (null!=found && found.equals(tag))
		{
		    return child;
		}
		Log.d(TAG, "found view with tag: " + tag);
	    }
	    else if (o instanceof ViewGroup)
	    {
		ViewGroup vg = (ViewGroup)o;
		vg.findViewWithTag(tag);
	    }
	}

    }
     */


    private void applyPresets( String action, String name)
    {
	ViewGroup root = (ViewGroup)findViewById(R.id.topLinearLayout);

	String tags[] = { "BRI", "SPD", "FAD", "AMP", "RND", "STR", "RGB" };

	if (action.equalsIgnoreCase("load"))
	{
	    HashMap<String, Integer> b = loadPreset(name);
	    if (null==b)
	    {
		return;
	    }
	    Integer mode = b.get("MOD");
	    if (null!=mode)
	    {
		setCmd(String.format("MOD=%x", mode));
		mSpinnerMode.setSelection(mode);
	    }
	    Integer color = b.get("RGB");
	    if (null!=color)
	    {	    
		mColorWheel.setColor(color);
		setCmd(String.format("RGB=%x", color & 0xffffff));
	    }

	    for (String tag : tags)
	    {
		View view  = root.findViewWithTag(tag);
		Integer value = b.get(tag);
		if (null!=view && (view instanceof SeekBar) && null!=value)
		{
		    ((SeekBar)view).setProgress(value);
		    setCmd(String.format("%s=%x", tag, value));
		}
	    }	    
	}
	else
	{
	    HashMap<String, Integer> b = new HashMap<String, Integer>();
	    for (String tag : tags)
	    {
		View view  = root.findViewWithTag(tag);
		if (null!=view && (view instanceof SeekBar))
		{
		    Integer value = ((SeekBar)view).getProgress();
		    if (null!=value)
		    {
			b.put(tag, value);
		    }		    
		}		
	    }
	    //mMo
	    int mode  = mSpinnerMode.getSelectedItemPosition();
	    int color = mColorWheel.getSelectedColor();
	    b.put("MOD", mode);
	    b.put("RGB", color);
	    savePreset(name, b);
	}
    }


    private void savePreset(String name, HashMap<String, Integer> b)
    {
	String FILENAME = "preset_" + name;
	try
	{
	    ObjectOutputStream oos = new ObjectOutputStream(openFileOutput(FILENAME, Context.MODE_PRIVATE));
	    oos.writeObject(b);
	    oos.close();
	    showToast("Preset '" + name + "' saved");
	} 
	catch (Exception e)
	{
	    Toast.makeText(getApplicationContext(), "Failed to save preset '" + name + "': " + e, Toast.LENGTH_LONG).show();
	} 
    }


    private void showToast(String string)
    {
	Toast.makeText(getApplicationContext(), string, Toast.LENGTH_LONG).show();	
    }


    private HashMap<String, Integer> loadPreset(String name)
    {
	String FILENAME = "preset_" + name;
	try
	{
	    ObjectInputStream oos = new ObjectInputStream(openFileInput(FILENAME));
	    @SuppressWarnings("unchecked")
            HashMap<String, Integer> b = (HashMap<String, Integer>) oos.readObject();
	    oos.close();
	    showToast("Preset '" + name + "' loaded");
	    return b;
	} 
	catch (Exception e)
	{
	    Toast.makeText(getApplicationContext(), "Failed to log preset '" + name + "': " + e, Toast.LENGTH_LONG).show();
	}
	return null;
    }


    @SuppressWarnings("unused")
    private void appendTitle(String msg)
    {
	if (null!=mTitle)
	{
	    mTitle.append(msg);
	}
    }


    private void updateSelectedInfo()
    {
	//mInfoArea.setText("" + mmSelectedName + "\n" + mmSelectedAddress);
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

    // interface OnColorSelectedListener
    public void colorSelected(Integer color)
    {
	setCmd(String.format("RGB=%06x", color & 0xFFFFFF));
    }


    public void onItemSelected(AdapterView<?> av, View v, int arg2, long arg3)
    {
	Spinner spinner = (Spinner)av;
	Object item = spinner.getSelectedItem();
	int idx = spinner.getSelectedItemPosition();
	setCmd("MOD=" + idx + "," + item);
    }


    public void onNothingSelected(AdapterView<?> arg0)
    {
    }

    private void setTitleMessage(String s)
    {
	mTitle.setText(s);
    }
    /*
    public void onBTStateChanged(int state, String msg)
    {
	Log.d(TAG, "onBTStateChanged: " + state + ", " + msg);
	switch (state)
	{
	case BluetoothConnector.STATE_NONE: 		setTitleMessage("none"); break;
	case BluetoothConnector.STATE_LISTEN:		setTitleMessage("listen"); break;	
	case BluetoothConnector.STATE_CONNECTING:	setTitleMessage("connecting"); break;
	case BluetoothConnector.STATE_CONNECTED:	setTitleMessage("connected"); break;
	case BluetoothConnector.STATE_DISCONNECTED:	setTitleMessage("disconnected"); break;
	case BluetoothConnector.STATE_TIMEOUT:		setTitleMessage("timeout"); break;
	default: setTitleMessage("unknown(" + state + ")");
	}
    }
     */

    public void onBTDataReceived(byte[] data, int len)
    {
	String s = new String(data, 0, len);
	Log.d(TAG, "onBTDataReceived: '" + s + "'");
    }

    private static final String TAG = "WaterStrip";


    private String receivedLine = "";


    private void enableControls(boolean on)
    {
	if (on)
	{
	    ((SeekBar) findViewById(R.id.seekBright)).setEnabled(true);
	    ((SeekBar) findViewById(R.id.seekFade)).setEnabled(true);

	    ((SeekBar) findViewById(R.id.seekSpeed)).setVisibility(View.VISIBLE);
	    ((SeekBar) findViewById(R.id.seekAmplitude)).setVisibility(View.VISIBLE);
	    ((SeekBar) findViewById(R.id.seekRand)).setVisibility(View.VISIBLE);
	    ((SeekBar) findViewById(R.id.seekStrength)).setVisibility(View.VISIBLE);
	    mColorWheel.setEnabled(true);
	}
	else
	{
	    ((SeekBar) findViewById(R.id.seekBright)).setEnabled(false);
	    ((SeekBar) findViewById(R.id.seekFade)).setEnabled(false);

	    ((SeekBar) findViewById(R.id.seekSpeed)).setVisibility(View.INVISIBLE);
	    ((SeekBar) findViewById(R.id.seekAmplitude)).setVisibility(View.INVISIBLE);
	    ((SeekBar) findViewById(R.id.seekRand)).setVisibility(View.INVISIBLE);
	    ((SeekBar) findViewById(R.id.seekStrength)).setVisibility(View.INVISIBLE);
	    mColorWheel.setEnabled(false);	    
	}
    }

    private final Handler mHandler = new Handler() {

	private Object mConnectedDeviceName;

	@Override
	public void handleMessage(Message msg) {

	    Log.d(TAG, "Got message "+ msg);
	    switch (msg.what) {

	    case BluetoothConnector.MESSAGE_DEBUG_MSG:
		String logLine = (String)msg.obj; Log.d(TAG, logLine);
		doLog(logLine);
		break;

	    case BluetoothConnector.MESSAGE_STATE_CHANGE:
		doLog("MESSAGE_STATE_CHANGE: " + msg.arg1);

		enableControls(false);
		switch (msg.arg1) {
		case BluetoothSerialService.STATE_CONNECTED:
		    doLog("STATE_CONNECTED");
		    setTitleMessage("connected to " + mConnectedDeviceName);
		    setCmd("HELLO");
		    enableControls(true);
		    break;
		case BluetoothSerialService.STATE_CONNECTING:
		    doLog("STATE_CONNECTING");
		    setTitleMessage("connecting");
		    break;
		case BluetoothSerialService.STATE_DISCONNECTED:
		    doLog("STATE_DISCONNECTED");
		    setTitleMessage("disconnected");
		    break;
		case BluetoothSerialService.STATE_TIMEOUT:
		    doLog("STATE_TIMEOUT");
		    setTitleMessage("timeout");
		    break;
		case BluetoothSerialService.STATE_LISTEN:
		case BluetoothSerialService.STATE_NONE:
		    doLog("STATE_LISTEN/STATE_NONE");
		    setTitleMessage("not connected");
		    break;
		}
		break;

	    case BluetoothConnector.MESSAGE_READ:                
		Log.d(TAG, "Got MESSAGE_READ "+ msg);
		byte[] readBuf = (byte[]) msg.obj;
		//Log.d(TAG, "Got payload "+ format(readBuf, 128));	

		String payload = new String(readBuf, 0, msg.arg1);		
		doLog("RCVD: " + payload);
		synchronized (receivedLine)
		{
		    receivedLine += payload;
		    if (receivedLine.endsWith("\r") || receivedLine.endsWith("\n"))
		    {
			doLog("COMMAND: " + receivedLine);
			handleCommand(receivedLine);
			receivedLine = "";
		    }
		}

		break;

	    case BluetoothConnector.MESSAGE_DEVICE_NAME:
		// save the connected device's name
		mConnectedDeviceName = msg.getData().getString(BluetoothConnector.DEVICE_NAME);
		Toast.makeText(getApplicationContext(), "Connected to "
			+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
		break;

		// shared back messages sent to remote. 
		//              case MESSAGE_WRITE:
		//          	Log.d(TAG, "Got MESSAGE_WRITE "+ msg);
		//                  break;

	    case BluetoothConnector.MESSAGE_TOAST:
		Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothConnector.TOAST), Toast.LENGTH_SHORT).show();
		break;
	    }
	}

	String mSupportedFeatures = "";

	private void handleCommand(String receivedLine)
	{
	    if (null!=mLogView)
	    {
		//doLog("RCVD: " + receivedLine);
	    }

	    if (receivedLine.startsWith("HELLO"))
	    {
		mSupportedFeatures = receivedLine.substring(5);
		for (char c : mSupportedFeatures.toUpperCase().toCharArray())
		{
		    switch (c)
		    {
		    case 'B': // brightness 
			break;
		    case 'A': // amplitude
			break;
		    case 'P': // sPeed 
			break;
		    case 'F': // fade 
			break;
		    case 'R': // random 
			break;
		    case 'S': // strength
			break;
		    }
		}
	    }
	}
    };


    private void savePeerInfo()
    {
	String FILENAME = "remote_water.txt";

	doLog("SELECTED: " + mmSelectedName + " " + mmSelectedAddress);

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
	    Toast.makeText(getApplicationContext(), "Failed to peer info: " + e, Toast.LENGTH_LONG).show();
	} 
    }


    private void loadPeerInfo()
    {
	String FILENAME = "remote_water.txt";

	try
	{
	    BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(FILENAME)));
	    mmSelectedAddress = reader.readLine();
	    mmSelectedName = reader.readLine();
	    reader.close();	    
	} 
	catch (Exception e)
	{
	    Toast.makeText(getApplicationContext(), "Failed to load peer info: " + e, Toast.LENGTH_LONG).show();
	} 
    }



}