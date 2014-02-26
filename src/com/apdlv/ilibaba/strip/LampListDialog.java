
package com.apdlv.ilibaba.strip;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.dialogs.OptionListDialog;


public class LampListDialog extends OptionListDialog  
{    
    
    public LampListDialog(StripControlActivity activity, String lamps[])
    {
	super(activity);
	this.lamps = lamps;
    }

    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    
        if (lamps.length>1)
        {
            for (int i=0; i<lamps.length; i++)
            {        	
                String desc = i<lamps.length ? lamps[i] : "Circuit " + (i+1);
                desc = desc.replaceAll("_", " ");
                addOption(desc);
            }
        }
        else
        {
            addOptions("\nOff\n", "\nOn\n", "\nToggle\n");
        }        
    }

    
    @Override // implements abstract method from superclass
    public void onItemClick(AdapterView<?> av, View v, int selectedIndex, long l) 
    {
	((StripControlActivity)getActivity()).setCmd(createCommand(selectedIndex));            
    }

    
    @Override 
    public boolean onItemLongClick(AdapterView<?> av, View v, int selectedIndex, long l) 
    {
	onItemClick(av, v, selectedIndex, l);
	this.dismiss();
	return true;           
    }

    
    @Override // implements abstract method from superclass
    protected int getOptionLayoutResID()
    {
	return R.layout.option_lamp;
    }

    
    private String createCommand(int selectedIndex)
    {
	if (lamps.length>1)
	{
	    int no = selectedIndex;
	    return "P" + no + "=2"; // send toggle command
	}
	else
	{
	    int state = selectedIndex; // 0=off, 1=on, 2=toggle
	    return "P=" + state; // send toggle command
	}
    }

    private String[] lamps;
}
