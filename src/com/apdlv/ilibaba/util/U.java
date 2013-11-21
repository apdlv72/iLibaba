package com.apdlv.ilibaba.util;

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
}
