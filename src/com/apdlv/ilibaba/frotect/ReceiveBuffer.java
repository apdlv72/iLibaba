package com.apdlv.ilibaba.frotect;

import java.util.Calendar;

public class ReceiveBuffer
{
    public void append(String s)
    {
        if (null!=s) synchronized (this)
        {		
    	if (null==receiving) receiving=new StringBuilder();		
    	receiving.append(s);
    	if (!s.endsWith("\n"))
    	    receiving.append("\n");
        }	
    }

    public String finish()
    {
	return finish(null);
    }

    public String finish(String s)
    {
        synchronized(this)
        {
    	if (null!=s) append(s);
    	completed = (null==receiving) ? null : receiving.toString();
    	receiving = null;
    	receiveTime = Calendar.getInstance();
    	return completed;
        }	    
    }

    public boolean isComplete()
    {
        return null!=completed && completed.length()>0;
    }

    public String toString()
    {
        return completed;
    }

    private StringBuilder receiving = null;
    private String        completed = null;
    private Calendar receiveTime;
    
    public Calendar getReceiveTime()
    {
	return this.receiveTime;
    }
}