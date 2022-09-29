package com.hbisoft.hbrecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;

public class FloatingCameraViewService extends Service implements View.OnClickListener {
    public static final String TAG = FloatingCameraViewService.class.getName();

    public static CameraView cameraView;

    public static FloatingCameraViewService context;
    private static boolean isWindowViewAdded;
    private IBinder binder = new ServiceBinder();

    public Handler handler = new Handler();

    public ImageButton hideCameraBtn;
    private boolean isCameraViewHidden;

    public View mCurrentView;
    private LinearLayout mFloatingView;

    public WindowManager mWindowManager;
    private OverlayResize overlayResize = OverlayResize.MINWINDOW;

    public WindowManager.LayoutParams params;
    private SharedPreferences prefs;

    public ImageButton resizeOverlay;

    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            resizeOverlay.setVisibility(View.GONE);
            hideCameraBtn.setVisibility(View.GONE);
            switchCameraBtn.setVisibility(View.GONE);
        }
    };

    public ImageButton switchCameraBtn;
    private Values values;

    private enum OverlayResize {
        MAXWINDOW,
        MINWINDOW
    }

    public FloatingCameraViewService() {
        context = this;
    }

    public static boolean getIsWindowViewAdded() {
        return isWindowViewAdded;
    }

    public static void setIsWindowViewAdded(boolean z) {
        isWindowViewAdded = z;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbinding and stopping service");
        stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mFloatingView = (LinearLayout) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_floating_camera_view, (ViewGroup) null);

        cameraView = (CameraView) mFloatingView.findViewById(R.id.cameraView);
        Facing facing = Facing.FRONT;
        try {
            facing = intent.getIntExtra("facing", 0) == 0 ? Facing.FRONT : Facing.BACK;
        } catch (Exception exception) {
        }
        cameraView.setFacing(facing);
        hideCameraBtn = (ImageButton) mFloatingView.findViewById(R.id.hide_camera);
        switchCameraBtn = (ImageButton) mFloatingView.findViewById(R.id.switch_camera);
        resizeOverlay = (ImageButton) mFloatingView.findViewById(R.id.overlayResize);
        values = new Values();
        hideCameraBtn.setOnClickListener(this);
        switchCameraBtn.setOnClickListener(this);
        resizeOverlay.setOnClickListener(this);
        mCurrentView = mFloatingView;
        int xPos = getXPos();
        int yPos = getYPos();
        params = new WindowManager.LayoutParams(values.smallCameraX, values.smallCameraY, Build.VERSION.SDK_INT < 26 ? 2002 : 2038, 8, -3);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = xPos;
        params.y = yPos;
        if (hasCameraPermission()) {
            mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            mWindowManager.addView(mCurrentView, params);
            setIsWindowViewAdded(true);
            try {
                cameraView.open();
            } catch (Exception e) {
                Log.d(TAG, "onStartCommand: " + e.getLocalizedMessage());
            }
            setupDragListener();
        }
        return START_STICKY;
    }

    private boolean hasCameraPermission() {
        return ActivityCompat.checkSelfPermission(getBaseContext(), "android.permission.CAMERA") == 0;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        changeCameraOrientation();
    }

    private void changeCameraOrientation() {
        values.buildValues();
        int i = overlayResize == OverlayResize.MAXWINDOW ? values.bigCameraX : values.smallCameraX;
        int i2 = overlayResize == OverlayResize.MAXWINDOW ? values.bigCameraY : values.smallCameraY;
        if (!isCameraViewHidden) {
            params.height = i2;
            params.width = i;
            if (isWindowViewAdded) {
                try {
                    mWindowManager.updateViewLayout(mCurrentView, params);
                } catch (Exception exception) {
                }
            }
        }
    }

    private void setupDragListener() {
        mCurrentView.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX;
            private float initialTouchY;
            private int initialX;
            private int initialY;
            boolean isMoving = false;
            private WindowManager.LayoutParams paramsF;

            {
                this.paramsF = params;
            }

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action != 0) {
                    if (action == 1) {
                        handler.postDelayed(runnable, 3000);
                    } else if (action == 2) {
                        int rawX = (int) (motionEvent.getRawX() - initialTouchX);
                        int rawY = (int) (motionEvent.getRawY() - initialTouchY);
                        paramsF.x = initialX + rawX;
                        paramsF.y = initialY + rawY;
                        if (Math.abs(rawX) > 10 || Math.abs(rawY) > 10) {
                            isMoving = true;
                        }
                        try {
                            mWindowManager.updateViewLayout(mCurrentView, paramsF);
                        } catch (Exception exception) {
                        }
                        persistCoordinates(initialX + rawX, initialY + rawY);
                        return true;
                    }
                    return false;
                }
                if (resizeOverlay.isShown()) {
                    resizeOverlay.setVisibility(View.GONE);
                    hideCameraBtn.setVisibility(View.GONE);
                    switchCameraBtn.setVisibility(View.GONE);
                } else {
                    resizeOverlay.setVisibility(View.VISIBLE);
                    hideCameraBtn.setVisibility(View.VISIBLE);
                    switchCameraBtn.setVisibility(View.VISIBLE);
                    handler.removeCallbacks(runnable);
                }
                isMoving = false;
                initialX = paramsF.x;
                initialY = paramsF.y;
                initialTouchX = motionEvent.getRawX();
                initialTouchY = motionEvent.getRawY();
                return true;
            }
        });
        resizeOverlay.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX;
            private float initialTouchY;
            private int initialX;
            private int initialY;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == 0) {
                    initialX = params.width;
                    initialY = params.height;
                    initialTouchX = motionEvent.getRawX();
                    initialTouchY = motionEvent.getRawY();
                    return true;
                } else if (action == 1) {
                    handler.postDelayed(runnable, 3000);
                    return false;
                } else if (action != 2) {
                    return false;
                } else {
                    if (resizeOverlay.isShown()) {
                        handler.removeCallbacks(runnable);
                    }
                    params.width = initialX + ((int) (motionEvent.getRawX() - initialTouchX));
                    params.height = initialY + ((int) (motionEvent.getRawY() - initialTouchY));
                    try {
                        mWindowManager.updateViewLayout(mCurrentView, params);
                    } catch (Exception exception) {
                    }
                    return true;
                }
            }
        });
    }

    private int getXPos() {
        return Integer.parseInt(getDefaultPrefs().getString(Const.PREFS_CAMERA_OVERLAY_POS, "0X100").split("X")[0]);
    }

    private int getYPos() {
        return Integer.parseInt(getDefaultPrefs().getString(Const.PREFS_CAMERA_OVERLAY_POS, "0X100").split("X")[1]);
    }


    public void persistCoordinates(int i, int i2) {
        SharedPreferences.Editor edit = getDefaultPrefs().edit();
        edit.putString(Const.PREFS_CAMERA_OVERLAY_POS, i + "X" + i2).apply();
    }

    private SharedPreferences getDefaultPrefs() {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        }
        return prefs;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (prefs != null) {
            prefs.edit().putBoolean(Const.PREFS_TOOLS_CAMERA, false).apply();
        }
        if (mFloatingView != null) {
            handler.removeCallbacks(runnable);
            if (getIsWindowViewAdded()) {
                mWindowManager.removeView(mCurrentView);
                setIsWindowViewAdded(false);
            }
            cameraView.close();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.hide_camera) {
            Log.d(TAG, "hide camera");
            if (mCurrentView.equals(mFloatingView)) {
                if (getIsWindowViewAdded()) {
                    mWindowManager.removeViewImmediate(mCurrentView);
                    setIsWindowViewAdded(false);
                }
                cameraView.close();
                mFloatingView = null;
            }
            stopService(new Intent(this, FloatingControlCameraService.class));
            prefs.edit().putBoolean(Const.PREFS_TOOLS_CAMERA, false).apply();
            setupDragListener();
        } else if (id == R.id.switch_camera) {
            cameraView.setFacing(cameraView.getFacing() == Facing.BACK ? Facing.FRONT : Facing.BACK);
        }
    }

    private void showCameraView() {
        mWindowManager.removeViewImmediate(mCurrentView);
        LinearLayout linearLayout = mFloatingView;
        mCurrentView = linearLayout;
        mWindowManager.addView(linearLayout, params);
        isCameraViewHidden = false;
        setupDragListener();
    }

    private class Values {
        int bigCameraX;
        int bigCameraY;
        int cameraHideX = dpToPx(60);
        int cameraHideY = dpToPx(60);
        int smallCameraX;
        int smallCameraY;

        public Values() {
            buildValues();
        }

        private int dpToPx(int i) {
            return Math.round(((float) i) * (getResources().getDisplayMetrics().xdpi / 160.0f));
        }


        public void buildValues() {
            if (FloatingCameraViewService.context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                smallCameraX = dpToPx(160);
                smallCameraY = dpToPx(120);
                bigCameraX = dpToPx(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION);
                bigCameraY = dpToPx(150);
                return;
            }
            smallCameraX = dpToPx(120);
            smallCameraY = dpToPx(160);
            bigCameraX = dpToPx(150);
            bigCameraY = dpToPx(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION);
        }
    }

    public class ServiceBinder extends Binder {
        public ServiceBinder() {
        }


        public FloatingCameraViewService getService() {
            return FloatingCameraViewService.this;
        }
    }
}

