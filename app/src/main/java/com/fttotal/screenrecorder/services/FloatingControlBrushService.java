package com.fttotal.screenrecorder.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.hbisoft.hbrecorder.Const;
import com.fttotal.screenrecorder.R;

public class FloatingControlBrushService extends Service implements View.OnClickListener {
    private final String TAG = FloatingControlBrushService.class.getName();

    private IBinder binder = new ServiceBinder();

    public LinearLayout floatingControls;

    public GestureDetector gestureDetector;

    public Handler handler = new Handler();

    public int height;

    public ImageView img;

    public boolean isOverRemoveView;

    public View mRemoveView;

    public int[] overlayViewLocation = {0, 0};

    public WindowManager.LayoutParams params;

    public SharedPreferences prefs;
    public BroadcastReceiver receiverCapture = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                int i = intent.getExtras().getInt("capture");
                if (i == 0) {
                    floatingControls.setVisibility(View.INVISIBLE);
                } else if (i == 1) {
                    floatingControls.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    public int[] removeViewLocation = {0, 0};

    public Runnable runnable = () -> setAlphaAssistiveIcon();

    public Vibrator vibrate;

    public int width;

    public WindowManager windowManager;


    public boolean isPointInArea(int loc1, int loc2, int loc3, int loc4, int width) {
        return loc1 >= loc3 - width && loc1 <= loc3 + width && loc2 >= loc4 - width && loc2 <= loc4 + width;
    }


    public void setAlphaAssistiveIcon() {
        ViewGroup.LayoutParams layoutParams = img.getLayoutParams();
        layoutParams.height = Math.min(width / 10, height / 10);
        layoutParams.width = Math.min(width / 10, height / 10);
        img.setImageResource(R.drawable.ic_brush_service);
        floatingControls.setAlpha(0.5f);
        img.setLayoutParams(layoutParams);
        if (params.x < width - params.x) {
            params.x = 0;
        } else {
            params.x = width;
        }
        try {
            windowManager.updateViewLayout(floatingControls, params);
        } catch (Exception e) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        floatingControls = (LinearLayout) ((LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_floatbutton_control_brush, (ViewGroup) null);
        img = (ImageView) floatingControls.findViewById(R.id.imgIcon);
        mRemoveView = onGetRemoveView();
        setupRemoveView(mRemoveView);
        params = new WindowManager.LayoutParams(-2, -2, Build.VERSION.SDK_INT < 26 ? 2002 : 2038, 8, -3);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = width;
        params.y = height / 4;
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return true;
            }
        });
        floatingControls.setOnTouchListener(new View.OnTouchListener() {
            private boolean flag = false;
            private float initialTouchX;
            private float initialTouchY;
            private int initialX;
            private int initialY;
            private boolean oneRun = false;
            private WindowManager.LayoutParams paramsF;

            {
                paramsF = params;
            }

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                handler.removeCallbacks(runnable);
                if (gestureDetector.onTouchEvent(motionEvent)) {
                    mRemoveView.setVisibility(View.GONE);
                    handler.removeCallbacks(runnable);
                    handler.postDelayed(runnable, 2000);
                    openBrsuh();
                } else {
                    int action = motionEvent.getAction();
                    if (action == MotionEvent.ACTION_DOWN) {
                        ViewGroup.LayoutParams layoutParams = img.getLayoutParams();
                        layoutParams.height = Math.min(width / 8, height / 8);
                        layoutParams.width = Math.min(width / 8, height / 8);
                        img.setLayoutParams(layoutParams);
                        floatingControls.setAlpha(1.0f);
                        initialX = paramsF.x;
                        initialY = paramsF.y;
                        initialTouchX = motionEvent.getRawX();
                        initialTouchY = motionEvent.getRawY();
                        flag = true;
                    } else if (action == MotionEvent.ACTION_UP) {
                        flag = false;
                        if (params.x < width - params.x) {
                            params.x = 0;
                        } else {
                            params.x = width - floatingControls.getWidth();
                        }
                        if (isOverRemoveView) {
                            Log.d(TAG, "onTouch: AAA ");
                            prefs.edit().putBoolean(Const.PREFS_TOOLS_BRUSH, false).apply();
                            stopSelf();
                        } else {
                            windowManager.updateViewLayout(floatingControls, params);
                            handler.postDelayed(runnable, 2000);
                        }
                        mRemoveView.setVisibility(View.GONE);
                    } else if (action == MotionEvent.ACTION_MOVE) {
                        paramsF.x = initialX + ((int) (motionEvent.getRawX() - initialTouchX));
                        paramsF.y = initialY + ((int) (motionEvent.getRawY() - initialTouchY));
                        if (flag) {
                            mRemoveView.setVisibility(View.VISIBLE);
                        }
                        windowManager.updateViewLayout(floatingControls, paramsF);
                        floatingControls.getLocationOnScreen(overlayViewLocation);
                        mRemoveView.getLocationOnScreen(removeViewLocation);
                        isOverRemoveView = isPointInArea(overlayViewLocation[0], overlayViewLocation[1], removeViewLocation[0], removeViewLocation[1], mRemoveView.getWidth());
                        if (isOverRemoveView) {
                            if (oneRun) {
                                if (Build.VERSION.SDK_INT < 26) {
                                    vibrate.vibrate(200);
                                } else {
                                    vibrate.vibrate(VibrationEffect.createOneShot(200, 255));
                                }
                            }
                            oneRun = false;
                        } else {
                            oneRun = true;
                        }
                    } else if (action == MotionEvent.ACTION_CANCEL) {
                        mRemoveView.setVisibility(View.GONE);
                    }
                }
                return false;
            }
        });
        addBubbleView();
        handler.postDelayed(runnable, 2000);
        registerReceiver(receiverCapture, new IntentFilter(Const.ACTION_SCREEN_SHOT));
    }

    private void setupRemoveView(View view) {
        view.setVisibility(View.GONE);
        try {
            windowManager.addView(view, newWindowManagerLayoutParamsForRemoveView());
        } catch (Exception exception) {
        }
    }

    private static WindowManager.LayoutParams newWindowManagerLayoutParamsForRemoveView() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, Build.VERSION.SDK_INT < 26 ? 2002 : 2038, 262664, -3);
        layoutParams.gravity = 81;
        layoutParams.y = 56;
        return layoutParams;
    }


    public View onGetRemoveView() {
        return LayoutInflater.from(this).inflate(R.layout.overlay_remove_view, (ViewGroup) null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ViewGroup.LayoutParams layoutParams = img.getLayoutParams();
        layoutParams.height = Math.min(width / 8, height / 8);
        layoutParams.width = Math.min(width / 8, height / 8);
        img.setLayoutParams(layoutParams);
        floatingControls.setAlpha(1.0f);
        return super.onStartCommand(intent, flags, startId);
    }

    public void addBubbleView() {
        if (windowManager != null && floatingControls != null) {
            try {
                windowManager.addView(floatingControls, params);
            } catch (Exception exception) {
            }
        }
    }

    public void removeBubbleView() {
        if (windowManager != null && floatingControls != null) {
            try {
                windowManager.removeView(floatingControls);
            } catch (Exception exception) {
            }
        }
    }

    @Override
    public void onClick(View view) {
        view.getId();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean vibrate_key = prefs.getBoolean(getString(R.string.preference_vibrate_key), true);
        if (vibrate_key) {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
        }
    }

    public void openBrsuh() {
        startService(new Intent(this, BrushService.class));
    }

    @Override
    public void onDestroy() {
        removeBubbleView();
        unregisterReceiver(receiverCapture);
        if (!(windowManager == null || mRemoveView == null)) {
            try {
                windowManager.removeView(mRemoveView);
            } catch (Exception exception) {
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding successful!");
        return binder;
    }

    public class ServiceBinder extends Binder {
        public ServiceBinder() {
        }


        public FloatingControlBrushService getService() {
            return FloatingControlBrushService.this;
        }
    }
}
