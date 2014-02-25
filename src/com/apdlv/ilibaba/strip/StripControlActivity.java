package com.apdlv.ilibaba.strip;


import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Scroller;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.bt.SPPConnection;
import com.apdlv.ilibaba.bt.SPPDataHandler;
import com.apdlv.ilibaba.bt.SPPService;
import com.apdlv.ilibaba.color.HSVColorWheel;
import com.apdlv.ilibaba.color.OnColorSelectedListener;
import com.apdlv.ilibaba.frotect.FrotectActivity;
import com.apdlv.ilibaba.gate.DeviceListActivity;
import com.apdlv.ilibaba.shake.Shaker.Callback;
import com.apdlv.ilibaba.util.U;

public class StripControlActivity extends Activity implements OnSeekBarChangeListener, Callback, OnColorSelectedListener, OnItemSelectedListener
{
    private TextView mTextCommand;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mTitle;

    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PRESET    = 3;
    private static final int REQUEST_CONNECT_DEVICE = 1;

    // replaced by SSPConnection now
    //private BTStripSerialService mmBTConnector;  
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

	mLogView = (TextView) findViewById(R.id.logView);
	mLogView.setMaxLines(1000);
	mLogView.setMovementMethod(ScrollingMovementMethod.getInstance());
	Scroller scroller = new Scroller(mLogView.getContext());
	mLogView.setScroller(scroller);	

	setContentView(R.layout.activity_strip);

	mTextCommand = (TextView) findViewById(R.id.textCommand);
	mColorWheel = (HSVColorWheel) findViewById(R.id.hSVColorWheel);
	mColorWheel.setListener(this);
	
	(mSpinnerMode = ((Spinner)findViewById(R.id.spinnerMode))).setOnItemSelectedListener(this);		
	{	    
	    StringBuilder sb = new StringBuilder();
	    // Version, Kind, Power, Bright, Amplitude, Speed, Fade, Brightness, Random, sTrength, Color
	    sb.append("H:V=1 K=NONE");
	    
	    //sb.append("ARGB"); // common Anode, RGB	    
	    //sb.append(" P=0-2 B=0-FF A=0-FF S=0-FFF F=0-FFF R=0-FF T=0-FFF C=0-FFFFFF ");
	    // supported mode numbers and names
	    //sb.append("M=0=WATER\t1=WATER2\t2=FADE\t3=RAINBOW\t4=CONST\t5=RAINFLOW\temp\r\n");
	    
	    analyzeHelloLine(sb.toString());	    
	}

	((SeekBar) findViewById(R.id.seekBright)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekAmplitude)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekSpeed)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekFade)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekStrength)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekRand)).setOnSeekBarChangeListener(this);


	enableControls(false);
	//enableControls(true);

	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	// If the adapter is null, then Bluetooth is not supported
	if (mBluetoothAdapter == null) {
	    Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
	    finish();
	    return;
	}
	
	Intent intent = new Intent(this, SPPService.class);
	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);	        
	
	//mmBTConnector = new BTStripSerialService(getApplicationContext(), mHandler, false /* bytewise */);
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
	switch (item.getItemId()) 
	{
	case R.id.menu_water_reconnect:
	    if (null!=mmSelectedAddress)
	    {
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mmSelectedAddress);
		//mmBTConnector.connect(device);
		mConnection.connect(device);
	    }
	    break;
	case R.id.menu_water_next:
	    nextActivity();
	    return true;
	    
	case R.id.menu_water_power:
	    if (mConnection.isConnected())
	    {
		LampListDialog dialog = new LampListDialog(this, lampNames);
		dialog.show();
	    }
	    return true;
/*	    
	case R.id.menu_water_on:
	    setCmd("P=1");
	    return true;
	case R.id.menu_water_off:
	    setCmd("P=0");
	    return true;
*/	    
	case R.id.menu_water_select:
	    // Launch the PresetsActivity to see devices and do scan
	    Intent serverIntent = new Intent(this, DeviceListActivity.class);
	    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	    return true;
	case R.id.menu_water_disconnect:
	    //mmBTConnector.disconnect();
	    mConnection.disconnect();
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
	//Intent i = new Intent(getApplicationContext(), GateControlActivity.class);
	Intent i = new Intent(getApplicationContext(), FrotectActivity.class);
	startActivity(i);            
	finish();	
    }


    public void onProgressChanged(SeekBar s, int progress, boolean fromUser)
    {
	String hexProgress = String.format("%x", progress);
	String command = null==s.getTag() ? null : "" + s.getTag() + "=" + hexProgress; //s.getProgress();	
	if (null!=command)
	{
	    // use the tag from the respective scale and send as command prefix
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
	//mmBTConnector.disconnect();
	mConnection.disconnect();
    };

    @Override
    protected void onDestroy() 
    {
	super.onDestroy();
	//mmBTConnector.disconnect();
	mConnection.disconnect();
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
	//mmBTConnector.write((s+"\n").getBytes());
	mConnection.sendLine(s);
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
		//mmBTConnector.connect(device);
		mConnection.connect(device);
	    }
	    break;

	}
    }

    private void applyPresets( String action, String name)
    {
	ViewGroup root = (ViewGroup)findViewById(R.id.topLinearLayout);

	String tags[] = { "B", "S", "F", "A", "R", "T", "C" };

	if (action.equalsIgnoreCase("load"))
	{
	    HashMap<String, Integer> b = loadPreset(name);
	    if (null==b)
	    {
		return;
	    }
	    Integer mode = b.get("M");
	    if (null!=mode)
	    {
		setCmd(String.format("M=%x", mode));
		mSpinnerMode.setSelection(mode);
	    }
	    Integer color = b.get("C");
	    if (null!=color)
	    {	    
		mColorWheel.setColor(color);
		setCmd(String.format("C=%x", color & 0xffffff));
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
	    b.put("M", mode);
	    b.put("C", color);
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
	setCmd(String.format("C=%06x", color & 0xFFFFFF));
    }


    public void onItemSelected(AdapterView<?> av, View v, int arg2, long arg3)
    {
	Spinner spinner = (Spinner)av;
	//Object item = spinner.getSelectedItem();
	int idx = spinner.getSelectedItemPosition();
	setCmd("M=" + idx); // + "," + item);
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
	case BTStripSerialService.STATE_NONE: 		setTitleMessage("none"); break;
	case BTStripSerialService.STATE_LISTEN:		setTitleMessage("listen"); break;	
	case BTStripSerialService.STATE_CONNECTING:	setTitleMessage("connecting"); break;
	case BTStripSerialService.STATE_CONNECTED:	setTitleMessage("connected"); break;
	case BTStripSerialService.STATE_DISCONNECTED:	setTitleMessage("disconnected"); break;
	case BTStripSerialService.STATE_TIMEOUT:		setTitleMessage("timeout"); break;
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

    
    private int visibleIfFeatured(FEATURE f)
    {
	return supportedFeatures.contains(f) ? View.VISIBLE : View.INVISIBLE;
    }
    

    private void enableControls(boolean on)
    {
	//String features = "" + supportedFeatures;
	
	if (on)
	{
	    ((SeekBar) findViewById(R.id.seekBright)   ).setVisibility(visibleIfFeatured(FEATURE.BRIGHTNESS));
	    ((SeekBar) findViewById(R.id.seekFade)     ).setVisibility(visibleIfFeatured(FEATURE.FADING));
	    ((SeekBar) findViewById(R.id.seekSpeed)    ).setVisibility(visibleIfFeatured(FEATURE.SPEED));
	    ((SeekBar) findViewById(R.id.seekAmplitude)).setVisibility(visibleIfFeatured(FEATURE.AMPLITUDE));
	    ((SeekBar) findViewById(R.id.seekRand)     ).setVisibility(visibleIfFeatured(FEATURE.RANDOMNESS));
	    ((SeekBar) findViewById(R.id.seekStrength) ).setVisibility(visibleIfFeatured(FEATURE.STRENGTH));
	    mColorWheel.setEnabled(true);
	    mSpinnerMode.setEnabled(true);
	}
	else
	{
	    ((SeekBar) findViewById(R.id.seekBright)   ).setVisibility(View.INVISIBLE);	    
	    ((SeekBar) findViewById(R.id.seekFade)     ).setVisibility(View.INVISIBLE);	    
	    ((SeekBar) findViewById(R.id.seekSpeed)    ).setVisibility(View.INVISIBLE);
	    ((SeekBar) findViewById(R.id.seekAmplitude)).setVisibility(View.INVISIBLE);
	    ((SeekBar) findViewById(R.id.seekRand)     ).setVisibility(View.INVISIBLE);
	    ((SeekBar) findViewById(R.id.seekStrength) ).setVisibility(View.INVISIBLE);
	    U.setEnabled(mColorWheel,  false);	    
	    U.setEnabled(mSpinnerMode, false);
	}
    }


    private void parseStripModes(String modeStr)
    {
	//ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.mode_name);
	mSpinnerMode.setAdapter(adapter);
	String[] parts = modeStr.split("[\\t ]+");
	
	for (String name : parts)
	{
	    name = name.replaceAll("^[0-9]+=", "").replaceAll("_", " ");
	    adapter.add(name);
	}
    }
    
    private void setStripModes(String ... names)
    {
	//ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.mode_name);
	mSpinnerMode.setAdapter(adapter);
	
	for (String name : names)
	{
	    adapter.add(name);
	};
    }


    private void analyzeUpdateLine(String line)
    {
	doLog("GOT Update LINE: " + line);
    }
    
    
    enum FEATURE 
    { 
	AMPLITUDE,
	BRIGHTNESS,
	COLOR,
	POWER,
	SPEED,
	STRENGTH,
	FADING,
	RANDOMNESS
    };
    
    private Set<FEATURE> supportedFeatures = new HashSet<StripControlActivity.FEATURE>();
    private int numberLamps = 0;
    private String[] lampNames;
    
    void resetFeatures()
    {
	supportedFeatures.clear();
    }
    
    void setFeature(FEATURE feature, boolean supported)
    {	
	if (supported) supportedFeatures.add(feature); else supportedFeatures.remove(feature);
    }
    
    void setLamps(String ... lamps)
    {	
	numberLamps = lamps.length;
	this.lampNames = lamps;
    }

    private void analyzeHelloLine(String line)
    {
	doLog("GOT HELLO LINE: " + line);
	try
	{
	    resetFeatures();
	    
	    Pattern pKind = Pattern.compile("K=([^ ]*)");
	    Matcher matcher = pKind.matcher(line);
	    if (matcher.find())
	    {
		String kind = matcher.group(1);
		if ("NONE".equalsIgnoreCase(kind))
		{
		    enableControls(false);
		    return;
		}
		else if ("ARGB".equalsIgnoreCase(kind))
		{
		    // H:V=1 K=ARGB P=0-2 B=0-FF A=0-FF S=0-FFF F=0-FFF R=0-FF T=0-FFF C=0-FFFFFF M=0=Water 1=WaterColored 2=Fading 3=Rainbow 4=Constant 5=RainFlow	
		    //setStripModes("Water", "WaterColored", "Fading", "Rainbow", "Constant", "RainFlow");
		    setLamps("one");
		}		
		else if ("LPD6803".equalsIgnoreCase(kind))
		{
		    // H:V=1 K=LPD6803 P=0-2 B=0-FF A=0-FF S=0-FFF F=0-FFF R=0-FF T=0-FFF C=0-FFFFFF M=0=Water	1=WaterColored	2=Fading 3=Rainbow 4=RainbowLong 5=RainbowShort 6=Constant 7=RainFlow 8=RainFlowShort	
		    //setStripModes("Water1", "Water2", "Rainbow1", "Rainbow2", "Rainbow3",  "Fading1", "Fading2", "Funky");
		    setLamps("one");
		}
		else if ("OTURMA".equals(kind))
		{
		    // H:V=1 K=OTURMA L=Up_1,Up_2,Down,Strip_1,Strip_2 P=0-2 B=0-FF C=0-FFFFFF M=0=UP1+2	1=UP1+2/DOWN	2=UP1/DOWN	3=DOWN	4=STRIP1	5=STRIP1+2	6=STRIP2	7=FAKETV
		    
		    Pattern p = Pattern.compile(" L=([a-zA-Z_0-9,\\.]+) ");
		    matcher = p.matcher(line);
		    if (matcher.find())
		    {
			String value = matcher.group(1);
			String lamps[] = value.split(",");
			setLamps(lamps);			
		    }		    
		    else
		    {		    
			setLamps("Up 1", "Up 2", "Down", "Strip 1", "Strip 2");
		    }

		    //setStripModes("Up+Up", "Up+Up+Down", "Up+Down", "Down", "Strip1", "Strip1+2", "Strip2", "FakeTV");		    
		}
		
		setFeature(FEATURE.AMPLITUDE,  line.contains(" A="));
		setFeature(FEATURE.BRIGHTNESS, line.contains(" B="));
		setFeature(FEATURE.COLOR,      line.contains(" C="));
		setFeature(FEATURE.POWER,      line.contains(" P="));
		setFeature(FEATURE.SPEED,      line.contains(" S="));
		setFeature(FEATURE.STRENGTH,   line.contains(" T="));
		setFeature(FEATURE.FADING,     line.contains(" F="));		    
		setFeature(FEATURE.RANDOMNESS, line.contains(" R="));
		
		String modes = line.replaceAll("^.*M=", "").replaceAll("[\r\n].*", "");
		parseStripModes(modes);
		
		// enable controls here (after we know the features)
		enableControls(true);
		return;
	    }		
	}
	catch (Exception e)
	{
	    doLog("analyzeHelloLine: " + e);
	}

	// fallback
	//setStripModes("Mode 1", "Mode 2", "Mode 3", "Mode 4", "Mode 5",  "Mode 6", "Mode 7", "Mode 8");
    }


    private final SPPDataHandler mDataHandler = new SPPDataHandler()
    {
	@Override
	protected void onLineReceived(String receivedLine) 
	{
	    handleCommand(receivedLine);
	}
	
	@Override
	protected void onDebugMessage(String msg) 
	{
	    doLog(msg);
	}
	
	@Override
	protected void onDeviceConnected() 
	{
	    mConnection.sendLine("H"); // "help" (request device features) 
	    mConnection.sendLine("H"); // "help" (request device features)
	    setTitleMessage("connected to " + mConnectedDeviceName);
	}
	
	@Override
	protected void onDeviceInfo(Device device) 
	{
	    mConnectedDeviceName = device.getName();
	};
	
	@Override
	protected void onToast(String msg) 
	{
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	    
	}
	
	@Override
	protected void onConnectingDevice() 
	{
	    setTitleMessage("connecting");

	};

	@Override
	protected void onDeviceDisconnected() 
	{
	    setTitleMessage("disconnected");
	    enableControls(false);
	};

	@Override
	protected void onTimeout() 
	{
	    setTitleMessage("timeout");
	};
	
	@Override
	protected void onIdle() 
	{
	    setTitleMessage("idle");	    
	};
	
	@Override
	protected void onServiceConnected() 
	{
	    setTitleMessage("connected service");	    	    
	};
    };

    
    private SPPConnection mConnection = new SPPConnection(mDataHandler)
    {
	@Override
	public void disconnect() 
	{
	    // connection about to terminate, E0 will make uC indicate this via the status LED (flash it 3x)
	    sendLine("\nE0"); 
	    sendLine("\n");
	    super.disconnect();
	}	
    };
    
    
    private String mConnectedDeviceName;

    private void handleCommand(String receivedLine)
    {
	String mSupportedFeatures = "";
	
	if (null!=mLogView)
	{
	    //doLog("RCVD: " + receivedLine);
	}

	// old version sent just a hello
	if (receivedLine.startsWith("HELLO"))
	{
	    mSupportedFeatures = receivedLine.substring(5);		
	    analyzeHelloLine(mSupportedFeatures);
	}

	if (receivedLine.startsWith("H:"))
	{
	    mSupportedFeatures = receivedLine.substring(2);		
	    analyzeHelloLine(mSupportedFeatures);		
	    // Ask arduino to send an update line with current values
	    setCmd("G");				
	}

	if (receivedLine.startsWith("G:"))
	{
	    String getUpdateLine = receivedLine.substring(2);
	    analyzeUpdateLine(getUpdateLine);
	}

    }


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
