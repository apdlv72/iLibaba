package com.apdlv.ilibaba.frotect;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.R.id;
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
    private static final String ACK_UPDATE = "update";

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
    int miscId[]   = { R.id.sensorMisc1,   R.id.sensorMisc2,   R.id.sensorMisc3,   R.id.sensorMisc4,   R.id.sensorMisc5  }; 
    int limitsId[] = { R.id.sensorLimits1, R.id.sensorLimits2, R.id.sensorLimits3, R.id.sensorLimits4, -1                }; 

    
    public boolean   mConnected = false;

    private BluetoothAdapter mBluetoothAdapter;
    private Device           mConnectedDevice = null;
    
    private final FrotectBTDataHandler mHandler = new FrotectBTDataHandler();
    
    private Sensor[] sensors = new Sensor[5];    
    private TextView mErrorView;
    
    private static final int VERB_NORM    = 1;
    private static final int VERB_VERBOSE = 2;
    private static final int VERB_DEBUG   = 3;
    
    private long startupMillis;
    private long lastPingLocal;
    private long lastUpdateLocal, lastUpdateRemote;

    protected ReceiveBuffer helpBuffer    = new ReceiveBuffer();
    protected ReceiveBuffer infoBuffer    = new ReceiveBuffer();
    protected ReceiveBuffer statsBuffer   = new ReceiveBuffer();
    protected ReceiveBuffer minmaxBuffer  = new ReceiveBuffer();
    protected ReceiveBuffer sensorsBuffer = new ReceiveBuffer();
    protected ReceiveBuffer strandsBuffer = new ReceiveBuffer();
    protected ReceiveBuffer starttimesBuffer  = new ReceiveBuffer();

    protected Integer numStrands = null;
    protected Integer numSensors = null;
    protected Integer avlStrands = null;
    protected Double  numCost100    = null;

    private int verbosity = VERB_NORM;
    private boolean mWithSound=false;
    private boolean mExitConfirmation=true;;
    private Vibrator mVibrator;    

    
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

    public class Sensor
    {
	// widgets that define and visualize the actual sensors
	public ImageView   icon;
	public TextView    value, limits, misc;

	// recent values read per sensor
	public boolean lit, avail, bound, used, last;
	public double  lo, hi, averageTemp;
	public int power;
	public String  addr;
	public int minutesOn;
	public Double singleTemp;
	
	public Double rampStart;
	public Double rampEnd;
	public Double rampDelta;
	
	public String toString()
	{
	    return "lit=" + lit + ", avail=" + avail + ", used=" + used + ", bound=" + bound + ", last=" + last + 
		   ", avgT=" + averageTemp + ", singleT=" + singleTemp + "minOn=" + minutesOn;
	}
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);

	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	// If the adapter is null, then Bluetooth is not supported
	if (mBluetoothAdapter == null) 
	{
	    Toast.makeText(this, "WARNING: Bluetooth is not available", Toast.LENGTH_LONG).show();
	    finish();
	}

	// Register for broadcasts on BluetoothAdapter state change
	IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
	registerReceiver(mReceiver, filter);
	
	// Enable bluetooth if it's now on already.
	if (!mBluetoothAdapter.isEnabled()) 
	{
	    mBluetoothAdapter.enable();	    
	} 	
	
	mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	    
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

	U.setInvisibile(mErrorView = (TextView) findViewById(R.id.frotectGeneralError));
	
	buttonTemp   = findViewById(R.id.frotectButtonTemp); 
	buttonPower  = findViewById(R.id.frotectButtonPower); 
	buttonCost   = findViewById(R.id.frotectButtonCost); 
	buttonDuty   = findViewById(R.id.frotectButtonDuty);
	
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

	Intent intent = new Intent(this, SPPService.class);
	startService(intent);
    }


    @Override
    protected void onStart()
    {
	super.onStart();

	this.startupMillis = Calendar.getInstance().getTimeInMillis();
	
	// Enable bluetooth if it's now on already.
	if (!mBluetoothAdapter.isEnabled()) 
	{
	    mBluetoothAdapter.enable();	    
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
	Log.d(TAG, "onDestroy: Unregistering bluetooth broadcast receiver");
	unregisterReceiver(mReceiver);
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.activity_frotect, menu);
	return true;
    }

    
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
	U.setEnabled(mBluetoothAdapter.isEnabled(), menu, id.menu_frotect_select, id.menu_frotect_reconnect, id.menu_frotect_disconnect);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
	switch (item.getItemId()) 
	{
	
	case R.id.menu_frotect_reconnect:	    
	    if (U.isEmpty(mmSelectedDeviceAddress)) return false;
	    if (mConnection.isConnected()) return false;
	    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mmSelectedDeviceAddress);
	    mConnection.connect(device); // F0:B4:79:07:AE:EE
	    break;
	    
	case R.id.menu_frotect_next:
	    nextActivity();
	    return true;
	    
	case R.id.menu_frotect_select:
	    if (!mBluetoothAdapter.isEnabled()) 
	    {
		// Ask user to enable bluetooth if it's not 
		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	    }
	    else
	    {	    
		// Launch the PresetsActivity to see devices and do scan
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	    }
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

    public void vibrate(long l)
    {
	mVibrator.vibrate(l);	    
    }

    public void vibrate(long[] pattern, int repeat)
    {
	mVibrator.vibrate(pattern, repeat);	    
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
	    mHandler.onToast("Sensor info incomplete");
	    return true; // click was consumed
	}

	// not a strand but the extra unbound sensor -> open sensor dialog if connected
	if (n>=4)
	{
	    SensorDialog dialog = new SensorDialog(this);
	    mHandler.addStatusListener(dialog);
	    //mHandler.addDataListener(dialog);
	    mHandler.addDataCompleteListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();
	}
	else
	{
	    int strand = n+1;
	    StrandDialog dialog = new StrandDialog(this, strand);
	    mHandler.addStatusListener(dialog);
	    mHandler.addDataCompleteListener(dialog);
	    mHandler.addDataListener(dialog); // receive STR messagesupdate strand info (temp, on-minutes etc.)
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
	boolean connected = mConnection.isConnected();

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
	// always show config button since there are connection independent elements now (verbosity)
	U.setVisible(buttonConfig, true); // haveInfo); // have info? => have values for cost etc.
	
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

    protected void doLog(String s)
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
	    mConnection.sendLine("\n\nH\n\n"); // request command line help to be sent
	    mConnection.sendLine(MessageParser.CMD_ACK + ACK_UPDATE);
	    break;
	case R.id.frotectButtonPower:
	    stDialog = new StatsDialog(this, StatsDialog.POWER);
	    mHandler.addDataCompleteListener(stDialog);
	    stDialog.setOnDismissListener(this);
	    stDialog.show();	    
	    break;
	case R.id.frotectButtonTemp:
	    stDialog = new StatsDialog(this, StatsDialog.TEMP);
	    mHandler.addDataCompleteListener(stDialog);
	    stDialog.setOnDismissListener(this);
	    stDialog.show();	    
	    break;
	case R.id.frotectButtonCost:
	    stDialog = new StatsDialog(this, StatsDialog.COST);
	    mHandler.addDataCompleteListener(stDialog);
	    stDialog.setOnDismissListener(this);
	    stDialog.show();	    
	    break;
	case R.id.frotectButtonDuty:
	    stDialog = new StatsDialog(this, StatsDialog.DUTY);
	    mHandler.addDataCompleteListener(stDialog);
	    stDialog.setOnDismissListener(this);
	    stDialog.show();	    
	    break;
	case R.id.frotectButtonConfig:
	    ConfigDialog cDialog = new ConfigDialog(this);
	    mHandler.addStatusListener(cDialog);	    
	    mHandler.addDataListener(cDialog); // receive ACKs
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
	enum TYPE { INFO, HELP, STATS, MINMAX, STRANDS, SENSORS, STARTTIMES }; 

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
	protected void onLineReceived(String receivedLine) 
	{ 
	    try
	    {
		handleCommand(receivedLine);
	    }
	    catch (Exception e)
	    {
		Log.e(TAG, "onLineReceived: " + e);
		U.setText(mErrorView, "EXCPT: " + e, Color.MAGENTA);
		U.setVisible(mErrorView);
	    }
	}

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
	    mConnection.sendLine("\nV1\n"); // set verbosity to "normal" (to see events, heartbeat etc.)
	    mConnection.sendLine("\nD\n");  // dump all info including strands, min/max and stats
	    mConnection.sendLine("\nS\n");  // dump sensor info 
	    mConnection.sendLine("\nH\n");  // request command line help to be sent  

	    long pattern[] = { 0, 50, 200, 50 };
	    vibrate(pattern, -1);

	    FrotectActivity.this.mConnected = true;	    
	    updateControls();
	}

	@Override
	protected void onDeviceDisconnected() 
	{ 
	    setTitleMsg("unconnected");

	    FrotectActivity.this.mConnected = false;
	    updateControls();
	    long pattern[] = { 0, 50, 200, 50, 200, 50 };
	    vibrate(pattern, -1);
	}
	

	@Override
	protected void onDebugMessage(String msg) { if (verbosity>=VERB_DEBUG) doLog(msg); }

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
	    if (null==c) return;
	    
	    if (verbosity>=VERB_DEBUG)
	    {
		doLog("LINE: " + c);
	    }
	    
	    try
	    {
		if (c.startsWith("P.")) // ping
		{
		    lastPingLocal = Calendar.getInstance().getTimeInMillis()-startupMillis;
		    int localProgress  = (int)(((lastPingLocal-lastUpdateLocal)/1000)%62);
		    
		    // its already in seconds not ms:
		    long lastPingRemote = (long)Math.round(MessageParser.parsePing(c));
		    int  remoteProgress = (int)(lastPingRemote-lastUpdateRemote); 

		    U.setText(tvLastPing, String.format(Locale.ENGLISH, "%d / %d", localProgress, remoteProgress));
		    U.setProgress(pingProgress, localProgress); 
		}
		else if (hasPrefix(c, "HB")) // heartbeat
		{
		    Map<String, Object> map = MessageParser.parseHeartbeatInfo(c);
		    if (null!=map)
		    {
			// led status 
			Integer led  = (Integer) map.get("l"); 
		         // condition: 0 - during setup, 1 - ok, 2 - some sensors missing, 3 - no sensors/severe error
			Integer cnd  = (Integer) map.get("c");       
			@SuppressWarnings("unused")
                        Integer temp = (Integer) map.get("t");       

			boolean on = null!=led && led<1; // inverse logic (open collector connects LED to GND) 
			boolean ok = null!=cnd && cnd<2; 
			if (!ok)
			{
			    showToast("Sonsors missing!");
			}
			int id = on ? R.drawable.blue_lheart_64 : R.drawable.blue_dheart_64;			
			U.setImageResource(imgHeartBeat, id);
		    }
		}
		else if (hasPrefix(c, "UP"))
		{
		    Map<String, Object> map = MessageParser.parseUpdateInfo(c);
		    if (null!=map)
		    {
			Double t = (Double) map.get("t");
			if (null!=t) lastUpdateRemote = (long)Math.round(t);
			lastUpdateLocal = Calendar.getInstance().getTimeInMillis()-startupMillis;
		    }
		}
		else if (hasPrefix(c, MessageParser.CMD_ACK))
		{
		    if (c.contains(ACK_UPDATE))
		    {
			vibrate(100);
		    }
		}
		else if (hasPrefix(c, "CUR")) // current minutes on
		{
		    Map<String, Object> map = MessageParser.parseCurrentInfo(c);
		    if (null!=map && map.containsKey("n") && map.containsKey("on"))
		    {
			int n  = (Integer) map.get("n");
			int on = (Integer) map.get("on");
			updateInfoFromCurrInfo(n, on);
		    }
		}
		else if (hasPrefix(c, "D")) // debug
		{
		    if (verbosity>=VERB_DEBUG) doLog(c);
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
		else if (hasPrefix(c, "SEN")) // sensor info (basically addresses and status)
		{
		    onSensorInfo(c);
		}
		else if (c.startsWith("SEN!")) // end of sensors info
		{		
		    dispatchDataComplete(TYPE.SENSORS, sensorsBuffer.finish());
		    checkDataAvailability();
		}
		else if (hasPrefix(c, "MM")) // min/max averageTemp. history
		{		
		    minmaxBuffer.append(c);
		}
		else if (c.startsWith("MM!")) // end of min/max averageTemp. history
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
		    U.setText(mErrorView, c.replaceAll("^E. ", "ERR: "), Color.RED);
		    U.setVisible(mErrorView);
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
		else if (hasPrefix(c, "W")) // warning
		{		
		    logErrorOrWarn(c);
		    U.setText(mErrorView, c.replaceAll("^W. ", "WARN: "), Color.YELLOW);
		    U.setVisible(mErrorView);
		}
		else if (hasPrefix(c, "CMD")) // help
		{		
		    if (verbosity>=VERB_VERBOSE) doLog(c);
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
		else if (hasPrefix(c, "T")) // start times info
		{		
		    starttimesBuffer.append(c.replaceAll("^T.", ""));	    
		}
		else if (c.startsWith("T!"))
		{
		    finalizeStarttimes();		    
		    checkDataAvailability();
		} 
	    }
	    catch (Exception e)
	    {
		doLog("handleCommand: " + U.toStacktrace(e));
		doLog("handleCommand: line: '" + c + "'");
		
		U.setText(mErrorView, "EXCPT: " + e, Color.MAGENTA);
		U.setVisible(mErrorView);
	    }
	}

	private void updateInfoFromCurrInfo(int n, int on)
        {
	    Sensor sensor = sensors[n-1];
	    sensor.minutesOn = on;
	    // "CUR.on="
	    U.setText(sensor.misc, "on: " + on + "m"); 
        }

	private void onSensorInfo(String c)
	{
	    HashMap<String, Object> map = MessageParser.parseSensorInfo(c);
	    if (null!=map)
	    {
		int    n      = (Integer) map.get("n");
		String addr   = (String)  map.get("@");
		boolean used  = (Boolean) map.get("used");
		boolean avail = (Boolean) map.get("avail");
		Boolean bound = (Boolean) map.get("bnd");
		updateInfoFromSensorInfo(n, addr, used, avail, bound);
	    }

	    // summarize only if this is a continuation line, not e.g. "SEN. "
	    if (U.startsWith(c, "SEN: ")) sensorsBuffer.append(c);
	}

	private void onEvent(String c)
	{
	    if (c.contains("e=hist.sav"))
	    {
		if (verbosity>VERB_NORM) showToast("Temp. history was saved.");
		doLog(c);
		mConnection.sendLine("");
		mConnection.sendLine("DM"); // dump min/max
		mConnection.sendLine("");
	    }
	    if (c.contains("e=hist.upd"))
	    {
		String reason = c.replaceAll(".*,reas=","");
		if (verbosity>VERB_NORM) showToast("Temp. history was updated.\n(" + reason + ")");
	    }
	    else if (c.contains("e=stats.sav"))
	    {
		doLog(c);
		mConnection.sendLine("");
		mConnection.sendLine("DH"); // dump hourly stats
		mConnection.sendLine("");
		mConnection.sendLine("DD"); // dump daily  stats 
		mConnection.sendLine("");
		mConnection.sendLine("DW"); // dump weekly stats
		mConnection.sendLine("");
	    }
	    else if (c.contains("e=pwr."))
	    {
		try
		{
		    String s = c.replaceAll("^.*,n=", "");
		    int n = Integer.parseInt(s);
		    String onOff = c.contains(".on") ? "on" : "off";		    
		    //onToast("Strand " + n +": powered " + onOff);
		    doLog("Strand " + n +": powered " + onOff);
		    
		    Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); 

		    //MediaPlayer player = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI);
		    if (mWithSound)
		    {
			MediaPlayer player = MediaPlayer.create(getApplicationContext(), uri);
			player.setVolume(0.02f, 0.02f);
			player.start();
		    }
		}
		catch (Exception x)
		{
		    doLog("onEvent: " + x);
		}		
	    }
	    else if (c.contains("e=ontime.upd"))
	    {
		String reason = c.replaceAll(".*,reas=","");
		if (reason.contains("save"))
		{
		    if (verbosity>VERB_NORM) showToast("On-times were updated and saved.");
		}
		else if (reason.contains("poff"))
		{
		    if (verbosity>VERB_NORM) showToast("On-times changed\n(Strand powered off)");
		}
		else if (reason.contains("unbnd"))
		{
		    showToast("On-times changed.\n(Strand unbound)" + reason);
		}
		else
		{
		    showToast("On-times changed (Unknown reason: " + reason + ")");
		}
		doLog(c);
	    }
	    else if (c.contains("e=sens.retry"))
	    {
		String retries = c.replaceAll(".*e=sens.retry,r=", "");
		showToast("Searching for sensors.\nRetries:" + retries);
		doLog(c);
	    }	    
	    else if (c.contains("e=sens.redetect"))
	    {
		showToast("Sensors re-detection started.");
		// do NOT log. might occur numerous times when sensor cable was detached
	    }
	    else if (c.contains("e=sens.init"))
	    {
		String info = c.replaceAll(".*e=sens.init", "");
		showToast("Sensors intiialized: " + info);
		doLog(c);
	    }
	}

	
	private void finalizeInfo()
	{
	    synchronized(this)
	    {
		String info = infoBuffer.finish();	    

		Map<String, String> map = MessageParser.parseInfoMessages(info);
		String sV = map.get("Frotect");
		if (!U.isEmpty(sV))
		{
		    setMainTitle(sV.replaceAll(", d=", " "));
		}

		String sM = map.get("#Strnds");
		String sN = map.get("#Sens");
		String sA = map.get("AvlSens");
		String sC = map.get("Cost");

		numStrands = U.toInt(sM);
		numSensors = U.toInt(sN);
		avlStrands = U.toInt(sA);
		numCost100 = U.toDouble(sC);

		dispatchDataComplete(TYPE.INFO, info);
	    }
	}

	
	private void finalizeStarttimes()
	{
	    synchronized(this)
	    {
		String uptime = starttimesBuffer.finish();	    
		dispatchDataComplete(TYPE.STARTTIMES, uptime);
	    }
	}
	
	
	private void onFlashInfo(String c)
	{
	    try
	    {
		HashMap<String, Integer> map = MessageParser.parseFlashInfo(c);
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

	private void onStrandInfo(String line)
	{
	    try
	    {
		// STR: n=1, val=0, lit=0, upd=1, tu=28.50, tl=24.00, averageTemp=?, err=50, last=117965829, ago=0, addr=28:50:81:E1:04:00:00:6E, used=1, avail=1
		HashMap<String, Object> map = MessageParser.parseStrandInfo(line);

		if (null==map)
		{
		    doLog("onStrandInfo: failed to parse: " + line);
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
		    int     power = (Integer) map.get("P");
		    int     err   = (Integer) map.get("err");
		    
		    String  addr  = (String)  map.get("@");
		    Boolean used  = (Boolean) map.get("usd");
		    Boolean avail = (Boolean) map.get("avl");
		    Boolean bound = (Boolean) map.get("bnd");
		    
		    Double rampStart = (Double) map.get("tls");
		    Double rampEnd   = (Double) map.get("tle");
		    Double rampDelta = (Double) map.get("rpd");
		    
		    updateInfoFromStrandInfo(n, avail, used, lit, tu, tl, t, err, power, addr, bound, rampStart, rampEnd, rampDelta);
		}

		// append only if this is a continuation message,not "STR."
		if (line.startsWith("STR:"))
		{
		    strandsBuffer.append(line);
		}
		
		if (verbosity>VERB_NORM) doLog(line);
	    }
	    catch (Exception e)
	    {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		doLog("onStrandInfo: " + sw.toString());		
	    }
	}

	private void onMeasurement(String c)
	{
	    try
	    {
		HashMap<String, Object> map = MessageParser.parseMeasurement(c);
		
		int strand = (Integer) map.get("s");
		double now = (Double) map.get("now");
		double old = (Double) map.get("old");

		TextView misc = (TextView) findViewById(miscId[strand-1]);
		U.setText(misc, String.format(Locale.ENGLISH, "%2.1f", now));

		updateInfoFromMeasurement(strand, now, old);
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

	private void updateInfoFromSensorInfo(int n, String addr, boolean used, boolean avail, Boolean bound)
	{
	    n=n-1;
	    Sensor sensor = sensors[n];

	    sensor.avail = avail;
	    sensor.used  = used;
	    if (null!=addr) sensor.addr = addr;
	    if (!sensor.used)
	    {
		U.setText(sensor.value, "n.a.");
		U.setText(sensor.limits, "");
		U.setText(sensor.misc,   "unbound");
	    }
	    visualizeSensorStatus(sensor);
	}


	private void updateInfoFromMeasurement(int n, double t, double last)
	{
	    try
	    {
		Sensor sensor = sensors[n-1];
		String value = String.format(Locale.ENGLISH, "%2.1f°C", t);
		U.setText(sensor.value, value);	    

		boolean same = Math.abs(last-t)<0.1;
		U.setText(sensor.misc, value, same ? Color.WHITE : Color.CYAN);
		sensor.singleTemp = t;
		sensor.used = sensor.avail = true; // obviously, otherwise there was no measure

		visualizeSensorStatus(sensor);
	    }
	    catch (Exception e)
	    {
		System.out.println("updateInfo: " + e);
	    }
	}

	private void updateInfoFromStrandInfo(int n, Boolean avail, Boolean used, boolean lit, double tu, double tl, 
		Double t, int err,  int power, String addr, Boolean bound,
		Double rampStart, Double rampEnd, Double rampDelta)
	{
	    n=n-1;
	    
	    //	    ImageView sensorBulb  = sensors[n].icon;
	    TextView  value  = sensors[n].value;
	    TextView  limits = sensors[n].limits; 
	    TextView  misc   = sensors[n].misc; 

	    visualizeSensorStatus(sensors[n]);
	    if (null!=avail) U.setEnabled(value, avail);

	    Sensor sensor = sensors[n];
	    
	    // wild guess if there is no bound info
	    sensor.bound = (null==bound) ? n<4 : bound;
	    sensor.lit   = lit;
	    sensor.lo    = tl;
	    sensor.hi    = tu;
	    sensor.power = power;
	    
	    if (null!=avail) sensor.avail = avail;
	    if (null!=used)  sensor.used  = used;
	    
	    if (U.notEmpty(addr)) sensor.addr      = addr;	    
	    if (null!=rampStart)  sensor.rampStart = rampStart;
	    if (null!=rampEnd  )  sensor.rampEnd   = rampEnd;
	    if (null!=rampDelta)  sensor.rampDelta = rampDelta;
	    
	    if (null!=t) sensor.averageTemp = t;

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
		String s = String.format(Locale.ENGLISH, "%2.1f°C", t);
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

	    if (!sensor.avail)
	    {
		U.setTextColor(value, (null!=used && used) ? Color.RED : Color.GRAY);
	    }	    

	    if (!sensor.used)
	    {
		U.setText(value, "unused");
	    }
	    else 
	    {
		U.setText(limits, String.format(Locale.ENGLISH, "%2.1f / %2.1f", tl, tu));
	    }

	    if (err>0)
	    {
		U.setText(misc, "errors: " + err);
	    }
	    else //if (sensor.minutesOn>0)
	    {
		U.setText(misc, "on: " + sensor.minutesOn + "m");
	    }
	}
    }

    class NumberPicker implements OnClickListener //, OnTouchListener
    {
	private View dec;
	private View inc;
	protected TextView value;
	private double min;
	private double max;
	private double step;
	//private HoldThread holdThread;
	//private Handler handler;
	private int numSubDecimals;

	public NumberPicker(Dialog dialog, int decID, int valueID, int incID, double start, double min, double max)
	{
	    this(dialog, decID, valueID, incID, start, min, max, 0.5f);
	}
	
	public NumberPicker(Dialog dialog,  int decID, int valueID,  int incID, double start, double minVal, double maxVal, double stepVal)
	{
	    this( dialog,   decID,  valueID,  incID,  start,  minVal,  maxVal,  stepVal, 1);
	}
	
	public NumberPicker(Dialog dialog, final int decID, int valueID, final int incID, double start, double minVal, double maxVal, double stepVal, int numSubDecimals)
	{
	    setListener(dec = dialog.findViewById(decID));
	    setListener(inc = dialog.findViewById(incID));
	    value = (TextView) dialog.findViewById(valueID);

	    min  = minVal;
	    max  = maxVal;
	    step = stepVal;
	    this.numSubDecimals = numSubDecimals;
	    
	    setValue(start);
	    
	    /*
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
	    */

	    //holdThread = new HoldThread(handler);
	    //holdThread.start();	    
	}
	
        protected void setValue(double v)
        {
            value.setText(String.format(Locale.ENGLISH, "%-2." + numSubDecimals + "f", v));
        }
	
	private void setListener(View v)
	{
	    if (null!=v) 
	    {
		v.setOnClickListener(this);
		//v.setOnTouchListener(this);
	    }
	}

	@Override
	protected void finalize() throws Throwable
	{
	    stop();
	    super.finalize();
	}
		
	public void stop()
	{
	    //holdThread.cancel();
	    //holdThread.stop();
	    //holdThread = null;
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
	    setValue(d);
	}

	private void decValue(double amount)
	{
	    double d = getValue();
	    d = Math.max(min, d-amount);
	    setValue(d);
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
		
	/*
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
			doSleep(330);
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
	*/

	/*
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
        */
    }

    
    class NumberPickerInt extends NumberPicker
    {
	public NumberPickerInt(Dialog dialog, final int decID, int valueID, final int incID, int start, int minVal, int maxVal)
	{
	    this(dialog, decID, valueID, incID, start, minVal, maxVal, 1);
	}
	
	public NumberPickerInt(Dialog dialog, final int decID, int valueID, final int incID, int start, int minVal, int maxVal, int stepVal)
	{
	    super(dialog, decID,  valueID, incID,  start,  minVal,  maxVal,  stepVal);
	}
	
	@Override
        protected void setValue(double v)
        {
            value.setText(String.format(Locale.ENGLISH, "%d", (long)v));
        }
    }
    
    class SensorDialog extends SPPStatusAwareDialog implements FrotectBTDataCompleteListener, SPPDataListener
    {
	private static final String ACK_MOVED = "sens.moved";
	private static final String ACK_RESCN = "sens.rescn";
	
	private int sensorAddrIDs[]   = { R.id.sensorAddr1,    R.id.sensorAddr2,   R.id.sensorAddr3,   R.id.sensorAddr4,   R.id.sensorAddr5 };
	private int sensorUpIDs[]     = { R.id.sensorUp1,      R.id.sensorUp2,     R.id.sensorUp3,     R.id.sensorUp4,     R.id.sensorUp5   };
	private int sensorDownIDs[]   = { R.id.sensorDown1,    R.id.sensorDown2,   R.id.sensorDown3,   R.id.sensorDown4,   R.id.sensorDown5 };
	private int sensorBoundsIDs[] = { R.id.tvSensorBound1, R.id.tvSensorBound2, R.id.tvSensorBound3, R.id.tvSensorBound4, R.id.tvSensorBound5 }; 

	SensorViews infos[] = { new SensorViews(), new SensorViews(), new SensorViews(), new SensorViews(), new SensorViews(), };

	private FrotectActivity frotect; 

	class SensorViews 
	{
	    View     up, down, bound;
	    TextView addr;

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

	    requestWindowFeature(Window.FEATURE_NO_TITLE); 
	    //setTitle("Sensors");
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
	    registerViews(false, R.id.frotectRescan, R.id.frotectSensorDialogButtonUpdate);

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
		// sensorsBuffer contains info sent upon the "S" command.
		// strandsBuffer contains info sent upon the "DS" command.
		// If either one was received completely, sensors addresses and order are known.
		if (frotect.sensorsBuffer.isComplete() || frotect.strandsBuffer.isComplete())
		{
		    refreshViews();
		}
		else
		{
		    // request uC to send sensor info
		    mConnection.sendLine("S"); // dump sensor info
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
	    mConnection.sendLine("X"); 
	    mConnection.sendLine(MessageParser.CMD_ACK + ACK_RESCN);
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
	    switch (type)
	    {
	    case STRANDS:
	    case SENSORS:
		refreshViews();
		break;
	    default:
	    }
	}

        private void refreshViews()
        {   
            for (int i=0; i<sensors.length; i++) 
            { 
        	Sensor s = sensors[i]; 
        	SensorViews info = infos[i];
        	
        	String addr = s.addr;
        	addr = OneWireAddrTool.shortenAddr(addr);
        	int color = OneWireAddrTool.colorFromAddr(addr);
        	String enc = OneWireAddrTool.encodeAddr(addr);
        	
        	info.setFound(i, s.avail);
        	
        	U.setText(info.addr, enc + "\n[" + addr + "]");
        	U.setTextColor(info.addr, color);
        	U.setVisible(info.bound, s.bound);
            }
        }
        
	@Override
	public void onClick(int id)
	{
	    if (R.id.frotectRescan==id) 
	    { 
		confirmRescan();  
		return;  
	    }
	    
	    if (R.id.frotectSensorDialogButtonUpdate==id)
	    {
		mConnection.sendLine("");
		mConnection.sendLine("D");
		mConnection.sendLine("");
		return;
	    }

	    for (int i=0; i<5; i++)
	    {
		if (sensorUpIDs[i]==id)   
		{ 
		    mConnection.sendLine("");
		    // "Move up" command
		    mConnection.sendLine("-" + (i+1) + "\n"); 
		    mConnection.sendLine("");
		    // request to send sensors info to force refreshViews():
		    mConnection.sendLine("S"); 
		    mConnection.sendLine("");
		    // request ACK_MOVED to give feedback to user (vibrate)
		    mConnection.sendLine(MessageParser.CMD_ACK + ACK_MOVED); 
		    mConnection.sendLine("");
		    break; 
		}  
		else if (sensorDownIDs[i]==id) 
		{ 
		    mConnection.sendLine("");
		    // "Move down" command
		    mConnection.sendLine("+" + (i+1) + "\n");  
		    mConnection.sendLine("");
		    mConnection.sendLine("S"); 
		    mConnection.sendLine("");
		    mConnection.sendLine(MessageParser.CMD_ACK + ACK_MOVED);
		    mConnection.sendLine("");
		    break; 
		}
	    }	    
	}

	public void onLineReceived(String line)
        {
	    if (line.startsWith("ACK."))
	    {
		if (line.contains(ACK_MOVED))
		{
		    vibrate(100L);
		}
		else if (line.contains(ACK_RESCN))
		{
		    vibrate(500L);		
		}
	    }
        }

    }

    class StrandDialog extends SPPStatusAwareDialog implements FrotectBTDataCompleteListener, SPPDataListener, android.view.View.OnClickListener
    {
	private FrotectActivity frotect;
	private NumberPicker npLower, npUpper, npPower;
	private TextView tvStrandAddress;
	private TextView tvMinutesOn;
	private TextView tvLastTemp;
	private TextView tvAvgTemp  ;
	private TextView tvRampInfo;
	private NumberPicker npRampDelta;
	private NumberPicker npRampEnd;
	private int     strandNo;
	private boolean bound;

	protected StrandDialog(FrotectActivity frotect, int strandNo)
	{
	    super(frotect);
	    requestWindowFeature(Window.FEATURE_NO_TITLE); 

	    this.frotect = frotect;
	    this.strandNo = strandNo;
	    LayoutInflater inflater = this.getLayoutInflater();
	    setContentView(inflater.inflate(R.layout.dialog_strand, null));

	    this.tvStrandAddress = (TextView) findViewById(R.id.frotectStrandAddr);
	    
	    this.tvMinutesOn = (TextView) findViewById(R.id.frotectStrandDialogMinutesOn);
	    this.tvAvgTemp   = (TextView) findViewById(R.id.frotectStrandDialogAvgTemp);
	    this.tvLastTemp  = (TextView) findViewById(R.id.frotectStrandDialogLastTemp);	    
	    this.tvRampInfo  = (TextView) findViewById(R.id.frotectStrandDialogRampingInfo);
	}

	@Override
	protected void onStart() 
	{
	    mHandler.addDataCompleteListener(this);
	    mHandler.addStatusListener(this);	   
	    mHandler.addDataListener(this);
	};
	
	@Override
	public void show()
	{
	    super.show();
	    refreshControls();
	}

	private void refreshControls()
	{
	    Sensor info = sensors[strandNo-1];	    
	    System.out.println("refreshControls: strand " + strandNo + ": " + info);
	    
	    double currLo      = -30;
	    double currHi      =  30;
	    int    currPower   =   0;
	    double currDelta   =   0;
	    double currRampEnd = -30;
	    if (info.avail)
	    {
		currLo      = info.lo;
		currHi      = info.hi;    // no need to take ramping start value into consideration here. 
		currPower   = info.power; // using the current threshold and preserving delta and end value
		currDelta   = info.rampDelta; // will yield the same results when saved, i.e. the current temperature
		currRampEnd = info.rampEnd;   // will decrease/increase with the same rate from the current day on		
	    }

	    String addr  = OneWireAddrTool.shortenAddr(info.addr);
	    int    color = OneWireAddrTool.colorFromAddr(addr);
	    String enc   = OneWireAddrTool.encodeAddr(addr);
	    U.setText(tvStrandAddress, enc + "\n[" + addr + "]");
	    U.setTextColor(tvStrandAddress, color);

	    // register views to be disabled when there is no connection
	    registerViews(false, R.id.buttonThresSave, R.id.buttonStrandFlash, R.id.buttonStrandBind, R.id.buttonStrandUnbind);
	    
	    // same as above, however to not capture onClick events to make sure the NumberPickers will receive them
	    registerViews(false, false, R.id.decLo,  R.id.incLo, R.id.decHi, R.id.incHi, R.id.decPower, R.id.incPower);     
	    registerViews(false, false, R.id.decRampDelta,  R.id.incRampDelta, R.id.decRampEnd, R.id.incRampEnd);     
	    setStatus(mConnected);	    

	    this.npLower     = new NumberPicker(this,    R.id.decLo,    R.id.valueLo,    R.id.incLo,    currLo,   -30,  30); 
	    this.npUpper     = new NumberPicker(this,    R.id.decHi,    R.id.valueHi,    R.id.incHi,    currHi,   -30,  30); 
	    this.npPower     = new NumberPickerInt(this, R.id.decPower, R.id.valuePower, R.id.incPower, currPower,  0, 2*255);	    
	    this.npRampDelta = new NumberPicker(this,    R.id.decRampDelta, R.id.valueRampDelta, R.id.incRampDelta,  currDelta,    -1.2,  1.2, 0.01, 2); 
	    this.npRampEnd   = new NumberPicker(this,    R.id.decRampEnd,   R.id.valueRampEnd,   R.id.incRampEnd,    currRampEnd,  -30,  30       ); 
	    	    
	    bound = info.bound;
	    U.setVisible(findViewById(R.id.layoutStrandBind),  !bound);
	    U.setVisible(findViewById(R.id.layoutStrandUnbind), bound);

	    U.setText(tvMinutesOn, "on: " + info.minutesOn + "m");

	    U.setText(tvAvgTemp,   String.format(Locale.ENGLISH, "avg: %2.1f",  info.averageTemp) + "°C");
	    if (null!=info.singleTemp)
	    {
		U.setText(tvLastTemp,  String.format(Locale.ENGLISH, "last: %2.1f", info.singleTemp ) + "°C");
	    }
	    
	    String extraInfo = "";
	    if (null!=info.rampStart)
	    {
		extraInfo += "\nrmp:" + info.rampStart + "/" + info.rampEnd;
		extraInfo += "\nΔ: " + info.rampDelta + "°C/d";
	    }
	    U.setText(tvRampInfo, extraInfo);	    	    
	}

	@Override
	protected void onStop() 
	{
	    npLower.stop();
	    npUpper.stop();
	    npPower.stop();
	    npRampDelta.stop();
	    npRampEnd.stop();
	    super.onStop();
	};
	
	@Override
	public void onConnect(Device device)
	{
	    super.onConnect(device);
	    refreshControls();
	    //U.setInvisibile(findViewById(bound ? R.id.layoutStrandBind : R.id.layoutStrandUnbind));
	}

	public void onDataComplete(TYPE type, String data)
	{
	    //if (type==TYPE.STRANDS) { refreshControls(); } 
	}

	public void onLineReceived(String line)
	{
	    //if (line.startsWith("STR") || line.startsWith("MEAS") || line.startsWith("CUR")) refreshControls();
	    if (line.startsWith("ACK.") && line.contains(ACK_SET))
	    {
		refreshControls();
		vibrate(100L);
	    }
	}
	
	private static final String ACK_SET = "set.LUPRQ";

	@Override
	public void onClick(int id)
	{
	    switch (id)
	    {
	    case R.id.buttonThresSave:
		String cmd1 = String.format(Locale.ENGLISH, "%s%d=%d", MessageParser.CMD_LOWER,      strandNo, (int)Math.round(100*npLower.getValue()));
		String cmd2 = String.format(Locale.ENGLISH, "%s%d=%d", MessageParser.CMD_UPPER,      strandNo, (int)Math.round(100*npUpper.getValue()));
		String cmd3 = String.format(Locale.ENGLISH, "%s%d=%d", MessageParser.CMD_POWER,      strandNo, (int)Math.round(    npPower.getValue()));
		String cmd4 = String.format(Locale.ENGLISH, "%s%d=%d", MessageParser.CMD_RAMP_LIMIT, strandNo, (int)Math.round(100*npRampEnd.getValue()));
		String cmd5 = String.format(Locale.ENGLISH, "%s%d=%d", MessageParser.CMD_RAMP_DELTA, strandNo, (int)Math.round(100*npRampDelta.getValue()));
		// Send request for ACK_MOVED to detect when everything was EVERY value was updated.
		// If we would detect the single strand updates in onLineReceived(), the values show in the dialog
		// would flicker until finally the last command was accepted.		
		String cmd6 = MessageParser.CMD_ACK + ACK_SET; 
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine(cmd1);
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine(cmd2);
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine(cmd3);
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine(cmd4);
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine(cmd5);
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine(cmd6);
		frotect.mConnection.sendLine("");
		//dismiss();
		break;
	    case R.id.buttonStrandFlash:
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine("I" + strandNo);
		frotect.mConnection.sendLine("");
		break;
	    case R.id.buttonStrandBind:
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine("G" + strandNo); // re-bind command
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine("S"); // request sensor info update
		frotect.mConnection.sendLine("");
		break;
	    case R.id.buttonStrandUnbind:
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine("F" + strandNo); // "F"orget the sensor
		frotect.mConnection.sendLine("");
		frotect.mConnection.sendLine("S"); // request sensor info update
		frotect.mConnection.sendLine("");
		break;
	    }
	}
    }

    class ConfigDialog extends SPPStatusAwareDialog implements OnCheckedChangeListener, SPPDataListener, android.widget.CompoundButton.OnCheckedChangeListener 
    {
	private static final String ACK_SET = "set.cfg";
	private NumberPicker    cost, strands, sensors;
	private FrotectActivity frotect;
	private RadioGroup mRadioGroupVerbosity;
	private CheckBox mNotifSound, mExitConfirmation;


	protected ConfigDialog(FrotectActivity frotect)
	{
	    super(frotect);
	    requestWindowFeature(Window.FEATURE_NO_TITLE); 

	    this.frotect = frotect;
	    LayoutInflater inflater = this.getLayoutInflater();
	    setContentView(inflater.inflate(R.layout.dialog_config, null));	    
	    //setTitle("Config ");

	    double currCost = null==numCost100 ? 20 : 0.01*numCost100;

	    // register these to be hidden when unconnected:
	    registerViews(R.id.saveCost, R.id.saveNumStrands, R.id.saveNumSensors);
	    
	    // only disable however this ids when unconnected:
	    registerViews(false, R.id.frotectReboot);	    
	    registerViews(false, R.id.decCost,       R.id.valueCost,       R.id.incCost      );
	    registerViews(false, R.id.decNumStrands, R.id.valueNumStrands, R.id.incNumStrands);
	    registerViews(false, R.id.decNumSensors, R.id.valueNumSensors, R.id.incNumSensors);
	    
	    cost    = new NumberPicker(this,    R.id.decCost,       R.id.valueCost,       R.id.incCost,    currCost, 0, 100,  0.25);
	    strands = new NumberPickerInt(this, R.id.decNumStrands, R.id.valueNumStrands, R.id.incNumStrands,     4, 1,   4,  1   );
	    sensors = new NumberPickerInt(this, R.id.decNumSensors, R.id.valueNumSensors, R.id.incNumSensors,     4, 1,   4,  1   );


	    frotect.mHandler.addStatusListener(this);
	    try 
	    {
		mNotifSound = (CheckBox) findViewById(R.id.frotectCheckButtonNotifSound);
		if (null!=mNotifSound)
		{
		    U.setOnCheckedChangeListener(mNotifSound, this);
		    U.setChecked(mNotifSound, frotect.getSoundNotifications());
		}
		
		mExitConfirmation = (CheckBox) findViewById(R.id.frotectCheckButtonConfirmExit);
		if (null!=mExitConfirmation)
		{
		    U.setOnCheckedChangeListener(mExitConfirmation, this);
		    U.setChecked(mExitConfirmation, frotect.getExitConfirmation());
		}
	    } 
	    catch (Exception e) 
	    { 
		showToast("ConfigDialog: " + e); 
	    }
	    
	    mRadioGroupVerbosity = (RadioGroup) findViewById(R.id.frotectRadioGroupVerbosity);
	    mRadioGroupVerbosity.setOnCheckedChangeListener(this);
	    
	    setCheckedState();
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
		frotect.mConnection.sendLine("\nC" + (int)Math.round(100*cost.getValue()));
		frotect.mConnection.sendLine(MessageParser.CMD_ACK + ACK_SET);
		break;	    	
	    case R.id.saveNumStrands:
		// M3      set # of strands to 3
		frotect.mConnection.sendLine("\nM" + ((int)strands.getValue()));		
		frotect.mConnection.sendLine(MessageParser.CMD_ACK + ACK_SET);
		break;
	    case R.id.saveNumSensors:
		// N4      set # of sensors to 4
		frotect.mConnection.sendLine("\nN" + ((int)sensors.getValue()));
		frotect.mConnection.sendLine(MessageParser.CMD_ACK + ACK_SET);
		break;	    
	    case R.id.frotectReboot:
		confirmReboot();
		break;
	    }
	}

	private void rebootConfirmed()
	{
	    frotect.mConnection.sendLine("\nB=4711\n");		
	    vibrate(500);
	}

	public void confirmReboot()
	{
	    AlertDialog.Builder builder = new AlertDialog.Builder(frotect);
	    builder.setMessage("Really reboot µC?").setTitle("Confirmation");
	    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { rebootConfirmed(); }});
	    builder.setNegativeButton("No",  new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) {  /* do nothing */  }});	    
	    builder.create().show();
	}
	
	private void setCheckedState()
	{
	    switch (frotect.verbosity)
	    {
	    case VERB_NORM: 
		U.check(mRadioGroupVerbosity, R.id.frotectRadioButtonNormal);
		break;
	    case VERB_VERBOSE: 
		U.check(mRadioGroupVerbosity, R.id.frotectRadioButtonVerbose);
		break;
	    case VERB_DEBUG: 
		U.check(mRadioGroupVerbosity, R.id.frotectRadioButtonDebug);
		break;
	    }
	}

	public void onCheckedChanged(RadioGroup rg, int checkedId)
	{
	    if (R.id.frotectRadioButtonNormal==checkedId)
	    {
		frotect.setVerbosity(FrotectActivity.VERB_NORM);
	    }
	    else if (R.id.frotectRadioButtonVerbose==checkedId)
	    {
		frotect.setVerbosity(FrotectActivity.VERB_VERBOSE);

	    }
	    else if (R.id.frotectRadioButtonDebug==checkedId)
	    {
		frotect.setVerbosity(FrotectActivity.VERB_DEBUG);

	    }		
	}

	public void onCheckedChanged(CompoundButton butt, boolean checked)
        {
	    if (mNotifSound==butt)
	    {
		frotect.setSoundNotifications(checked);
	    }
	    else if (mExitConfirmation==butt)
	    {
		frotect.setExitConfirmation(checked);
	    }
        }

	public void onLineReceived(String line)
        {
	    //System.err.println("ConfigDialog: got line '" + line + "'");
	    if (line.startsWith(MessageParser.MSG_ACK) && line.contains(ACK_SET))
	    {
		vibrate(100);
	    }
        }
    }


    class InfoDialog extends OnClickAwareDialog implements FrotectBTDataCompleteListener
    {
	private FrotectActivity frotect;
	private TextView tvInfo;
	private Calendar receiveTime;
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
	

	private void sendLog()
	{
	    String toast    = "Sending info ...";
	    String subject  = "Frotect info";
	    String body     = "" + tvInfo.getText();
	    String csv      = null;
	    String filename = null;
	    
	    MessageParser parser = new MessageParser(getContext());
	    String date = "";
	    if (null!=receiveTime)
	    {
		date = MessageParser.YYYYMMDDHHmmss.format(receiveTime.getTime()).replaceAll("[ :]", "_") + "_";
	    }
	    
	    switch (mType)
	    {
	    	case INFO:
	    	    break;
	    	case HELP:
	    	    toast   = "Sending help ... ";
		    subject = "Frotect command line help";
	    	    break;
	    	case STATS:
	    	    toast    = "Sending power stats ... ";
		    subject  = "Frotect power stats";
		    csv      = parser.convertStatsToCsv(body, receiveTime);
		    filename = date + "power.csv";
	    	    break;
	    	case MINMAX:
	    	    toast    = "Sending temperature stats ... ";
		    subject  = "Frotect temperature stats";
		    csv      = parser.convertHistoryToCsv(body, receiveTime);
		    filename = date + "temperatures.csv";
	    	    break;
	    	default:
	    	    toast   = "Sending " + mType;
	    	    subject = "Frotect " + mType + " info";
	    }
	    
	    Toast.makeText(frotect, toast, Toast.LENGTH_SHORT).show();
	    Log.d(TAG, toast);

	    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
	    emailIntent.setType("text/plain");

	    String[] recipients = new String[]{"a.pogoda@venista.com", };
	    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,   recipients);
	    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
	    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,    body);
	    
	    if (null!=csv)
	    {
		try
		{
		    File root = Environment.getExternalStorageDirectory();
		    File file = new File(root, filename);
		    FileWriter fw = new FileWriter(file);
		    fw.write(csv.toCharArray());
		    fw.close();

		    if (!file.exists() || !file.canRead()) 
		    {
			showToast("Attachment Error");
			body += "\nFailed to save attachment";
		    }
		    else
		    {
			Uri uri = Uri.parse("file://" + file);
			emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
		    }
		}
		catch (Exception e)
		{
		    showToast("Attachment: " + e);
		    body += "\nFailed to attach csv: " + e;
		}		
	    }
	    
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
		receiveTime = frotect.helpBuffer.getReceiveTime();
		mType = TYPE.HELP;
		break;
	    case R.id.buttonInfoGeneral:
		tvInfo.setText("" + frotect.infoBuffer.toString());
		receiveTime = frotect.infoBuffer.getReceiveTime();
		mType = TYPE.INFO;
		break;
	    case R.id.buttonInfoMinMax:
		tvInfo.setText("" + frotect.minmaxBuffer.toString());
		receiveTime = frotect.minmaxBuffer.getReceiveTime();
		mType = TYPE.MINMAX;
		break;
	    case R.id.buttonInfoStats:
		tvInfo.setText("" + frotect.statsBuffer.toString());
		receiveTime = frotect.statsBuffer.getReceiveTime();
		mType = TYPE.STATS;
		break;
	    case R.id.buttonSendInfoText:
		sendLog();
		break;
	    }
	}
    }

    private void showToast(String text)
    {
	Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
        
    public void setSoundNotifications(boolean on)
    {
	mWithSound = on;	
	showToast("Sound was " + (on ? "enabled" : "disabled"));
    }

    public boolean getSoundNotifications()
    {
	return mWithSound;	
    }
    
    public void setExitConfirmation(boolean on)
    {
	mExitConfirmation=on;
    }

    public boolean getExitConfirmation()
    {
	return mExitConfirmation;
    }

    public void setVerbosity(int verbosity)
    {
	this.verbosity = verbosity;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() 
    {
	@Override
	public void onReceive(Context context, Intent intent) 
	{
	    final String action = intent.getAction();

	    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) 
	    {
		final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
		
		switch (state) 
		{
        		case BluetoothAdapter.STATE_OFF:
        		    showToast("Bluetooth turned off");
        		    //mBluetoothEnabled = false;
        		    break;
        		case BluetoothAdapter.STATE_TURNING_OFF:
        		    //showToast("Turning Bluetooth off...");
        		    //mBluetoothEnabled = false; // preemptive obedience
        		    break;
        		case BluetoothAdapter.STATE_ON:
        		    showToast("Bluetooth turned on");
        		    //mBluetoothEnabled = true;
        		    break;
        		case BluetoothAdapter.STATE_TURNING_ON:
        		    showToast("Turning Bluetooth on...");
        		    //mBluetoothEnabled = false; // if turning on, it's not on yet
        		    break;
		}

	    }
	}
    };
    
    
    public boolean onKeyDown(int keyCode, KeyEvent event) 
    {
	//Handle the back button
	if (keyCode == KeyEvent.KEYCODE_BACK && isTaskRoot()) {
	    if (mExitConfirmation)
	    {
		//Ask the user if they want to quit
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Quit")
		.setMessage("Do you want to quit?")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
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


