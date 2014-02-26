package com.apdlv.ilibaba.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

public class LastStatusHelper
{
    final static String FILENAME = "last_activity.txt";

    public static void startActivity(Activity current, Class<? extends Activity> next)
    {
	saveActivity(current, next);
        Intent i = new Intent(current, next);
        current.startActivity(i);
        current.finish();
    }
    

    public static void saveCurrActivity(Activity activity)
    {
	saveActivity(activity, activity.getClass());
    }

    
    public static boolean startLastActivity(Activity activity)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(activity.openFileInput(FILENAME)));
            String nextClassName = reader.readLine();
            reader.close();
            
            if (null==nextClassName || nextClassName.length()<3)
            {
        	return false;
            }
            
            String currClassName = activity.getClass().getName();
            if (currClassName.equals(nextClassName))
            {
        	return false;
            }
            
            @SuppressWarnings("unchecked")
            Class<? extends Activity> clazz = (Class<? extends Activity>) Class.forName(nextClassName);
            if (null==clazz)
            {
        	return false;
            }
            
            Intent i = new Intent(activity, clazz);
            activity.startActivity(i);
            activity.finish();
	    reader.close();
	    
	    return true;
        } 
        catch (Exception e)
        {
            Toast.makeText(activity, "startLastActivity: " + e, Toast.LENGTH_LONG).show();
        }
        return false;
    }
    

    private static void saveActivity(Activity activity, Class<? extends Activity> clazz)
    {
	FileOutputStream fos = null;
        try
        {
            fos = activity.openFileOutput(FILENAME, Activity.MODE_PRIVATE);
	    fos.write(clazz.getName().getBytes());
        }
        catch (Exception e)
        {
            Toast.makeText(activity, "saveActivity: " + e, Toast.LENGTH_LONG).show();
        }
        finally
        {
            if (null!=fos) try { fos.close(); } catch (Exception e) {}            
        }        
    }    
    
}
