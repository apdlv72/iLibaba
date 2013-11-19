package com.apdlv.ilibaba.frotect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.app.Dialog;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.frotect.FrotectActivity.FrotectBTDataCompleteListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;


public class StatsDialog extends Dialog implements FrotectBTDataCompleteListener, OnCheckedChangeListener
{
    final static String TAG = StatsDialog.class.getSimpleName();
    
    private HashMap<String, GraphViewData[]> statistics;
    private HashMap<String, GraphViewData[]> minmaxHist;

    private GraphView mGraphView;
    private FrotectActivity frotect;

    public StatsDialog(FrotectActivity frotect, String style)
    {
	super(frotect);
	
	this.frotect = frotect;
	this.mStyle = style;
	LayoutInflater inflater = this.getLayoutInflater();
	setContentView(inflater.inflate(R.layout.dialog_stats, null));

	if (FrotectActivity.POWER.equals(mStyle))
	{
	    setTitle("Power consumption");
	    initPower();
	}
	else if (FrotectActivity.TEMP.equals(mStyle))
	{
	    setTitle("Temperature history");
	    initTemp();
	}
	else if (FrotectActivity.COST.equals(mStyle))
	{
	    setTitle("Cost");
	    initCost();
	}
	else if (FrotectActivity.DUTY.equals(mStyle))
	{
	    setTitle("Duty cycle");
	    initDuty();
	}

	for (int n=0; n<checkboxId.length; n++)
	{
	    CheckBox cb = (CheckBox) findViewById(checkboxId[n]);
	    cb.setOnCheckedChangeListener(this);
	}

    }
    

    @Override
    public void show()
    {
	super.show();

	statistics = Parser.parseStats(frotect.statsCompleted);
	minmaxHist = Parser.parseHistories(frotect.minMaxCompleted);
	
	refreshData();
    }

    
    public void onDataComplete(TYPE type, String data)
    {
	switch (type)
	{
	case STATS: 
	    statistics = Parser.parseStats(data);
	    refreshData(); 
	    break;
	
	case MINMAX:
	    minmaxHist  = Parser.parseHistories(data);
	    refreshData(); 
	    break;

	case STARTTIMES:
	    startTimesBuffer = data;
	    refreshData(); 
	    break;
	    
	default:
	}
    }	


    private void refreshData()
    {
	if (FrotectActivity.POWER.equals(mStyle))
	{
	    refreshPower();
	}
	if (FrotectActivity.TEMP.equals(mStyle))
	{
	    refreshTemp();
	}
	if (FrotectActivity.COST.equals(mStyle))
	{
	    refreshCost();
	}
	if (FrotectActivity.DUTY.equals(mStyle))
	{
	    refreshDuty();
	}	
    }

    void refreshStartTimes(double min, double max)
    {
	if (null==startTimesBuffer) return;
	StringReader sr = new StringReader(startTimesBuffer);
	BufferedReader br = new BufferedReader(sr);

	String line = null;
	do 
	{
	    try { line = br.readLine(); } catch (IOException e) {}
	    if (null!=line)
	    {
		Map<String, Double> map = Parser.parseStartTime(line);
		Double ago = map.get("ago");

		GraphViewData d[] = new GraphViewData[2];
		d[0] = new GraphViewData(ago-0.001,min);
		d[1] = new GraphViewData(ago+0.001,max);

		GraphViewSeries series = new GraphViewSeries("|", whStyle, d);
		mGraphView.addSeries(series);
	    }
	}
	while (null!=line);
    }


    void initDuty()
    {       
	// graph with dynamically genereated horizontal and vertical labels
	LineGraphView gv = new LineGraphView(frotect, "% on");

	gv.setDrawingCacheQuality(GraphView.DRAWING_CACHE_QUALITY_HIGH);
	gv.getGraphViewStyle().setTextSize(10);
	//graphView.setDrawBackground(true);

	refreshDuty();

	//	graphView.setShowLegend(true);
	gv.setManualYAxisBounds(1, 0);

	gv.setViewPort(-4, 4);  
	gv.setScrollable(true);  
	gv.setScalable(true);  
	gv.getGraphViewStyle().setNumHorizontalLabels(5);
	gv.getGraphViewStyle().setNumVerticalLabels(11);

	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.addView(mGraphView = gv);	
    }

    private void refreshDuty()
    {
	if (null==statistics) return;

	mGraphView.removeAllSeries();

	for (String k : statistics.keySet())
	{
	    if (!k.startsWith("r")) continue;

	    GraphViewData[] values = statistics.get(k);	    
	    GraphViewSeriesStyle style = getStyle(k);

	    if (isChecked(k))
	    {	    
		GraphViewSeries series = new GraphViewSeries(k, style, values);
//		for (GraphViewData v : values)
//		{
//		    System.out.println("refreshDuty: " + v.valueX + "," + v.valueY);
//		}
		mGraphView.addSeries(series);
	    }

	}

	refreshStartTimes(0,1.0);
    }
    
    
    public GraphViewSeriesStyle getStyle(String key)
    {
	return getStyle(key, false);
    }
    
    public GraphViewSeriesStyle getStyle(String key, boolean lo)
    {
	GraphViewSeriesStyle style = grayStyle;
	if (lo)
	{
	    if (key.endsWith("1")) style = style01Lo;
	    if (key.endsWith("2")) style = style02Lo;
	    if (key.endsWith("3")) style = style03Lo;
	    if (key.endsWith("4")) style = style04Lo;
	    if (key.endsWith("5")) style = style05Lo;
	}
	else
	{
	    	if (key.endsWith("1")) style = style01;
	    	if (key.endsWith("2")) style = style02;
	    	if (key.endsWith("3")) style = style03;
	    	if (key.endsWith("4")) style = style04;
	    	if (key.endsWith("5")) style = style05;	    
	}
	return style;
    }

    void initPower()
    {       
	// graph with dynamically genereated horizontal and vertical labels
	LineGraphView gv = new LineGraphView(frotect, "kWh/day");

	gv.setDrawingCacheQuality(GraphView.DRAWING_CACHE_QUALITY_HIGH);
	gv.getGraphViewStyle().setTextSize(10);

	refreshPower();

	//	graphView.setDrawBackground(true);
	//	graphView.setShowLegend(true);
	//	graphView.setManualYAxisBounds(20, 0);	
	gv.setViewPort(-4, 4);  
	gv.setScrollable(true);  
	gv.setScalable(true);  
	gv.getGraphViewStyle().setNumHorizontalLabels(5);
	gv.getGraphViewStyle().setNumVerticalLabels(11);

	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.addView(mGraphView = gv);	
    }

    private void refreshPower()
    {
	if (null==statistics) return;

	mGraphView.removeAllSeries();
	for (String k : statistics.keySet())
	{
	    if (!k.startsWith("P")) continue;

	    GraphViewData[] values = statistics.get(k);	    
	    GraphViewSeriesStyle style = getStyle(k);
	    if (isChecked(k))
	    {
		GraphViewSeries series = new GraphViewSeries(k, style, values);
		mGraphView.addSeries(series);		
	    }
	}

	refreshStartTimes(0,100);
    }


    void initTemp()
    {       
	//	GraphViewSeriesStyle loStyle = new GraphViewSeriesStyle();  
	//	loStyle.setValueDependentColor(new ValueDependentColor()
	//	{
	//	  public int get(GraphViewDataInterface data)
	//	  {  
	//	      double y = data.getY();
	//	      //return Color.rgb((int)(150+6*y), 0, (int)(150-6*y));
	//	      return Color.rgb(255, 0, 0);
	//	  }  
	//	});  		
	//	loStyle.thickness=10;

	// graph with dynamically genereated horizontal and vertical labels
	LineGraphView graphView = new LineGraphView(frotect, "¡C");

	graphView.setDrawingCacheQuality(GraphView.DRAWING_CACHE_QUALITY_HIGH);
	graphView.getGraphViewStyle().setTextSize(10);
	//graphView.setDrawBackground(true);

	// custom static labels
	//graphView.setHorizontalLabels(new String[] {"7", "6", "5", "4", "3", "2", "1", "0"});
	//graphView.setVerticalLabels(new String[] {"8¡", "6¡", "4¡", "2¡",  "0¡" });
	//	graphView.addSeries(hiSeries); // data
	//	graphView.addSeries(loSeries); // data

	refreshTemp();

	//	graphView.setShowLegend(true);
	//	graphView.setLegendAlign(LegendAlign.MIDDLE);  
	//	graphView.setLegendWidth(100);  
	//graphView.setManualYAxisBounds(30, -15);
	graphView.setManualYAxisBounds(35, -15);

	graphView.setViewPort(-4, 4);  
	graphView.setScrollable(true);  
	graphView.setScalable(true);  
	graphView.getGraphViewStyle().setNumHorizontalLabels(5);
	graphView.getGraphViewStyle().setNumVerticalLabels(11);

	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.addView(mGraphView = graphView);	
    }
    
    int checkboxId[] = { R.id.checkBoxSensor1, R.id.checkBoxSensor2, R.id.checkBoxSensor3, R.id.checkBoxSensor4, R.id.checkBoxSensor5 };     
    
    private boolean isChecked(String key)
    {
	try
	{
	    String numStr = key.replaceAll("^[^0-9]*", "");
	    int num = Integer.parseInt(numStr)-1;
	    CheckBox cb = (CheckBox) findViewById(checkboxId[num]);	
	    return null!=cb && cb.isChecked();
	}
	catch (Exception e)
	{
	    Log.e(TAG, "isChecked: key='" + key + "': " + e);
	}
	return false;
    }
    

    private void refreshTemp()
    {
	if (null==minmaxHist) return;

	mGraphView.removeAllSeries();	   
	Set<String> keySet = minmaxHist.keySet(); 
	System.out.println("refreshTemp: keys: " + keySet);
		
	for (String key : minmaxHist.keySet())
	{
	    GraphViewData[] values = minmaxHist.get(key);

	    GraphViewSeriesStyle style = getStyle(key, key.startsWith("lo"));
	    if (isChecked(key))
	    {
		GraphViewSeries series = new GraphViewSeries(key, style, values);
		mGraphView.addSeries(series);
	    }
	}
	refreshStartTimes(-10,30);
    }

    void initCost()
    {       
	// graph with dynamically genereated horizontal and vertical labels
	LineGraphView graphView = new LineGraphView(frotect, "Û/day");

	graphView.setDrawingCacheQuality(GraphView.DRAWING_CACHE_QUALITY_HIGH);
	graphView.getGraphViewStyle().setTextSize(10);

	refreshCost();

	graphView.setViewPort(-4, 4);  
	graphView.setScrollable(true);  
	graphView.setScalable(true);  
	graphView.getGraphViewStyle().setNumHorizontalLabels(5);
	graphView.getGraphViewStyle().setNumVerticalLabels(11);

	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.addView(mGraphView = graphView);	
	refreshStartTimes(0,10);
    }

    private void refreshCost()
    {
	if (null==statistics) return;

	mGraphView.removeAllSeries();	    
	for (String k : statistics.keySet())
	{
	    if (!k.equals("C")) continue;

	    GraphViewData[] values = statistics.get(k);	    
	    GraphViewSeriesStyle style = grayStyle;
	    GraphViewSeries series = new GraphViewSeries(k, style, values);
	    mGraphView.addSeries(series);
	}
	//graphView.setManualYAxisBounds(1.0, 0);
	refreshStartTimes(0,1);
    }


    HashMap<String, GraphViewData[]> toReverseArray(HashMap<String, ArrayList<GraphViewData>> map)
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


    //private BTFrotectSerialServiceConnection mConnection = new BTFrotectSerialServiceConnection(mHandler);

    private GraphViewSeriesStyle whStyle   = new GraphViewSeriesStyle(Color.YELLOW, 5);	
    private GraphViewSeriesStyle grayStyle = new GraphViewSeriesStyle(Color.GRAY, 5);	

    private GraphViewSeriesStyle style01   = new GraphViewSeriesStyle(Color.RED, 1);	
    private GraphViewSeriesStyle style02   = new GraphViewSeriesStyle(Color.GREEN, 1);	
    private GraphViewSeriesStyle style03   = new GraphViewSeriesStyle(Color.BLUE, 1);	
    private GraphViewSeriesStyle style04   = new GraphViewSeriesStyle(Color.YELLOW, 1);
    private GraphViewSeriesStyle style05   = new GraphViewSeriesStyle(Color.MAGENTA, 1);

    private GraphViewSeriesStyle style01Lo   = new GraphViewSeriesStyle(Color.RED, 2);	
    private GraphViewSeriesStyle style02Lo   = new GraphViewSeriesStyle(Color.GREEN, 2);	
    private GraphViewSeriesStyle style03Lo   = new GraphViewSeriesStyle(Color.BLUE, 2);	
    private GraphViewSeriesStyle style04Lo   = new GraphViewSeriesStyle(Color.YELLOW, 2);
    private GraphViewSeriesStyle style05Lo   = new GraphViewSeriesStyle(Color.MAGENTA, 2);

    
    private String mStyle;

    private String startTimesBuffer = null;

    public void onCheckedChanged(CompoundButton arg0, boolean arg1)
    {
	this.refreshData();
    } 
}

