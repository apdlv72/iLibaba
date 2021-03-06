package com.apdlv.ilibaba.frotect;

import java.util.HashMap;
import java.util.Set;

import android.app.Dialog;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.frotect.FrotectActivity.FrotectBTDataCompleteListener;
import com.apdlv.ilibaba.util.NiceScale;
import com.apdlv.ilibaba.util.U;
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

    private View checkBoxContainer, unboundSensor;
    private int checkboxId[] = { R.id.checkBoxSensor1, R.id.checkBoxSensor2, R.id.checkBoxSensor3, R.id.checkBoxSensor4, R.id.checkBoxSensor5 };
    
    boolean isDuty()
    {
	return StatsDialog.DUTY.equals(mStyle);
    }

    boolean isPower()
    {
	return StatsDialog.POWER.equals(mStyle);
    }

    boolean isTemp()
    {
	return StatsDialog.TEMP.equals(mStyle);
    }
    
    private boolean isCost()
    {	
	return StatsDialog.COST.equals(mStyle);
    }

    public StatsDialog(FrotectActivity frotect, String style)
    {
	super(frotect, android.R.style.Theme_Light);
	requestWindowFeature(Window.FEATURE_NO_TITLE);
	getWindow().setFlags(
		WindowManager.LayoutParams.FLAG_FULLSCREEN,        
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
	
	this.frotect = frotect;
	this.mStyle = style;

	LayoutInflater inflater = this.getLayoutInflater();
	setContentView(inflater.inflate(R.layout.dialog_stats, null));

	this.checkBoxContainer = findViewById(R.id.checkBoxContainer);
	this.unboundSensor     = findViewById(R.id.checkBoxSensor5);

	if (isPower())
	{
	    //setTitle("Power consumption");
	    initPower();
	}
	else if (isTemp())
	{
	    //setTitle("Temperature history");
	    initTemp();
	}
	else if (isCost())
	{
	    //setTitle("Cost");
	    initCost();
	}
	else if (isDuty())
	{
	    //setTitle("Duty cycle");
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

	statistics = GraphParser.parseStats(frotect.statsBuffer.toString());
	minmaxHist = GraphParser.parseHistories(frotect.minmaxBuffer.toString());
	startTimes = GraphParser.parseStartTimes(frotect.starttimesBuffer.toString());

	refreshData();
    }


    public void onDataComplete(TYPE type, String data)
    {
	switch (type)
	{
	case STATS: 
	    statistics = GraphParser.parseStats(data);
	    refreshData(); 
	    break;

	case MINMAX:
	    minmaxHist  = GraphParser.parseHistories(data);
	    refreshData(); 
	    break;

	case STARTTIMES:
	    refreshData(); 
	    break;

	default:
	}
    }	


    void initDuty()
    {       
	// graph with dynamically genereated horizontal and vertical labels
	LineGraphView gv = new LineGraphView(frotect, "y: % on-time,        x: days");

	gv.setDrawingCacheQuality(GraphView.DRAWING_CACHE_QUALITY_HIGH);
	gv.getGraphViewStyle().setTextSize(10);
	//graphView.setDrawBackground(true);

	refreshDuty();

	//	graphView.setShowLegend(true);
	gv.setManualYAxisBounds(1, 0);

	gv.setViewPort(-4, 4);  
	gv.setScrollable(true);  
	//gv.setScalable(true);  
	gv.getGraphViewStyle().setNumHorizontalLabels(5);
	gv.getGraphViewStyle().setNumVerticalLabels(11);

	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.setBackgroundColor(Color.BLACK);

	layout.addView(mGraphView = gv);	
    }

    public GraphViewSeriesStyle getStyle(String key)
    {
	return getStyle(key, false);
    }

    public GraphViewSeriesStyle getStyle(String key, boolean lo)
    {
	GraphViewSeriesStyle style = styleWhite;
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
	    if (key.endsWith("1")) style = style01Hi;
	    if (key.endsWith("2")) style = style02Hi;
	    if (key.endsWith("3")) style = style03Hi;
	    if (key.endsWith("4")) style = style04Hi;
	    if (key.endsWith("5")) style = style05Hi;	    
	}
	return style;
    }

    void initPower()
    {       
	// graph with dynamically genereated horizontal and vertical labels
	LineGraphView gv = new LineGraphView(frotect, "y: kWh/day,        x: days");

	gv.setDrawingCacheQuality(GraphView.DRAWING_CACHE_QUALITY_HIGH);
	gv.getGraphViewStyle().setTextSize(10);

	@SuppressWarnings("unused")
        Double mm[] = refreshPower();

	//	graphView.setDrawBackground(true);
	//	graphView.setShowLegend(true);
	//	graphView.setManualYAxisBounds(20, 0);	
	gv.setViewPort(-4, 4);  
	gv.setScrollable(true);  
	//gv.setScalable(true);  
	gv.getGraphViewStyle().setNumHorizontalLabels(5);
	gv.getGraphViewStyle().setNumVerticalLabels(11);

	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.setBackgroundColor(Color.BLACK);

	layout.addView(mGraphView = gv);	
    }

    void initTemp()
    {       
	// graph with dynamically genereated horizontal and vertical labels
	LineGraphView graphView = new LineGraphView(frotect, "y: �C,        x: days");

	graphView.setDrawingCacheQuality(GraphView.DRAWING_CACHE_QUALITY_HIGH);
	graphView.getGraphViewStyle().setTextSize(10);
	//graphView.setDrawBackground(true);

	Double minMax[] = refreshTemp();

	graphView.setManualYAxisBounds(-10, -10);
	if (null!=minMax && 2==minMax.length) 
	{
	    Double min = minMax[0];
	    Double max = minMax[1];
	    if (null==min) min= 0.0;
	    if (null==max) max=10.0;

	    graphView.setManualYAxisBounds(Math.floor(min)-1, Math.ceil(max)+1);
	}	

	graphView.setViewPort(-14, 14);  // 14 days
	graphView.setScrollable(true);  
	//graphView.setScalable(true);  
	graphView.getGraphViewStyle().setNumHorizontalLabels(5);
	graphView.getGraphViewStyle().setNumVerticalLabels(21);

	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.setBackgroundColor(Color.BLACK);
	layout.addView(mGraphView = graphView);

	TextView xLabel = new TextView(getContext());
	xLabel.setText("days");
	layout.addView(xLabel);
    }

    private Double[] refreshData()
    {
	Double mm[] = null;
	if (StatsDialog.POWER.equals(mStyle))
	{
	    U.setVisible(checkBoxContainer);
	    U.removeView(unboundSensor);
	    mm = refreshPower();
	}
	else if (StatsDialog.TEMP.equals(mStyle))
	{
	    U.setVisible(checkBoxContainer);
	    mm = refreshTemp();
	}
	else if (StatsDialog.DUTY.equals(mStyle))
	{
	    U.setVisible(checkBoxContainer);
	    U.removeView(unboundSensor);
	    mm = refreshDuty();
	}	
	else if (StatsDialog.COST.equals(mStyle))
	{
	    U.setInvisibile(checkBoxContainer);
	    U.removeView(checkBoxContainer);

	    mm = refreshCost();
	}

	refreshAxisSettings(mm);        
	return mm;
    }

    private void refreshAxisSettings(Double[] mm)
    {
	if (null!=mm && null!=mm[0] && null!=mm[1])
	{
	    Double minX = mm[0];
	    Double maxX = mm[1];
	    Double minY = mm[2];
	    Double maxY = mm[3];
	    Log.e(TAG, "X: [" + minX + ", " + maxX + "], Y: [" + minY + ", " + maxY + "]");
	    
	    if (null==minY) minY= 0.0; 
	    if (null==maxY) maxY=10.0; 

	    if (null==minX) minX=-14.0; // 14 days;
	    if (null==maxX) maxX=  0.0; // today
	    
	    if (isDuty())
	    {
		minY = 0.0; maxY = 1.2; // sometimes slightly larger than 1.0 
	    }
	    else if (isPower() || isCost())
	    {
		minY = 0.0;
	    }
	    
	    double delta = maxY-minY;
	    if (delta<0.01)
	    {
		maxY = minY+1;
	    }	    
	    
    	    delta = maxX-minX;
    	    if (delta<0.1) minX=maxX-7; // days

	    NiceScale gcY = new NiceScale(minY, maxY);

	    Log.e(TAG, "Y-Axis: min=" + gcY.getNiceMin() + ", max=" + gcY.getNiceMax() + ", #ticks=" + gcY.getNumberOfTicks());
	    mGraphView.setManualYAxisBounds(gcY.getNiceMax(), gcY.getNiceMin());
	    mGraphView.getGraphViewStyle().setNumVerticalLabels(gcY.getNumberOfTicks());    	 
	    
	    int maxDay  = (int) Math.ceil(Math.abs(maxX));
	    int xTicks = maxDay+1; // show one tick pre day plus one extra (for y-axis)
	    if (maxDay<=4)
	    {
		xTicks = 2*maxDay + 1; // show 1 day in 4 intervals (6h each)  
	    }	    
	    else if (maxDay>7)
	    {
		//maxDay = 2*(maxDay/2+1);
		maxDay = 7;
		xTicks = maxDay+1; // show 10 days, user can scroll to see more
	    }
	    
	    Log.e(TAG, "X-Axis: maxDay=" + maxDay);
	    mGraphView.getGraphViewStyle().setNumHorizontalLabels(xTicks);	    
	    //mGraphView.setViewPort(-minDay, minDay);
	    mGraphView.setViewPort(0, maxDay);	    
	    
	    addStartTimes(gcY.getNiceMin(), gcY.getNiceMax());
	}
    }


    private Double[] refreshCost()
    {
	if (null==statistics) return null;

	Double minX=null, maxX=null, minY=null, maxY=null;

	mGraphView.removeAllSeries();	    
	for (String k : statistics.keySet())
	{
	    if (!k.equals("C")) continue;

	    GraphViewData[] values = statistics.get(k);	    
	    Double mm[] = findMinMax(values);
	    if (null==minX || (null!=mm[0] && mm[0]<minX)) minX=mm[0];
	    if (null==maxX || (null!=mm[1] && mm[1]>maxX)) maxX=mm[1];
	    if (null==minY || (null!=mm[2] && mm[2]<minY)) minY=mm[2];
	    if (null==maxY || (null!=mm[3] && mm[3]>maxY)) maxY=mm[3];

	    GraphViewSeriesStyle style = styleWhite;
	    GraphViewSeries series = new GraphViewSeries(k, style, values);
	    mGraphView.addSeries(series);

	    //dump(values);
	}
	//graphView.setManualYAxisBounds(1.0, 0);

	Double rv[] = { minX, maxX, minY, maxY };
	return rv;
    }

    
//    void dump(GraphViewData values[])
//    {
//	for (GraphViewData v : values)
//	{
//	    double x = v.getX();
//	    double y = v.getY();
//	    System.out.println("(" + x + "," + y + ") ");
//	}
//    }
//

    private Double[] refreshDuty()
    {
	if (null==statistics) return  null;

	Double minX=null, maxX=null, minY=null, maxY=null;

	mGraphView.removeAllSeries();

	for (String k : statistics.keySet())
	{
	    if (!k.startsWith("r")) continue;

	    GraphViewData[] values = statistics.get(k);	    
	    GraphViewSeriesStyle style = getStyle(k);

	    if (isChecked(k))
	    {	    
		Double mm[] = findMinMax(values);
		if (null==minX || (null!=mm[0] && mm[0]<minX)) minX=mm[0];
		if (null==maxX || (null!=mm[1] && mm[1]>maxX)) maxX=mm[1];
		if (null==minY || (null!=mm[2] && mm[2]<minY)) minY=mm[2];
		if (null==maxY || (null!=mm[3] && mm[3]>maxY)) maxY=mm[3];

		GraphViewSeries series = new GraphViewSeries(k, style, values);
		mGraphView.addSeries(series);
	    }    
	}

	Double rv[] = { minX, maxX, minY, maxY };
	return rv;
    }


    private Double[] refreshPower()
    {
	if (null==statistics) return null;

	Double minX=null, maxX=null, minY=null, maxY=null;

	mGraphView.removeAllSeries();
	for (String k : statistics.keySet())
	{
	    if (!k.startsWith("P")) continue;

	    GraphViewData[] values = statistics.get(k);	    
	    GraphViewSeriesStyle style = getStyle(k);
	    if (isChecked(k))
	    {
		Double mm[] = findMinMax(values);
		if (null==minX || (null!=mm[0] && mm[0]<minX)) minX=mm[0];
		if (null==maxX || (null!=mm[1] && mm[1]>maxX)) maxX=mm[1];
		if (null==minY || (null!=mm[2] && mm[2]<minY)) minY=mm[2];
		if (null==maxY || (null!=mm[3] && mm[3]>maxY)) maxY=mm[3];

		GraphViewSeries series = new GraphViewSeries(k, style, values);
		mGraphView.addSeries(series);		
	    }
	}

	Double rv[] = { minX, maxX, minY, maxY };
	return rv;
    }


    private Double[] refreshTemp()
    {
	if (null==minmaxHist) return null;

	mGraphView.removeAllSeries();	   
	Set<String> keySet = minmaxHist.keySet(); 
	System.out.println("refreshTemp: keys: " + keySet);

	Double minX=null, maxX=null, minY=null, maxY=null;

	String[] keys = minmaxHist.keySet().toArray(new String[0]);
	
	// iterate in reverse order such that last series lies bottom most
	for (int l=keys.length, i=l-1; i>=0; i--)
	//for (String key : minmaxHist.keySet())
	{
	    String key = keys[i];
	    GraphViewData[] values = minmaxHist.get(key);

	    GraphViewSeriesStyle style = getStyle(key, key.startsWith("lo"));
	    
	    // do not consider if key is checked or not since the y axis will
	    // become bumpy if unchecking a data series that was the origin of
	    // min/max value before unchecking.
	    //if (isChecked(key))
	    {
		Double mm[] = findMinMax(values);
		if (null==minX || (null!=mm[0] && mm[0]<minX)) minX=mm[0];
		if (null==maxX || (null!=mm[1] && mm[1]>maxX)) maxX=mm[1];
		if (null==minY || (null!=mm[2] && mm[2]<minY)) minY=mm[2];
		if (null==maxY || (null!=mm[3] && mm[3]>maxY)) maxY=mm[3];
	    }
	    // however DO consider checked state when adding these
	    if (isChecked(key))
	    {	
		GraphViewSeries series = new GraphViewSeries(key, style, values);
		mGraphView.addSeries(series);
	    }
	}

	Double rv[] = { minX, maxX, minY, maxY };
	return rv;
    }


    void addStartTimes(double min, double max)
    {
	if (null==startTimes) return;
	
	for (double daysAgo : startTimes)
	{
	    double delta = max-min;
	    double offs  = 0.1*delta;
	    // add small ticks on the bottom line that indicate the start times
	    GraphViewData d[]  = { new GraphViewData(daysAgo, min), new GraphViewData(daysAgo, min+offs) };
	    GraphViewSeries series = new GraphViewSeries("|", styleDarkGray, d);
	    mGraphView.addSeries(series);	    
	}
    }


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


    private static Double[] findMinMax(GraphViewData[] values)
    {
	Double minY = null, maxY = null;
	Double minX = null, maxX = null;

	for (GraphViewData v : values)
	{
	    double y = v.getY();
	    double x = v.getX();
	    if (null==minX || x<minX) minX=x;
	    if (null==maxX || x>maxX) maxX=x;
	    if (null==minY || y<minY) minY=y;
	    if (null==maxY || y>maxY) maxY=y;
	}

	Double rv[] = { minX, maxX, minY, maxY };
	return rv;
    }


    void initCost()
    {       
	// graph with dynamically generated horizontal and vertical labels
	LineGraphView graphView = new LineGraphView(frotect, "y: �/day,        x: days");

	graphView.setDrawingCacheQuality(GraphView.DRAWING_CACHE_QUALITY_HIGH);
	graphView.getGraphViewStyle().setTextSize(10);

	refreshCost();

	graphView.setViewPort(-4, 4);  
	graphView.setScrollable(true);  
	//graphView.setScalable(true);  
	graphView.getGraphViewStyle().setNumHorizontalLabels(5);
	graphView.getGraphViewStyle().setNumVerticalLabels(11);
	
	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.setBackgroundColor(Color.BLACK);

	layout.addView(mGraphView = graphView);	
    }

    private GraphViewSeriesStyle styleWhite    = new GraphViewSeriesStyle(Color.WHITE, 2);	
    private GraphViewSeriesStyle styleDarkGray = new GraphViewSeriesStyle(Color.DKGRAY,3);	

    private GraphViewSeriesStyle style01Hi = new GraphViewSeriesStyle(Color.rgb(0xff,    0,    0), 1); // RED,     1);	
    private GraphViewSeriesStyle style02Hi = new GraphViewSeriesStyle(Color.rgb(   0, 0xff,    0), 1); // GREEN,   1);	
    private GraphViewSeriesStyle style03Hi = new GraphViewSeriesStyle(Color.rgb(   0,    0, 0xff), 1); // BLUE,    1);	
    private GraphViewSeriesStyle style04Hi = new GraphViewSeriesStyle(Color.rgb(0xff,    0, 0xff), 1); // MAGENTA, 1);
    private GraphViewSeriesStyle style05Hi = new GraphViewSeriesStyle(Color.rgb(0xff, 0xff,    0), 1); // YELLOW,  1);

    private GraphViewSeriesStyle style01Lo = new GraphViewSeriesStyle(Color.rgb(0x80,    0,    0), 2); // RED,     1);	
    private GraphViewSeriesStyle style02Lo = new GraphViewSeriesStyle(Color.rgb(   0, 0x80,    0), 2); // GREEN,   1);	
    private GraphViewSeriesStyle style03Lo = new GraphViewSeriesStyle(Color.rgb(   0,    0, 0x80), 2); // BLUE,    1);	
    private GraphViewSeriesStyle style04Lo = new GraphViewSeriesStyle(Color.rgb(0x80,    0, 0x80), 2); // MAGENTA, 1);
    private GraphViewSeriesStyle style05Lo = new GraphViewSeriesStyle(Color.rgb(0x80, 0x80,    0), 2); // YELLOW,  1);


    private String mStyle;

    private Double[] startTimes = null;

    public static final String POWER = "power";

    public static final String TEMP = "averageTemp";

    public static final String COST = "cost";

    public static final String DUTY = "duty";

    public void onCheckedChanged(CompoundButton arg0, boolean arg1)
    {
	this.refreshData();
    } 
}

