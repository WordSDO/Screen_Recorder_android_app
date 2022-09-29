package com.fttotal.screenrecorder.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.hbisoft.hbrecorder.Const;
import com.hbisoft.hbrecorder.FloatingControlCameraService;
import com.otaliastudios.cameraview.controls.Facing;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.activities.CheckPermissionActivity;

public class ToolsService extends Service implements View.OnTouchListener, View.OnClickListener {
    private ConstraintLayout mLayout;
    private NotificationManager mNotificationManager;
    private WindowManager.LayoutParams mParams;

    public SharedPreferences prefs;
    private WindowManager windowManager;
    private Intent mScreenCaptureIntent = null;

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

        this.windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        this.mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mLayout = (ConstraintLayout) ((LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_tools, (ViewGroup) null);

        Switch sw_capture = (Switch) this.mLayout.findViewById(R.id.sw_capture);
        Switch sw_camera = mLayout.findViewById(R.id.sw_camera);
        Switch sw_brush = mLayout.findViewById(R.id.sw_brush);

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ((ImageView) mLayout.findViewById(R.id.imv_close)).setOnClickListener(view -> stopSelf());

        sw_capture.setChecked(this.prefs.getBoolean(Const.PREFS_TOOLS_CAPTURE, false));
        sw_camera.setChecked(this.prefs.getBoolean(Const.PREFS_TOOLS_CAMERA, false));
        sw_brush.setChecked(this.prefs.getBoolean(Const.PREFS_TOOLS_BRUSH, false));

        sw_capture.setOnCheckedChangeListener((compoundButton, b) -> {
            ToolsService.this.prefs.edit().putBoolean(Const.PREFS_TOOLS_CAPTURE, b).apply();
            Intent intent =new Intent(this, FloatingControlCaptureService.class);
            intent.putExtra("android.intent.extra.INTENT", mScreenCaptureIntent);
            if (!b) {
                stopService(intent);
            } else {
                startService(intent);
            }
        });

        sw_camera.setOnCheckedChangeListener((compoundButton, b) -> {
            if (!Const.isMyServiceRunning(FloatingControlCameraService.class, this)) {
                Intent intent2 = new Intent(this, CheckPermissionActivity.class);
                intent2.addFlags(268435456);
                intent2.putExtra("boolean", true);
                intent2.putExtra("facing", Facing.FRONT);
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Const.PREFS_TOOLS_CAMERA, true).apply();
                startActivity(intent2);
            }

            prefs.edit().putBoolean(Const.PREFS_TOOLS_CAMERA, b).apply();
            Intent intent = new Intent(ToolsService.this, CheckPermissionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("boolean", b);
            startActivity(intent);
            stopSelf();
        });
        sw_brush.setOnCheckedChangeListener((compoundButton, b) -> {
            prefs.edit().putBoolean(Const.PREFS_TOOLS_BRUSH, b).apply();
            Intent intent = new Intent(ToolsService.this, FloatingControlBrushService.class);
            if (!b) {
                stopService(intent);
            } else {
                startService(intent);
            }
        });
        mParams = new WindowManager.LayoutParams(-1, -1, Build.VERSION.SDK_INT < 26 ? 2002 : 2038, 8, -3);
        windowManager.addView(mLayout, mParams);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mScreenCaptureIntent = intent.getParcelableExtra("android.intent.extra.INTENT");
        }
        initView();
        return START_NOT_STICKY;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.imv_close) {
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
