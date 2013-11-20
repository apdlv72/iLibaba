package com.apdlv.ilibaba.frotect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;

public class Parser
{

    public Parser()
    {
	// TODO Auto-generated constructor stub
    }
    
    public static HashMap<String, Integer> parseFlashInfo(String c) throws NumberFormatException
    {
	    String lStr = c.replaceAll(".*lit=", "");
	    String nStr = c.replaceAll(",lit.*", "").replaceAll(".*n=", "");

	    HashMap<String, Integer> map = new HashMap<String, Integer>();
	    map.put("n",   Integer.parseInt(nStr));
	    map.put("lit", Integer.parseInt(lStr));
	    return map;
    }



    static public HashMap<String,Object> parseMeasurement(String line)
    {
	if (null==line) return null; // || !line.startsWith("MEAS:")) return null;

	Pattern p = Pattern.compile(
		  "[sn]=([0-9]+),.*" +
		  "now=([-0-9\\.]+),.*" +
		  "old=([-0-9\\.]+),.*" +
		  "avg=([-0-9\\.]+)");

	Matcher m = p.matcher(line);
	if (!m.find())
	{
	    return null;
	}

	HashMap<String,Object> res = new HashMap<String, Object>();	
	res.put("s",     toInt(m.group(1)));
	res.put("now",   toDouble(m.group(2)));
	res.put("old",   toDouble(m.group(3)));
	res.put("avg",   toDouble(m.group(4)));
	
	return res;
    }
    
    // SEN: n=4, @=xx;20;03;40;55;66;77;99,used=1,avail=1
    static public HashMap<String,Object> parseSensorInfo(String line)
    {
	if (null==line) return null; // || !line.startsWith("MEAS:")) return null;

	Pattern p = Pattern.compile(
		  "n=([0-9]+),.*" +
		  "@=([0-9a-zA-Z:;]+),.*" +
		  "used=([0-1]+),.*" +
		  "avail=([0-1]+)");

	Matcher m = p.matcher(line);
	if (!m.find())
	{
	    return null;
	}

	HashMap<String,Object> res = new HashMap<String, Object>();	
	res.put("n",       toInt(m.group(1)));
	res.put("@",             m.group(2));
	res.put("used",  0<toInt(m.group(3)));
	res.put("avail", 0<toInt(m.group(4)));

	Pattern p2 = Pattern.compile(",bnd=([0-1]+)");
	Matcher m2 = p2.matcher(line);
	if (m2.find())
	{
	    res.put("bnd", 0<toInt(m2.group(1)));
	}

	return res;
    }

    
    static public HashMap<String,Object> parseStrandInfo(String line)
    {
	// STR. n=1,v=1,lit=1,upd=1,tl=2.00,tu=5.00,P=96.50,t=11.00,err=0,last=4294967295,ago=0,pin=2,@=28;50;81;E1;04;00;00;6E,used=1,avail=1
	// STR. n=2,v=1,lit=1,upd=0,tl=3.00,tu=5.00,P=96.50,t=9.50,err=0,last=4294967295,ago=0,pin=4,@=28;89;48;C8;04;00;00;0E,used=1,avail=1
	// STR. n=01,v=01,lit=01,upd=00,tl=3.50,tu=4.50,P=16.00,t=0.00,err=00,last=00,ago=07,pin=02,@=00;00;00;00;00;00;00;00,used=01,avail=01
	
	if (null==line) return null; // || !line.startsWith("STR.")) return null;
	
	Pattern p = Pattern.compile(
		  "n=([0-9]+),.*" +
		  "v=([0-1]+),.*"  +
		"lit=([0-1]+),.*"  +
		"upd=([0-1]+),.*"  +
		 "tl=([-0-9\\.]+),.*" +
		 "tu=([-0-9\\.]+),.*" +
		  "P=([0-9\\.]+),.*" +
 	 	  "t=([-0-9\\.\\?]+),.*" +
 	 	"err=([0-9]+),.*"  +
 	       "last=([0-9]+),.*"  +
 	        "ago=([-0-9]+),.*"  + 
 	        "pin=([0-9]+),.*"  +
 	          "@=([0-9a-fA-F;:]+),.*" +
 	       "used=([0-1]+),.*"   +
 	      "avail=([0-1]+)");

	Matcher m = p.matcher(line);
	if (!m.find())
	{
	    return null;
	}
	
	HashMap<String,Object> res = new HashMap<String, Object>();
	
	res.put("n",     toInt(m.group(1)));
	res.put("v",     toInt(m.group(2))!=0);
	res.put("lit",   toInt(m.group(3))!=0);
	res.put("upd",   toInt(m.group(4))!=0);
	res.put("tl",    toDouble(m.group(5)));
	res.put("tu",    toDouble(m.group(6)));
	res.put("P",     toDouble(m.group(7)));
	String tStr = m.group(8);
	try
	{
	    // e.g. "STR: n=1,v=0,lit=0,upd=1,tl=3.50,tu=4.50,P=16.00,t=?,err=0,last=0,ago=0,pin=2,@=00;00;00;00;00;00;00;00,used=1,avail=0
	    res.put("t", toDouble(tStr));
	}
	catch (Exception e) {}
	res.put("err",   toInt(m.group(9)));
	res.put("last",  toLong(m.group(10)));
	res.put("ago",   toLong(m.group(11)));
	res.put("pin",   toInt(m.group(12)));
	res.put("@",     m.group(13));
	res.put("used",  toInt(m.group(14))!=0);
	res.put("avail", toInt(m.group(15))!=0);
	
	
	Pattern p2 = Pattern.compile(",bnd=([0-1]+)");
	Matcher m2 = p2.matcher(line);
	if (m2.find())
	{
	    res.put("bnd", 0<toInt(m2.group(1)));
	}
	
	return res;
    }

    
//    public static void main(String[] args)
//    {
//	String line = "STR. n=1,v=1,lit=0,upd=0,tl=3.00,tu=5.00,P=96.50,temp=8.00,err=0,last=4294967295,ago=0,pin=2,@=28;50;81;E1;04;00;00;6E,used=1,avail=1";
//	HashMap<String, Object> map = parseStrandInfo(line);
//    }

    private static Double toDouble(String s)
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


    static public HashMap<String, GraphViewData[]> parseStats(String stats)
    {
	if (null==stats) return null;
	HashMap<String,ArrayList<GraphViewData>> map = new HashMap<String,ArrayList<GraphViewData>>(10);

	StringReader sr = new StringReader(stats);
	BufferedReader br = new BufferedReader(sr);

	String line = null;
	do
	{
	    try { line = br.readLine(); } catch (IOException e) {}
	    if (null!=line)
	    {
		if (!line.matches("^ST.\\:.*")) continue;

		Pattern tPattern = Pattern.compile("t=([-0-9\\.]+[hdw]),.*,v=([0-1]+),[^\\{]*(\\{.*\\}),.*C=([0-9\\.]+)");
		Matcher tM = tPattern.matcher(line);

		if (!tM.find())
		{
		    //System.out.println("NO MATCH: " + line);
		}
		else		    
		{
		    String sTime    = tM.group(1);
		    String sValid   = tM.group(2);
		    String sStrands = tM.group(3);
		    String sCost    = tM.group(4);

		    int val = Integer.parseInt(sValid);
		    if (val<1) continue;

		    double t   = parseTime(sTime);
		    double mul = getTimeMultiplier(sTime);		    
		    double cost = Double.parseDouble(sCost);
		    cost = 0.01*cost/mul; // normalize to EUR / hour			    

		    ArrayList<GraphViewData> cList = map.get("C");			
		    if (null==cList) { cList = new ArrayList<GraphViewData>(40); map.put("C", cList); }
		    cList.add(new GraphViewData(t, cost));

		    String split[] = sStrands.split("\\}\\{"); // "{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}"
		    for (int i=0; i<split.length; i++)
		    {
			String strand = split[i];
			Pattern hlPattern = Pattern.compile("n=([0-9]*).*r=([0-9\\.]+),.*P=([0-9\\.k]+)");
			Matcher hlM = hlPattern.matcher(strand);
			if (!hlM.find())
			{
			    //System.out.println("NO MATCH: " + sStrands);
			}
			else
			{			
			    String no = hlM.group(1); // number of value
			    //int   n = Integer.parseInt(hlM.group(1));
			    double  r = Float.parseFloat(hlM.group(2));
			    double  p = parsePower(hlM.group(3));

			    p /= mul; // normalize to kWh per hour			    

			    String rName = "r" + no;
			    String pName = "P" + no;

			    //System.out.println("temp=" + t + ", n=" + no + ", r=" + r + ", P=" + p);			    

			    ArrayList<GraphViewData> rList = map.get(rName);
			    ArrayList<GraphViewData> pList = map.get(pName);			
			    if (null==rList) { rList = new ArrayList<GraphViewData>(40); map.put(rName, rList); }
			    if (null==pList) { pList = new ArrayList<GraphViewData>(40); map.put(pName, pList); }

			    rList.add(new GraphViewData(t, r));
			    pList.add(new GraphViewData(t, p));
			}			
		    }
		}
	    }
	}
	while (null!=line);

	HashMap<String, GraphViewData[]> result = toReverseArray(map);	
	return result;
    }


    static private double parsePower(String s)
    {
	double mul = s.endsWith("k") ? 1000 : 1;
	return mul * Double.parseDouble(s.replaceAll("k", ""));
    }

    static private double parseTime(String s)
    {
	double mul = getTimeMultiplier(s);

	s = s.substring(0, s.length()-1);
	double val = Double.parseDouble(s);

	return mul*val;
    }

    static private double getTimeMultiplier(String s)
    {
	double mul = 1.0;
	if (s.endsWith("h")) mul =    1.0;
	if (s.endsWith("d")) mul =   24.0;
	if (s.endsWith("w")) mul = 7*24.0;
	return mul;
    }


    static private HashMap<String, GraphViewData[]> toReverseArray(HashMap<String, ArrayList<GraphViewData>> map)
    {
	HashMap<String, GraphViewData[]> result = new HashMap<String, GraphView.GraphViewData[]>();

	for (String k : map.keySet())
	{
	    ArrayList<GraphViewData> list = map.get(k);
	    Collections.reverse(list);

	    GraphViewData[] data = list.toArray(new GraphViewData[0]);
	    result.put(k, data);
	}
	return result;
    }

    public static HashMap<String, GraphViewData[]> parseHistories(String history) 
    {
	if (null==history) return null;
        HashMap<String,ArrayList<GraphViewData>> map = new HashMap<String,ArrayList<GraphViewData>>(10);
        
        StringReader sr = new StringReader(history);
        BufferedReader br = new BufferedReader(sr);
        
        String line = null;
        do
        {
            try { line = br.readLine(); } catch (IOException e) {}
            if (null!=line)
            {        	
        	Pattern tPattern = Pattern.compile("t=([-0-9\\.]+)h,.*,(\\{.*\\})");
        	Matcher tM = tPattern.matcher(line);
        	
        	if (!tM.find())
        	{
        	    //System.out.println("NO MATCH: " + line);
        	}
        	else		    
        	{
        	    String s1 = tM.group(1);
        	    String s2 = tM.group(2);
        	    
        	    float t = Float.parseFloat(s1);
        	    t/=24.0; // hours -> days
        	    
        	    String split[] = s2.split("\\}\\{"); // "{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}"
        	    for (int i=0; i<split.length; i++)
        	    {
        		String s3 = split[i];
        		Pattern hlPattern = Pattern.compile("s=([0-9]+),lo=([-0-9\\.]+),hi=([-0-9\\.]+)");
        		Matcher hlM = hlPattern.matcher(s3);
        		if (!hlM.find())
        		{
        		    //System.out.println("NO MATCH: " + s3);
        		}
        		else
        		{			
        		    String no = hlM.group(1); // number of value
        		    float  lo = Float.parseFloat(hlM.group(2));
        		    float  hi = Float.parseFloat(hlM.group(3));
        		    
        		    String loName = "lo" + no;
        		    String hiName = "hi" + no;
        		    ArrayList<GraphViewData> loList = map.get(loName);
        		    ArrayList<GraphViewData> hiList = map.get(hiName);			
        		    if (null==loList) { loList = new ArrayList<GraphViewData>(40); map.put(loName, loList); }
        		    if (null==hiList) { hiList = new ArrayList<GraphViewData>(40); map.put(hiName, hiList); }
    
        		    loList.add(new GraphViewData(t, lo));
        		    hiList.add(new GraphViewData(t, hi));
        		}
        	    }
        	}
        			
            }
        }
        while (null!=line);
    
        HashMap<String, GraphViewData[]> result = toReverseArray(map);	
        return result;
    }


    public static Map<String,Double> parseStartTime(String line)
    {
	Map<String,Double> map = new HashMap<String, Double>();
	
	// line = "T: n=0, temp=00m, ago=30720mT: n=0, temp=00m, ago=30720m"
	Pattern tPattern = Pattern.compile("temp=([-0-9\\.]+[mhdw]),.*ago=([-0-9\\.]+[mhdw])");
	Matcher tM = tPattern.matcher(line);

	if (!tM.find())
	{
	    //System.out.println("NO MATCH: " + line);
	    return map;
	}

	String sTime = tM.group(1);
	String sAgo  = tM.group(1);

	double t   = parseTime(sTime);
	double ago = parseTime(sAgo);
	
	map.put("temp",   t);
	map.put("ago", ago);	
	return map;
    }

    
    public static Map<String,String> parseInfo(String data)
    {
	Map<String,String> map = new HashMap<String, String>();
	StringReader sr = new StringReader(data);
	BufferedReader br = new BufferedReader(sr);

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
	sr.close();
	return map;
    }	

}
