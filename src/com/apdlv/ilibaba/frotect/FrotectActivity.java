package com.apdlv.ilibaba.frotect;

import java.util.HashMap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.TempActivity;
import com.apdlv.ilibaba.gate.BluetoothSerialService;
import com.apdlv.ilibaba.gate.DeviceListActivity;
import com.apdlv.ilibaba.gate.GateControlActivity;
import com.apdlv.ilibaba.strip.BluetoothConnector;

public class FrotectActivity extends Activity implements OnClickListener
{

    public static final String DUTY = "duty";
    public static final String COST = "cost";
    public static final String TEMP = "temp";
    public static final String POWER = "power";
    protected static final String TAG = "FrotectActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothConnector mmBTConnector;

    private static final int REQUEST_ENABLE_BT = 2;
    //private static final int REQUEST_PRESET    = 3;
    private static final int REQUEST_CONNECT_DEVICE = 1;

    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_frotect);
	
	mLogView = (TextView) findViewById(R.id.frotectLogView);
	if (null!=mLogView)	   
	{
	    mLogView.setMaxLines(1000);
	    mLogView.setMovementMethod(ScrollingMovementMethod.getInstance());
	    Scroller scroller = new Scroller(mLogView.getContext());
	    mLogView.setScroller(scroller);	
	}

	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	// If the adapter is null, then Bluetooth is not supported
	if (mBluetoothAdapter == null) {
	    Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
	    finish();
	    return;
	}

	mmBTConnector = new BluetoothConnector(getApplicationContext(), mHandler);
	
	setOnClickListener(buttonTemp  = (Button) findViewById(R.id.buttonTemp));
	setOnClickListener(buttonPower = (Button) findViewById(R.id.buttonPower));
	setOnClickListener(buttonCost = (Button) findViewById(R.id.buttonCost));
	setOnClickListener(buttonDuty = (Button) findViewById(R.id.buttonDuty));
	
	
    }
    
    private void setOnClickListener(Button button)
    {
	if (null!=button) button.setOnClickListener(this);
    }

    Button buttonTemp;
    Button buttonPower;
    Button buttonCost;
    Button buttonDuty;

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
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mmSelectedAddress);
		mmBTConnector.connect(device);
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
	    mmBTConnector.disconnect();
	    break;
//	case R.id.menu_water_presets:
//	    Intent serverIntent2 = new Intent(this, PresetsActivity.class);
//	    startActivityForResult(serverIntent2, REQUEST_PRESET);
//	    break;
	    //	case R.id.discoverable: // Ensure this device is discoverable by others
	    //          ensureDiscoverable();
	    //          return true;
	default:
	    Toast.makeText(getApplicationContext(), "Unknown option " + item.getItemId(), Toast.LENGTH_SHORT).show();
	}
	return false;
    }

    
    private final Handler mHandler = new Handler() 
    {
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

		//enableControls(false);
		switch (msg.arg1) {
		case BluetoothSerialService.STATE_CONNECTED:
		    doLog("STATE_CONNECTED");
		    setTitleMessage("connected to " + mConnectedDeviceName);
		    //setCmd("H"); // send hello commands
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

	private void handleCommand(String c)
        {
	    if (c.startsWith("D:")) // debug
	    {
		doLog(c);
	    }
	    else if (c.startsWith("MAGIC:"))
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("SH"))  // per hour stats
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("SD")) // per day
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("SW")) // per day
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("S.")) // end of stats
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("HIS")) // history
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("ERROR:")) // error
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("DEACT:")) // 
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("H:")) // help
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("I:")) // help
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("WARN:")) // help
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("CMD")) // help
	    {		
		doLog(c);
	    }
	    else if (c.startsWith("STR:")) // strand ifo 
	    {		
		// STR: n=1, val=0, lit=0, upd=1, tu=28.50, tl=24.00, t=?, err=50, last=117965829, ago=0, addr=28:50:81:E1:04:00:00:6E, used=1, avail=1
		
		c = c.substring(4);
		String split[] = c.split(",");
		HashMap<String, String> map = new HashMap<String, String>();
		for (String kv : split)
		{
		    kv=kv.trim();
		    String pair[] = kv.split("=");
		    map.put(pair[0].trim(), pair[1].trim());
		}
		
		int   n   = Integer.parseInt(map.get("n"));
		int   lit = Integer.parseInt(map.get("lit"));
		float tu = Float.parseFloat(map.get("tu"));
		float tl = Float.parseFloat(map.get("tl"));
		float t = Float.parseFloat(map.get("t"));
		int   err = Integer.parseInt(map.get("err"));
		long  last = Long.parseLong(map.get("err"));
		int   avail = Integer.parseInt(map.get("avail"));
		
		updateInfo(n, avail, lit, tu, tl, t, err, last);
		
		doLog(c);
	    }
	    
        }

	private void updateInfo(int n, int avail, int lit, float tu, float tl, float t, int err, long last)
        {
	    int sensorId[]   = { R.id.sensor1, R.id.sensor2, R.id.sensor3, R.id.sensor4, R.id.sensor5 };
	    int tempId  []   = { R.id.temp1, R.id.temp2, R.id.temp3, R.id.temp4, R.id.temp5 };
	    //int lastId[]     = { R.id.last1, R.id.last2, R.id.last3, R.id.last4, R.id.last5 };
	    int loId[]       = { R.id.lo1, R.id.lo2, R.id.lo3, R.id.lo4, -1 };
	    //int hiId[]       = { R.id.hi1, R.id.hi2, R.id.hi3, R.id.hi4, -1 };
	    int errorsId[]   = { R.id.errors1, R.id.errors2, R.id.errors3, R.id.errors4, R.id.errors4 };
	    
	    n=n-1;
	    RadioButton sensor = (RadioButton) findViewById(sensorId[n]);
	    TextView temp = (TextView) findViewById(tempId[n]);
	    //TextView tvLast = (TextView) findViewById(lastId[n]);
	    TextView lo = (TextView) findViewById(loId[n]);
	    //TextView hi = (TextView) findViewById(hiId[n]);
	    TextView errors = (TextView) findViewById(errorsId[n]);
	    
	    //setText(sensor, String.format("%2.1f¡C", t));
	    sensor.setSelected(1==lit);
	    sensor.setEnabled(1==avail);
	    setText(temp,   String.format("%2.1f¡C", t));
	    //setText(hi, String.format("hi: %2.1f¡C", tu));
	    setText(lo, String.format("limits: %2.1f¡C / %2.1f¡C", tl, tu));
	    setText(errors, "errors: " + err);
	    //setText(tvLast, "last: " + last);
        }

	private void setText(TextView tv, String text)
        {
	    if (null==tv) return;
	    tv.setText(text);
	    
        }

	private void enableControls(boolean b)
        {
	    // TODO Auto-generated method stub
	    
        }

	private void setTitleMessage(String string)
        {
	    // TODO Auto-generated method stub
	    
        }

	private void doLog(String string)
        {
	    // TODO Auto-generated method stub
	    
        }
    };

    
    private void nextActivity()
    {
	Intent i = new Intent(getApplicationContext(), GateControlActivity.class);
	startActivity(i);            
	finish();	
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

    void setCmd(String s)
    {
	//mTextCommand.setText(s);
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

    private TextView mLogView;

    private String receivedLine = "";
    private String mmSelectedAddress;
    private String mmSelectedName;


    public void onClick(View button)
    {
	if (buttonPower==button)
	{
	    Intent i = new Intent(getApplicationContext(), TempActivity.class);
	    i.setAction(POWER);
	    startActivity(i);            	    
	}
	if (buttonTemp==button)
	{
	    Intent i = new Intent(getApplicationContext(), TempActivity.class);
	    i.setAction(TEMP);
	    startActivity(i);            
	}
	if (buttonCost==button)
	{
	    Intent i = new Intent(getApplicationContext(), TempActivity.class);
	    i.setAction(COST);
	    startActivity(i);            
	}
	if (buttonDuty==button)
	{
	    Intent i = new Intent(getApplicationContext(), TempActivity.class);
	    i.setAction(DUTY);
	    startActivity(i);            
	}
    }

}


