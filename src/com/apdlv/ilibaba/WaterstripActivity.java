package com.apdlv.ilibaba;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.apdlv.ilibaba.color.ColorPickerCircView;
import com.apdlv.ilibaba.color.OnColorChangedListener;
import com.apdlv.ilibaba.views.VerticalSeekBar;

public class WaterstripActivity extends Activity implements OnSeekBarChangeListener, OnColorChangedListener, OnClickListener
{

    private VerticalSeekBar mSeekBright;
    private SeekBar mSeekStrength;
    private SeekBar mSeekRandom;
    private SeekBar mSeekSpeed;
    private SeekBar mSeekFade;
    private TextView mTextCommand;
    private ColorPickerCircView mColorPicker;
    private Button mButtonOff;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_waterstrip);

	(mSeekBright   = (VerticalSeekBar) findViewById(R.id.verticalSeekBright)).setOnSeekBarChangeListener(this);
	(mSeekStrength = (SeekBar) findViewById(R.id.seekStrength)).setOnSeekBarChangeListener(this);
	(mSeekRandom   = (SeekBar) findViewById(R.id.seekRandom)).setOnSeekBarChangeListener(this);
	(mSeekSpeed    = (SeekBar) findViewById(R.id.seekSpeed)).setOnSeekBarChangeListener(this);
	(mSeekFade     = (SeekBar) findViewById(R.id.seekFade)).setOnSeekBarChangeListener(this);

	(mButtonOff = (Button) findViewById(R.id.buttonWaterOff)).setOnClickListener(this);

	mTextCommand   = (TextView) findViewById(R.id.textCommand);

	mColorPicker = (ColorPickerCircView) findViewById(R.id.colorPickerCirc);
	if (null!=mColorPicker)
	{
	    mColorPicker.setOnColorChangedListener(this);
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
	    Intent i = new Intent(getApplicationContext(), BluetoothHC05.class);
	    startActivity(i);            
	    finish();
	    return true;
	case R.id.menu_water_onoff:
	    mTextCommand.setText("STATE=toggle");
	    return true;
	}
	return false;
    }


    public void onProgressChanged(SeekBar s, int progress, boolean fromUser)
    {
	String command = null;
	if (mSeekBright==s)
	{
	    command = "BRI=" + s.getProgress();
	}
	else if (mSeekFade==s)
	{
	    command = "FAD=" + s.getProgress();

	}
	else if (mSeekRandom==s)
	{
	    command = "RND=" + s.getProgress();

	}
	else if (mSeekSpeed==s)
	{
	    command = "SPD=" + s.getProgress();

	}
	else if (mSeekStrength==s)
	{
	    command = "STR=" + s.getProgress();	    
	}

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

}
