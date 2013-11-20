package com.apdlv.ilibaba.frotect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.apdlv.ilibaba.R;
import com.apdlv.ilibaba.bt.SPPService;
import com.apdlv.ilibaba.bt.SPPConnection;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;


public class TempActivity extends Activity
{
    private static final String TAG = TempActivity.class.getSimpleName();
    private HashMap<String, GraphViewData[]> statistics;
    private HashMap<String, GraphViewData[]> minmaxHist;

    private GraphView mGraphView;

    @Override
    protected void onNewIntent(Intent intent)
    {
	super.onNewIntent(intent);
	System.out.println("intent: " + intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.dialog_stats);

	mStyle = getIntent().getAction();      
	if (StatsDialog.POWER.equals(mStyle))
	{
	    initPower();
	}
	if (StatsDialog.TEMP.equals(mStyle))
	{
	    initTemp();
	}
	if (StatsDialog.COST.equals(mStyle))
	{
	    initCost();
	}
	if (StatsDialog.DUTY.equals(mStyle))
	{
	    initDuty();
	}

	statisticsBuffer = new StringBuilder(); //Parser.TEST_STATISTICS);
	minmaxHistBuffer = new StringBuilder(); // Parser.TEST_MINMAXHIST);       

	refreshData();
    }


    @Override
    protected void onStart()
    {
	super.onStart();

	Intent intent = new Intent(this, SPPService.class);
	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);			
    }


    @Override
    protected void onStop() 
    {
	super.onStop();
	Log.d(TAG, "Unbinding from service");
	mConnection.unbind(this);
    };


    private void refreshData()
    {
	if (null!=statisticsBuffer)
	{
	    statistics = Parser.parseStats(statisticsBuffer.toString());
	}
	if (null!=minmaxHistBuffer)
	{
	    minmaxHist  = Parser.parseHistories(minmaxHistBuffer.toString());
	}

	if (StatsDialog.POWER.equals(mStyle))
	{
	    refreshPower();
	}
	if (StatsDialog.TEMP.equals(mStyle))
	{
	    refreshTemp();
	}
	if (StatsDialog.COST.equals(mStyle))
	{
	    refreshCost();
	}
	if (StatsDialog.DUTY.equals(mStyle))
	{
	    refreshDuty();
	}	
    }

    void refreshStartTimes(double min, double max)
    {
	if (null==startTimesBuffer) return;
	StringReader sr = new StringReader(startTimesBuffer.toString());
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
	LineGraphView gv = new LineGraphView(this, "Duty cycle %");

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
	    GraphViewSeriesStyle style = grayStyle;
	    if (k.endsWith("1")) style = style01;
	    if (k.endsWith("2")) style = style02;
	    if (k.endsWith("3")) style = style03;
	    if (k.endsWith("4")) style = style04;

	    GraphViewSeries series = new GraphViewSeries(k, style, values);
	    for (GraphViewData v : values)
	    {
		System.out.println("refreshDuty: " + v.valueX + "," + v.valueY);
	    }

	    mGraphView.addSeries(series);
	}

	refreshStartTimes(0,1.0);
    }

    void initPower()
    {       
	// graph with dynamically genereated horizontal and vertical labels
	LineGraphView gv = new LineGraphView(this, "Power kWh/day");

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
	    GraphViewSeriesStyle style = grayStyle;
	    if (k.endsWith("1")) style = style01;
	    if (k.endsWith("2")) style = style02;
	    if (k.endsWith("3")) style = style03;
	    if (k.endsWith("4")) style = style04;

	    GraphViewSeries series = new GraphViewSeries(k, style, values);
	    mGraphView.addSeries(series);
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
	LineGraphView graphView = new LineGraphView(this, "Temperature °C");

	graphView.setDrawingCacheQuality(GraphView.DRAWING_CACHE_QUALITY_HIGH);
	graphView.getGraphViewStyle().setTextSize(10);
	//graphView.setDrawBackground(true);

	// custom static labels
	//graphView.setHorizontalLabels(new String[] {"7", "6", "5", "4", "3", "2", "1", "0"});
	//graphView.setVerticalLabels(new String[] {"8°", "6°", "4°", "2°",  "0°" });
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

    private void refreshTemp()
    {
	if (null==minmaxHist) return;

	mGraphView.removeAllSeries();	    
	for (String k : minmaxHist.keySet())
	{
	    GraphViewData[] values = minmaxHist.get(k);

	    GraphViewSeriesStyle style = grayStyle;
	    if (k.startsWith("hi")) style = hiStyle;
	    if (k.startsWith("lo")) style = loStyle;

	    GraphViewSeries series = new GraphViewSeries(k, style, values);
	    mGraphView.addSeries(series);	    
	}
	refreshStartTimes(-10,30);
    }

    void initCost()
    {       
	// graph with dynamically genereated horizontal and vertical labels
	LineGraphView graphView = new LineGraphView(this, "Cost €/day");

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
	    if (k.endsWith("1")) style = style01;
	    if (k.endsWith("2")) style = style02;
	    if (k.endsWith("3")) style = style03;
	    if (k.endsWith("4")) style = style04;

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


    private void handleCommand(String line)
    {
	if (line.startsWith("SH") || line.startsWith("SD") || line.startsWith("SW"))
	{
	    if (null==statisticsBuffer) statisticsBuffer=new StringBuilder();
	    statisticsBuffer.append(line).append("\n");
	}
	else if (line.startsWith("MM"))
	{
	    if (null==minmaxHistBuffer) minmaxHistBuffer=new StringBuilder();
	    minmaxHistBuffer.append(line).append("\n");	    
	}
	else if (line.startsWith("T.") || line.startsWith("T:"))
	{
	    startTimesBuffer.append(line).append("\n");
	}
	else if (line.startsWith("EV: "))
	{
	    // received an change event while aiting for end of stats. remember this and request when complete
	    if (mUpdateOngoing)	mUpdateSuppressed=true; else requestUpdate();
	}
	else if (line.startsWith("S:")) // stats complete
	{
	    // there was a change event while reading last update. assume its out of sync and request again
	    if (mUpdateSuppressed) requestUpdate(); else refreshData();	    
	}
    }

    private void requestUpdate()
    {
	statisticsBuffer = minmaxHistBuffer = null;
	mConnection.sendLine("\n");
	mConnection.sendLine("D\n"); // request device to send complete status, lo/hi history etc.
	mUpdateOngoing = true;
	mUpdateSuppressed = false;
    }

    private final Handler mHandler = new Handler() 
    {

	@Override
	public void handleMessage(Message msg) 
	{
	    switch (msg.what) 
	    {

	    case SPPService.MESSAGE_HELLO:
		Toast.makeText(getApplicationContext(), "TempActivity connected to service", Toast.LENGTH_SHORT).show();
		requestUpdate();
		break;

	    case SPPService.MESSAGE_READLINE:                
		String line = (String) msg.obj;
		Log.d(TAG, "got msg: " + line);
		handleCommand(line);
		break;

	    default:
		Log.d(TAG, "unhandled message: " + msg);
	    }
	}

    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.temp_chart, menu);
	return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) 
    {
	switch (item.getItemId()) 
	{
	case R.id.menu_frotect_refresh:
	    mConnection.sendLine("D\n");
	    break;
	}

	return false;
    }


    private SPPConnection mConnection = new SPPConnection(mHandler);

    private StringBuilder startTimesBuffer = new StringBuilder();
    private StringBuilder minmaxHistBuffer = new StringBuilder();
    private StringBuilder statisticsBuffer = new StringBuilder();
    private boolean mUpdateOngoing  = false;
    private boolean mUpdateSuppressed = false;

    private GraphViewSeriesStyle whStyle   = new GraphViewSeriesStyle(Color.YELLOW, 5);	
    private GraphViewSeriesStyle loStyle   = new GraphViewSeriesStyle(Color.BLUE, 5);	
    private GraphViewSeriesStyle hiStyle   = new GraphViewSeriesStyle(Color.RED, 5);	
    private GraphViewSeriesStyle grayStyle = new GraphViewSeriesStyle(Color.GRAY, 5);	
    private GraphViewSeriesStyle style01   = new GraphViewSeriesStyle(Color.RED, 1);	
    private GraphViewSeriesStyle style02   = new GraphViewSeriesStyle(Color.GREEN, 1);	
    private GraphViewSeriesStyle style03   = new GraphViewSeriesStyle(Color.BLUE, 1);	
    private GraphViewSeriesStyle style04   = new GraphViewSeriesStyle(Color.YELLOW, 1);
    private String mStyle;	
}

