package com.apdlv.ilibaba.strip;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.dialogs.OptionListDialog;

 
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

public class ModeListDialog extends OptionListDialog
{

    public ModeListDialog(StripControlActivity activity, String[] modes)
    {
	super(activity);
	this.modes = modes;
    }

    
    @Override
    protected void onCreate(Bundle savedInstanceState)    
    {
	super.onCreate(savedInstanceState);
	addOptions(modes);
    }


    @Override // implements abstract method from superclass
    protected int getOptionLayoutResID()
    {
	return R.layout.option_mode;
    }

    
    @Override // implements abstract method from superclass
    public void onItemClick(AdapterView<?> av, View v, int selectedIndex,  long arg3)
    {
	((StripControlActivity)getActivity()).setSelectedMode(selectedIndex, modes[selectedIndex]);
    }
    

    @Override 
    public boolean onItemLongClick(AdapterView<?> av, View v, int selectedIndex, long l) 
    {
	onItemClick(av, v, selectedIndex, l);
	this.dismiss();
	return true;           
    }

    
    private String[] modes;
}
