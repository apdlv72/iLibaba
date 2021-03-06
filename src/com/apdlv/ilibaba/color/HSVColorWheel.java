package com.apdlv.ilibaba.color;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;


public class HSVColorWheel  extends View {

    private static final float SCALE = 2f;
    private static final float FADE_OUT_FRACTION = 0.03f;

    private static final int POINTER_LINE_WIDTH_DP = 2;
    private static final int POINTER_LENGTH_DP = 10;

    private final Context context;

    private OnColorSelectedListener listener;

    public HSVColorWheel(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
	this.context = context;
	init();
    }

    public HSVColorWheel(Context context, AttributeSet attrs) {
	super(context, attrs);
	this.context = context;
	init();
    }

    public HSVColorWheel(Context context) {
	super(context);
	this.context = context;
	init();
    }

    private int scale;
    private int pointerLength;
    private int innerPadding;
    private Paint pointerPaint = new Paint();
    private Paint borderPaint = new Paint();

    private void init() {
	float density = context.getResources().getDisplayMetrics().density;
	scale = (int) (density * SCALE);
	pointerLength = (int) (density * POINTER_LENGTH_DP );
	pointerPaint.setStrokeWidth(  (int) (density * POINTER_LINE_WIDTH_DP ) );
	innerPadding = pointerLength / 2;

	borderPaint.setStrokeWidth(  (int) (density * POINTER_LINE_WIDTH_DP ) );
	borderPaint.setAntiAlias(true);
	borderPaint.setColor(Color.GRAY);
    }

    public void setListener( OnColorSelectedListener listener ) {
	this.listener = listener;
    }

    float[] colorHsv = { 0f, 0f, 1f };
    private boolean isEnabled;


    public void setColor( int color ) {
	Color.colorToHSV(color, colorHsv);
	invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
	if ( bitmap != null ) 
	{
	    if (isEnabled)
	    {
		canvas.drawBitmap(bitmap, null, rect, null);
	    }
	    else
	    {
		canvas.drawCircle((rect.left+rect.right)/2, (rect.top+rect.bottom)/2, rect.width()/2, borderPaint);
		canvas.drawText("not connected", 60, (rect.top+rect.bottom)/2, pointerPaint); 
	    }

	    if (isEnabled)
	    {
		float hueInPiInterval = colorHsv[0] / 180f * (float)Math.PI;
		selectedPoint.x = rect.left + (int) (-FloatMath.cos( hueInPiInterval ) * colorHsv[1] * innerCircleRadius + fullCircleRadius);
		selectedPoint.y = rect.top + (int) (-FloatMath.sin( hueInPiInterval ) * colorHsv[1] * innerCircleRadius + fullCircleRadius);

		canvas.drawLine( selectedPoint.x - pointerLength, selectedPoint.y, selectedPoint.x + pointerLength, selectedPoint.y, pointerPaint );
		canvas.drawLine( selectedPoint.x, selectedPoint.y - pointerLength, selectedPoint.x, selectedPoint.y + pointerLength, pointerPaint );
	    }
	}
    }

    @Override public void setEnabled(boolean enabled) 
    {
	super.setEnabled(enabled);
	this.isEnabled = enabled;

    };

    private Rect rect;
    private Bitmap bitmap;

    private int[] pixels;
    private float innerCircleRadius;
    private float fullCircleRadius;

    private int scaledWidth;
    private int scaledHeight;
    private int[] scaledPixels;

    private float scaledInnerCircleRadius;
    private float scaledFullCircleRadius;
    private float scaledFadeOutSize;

    private Point selectedPoint = new Point();
    private int mLastColor = 0xffffff;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	super.onSizeChanged(w, h, oldw, oldh);

	rect = new Rect( innerPadding, innerPadding, w - innerPadding, h - innerPadding );
	bitmap = Bitmap.createBitmap( w - 2 * innerPadding, h - 2 * innerPadding, Config.ARGB_8888 );

	fullCircleRadius = Math.min( rect.width(), rect.height() ) / 2;
	innerCircleRadius = fullCircleRadius * ( 1 - FADE_OUT_FRACTION );

	scaledWidth = rect.width() / scale;
	scaledHeight = rect.height() / scale;
	scaledFullCircleRadius = Math.min( scaledWidth, scaledHeight ) / 2;
	scaledInnerCircleRadius = scaledFullCircleRadius * ( 1 - FADE_OUT_FRACTION );
	scaledFadeOutSize = scaledFullCircleRadius - scaledInnerCircleRadius;
	scaledPixels = new int[ scaledWidth * scaledHeight ];
	pixels = new int[ rect.width() * rect.height() ];

	createBitmap();
    }

    private void createBitmap() {
	int w = rect.width();
	int h = rect.height();

	float[] hsv = new float[] { 0f, 0f, 1f };
	int alpha = 255;

	int x = (int) -scaledFullCircleRadius, y = (int) -scaledFullCircleRadius;
	for ( int i = 0; i < scaledPixels.length; i++ ) {
	    if ( i % scaledWidth == 0 ) {
		x = (int) -scaledFullCircleRadius;
		y++;
	    } else {
		x++;
	    }

	    double centerDist = Math.sqrt( x*x + y*y );
	    if ( centerDist <= scaledFullCircleRadius ) {
		hsv[ 0 ] = (float) (Math.atan2( y, x ) / Math.PI * 180f) + 180;
		hsv[ 1 ] = (float) (centerDist / scaledInnerCircleRadius);
		if ( centerDist <= scaledInnerCircleRadius ) {
		    alpha = 255;
		} else {
		    alpha = 255 - (int) ((centerDist - scaledInnerCircleRadius) / scaledFadeOutSize * 255);
		}
		scaledPixels[ i ] = Color.HSVToColor( alpha, hsv );
	    } else {
		scaledPixels[ i ] = 0x00000000;
	    }
	}

	int scaledX, scaledY;
	for( x = 0; x < w; x++ ) {
	    scaledX = x / scale;
	    if ( scaledX >= scaledWidth ) scaledX = scaledWidth - 1;
	    for ( y = 0; y < h; y++ ) {
		scaledY = y / scale;
		if ( scaledY >= scaledHeight ) scaledY = scaledHeight - 1;
		pixels[ x * h + y ] = scaledPixels[ scaledX * scaledHeight + scaledY ];
	    }
	}

	bitmap.setPixels( pixels, 0, w, 0, 0, w, h );

	invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	int maxWidth = MeasureSpec.getSize( widthMeasureSpec );
	int maxHeight = MeasureSpec.getSize( heightMeasureSpec );

	int width, height;
	/*
	 * Make the view quadratic, with height and width equal and as large as possible
	 */
	width = height = Math.min( maxWidth, maxHeight );

	setMeasuredDimension( width, height );
    }

    public int getColorForPoint( int x, int y, float[] hsv ) {
	x -= fullCircleRadius;
	y -= fullCircleRadius;
	double centerDist = Math.sqrt( x*x + y*y );
	hsv[ 0 ] = (float) (Math.atan2( y, x ) / Math.PI * 180f) + 180;
	hsv[ 1 ] = Math.max( 0f, Math.min( 1f, (float) (centerDist / innerCircleRadius) ) );
	return Color.HSVToColor( hsv );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	int action = event.getActionMasked();
	switch ( action ) {
	case MotionEvent.ACTION_DOWN:
	case MotionEvent.ACTION_MOVE:
	    if ( listener != null ) {
		mLastColor = getColorForPoint( (int)event.getX(), (int)event.getY(), colorHsv );
		listener.colorSelected( mLastColor );
	    }
	    invalidate();
	    return true;
	}
	return super.onTouchEvent(event);
    }

    public int getSelectedColor()
    {
	return mLastColor ;
    }

 }