package com.apdlv.ilibaba.util;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Checkable;
import android.widget.TextView;

/** utils */
public class U
{
    public static void setTextColor(TextView tv, int i)
    {
        if (null!=tv) tv.setTextColor(i);
    }

    public static void setText(TextView tv, String text)
    {
        if (null!=tv) tv.setText(text);
    }

    public static void setVisible(View v)
    {
        setVisible(v, true);
    }

    public static void setVisible(View v, boolean b)
    {
        if (null!=v) v.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

    public static void setInvisibile(View v)
    {
        setVisible(v, false);
    }

    public static void setEnabled(View v, boolean e)
    {
        if (null!=v) v.setEnabled(e);
    }

    public static void setChecked(Checkable c, boolean b)
    {
        if (null!=c) c.setChecked(b);
    }

    public static boolean isEmpty(String s)
    {
        return null==s || s.length()<1;
    }

    public static boolean notEmpty(String s)
    {
        return !isEmpty(s);
    }

    
    public static void setEnabled(boolean enabled, MenuItem ... items)
    {
	for (MenuItem i : items)
	{
	    if (null!=i) i.setEnabled(enabled);
	}
    }

    public static void setEnabled(boolean enabled, Menu menu, int ... ids)
    {
	if (null==menu) return;
	for (int id : ids)
	{
	    MenuItem mi = menu.findItem(id);
	    if (null!=mi) mi.setEnabled(enabled);
	}
    }
    
    public static void setCursorVisible(TextView view, boolean visible)
    {
	if (null!=view) view.setCursorVisible(visible);
    }
    
    public static void setText(View view, String text)
    {
	if (null!=view) setText(view, text);
    }
    
    public static void setText(View view, int resId)
    {
	if (null!=view) setText(view, resId);
    }
    

}
