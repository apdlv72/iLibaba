// https://github.com/jesperborgstrup/buzzingandroid/tree/master/src/com/buzzingandroid/ui
package com.apdlv.ilibaba.hsv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;


public class HSVColorPickerDialog extends AlertDialog {

    
	private static final int PADDING_DP = 20;

	private static final int CONTROL_SPACING_DP = 20;
	private static final int SELECTED_COLOR_HEIGHT_DP = 50;
	private static final int BORDER_DP = 1;
	private static final int BORDER_COLOR = Color.BLACK;

	private final OnColorSelectedListener listener;
	private int selectedColor;

	public HSVColorPickerDialog(Context context, int initialColor, final OnColorSelectedListener listener) {
		super(context);
		this.selectedColor = initialColor;
		this.listener = listener;

		colorWheel = new HSVColorWheel( context );
		valueSlider = new HSVValueSlider( context );
		int padding = (int) (context.getResources().getDisplayMetrics().density * PADDING_DP);
		int borderSize = (int) (context.getResources().getDisplayMetrics().density * BORDER_DP);
		RelativeLayout layout = new RelativeLayout( context );

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT );
		lp.bottomMargin = (int) (context.getResources().getDisplayMetrics().density * CONTROL_SPACING_DP);
		colorWheel.setListener( new OnColorSelectedListener() {
			public void colorSelected(Integer color) {
				valueSlider.setColor( color, true );
			}
		} );
		colorWheel.setColor( initialColor );
		colorWheel.setId( 1 );
		layout.addView( colorWheel, lp );

		int selectedColorHeight = (int) (context.getResources().getDisplayMetrics().density * SELECTED_COLOR_HEIGHT_DP);

		FrameLayout valueSliderBorder = new FrameLayout( context );
		valueSliderBorder.setBackgroundColor( BORDER_COLOR );
		valueSliderBorder.setPadding( borderSize, borderSize, borderSize, borderSize );
		valueSliderBorder.setId( 2 );
		lp = new RelativeLayout.LayoutParams( LayoutParams.MATCH_PARENT, selectedColorHeight + 2 * borderSize );
		lp.bottomMargin = (int) (context.getResources().getDisplayMetrics().density * CONTROL_SPACING_DP);
		lp.addRule( RelativeLayout.BELOW, 1 );
		layout.addView( valueSliderBorder, lp );

		valueSlider.setColor( initialColor, false );
		valueSlider.setListener( new OnColorSelectedListener() {
			public void colorSelected(Integer color) {
				selectedColor = color;
				selectedColorView.setBackgroundColor( color );
			}
		});
		valueSliderBorder.addView( valueSlider );

		FrameLayout selectedColorborder = new FrameLayout( context );
		selectedColorborder.setBackgroundColor( BORDER_COLOR );
		lp = new RelativeLayout.LayoutParams( LayoutParams.MATCH_PARENT, selectedColorHeight + 2 * borderSize );
		selectedColorborder.setPadding( borderSize, borderSize, borderSize, borderSize );
		lp.addRule( RelativeLayout.BELOW, 2 );
		layout.addView( selectedColorborder, lp );

		selectedColorView = new View( context );
		selectedColorView.setBackgroundColor( selectedColor );
		selectedColorborder.addView( selectedColorView );

		setButton( BUTTON_NEGATIVE, context.getString( android.R.string.cancel ), clickListener );
		setButton( BUTTON_POSITIVE, context.getString( android.R.string.ok ), clickListener );

		setView( layout, padding, padding, padding, padding );
	}

	private OnClickListener clickListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			switch ( which ) {
			case BUTTON_NEGATIVE:
				dialog.dismiss();
				break;
			case BUTTON_NEUTRAL:
				dialog.dismiss();
				listener.colorSelected( -1 );
				break;
			case BUTTON_POSITIVE:
				listener.colorSelected( selectedColor );
				break;
			}
		}
	};

	private HSVColorWheel colorWheel;
	private HSVValueSlider valueSlider;

	private View selectedColorView;

	/**
	 * Adds a button to the dialog that allows a user to select "No color",
	 * which will call the listener's {@link OnColorSelectedListener#colorSelected(Integer) colorSelected(Integer)} callback
	 * with null as its parameter  
	 * @param res A string resource with the text to be used on this button
	 */
	public void setNoColorButton( int res ) {
		setButton( BUTTON_NEUTRAL, getContext().getString( res ), clickListener ); 
	}

}