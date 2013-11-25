package com.apdlv.ilibaba.frotect;

public class GraphScaler
{
    private double oldMin;
    private double oldMax;
    private int    numTicks;
    private double newMin;
    private double newMax;


    public GraphScaler(double min, double max)
    {
	this.oldMin = min;
	this.oldMax = max;
//	System.out.println("oldMin:  " + max);
//	System.out.println("oldMax:  " + min);
	
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
	this.newMin = sign(min)*minTicks*sub;
	this.newMax = sign(max)*maxTicks*sub;
//	
//	System.out.println("numTicks: " + numTicks);
//	System.out.println("newMin: " + newMin);
//	System.out.println("newMax: " + newMax); 
    }

    
    public int getTicks()
    {
	return numTicks;
    }
    
    public double getMin()
    {
	return newMin;
    }
    
    public double getMax()
    {
	return newMax;
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
    
    public static void main(String[] args)
    {
	GraphScaler gs = new GraphScaler(-15, 5.1);		
    }
    
}
