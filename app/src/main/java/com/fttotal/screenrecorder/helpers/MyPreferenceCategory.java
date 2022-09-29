package com.fttotal.screenrecorder.helpers;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import com.fttotal.screenrecorder.R;

public class MyPreferenceCategory extends PreferenceCategory {

    public MyPreferenceCategory(Context context) {
        super(context);
    }

    public MyPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyPreferenceCategory(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        titleView.setTextColor(getContext().getColor(R.color.colorRed));

        //or to support all versions use
        Typeface typeface = ResourcesCompat.getFont(getContext(), R.font.inter_bold);
        titleView.setTypeface(typeface);
        titleView.setTextSize(16);
    }

}