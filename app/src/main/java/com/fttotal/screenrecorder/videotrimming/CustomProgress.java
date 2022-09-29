package com.fttotal.screenrecorder.videotrimming;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import com.fttotal.screenrecorder.R;

public class CustomProgress extends ProgressBar {
    public CustomProgress(Context context) {
        super(context);
        setTintColor(context);
    }

    public CustomProgress(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setTintColor(context);
    }

    public CustomProgress(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setTintColor(context);
    }

    public CustomProgress(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        setTintColor(context);
    }

    private void setTintColor(Context context) {
        getIndeterminateDrawable().setColorFilter(TrimmerUtils.getColor(context, R.color.colorAccent), PorterDuff.Mode.MULTIPLY);
    }
}
