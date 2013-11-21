package com.apdlv.ilibaba.frotect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.bt.SPPConnection;
import com.apdlv.ilibaba.bt.SPPDataHandler;
import com.apdlv.ilibaba.bt.SPPDataHandler.Device;
import com.apdlv.ilibaba.bt.SPPDataListener;
import com.apdlv.ilibaba.bt.SPPService;
import com.apdlv.ilibaba.bt.SPPStatusAwareDialog;
import com.apdlv.ilibaba.bt.SPPStatusListener;
import com.apdlv.ilibaba.frotect.FrotectActivity.FrotectBTDataCompleteListener.TYPE;
import com.apdlv.ilibaba.gate.DeviceListActivity;
import com.apdlv.ilibaba.gate.GateControlActivity;
import com.apdlv.ilibaba.util.OnClickAwareDialog;
import com.apdlv.ilibaba.util.OnClickHelper;
import com.apdlv.ilibaba.util.U;


public class FrotectActivity extends Activity implements OnClickListener, OnLongClickListener, OnDismissListener
{
    protected static final String TAG = "FrotectActivity";

    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_CONNECT_DEVICE = 1;

    private TextView mMainTitle;    
    private TextView mSubTitle;    
    private TextView mLogTextView;
    private TextView tvLastPing = null;

    private View buttonTemp, buttonPower, buttonCost, buttonDuty, buttonUpdate; 
    private View buttonConfig, buttonInfo;

    private ImageView imgHeartBeat;    
    private ProgressBar pingProgress = null;


    private String mmSelectedDeviceAddress = "F0:B4:79:07:AE:EE"; // "BuzBekci1"
    //private String mmSelectedName;

    int tableIds[] = { R.id.sensorTable1,  R.id.sensorTable2,  R.id.sensorTable3,  R.id.sensorTable4,  R.id.sensorTable5 };
    int iconId[]   = { R.id.sensorIcon1,   R.id.sensorIcon2,   R.id.sensorIcon3,   R.id.sensorIcon4,   R.id.sensorIcon5  };
    int valueId[]  = { R.id.sensorValue1,  R.id.sensorValue2,  R.id.sensorValue3,  R.id.sensorValue4,  R.id.sensorValue5 };
    int limitsId[] = { R.id.sensorMisc1,   R.id.sensorMisc2,   R.id.sensorMisc3,   R.id.sensorMisc4,   R.id.sensorMisc5  }; 
    int miscId[]   = { R.id.sensorLimits1, R.id.sensorLimits2, R.id.sensorLimits3, R.id.sensorLimits4, -1                }; 

    public boolean   mConnected = false;

    private BluetoothAdapter mBluetoothAdapter;
    private Device   mConnectedDevice = null;
    private long startupMillis;

    private final FrotectBTDataHandler mHandler = new FrotectBTDataHandler();

    protected SPPConnection mConnection = new SPPConnection(mHandler)
    {
	@Override
	public void disconnect() 
	{
	    // connection about to terminate, E0 will make uC indicate this via the status LED (flash it 3x)
	    sendLine("\nE0"); 
	    sendLine("\n");
	    super.disconnect();
	};
    };

    private long lastPingLocal;
    private long lastUpdateLocal;

    protected ReceiveBuffer helpBuffer    = new ReceiveBuffer();
    protected ReceiveBuffer infoBuffer    = new ReceiveBuffer();
    protected ReceiveBuffer statsBuffer   = new ReceiveBuffer();
    protected ReceiveBuffer minmaxBuffer  = new ReceiveBuffer();
    protected ReceiveBuffer sensorsBuffer = new ReceiveBuffer();
    protected ReceiveBuffer strandsBuffer = new ReceiveBuffer();

    protected int    numStrands = -1;
    protected int    numSensors = -1;
    protected int    avlStrands = -1;
    protected double numCost    = -1;

    public class Sensor
    {
	// widgets that define and visualize the actual sensors
	public ImageView   icon;
	public TextView    value, limits, misc;

	// recent values read per sensor
	public boolean lit, avail, bound, used, last;
	public double  lo, hi, power, temp;
	public String  addr;
    }

    private Sensor[] sensors = new Sensor[5];

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);

	// Set up the window layout
	requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	setContentView(R.layout.activity_frotect);
	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

	// Set up the custom title
	mMainTitle = (TextView) findViewById(R.id.title_left_text);
	setMainTitle("Frotect");  

	mSubTitle = (TextView) findViewById(R.id.title_right_text);        

	View v = findViewById(R.id.frotectLogTextView);
	mLogTextView = (TextView) v;
	if (null!=mLogTextView)	   
	{
	    mLogTextView.setMaxLines(1000);
	    mLogTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
	    Scroller scroller = new Scroller(mLogTextView.getContext());
	    mLogTextView.setScroller(scroller);	
	}

	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	// If the adapter is null, then Bluetooth is not supported
	if (mBluetoothAdapter == null) 
	{
	    Toast.makeText(this, "WARNING: Bluetooth is not available", Toast.LENGTH_LONG).show();
	}

	buttonTemp   = findViewById(R.id.buttonTemp); 
	buttonPower  = findViewById(R.id.buttonPower); 
	buttonCost   = findViewById(R.id.buttonCost); 
	buttonDuty   = findViewById(R.id.buttonDuty);
	buttonInfo   = findViewById(R.id.frotectButtonInfo);
	buttonUpdate = findViewById(R.id.frotectButtonUpdate);
	buttonConfig = findViewById(R.id.frotectButtonConfig);

	OnClickHelper.registerViews(this, this, buttonTemp, buttonPower,  buttonCost, buttonDuty);
	OnClickHelper.registerViews(this, this, buttonInfo, buttonUpdate, buttonConfig);
	
	OnClickHelper.registerViews(this, this, tableIds);
	OnClickHelper.registerViewsLong(this, this, tableIds);
	
	for (int i=0; i<5; i++)
	{
	    Sensor s = sensors[i] = new Sensor();
	    //s.table  = (TableLayout) findViewById(tableIds[i]);	    
	    s.icon   = (ImageView)   findViewById(iconId[i]);	    
	    s.value  = (TextView)    findViewById(valueId[i]);
	    s.limits = (TextView)    findViewById(limitsId[i]);
	    s.misc   = (TextView)    findViewById(miscId[i]);
	    
	    s.lit    =  false; 
	    s.bound  =  true; // wild guess: last strand/sensor not bound -> updated in updateInfo

	    visualizeSensorStatus(s);
	}
	sensors[4].last = true;

	tvLastPing          = (TextView)    findViewById(R.id.textViewPing);
	imgHeartBeat        = (ImageView)   findViewById(R.id.imgHeartBeat);
	pingProgress = (ProgressBar) findViewById(R.id.frotectPingProgress);
	if (null!=pingProgress) pingProgress.setMax(60);
	customizeProgressbar(pingProgress);

	updateControls();

	Intent intent = new Intent(this, SPPService.class);
	startService(intent);
    }


    @Override
    protected void onStart()
    {
	super.onStart();

	this.startupMillis = Calendar.getInstance().getTimeInMillis();

	if (!mBluetoothAdapter.isEnabled()) 
	{
	    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	    // Otherwise, setup the chat session
	} 

	updateControls();

	Intent intent = new Intent(this, SPPService.class);
	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);	
    }

//    @Override
//    protected void onPause() 
//    {
//	super.onPause();
//	Log.d(TAG, "onPause: Unbinding from service");
//	mConnection.unbind(this);
//    };
//
//    @Override
//    protected void onResume() 
//    {
//	super.onResume();
//	Log.d(TAG, "onResume: Binding to service");
//	Intent intent = new Intent(this, SPPService.class);
//	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);	
//    };


    @Override
    protected void onStop() 
    {
	super.onStop();
	Log.d(TAG, "onStop: Disconnecting from device");
	mConnection.disconnect();
	Log.d(TAG, "onStop: Unbinding from service");
	mConnection.unbind(this);
    };

    @Override
    protected void onDestroy() 
    {
	super.onDestroy();
	Log.d(TAG, "onDestroy: Disconnection from device");
	mConnection.disconnect();	
	Log.d(TAG, "onDestroy: Unbinding from service");
	mConnection.unbind(this);
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.activity_frotect, menu);
	return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) 
    {
	switch (item.getItemId()) 
	{
	case R.id.menu_frotect_reconnect:
	    if (U.isEmpty(mmSelectedDeviceAddress)) return false;

	    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mmSelectedDeviceAddress);
	    mConnection.connect(device); // F0:B4:79:07:AE:EE
	    break;
	case R.id.menu_frotect_next:
	    nextActivity();
	    return true;
	case R.id.menu_frotect_select:
	    // Launch the PresetsActivity to see devices and do scan
	    Intent serverIntent = new Intent(this, DeviceListActivity.class);
	    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	    return true;
	case R.id.menu_frotect_disconnect:
	    mConnection.disconnect();
	    break;

	default:
	    Toast.makeText(getApplicationContext(), "Unknown option " + item.getItemId(), Toast.LENGTH_SHORT).show();
	}
	return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
	switch (requestCode) 
	{

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
	    if (resultCode == Activity.RESULT_OK) 
	    {
		// Get the device MAC address
		String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		
		// Get the BLuetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		mmSelectedDeviceAddress = address;
		//savePeerInfo();

		// Attempt to connect to the device
		mConnection.connect(device);
	    }
	    break;

	}
    }

    public void onClick(View view)
    {  
	onClick(view.getId());
    }

    public boolean onLongClick(View view)
    {
	int n = findClickedSensor(view.getId());	
	if (n<0) return false; // click was not consumed

	if (!strandsBuffer.isComplete()) //if (!mConnected)
	{
	    mHandler.onToast("No sensor info.");
	    return true; // click was consumed
	}

	// not a strand but the extra unbound sensor -> open sensor dialog if connected
	if (n>=4)
	{
	    SensorDialog dialog = new SensorDialog(this);
	    mHandler.addStatusListener(dialog);
	    mHandler.addDataCompleteListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();
	}
	else
	{
	    int strand = n+1;
	    StrandDialog dialog = new StrandDialog(this, strand);
	    mHandler.addStatusListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();
	}
	return false;
    }

    public void onDismiss(DialogInterface dialog)
    {
	if (dialog instanceof SPPDataListener)
	{
	    mHandler.removeDataListener((SPPDataListener)dialog);
	}
	if (dialog instanceof SPPStatusListener)
	{	    
	    mHandler.removeStatusListener((SPPStatusListener)dialog);
	}	    	
	if (dialog instanceof FrotectBTDataCompleteListener)
	{	    
	    mHandler.removeDataCompleteListener((FrotectBTDataCompleteListener)dialog);
	}	    	
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
    {
	System.out.println("onCreateContextMenu");
    }

    private void customizeProgressbar(ProgressBar progressBar)
    {
        Resources res = getResources();
        progressBar.setProgressDrawable(res.getDrawable(R.drawable.frotect_progress));
    }

    private void setMainTitle(String s)
    {
        if (null!=mMainTitle) mMainTitle.setText(s);        	
    }

    private void setTitleMsg(String msg)
    {
        U.setText(mSubTitle, msg);
    }

    private void visualizeSensorStatus(Sensor sensor)
    {
	if (null==sensor) return;

	if (sensor.last)
	{
	    sensor.icon.setImageResource(mConnected && sensor.avail ? R.drawable.blue_thermo_48 : R.drawable.gray_thermo_48);	    
	}
	else
	{
	    if (mConnected)
	    {
		sensor.icon.setImageResource(sensor.lit ? R.drawable.blue_bulb_64 : R.drawable.gray_bulb_64);
	    }
	    else
	    {
		sensor.icon.setImageResource(R.drawable.unkn_bulb_64);
	    }
	}
    }

    private void updateControls()
    {
	boolean connected = mConnected;

	setMainTitle("Frotect");

	U.setText(tvLastPing, connected ? "connected" : "not connected");

	pingProgress.setProgress(0);
	U.setEnabled(pingProgress, connected);

	U.setVisible(imgHeartBeat, connected);
	U.setVisible(buttonUpdate, connected);

	checkDataAvailability();

	for (Sensor s : sensors)
	{
	    visualizeSensorStatus(s);
	}	
    }

    private void checkDataAvailability()
    {
	boolean haveStats  = statsBuffer.isComplete(); 
	boolean haveMinMax = minmaxBuffer.isComplete(); 
	boolean haveInfo   = infoBuffer.isComplete(); 
	boolean haveHelp   = helpBuffer.isComplete(); 

	U.setVisible(buttonInfo,   haveStats || haveMinMax || haveHelp || haveInfo);
	U.setVisible(buttonConfig, haveInfo); // have info? => have values for cost etc.
	
	U.setEnabled(buttonCost,   haveStats);
	U.setEnabled(buttonDuty,   haveStats);
	U.setEnabled(buttonPower,  haveStats);
	U.setEnabled(buttonTemp,   haveMinMax);		
    }

    private void nextActivity()
    {
	Intent i = new Intent(getApplicationContext(), GateControlActivity.class);
	startActivity(i);            
	finish();	
    }

    private void scrollToEnd()  
    {
	if (null==mLogTextView) return;
	Layout layout = mLogTextView.getLayout();
	if (null==layout) return;

	final int scrollAmount = mLogTextView.getLayout().getLineTop(mLogTextView.getLineCount())-mLogTextView.getHeight();
	if (scrollAmount>0)
	{
	    mLogTextView.scrollTo(0, scrollAmount);
	}
	else
	{
	    mLogTextView.scrollTo(0,0);
	}
    }

    private void doLog(String s)
    {
	Log.d(TAG, s);
	if (null==mLogTextView) return;
	mLogTextView.append(s + "\n");
	scrollToEnd();
    }

    private void onClick(int id)
    {
	StatsDialog stDialog;

	switch (id)
	{
	case R.id.frotectButtonInfo:
	    InfoDialog dialog = new InfoDialog(this);
	    mHandler.addDataCompleteListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();
	    break;
	case R.id.frotectButtonUpdate:	
	    if (!mConnected)
	    {
		mHandler.onToast("No connection!");
		return;
	    }
	    mConnection.sendLine("\n\nD\n\n"); // dump all
	    break;
	case R.id.buttonPower:
	    stDialog = new StatsDialog(this, StatsDialog.POWER);
	    mHandler.addDataCompleteListener(stDialog);
	    stDialog.setOnDismissListener(this);
	    stDialog.show();	    
	    break;
	case R.id.buttonTemp:
	    stDialog = new StatsDialog(this, StatsDialog.TEMP);
	    mHandler.addDataCompleteListener(stDialog);
	    stDialog.setOnDismissListener(this);
	    stDialog.show();	    
	    break;
	case R.id.buttonCost:
	    stDialog = new StatsDialog(this, StatsDialog.COST);
	    mHandler.addDataCompleteListener(stDialog);
	    stDialog.setOnDismissListener(this);
	    stDialog.show();	    
	    break;
	case R.id.buttonDuty:
	    stDialog = new StatsDialog(this, StatsDialog.DUTY);
	    mHandler.addDataCompleteListener(stDialog);
	    stDialog.setOnDismissListener(this);
	    stDialog.show();	    
	    break;
	case R.id.frotectButtonConfig:
	    ConfigDialog cDialog = new ConfigDialog(this);
	    mHandler.addStatusListener(cDialog);
	    //mHandler.addDataCompleteListener(cDialog);
	    cDialog.setOnDismissListener(this);
	    cDialog.show();	    
	    break;
	default:
	    int n = findClickedSensor(id);
	    if (-1<n)
	    {
		if (!mConnected)
		{
		    mHandler.onToast("No connection!");
		}
		else if (n<4) //(sensors[n].bound)
		{
		    mConnection.sendLine("I" + (n+1)); // send "identify" command
		}		    		    
	    }
	}
    }

    private int findClickedSensor(int id)
    {
	for (int i=0; i<5; i++)
	{
	    if (tableIds[i]==id || valueId[i]==id || iconId[i]==id)
	    {
		return i;
	    }
	}
	return -1;
    }

    interface FrotectBTDataCompleteListener
    {
	enum TYPE { INFO, HELP, STATS, MINMAX, STARTTIMES, STRANDS }; 

	void onDataComplete(TYPE type, String data);
    }

    class FrotectBTDataHandler extends SPPDataHandler
    {
	private HashSet<FrotectBTDataCompleteListener> dataCompleteListeners = new HashSet<FrotectBTDataCompleteListener>();

	public void addDataCompleteListener(FrotectBTDataCompleteListener l)	
	{
	    dataCompleteListeners.add(l);
	}

	public void removeDataCompleteListener(FrotectBTDataCompleteListener l)	
	{
	    dataCompleteListeners.remove(l);
	}

	private void dispatchDataComplete(FrotectBTDataCompleteListener.TYPE type, String data)
	{
	    for (FrotectBTDataCompleteListener l : dataCompleteListeners)
	    {
		l.onDataComplete(type, data);
	    }
	}

	private String receivedLine = "";

	@Override
	protected void onDataReceived(byte[] readBuf, int len)
	{
	    String payload = new String(readBuf, 0, len);		
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
	}

	@Override
	protected void onToast(String msg)
	{
	    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onDeviceInfo(Device device)
	{
	    mConnectedDevice = device;
	}

	@Override
	protected void onLineReceived(String receivedLine) { handleCommand(receivedLine); }

	@Override
	protected void onIdle() { setTitleMsg("unconnected"); }

	@Override
	protected void onTimeout() { setTitleMsg("timeout"); }

	@Override
	protected void onConnectingDevice() { setTitleMsg("connecting to " + mConnectedDevice.getName()); }

	@Override
	protected void onDeviceConnected() 
	{ 
	    setTitleMsg("connected to " + mConnectedDevice.getName());
	    mConnection.sendLine("\nE1\n"); // connection established
	    mConnection.sendLine("\nS\n");  // dump sensor info 
	    mConnection.sendLine("\nD\n");  // dump all info including strands, min/max and stats
	    mConnection.sendLine("\nH\n");  // request help commands accepted 

	    FrotectActivity.this.mConnected = true;	    
	    updateControls();
	}

	@Override
	protected void onDeviceDisconnected() 
	{ 
	    setTitleMsg("disconnected");

	    FrotectActivity.this.mConnected = false;
	    updateControls();
	}

	@Override
	protected void onDebugMessage(String msg) { doLog(msg); }

	@Override
	protected void onServiceConnected()
	{
	    //Toast.makeText(getApplicationContext(), "Connected to service", Toast.LENGTH_SHORT).show();
	    doLog("Connected to service");
	}

	private boolean hasPrefix(String s, String prefix)
	{
	    return (s.startsWith(prefix+".") || s.startsWith(prefix+":"));
	}

	private void handleCommand(String c)
	{
	    //doLog("LINE: " + c);
	    try
	    {
		if (c.startsWith("P.")) // ping
		{
		    lastPingLocal = Calendar.getInstance().getTimeInMillis()-startupMillis;

		    c = c.replaceAll("^P.*s=", "");
		    //double ms = Double.parseDouble(c);		    		    

		    int progress = (int)(((lastPingLocal-lastUpdateLocal)/1000)%60);
		    //System.out.println("lastUpdateLocal=" + lastUpdateLocal + ", lastPingLocal=" + lastPingLocal + ", progress=" + progress);

		    U.setText(tvLastPing, String.format(Locale.ENGLISH, "%d", progress));

		    if (null!=pingProgress)
		    {
			//			long l   = (long)Math.round(ms);
			//			long div = pingProgress.getMax();
			//int progress = (int)(l%div);


			pingProgress.setProgress(progress);
		    }
		}
		else if (hasPrefix(c, "UP"))
		{
		    lastUpdateLocal = Calendar.getInstance().getTimeInMillis()-startupMillis;
		}
		else if (hasPrefix(c, "HB")) // heartbeat
		{
		    boolean on = c.contains("0"); // inverse logic (open collector connects LED to GND)
		    //setVisibile(imgHeartBeat, on);
		    int id = on ? R.drawable.blue_lheart_64 : R.drawable.blue_dheart_64;
		    if (null!=imgHeartBeat) imgHeartBeat.setImageResource(id);
		}
		else if (hasPrefix(c, "D")) // debug
		{
		    doLog(c);
		}
		else if (hasPrefix(c, "MAGIC"))
		{		
		    logErrorOrWarn("Got magic. uC rebooted?");
		}
		else if (hasPrefix(c, "STH") || hasPrefix(c, "STD") || hasPrefix(c, "STW")) // stats
		{		
		    statsBuffer.append(c);
		}
		else if (c.startsWith("ST!"))
		{
		    dispatchDataComplete(TYPE.STATS, statsBuffer.finish());
		    checkDataAvailability();
		}
		else if (hasPrefix(c, "SEN")) // min/max temp. history
		{
		    onSensorInfo(c);
		}
		else if (c.startsWith("SEN!")) // end of sensors info
		{		
		    dispatchDataComplete(TYPE.STRANDS, sensorsBuffer.finish());
		    checkDataAvailability();
		}
		else if (hasPrefix(c, "MM")) // min/max temp. history
		{		
		    minmaxBuffer.append(c);
		}
		else if (c.startsWith("MM!")) // end of min/max temp. history
		{		
		    dispatchDataComplete(TYPE.MINMAX, minmaxBuffer.finish());
		    checkDataAvailability();
		}
		else if (hasPrefix(c, "EV")) // error
		{
		    onEvent(c);
		}
		else if (hasPrefix(c, "E")) // error
		{		
		    logErrorOrWarn(c);
		}
		else if (hasPrefix(c, "DEACT")) // 
		{		
		    logErrorOrWarn(c);
		}
		else if (hasPrefix(c, "H")) // help
		{		
		    helpBuffer.append(c.replaceAll("^H.", ""));
		}
		else if (c.startsWith("H!")) // end of help
		{		
		    dispatchDataComplete(TYPE.HELP, helpBuffer.finish());
		    checkDataAvailability();
		}
		else if (hasPrefix(c, "WARN")) // warning
		{		
		    logErrorOrWarn(c);
		}
		else if (hasPrefix(c, "CMD")) // help
		{		
		    doLog(c);
		}
		else if (hasPrefix(c, "MEAS")) // MEAS: s=1,now=0.00,old=8.00,avg=8.00
		{		
		    onMeasurement(c);
		}
		else if (hasPrefix(c, "STR")) // strand info 
		{
		    onStrandInfo(c);  
		}
		else if (c.startsWith("STR!")) // end of strand info 
		{		
		    dispatchDataComplete(TYPE.STRANDS, strandsBuffer.finish(c));
		    checkDataAvailability();
		}
		else if (hasPrefix(c, "F")) // flash strand  
		{
		    onFlashInfo(c);  
		}
		else if (hasPrefix(c, "I")) // info, ":" means complete -> update
		{		
		    infoBuffer.append(c.replaceAll("^I.", ""));	    
		}
		else if (c.startsWith("I!"))
		{
		    finalizeInfo();		    
		    checkDataAvailability();
		} 
		else if (hasPrefix(c, "I")) // uptime logs
		{ 
		    doLog(c); 
		}  
		else if (c.startsWith("T!")) // end of uptime logs 
		{
		    // ignore
		} 
	    }
	    catch (Exception e)
	    {
		doLog("handleCommand: " + e);
	    }
	}

	private void onSensorInfo(String c)
	{
	    HashMap<String, Object> map = Parser.parseSensorInfo(c);
	    if (null!=map)
	    {
		int    n      = (Integer) map.get("n");
		String addr   = (String) map.get("@");
		boolean used  = (Boolean) map.get("used");
		boolean avail = (Boolean) map.get("avail");
		Boolean bound = (Boolean) map.get("bnd");

		updateInfo(n, addr, used, avail, bound);
	    }

	    // summarize only if this is a continuation line, not e.g. "SEN. "
	    if (c.startsWith("SEN: "))
	    {
		sensorsBuffer.append(c);
	    }
	}

	private void onEvent(String c)
	{
	    if (c.contains("e=hist.saved"))
	    {
		mConnection.sendLine("DM"); // dump min/max
	    }
	    else if (c.contains("e=stats.saved"))
	    {
		mConnection.sendLine("DH"); // dump hourly stats
		mConnection.sendLine("DD"); // dump daily  stats 
		mConnection.sendLine("DW"); // dump weekly stats
	    }
	    else if (c.contains("e=power."))
	    {
		try
		{
		    String s = c.replaceAll("^.*, *n=", "");
		    int n = Integer.parseInt(s);
		    String onOff = c.contains(".on") ? "on" : "off";		    
		    onToast("Strand " + n +": powered " + onOff);
		}
		catch (Exception x)
		{
		    doLog("onEvent: " + x);
		}		
	    }
	}

	private void finalizeInfo()
	{
	    synchronized(this)
	    {
		String info = infoBuffer.finish();	    

		Map<String, String> map = Parser.parseInfo(info);
		String sV = map.get("Frotect");
		if (!U.isEmpty(sV))
		{
		    setMainTitle(sV.replaceAll(", d=", " "));
		}

		String sM = map.get("NumStrands");
		String sN = map.get("ExpSensors");
		String sA = map.get("AvlSensors");
		String sC = map.get("CostPerkWh");

		numStrands = Integer.parseInt(sM);
		numSensors = Integer.parseInt(sN);
		avlStrands = Integer.parseInt(sA);
		numCost    = Double.parseDouble(sC);

		dispatchDataComplete(TYPE.INFO, info);
	    }
	}

	private void onFlashInfo(String c)
	{
	    try
	    {
		HashMap<String, Integer> map = Parser.parseFlashInfo(c);
		int n = map.get("n")-1;
		Sensor s = sensors[n];
		s.lit = map.get("lit")>0;
		//System.out.println("FLASH: " + n + " -> " + s.lit);
		visualizeSensorStatus(s);
	    }
	    catch (Exception e)
	    {
		logDebug("onFlashInfo: '" + c + "': " + e);
	    }
	}

	private void onStrandInfo(String c)
	{
	    try
	    {
		// STR: n=1, val=0, lit=0, upd=1, tu=28.50, tl=24.00, temp=?, err=50, last=117965829, ago=0, addr=28:50:81:E1:04:00:00:6E, used=1, avail=1
		HashMap<String, Object> map = Parser.parseStrandInfo(c);

		if (null==map)
		{
		    doLog("PARSE ERROR\n");
		}
		else			
		{
		    int     n     = (Integer) map.get("n");
		    @SuppressWarnings("unused")
		    boolean v     = (Boolean) map.get("v");
		    boolean lit   = (Boolean) map.get("lit");
		    @SuppressWarnings("unused")
		    boolean upd   = (Boolean) map.get("upd");
		    double  tu    = (Double)  map.get("tu");
		    double  tl    = (Double)  map.get("tl");
		    Double  t     = (Double)  map.get("t"); // can be null
		    double  power = (Double)  map.get("P");
		    int     err   = (Integer) map.get("err");
		    long    last  = (Long)    map.get("last");
		    boolean used  = (Boolean) map.get("used");
		    boolean avail = (Boolean) map.get("avail");
		    String  addr  = (String)  map.get("@");

		    Boolean bound = (Boolean) map.get("bnd");
		    updateInfo(n, avail, used, lit, tu, tl, t, err, last, power, addr, bound);
		}

		// append only if this is a continuation message,not "STR."
		if (c.startsWith("STR:"))
		{
		    strandsBuffer.append(c);
		}
		doLog(c);
	    }
	    catch (Exception e)
	    {
		doLog("onStrandInfo: " + e);
	    }
	}

	private void onMeasurement(String c)
	{
	    try
	    {
		HashMap<String, Object> map = Parser.parseMeasurement(c);
		
		int strand = (Integer) map.get("s");
		double now = (Double) map.get("now");
		double old = (Double) map.get("old");

		TextView misc = (TextView) findViewById(miscId[strand-1]);
		U.setText(misc, String.format(Locale.ENGLISH, "%2.1f", now));

		updateInfo(strand, now, old);
	    }
	    catch (Exception e)
	    {
		doLog("onMeasurement: " + e);
	    }
	}

	private void logErrorOrWarn(String c)
	{
	    Toast.makeText(getApplicationContext(),c, Toast.LENGTH_LONG).show();
	}

	private void logDebug(String c)
	{
	    Toast.makeText(getApplicationContext(),c, Toast.LENGTH_SHORT).show();
	}

	private void updateInfo(int n, String addr, boolean used, boolean avail, Boolean bound)
	{
	    n=n-1;
	    Sensor sensor = sensors[n];

	    sensor.avail = avail;
	    sensor.used  = used;
	    sensor.addr  = addr;
	    if (!sensor.used)
	    {
		U.setText(sensor.value, "n.a.");
		U.setText(sensor.limits, "");
		U.setText(sensor.misc,   "unbound");
	    }
	    visualizeSensorStatus(sensor);
	}


	private void updateInfo(int n, double t, double last)
	{
	    try
	    {
		Sensor sensor = sensors[n-1];
		String value = String.format(Locale.ENGLISH, "%2.1f¡C", t);
		U.setText(sensor.value, value);	    

		sensor.value.setTextColor(Math.abs(last-t)<0.1 ? Color.WHITE : Color.CYAN);
		sensor.temp = t;
		sensor.used = sensor.avail = true; // obviously, otherwise there was no measure

		visualizeSensorStatus(sensor);
	    }
	    catch (Exception e)
	    {
		System.out.println("updateInfo: " + e);
	    }
	}

	private void updateInfo(int n, boolean avail, boolean used, boolean lit, double tu, double tl, Double t, int err, long last, double power, String addr, Boolean bound2)
	{
	    n=n-1;
	    // wild guess if there is no bound info
	    boolean bound = (null==bound2) ? n<4 : bound2;

	    //	    ImageView sensorBulb  = sensors[n].icon;
	    TextView  value = sensors[n].value;
	    TextView  limits      = sensors[n].limits; 
	    TextView  misc        = sensors[n].misc; 

	    visualizeSensorStatus(sensors[n]);
	    U.setEnabled(value, avail);

	    //	    if (sensorValue instanceof RadioButton)
	    //	    {
	    //		((RadioButton)sensorValue).setChecked(false);
	    //	    }

	    Sensor sensor = sensors[n];
	    sensor.bound = bound;
	    sensor.lit   = lit;
	    sensor.avail = avail;
	    sensor.used  = used;
	    sensor.lo    = tl;
	    sensor.hi    = tu;
	    sensor.power = power;	    
	    sensor.addr  = addr;
	    if (null!=t) sensor.temp = t;

	    if (!sensor.used)
	    {
		U.setText(value,  "n.a.");
		U.setText(limits, "");
		U.setText(misc,   "unbound");
	    }	    
	    else if (null==t)
	    {
		U.setText(value, "?");
	    }
	    else
	    {
		String s = String.format(Locale.ENGLISH, "%2.1f¡C", t);
		U.setText(value, s);
	    }

	    Long now = Calendar.getInstance().getTimeInMillis();		  
	    Long chg = (Long) value.getTag();
	    if (null==chg || now-chg>2000) 
	    {
		U.setTextColor(value, Color.WHITE);
	    }
	    else
	    {
		U.setTextColor(value, Color.YELLOW);
	    }		
	    value.setTag(now);

	    if (!avail)
	    {
		U.setTextColor(value, used ? Color.RED : Color.GRAY);
	    }	    

	    if (!sensor.used || !sensor.bound)
	    {
		U.setText(value, "unbound");
	    }
	    else 
	    {
		U.setText(limits, String.format(Locale.ENGLISH, "%2.1f / %2.1f", tl, tu));
	    }

	    if (err>0)
	    {
		U.setText(misc, "errors: " + err);
	    }
	}
    }

    class NumberPicker implements OnClickListener, OnTouchListener
    {
	private View dec;
	private View inc;
	private TextView value;
	private double min;
	private double max;
	private double step;
	private HoldThread holdThread;
	private Handler handler;

	public NumberPicker(Dialog dialog, int decID, int valueID, int incID, double start, double min, double max)
	{
	    this(dialog, decID, valueID, incID, start, min, max, 0.5f);
	}
	
	private void setListener(View v)
	{
	    if (null!=v) 
	    {
		//v.setOnClickListener(this);
		v.setOnTouchListener(this);
	    }
	}

	public NumberPicker(Dialog dialog, final int decID, int valueID, final int incID, double start, double minVal, double maxVal, double stepVal)
	{
	    setListener(dec = dialog.findViewById(decID));
	    setListener(inc = dialog.findViewById(incID));
	    value = (TextView) dialog.findViewById(valueID);

	    min  = minVal;
	    max  = maxVal;
	    step = stepVal;
	    value.setText(String.format(Locale.ENGLISH, "%-2.1f", start));
	    
	    this.handler = new Handler()
	    {
		public void dispatchMessage(Message msg)
		{
		    int id = msg.arg1;
		    int multiply =  msg.arg2;
		    if (incID==id)
		    {
			incValue(multiply*step);
		    }
		    else if (decID==id)
		    {
			decValue(multiply*step);
		    }
		}
	    };

	    holdThread = new HoldThread(handler);
	    holdThread.start();	    
	}
	
	@Override
	protected void finalize() throws Throwable
	{
	    stop();
	    super.finalize();
	}
		
	public void stop()
	{
	    holdThread.cancel();
	    holdThread.stop();
	    holdThread = null;
	}

	public void onClick(View view)
	{
	    onClick(view.getId());
	}	

	public void onClick(int id)
	{
	    if (id==dec.getId())
	    {
		decValue(step);
	    }
	    else if (id==inc.getId())
	    {
		incValue(step);
	    }
	}
	
	private void incValue(double amount)
	{
	    double d = getValue();
	    d = Math.min(max, d+amount);
	    value.setText(String.format(Locale.ENGLISH, "%-2.1f", d));
	}

	private void decValue(double amount)
	{
	    double d = getValue();
	    d = Math.max(min, d-amount);
	    value.setText(String.format(Locale.ENGLISH, "%-2.1f", d));
	}

	public double getValue()
	{
	    String s = ""+value.getText();
	    s = s.replaceAll(",",".");
	    double d = Double.parseDouble(s);
	    return d;
	}

	public void setEnabled(boolean e)
	{
	    dec.setEnabled(e);
	    inc.setEnabled(e);
	}
	
	final int WHAT_VIEW = 1; 
		
	private class HoldThread extends Thread
	{
	    private volatile int     viewId    = -1;
	    private volatile boolean cancelled = false;
	    private Handler handler;

	    public HoldThread(Handler handler)
	    {
		this.viewId = -1;
		this.handler = handler;
	    }
	    
	    @Override
	    public void run()
	    {
		while (!cancelled)
		{
		    while (viewId<0) doSleep(10);
		    int multiply = 2;
		    while (viewId>-1)
		    {	
			handler.obtainMessage(WHAT_VIEW, viewId, multiply/2).sendToTarget();
			doSleep(250);
			multiply++;
		    }
		}
	    }
	    
	    public void cancel()
	    {
		cancelled = true;
		interrupt();
	    }
	    
	    private void doSleep(long ms)	    
	    {
		try { Thread.sleep(ms); } catch (InterruptedException e) {}
	    }

	    public void setView(int id)
            {
		this.viewId = id;
            }
	}
	

	public boolean onTouch(View view, MotionEvent ev)
        {
	    int  action = ev.getAction();
	    System.out.println("onTouch: action=" + action);

	    switch (action)
	    {
	    case MotionEvent.ACTION_DOWN:
		System.out.println("Setting viewId in holdThread");
		holdThread.setView(view.getId());
		break;
	    case MotionEvent.ACTION_UP:
		System.out.println("Unsetting viewId in holdThread");
		holdThread.setView(-1);
		break;		
	    }	    
	    return true;
        }
    }

    class SensorDialog extends SPPStatusAwareDialog implements FrotectBTDataCompleteListener
    {
	private int sensorAddrIDs[]   = { R.id.sensorAddr1,   R.id.sensorAddr2,   R.id.sensorAddr3,   R.id.sensorAddr4,   R.id.sensorAddr5 };
	private int sensorUpIDs[]     = { R.id.sensorUp1,     R.id.sensorUp2,     R.id.sensorUp3,     R.id.sensorUp4,     R.id.sensorUp5   };
	private int sensorDownIDs[]   = { R.id.sensorDown1,   R.id.sensorDown2,   R.id.sensorDown3,   R.id.sensorDown4,   R.id.sensorDown5 };
	private int sensorBoundsIDs[] = { R.id.tvSensorBound1, R.id.tvSensorBound2, R.id.tvSensorBound3, R.id.tvSensorBound4, R.id.tvSensorBound5 }; 

	AddrInfo infos[] = { new AddrInfo(), new AddrInfo(), new AddrInfo(), new AddrInfo(), new AddrInfo(), };

	private FrotectActivity frotect; 

	class AddrInfo 
	{
	    View     up, down, bound;
	    TextView addr;
	    boolean  avail;

	    void setFound(int i, boolean avail)
	    {
		if (null!=up)
		{
		    up.setVisibility(i>0 ? View.VISIBLE : View.INVISIBLE);
		}
		if (null!=addr)
		{
		    addr.setVisibility(View.VISIBLE);
		}		
		if (i>0 && null!=infos[i-1].down)
		{
		    infos[i-1].down.setVisibility(View.VISIBLE);
		}		
	    }

	    void setAvailable(boolean avail)
	    {
		int v = avail ? View.VISIBLE : View.INVISIBLE; 
		if (null!=up)
		{
		    up.setVisibility(v);
		}
		if (null!=addr)
		{
		    addr.setVisibility(v);
		}		
		if (null!=down)
		{
		    down.setVisibility(v);
		}
	    }
	};

	protected SensorDialog(FrotectActivity frotect)
	{
	    super(frotect);
	    this.frotect = frotect;

	    setTitle("Sensors");
	    LayoutInflater inflater = this.getLayoutInflater();
	    setContentView(inflater.inflate(R.layout.dialog_sensors, null));

	    for (int i=0; i<5; i++)
	    {
		infos[i].up    = findViewById(sensorUpIDs[i]);		
		infos[i].down  = findViewById(sensorDownIDs[i]);		
		infos[i].addr  = (TextView) findViewById(sensorAddrIDs[i]);		
		infos[i].bound = findViewById(sensorBoundsIDs[i]);		
		infos[i].setAvailable(false);		
	    }

	    registerViews(false, sensorAddrIDs);
	    registerViews(false, sensorUpIDs);
	    registerViews(false, sensorDownIDs);
	    registerViews(false, R.id.frotectRescan);

	    // make sure the views show the appropriate state
	    setStatus(mConnected);
	}

	@Override
	protected void onStart() 
	{
	    super.onStart();	    

	    // make sure we will receive sensor info updates after we modified the order of sensors
	    mHandler.addDataCompleteListener(this);
	    mHandler.addStatusListener(this);

	    // if sensor data was already collected, uses this information
	    synchronized (frotect)
	    {
		if (frotect.sensorsBuffer.isComplete())
		{
		    parseStrandInfo(frotect.sensorsBuffer.toString());
		}
		else
		{
		    // request uC to send sensor info
		    mConnection.sendLine("\nS\n"); // dump sensor info
		    mConnection.sendLine("\nS\n"); // dump sensor info		    
		}
	    }
	};

	@Override
	protected void onStop()
	{
	    super.onStop();
	    mHandler.removeStatusListener(this);
	    mHandler.removeDataCompleteListener(this);
	}


	private void rescanConfirmed()
	{
	    mConnection.sendLine("\nX\n"); // Rescan sensors command
	}

	public void confirmRescan()
	{
	    AlertDialog.Builder builder = new AlertDialog.Builder(frotect);
	    builder.setMessage("Really rescan sensors?").setTitle("Confirmation");
	    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { rescanConfirmed(); }});
	    builder.setNegativeButton("No",  new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) {  /* do nothing */  }});
	    builder.create().show();
	}

	// interface FrotectBTDataCompleteListener	
	public void onDataComplete(TYPE type, String data)
	{
	    if (FrotectBTDataCompleteListener.TYPE.STRANDS==type) 
	    {
		parseStrandInfo(data);
	    }
	}

	private void parseStrandInfo(String data)
	{
	    StringReader sr = new StringReader(data);
	    BufferedReader br = new BufferedReader(sr);

	    String line = null;
	    do
	    {
		try { line = br.readLine(); } catch (IOException e) {}
		if (!U.isEmpty(line)) try
		{
		    HashMap<String, Object> map = Parser.parseSensorInfo(line);

		    int     num   = (Integer) map.get("n");
		    boolean avail = (Boolean) map.get("avail");
		    String  addr  = (String)  map.get("@");
		    Boolean bound = (Boolean) map.get("bnd");

		    AddrInfo info = infos[num-1];
		    info.addr.setText(addr);
		    U.setVisible(info.bound, null!=bound && bound);

		    info.setFound(num-1, avail);
		}
		catch (Exception e)
		{
		    frotect.doLog("parseOneLine: " + e + " on '" + line + "'");
		}
	    }
	    while (null!=line);
	}

	@Override
	public void onClick(int id)
	{
	    if (R.id.frotectRescan==id) 
	    { 
		confirmRescan();  
		return;  
	    }

	    for (int i=0; i<5; i++)
	    {
		if (sensorUpIDs[i]==id)   
		{ 
		    mConnection.sendLine("\n-" + (i+1) + "\n"); // Move up command 
		    break; 
		}  
		else if (sensorDownIDs[i]==id) 
		{ 
		    mConnection.sendLine("\n+" + (i+1) + "\n"); // Move down command 
		    break; 
		}
	    }	    
	}
    }

    class StrandDialog extends SPPStatusAwareDialog implements FrotectBTDataCompleteListener, SPPDataListener, android.view.View.OnClickListener
    {
	private int strandNo;
	private FrotectActivity frotect;
	private NumberPicker lo, hi, pwr;
	private TextView strandAddress;
	private boolean bound;

	protected StrandDialog(FrotectActivity frotect, int strandNo)
	{
	    super(frotect);
	    this.frotect = frotect;
	    this.strandNo = strandNo;
	    LayoutInflater inflater = this.getLayoutInflater();
	    setContentView(inflater.inflate(R.layout.dialog_strand, null));
	    setTitle("Strand " + strandNo);

	    this.strandAddress = (TextView) findViewById(R.id.frotectStrandAddr);
	}

	@Override
	public void show()
	{
	    super.show();
	    refreshControls();
	}

	private void refreshControls()
	{
	    Sensor info = sensors[strandNo-1];	    
	    double currLo    = -30;
	    double currHi    =  30;
	    double currPower =   0;
	    if (info.avail)
	    {
		currLo    = info.lo;
		currHi    = info.hi;
		currPower = info.power;
	    }

	    U.setText(strandAddress, info.addr);

	    // register views to be disabled when there is no connection
	    registerViews(false, R.id.buttonThresSave, R.id.buttonStrandFlash, R.id.buttonStrandBind, R.id.buttonStrandUnbind);
	    
	    // same as above, however to not capture onClick events to make sure the NumberPickers will receive them
	    registerViews(false, false, R.id.decLo,  R.id.incLo, R.id.decHi, R.id.incHi, R.id.decPower, R.id.incPower);     
	    setStatus(mConnected);	    

	    lo  = new NumberPicker(this, R.id.decLo,    R.id.valueLo,    R.id.incLo,    currLo, -30, 30); 
	    hi  = new NumberPicker(this, R.id.decHi,    R.id.valueHi,    R.id.incHi,    currHi, -30, 30); 
	    pwr = new NumberPicker(this, R.id.decPower, R.id.valuePower, R.id.incPower, currPower, 0, 200); 
	    bound = info.bound;

	    U.setInvisibile(findViewById(bound ? R.id.layoutStrandBind : R.id.layoutStrandUnbind));
	}

	@Override
	protected void onStop() 
	{
	    lo.stop();
	    hi.stop();
	    pwr.stop();
	    super.onStop();
	};
	
	@Override
	public void onConnect(Device device)
	{
	    super.onConnect(device);
	    U.setInvisibile(findViewById(bound ? R.id.layoutStrandBind : R.id.layoutStrandUnbind));
	}

	public void onDataComplete(TYPE type, String data)
	{
	    if (type==TYPE.STRANDS) refreshControls();
	}

	public void onLineReceived(String line)
	{
	    if (line.startsWith("STR"))	refreshControls();
	}

	@Override
	public void onClick(int id)
	{
	    switch (id)
	    {
	    case R.id.buttonThresSave:
		String cmd1 = String.format(Locale.ENGLISH, "L%d=%2.1f", strandNo, lo.getValue());
		String cmd2 = String.format(Locale.ENGLISH, "U%d=%2.1f", strandNo, hi.getValue());
		String cmd3 = String.format(Locale.ENGLISH, "P%d=%2.1f", strandNo, pwr.getValue());
		frotect.mConnection.sendLine(cmd1);
		frotect.mConnection.sendLine(cmd2);
		frotect.mConnection.sendLine(cmd3);
		//dismiss();
		break;
	    case R.id.buttonStrandFlash:
		frotect.mConnection.sendLine("I" + strandNo);
		break;
	    case R.id.buttonStrandBind:
		frotect.mConnection.sendLine("\nG" + strandNo);  // re-bind command
		frotect.mConnection.sendLine("\nS\n" + strandNo);
		break;
	    case R.id.buttonStrandUnbind:
		frotect.mConnection.sendLine("\nF" + strandNo);
		frotect.mConnection.sendLine("\nS\n" + strandNo);
		break;
	    }
	}
    }

    class ConfigDialog extends SPPStatusAwareDialog 
    {
	private NumberPicker    cost, strands, sensors;
	private FrotectActivity frotect;

	protected ConfigDialog(FrotectActivity frotect)
	{
	    super(frotect);

	    this.frotect = frotect;
	    LayoutInflater inflater = this.getLayoutInflater();
	    setContentView(inflater.inflate(R.layout.dialog_config, null));	    
	    setTitle("Config ");

	    cost    = new NumberPicker(this, R.id.decCost,       R.id.valueCost,       R.id.incCost,       20, 0, 100, 0.25);
	    strands = new NumberPicker(this, R.id.decNumStrands, R.id.valueNumStrands, R.id.incNumStrands,  4, 1,   4, 1.00);
	    sensors = new NumberPicker(this, R.id.decNumSensors, R.id.valueNumSensors, R.id.incNumSensors,  4, 1,   4, 1.00);

	    // register these to be hidden when unconnected:
	    registerViews(R.id.saveCost, R.id.saveNumStrands, R.id.saveNumSensors);
	    
	    // only disable however this viewId when unconnected:
	    registerViews(false, R.id.frotectReboot);

	    frotect.mHandler.addStatusListener(this);
	}
	
	@Override
	protected void onStop()
	{
	    cost.stop();
	    strands.stop();
	    sensors.stop();
	    super.onStop();
	}

	public void onClick(int id)
	{
	    switch (id)
	    {
	    case R.id.saveCost:
		// C0.2    set cost/kWh to 0.2
		frotect.mConnection.sendLine("\nC" + cost.getValue());
		break;	    	
	    case R.id.saveNumStrands:
		// M3      set # of strands to 3
		frotect.mConnection.sendLine("\nM" + ((int)strands.getValue()));		
		break;
	    case R.id.saveNumSensors:
		// N4      set # of sensors to 4
		frotect.mConnection.sendLine("\nN" + ((int)sensors.getValue()));
		break;	    
	    case R.id.frotectReboot:
		confirmReboot();
		break;
	    }
	}

	private void rebootConfirmed()
	{
	    frotect.mConnection.sendLine("\nB=4711\n");				
	}

	public void confirmReboot()
	{
	    AlertDialog.Builder builder = new AlertDialog.Builder(frotect);
	    builder.setMessage("Really reboot µC?").setTitle("Confirmation");
	    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { rebootConfirmed(); }});
	    builder.setNegativeButton("No",  new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) {  /* do nothing */  }});	    
	    builder.create().show();
	}
    }

    class InfoDialog extends OnClickAwareDialog implements FrotectBTDataCompleteListener
    {
	private FrotectActivity frotect;
	private TextView tvInfo;
	private FrotectBTDataCompleteListener.TYPE mType = TYPE.INFO;
	//private ContextMenu menu;
	//private boolean autoscroll;

	protected InfoDialog(FrotectActivity frotect)
	{
	    super(frotect);
	    this.frotect = frotect;
	    LayoutInflater inflater = this.getLayoutInflater();
	    setContentView(inflater.inflate(R.layout.dialog_info, null));
	    setTitle("Info ");	    
	}
	
	/*
	private static final int MENU_ITEM_DISMISS         = Menu.FIRST + 0;
	private static final int MENU_ITEM_SEND_LOG        = Menu.FIRST + 1;
	private static final int MENU_ITEM_AUTOSCROLL      = Menu.FIRST + 2;
	private static final int MENU_ITEM_SCROLL_TO_TOP   = Menu.FIRST + 3;
	private static final int MENU_ITEM_SCROLL_TO_END   = Menu.FIRST + 4;


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
	{
	    super.onCreateContextMenu(menu, v, menuInfo);
	    this.menu = menu;
	    menu.setHeaderTitle("Info");
	    int n=0;
	    menu.add(0, MENU_ITEM_DISMISS,        n++, "Dismiss");
	    menu.add(0, MENU_ITEM_SEND_LOG,       n++, "Send log");
	    menu.add(0, MENU_ITEM_AUTOSCROLL,     n++, this.autoscroll ? "Disable autoscroll" : "Enable autoscroll");
	    menu.add(0, MENU_ITEM_SCROLL_TO_TOP,  n++, "Scroll to top");
	    menu.add(0, MENU_ITEM_SCROLL_TO_END,  n++, "Scroll to end");
	}

	private void scrollToTop() 
	{
	    tvInfo.scrollTo(0,0);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) 
	{
	    switch (item.getItemId()) 
	    {
	    case MENU_ITEM_DISMISS:
		break;
	    case MENU_ITEM_SEND_LOG:
		sendLog();
		break;
	    case MENU_ITEM_AUTOSCROLL:
		this.autoscroll = !this.autoscroll;
		item.setTitle(this.autoscroll ? "Autoscroll enabled" : "Autoscroll disabled");
		break;
	    case MENU_ITEM_SCROLL_TO_TOP:
		scrollToTop();
		break;
	    case MENU_ITEM_SCROLL_TO_END:
		scrollToEnd();
		break;
	    }
	    return super.onContextItemSelected(item);	    
	}
	 */
	private void sendLog()
	{
	    Toast.makeText(frotect, "Send info ...", Toast.LENGTH_SHORT).show();
	    Log.d(TAG, "Sending info text\n");

	    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
	    String[] recipients = new String[]{"a.pogoda@venista.com", };

	    String body = "" + tvInfo.getText();

	    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,   recipients);
	    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Frotect " + mType);
	    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,    body);
	    emailIntent.setType("text/plain");
	    startActivity(Intent.createChooser(emailIntent, "Send info"));
	    //finish();		
	}

	/*	
	private void scrollToEnd()
	{
	    final int scrollAmount = tvInfo.getLayout().getLineTop(tvInfo.getLineCount())-tvInfo.getHeight();
	    if (scrollAmount>0)
	    {
		tvInfo.scrollTo(0, scrollAmount);
	    }
	    else
	    {
		tvInfo.scrollTo(0,0);
	    }
	}
	 */

	@Override
	public void show()
	{
	    super.show();

	    registerViews(R.id.buttonInfoHelp, R.id.buttonInfoGeneral, R.id.buttonInfoMinMax, R.id.buttonInfoStats, R.id.buttonSendInfoText);

	    tvInfo = (TextView) findViewById(R.id.frotectInfoView);
	    U.setText(tvInfo, frotect.infoBuffer.toString());
	    mType = TYPE.INFO;
	}

	// InfoDialog.onDataComplete(...) will be called by FrotectBTDataHandler whenever there is new data available
	public void onDataComplete(TYPE type, String data)
	{
	    if (type==mType)
	    {
		U.setText(tvInfo, data);
	    }	    
	}

	@Override
	public void onClick(int id)
	{
	    switch (id)
	    {
	    case R.id.buttonInfoHelp:
		tvInfo.setText("" + frotect.helpBuffer.toString());
		mType = TYPE.HELP;
		break;
	    case R.id.buttonInfoGeneral:
		tvInfo.setText("" + frotect.infoBuffer.toString());
		mType = TYPE.INFO;
		break;
	    case R.id.buttonInfoMinMax:
		tvInfo.setText("" + frotect.minmaxBuffer.toString());
		mType = TYPE.MINMAX;
		break;
	    case R.id.buttonInfoStats:
		tvInfo.setText("" + frotect.statsBuffer.toString());
		mType = TYPE.STATS;
		break;
	    case R.id.buttonSendInfoText:
		sendLog();
		break;
	    }
	}
    }

}


