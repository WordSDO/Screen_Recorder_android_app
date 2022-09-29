package com.fttotal.screenrecorder.videotrimming;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import com.fttotal.screenrecorder.R;

public class CustomProgressView extends Dialog {
    public CustomProgressView(Context context) {
        super(context);
        View inflate = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null);
        requestWindowFeature(1);
        getWindow().setLayout(-1, -1);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        setCancelable(false);
        setContentView(inflate);
    }
}
