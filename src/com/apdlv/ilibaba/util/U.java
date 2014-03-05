package com.apdlv.ilibaba.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
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
        if (null!=v) v.setVisibility(View.VISIBLE);
    }

    public static void setVisible(View v, boolean b)
    {
        if (null!=v) v.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

    public static void setInvisibile(View v)
    {
        if (null!=v) setVisible(v, false);
    }

    public static void setEnabled(View v, Boolean e)
    {
        if (null!=v && null!=e) v.setEnabled(e);
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
    
    public static void setText(TextView view, int resId)
    {
	if (null!=view) view.setText(resId);
    }

    public static void setProgress(ProgressBar pBar, int progress)
    {
	if (null!=pBar) pBar.setProgress(progress);
    }

    public static void setImageResource(ImageView iv, int id)
    {
	if (null!=iv) iv.setImageResource(id);	
    }

    public static boolean startsWith(String c, String prefix)
    {
	return null!=c && null!=prefix && c.startsWith(prefix);
    }

    public static void setText(TextView tv, String text, int color)
    {
	setText(tv, text);
	setTextColor(tv, color);
    }

    public static void check(RadioGroup rg, int id)
    {
	if (null!=rg && id>-1) rg.check(id);
    }

    public static Integer toInt(String s)
    {
	return isEmpty(s) ? null  : Integer.parseInt(s);
    }

    public static Double toDouble(String s)
    {
	return isEmpty(s) ? null  : Double.parseDouble(s);
    }

    public static void removeView(View v)
    {
	if (null==v) return;
	ViewGroup parent = (ViewGroup) v.getParent();
	if (null==parent) return;
	parent.removeView(v);
    }

    public static void setOnCheckedChangeListener(CompoundButton b, OnCheckedChangeListener l)
    {
	if (null!=b && null!=l) b.setOnCheckedChangeListener(l);
    }
    
    public static String toStacktrace(Exception e)
    {
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    e.printStackTrace(pw);
	    String msg = sw.toString();
	    return msg;
    }

    public static void sleep(long ms)
    {
	try { Thread.sleep(ms); } catch (Exception e) {}	
    }

}
