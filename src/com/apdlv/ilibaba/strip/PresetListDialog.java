package com.apdlv.ilibaba.strip;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.dialogs.OptionListDialog;

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
    }

    
    @Override
    public boolean onItemLongClick(AdapterView<?> av, View v, int selectedIndex, long arg3) 
    {  
	((StripControlActivity)getActivity()).setCmd("W=" + selectedIndex);            		
	return true;
    }
    
    @Override // implements abstract method from superclass
    protected int getOptionLayoutResID()
    {
	return R.layout.option_preset;
    }

    private String[] presets;
}
