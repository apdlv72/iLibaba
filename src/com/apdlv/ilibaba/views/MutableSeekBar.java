package com.apdlv.ilibaba.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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
	inactiveThumb = null;

	/* does not work
	Resources res = getResources();
	inactiveThumb = res.getDrawable(R.drawable.seek_thumb_inactive);
	*/

	/* does not work
	Bitmap gray = toGrayscale(bitmap); //new BitmapDrawable(bitmap);
	inactiveThumb = new BitmapDrawable(gray);
	inactiveThumb.setAlpha(255);	
	inactiveThumb.setBounds(activeThumb.getBounds());
	inactiveThumb.setChangingConfigurations(activeThumb.getChangingConfigurations());
	inactiveThumb.setVisible(true, true);
	 */
    }

    @SuppressWarnings("unused")
    private Bitmap toGrayscale(Bitmap bmpOriginal)
    {        
	int width, height;
	height = bmpOriginal.getHeight();
	width = bmpOriginal.getWidth();    

	Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	Canvas c = new Canvas(bmpGrayscale);
	Paint paint = new Paint();
	ColorMatrix cm = new ColorMatrix();
	cm.setSaturation(0);
	ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
	paint.setColorFilter(f);
	c.drawBitmap(bmpOriginal, 0, 0, paint);
	return bmpGrayscale;
    }


    @Override
    public void setThumb(Drawable thumb)
    {
	// TODO Auto-generated method stub
	super.setThumb(thumb);
	activeThumb = thumb;
    }

    @Override
    public void setEnabled(boolean state) 
    {
	super.setEnabled(state);
	if (state)
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
