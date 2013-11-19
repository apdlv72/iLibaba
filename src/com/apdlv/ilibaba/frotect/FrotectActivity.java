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
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Scroller;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.frotect.FrotectActivity.FrotectBTDataCompleteListener.TYPE;
import com.apdlv.ilibaba.gate.DeviceListActivity;
import com.apdlv.ilibaba.gate.GateControlActivity;


public class FrotectActivity extends Activity implements OnClickListener, OnLongClickListener, OnDismissListener
{
    public static final String DUTY = "duty";
    public static final String COST = "cost";
    public static final String TEMP = "temp";
    public static final String POWER = "power";
    protected static final String TAG = "FrotectActivity";
    private BluetoothAdapter mBluetoothAdapter;
    //private BTFrotectSerialService mBTService;

    private static final int REQUEST_ENABLE_BT = 2;
    //private static final int REQUEST_PRESET    = 3;
    private static final int REQUEST_CONNECT_DEVICE = 1;

    private View buttonTemp, buttonPower, buttonCost, buttonDuty, buttonConfig, buttonInfo, buttonUpdate;
    
    private ImageView imgHeartBeat;
    
    private TextView mLogTextView;
    private String mmSelectedAddress = "F0:B4:79:07:AE:EE"; // "BuzBekci1"
    //private String mmSelectedName;

    int tableId[]   = { R.id.sensorTable1,  R.id.sensorTable2,  R.id.sensorTable3,  R.id.sensorTable4,  R.id.sensorTable5 };
    int iconId[]    = { R.id.sensorIcon1,   R.id.sensorIcon2,   R.id.sensorIcon3,   R.id.sensorIcon4,   R.id.sensorIcon5  };
    int valueId[]   = { R.id.sensorValue1,  R.id.sensorValue2,  R.id.sensorValue3,  R.id.sensorValue4,  R.id.sensorValue5 };
    // swapped on purpose
    int limitsId[]  = { R.id.sensorMisc1,   R.id.sensorMisc2,   R.id.sensorMisc3,   R.id.sensorMisc4,   R.id.sensorMisc5  }; 
    int miscId[]    = { R.id.sensorLimits1, R.id.sensorLimits2, R.id.sensorLimits3, R.id.sensorLimits4, -1                }; 


    // status of strands (lit or not)
    //private TextView mInfoArea;
    private TextView mMainTitle;    
    private TextView mSubTitle;    
    private String mConnectedDeviceName;
    //private String mConnectedDeviceAddr;

    public boolean mConnected = false;
    
    public class SensorInfo
    {
	// widgets that define and visualize the actual sensors
	public ImageView   icon;
	public TextView    value;
	public TextView    limits;
	public TextView    misc;
	public TableLayout table;
	//public RadioButton extra;
	
	// recent values read per sensor
	public boolean     lit;
	public boolean     avail;
	public double      lo;
	public double      hi;
	public double      power;
	public double      temp;
	protected String   addr;
	
	public boolean bound;
	public boolean used;
	public boolean last;
    }
    
    private SensorInfo[] sensors = new SensorInfo[5];
    private long startupMillis;
    //private RadioButton extra;
    
    public void setMainTitle(String s)
    {
        if (null!=mMainTitle) mMainTitle.setText(s);        	
    }
    
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

	setOnClickListener(buttonTemp    = findViewById(R.id.buttonTemp));
	setOnClickListener(buttonPower   = findViewById(R.id.buttonPower));
	setOnClickListener(buttonCost    = findViewById(R.id.buttonCost));
	setOnClickListener(buttonDuty    = findViewById(R.id.buttonDuty));
	setOnClickListener(buttonInfo    = findViewById(R.id.frotectButtonInfo));
	setOnClickListener(buttonUpdate  = findViewById(R.id.frotectButtonUpdate));
	setOnClickListener(buttonConfig  = findViewById(R.id.frotectButtonConfig));
	
	for (int i=0; i<5; i++)
	{
	    SensorInfo s = sensors[i] = new SensorInfo();
	    s.table  = (TableLayout) findViewById(tableId[i]);	    
	    s.icon   = (ImageView)   findViewById(iconId[i]);	    
	    s.value  = (TextView)    findViewById(valueId[i]);
	    s.limits = (TextView)    findViewById(limitsId[i]);
	    s.misc   = (TextView)    findViewById(miscId[i]);
	    s.lit    =  false; 
	    s.bound  =  true; // wild guess: last sstrand/sensor not bound -> updated in updateInfo
	    
	    setOnClickListener(s.table);
	    visualizeSensorStatus(s);
	}
	sensors[4].last = true;
	
	tvLastPing          = (TextView)    findViewById(R.id.textViewPing);
	imgHeartBeat        = (ImageView)   findViewById(R.id.imgHeartBeat);
	frotectPingProgress = (ProgressBar) findViewById(R.id.frotectPingProgress);
	if (null!=frotectPingProgress) frotectPingProgress.setMax(60);
	customizeProgressbar(frotectPingProgress);
	
	enableControls(false);
	setEnabled(buttonCost,  false);
	setEnabled(buttonDuty,  false);
	setEnabled(buttonPower, false);
	setEnabled(buttonTemp,  false);

	Intent intent = new Intent(this, BTFrotectSerialService.class);
	startService(intent);
    }

    
    void customizeProgressbar(ProgressBar progressBar)
    {
	Resources res = getResources();
	progressBar.setProgressDrawable(res.getDrawable( R.drawable.frotect_progress));
    }
    
    public static String colorDecToHex(int p_red, int p_green, int p_blue)
    {
        String red = Integer.toHexString(p_red);
        String green = Integer.toHexString(p_green);
        String blue = Integer.toHexString(p_blue);

        if (red.length() == 1)
        {
            red = "0" + red;
        }
        if (green.length() == 1)
        {
            green = "0" + green;
        }
        if (blue.length() == 1)
        {
            blue = "0" + blue;
        }

        String colorHex = "#" + red + green + blue;
        return colorHex;
    }
    
    private static void setTextColor(TextView tv, int i)
    {
	if (null!=tv) tv.setTextColor(i);
    }
    
    private static void setText(TextView tv, String text)
    {
	if (null!=tv) tv.setText(text);
    }
    
    private static void setVisibile(View v, boolean b)
    {
	if (null!=v) v.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

    private static void setInvisibile(View v)
    {
	setVisibile(v, false);
    }


    private static void setEnabled(View v, boolean e)
    {
	if (null!=v) v.setEnabled(e);
    }

    
    private static void setChecked(RadioButton rb, boolean b)
    {
	if (null!=rb) rb.setChecked(b);
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

	enableControls(false);
	//frotectPingProgress.setProgress(4);
	//frotectPingProgress.setSecondaryProgress(7);

	Intent intent = new Intent(this, BTFrotectSerialService.class);
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
//	Intent intent = new Intent(this, BTFrotectSerialService.class);
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

    public void onStrandClick(View view)
    {
	System.out.println("onStrandClick:" + view);	
	onClick(view);
    }

    
    private void setTitleMsg(String msg)
    {
	setText(mSubTitle, msg);
    }
    
    private long lastPingLocal;
    private long lastUpdateLocal;
    private TextView tvLastPing = null;
    
    private ProgressBar frotectPingProgress = null;

    public StringBuilder helpReceiving   = null;
    public String        helpCompleted   = null;

    public StringBuilder infoReceiving   = null;
    public String        infoCompleted   = null;

    public StringBuilder minMaxReceiving = null;
    public String        minMaxCompleted = null;

    public StringBuilder statsReceiving  = null;
    public String        statsCompleted  = null;

    public StringBuilder sensorsReceiving  = null;
    public String        sensorsCompleted  = null;

    private int numStrands = -1;
    private int numSensors = -1;
    private int avlStrands = -1;
    private double numCost = -1;

    private final FrotectBTDataHandler mHandler = new FrotectBTDataHandler(); 

    interface FrotectBTDataCompleteListener
    {
	enum TYPE { INFO, HELP, STATS, MINMAX, STARTTIMES, STRANDS }; 
	
	void onDataComplete(TYPE type, String data);
    }
    
    class FrotectBTDataHandler extends BTDataHandler
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
	protected void onDeviceName(String deviceName)
        {
	    mConnectedDeviceName = deviceName;
	    //Toast.makeText(getApplicationContext(), "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
        }

	@Override
	protected void onDeviceAddr(String deviceAddr)
        {
	    mmSelectedAddress = deviceAddr;
	    //Toast.makeText(getApplicationContext(), "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
        }

	@Override
        protected void onLineReceived(String receivedLine) { handleCommand(receivedLine); }

	@Override
        protected void onIdle() { setTitleMsg("unconnected"); }

	@Override
        protected void onTimeout() { setTitleMsg("timeout"); }

	@Override
        protected void onConnectingDevice() { setTitleMsg("connecting to " + mConnectedDeviceName); }

	@Override
        protected void onDeviceConnected() 
	{ 
	    setTitleMsg("connected to " + mConnectedDeviceName);
	    mConnection.sendLine("\nS\n"); // dump sensor info 
	    mConnection.sendLine("\nD\n"); // dump all info including strands, min/max and stats
	    mConnection.sendLine("\nH\n"); // request help commands accepted 

	    mConnection.sendLine("\nS\n"); // send twice to make sure we receive it 
	    mConnection.sendLine("\nD\n"); 
	    mConnection.sendLine("\nH\n");
	    
	    enableControls(true);
	    FrotectActivity.this.mConnected = true;
	}

	@Override
        protected void onDeviceDisconnected() 
	{ 
	    setTitleMsg("disconnected");
	    
	    enableControls(false);
	    FrotectActivity.this.mConnected = false;
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
	    doLog("LINE: " + c);
	    try
	    {
		if (c.startsWith("P.")) // ping
		{
		    lastPingLocal = Calendar.getInstance().getTimeInMillis()-startupMillis;
		    
		    c = c.replaceAll("^P.*s=", "");
		    //double ms = Double.parseDouble(c);		    		    

		    int progress = (int)(((lastPingLocal-lastUpdateLocal)/1000)%60);
		    //System.out.println("lastUpdateLocal=" + lastUpdateLocal + ", lastPingLocal=" + lastPingLocal + ", progress=" + progress);

		    setText(tvLastPing, String.format(Locale.ENGLISH, "%d", progress));

		    if (null!=frotectPingProgress)
		    {
//			long l   = (long)Math.round(ms);
//			long div = frotectPingProgress.getMax();
			//int progress = (int)(l%div);
			
			
			frotectPingProgress.setProgress(progress);
		    }
		}
		else if (hasPrefix(c, "UP"))
		{
		    lastUpdateLocal = Calendar.getInstance().getTimeInMillis()-startupMillis;
		    
		    //logErrorOrWarn(c);
		    c = c.replaceAll("^P. *millis=", "");
		    double ms = Double.parseDouble(c);
		    
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
		    addToStats(c);
		}
		else if (c.startsWith("ST!"))
		{
		    finalizeStats(c);
		    checkDataAvailability();
		}
		else if (hasPrefix(c, "SEN")) // min/max temp. history
		{
		    onSensorInfo(c);
		}
		else if (c.startsWith("SEN!")) // end of min/max temp. history
		{		
		    finalizeSensors(c);
		}
		else if (hasPrefix(c, "MM")) // min/max temp. history
		{		
		    addMinMax(c);
		}
		else if (c.startsWith("MM!")) // end of min/max temp. history
		{		
		    finalizeMinMax(c);
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
		    addHelp(c);
		}
		else if (c.startsWith("H!")) // end of help
		{		
		    finalizeHelp(c);
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
		    doLog(c);
		    onStrandInfo(c);  
		}
		else if (c.startsWith("STR!")) // end of strand info 
		{		
		    finalizeSensors(c);
		}
		else if (hasPrefix(c, "F")) // flash strand  
		{
		    onFlashInfo(c);  
		}
		else if (hasPrefix(c, "I")) // info, ":" means complete -> update
		{		
		    addInfo(c);
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
		addToSensors(c);
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

	private void addMinMax(String c)
        {
	    synchronized (this)
	    {
		if (null==minMaxReceiving) minMaxReceiving=new StringBuilder();		
		minMaxReceiving.append(c).append("\n");
	    }	    
        }

	private void finalizeMinMax(String c)
        {
	    synchronized (this)
	    {
		minMaxCompleted = (null==minMaxReceiving) ? null : minMaxReceiving.toString();
		minMaxReceiving = null;

		dispatchDataComplete(TYPE.MINMAX, minMaxCompleted);
	    }	    
        }

	private void addHelp(String c)
        {
	    synchronized (this)
	    {
		if (null==helpReceiving) helpReceiving=new StringBuilder();		
		helpReceiving.append(c.replaceAll("^H.", "")).append("\n");
	    }	    
        }

	private void finalizeHelp(String c)
        {
	    synchronized (this)
	    {
		helpCompleted = (null==helpReceiving) ? null : helpReceiving.toString();
		helpReceiving = null;

		dispatchDataComplete(TYPE.HELP, helpCompleted);
	    }	    
        }
	
        private void addInfo(String c)
        {
	    synchronized(this)
	    {
		if (null==infoReceiving) infoReceiving=new StringBuilder();
		infoReceiving.append(c.replaceAll("^I.", "")).append("\n");
	    }
        }

        private void finalizeInfo()
        {
	    synchronized(this)
	    {
		infoCompleted = (null==infoReceiving) ? null : infoReceiving.toString();
		infoReceiving = null;
		
		Map<String, String> map = Parser.parseInfo(infoCompleted);
		String sV = map.get("Frotect");
		if (!isEmpty(sV))
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
		
		dispatchDataComplete(TYPE.INFO, infoCompleted);
	    }
        }

	private void addToStats(String c)
        {
	    synchronized(this)
	    {
		if (null==statsReceiving) statsReceiving=new StringBuilder();
		statsReceiving.append(c).append("\n");
	    }
        }

	private void finalizeStats(String s)
        {
	    synchronized(this)
	    {
		statsCompleted = (null==statsReceiving) ? null : statsReceiving.toString();
		statsReceiving = null;
		
		dispatchDataComplete(TYPE.STATS, statsCompleted);
	    }	    
        }

	private void addToSensors(String c)
        {
	    synchronized(this)
	    {
		if (null==sensorsReceiving) sensorsReceiving=new StringBuilder();
		sensorsReceiving.append(c).append("\n");
	    }
        }

	private void finalizeSensors(String s)
        {
	    synchronized(this)
	    {
		sensorsCompleted = (null==sensorsReceiving) ? null : sensorsReceiving.toString();
		sensorsReceiving = null;
		
		dispatchDataComplete(TYPE.STRANDS, sensorsCompleted);
	    }	    
        }

	private void onFlashInfo(String c)
	{
	    try
	    {
		String lStr = c.replaceAll(".*lit=", "");
		String nStr = c.replaceAll(",lit.*", "").replaceAll(".*n=", "");

		int n = Integer.parseInt(nStr)-1;
		int l = Integer.parseInt(lStr);

		SensorInfo s = sensors[n];
		s.lit = l>0;
				
		System.out.println("FLASH: " + n + " -> " + s.lit);
		
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
        	HashMap<String, Object> map = Parser.parseStrandInfo(c);
        	// STR: n=1, val=0, lit=0, upd=1, tu=28.50, tl=24.00, temp=?, err=50, last=117965829, ago=0, addr=28:50:81:E1:04:00:00:6E, used=1, avail=1

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

        	doLog(c);
            }
            catch (Exception e)
            {
        	doLog("" + e);
            }
        }

        private void onMeasurement(String c)
        {
	    HashMap<String, Object> map = Parser.parseMeasurement(c);
	    if (null==map)
	    {
	    doLog("PARSE ERROR\n");
	    }
	    else			
	    {
		int strand = (Integer) map.get("s");
		double now = (Double) map.get("now");
		double old = (Double) map.get("old");

		TextView misc = (TextView) findViewById(miscId[strand-1]);
		setText(misc, String.format(Locale.ENGLISH, "%2.1f", now));

		updateInfo(strand, now, old);
	    }
        }
	
	private void logErrorOrWarn(String c)
        {
	    Toast.makeText(getApplicationContext(),c, Toast.LENGTH_LONG).show();
        }

	@SuppressWarnings("unused")
        private void logDebug(String c)
        {
	    Toast.makeText(getApplicationContext(),c, Toast.LENGTH_SHORT).show();
        }

	private void updateInfo(int n, String addr, boolean used, boolean avail, Boolean bound)
	{
	    n=n-1;
	    SensorInfo sensor = sensors[n];
	    
	    sensor.avail = avail;
	    sensor.used  = used;
	    sensor.addr  = addr;
	    if (!sensor.used)
	    {
		setText(sensor.value, "n.a.");
		setText(sensor.limits, "");
		setText(sensor.misc,   "unbound");
	    }
	    visualizeSensorStatus(sensor);
	}
	
	
	private void updateInfo(int n, double t, double last)
	{
	    try
	    {
		SensorInfo sensor = sensors[n-1];
		String value = String.format(Locale.ENGLISH, "%2.1f¡C", t);
		setText(sensor.value, value);	    

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
	    setEnabled(value, avail);
	    
//	    if (sensorValue instanceof RadioButton)
//	    {
//		((RadioButton)sensorValue).setChecked(false);
//	    }
	    
	    SensorInfo sensor = sensors[n];
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
		setText(value,  "n.a.");
		setText(limits, "");
		setText(misc,   "unbound");
	    }	    
	    else if (null==t)
	    {
		setText(value, "?");
	    }
	    else
	    {
		String s = String.format(Locale.ENGLISH, "%2.1f¡C", t);
		setText(value, s);
	    }

	    Long now = Calendar.getInstance().getTimeInMillis();		  
	    Long chg = (Long) value.getTag();
	    if (null==chg || now-chg>2000) 
	    {
		setTextColor(value, Color.WHITE);
	    }
	    else
	    {
		setTextColor(value, Color.YELLOW);
	    }		
	    value.setTag(now);
	    
	    if (!avail)
	    {
		setTextColor(value, used ? Color.RED : Color.GRAY);
	    }	    
	    	    
	    if (!sensor.used || !sensor.bound)
	    {
		setText(value, "unbound");
	    }
	    else 
	    {
		setText(limits, String.format(Locale.ENGLISH, "%2.1f / %2.1f", tl, tu));
	    }
	    
	    if (err>0)
	    {
		setText(misc, "errors: " + err);
	    }
	}
    };

    private void visualizeSensorStatus(SensorInfo sensor)
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

    private void enableControls(boolean b)
    {
	frotectPingProgress.setProgress(0);
	setText(tvLastPing, "not connected");
	setEnabled(frotectPingProgress, b);
	setVisibile(imgHeartBeat, b);
	
	setEnabled(buttonUpdate, true);
		
	for (SensorInfo s : sensors)
	{
	    visualizeSensorStatus(s);
	}
	
	setMainTitle("Frotect");
    }
        
    private void checkDataAvailability()
    {
	boolean haveStats  = statsCompleted!=null && statsCompleted.length()>0;
	boolean haveMinMax = minMaxCompleted!=null && minMaxCompleted.length()>0;
	setEnabled(buttonCost,  haveStats);
	setEnabled(buttonDuty,  haveStats);
	setEnabled(buttonPower, haveStats);
	setEnabled(buttonTemp,  haveMinMax);	
    }

    BTFrotectSerialServiceConnection mConnection = new BTFrotectSerialServiceConnection(mHandler);

    private void nextActivity()
    {
	Intent i = new Intent(getApplicationContext(), GateControlActivity.class);
	startActivity(i);            
	finish();	
    }

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
	    if (null!=mmSelectedAddress)
	    {
		if (null==mmSelectedAddress || mmSelectedAddress.length()<1) return false;
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mmSelectedAddress);
		mConnection.connect(device); // F0:B4:79:07:AE:EE
	    }
	    break;
	case R.id.menu_frotect_next:
	    nextActivity();
	    return true;
	    //	case R.id.menu_water_on:
	    //	    setCmd("P=1");
	    //	    return true;
	    //	case R.id.menu_water_off:
	    //	    setCmd("P=0");
	    //	    return true;
	case R.id.menu_frotect_select:
	    // Launch the PresetsActivity to see devices and do scan
	    Intent serverIntent = new Intent(this, DeviceListActivity.class);
	    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	    return true;
	case R.id.menu_frotect_disconnect:
	    mConnection.disconnect();
	    break;
	    //	case R.id.menu_water_presets:
	    //	    Intent serverIntent2 = new Intent(this, PresetsActivity.class);
	    //	    startActivityForResult(serverIntent2, REQUEST_PRESET);
	    //	    break;
	    //	case R.id.discoverable: // Ensure this device is discoverable by others
	    //          ensureDiscoverable();
	    //          return true;

//	case R.id.menu_frotect_update:
//	    mConnection.sendLine("D"); // command "D"  Dump all info
//	    break;

	default:
	    Toast.makeText(getApplicationContext(), "Unknown option " + item.getItemId(), Toast.LENGTH_SHORT).show();
	}
	return false;
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

    private void setOnClickListener(View v)
    {
	if (null!=v)
	{
	    v.setOnClickListener(this);
	    v.setOnLongClickListener(this);
	}
    }

    public void onClick(View view)
    {
	if (buttonInfo==view)
	{
	    InfoDialog dialog = new InfoDialog(this);
	    mHandler.addDataCompleteListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();	    
	}
	else if (buttonUpdate==view)	
	{
	    if (!mConnected)
	    {
		mHandler.onToast("No connection!");
		return;
	    }
	    mConnection.sendLine("\n\nD\n\n"); // dump all
	}
	else if (buttonPower==view)
	{
	    StatsDialog dialog = new StatsDialog(this, POWER);
	    mHandler.addDataCompleteListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();	    
	}
	else if (buttonTemp==view)
	{
	    StatsDialog dialog = new StatsDialog(this, TEMP);
	    mHandler.addDataCompleteListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();	    
	}
	else if (buttonCost==view)
	{
	    StatsDialog dialog = new StatsDialog(this, COST);
	    mHandler.addDataCompleteListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();	    
	}
	else if (buttonDuty==view)
	{
	    StatsDialog dialog = new StatsDialog(this, DUTY);
	    mHandler.addDataCompleteListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();	    
	}
	else if (buttonConfig==view)
	{
	    ConfigDialog dialog = new ConfigDialog(this);
	    dialog.setOnDismissListener(this);
	    dialog.show();	    
	}
	else
	{
	    int n = findClickedSensor(view);
	    if (-1<n)
	    {
		if (!mConnected)
		{
		    mHandler.onToast("No connection!");
		}
		else if (sensors[n].bound)
		{
		    mConnection.sendLine("I" + (n+1)); // send "identify" command
		}		    		    
	    }
	}
    }
    
    
    private int findClickedSensor(View view)
    {
	for (int i=0; i<5; i++)
	{
	    SensorInfo s = sensors[i];
	    if (s.table==view || s.value==view || s.icon==view)
	    {
		return i;
	    }
	}
	return -1;
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
	    if (resultCode == Activity.RESULT_OK) {
		// Get the device MAC address
		String address = data.getExtras()
			.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BLuetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

		mmSelectedAddress = address;
		//mmSelectedName = device.getName();
		//savePeerInfo();
		//updateSelectedInfo();

		// Attempt to connect to the device
		//mChatService.connect(device);
		mConnection.connect(device);
		//mmBTConnector.connect(device);
	    }
	    break;

	}
    }

    class NumberPicker implements OnClickListener
    {
	private View dec;
	private View inc;
	private TextView value;
	private double min;
	private double max;
	private double step;

	public NumberPicker(Dialog dialog, int decID, int valueID, int incID, double start, double min, double max)
	{
	    this(dialog, decID, valueID, incID, start, min, max, 0.5f);
	}

	public NumberPicker(Dialog dialog, int decID, int valueID, int incID, double start, double min, double max, double step)
        {
	    this.dec   = dialog.findViewById(decID);
	    this.inc   = dialog.findViewById(incID);
	    this.value = (TextView) dialog.findViewById(valueID);

	    this.dec.setOnClickListener(this);
	    this.inc.setOnClickListener(this);
	    this.value.setText(String.format(Locale.ENGLISH, "%-2.1f", start));
	    this.min = min;
	    this.max = max;
	    this.step = step;
        }

	public void onClick(View b)
	{
	    double d = getValue();
	    if (b==dec)
	    {
		d = Math.max(min,d-step);
	    }
	    if (b==inc)
	    {
		d = Math.min(max,d+step);
	    }
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
    }

    class SensorDialog extends Dialog implements BTDataListener, BTConnectionStatusListener, FrotectBTDataCompleteListener, android.view.View.OnClickListener
    {
	private int sensorAddrIDs[]   = { R.id.sensorAddr1,   R.id.sensorAddr2,   R.id.sensorAddr3,   R.id.sensorAddr4,   R.id.sensorAddr5 };
	private int sensorUpIDs[]     = { R.id.sensorUp1,     R.id.sensorUp2,     R.id.sensorUp3,     R.id.sensorUp4,     R.id.sensorUp5   };
	private int sensorDownIDs[]   = { R.id.sensorDown1,   R.id.sensorDown2,   R.id.sensorDown3,   R.id.sensorDown4,   R.id.sensorDown5 };
	// binding functionality moved to strand dialog
	private int sensorBoundsIDs[] = { R.id.tvSensorBound1, R.id.tvSensorBound2, R.id.tvSensorBound3, R.id.tvSensorBound4, R.id.tvSensorBound5 }; 

	AddrInfo infos[] = { new AddrInfo(), new AddrInfo(), new AddrInfo(), new AddrInfo(), new AddrInfo(), };
	
	private View rescan;
	
	private FrotectActivity frotect; 

	class AddrInfo 
	{
	    View up, down; //, unbind;
	    TextView addr;
	    TextView bound;
	    boolean avail;
	    
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
		// binding functionality moved to strand dialog
//		if (null!=unbind)
//		{
//		    unbind.setVisibility(View.VISIBLE);
//		}		
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
		setOnClickListener(rescan = findViewById(R.id.frotectRescan));
		setOnClickListener(infos[i].up     = findViewById(sensorUpIDs[i])  );		
		setOnClickListener(infos[i].down   = findViewById(sensorDownIDs[i]));
		//setOnClickListener(infos[i].unbind = findViewById(sensorUnbindIDs[i]));
		
		infos[i].addr  = (TextView) findViewById(sensorAddrIDs[i]);		
		infos[i].bound = (TextView) findViewById(sensorBoundsIDs[i]);		
		infos[i].setAvailable(false);		

		infos[i].up.setEnabled(mConnected);
		infos[i].down.setEnabled(mConnected);	
		// binding functionality moved to strand dialog
	    }
	}

	void setOnClickListener(View v)
	{
	    if (null!=v) v.setOnClickListener(this);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart() 
	{
	    super.onStart();	    
	    
	    // make sure we will receive sensor info updates after we modified the order of sensors
	    mHandler.addDataListener(this);
	    boolean requestUpdate = true;
	    
	    // if sensor data was already collected, uses this information
	    // TODO: dont reparse sensor info if there are already sensor addresses in "sensors[]"
	    synchronized (frotect)
            {
		if (notEmpty(frotect.sensorsCompleted))
		{
		    parseCompleteSensorData(frotect.sensorsCompleted);
		    requestUpdate = false;
		}
            }
	    
	    // otherwise ask the uC to send sensor data now
	    if (requestUpdate)
	    {
		mConnection.sendLine("\nS\n"); // dump sensor info
		mConnection.sendLine("\nS\n"); // dump sensor info
	    }
	};

	@Override
	protected void onStop()
	{
	    super.onStop();
	    mHandler.removeDataListener(this);
	}

	public void onClick(View v)
	{
	    if (v==rescan) 
	    { 
		confirmRescan();  
		return;  
	    }
	    
	    for (int i=0; i<5; i++)
	    {
		if (v==infos[i].up)   
		{ 
		    mConnection.sendLine("\n-" + (i+1)); // Move up command 
		    mConnection.sendLine("\n");
		    break; 
		}  
		else if (v==infos[i].down) 
		{ 
		    mConnection.sendLine("+" + (i+1)); // Move down command 
		    mConnection.sendLine("\n");
		    break; 
		}
	    }
	}

	private void rescanConfirmed()
	{
	    mConnection.sendLine("\nX\n"); // Rescan sensors command
	}
	
	public void confirmRescan()
	{
	    // 1. Instantiate an AlertDialog.Builder with its constructor
	    AlertDialog.Builder builder = new AlertDialog.Builder(frotect);

	    // 2. Chain together various setter methods to set the dialog characteristics
	    builder.setMessage("Really rescan sensors?").setTitle("Confirmation");

	    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) { rescanConfirmed(); } 
	    });
	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {  /* do nothing */ } 
	    });
	    
	    // 3. Get the AlertDialog from create()
	    AlertDialog dialog = builder.create();
	    dialog.show();
	}

	public void onConnect(String name, String addr)
        {
	    setConnected(true);
        }

	public void onDisconnect(String name, String addr)
        {
	    setConnected(false);
        }
	
	private void setConnected(boolean b)
        {
	    for (AddrInfo i : infos)
	    {
		setEnabled(i.up,   b);
		setEnabled(i.down, b);
		setEnabled(i.addr, b);
	    }
        }
	
	// interface FrotectBTDataCompleteListener	
	public void onDataComplete(TYPE type, String data)
	{
	    if (FrotectBTDataCompleteListener.TYPE.STRANDS==type) 
	    {
		parseCompleteSensorData(data);	    
	    }
	}
	
	// interface BTDataListener	
	public void onLineReceived(String line)
        {
	    // SEN: n=4, @=11;22;00;00;33;00;00;ff,used=01,avail=0
	    if (line.startsWith("SEN: ") || line.startsWith("SEN. "))
	    {
		parseOneLine(line);
	    }
        }

        private void parseOneLine(String line)
        {
            if (isEmpty(line)) return;
            
            try
            {
        	HashMap<String, Object> map = Parser.parseSensorInfo(line);

        	int     num   = (Integer) map.get("n");
        	boolean avail = (Boolean) map.get("avail");
        	String  addr  = (String)  map.get("@");
        	Boolean bound = (Boolean) map.get("bnd");

        	AddrInfo info = infos[num-1];
        	info.addr.setText(addr);
        	setVisibile(info.bound, null!=bound && bound);
        	
        	info.setFound(num-1, avail);
            }
            catch (Exception e)
            {
        	frotect.doLog("parseOneLine: " + e + " on '" + line + "'");
            }
        }

        private void parseCompleteSensorData(String data)	
        {
		
	    StringReader sr = new StringReader(data);
	    BufferedReader br = new BufferedReader(sr);

	    String line = null;
	    do
	    {
		try { line = br.readLine(); } catch (IOException e) {}
		parseOneLine(line);
	    }
	    while (null!=line);
        }
    }

    class StrandDialog extends Dialog implements BTConnectionStatusListener, FrotectBTDataCompleteListener, BTDataListener, android.view.View.OnClickListener
    {
	private Button buttonSave;
	private Button buttonFlash;
	private int strandNo;
	private FrotectActivity frotect;
	private NumberPicker lo;
	private NumberPicker hi;
	private NumberPicker pwr;
	private TextView strandAddress;
	private Button buttonBind;
	private Button buttonUnbind;
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
	    SensorInfo info = sensors[strandNo-1];	    
	    double currLo    = -30;
	    double currHi    =  30;
	    double currPower =   0;
	    if (info.avail)
	    {
		currLo    = info.lo;
		currHi    = info.hi;
		currPower = info.power;
	    }

	    this.lo  = new NumberPicker(this, R.id.decLo,    R.id.valueLo,    R.id.incLo,    currLo, -30, 30); 
	    this.hi  = new NumberPicker(this, R.id.decHi,    R.id.valueHi,    R.id.incHi,    currHi, -30, 30); 
	    this.pwr = new NumberPicker(this, R.id.decPower, R.id.valuePower, R.id.incPower, currPower, 0, 200); 
	    this.bound = info.bound;

	    setListener(this.buttonSave   = (Button) findViewById(R.id.buttonThresSave));
	    setListener(this.buttonFlash  = (Button) findViewById(R.id.buttonStrandFlash));	    	    
	    setListener(this.buttonBind   = (Button) findViewById(R.id.buttonStrandBind));
	    setListener(this.buttonUnbind = (Button) findViewById(R.id.buttonStrandUnbind));
	    
	    setInvisibile(findViewById(bound ? R.id.layoutStrandBind : R.id.layoutStrandUnbind));
	    
	    setConnected(mConnected);	    
	    setText(strandAddress, info.addr);
        }

	private void setListener(Button b)
	{
	    b.setOnClickListener(this);
	}

	public void onClick(View v)
	{
	    if (buttonSave==v)
	    {		
		String cmd1 = String.format(Locale.ENGLISH, "L%d=%2.1f", strandNo, lo.getValue());
		String cmd2 = String.format(Locale.ENGLISH, "U%d=%2.1f", strandNo, hi.getValue());
		String cmd3 = String.format(Locale.ENGLISH, "P%d=%2.1f", strandNo, pwr.getValue());
		frotect.mConnection.sendLine(cmd1);
		frotect.mConnection.sendLine(cmd2);
		frotect.mConnection.sendLine(cmd3);
		dismiss();
	    }
	    else if (buttonFlash==v)
	    {
		frotect.mConnection.sendLine("I" + strandNo);
	    }
	    else if (buttonBind==v)
	    {
		frotect.mConnection.sendLine("\nG" + strandNo);  // re-bind command
		frotect.mConnection.sendLine("\nS\n" + strandNo);
	    }
	    else if (buttonUnbind==v)
	    {
		frotect.mConnection.sendLine("\nF" + strandNo);
		frotect.mConnection.sendLine("\nS\n" + strandNo);
	    }
	}

	public void onConnect(String name, String addr)
        {
	    setConnected(true);
        }

	public void onDisconnect(String name, String addr)
        {
	    setConnected(false);
        }
	
	private void setConnected(boolean b)
	{
	    setEnabled(buttonSave, b);
	    setEnabled(buttonFlash, b);
	    setEnabled(buttonBind, b);
	    setEnabled(buttonUnbind, b);
	    lo.setEnabled(b);
	    hi.setEnabled(b);
	    pwr.setEnabled(b);	    
	}

	public void onDataComplete(TYPE type, String data)
        {
	    if (type==TYPE.STRANDS)
	    {		
		refreshControls();
	    }
        }

	public void onLineReceived(String line)
        {
	    if (line.startsWith("STR"))
	    {
		refreshControls();
	    }
        }
    }

    class ConfigDialog extends Dialog implements android.view.View.OnClickListener
    {
	private NumberPicker cost;
	private NumberPicker strands;
	private NumberPicker sensors;
	private View saveCost;
	private View saveStrands;
	private View saveSensors;
	private FrotectActivity frotect;
	private View reboot;

	protected ConfigDialog(FrotectActivity frotect)
	{
	    super(frotect);
	    
	    this.frotect = frotect;
	    LayoutInflater inflater = this.getLayoutInflater();
	    setContentView(inflater.inflate(R.layout.dialog_config, null));	    
	    setTitle("Config ");
	    
	    this.cost    = new NumberPicker(this, R.id.decCost,       R.id.valueCost,       R.id.incCost,       20, 0, 100, 0.25);
	    this.strands = new NumberPicker(this, R.id.decNumStrands, R.id.valueNumStrands, R.id.incNumStrands,  4, 1,   4, 1.00);
	    this.sensors = new NumberPicker(this, R.id.decNumStrands, R.id.valueNumStrands, R.id.incNumStrands,  4, 1,   4, 1.00);
	    
	    this.addListener(this.saveCost   = findViewById(R.id.saveCost));
	    this.addListener(this.saveStrands = findViewById(R.id.saveNumStrands));
	    this.addListener(this.saveSensors = findViewById(R.id.saveNumSensors));
	    this.addListener(this.reboot      = findViewById(R.id.frotectReboot));
	}

	private void addListener(View view)
        {
	    if (null!=view) view.setOnClickListener(this);
        }

	public void onClick(View view)
        {
	    if (view==saveCost)
	    {
		mConnection.sendLine("\nC=" + cost.getValue());
	    }
	    else if (view==saveStrands)
	    {
		mConnection.sendLine("\nN=" + ((int)strands.getValue()));		
	    }
	    else if (view==saveSensors)
	    {
		mConnection.sendLine("\nM=" + ((int)sensors.getValue()));				
	    }
	    else if (view==reboot)
	    {
		confirmReboot();
	    }
        }
	
	private void rebootConfirmed()
	{
	    mConnection.sendLine("\nB=4711\n");				
	}
	
	public void confirmReboot()
	{
	    // 1. Instantiate an AlertDialog.Builder with its constructor
	    AlertDialog.Builder builder = new AlertDialog.Builder(frotect);

	    // 2. Chain together various setter methods to set the dialog characteristics
	    builder.setMessage("Really reboot µC?").setTitle("Confirmation");

	    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) { rebootConfirmed(); } 
	    });
	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {  /* do nothing */ } 
	    });
	    
	    // 3. Get the AlertDialog from create()
	    AlertDialog dialog = builder.create();
	    dialog.show();
	}

    }
    
    class InfoDialog extends Dialog implements FrotectBTDataCompleteListener, android.view.View.OnClickListener
    {
	private FrotectActivity frotect;
	private View buttHelp;
	private View buttInfo;
	private View buttMinMax;
	private View buttStats;
	private View buttSend; 
	private TextView tvInfo;
	private FrotectBTDataCompleteListener.TYPE mType = TYPE.INFO;
	private ContextMenu menu;
	private boolean autoscroll;

	protected InfoDialog(FrotectActivity frotect)
	{
	    super(frotect);
	    this.frotect = frotect;
	    LayoutInflater inflater = this.getLayoutInflater();
	    setContentView(inflater.inflate(R.layout.dialog_info, null));
	    setTitle("Info ");	    
	}

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

	@Override
	public void show()
	{
	    super.show();
	    
	    setListener(this.buttHelp   = findViewById(R.id.buttonInfoHelp));	    
	    setListener(this.buttInfo   = findViewById(R.id.buttonInfoGeneral));
	    setListener(this.buttMinMax = findViewById(R.id.buttonInfoMinMax));
	    setListener(this.buttStats  = findViewById(R.id.buttonInfoStats));
	    setListener(this.buttSend   = findViewById(R.id.buttonSendInfoText));

	    tvInfo = (TextView) findViewById(R.id.frotectInfoView);

	    setText(tvInfo, frotect.helpCompleted);
	    mType = TYPE.INFO;
	}

	private void setListener(View v)
	{
	    if (null!=v) v.setOnClickListener(this);
	}

	public void onClick(View v)
	{
	    if (buttHelp==v)
	    {		
		tvInfo.setText("" + frotect.helpCompleted);
		mType = TYPE.HELP;
	    }
	    else if (buttInfo==v)
	    {
		tvInfo.setText("" + frotect.infoCompleted);
		mType = TYPE.INFO;
	    }
	    else if (buttMinMax==v)
	    {
		tvInfo.setText("" + frotect.minMaxCompleted);
		mType = TYPE.MINMAX;
	    }
	    else if (buttStats==v)
	    {
		tvInfo.setText("" + frotect.statsCompleted);
		mType = TYPE.STATS;
	    }
	    else if (buttSend==v)
	    {
		sendLog();
	    }
	}
	// InfoDialog.onDataComplete(...) will be called by FrotectBTDataHandler whenever there is new data available
	public void onDataComplete(TYPE type, String data)
	{
	    if (type==mType)
	    {
		setText(tvInfo, data);
	    }	    
	}
    }

    public boolean onLongClick(View view)
    {
	int n = findClickedSensor(view);	
	if (n<0) return false;
	
	// not a strand but the extra unbound sensor -> open sensor dialog if connected
	if (n>=4)
	{
	    if (!mConnected)
	    {
		mHandler.onToast("No connection!");
		return true;
	    }
	    
	    SensorDialog dialog = new SensorDialog(this);
	    mHandler.addConnectionStatusListener(dialog);
	    mHandler.addDataListener(dialog);
	    dialog.setOnDismissListener(this);
	    dialog.show();
	    return true;
	}
	
	int strand = n+1;
	StrandDialog dialog = new StrandDialog(this, strand);
	mHandler.addConnectionStatusListener(dialog);
	dialog.setOnDismissListener(this);
	dialog.show();
	return false;
    }


    public void onDismiss(DialogInterface dialog)
    {
	if (dialog instanceof BTDataListener)
	{
	    mHandler.removeDataListener((BTDataListener)dialog);
	}
	if (dialog instanceof BTConnectionStatusListener)
	{	    
	    mHandler.removeConnectionStatusListener((BTConnectionStatusListener)dialog);
	}	    	
	if (dialog instanceof FrotectBTDataCompleteListener)
	{	    
	    mHandler.removeDataCompleteListener((FrotectBTDataCompleteListener)dialog);
	}	    	
    }
    
    private static boolean isEmpty(String s)
    {
	return null==s || s.length()<1;
    }
    
    private static boolean notEmpty(String s)
    {
	return !isEmpty(s);
    }

    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
    {
	System.out.println("onCreateContextMenu");
    }
    
}


