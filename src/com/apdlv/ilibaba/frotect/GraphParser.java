package com.apdlv.ilibaba.frotect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;

public class GraphParser extends MessageParser
{
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

		    // make days positive, since GraphView cannot handle negative values nicely
		    double days  = -1 * parseTimeToDays(sTime);
		    double mul   = getTimeMultiplierToDays(sTime);		    
		    double cost  = Double.parseDouble(sCost);
		    cost = 0.01*cost/mul; // normalize to EUR / day			    

		    ArrayList<GraphViewData> cList = map.get("C");			
		    if (null==cList) { cList = new ArrayList<GraphViewData>(40); map.put("C", cList); }
		    
		    // add as days, not hours
		    cList.add(new GraphViewData(days, cost));

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
			    double  r = Float.parseFloat(hlM.group(2));
			    double  p = parsePowerToKWh(hlM.group(3)); // p is in kWh per time unit, i.e. hour, day etc.

			    p /= mul; // normalize to kWh per day			    

			    String rName = "r" + no;
			    String pName = "P" + no;

			    //System.out.println("averageTemp=" + t + ", n=" + no + ", r=" + r + ", P=" + p);			    

			    ArrayList<GraphViewData> rList = map.get(rName);
			    ArrayList<GraphViewData> pList = map.get(pName);			
			    if (null==rList) { rList = new ArrayList<GraphViewData>(40); map.put(rName, rList); }
			    if (null==pList) { pList = new ArrayList<GraphViewData>(40); map.put(pName, pList); }

			    rList.add(new GraphViewData(days, r));
			    pList.add(new GraphViewData(days, p));
			}			
		    }
		}
	    }
	}
	while (null!=line);

	HashMap<String, GraphViewData[]> result = toSortedArray(map);	
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

		    // make time positive, since GraphView cannot handle negative values nicely
		    float t = -1 * Float.parseFloat(s1); // hours always
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

	HashMap<String, GraphViewData[]> result = toSortedArray(map);	
	return result;
    }

    private static Comparator<? super GraphViewData> comparator = new Comparator<GraphViewData>()
    {
	public int compare(GraphViewData d1, GraphViewData d2)
        {
	    return (int)Math.round(1000*(d1.getX()-d2.getX()));
        }	
    };     


    private static HashMap<String, GraphViewData[]> toSortedArray(HashMap<String, ArrayList<GraphViewData>> map)
    {
	HashMap<String, GraphViewData[]> result = new HashMap<String, GraphView.GraphViewData[]>();

	for (String k : map.keySet())
	{
	    ArrayList<GraphViewData> list = map.get(k);
	    	
	    //String s1 = dump(list);
	    Collections.sort(list, comparator);
	    //Collections.reverse(list);
	    //String s2 = dump(list);
	    
//	    if (!s1.equals(s2))
//	    {
//		System.out.println("toSortedArray: order changed");
//		System.out.println("before: " + s1);
//		System.out.println("after:  " + s2);
//	    }

	    GraphViewData[] data = list.toArray(new GraphViewData[0]);
	    result.put(k, data);
	}
	return result;
    }


    private static String dump(ArrayList<GraphViewData> list)
    {
	StringBuilder sb = new StringBuilder();
	for (GraphViewData d : list)
	{
	    sb.append(" (" + String.format("%2.1f", d.getX())  + "," + String.format("%2.1f", d.getY()) + ")");
	}
	return sb.toString();
    }


    protected static HashMap<String, GraphViewData[]> toReverseArray(HashMap<String, ArrayList<GraphViewData>> map)
    {
	HashMap<String, GraphViewData[]> result = new HashMap<String, GraphViewData[]>();

	for (String k : map.keySet())
	{
	    ArrayList<GraphViewData> list = map.get(k);
	    Collections.reverse(list);

	    GraphViewData[] data = list.toArray(new GraphViewData[0]);
	    result.put(k, data);
	}
	return result;
    }


}
