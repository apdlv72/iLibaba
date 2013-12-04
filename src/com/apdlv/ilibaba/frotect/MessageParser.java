package com.apdlv.ilibaba.frotect;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;


public class MessageParser
{
    private static final String TAG = MessageParser.class.getSimpleName();

    protected static final Pattern P_MEASUREMENT = Pattern.compile(
        "[sn]=([0-9]+),.*" +
        "now=([-0-9\\.]+),.*" +
        "old=([-0-9\\.]+),.*" +
        "avg=([-0-9\\.]+)");
    
    protected static final Pattern P_CURRENTINFO = Pattern.compile(
        "n=([0-9]+),.*" +
        "on=([-0-9]+)");
    
    protected static final Pattern P_SENSORINFO = Pattern.compile(
        "n=([0-9]+),.*" +
        "@=([0-9a-zA-Z:;]+),.*" +
        "usd=([0-1]+),.*"  +
        "avl=([0-1]+),.*" + 
        "bnd=([0-1]+)");
    
    protected static final Pattern P_STRANDINFO = Pattern.compile(
        "n=([0-9]+),.*"    +
        "v=([0-1]+),.*"    +
        "lit=([0-1]+),.*"  +
        "upd=([0-1]+),.*"  +
        "tl=([-0-9\\.]+),.*" +
        "tu=([-0-9\\.]+),.*" +
        "P=([0-9\\.]+),.*"   +
        "t=([-0-9\\.\\?]+),.*" +
        "err=([0-9]+),.*"   +
        "ago=([-0-9]+),.*"  + 
        "pin=([0-9]+),.*"   +
        "@=([0-9a-fA-F;:]+),.*" +
        "usd=([0-1]+),.*"   +
        "avl=([0-1]+),.*"  +
        "bnd=([0-1]+)"
    );
    
    protected static final Pattern P_STARTTIME     = Pattern.compile("n=([0-9]+),.*i=([0-9]+),.*t=([-0-9\\.]+[mhdw]),.*ago=([-0-9\\.]+[mhdw])");
    protected static final Pattern P_STATSTOCSV    = Pattern.compile("t=([-0-9\\.]+[hdw]),.*,v=([0-1]+),[^\\{]*(\\{.*\\}),.*C=([0-9\\.]+)");
    protected static final Pattern P_ONESTRANDINFO = Pattern.compile("n=([0-9]*),.*on=([0-9]+[mh]),.*r=([0-9\\.]+),.*P=([0-9\\.k]+)");
    protected static final Pattern P_HISTORY       = Pattern.compile("t=([-0-9\\.]+)h,.*,(\\{.*\\})");
    protected static final Pattern P_ONESTRANDHIST = Pattern.compile("s=([0-9]+),lo=([-0-9\\.]+),hi=([-0-9\\.]+)");
    protected static final Pattern P_UPDATE        = Pattern.compile("chng=([0-9]),.*t=([-0-9\\.])");
    protected static final Pattern P_HEARBEAT      = Pattern.compile("l=([0-9]),.*c[nd]*=([0-9]+),.*t=([-0-9]+)");

    static public String convertStatsToCsv(String stats)
    {
	try { return _convertStatsToCsv(stats); } catch (Exception e) { Log.e(TAG, "convertStatsToCsv: " + e); }
	return "";
    }
    
    public static String convertHistoryToCsv(String history)
    {
	try { return _convertHistoryToCsv(history); } catch (Exception e) { Log.e(TAG, "convertHistoryToCsv: " + e); }
	return "";
    }
    
    // F: n=4,lit=1
    public static HashMap<String, Integer> parseFlashInfo(String c) throws NumberFormatException
    {
	String lStr = c.replaceAll(".*lit=", "");
	String nStr = c.replaceAll(",lit.*", "").replaceAll(".*n=", "");

	HashMap<String, Integer> map = new HashMap<String, Integer>();
	map.put("n",   Integer.parseInt(nStr));
	map.put("lit", Integer.parseInt(lStr));
	return map;
    }

    // MEAS: s=2,now=4.00,old=3.50,avg=3.50
    public static HashMap<String,Object> parseMeasurement(String line)
    {
	if (null==line) return null; 
	
	HashMap<String,Object> map = null;	
	try
	{
	    Matcher m = P_MEASUREMENT.matcher(line);
	    if (!m.find()) return null;

	    map = new HashMap<String, Object>();
	    map.put("s",     toInt(m.group(1)));
	    map.put("now",   0.01*toDouble(m.group(2)));
	    map.put("old",   0.01*toDouble(m.group(3)));
	    map.put("avg",   0.01*toDouble(m.group(4)));
	}
	catch (Exception e)
	{
	    Log.e(TAG, "parseMeasurement: '" + line + "': " + e);
	}
	return map;
    }

    // CUR: n=1,on=60
    public static Map<String,Object> parseCurrentInfo(String line)
    {
	if (null==line) return null; 

	HashMap<String,Object> map = null;
	try
	{
	    Matcher m = P_CURRENTINFO.matcher(line);
	    if (!m.find()) return null;

	    map = new HashMap<String, Object>();	
	    map.put("n",  toInt(m.group(1)));
	    map.put("on", toInt(m.group(2)));
	}
	catch (Exception e)
	{
	    Log.e(TAG, "parseCurrentInfo: '" + line + "': " + e);
	    
	}
	return map;
    }

    // SEN: n=4, @=xx;20;03;40;55;66;77;99,used=1,avail=1
    static public HashMap<String,Object> parseSensorInfo(String line)
    {
	if (null==line) return null; 

	HashMap<String,Object> map = null;
	try
	{
	    Matcher m = P_SENSORINFO.matcher(line);
	    if (!m.find()) return null;

	    map = new HashMap<String, Object>();	
	    map.put("n",       toInt(m.group(1)));
	    map.put("@",             m.group(2));
	    map.put("used",  0<toInt(m.group(3)));
	    map.put("avail", 0<toInt(m.group(4)));
	    map.put("bnd",   0<toInt(m.group(5)));
	}
	catch (Exception e)
	{
	    Log.e(TAG, "parseSensorInfo: '" + line + "': " + e);	    
	}
	return map;
    }

    // STR. n=1,v=1,lit=1,upd=1,tl=2.00,tu=5.00,P=96.50,t=11.00,err=0,last=4294967295,ago=0,pin=2,@=28;50;81;E1;04;00;00;6E,used=1,avail=1,bnd=1
    static public HashMap<String,Object> parseStrandInfo(String line)
    {
	if (null==line) return null; 

	Matcher m = P_STRANDINFO.matcher(line);
	if (!m.find()) return null;

	HashMap<String,Object> res = null;
	try
	{
	    res = new HashMap<String, Object>();
	    res.put("n",     toInt(m.group(1)));
	    res.put("v",     toInt(m.group(2))!=0);
	    res.put("lit",   toInt(m.group(3))!=0);
	    res.put("upd",   toInt(m.group(4))!=0);
	    res.put("tl",    0.01*toDouble(m.group(5)));
	    res.put("tu",    0.01*toDouble(m.group(6)));
	    res.put("P",     toDouble(m.group(7)));
	    String tStr = m.group(8);
	    // e.g. "STR: n=1,v=0,lit=0,upd=1,tl=3.50,tu=4.50,P=16.00,t=?,err=0,last=0,ago=0,pin=2,@=00;00;00;00;00;00;00;00,used=1,avail=0
	    try { res.put("t", 0.01*toDouble(tStr)); } catch (Exception e) {}
	    res.put("err",   toInt(m.group(9)));
	    res.put("ago",   toLong(m.group(10)));
	    res.put("pin",   toInt(m.group(11)));
	    res.put("@",     m.group(12));
	    res.put("usd",   toInt(m.group(13))!=0);
	    res.put("avl",   toInt(m.group(14))!=0);
	    res.put("bnd",   0<toInt(m.group(15)));
	}
	catch (Exception e)
	{
	    Log.e(TAG, "parseStrandInfo: '" + line + "': " + e);	    
	}	
	return res;
    }

    // T: n=0,i=0,t=00h,ago=30720h
    public static Map<String,Object> parseStartTime(String line)
    {
	if (null==line) return null; 

	// "n=([0-9]+),.*i=([0-9]+),.*t=([-0-9\\.]+[mhdw]),.*ago=([-0-9\\.]+[mhdw])"
	Matcher m = P_STARTTIME.matcher(line);
	if (!m.find()) return null;

	Map<String,Object> map = null;
	try
	{
	    map = new HashMap<String, Object>();
	    String sN = m.group(1);
	    String sI = m.group(2);
	    String sT = m.group(3);
	    String sA = m.group(4);
	    
	    int    n   = toInt(sN);
	    int    i   = toInt(sI);
	    double t   = parseTimeToDays(sT);
	    double ago = parseTimeToDays(sA);

	    map.put("n",   n);
	    map.put("i",   i);
	    map.put("t",   t);
	    map.put("ago", ago);
	}
	catch (Exception e)
	{
	    Log.e(TAG, "parseStartTime: '" + line + "': " + e);	    
	}
	return map;
    }

    public static Map<String,String> parseInfoMessages(String lines)
    {
	if (null==lines) return null;
	
	Map<String,String> map = new HashMap<String, String>();
	BufferedReader br = new BufferedReader(new StringReader(lines));

	String line = null;
	do
	{
	    try { line = br.readLine(); } catch (IOException e) {}
	    if (null!=line)
	    {
		line = line.replaceAll("^I.", "");
		String key = line.replaceAll("=.*", "");
		String val = line.replaceAll("^[^=]*=", "");
		map.put(key,val);
	    }
	}
	while (null!=line);
	
	try { br.close(); } catch (Exception e) {}
	
	return map;
    }

    public static double parsePing(String line)
    {
        try
        {
            line = line.replaceAll("^P.*s=", "");
            double ms = Double.parseDouble(line);		    		    
            return ms;
        }
        catch (Exception e)
        {
            return -1.0;
        }
    }

    // UP. change=1,s=2.03
    public static Map<String, Object> parseUpdateInfo(String line)
    {	
	if (null==line) return null;
        
        Map<String,Object> map = null; 
        try
        {
            Matcher m = P_UPDATE.matcher(line);
            if (!m.find()) return map;
    
            map = new HashMap<String, Object>();
            int    c = toInt(m.group(1));
            double t = toDouble(m.group(2));
    
            map.put("change",   c);
            map.put("t",        t);
        }
        catch (Exception e)
        {
            Log.e(TAG, "parseUpdateInfo: " + e);
        }
        return map;
    }

    // HB. l=1,cnd=1
    public static Map<String, Object> parseHeartbeatInfo(String line)
    {
        Map<String,Object> map = null;
        try
        {
            // line = "T: n=0, averageTemp=00m, ago=30720mT: n=0, averageTemp=00m, ago=30720m"
            Matcher m = P_HEARBEAT.matcher(line);
            if (!m.find()) return null;
    
            map = new HashMap<String, Object>();
            int l = toInt(m.group(1));
            int c = toInt(m.group(2));
            int t = toInt(m.group(3));
    
            map.put("l",   l);
            map.put("c",   c);
            map.put("cnd", c);
            map.put("t",   t);
        }
        catch (Exception e)
        {
            Log.e(TAG, "parseHeartbeatInfo: " + e);
        }
        return map;
    }

    protected static double parsePowerToKWh(String s)
    {
	double mul = s.endsWith("k") ? 1 : 0.001;
	return mul * Double.parseDouble(s.replaceAll("k", ""));
    }

    protected static double parseTimeToDays(String s)
    {
	double mul = getTimeMultiplierToDays(s);
	int    len = s.length();
	s = s.substring(0, len-1);
	double val = Double.parseDouble(s);
	return mul*val;
    }

    // time unit is minutes (strand on time) per hour 
    protected static double parseDurationToMinutes(String s)
    {
        double mul = getTimeMultiplierToMinutes(s);	
        int    len = s.length();
        s = s.substring(0, len-1);
        double val = Double.parseDouble(s);
        return mul*val;
    }

    protected static double getTimeMultiplierToDays(String s)
    {
        double mul = 1.0;
        if (s.endsWith("h")) mul =  1.0/24.0;
        //if (s.endsWith("d")) mul =  1.0;
        if (s.endsWith("w")) mul =  7.0;
        return mul;
    }

    static private double getTimeMultiplierToMinutes(String s)
    {
        double mul = 1.0;
        if (s.endsWith("m")) mul =       1.0;
        if (s.endsWith("h")) mul =      60.0;
        if (s.endsWith("d")) mul = 24.0*60.0;
        if (s.endsWith("w")) mul = 24.0*60.0*7.0;
        return mul;
    }

    static private String _convertStatsToCsv(String lines)
    {
        if (null==lines) return null;
        
        StringBuilder sb = new StringBuilder();
        sb.append("TimeRaw,DaysBack,CentsPerUnit,NormDivisor,EuroPerDay");
        for (int i=1; i<=5; i++)
        {
            sb.append(String.format(",MinOnPerHour%d,DutyCycle%d,kWhPerUnit%d,kWhPerDay%d", i, i, i, i));
        }
        sb.append("\n");
    
        BufferedReader br = new BufferedReader(new StringReader(lines));
        String line = null;
        do
        {
            try { line = br.readLine(); } catch (IOException e) {}
            if (null!=line)
            {
        	Matcher m = P_STATSTOCSV.matcher(line);
    
        	if (!m.find())
        	{
        	    Log.e(TAG, "_convertStatsToCsv: faield to parse '" + line + "'");
        	    sb.append("error,").append(line).append("\n");
        	}
        	else		    
        	{
        	    String sTime    = m.group(1);
        	    String sValid   = m.group(2);
        	    String sStrands = m.group(3);
        	    String sCost    = m.group(4);
    
        	    int val = Integer.parseInt(sValid);
        	    if (val<1) continue;
    
        	    double days    = parseTimeToDays(sTime);
        	    double divisor = getTimeMultiplierToDays(sTime); // 1/24 for hours, 1 for days, 7 for weeks		    
        	    double costRaw = Double.parseDouble(sCost);
   
        	    double costNorm = 0.01*costRaw/divisor; // normalize to EUR/day			    
        	    //Log.e(TAG, "time=" + sTime + ", days=" + days + ", mul=" + divisor + ", costRaw=" + costRaw + ", costNorm=" + costNorm);
    
        	    sb.append(sTime);
        	    sb.append(",").append(format(-days)  ); // make it positive
        	    sb.append(",").append(format(costRaw));
        	    sb.append(",").append(format(divisor));
        	    sb.append(",").append(format(costNorm));
    
        	    String split[] = sStrands.split("\\}\\{"); // "{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}"
        	    for (int i=0; i<split.length; i++)
        	    {
        		String strand = split[i];
        		Matcher hlM = P_ONESTRANDINFO.matcher(strand);
        		if (!hlM.find())
        		{
        		    //System.out.println("NO MATCH: " + sStrands);
        		}
        		else
        		{			
        		    //String no = hlM.group(1); // strand number
        		    double on      = parseDurationToMinutes(hlM.group(2)); // minutes on per time unit,  i.e. hour, day, etc.
        		    double duty    = toDouble(hlM.group(3));
        		    double kwhRaw  = parsePowerToKWh(hlM.group(4));    
        		    double kwhNorm = kwhRaw / divisor;
        		    
        		    on /= divisor;  // normalize to on-minutes per day
        		    on /= 24.0; // convert per day -> per hour
    
        		    sb.append(",").append(format(on));
        		    sb.append(",").append(format(duty));
        		    sb.append(",").append(format(kwhRaw));
        		    sb.append(",").append(format(kwhNorm));
        		}			
        	    }
        	    sb.append("\n");
        	}
            }
        }
        while (null!=line);
    
        return sb.toString();
    }

    private static String _convertHistoryToCsv(String lines) 
    {
        if (null==lines) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("Days,Min1,Max1,Min2,Max2,Min3,Max3,Min4,Max4,Min5,Max5\n");
    
        BufferedReader br = new BufferedReader(new StringReader(lines));    
        String line = null;
        do
        {
            try { line = br.readLine(); } catch (IOException e) {}
            if (null!=line)
            {        
        	// MM: N=27,c=0,t=-163.00h,i=9,{s=1,lo=2.5,hi=10.0}{s=2,lo=-1.0,hi=7.5}{s=3,lo=-1.0,hi=10.5}{s=4,lo=-1.0,hi=10.5}{s=5,lo=-0.5,hi=63.0}    
        	Matcher m = P_HISTORY.matcher(line);
    
        	if (!m.find())
        	{
        	    Log.e(TAG, "_convertHistoryToCsv: failed to parse '" + line + "'");
        	    sb.append("error,").append(line).append("\n");
        	}
        	else		    
        	{
        	    String s1 = m.group(1);
        	    String s2 = m.group(2);
    
        	    float t = Float.parseFloat(s1);
        	    t/=24.0; // hours (default) -> days
        	    sb.append(format(-t));
    
        	    String split[] = s2.split("\\}\\{"); // "{s=01,lo=2300,hi=2950}{s=02,lo=2300,hi=2950}...
        	    for (int i=0; i<split.length; i++)
        	    {
        		String s3 = split[i];
        		Matcher m2 = P_ONESTRANDHIST.matcher(s3);
        		if (!m2.find())
        		{
        		    //System.out.println("NO MATCH: " + s3);
        		}
        		else
        		{			
        		    //String no = hlM.group(1); // strand number
        		    double lo = 0.01*toDouble(m2.group(2));
        		    double hi = 0.01*toDouble(m2.group(3));
    
        		    sb.append(",").append(format(lo));
        		    sb.append(",").append(format(hi));
        		}
        	    }
        	    sb.append("\n");
        	}    
            }            
        }
        while (null!=line);
        
        try { br.close(); } catch (Exception e) {}
    
        return sb.toString();
    }

    private static String format(double d)
    {
	return String.format(Locale.ENGLISH, "%2.2f", d);
    }

    protected static Double toDouble(String s)
    {
	return null==s ? null : Double.parseDouble(s);
    }

    private static Integer toInt(String s)
    {
	return null==s ? null : Integer.parseInt(s);
    }

    private static Long toLong(String s)
    {
	return null==s ? null : Long.parseLong(s);
    }

}
