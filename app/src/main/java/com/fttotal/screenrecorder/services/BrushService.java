package com.fttotal.screenrecorder.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.raed.drawingview.BrushView;
import com.raed.drawingview.DrawingView;
import com.raed.drawingview.brushes.BrushSettings;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.adapters.ColorAdapter;

import java.util.ArrayList;

public class BrushService extends Service implements View.OnTouchListener, View.OnClickListener {
    public static LinearLayout lbrus;
    private ColorAdapter colorAdapter;

    public DrawingView drawingView;
    private ConstraintLayout mLayout;
    private NotificationManager mNotificationManager;
    private WindowManager.LayoutParams mParams;
    private String path = "";
    private WindowManager windowManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void initView() {
        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mLayout = (ConstraintLayout) ((LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_main_brush, (ViewGroup) null);
        drawingView = mLayout.findViewById(R.id.drawview);
        RecyclerView recyclerView = mLayout.findViewById(R.id.rcv);
        final ConstraintLayout constraintLayout2 = mLayout.findViewById(R.id.container_color);
        lbrus = mLayout.findViewById(R.id.layout_brush);
        ImageView imgPaint = mLayout.findViewById(R.id.imgPaint);
        ImageView imgEraser = mLayout.findViewById(R.id.imgEraser);
        ImageView imgUndo = mLayout.findViewById(R.id.imgUndo);
        SeekBar size_seek_bar = mLayout.findViewById(R.id.size_seek_bar);
        ((ImageView) mLayout.findViewById(R.id.imgClose)).setOnClickListener(this);
        ((ImageView) mLayout.findViewById(R.id.imv_close)).setOnClickListener(view -> constraintLayout2.setVisibility(View.GONE));
        mParams = new WindowManager.LayoutParams(-1, -1, Build.VERSION.SDK_INT < 26 ? 2002 : 2038, 8, -3);
        final BrushView brushView = mLayout.findViewById(R.id.brush_view);
        brushView.setDrawingView(drawingView);
        final BrushSettings brushSettings = drawingView.getBrushSettings();
        brushSettings.setSelectedBrush(0);
        drawingView.setUndoAndRedoEnable(true);
        imgEraser.setOnClickListener(view -> brushSettings.setSelectedBrush(4));
        imgUndo.setOnClickListener(view -> drawingView.undo());
        imgPaint.setOnClickListener(view -> {
            brushSettings.setSelectedBrush(View.VISIBLE);
            constraintLayout2.setVisibility(View.VISIBLE);
        });

        windowManager.addView(mLayout, mParams);
        size_seek_bar.setMax(100);
        size_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                brushSettings.setSelectedBrushSize(((float) i) / 100.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                brushView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                brushView.setVisibility(View.GONE);
            }
        });
        colorAdapter = new ColorAdapter(this, initColors(), i -> brushSettings.setColor(i));
        recyclerView.setAdapter(colorAdapter);
    }


    private ArrayList<Integer> initColors() {
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(Integer.valueOf(Color.parseColor("#ffffff")));
        arrayList.add(Integer.valueOf(Color.parseColor("#039BE5")));
        arrayList.add(Integer.valueOf(Color.parseColor("#00ACC1")));
        arrayList.add(Integer.valueOf(Color.parseColor("#00897B")));
        arrayList.add(Integer.valueOf(Color.parseColor("#FDD835")));
        arrayList.add(Integer.valueOf(Color.parseColor("#FFB300")));
        arrayList.add(Integer.valueOf(Color.parseColor("#F4511E")));
        return arrayList;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initView();
        return START_NOT_STICKY;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.imgClose) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        if (!(windowManager == null || mLayout == null)) {
            windowManager.removeView(mLayout);
        }
        super.onDestroy();
    }
}
