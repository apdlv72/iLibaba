package com.apdlv.ilibaba.strip;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.dialogs.OptionListDialog;
import com.apdlv.ilibaba.gate.GateControlActivity;
import com.apdlv.ilibaba.util.LastStatusHelper;

public class PresetListDialog extends OptionListDialog
{

    public PresetListDialog(StripControlActivity activity, String[] presets)
    {
	super(activity);
	this.presets = presets;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addOptions(presets);
    }

    
    @Override // implements abstract method from superclass
    public void onItemClick(AdapterView<?> av, View v, int selectedIndex, long arg3) 
    {
	((StripControlActivity)getActivity()).setCmd("L=" + selectedIndex);
	this.dismiss();
    }

    
    @Override
    public boolean onItemLongClick(AdapterView<?> av, View v, final int selectedIndex, long arg3) 
    {
	final PresetListDialog parent = this;
	//Ask the user if they want to quit
	new AlertDialog.Builder(getActivity())
	.setIcon(android.R.drawable.ic_dialog_alert)
	.setTitle("Save")
	.setMessage("Do you want to save this preset?")
	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialog, int which) {
		((StripControlActivity)getActivity()).setCmd("W=" + selectedIndex);            		
		parent.dismiss();    
	    }
	})
	.setNegativeButton("No", null)
	.show();
	
	this.dismiss();
	return true;
    }
    
    @Override // implements abstract method from superclass
    protected int getOptionLayoutResID()
    {
	return R.layout.option_preset;
    }

    private String[] presets;
}
