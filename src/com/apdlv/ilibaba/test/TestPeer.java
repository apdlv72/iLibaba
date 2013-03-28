package com.apdlv.ilibaba.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class TestPeer
{
    public static void main(String[] args) throws Exception
    {
	String devName = "/dev/tty.SeriellerAnschluss";
	System.out.println("TestPeer");

	while (true)
	{
	    System.out.println("Opening " + devName);
	    FileInputStream  fis = new FileInputStream(devName);
	    FileOutputStream fos = new FileOutputStream(devName);

	    System.out.println("fis: " + fis);
	    System.out.println("fos: " + fos);

	    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

	    boolean done = false;
	    while (!done)
	    {
		System.out.println("Waiting for command");
		String line = br.readLine();
		System.out.println("GOT: " + line);
		done = (null==line);
		    
		if (!done)
		{
		    System.out.println("READ:  " + line);
		    if (line.startsWith("t"))
		    {
			line = String.format("T%x", (long)(0xFFFFFFFFL*Math.random()));
		    }
		    else if (line.startsWith("o"))
		    {
			line = "OOPEN";
		    }
		    else
		    {
			line = "EERROR";
		    }

		    if (null!=line)
		    {
			System.out.println("WRITE: " + line);
			bw.append(line + "\n");
			bw.flush();
		    }
		}
	    }
	    
	    System.out.println("Closing " + devName);
	    br.close();
	    bw.close();
	}
    }
}
