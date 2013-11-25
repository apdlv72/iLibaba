package com.apdlv.ilibaba.frotect;

import android.util.Base64;

public class OneWireAddrTool
{
    public static String shortenAddr(String addr)
    {
	if (null!=addr)
	{
	    addr = colapseEqualOctets(addr);	    
	    addr = addr.replaceAll("^28;", "").replaceAll(";04;00;",";").replaceAll(";00;", ";");
	    addr = addr.replace(";", ".");
	}
	return addr;
    }

    public static String encodeAddr(String addr)
    {
	if (null!=addr)
	{
	    String split[] = addr.split("[;\\.]");
	    byte hex[] = new byte[split.length];
	    for (int i=0; i<split.length; i++)
	    {
		hex[i] = (byte)fromHex(split[i]);
	    }	    
	    addr = Base64.encodeToString(hex, Base64.NO_PADDING|Base64.NO_WRAP);
	}
	return addr;	
    }

    private static String colapseEqualOctets(String addr)
    {
	String split[] = addr.split(";");
	String first = split[0];
	boolean allEqual = true;
	for (String part : split)
	{
	    if (!part.equals(first))
	    {
		allEqual = false;
		break;
	    }
	}
	if (allEqual)
	{
	    addr = first;
	}
	return addr;
    }

    private static int fromHex(String s)
    {
	return Integer.parseInt(s,16);
    }


}
