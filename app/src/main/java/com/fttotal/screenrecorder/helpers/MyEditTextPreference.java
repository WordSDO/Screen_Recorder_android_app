package com.fttotal.screenrecorder.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;

import com.fttotal.screenrecorder.R;

public class MyEditTextPreference extends EditTextPreference {
    public MyEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MyEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyEditTextPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        int color = getContext().getColor(R.color.white);
        titleView.setTextColor(color);
    }
}
