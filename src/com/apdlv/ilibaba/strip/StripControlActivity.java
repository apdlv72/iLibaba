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
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Scroller;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.activities.DeviceListActivity;
import com.apdlv.ilibaba.bt.SPPConnection;
import com.apdlv.ilibaba.bt.SPPDataHandler;
import com.apdlv.ilibaba.bt.SPPService;
import com.apdlv.ilibaba.color.HSVColorWheel;
import com.apdlv.ilibaba.color.OnColorSelectedListener;
import com.apdlv.ilibaba.frotect.FrotectActivity;
import com.apdlv.ilibaba.gate.GateControlActivity;
import com.apdlv.ilibaba.shake.Shaker.Callback;
import com.apdlv.ilibaba.util.LastStatusHelper;
import com.apdlv.ilibaba.util.U;

public class StripControlActivity extends Activity implements OnSeekBarChangeListener, Callback, OnColorSelectedListener, /*OnItemSelectedListener,*/ OnClickListener
{
    private TextView mTextCommand;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mTitle;

    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PRESET    = 3;
    private static final int REQUEST_CONNECT_DEVICE = 1;

    private TextView mLogView;
    private HSVColorWheel mColorWheel;
    private Vibrator mVibrator;
    private long activityStarted;
    private Button mButtonModes;
    private Button mButtonLamps;
    private Button mButtonPresets;

    private String mmSelectedAddress;
    private String mmSelectedName;

    final static boolean D = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);	
        if (D) Log.d(TAG, "+++ ON CREATE +++");

        if (LastStatusHelper.startLastActivity(this))
        {
            return; // a different activity is about to be started
        }
        
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
	
	(mButtonModes   = (Button) findViewById(R.id.button_modes)  ).setOnClickListener(this);
	(mButtonLamps   = (Button) findViewById(R.id.button_lamps)  ).setOnClickListener(this);
	(mButtonPresets = (Button) findViewById(R.id.button_presets)).setOnClickListener(this);
	
	((SeekBar) findViewById(R.id.seekBright)   ).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekAmplitude)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekSpeed)    ).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekFade)     ).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekStrength) ).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekRand)     ).setOnSeekBarChangeListener(this);

	enableControls(false);

	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	// If the adapter is null, then Bluetooth is not supported
	if (mBluetoothAdapter == null) {
	    Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
	    finish();
	    return;
	}
	
	Intent intent = new Intent(this, SPPService.class);
	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);	        
	
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
		mConnection.connect(device);
	    }
	    break;
	case R.id.menu_water_next:
	    nextActivity();
	    return true;
	    
	case R.id.menu_water_select:
	    // Launch the PresetsActivity to see devices and do scan
	    Intent serverIntent = new Intent(this, DeviceListActivity.class);
	    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	    return true;
	case R.id.menu_water_disconnect:
	    mConnection.disconnect();
	    break;
	default:
	    Toast.makeText(getApplicationContext(), "Unknown option " + item.getItemId(), Toast.LENGTH_SHORT).show();
	}
	return false;
    }


    private void nextActivity()
    {
	LastStatusHelper.startActivity(this, FrotectActivity.class);
//	Intent i = new Intent(getApplicationContext(), FrotectActivity.class);
//	startActivity(i);            
//	finish();	
    }


    public void onProgressChanged(SeekBar s, int progress, boolean fromUser)
    {
	String hexProgress = String.format("%x", progress);
	String command = null==s.getTag() ? null : "" + s.getTag() + "=" + hexProgress; //s.getProgress();	
	if (null!=command)
	{
	    if (fromUser)
	    {
		// use the tag from the respective scale and send as command prefix
		setCmd(command);
	    }
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
	mConnection.disconnect();
	mConnection.unbind(this);
    };

    @Override
    protected void onDestroy() 
    {
	super.onDestroy();
	mConnection.disconnect();
    };

    @Override
    protected void onPause() 
    {
	super.onPause();
    };

    void setCmd(String s)
    {
	mTextCommand.setText(s);
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

		mConnection.connect(device);
	    }
	    break;

	}
    }
    
    private String getCurrentModeName()    
    {
	try
	{
	    return mAvailableModes[mSelectedModeIndex];
	}
	catch (Exception e)
	{
	    return "Mode " + mSelectedModeIndex;
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
		//mSpinnerMode.setSelection(mode);
		mButtonModes.setText(getCurrentModeName());
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

	    int mode  = mSelectedModeIndex; //  mSpinnerMode.getSelectedItemPosition();
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

    private Integer mLastSelectedColor = null;
    
    // interface OnColorSelectedListener
    public void colorSelected(Integer color)
    {
	// need to cast if comparing Integers not ints.
	if (null==mLastSelectedColor)
	{
	    setCmd(String.format("C=%06x", color & 0xFFFFFF));
	}
	else
	{
	    int a = mLastSelectedColor;
	    int b = color;
	    if (a!=b)
	    {
		setCmd(String.format("C=%06x", color & 0xFFFFFF));
	    }
	}
	mLastSelectedColor = color;
    }

    
    public void onNothingSelected(AdapterView<?> arg0)
    {
    }

    private void setTitleMessage(String s)
    {
	mTitle.setText(s);
    }

//    public void onBTDataReceived(byte[] data, int len)
//    {
//	String s = new String(data, 0, len);
//	Log.d(TAG, "onBTDataReceived: '" + s + "'");
//    }

    private static final String TAG = "WaterStrip";


    private int visibleIfFeatured(FEATURE f)
    {
	return supportedFeatures.contains(f) ? View.VISIBLE : View.INVISIBLE;
    }
    

    private void enableControls(boolean on)
    {
	if (on)
	{
	    ((SeekBar) findViewById(R.id.seekBright)   ).setVisibility(visibleIfFeatured(FEATURE.BRIGHTNESS));
	    ((SeekBar) findViewById(R.id.seekFade)     ).setVisibility(visibleIfFeatured(FEATURE.FADING));
	    ((SeekBar) findViewById(R.id.seekSpeed)    ).setVisibility(visibleIfFeatured(FEATURE.SPEED));
	    ((SeekBar) findViewById(R.id.seekAmplitude)).setVisibility(visibleIfFeatured(FEATURE.AMPLITUDE));
	    ((SeekBar) findViewById(R.id.seekRand)     ).setVisibility(visibleIfFeatured(FEATURE.RANDOMNESS));
	    ((SeekBar) findViewById(R.id.seekStrength) ).setVisibility(visibleIfFeatured(FEATURE.STRENGTH));
	    mColorWheel.setEnabled(true);
	    //mSpinnerMode.setEnabled(true);
	    U.setEnabled(mButtonModes,   true); 
	    U.setEnabled(mButtonLamps,   true);
	    U.setEnabled(mButtonPresets, true);
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
	    //U.setEnabled(mSpinnerMode, false);
	    U.setEnabled(mButtonModes,   false);
	    U.setEnabled(mButtonLamps,   false);
	    U.setEnabled(mButtonPresets, false);
	}
    }

    
    private void parseStripModes(String modeStr)
    {
	String[] parts = modeStr.split("[\\t ]+");
	
	mAvailableModes = new String[parts.length];
	
	int i = 0;
	for (String name : parts)
	{
	    name = name.replaceAll("^[0-9]+=", "").replaceAll("_", " ");
	    mAvailableModes[i++] = name;
	}
    }
    
    private void analyzeUpdateLine(String line)
    {
	doLog("GOT Update LINE: " + line);
	
	Integer ampli  = extractUpdateValue(line, "A");
	Integer brite  = extractUpdateValue(line, "B");
	Integer color  = extractUpdateValue(line, "C"); 
	Integer fade   = extractUpdateValue(line, "F");
	Integer mode   = extractUpdateValue(line, "M"); 
	Integer random = extractUpdateValue(line, "R");
	Integer speed  = extractUpdateValue(line, "S");
	Integer stren  = extractUpdateValue(line, "T");
	
	if (null!=mode)  mSelectedModeIndex = mode;	
	if (null!=color) mColorWheel.setColor(color);

	setSeekBar(R.id.seekBright,    brite);
	setSeekBar(R.id.seekAmplitude, ampli);
	setSeekBar(R.id.seekRand,      random);
	setSeekBar(R.id.seekSpeed,     speed);
	setSeekBar(R.id.seekStrength,  stren);
	setSeekBar(R.id.seekFade,      fade);
    }

    
    private void setSeekBar(int resID, Integer value)
    {
	if (null==value) return;
	SeekBar sb = (SeekBar) findViewById(resID);
	if (null==sb) return;
	sb.setProgress(value);
    }
    
    private static Integer parseInt(String s, int radix)
    {
	try
	{
	    return Integer.parseInt(s, radix);
	}
	catch (Exception e)
	{
	    return null;
	}
    }

    private static Integer extractUpdateValue(String line, String key)
    {
	String value  = line.replaceAll("^.*" + key + "=", "").replaceAll("[ ,].*","");
	if (value.equals(line)) return null;
	return parseInt(value, 16);
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
	this.mAvailableLamps = lamps;
    }
    
    
    void setPresets(String ... presets)
    {
	this.mAvailablePresets = presets;
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
		    // TODO: Parse also the presets from the devices capability line.
		    setPresets("OnChip 1", "OnChip 2", "OnChip 3", "OnChip 4", "OnChip 5", "OnChip 6", "OnMobile A", "OnMobile B");
		}		
		else if ("LPD6803".equalsIgnoreCase(kind))
		{
		    // H:V=1 K=LPD6803 P=0-2 B=0-FF A=0-FF S=0-FFF F=0-FFF R=0-FF T=0-FFF C=0-FFFFFF M=0=Water	1=WaterColored	2=Fading 3=Rainbow 4=RainbowLong 5=RainbowShort 6=Constant 7=RainFlow 8=RainFlowShort	
		    //setStripModes("Water1", "Water2", "Rainbow1", "Rainbow2", "Rainbow3",  "Fading1", "Fading2", "Funky");
		    setLamps("one");
		    // TODO: Parse also the presets from the devices capability line.
		    setPresets("OnChip 1", "OnChip 2", "OnChip 3", "OnChip 4", "OnChip 5", "OnChip 6", "OnMobile A", "OnMobile B", "OnMobile C");
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

		    // TODO: Parse also the presets from the devices capability line.
		    setPresets("Bright", "Dim", "Eat", "Party", "TV", "Absent");
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
	    mConnection.sendLine("E1");
	    mConnection.sendLine("");
	    mConnection.sendLine("H"); // "help" (request device features) 
	    mConnection.sendLine("");
	    mConnection.sendLine("H"); // "help" (request device features) 
	    mConnection.sendLine("");
	    setTitleMessage("connected to " + mConnectedDeviceName);
	    mLastSelectedColor = null;
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
	    mLastSelectedColor = null;
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
	    sendLine("\n");
	    sendLine("E0"); 
	    sendLine("\n");
	    super.disconnect();
	}	
    };
    
    
    private String mConnectedDeviceName;
    private int    mSelectedModeIndex = 0;
    
    private String[] mAvailableModes   = { "None"    };
    private String[] mAvailableLamps   = { "Default" };
    private String[] mAvailablePresets = { "Preset 1", "Preset 2", "Preset 3", "Preset 4", "Preset 5", "Preset 6" };

    private void handleCommand(String line)
    {
	String mSupportedFeatures = "";
	
	if (null!=mLogView)
	{
	    //doLog("RCVD: " + receivedLine);
	}

	if (null==line)
	{
	    return;
	}
	
	// old version sent just a hello
	if (line.startsWith("HELLO"))
	{
	    mSupportedFeatures = line.substring(5);		
	    analyzeHelloLine(mSupportedFeatures);
	}

	if (line.startsWith("H:"))
	{
	    mSupportedFeatures = line.substring(2);		
	    analyzeHelloLine(mSupportedFeatures);		
	    // Ask arduino to send an update line with current values
	    setCmd("");				
	    setCmd("G");				
	    setCmd("");				
	    setCmd("G");				
	}

	if (line.startsWith("G:"))
	{
	    String getUpdateLine = line.substring(2);
	    analyzeUpdateLine(getUpdateLine);
	}
	
	if (line.startsWith("D"))
	{	    
	    if (line.startsWith("D:LINE:''"))
	    {
		// ignore echoed by the uC		
	    }
	    else if (line.startsWith("D:LINE:"))
	    {
		doLog(line.replace("D:LINE:", "CMD: "));
	    }
	    else if (line.contains("CHG_SCHED"))
	    {
		//showToast("Saving scheduled");
	    }
	    else if (line.contains("CHG_SAV"))
	    {
		showToast("Changes saved");
	    }
	    else
	    {
		doLog(line);
	    }
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


    public void onClick(View v)
    {	
	if (v==mButtonModes)
	{
	    Dialog d = new ModeListDialog(this, this.mAvailableModes);
	    d.show();
	}
	else if (v==mButtonLamps)
	{
	    Dialog d = new LampListDialog(this, this.mAvailableLamps);
	    d.show();	    
	}
	else if (v==mButtonPresets)
	{
	    Dialog d = new PresetListDialog(this, this.mAvailablePresets);
	    d.show();	    
	}
    }


    public void setSelectedMode(int selectedIndex, String name)
    {
	mSelectedModeIndex = selectedIndex;
	// don't modify the button's text ... it may be very long an break the layout
	//mButtonModes.setText(name);	
	setCmd("M=" + selectedIndex);
    }


    private boolean mExitConfirmation=true;

    public boolean onKeyDown(int keyCode, KeyEvent event) 
    {
	//Handle the back button
	if (keyCode == KeyEvent.KEYCODE_BACK && isTaskRoot()) 
	{
	    if (mExitConfirmation)
	    {
		//Ask the user if they want to quit
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Quit")
		.setMessage("Do you want to quit?")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			LastStatusHelper.saveCurrActivity(StripControlActivity.this);
			//Stop the activity
			finish();    
		    }
		})
		.setNegativeButton("No", null)
		.show();
		return true;
	    }
	}

	return super.onKeyDown(keyCode, event);
    }

}
