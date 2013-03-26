package com.apdlv.ilibaba;

import java.util.Calendar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.apdlv.ilibaba.color.ColorPickerCircView;
import com.apdlv.ilibaba.color.OnColorChangedListener;
import com.apdlv.ilibaba.shake.Shaker;
import com.apdlv.ilibaba.shake.Shaker.Callback;

public class WaterstripActivity extends Activity implements OnSeekBarChangeListener, OnColorChangedListener, OnClickListener, Callback
{
    private TextView mTextCommand;
    private ColorPickerCircView mColorPicker;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mTitle;

    private static final int REQUEST_ENABLE_BT = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	
	//requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        if (null!=mTitle) mTitle.setText(R.string.view_name_water);

        setContentView(R.layout.activity_waterstrip);

	((SeekBar) findViewById(R.id.seekBright)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekAmplitude)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekRand)).setOnSeekBarChangeListener(this);

	// these are vertical ones:
	((SeekBar) findViewById(R.id.seekSpeed)).setOnSeekBarChangeListener(this);
	((SeekBar) findViewById(R.id.seekFade)).setOnSeekBarChangeListener(this);

	//(mButtonOff = (Button) findViewById(R.id.buttonWaterOff)).setOnClickListener(this);
	mTextCommand   = (TextView) findViewById(R.id.textCommand);

	mColorPicker = (ColorPickerCircView) findViewById(R.id.colorPickerCirc);
	if (null!=mColorPicker)
	{
	    mColorPicker.setOnColorChangedListener(this);
	}	

	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	// If the adapter is null, then Bluetooth is not supported
	if (mBluetoothAdapter == null) {
	    Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
	    finish();
	    return;
	}
	
	//Shaker shaker = new Shaker(this, 1.25d, 500, this);
	Shaker shaker = new Shaker(this, 2*1.25d, 500, this);
	
	mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	activityStarted = Calendar.getInstance().getTimeInMillis();
    }

    Vibrator mVibrator;
    long activityStarted;

    @Override
    protected void onStart()
    {
	super.onStart();
	
	if (!mBluetoothAdapter.isEnabled()) 
	{
	    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	    // Otherwise, setup the chat session
	} else {
	    //if (mChatService == null) setupChat();
	}
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.activity_waterstrip, menu);
	return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) 
    {
	switch (item.getItemId()) {
	case R.id.menu_water_next:
	    nextActivity();
	    return true;
	case R.id.menu_water_onoff:
	    mTextCommand.setText("STATE=toggle");
	    return true;
	}
	return false;
    }

    
    private void nextActivity()
    {
	Intent i = new Intent(getApplicationContext(), BluetoothHC05.class);
	startActivity(i);            
	finish();	
    }
    

    public void onProgressChanged(SeekBar s, int progress, boolean fromUser)
    {
	String command = null==s.getTag() ? null : "" + s.getTag() + "=" + s.getProgress();	
	if (null!=command)
	{
	    mTextCommand.setText(command);
	}
    }

    public void onStartTrackingTouch(SeekBar seekBar)
    {
	// TODO Auto-generated method stub	
    }

    public void onStopTrackingTouch(SeekBar seekBar)
    {
	// TODO Auto-generated method stub	
    }

    public void colorChanged(int color)
    {
	mTextCommand.setText(String.format("RGB=%06x", color & 0xFFFFFF));	
    }

    public void colorChanging(int color)
    {
	mTextCommand.setText(String.format("RGB=%06x", color & 0xFFFFFF));		
    }

    public void onClick(View v)
    {
	if (v instanceof Button)
	{
	    Button b = (Button)v;
	    if (b==v)
	    {
		mTextCommand.setText("STATE=toggle");
	    }
	}
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
	}
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
}
