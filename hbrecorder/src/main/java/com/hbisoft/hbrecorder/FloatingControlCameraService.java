package com.hbisoft.hbrecorder;


import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.otaliastudios.cameraview.controls.Facing;

public class FloatingControlCameraService extends Service {
    public static final String TAG = FloatingControlCameraService.class.getName();
    private final IBinder binder = new ServiceBinder();
    private final ServiceConnection floatingCameraConnection = new ServiceConnection() {
        public void onServiceDisconnected(ComponentName componentName) {
        }

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ((FloatingCameraViewService.ServiceBinder) iBinder).getService();
        }
    };
    private boolean isBound = false;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent2 = new Intent(this, FloatingCameraViewService.class);
        Facing facing = Facing.FRONT;
        try {
            facing = intent.getIntExtra("facing", 0) == 0 ? Facing.FRONT : Facing.BACK;
        } catch (Exception exception) {
        }
        intent2.putExtra("facing", facing);
        startService(intent2);
        isBound = bindService(intent2, floatingCameraConnection, BIND_AUTO_CREATE);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (isBound) {
            unbindService(floatingCameraConnection);
        }
        if (prefs != null) {
            Log.d(TAG, "onDestroy: BBB");
            prefs.edit().putBoolean(Const.PREFS_TOOLS_CAMERA, false).apply();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class ServiceBinder extends Binder {
        public ServiceBinder() {
        }


        public FloatingControlCameraService getService() {
            return FloatingControlCameraService.this;
        }
    }
}

