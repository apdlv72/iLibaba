package com.apdlv.ilibaba.frotect;

import android.graphics.Color;
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

    public static int colorFromAddr(String addr)
    {
	try
	{
	    if ("89.48.C8.0E".equals(addr)) return Color.RED;
	    if ("29.7F.C8.87".equals(addr)) return Color.GREEN;
	    if ("7B.2E.D9.28".equals(addr)) return Color.rgb(127, 127, 255);
	    if (         "00".equals(addr)) return Color.BLACK;
	    if ("50.81.E1.6E".equals(addr)) return Color.YELLOW;
	    
	    addr = shortenAddr(addr);
	    String split[] = addr.split("[;\\.]");
	    byte hex[] = new byte[split.length];
	    for (int i=0; i<split.length; i++)
	    {
		hex[i] = (byte)fromHex(split[i]);
	    }

	    /*
	    int r = getInt(hex, 0);
	    int g = getInt(hex, 1);
	    int b1 = getInt(hex, 3);
	    int b2 = getInt(hex, 2);
	    
	    r = (r*r)/256;
	    g = (g*g)/256;
	    int b = b2;
	    */
	    
	    /*
	    int r = getInt(hex, 0);
	    int g = getInt(hex, 1);
	    int b = getInt(hex, 3);
	    */

	    int r = getInt(hex, 2)-getInt(hex, 0);
	    int g = getInt(hex, 1)-getInt(hex, 3);
	    int b = contrast(getInt(hex, 3));

	    
//	    r = contrast(r);
//	    g = contrast(g);
//	    b = contrast(b);

	    System.out.println("colorFromAddr: " + addr + " -> r=" + r + ", g=" + g + ", b=" + b);

	    return Color.rgb(r,g,b);
	}
	catch (Exception e)
	{
	    System.err.println("colorFromAddr: " + e);
	}
	return Color.RED;
    }
    
    
    private static final int MIN = 20; // minimum to avoid it is 0 (black)

    private static int getInt(byte[] hex, int i)
    {
	int a = hex.length>i ? (int)(hex[i] & 0xff): 0;	
	return MIN + ((255-MIN)*a)/255;
    }
    
//    private static int getInt(byte[] hex, int i, int j)
//    {
//	int a = hex.length>i ? (int) hex[i] : 0;
//	int b = hex.length>j ? (int) hex[j] : 0;
//	int sum = a+b;
//	sum = sum % (255-MIN);
//	return sum+MIN; 
//    }
//
    
    public static int contrast(int input)
    {
	float f = (1.0f/128)*(input-128);	    
	double h  = Math.tanh(f);
	h = (h/0.76);
	h = Math.signum(h)*h*h;
	int o = (int) (129+(127.5*h));
	return o<0 ? 0 : o>255 ? 255 : o;
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
