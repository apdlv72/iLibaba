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
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.frotect.FrotectActivity.FrotectBTDataCompleteListener;
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

    private View checkBoxContainer;

    public StatsDialog(FrotectActivity frotect, String style)
    {
	super(frotect);
	requestWindowFeature(Window.FEATURE_NO_TITLE); 

	this.frotect = frotect;
	this.mStyle = style;
	this.checkBoxContainer = findViewById(R.id.checkBoxContainer);

	LayoutInflater inflater = this.getLayoutInflater();
	setContentView(inflater.inflate(R.layout.dialog_stats, null));

	if (StatsDialog.POWER.equals(mStyle))
	{
	    //setTitle("Power consumption");
	    initPower();
	}
	else if (StatsDialog.TEMP.equals(mStyle))
	{
	    //setTitle("Temperature history");
	    initTemp();
	}
	else if (StatsDialog.COST.equals(mStyle))
	{
	    //setTitle("Cost");
	    initCost();
	}
	else if (StatsDialog.DUTY.equals(mStyle))
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
	    startTimesBuffer = data;
	    refreshData(); 
	    break;

	default:
	}
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
	layout.setBackgroundColor(Color.BLACK);

	layout.addView(mGraphView = gv);	
    }

    public GraphViewSeriesStyle getStyle(String key)
    {
	return getStyle(key, false);
    }

    public GraphViewSeriesStyle getStyle(String key, boolean lo)
    {
	GraphViewSeriesStyle style = whStyle;
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

	Double mm[] = refreshPower();

	//	graphView.setDrawBackground(true);
	//	graphView.setShowLegend(true);
	//	graphView.setManualYAxisBounds(20, 0);	
	gv.setViewPort(-4, 4);  
	gv.setScrollable(true);  
	gv.setScalable(true);  
	gv.getGraphViewStyle().setNumHorizontalLabels(5);
	gv.getGraphViewStyle().setNumVerticalLabels(11);

	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.setBackgroundColor(Color.BLACK);

	layout.addView(mGraphView = gv);	
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

	Double minMax[] = refreshTemp();

	//	graphView.setShowLegend(true);
	//	graphView.setLegendAlign(LegendAlign.MIDDLE);  
	//	graphView.setLegendWidth(100);  
	// graphView.setManualYAxisBounds(30, -15);
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
	graphView.setScalable(true);  
	graphView.getGraphViewStyle().setNumHorizontalLabels(5);
	graphView.getGraphViewStyle().setNumVerticalLabels(21);

	LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
	layout.setBackgroundColor(Color.BLACK);
	layout.addView(mGraphView = graphView);

	TextView xLabel = new TextView(getContext());
	xLabel.setText("days");
	layout.addView(xLabel);
    }

    int checkboxId[] = { R.id.checkBoxSensor1, R.id.checkBoxSensor2, R.id.checkBoxSensor3, R.id.checkBoxSensor4, R.id.checkBoxSensor5 };     

    private Double[] refreshData()
    {
	Double mm[] = null;
	if (StatsDialog.POWER.equals(mStyle))
	{
	    U.setVisible(checkBoxContainer);
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
	    mm = refreshDuty();
	}	
	else if (StatsDialog.COST.equals(mStyle))
	{
	    U.setInvisibile(checkBoxContainer);
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
	    if (null==minY) minY= 0.0; if (null==minX) minX=0.0;  // today;
	    if (null==maxY) maxY=10.0; if (null==maxX) maxX=14.0; // 14 days;

	    GraphScaler gc = new GraphScaler(minY, maxY);

	    Log.e(TAG, "Y-Axis: min=" + gc.getMin() + ", max=" + gc.getMax() + ", ticks=" + gc.getTicks());
	    mGraphView.setManualYAxisBounds(gc.getMax(), gc.getMin());
	    mGraphView.getGraphViewStyle().setNumVerticalLabels(gc.getTicks());    	 
	    
	    int maxDay = (int) Math.ceil(maxX); 
	    Log.e(TAG, "X-Axis: days=" + maxDay);
	    mGraphView.getGraphViewStyle().setNumHorizontalLabels(maxDay);	    
	    mGraphView.setViewPort(-maxDay, maxDay);
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

	    GraphViewSeriesStyle style = whStyle;
	    GraphViewSeries series = new GraphViewSeries(k, style, values);
	    mGraphView.addSeries(series);


	}
	//graphView.setManualYAxisBounds(1.0, 0);
	refreshStartTimes(0,1);

	Double rv[] = { minX, maxX, minY, maxY };
	return rv;
    }


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

	refreshStartTimes(0,1.0);

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

	refreshStartTimes(0,100);

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

	for (String key : minmaxHist.keySet())
	{
	    GraphViewData[] values = minmaxHist.get(key);

	    GraphViewSeriesStyle style = getStyle(key, key.startsWith("lo"));
	    if (isChecked(key))
	    {
		Double mm[] = findMinMax(values);
		if (null==minX || (null!=mm[0] && mm[0]<minX)) minX=mm[0];
		if (null==maxX || (null!=mm[1] && mm[1]>maxX)) maxX=mm[1];
		if (null==minY || (null!=mm[2] && mm[2]<minY)) minY=mm[2];
		if (null==maxY || (null!=mm[3] && mm[3]>maxY)) maxY=mm[3];

		GraphViewSeries series = new GraphViewSeries(key, style, values);
		mGraphView.addSeries(series);
	    }
	}
	refreshStartTimes(-10,30);

	Double rv[] = { minX, maxX, minY, maxY };
	return rv;
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
		Map<String, Double> map = MessageParser.parseStartTime(line);
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
	layout.setBackgroundColor(Color.BLACK);

	layout.addView(mGraphView = graphView);	
	refreshStartTimes(0,10);
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


    private GraphViewSeriesStyle whStyle   = new GraphViewSeriesStyle(Color.WHITE, 3);	

    private GraphViewSeriesStyle style01   = new GraphViewSeriesStyle(Color.RED,     1);	
    private GraphViewSeriesStyle style02   = new GraphViewSeriesStyle(Color.GREEN,   1);	
    private GraphViewSeriesStyle style03   = new GraphViewSeriesStyle(Color.BLUE,    1);	
    private GraphViewSeriesStyle style04   = new GraphViewSeriesStyle(Color.MAGENTA, 1);
    private GraphViewSeriesStyle style05   = new GraphViewSeriesStyle(Color.YELLOW,  1);

    private GraphViewSeriesStyle style01Lo   = new GraphViewSeriesStyle(Color.RED,     2);	
    private GraphViewSeriesStyle style02Lo   = new GraphViewSeriesStyle(Color.GREEN,   2);	
    private GraphViewSeriesStyle style03Lo   = new GraphViewSeriesStyle(Color.BLUE,    2);	
    private GraphViewSeriesStyle style04Lo   = new GraphViewSeriesStyle(Color.MAGENTA, 2);
    private GraphViewSeriesStyle style05Lo   = new GraphViewSeriesStyle(Color.YELLOW,  2);


    private String mStyle;

    private String startTimesBuffer = null;

    public static final String POWER = "power";

    public static final String TEMP = "temp";

    public static final String COST = "cost";

    public static final String DUTY = "duty";

    public void onCheckedChanged(CompoundButton arg0, boolean arg1)
    {
	this.refreshData();
    } 
}

