
package com.apdlv.ilibaba.util;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

public class OnClickHelper 
{
    public static void registerViews(Activity act, OnClickListener listener, int ... ids)
    {
	for (int id : ids)
	{
	    View v = act.findViewById(id);
	    if (null!=v) v.setOnClickListener(listener);		    
	}
    }

    public static void registerViewsLong(Activity act, OnLongClickListener listener, int ... ids)
    {
	for (int id : ids)
	{
	    View v = act.findViewById(id);
	    if (null!=v) v.setOnLongClickListener(listener);		    
	}
    }

    public static void registerViews(Activity act, OnClickListener listener, View ... views)
    {
	for (View v : views)
	{
	    v.setOnClickListener(listener);		    
	}
    }

    public static void registerViewsLong(Activity act, OnLongClickListener listener, View ... views)
    {
	for (View v : views)
	{
	    v.setOnLongClickListener(listener);		    
	}
    }
}
