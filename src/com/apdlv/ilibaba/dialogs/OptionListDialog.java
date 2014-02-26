package com.apdlv.ilibaba.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.apdlv.ilibaba.R;

public abstract class OptionListDialog extends Dialog implements OnItemClickListener, OnItemLongClickListener
{
    public OptionListDialog(Activity activity)
    {
	super(activity);
	this.activity = activity;
    }

    public OptionListDialog(Activity activity, int theme)
    {
	super(activity, theme);
	this.activity = activity;
    }

    public OptionListDialog(Activity activity, boolean cancelable, OnCancelListener cancelListener)
    {
	super(activity, cancelable, cancelListener);
	this.activity = activity;
    }
    
    protected Activity getActivity()
    {
	return activity;
    }
    

    protected void addOption(String text)
    {
	mArrayAdapter.add(text);
    }
    
    protected void addOptions(String ... texts)
    {
	for (String t : texts)
	{
	    addOption(t);
	}
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(getListLayoutResID());
        
        Context context = getContext();
        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mArrayAdapter = new ArrayAdapter<String>(context, getOptionLayoutResID());
    
        // Find and set up the ListView for the option elements
        ListView optionListView = (ListView) findViewById(getListViewResID());
        optionListView.setAdapter(mArrayAdapter);
        optionListView.setOnItemClickListener(this);    
        optionListView.setOnItemLongClickListener(this);    
    }
    
    protected int getListLayoutResID()
    {
	return R.layout.options_list;
    }

    protected int getListViewResID()
    {
	return R.id.options_list_view;
    }

    
    abstract protected int  getOptionLayoutResID();    
    abstract public void    onItemClick(AdapterView<?> av, View v, int selectedIndex, long arg3);
    
    public boolean onItemLongClick(AdapterView<?> av, View v, int selectedIndex, long arg3) { return false; }

    private Activity             activity;
    private ArrayAdapter<String> mArrayAdapter;
}

