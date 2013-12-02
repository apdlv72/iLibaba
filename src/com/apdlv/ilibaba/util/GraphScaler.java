package com.apdlv.ilibaba.util;

public class GraphScaler
{
    @SuppressWarnings("unused")
    private double actMin, actMax;
    private int    numTicks;
    private double niceMin;
    private double niceMax;


    public GraphScaler(double min, double max)
    {
	this.actMin = min;
	this.actMax = max;
//	System.out.println("actMin:  " + max);
//	System.out.println("actMax:  " + min);
	
	double delta = abs(max-min);
	double expon = Math.floor(Math.log10(delta));
//	System.out.println("expon: " + expon);
	
	//double main = Math.pow(10, expon);
	//System.out.println("main:  " + main);
	double sub  = Math.pow(10, expon-1);
	System.out.println("sub:   " + sub);

	int minTicks = (int) ceil(abs(min)/sub);
	int maxTicks = (int) ceil(abs(max)/sub);
//	System.out.println("minTicks: " + minTicks);
//	System.out.println("maxTicks: " + maxTicks);
	
	this.numTicks = minTicks+maxTicks+1;
	this.niceMin = sign(min)*minTicks*sub;
	this.niceMax = sign(max)*maxTicks*sub;
//	
//	System.out.println("numTicks: " + numTicks);
//	System.out.println("niceMin: " + niceMin);
//	System.out.println("niceMax: " + niceMax); 
    }

    
    public int getNumberOfTicks()
    {
	return numTicks;
    }
    
    public double getNiceMin()
    {
	return niceMin;
    }
    
    public double getNiceMax()
    {
	return niceMax;
    }
    
    private static int sign(double d)
    {
	return d<0 ? -1 : 1;
    }


    private double ceil(double d)
    {
	return Math.ceil(d);
    }


    private static double abs(double d)
    {
	return Math.abs(d);
    }
    
//    public static void main(String[] args)
//    {
//	GraphScaler gs = new GraphScaler(-15, 5.1);		
//    }
    
}
