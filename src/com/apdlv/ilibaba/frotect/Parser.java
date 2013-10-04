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

    
    static public HashMap<String,Object> parseStrandInfo(String line)
    {
	line = "STR. n=01,v=01,lit=00,upd=00,tl=24.50,tu=28.00,P=127.50,t=23.00,err=00,last=00,ago=00,@=11;22;33;44;55;66;77;88,used=01,avail=01";
	
	if (null==line || !line.startsWith("STR.")) return null;
	
	Pattern p = Pattern.compile(
		  "n=([0-9]+),.*" +
		  "v=([01]+),.*" +
		"lit=([01]+),.*" +
		"upd=([01]+),.*" +
		 "tl=([0-9\\.]+),.*" +
		 "tu=([0-9\\.]+),.*" +
		  "P=([0-9\\.]+),.*" +
 	 	  "t=([0-9\\.]+),.*" +
 	 	"err=([0-9]+),.*" +
 	       "last=([0-9]+),.*" +
 	        "ago=([0-9]+),.*" +
 	          "@=([0-9;]+),.*" +
 	       "used=([01]+),.*" +
 	      "avail=([01]+)");

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
	res.put("t",     toDouble(m.group(8)));
	res.put("err",   toInt(m.group(9)));
	res.put("last",  toLong(m.group(10)));
	res.put("ago",   toInt(m.group(11)));
	res.put("@",     m.group(12));
	res.put("used",  toInt(m.group(13))!=0);
	res.put("avail", toInt(m.group(44))!=0);
	return res;
    }
    

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
	HashMap<String,ArrayList<GraphViewData>> map = new HashMap<String,ArrayList<GraphViewData>>(10);

	StringReader sr = new StringReader(stats);
	BufferedReader br = new BufferedReader(sr);

	String line = null;
	do
	{
	    try { line = br.readLine(); } catch (IOException e) {}
	    if (null!=line)
	    {
		if (!line.matches("^..\\..*")) continue;

		Pattern tPattern = Pattern.compile("t=([-0-9\\.]+[hdw]),.*,v=([01]+),[^\\{]*(\\{.*\\}),.*C=([0-9\\.]+)");
		Matcher tM = tPattern.matcher(line);

		if (!tM.find())
		{
		    System.out.println("NO MATCH: " + line);
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
		    cost = 100.0*cost/mul; // normalize to EUR / hour			    

		    ArrayList<GraphViewData> cList = map.get("C");			
		    if (null==cList) { cList = new ArrayList<GraphViewData>(40); map.put("C", cList); }
		    cList.add(new GraphViewData(t, cost));

		    String split[] = sStrands.split("\\}\\{"); // "{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}"
		    for (int i=0; i<1; i++)
		    {
			String strand = split[i];
			Pattern hlPattern = Pattern.compile("n=([0-9]*).*r=([0-9\\.]+),.*P=([0-9\\.k]+)");
			Matcher hlM = hlPattern.matcher(strand);
			if (!hlM.find())
			{
			    System.out.println("NO MATCH: " + sStrands);
			}
			else
			{			
			    String no = hlM.group(1); // number of sensor
			    //int   n = Integer.parseInt(hlM.group(1));
			    double  r = Float.parseFloat(hlM.group(2));
			    double  p = parsePower(hlM.group(3));

			    p /= mul; // normalize to kWh per hour			    

			    String rName = "r" + no;
			    String pName = "P" + no;

			    System.out.println("t=" + t + ", n=" + no + ", r=" + r + ", P=" + p);			    

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
	//return parseHistories();
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


    public static final String TEST_STATISTICS = 
        		"STR: n=01, val=01, lit=01, upd=01,tu=28.00,tl=24.50,t=24.50,err=00,last=762660,ago=00,addr=11;22;33;44;55;66;77;88, used=01, avail=01\n" +
        		"STR: n=02, val=01, lit=01, upd=01,tu=28.00,tl=24.50,t=25.00,err=00,last=762660,ago=00,addr=11;22;33;44;55;66;77;88, used=01, avail=01\n" +
        		"STR: n=03, val=01, lit=00, upd=01,tu=28.00,tl=24.50,t=24.50,err=00,last=762660,ago=00,addr=11;22;33;44;55;66;77;88, used=01, avail=01\n" +
        		"STR: n=04, val=01, lit=00, upd=01,tu=28.00,tl=24.50,t=27.50,err=00,last=762660,ago=00,addr=11;22;33;44;55;66;77;88, used=01, avail=01\n" +
        		"STR.\n" +
        		"SH. t=-00h,i=38,v=1,{n=01,on=29m,r=0.48,P=61.62}{n=02,on=28m,r=0.47,P=45.03}{n=03,on=48m,r=0.80,P=77.20}{n=04,on=55m,r=0.92,P=88.46},P=272.32,C=5.47\n" +
        		"SH. t=-01h,i=37,v=1,{n=01,on=33m,r=0.55,P=70.12}{n=02,on=44m,r=0.73,P=70.77}{n=03,on=38m,r=0.63,P=61.12}{n=04,on=36m,r=0.60,P=57.90},P=259.91,C=5.22\n" +
        		"SH. t=-02h,i=36,v=1,{n=01,on=32m,r=0.53,P=68.00}{n=02,on=53m,r=0.88,P=85.24}{n=03,on=60m,r=1.00,P=96.50}{n=04,on=29m,r=0.48,P=46.64},P=296.38,C=5.96\n" +
        		"SH. t=-03h,i=35,v=1,{n=01,on=51m,r=0.85,P=108.38}{n=02,on=32m,r=0.53,P=51.47}{n=03,on=50m,r=0.83,P=80.42}{n=04,on=40m,r=0.67,P=64.33},P=304.59,C=6.12\n" +
        		"SH. t=-04h,i=34,v=1,{n=01,on=45m,r=0.75,P=95.62}{n=02,on=13m,r=0.22,P=20.91}{n=03,on=45m,r=0.75,P=72.38}{n=04,on=45m,r=0.75,P=72.38},P=261.28,C=5.25\n" +
        		"SH. t=-05h,i=33,v=1,{n=01,on=23m,r=0.38,P=48.88}{n=02,on=47m,r=0.78,P=75.59}{n=03,on=55m,r=0.92,P=88.46}{n=04,on=45m,r=0.75,P=72.38},P=285.30,C=5.73\n" +
        		"SH. t=-06h,i=32,v=1,{n=01,on=51m,r=0.85,P=108.38}{n=02,on=32m,r=0.53,P=51.47}{n=03,on=52m,r=0.87,P=83.63}{n=04,on=48m,r=0.80,P=77.20},P=320.68,C=6.45\n" +
        		"SH. t=-07h,i=31,v=1,{n=01,on=49m,r=0.82,P=104.12}{n=02,on=01m,r=0.02,P=1.61}{n=03,on=41m,r=0.68,P=65.94}{n=04,on=48m,r=0.80,P=77.20},P=248.88,C=5.00\n" +
        		"SH. t=-08h,i=30,v=1,{n=01,on=42m,r=0.70,P=89.25}{n=02,on=56m,r=0.93,P=90.07}{n=03,on=14m,r=0.23,P=22.52}{n=04,on=58m,r=0.97,P=93.28},P=295.12,C=5.93\n" +
        		"SH. t=-09h,i=29,v=1,{n=01,on=41m,r=0.68,P=87.12}{n=02,on=49m,r=0.82,P=78.81}{n=03,on=34m,r=0.57,P=54.68}{n=04,on=48m,r=0.80,P=77.20},P=297.82,C=5.99\n" +
        		"SH. t=-10h,i=28,v=1,{n=01,on=56m,r=0.93,P=119.00}{n=02,on=60m,r=1.00,P=96.50}{n=03,on=36m,r=0.60,P=57.90}{n=04,on=29m,r=0.48,P=46.64},P=320.04,C=6.43\n" +
        		"SH. t=-11h,i=27,v=1,{n=01,on=37m,r=0.62,P=78.62}{n=02,on=27m,r=0.45,P=43.42}{n=03,on=46m,r=0.77,P=73.98}{n=04,on=48m,r=0.80,P=77.20},P=273.23,C=5.49\n" +
        		"SH. t=-12h,i=26,v=1,{n=01,on=44m,r=0.73,P=93.50}{n=02,on=34m,r=0.57,P=54.68}{n=03,on=36m,r=0.60,P=57.90}{n=04,on=33m,r=0.55,P=53.08},P=259.16,C=5.21\n" +
        		"SH. t=-13h,i=25,v=1,{n=01,on=49m,r=0.82,P=104.12}{n=02,on=46m,r=0.77,P=73.98}{n=03,on=36m,r=0.60,P=57.90}{n=04,on=32m,r=0.53,P=51.47},P=287.48,C=5.78\n" +
        		"SH. t=-14h,i=24,v=1,{n=01,on=33m,r=0.55,P=70.12}{n=02,on=49m,r=0.82,P=78.81}{n=03,on=36m,r=0.60,P=57.90}{n=04,on=38m,r=0.63,P=61.12},P=267.95,C=5.39\n" +
        		"SH. t=-15h,i=23,v=1,{n=01,on=44m,r=0.73,P=93.50}{n=02,on=32m,r=0.53,P=51.47}{n=03,on=50m,r=0.83,P=80.42}{n=04,on=54m,r=0.90,P=86.85},P=312.23,C=6.28\n" +
        		"SH. t=-16h,i=22,v=1,{n=01,on=41m,r=0.68,P=87.12}{n=02,on=33m,r=0.55,P=53.08}{n=03,on=38m,r=0.63,P=61.12}{n=04,on=57m,r=0.95,P=91.67},P=292.99,C=5.89\n" +
        		"SH. t=-17h,i=21,v=1,{n=01,on=27m,r=0.45,P=57.38}{n=02,on=40m,r=0.67,P=64.33}{n=03,on=60m,r=1.00,P=96.50}{n=04,on=52m,r=0.87,P=83.63},P=301.84,C=6.07\n" +
        		"SH. t=-18h,i=20,v=1,{n=01,on=34m,r=0.57,P=72.25}{n=02,on=33m,r=0.55,P=53.08}{n=03,on=42m,r=0.70,P=67.55}{n=04,on=17m,r=0.28,P=27.34},P=220.22,C=4.43\n" +
        		"SH. t=-19h,i=19,v=1,{n=01,on=53m,r=0.88,P=112.62}{n=02,on=41m,r=0.68,P=65.94}{n=03,on=08m,r=0.13,P=12.87}{n=04,on=53m,r=0.88,P=85.24},P=276.67,C=5.56\n" +
        		"SH. t=-20h,i=18,v=1,{n=01,on=37m,r=0.62,P=78.62}{n=02,on=58m,r=0.97,P=93.28}{n=03,on=50m,r=0.83,P=80.42}{n=04,on=34m,r=0.57,P=54.68},P=307.01,C=6.17\n" +
        		"SH. t=-21h,i=17,v=1,{n=01,on=46m,r=0.77,P=97.75}{n=02,on=29m,r=0.48,P=46.64}{n=03,on=47m,r=0.78,P=75.59}{n=04,on=26m,r=0.43,P=41.82},P=261.80,C=5.26\n" +
        		"SH. t=-22h,i=16,v=1,{n=01,on=54m,r=0.90,P=114.75}{n=02,on=47m,r=0.78,P=75.59}{n=03,on=25m,r=0.42,P=40.21}{n=04,on=41m,r=0.68,P=65.94},P=296.49,C=5.96\n" +
        		"SH. t=-23h,i=15,v=1,{n=01,on=40m,r=0.67,P=85.00}{n=02,on=21m,r=0.35,P=33.77}{n=03,on=49m,r=0.82,P=78.81}{n=04,on=48m,r=0.80,P=77.20},P=274.78,C=5.52\n" +
        		"SH. t=-24h,i=14,v=1,{n=01,on=22m,r=0.37,P=46.75}{n=02,on=46m,r=0.77,P=73.98}{n=03,on=53m,r=0.88,P=85.24}{n=04,on=50m,r=0.83,P=80.42},P=286.39,C=5.76\n" +
        		"SH. t=-25h,i=13,v=1,{n=01,on=20m,r=0.33,P=42.50}{n=02,on=51m,r=0.85,P=82.03}{n=03,on=51m,r=0.85,P=82.03}{n=04,on=48m,r=0.80,P=77.20},P=283.75,C=5.70\n" +
        		"SH. t=-26h,i=12,v=1,{n=01,on=45m,r=0.75,P=95.62}{n=02,on=29m,r=0.48,P=46.64}{n=03,on=45m,r=0.75,P=72.38}{n=04,on=51m,r=0.85,P=82.03},P=296.67,C=5.96\n" +
        		"SH. t=-27h,i=11,v=1,{n=01,on=56m,r=0.93,P=119.00}{n=02,on=39m,r=0.65,P=62.72}{n=03,on=42m,r=0.70,P=67.55}{n=04,on=56m,r=0.93,P=90.07},P=339.34,C=6.82\n" +
        		"SH. t=-28h,i=10,v=1,{n=01,on=60m,r=1.00,P=127.50}{n=02,on=59m,r=0.98,P=94.89}{n=03,on=37m,r=0.62,P=59.51}{n=04,on=18m,r=0.30,P=28.95},P=310.85,C=6.25\n" +
        		"SH. t=-29h,i=09,v=1,{n=01,on=48m,r=0.80,P=102.00}{n=02,on=49m,r=0.82,P=78.81}{n=03,on=33m,r=0.55,P=53.08}{n=04,on=55m,r=0.92,P=88.46},P=322.34,C=6.48\n" +
        		"SH. t=-30h,i=08,v=1,{n=01,on=43m,r=0.72,P=91.38}{n=02,on=43m,r=0.72,P=69.16}{n=03,on=20m,r=0.33,P=32.17}{n=04,on=42m,r=0.70,P=67.55},P=260.25,C=5.23\n" +
        		"SH. t=-31h,i=07,v=1,{n=01,on=60m,r=1.00,P=127.50}{n=02,on=35m,r=0.58,P=56.29}{n=03,on=34m,r=0.57,P=54.68}{n=04,on=39m,r=0.65,P=62.72},P=301.20,C=6.05\n" +
        		"SH. t=-32h,i=06,v=1,{n=01,on=52m,r=0.87,P=110.50}{n=02,on=39m,r=0.65,P=62.72}{n=03,on=51m,r=0.85,P=82.03}{n=04,on=49m,r=0.82,P=78.81},P=334.06,C=6.71\n" +
        		"SH. t=-33h,i=05,v=1,{n=01,on=60m,r=1.00,P=127.50}{n=02,on=28m,r=0.47,P=45.03}{n=03,on=44m,r=0.73,P=70.77}{n=04,on=30m,r=0.50,P=48.25},P=291.55,C=5.86\n" +
        		"SH. t=-34h,i=04,v=1,{n=01,on=36m,r=0.60,P=76.50}{n=02,on=31m,r=0.52,P=49.86}{n=03,on=32m,r=0.53,P=51.47}{n=04,on=25m,r=0.42,P=40.21},P=218.03,C=4.38\n" +
        		"SH. t=-35h,i=03,v=1,{n=01,on=55m,r=0.92,P=116.88}{n=02,on=38m,r=0.63,P=61.12}{n=03,on=40m,r=0.67,P=64.33}{n=04,on=49m,r=0.82,P=78.81},P=321.13,C=6.45\n" +
        		"SH. t=-36h,i=02,v=1,{n=01,on=13m,r=0.22,P=27.62}{n=02,on=53m,r=0.88,P=85.24}{n=03,on=50m,r=0.83,P=80.42}{n=04,on=39m,r=0.65,P=62.72},P=256.01,C=5.15\n" +
        		"SH. t=-37h,i=01,v=1,{n=01,on=58m,r=0.97,P=123.25}{n=02,on=35m,r=0.58,P=56.29}{n=03,on=44m,r=0.73,P=70.77}{n=04,on=32m,r=0.53,P=51.47},P=301.77,C=6.07\n" +
        		"SH. t=-38h,i=00,v=1,{n=01,on=31m,r=0.52,P=65.88}{n=02,on=36m,r=0.60,P=57.90}{n=03,on=45m,r=0.75,P=72.38}{n=04,on=17m,r=0.28,P=27.34},P=223.49,C=4.49\n" +
        		"SH. t=-39h,i=47,v=1,{n=01,on=26m,r=0.43,P=55.25}{n=02,on=33m,r=0.55,P=53.08}{n=03,on=57m,r=0.95,P=91.67}{n=04,on=42m,r=0.70,P=67.55},P=267.55,C=5.38\n" +
        		"SH. t=-40h,i=46,v=1,{n=01,on=42m,r=0.70,P=89.25}{n=02,on=42m,r=0.70,P=67.55}{n=03,on=51m,r=0.85,P=82.03}{n=04,on=35m,r=0.58,P=56.29},P=295.12,C=5.93\n" +
        		"SH. t=-41h,i=45,v=1,{n=01,on=28m,r=0.47,P=59.50}{n=02,on=26m,r=0.43,P=41.82}{n=03,on=26m,r=0.43,P=41.82}{n=04,on=56m,r=0.93,P=90.07},P=233.20,C=4.69\n" +
        		"SH. t=-42h,i=44,v=1,{n=01,on=58m,r=0.97,P=123.25}{n=02,on=14m,r=0.23,P=22.52}{n=03,on=11m,r=0.18,P=17.69}{n=04,on=43m,r=0.72,P=69.16},P=232.62,C=4.68\n" +
        		"SH. t=-43h,i=43,v=1,{n=01,on=33m,r=0.55,P=70.12}{n=02,on=26m,r=0.43,P=41.82}{n=03,on=22m,r=0.37,P=35.38}{n=04,on=28m,r=0.47,P=45.03},P=192.36,C=3.87\n" +
        		"SH. t=-44h,i=42,v=1,{n=01,on=28m,r=0.47,P=59.50}{n=02,on=28m,r=0.47,P=45.03}{n=03,on=28m,r=0.47,P=45.03}{n=04,on=32m,r=0.53,P=51.47},P=201.03,C=4.04\n" +
        		"SH. t=-45h,i=41,v=1,{n=01,on=43m,r=0.72,P=91.38}{n=02,on=48m,r=0.80,P=77.20}{n=03,on=38m,r=0.63,P=61.12}{n=04,on=42m,r=0.70,P=67.55},P=297.24,C=5.97\n" +
        		"SH. t=-46h,i=40,v=1,{n=01,on=31m,r=0.52,P=65.88}{n=02,on=52m,r=0.87,P=83.63}{n=03,on=55m,r=0.92,P=88.46}{n=04,on=38m,r=0.63,P=61.12},P=299.08,C=6.01\n" +
        		"SH. t=-47h,i=39,v=1,{n=01,on=33m,r=0.55,P=70.12}{n=02,on=48m,r=0.80,P=77.20}{n=03,on=37m,r=0.62,P=59.51}{n=04,on=13m,r=0.22,P=20.91},P=227.74,C=4.58\n" +
        		"SH: h=192,P=13386.95,P/h=69.72,C=269.08,C/h=1.40\n" +
        		"SD. t=-02d,i=10,v=1,{n=01,on=897m,r=0.62,P=1.91k}{n=02,on=869m,r=0.60,P=1.40k}{n=03,on=1038m,r=0.72,P=1.67k}{n=04,on=951m,r=0.66,P=1.53k},P=6.50k,C=130.71\n" +
        		"SD. t=-03d,i=09,v=1,{n=01,on=927m,r=0.64,P=1.97k}{n=02,on=958m,r=0.67,P=1.54k}{n=03,on=986m,r=0.68,P=1.59k}{n=04,on=1026m,r=0.71,P=1.65k},P=6.75k,C=135.61\n" +
        		"SD. t=-04d,i=08,v=1,{n=01,on=1014m,r=0.70,P=2.15k}{n=02,on=937m,r=0.65,P=1.51k}{n=03,on=931m,r=0.65,P=1.50k}{n=04,on=1069m,r=0.74,P=1.72k},P=6.88k,C=138.26\n" +
        		"SD. t=-05d,i=07,v=1,{n=01,on=899m,r=0.62,P=1.91k}{n=02,on=970m,r=0.67,P=1.56k}{n=03,on=996m,r=0.69,P=1.60k}{n=04,on=976m,r=0.68,P=1.57k},P=6.64k,C=133.51\n" +
        		"SD. t=-06d,i=06,v=1,{n=01,on=990m,r=0.69,P=2.10k}{n=02,on=916m,r=0.64,P=1.47k}{n=03,on=940m,r=0.65,P=1.51k}{n=04,on=1104m,r=0.77,P=1.78k},P=6.86k,C=137.97\n" +
        		"SD. t=-07d,i=05,v=1,{n=01,on=909m,r=0.63,P=1.93k}{n=02,on=958m,r=0.67,P=1.54k}{n=03,on=850m,r=0.59,P=1.37k}{n=04,on=959m,r=0.67,P=1.54k},P=6.38k,C=128.28\n" +
        		"SD. t=-08d,i=04,v=1,{n=01,on=934m,r=0.65,P=1.98k}{n=02,on=942m,r=0.65,P=1.52k}{n=03,on=1081m,r=0.75,P=1.74k}{n=04,on=901m,r=0.63,P=1.45k},P=6.69k,C=134.42\n" +
        		"SD. t=-09d,i=03,v=1,{n=01,on=952m,r=0.66,P=2.02k}{n=02,on=1116m,r=0.77,P=1.79k}{n=03,on=913m,r=0.63,P=1.47k}{n=04,on=1092m,r=0.76,P=1.76k},P=7.04k,C=141.56\n" +
        		"SD. t=-10d,i=02,v=1,{n=01,on=942m,r=0.65,P=2.00k}{n=02,on=959m,r=0.67,P=1.54k}{n=03,on=1041m,r=0.72,P=1.67k}{n=04,on=1044m,r=0.73,P=1.68k},P=6.90k,C=138.64\n" +
        		"SD. t=-11d,i=01,v=1,{n=01,on=1013m,r=0.70,P=2.15k}{n=02,on=888m,r=0.62,P=1.43k}{n=03,on=958m,r=0.67,P=1.54k}{n=04,on=990m,r=0.69,P=1.59k},P=6.71k,C=134.95\n" +
        		"SD. t=-12d,i=00,v=1,{n=01,on=1032m,r=0.72,P=2.19k}{n=02,on=959m,r=0.67,P=1.54k}{n=03,on=1047m,r=0.73,P=1.68k}{n=04,on=956m,r=0.66,P=1.54k},P=6.96k,C=139.83\n" +
        		"SD. t=-13d,i=13,v=1,{n=01,on=848m,r=0.59,P=1.80k}{n=02,on=930m,r=0.65,P=1.50k}{n=03,on=901m,r=0.63,P=1.45k}{n=04,on=976m,r=0.68,P=1.57k},P=6.32k,C=126.96\n" +
        		"SD. t=-14d,i=12,v=1,{n=01,on=985m,r=0.68,P=2.09k}{n=02,on=1090m,r=0.76,P=1.75k}{n=03,on=947m,r=0.66,P=1.52k}{n=04,on=1019m,r=0.71,P=1.64k},P=7.01k,C=140.86\n" +
        		"SD. t=-15d,i=11,v=1,{n=01,on=995m,r=0.69,P=2.11k}{n=02,on=1035m,r=0.72,P=1.66k}{n=03,on=902m,r=0.63,P=1.45k}{n=04,on=1064m,r=0.74,P=1.71k},P=6.94k,C=139.51\n" +
        		"SD: d=56,P=94.58k,P/d=1.69k,C=1901.06,C/d=33.95\n" +
        		"SW. t=-02w,i=14,v=1,{n=01,on=114h,r=0.68,P=14.53k}{n=02,on=112h,r=0.67,P=10.81k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=112h,r=0.67,P=10.81k},P=47.15k,C=947.76\n" +
        		"SW. t=-03w,i=13,v=1,{n=01,on=120h,r=0.71,P=15.30k}{n=02,on=114h,r=0.68,P=11.00k}{n=03,on=120h,r=0.71,P=11.58k}{n=04,on=110h,r=0.65,P=10.61k},P=48.50k,C=974.77\n" +
        		"SW. t=-04w,i=12,v=1,{n=01,on=116h,r=0.69,P=14.79k}{n=02,on=114h,r=0.68,P=11.00k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=108h,r=0.64,P=10.42k},P=47.21k,C=949.00\n" +
        		"SW. t=-05w,i=11,v=1,{n=01,on=114h,r=0.68,P=14.53k}{n=02,on=120h,r=0.71,P=11.58k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=118h,r=0.70,P=11.39k},P=48.50k,C=974.91\n" +
        		"SW. t=-06w,i=10,v=1,{n=01,on=116h,r=0.69,P=14.79k}{n=02,on=116h,r=0.69,P=11.19k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=116h,r=0.69,P=11.19k},P=48.18k,C=968.40\n" +
        		"SW. t=-07w,i=09,v=1,{n=01,on=118h,r=0.70,P=15.05k}{n=02,on=112h,r=0.67,P=10.81k}{n=03,on=112h,r=0.67,P=10.81k}{n=04,on=116h,r=0.69,P=11.19k},P=47.85k,C=961.89\n" +
        		"SW. t=-08w,i=08,v=1,{n=01,on=116h,r=0.69,P=14.79k}{n=02,on=116h,r=0.69,P=11.19k}{n=03,on=112h,r=0.67,P=10.81k}{n=04,on=110h,r=0.65,P=10.61k},P=47.41k,C=952.88\n" +
        		"SW. t=-09w,i=07,v=1,{n=01,on=112h,r=0.67,P=14.28k}{n=02,on=112h,r=0.67,P=10.81k}{n=03,on=116h,r=0.69,P=11.19k}{n=04,on=116h,r=0.69,P=11.19k},P=47.48k,C=954.27\n" +
        		"SW. t=-10w,i=06,v=1,{n=01,on=122h,r=0.73,P=15.55k}{n=02,on=116h,r=0.69,P=11.19k}{n=03,on=118h,r=0.70,P=11.39k}{n=04,on=118h,r=0.70,P=11.39k},P=49.52k,C=995.41\n" +
        		"SW. t=-11w,i=05,v=1,{n=01,on=112h,r=0.67,P=14.28k}{n=02,on=110h,r=0.65,P=10.61k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=112h,r=0.67,P=10.81k},P=46.70k,C=938.75\n" +
        		"SW. t=-12w,i=04,v=1,{n=01,on=110h,r=0.65,P=14.02k}{n=02,on=110h,r=0.65,P=10.61k}{n=03,on=116h,r=0.69,P=11.19k}{n=04,on=112h,r=0.67,P=10.81k},P=46.64k,C=937.50\n" +
        		"SW. t=-13w,i=03,v=1,{n=01,on=116h,r=0.69,P=14.79k}{n=02,on=110h,r=0.65,P=10.61k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=118h,r=0.70,P=11.39k},P=47.79k,C=960.64\n" +
        		"SW. t=-14w,i=02,v=1,{n=01,on=116h,r=0.69,P=14.79k}{n=02,on=110h,r=0.65,P=10.61k}{n=03,on=112h,r=0.67,P=10.81k}{n=04,on=114h,r=0.68,P=11.00k},P=47.21k,C=949.00\n" +
        		"SW. t=-15w,i=01,v=1,{n=01,on=114h,r=0.68,P=14.53k}{n=02,on=116h,r=0.69,P=11.19k}{n=03,on=112h,r=0.67,P=10.81k}{n=04,on=108h,r=0.64,P=10.42k},P=46.96k,C=943.88\n" +
        		"SW. t=-16w,i=00,v=1,{n=01,on=108h,r=0.64,P=13.77k}{n=02,on=114h,r=0.68,P=11.00k}{n=03,on=118h,r=0.70,P=11.39k}{n=04,on=116h,r=0.69,P=11.19k},P=47.35k,C=951.78\n" +
        		"SW. t=-17w,i=29,v=1,{n=01,on=116h,r=0.69,P=14.79k}{n=02,on=114h,r=0.68,P=11.00k}{n=03,on=118h,r=0.70,P=11.39k}{n=04,on=112h,r=0.67,P=10.81k},P=47.99k,C=964.52\n" +
        		"SW. t=-18w,i=28,v=1,{n=01,on=110h,r=0.65,P=14.02k}{n=02,on=114h,r=0.68,P=11.00k}{n=03,on=116h,r=0.69,P=11.19k}{n=04,on=108h,r=0.64,P=10.42k},P=46.64k,C=937.50\n" +
        		"SW. t=-19w,i=27,v=1,{n=01,on=112h,r=0.67,P=14.28k}{n=02,on=114h,r=0.68,P=11.00k}{n=03,on=118h,r=0.70,P=11.39k}{n=04,on=116h,r=0.69,P=11.19k},P=47.86k,C=962.03\n" +
        		"SW. t=-20w,i=26,v=1,{n=01,on=112h,r=0.67,P=14.28k}{n=02,on=114h,r=0.68,P=11.00k}{n=03,on=118h,r=0.70,P=11.39k}{n=04,on=110h,r=0.65,P=10.61k},P=47.28k,C=950.39\n" +
        		"SW. t=-21w,i=25,v=1,{n=01,on=108h,r=0.64,P=13.77k}{n=02,on=112h,r=0.67,P=10.81k}{n=03,on=112h,r=0.67,P=10.81k}{n=04,on=114h,r=0.68,P=11.00k},P=46.39k,C=932.38\n" +
        		"SW. t=-22w,i=24,v=1,{n=01,on=118h,r=0.70,P=15.05k}{n=02,on=114h,r=0.68,P=11.00k}{n=03,on=112h,r=0.67,P=10.81k}{n=04,on=110h,r=0.65,P=10.61k},P=47.47k,C=954.13\n" +
        		"SW. t=-23w,i=23,v=1,{n=01,on=118h,r=0.70,P=15.05k}{n=02,on=110h,r=0.65,P=10.61k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=112h,r=0.67,P=10.81k},P=47.47k,C=954.13\n" +
        		"SW. t=-24w,i=22,v=1,{n=01,on=112h,r=0.67,P=14.28k}{n=02,on=118h,r=0.70,P=11.39k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=112h,r=0.67,P=10.81k},P=47.48k,C=954.27\n" +
        		"SW. t=-25w,i=21,v=1,{n=01,on=116h,r=0.69,P=14.79k}{n=02,on=116h,r=0.69,P=11.19k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=112h,r=0.67,P=10.81k},P=47.79k,C=960.64\n" +
        		"SW. t=-26w,i=20,v=1,{n=01,on=112h,r=0.67,P=14.28k}{n=02,on=112h,r=0.67,P=10.81k}{n=03,on=118h,r=0.70,P=11.39k}{n=04,on=114h,r=0.68,P=11.00k},P=47.48k,C=954.27\n" +
        		"SW. t=-27w,i=19,v=1,{n=01,on=114h,r=0.68,P=14.53k}{n=02,on=118h,r=0.70,P=11.39k}{n=03,on=114h,r=0.68,P=11.00k}{n=04,on=118h,r=0.70,P=11.39k},P=48.31k,C=971.03\n" +
        		"SW. t=-28w,i=18,v=1,{n=01,on=110h,r=0.65,P=14.02k}{n=02,on=108h,r=0.64,P=10.42k}{n=03,on=116h,r=0.69,P=11.19k}{n=04,on=110h,r=0.65,P=10.61k},P=46.26k,C=929.75\n" +
        		"SW. t=-29w,i=17,v=1,{n=01,on=108h,r=0.64,P=13.77k}{n=02,on=118h,r=0.70,P=11.39k}{n=03,on=116h,r=0.69,P=11.19k}{n=04,on=116h,r=0.69,P=11.19k},P=47.54k,C=955.65\n" +
        		"SW. t=-30w,i=16,v=1,{n=01,on=110h,r=0.65,P=14.02k}{n=02,on=110h,r=0.65,P=10.61k}{n=03,on=112h,r=0.67,P=10.81k}{n=04,on=114h,r=0.68,P=11.00k},P=46.45k,C=933.62\n" +
        		"SW. t=-31w,i=15,v=1,{n=01,on=112h,r=0.67,P=14.28k}{n=02,on=112h,r=0.67,P=10.81k}{n=03,on=120h,r=0.71,P=11.58k}{n=04,on=114h,r=0.68,P=11.00k},P=47.67k,C=958.15\n" +
        		"SW: w=120,P=1424.54kWh,P/w=11.87k,C=28633.27,C/w=238.61\n" +
        		"S:\n";


    public static final String TEST_MINMAXHIST = 
        		"MM. N=00,c=1,t=-0.00h,i=-1,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=01,c=0,t=-6.00h,i=32,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=02,c=0,t=-12.00h,i=31,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=03,c=0,t=-18.00h,i=30,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=04,c=0,t=-24.00h,i=29,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=05,c=0,t=-30.00h,i=28,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=06,c=0,t=-36.00h,i=27,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=07,c=0,t=-42.00h,i=26,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=08,c=0,t=-48.00h,i=25,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=09,c=0,t=-54.00h,i=24,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=10,c=0,t=-60.00h,i=23,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=11,c=0,t=-66.00h,i=22,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=12,c=0,t=-72.00h,i=21,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=13,c=0,t=-78.00h,i=20,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=14,c=0,t=-84.00h,i=19,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=15,c=0,t=-90.00h,i=18,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=16,c=0,t=-96.00h,i=17,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=17,c=0,t=-102.00h,i=16,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=18,c=0,t=-108.00h,i=15,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=19,c=0,t=-114.00h,i=14,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=20,c=0,t=-120.00h,i=13,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=21,c=0,t=-126.00h,i=12,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=22,c=0,t=-132.00h,i=11,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=23,c=0,t=-138.00h,i=10,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=24,c=0,t=-144.00h,i=09,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=25,c=0,t=-150.00h,i=08,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=26,c=0,t=-156.00h,i=07,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=27,c=0,t=-162.00h,i=06,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=28,c=0,t=-168.00h,i=05,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=29,c=0,t=-174.00h,i=04,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=30,c=0,t=-180.00h,i=03,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=31,c=0,t=-186.00h,i=02,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=32,c=0,t=-192.00h,i=01,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=33,c=0,t=-198.00h,i=00,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=34,c=0,t=-204.00h,i=43,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=35,c=0,t=-210.00h,i=42,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=36,c=0,t=-216.00h,i=41,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=37,c=0,t=-222.00h,i=40,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=38,c=0,t=-228.00h,i=39,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=39,c=0,t=-234.00h,i=38,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=40,c=0,t=-240.00h,i=37,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=41,c=0,t=-246.00h,i=36,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=42,c=0,t=-252.00h,i=35,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM. N=43,c=0,t=-258.00h,i=34,{s=01,lo=23.00,hi=29.50}{s=02,lo=23.00,hi=29.50}{s=03,lo=23.00,hi=29.50}{s=04,lo=23.00,hi=29.50}{s=05,lo=23.00,hi=29.50}\n" +
        		"MM:\n";
    

    public static HashMap<String, GraphViewData[]> parseHistories(String history) 
    {
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
        	    System.out.println("NO MATCH: " + line);
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
        		    System.out.println("NO MATCH: " + s3);
        		}
        		else
        		{			
        		    String no = hlM.group(1); // number of sensor
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
	
	Pattern tPattern = Pattern.compile("t=([-0-9\\.]+[mhdw]),.*ago=([-0-9\\.]+[mhdw]),");
	Matcher tM = tPattern.matcher(line);

	if (!tM.find())
	{
	    System.out.println("NO MATCH: " + line);
	    return map;
	}

	String sTime = tM.group(1);
	String sAgo  = tM.group(1);

	double t   = parseTime(sTime);
	double ago = parseTime(sAgo);
	
	map.put("t",   t);
	map.put("ago", ago);	
	return map;
    }

}
