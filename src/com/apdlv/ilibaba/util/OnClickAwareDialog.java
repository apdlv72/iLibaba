
package com.apdlv.ilibaba.util;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;

public abstract class OnClickAwareDialog extends Dialog implements OnClickListener
{
    public OnClickAwareDialog(Context context)
    {
	super(context);
    }

    public void registerViews(int ... ids)
    {
	for (int id : ids) registerView(findViewById(id));
    }

    public void registerView(View v)
    {
	 if (null!=v) v.setOnClickListener(this);
    }

    public abstract void onClick(int id);

    public void onClick(View v)
    {
	int id = v.getId();
	this.onClick(id);
    }
}
