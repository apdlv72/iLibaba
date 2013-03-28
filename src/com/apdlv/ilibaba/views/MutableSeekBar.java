package com.apdlv.ilibaba.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class MutableSeekBar extends SeekBar
{

    public MutableSeekBar(Context context, AttributeSet attrs, int defStyle)
    {
	super(context, attrs, defStyle);
	init();
    }

    public MutableSeekBar(Context context, AttributeSet attrs)
    {
	super(context, attrs);
	init();
    }

    public MutableSeekBar(Context context)
    {
	super(context);
	init();
    }
    
    private void init()
    {
	Bitmap bitmap = Bitmap.createBitmap(10, 10, Config.RGB_565);
	bitmap.eraseColor(Color.GRAY);
	inactiveThumb = new BitmapDrawable(bitmap);
    }
    
    @Override
    public void setThumb(Drawable thumb)
    {
        // TODO Auto-generated method stub
        super.setThumb(thumb);
        activeThumb = thumb;
    }

    @Override
    public void setEnabled(boolean enabled) 
    {
	super.setEnabled(enabled);
	if (enabled)
	{
	    super.setThumb(activeThumb);
	}
	else
	{
	    super.setThumb(inactiveThumb);
	}
    };
    
    private Drawable activeThumb;
    private Drawable inactiveThumb;

}
