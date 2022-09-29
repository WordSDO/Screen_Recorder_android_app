package com.fttotal.screenrecorder.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
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

import com.google.android.exoplayer2.util.MimeTypes;
import com.hbisoft.hbrecorder.Const;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.activities.ScreenShotActivity;
import com.fttotal.screenrecorder.helpers.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class FloatingControlCaptureService extends Service implements View.OnClickListener, HBRecorderListener {
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
    public HBRecorder hbRecorder;


    String screenshotOutput;
    private int ssDensity;

    public Handler ssHandler;
    private int ssHeight;

    public ImageReader ssImageReader;

    public MediaProjection ssMediaProjection;

    public VirtualDisplay ssVirtualDisplay;
    private int ssWidth;
    ContentResolver resolver;

    private static int IMAGES_PRODUCED;
    ContentValues contentValues;
    private Intent mScreenCaptureIntent = null;
    private int mScreenCaptureResultCode;
    Uri mUri;


    private static int getVirtualDisplayFlags() {
        return 16;
    }

    public boolean isPointInArea(int i, int i2, int i3, int i4, int i5) {
        return i >= i3 - i5 && i <= i3 + i5 && i2 >= i4 - i5 && i2 <= i4 + i5;
    }


    public void setAlphaAssistiveIcon() {
        ViewGroup.LayoutParams layoutParams = img.getLayoutParams();
        layoutParams.height = Math.min(width / 10, height / 10);
        layoutParams.width = Math.min(width / 10, height / 10);
        img.setImageResource(R.drawable.ic_camera_service);
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
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                ssHandler = new Handler(Looper.getMainLooper());
                Looper.loop();
            }
        }.start();

        vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        floatingControls = (LinearLayout) ((LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_floatbutton_control_capture, (ViewGroup) null);

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
        params.y = height / 2;
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


//                    if (!hbRecorder.isBusyRecording()) {
                    openCapture();
//                    }
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
                            prefs.edit().putBoolean(Const.PREFS_TOOLS_CAPTURE, false).apply();
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
        windowManager.addView(view, newWindowManagerLayoutParamsForRemoveView());
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
        if (intent != null) {
            hbRecorder = new HBRecorder(this, this);
            setOutputPath();
            mScreenCaptureIntent = intent.getParcelableExtra("android.intent.extra.INTENT");
        }

        ViewGroup.LayoutParams layoutParams = img.getLayoutParams();
        layoutParams.height = Math.min(width / 8, height / 8);
        layoutParams.width = Math.min(width / 8, height / 8);
        img.setLayoutParams(layoutParams);
        floatingControls.setAlpha(1.0f);


        return super.onStartCommand(intent, flags, startId);
    }

    public void addBubbleView() {
        if (windowManager != null && floatingControls != null) {
            windowManager.addView(floatingControls, params);
        }
    }

    public void removeBubbleView() {
        if (windowManager != null && floatingControls != null) {
            windowManager.removeView(floatingControls);
        }
    }

    public void onClick(View view) {
    }


    public void openCapture() {
        if (!hbRecorder.isBusyRecording()) {
            Intent intent = new Intent(this, ScreenShotActivity.class);
            intent.putExtra("android.intent.extra.INTENT", mScreenCaptureIntent);
            intent.setFlags(268435456);
            startActivity(intent);
        }
    }

    public void captureScreen() {
        new Handler().postDelayed(() -> {
            initScreenshotRecorder();
            initImageReader();
            createImageVirtualDisplay();
        }, 300);
    }

    @Override
    public void onDestroy() {
        View view;
        removeBubbleView();
        unregisterReceiver(receiverCapture);
        if (!(windowManager == null || (view = mRemoveView) == null)) {
            windowManager.removeView(view);
        }
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        Log.d("TAG", "Binding successful!");
        return binder;
    }

    @Override
    public void HBRecorderOnStart() {

    }

    @Override
    public void HBRecorderOnComplete() {

    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {

    }

    public class ServiceBinder extends Binder {
        public ServiceBinder() {
        }

        public FloatingControlCaptureService getService() {
            return FloatingControlCaptureService.this;
        }
    }


    public void initScreenshotRecorder() {
        int intExtra = mScreenCaptureIntent.getIntExtra(Utils.SCREEN_CAPTURE_INTENT_RESULT_CODE, Utils.RESULT_CODE_FAILED);
        Object systemService = getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Objects.requireNonNull(systemService);
        ssMediaProjection = ((MediaProjectionManager) systemService).getMediaProjection(intExtra, mScreenCaptureIntent);
        ssMediaProjection.registerCallback(new MediaProjectionStopCallback(), ssHandler);
    }

    public void initImageReader() {
        if (ssImageReader == null) {
            ssDensity = Resources.getSystem().getDisplayMetrics().densityDpi;
            ssWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            ssHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
            ssImageReader = ImageReader.newInstance(ssWidth, ssHeight, 1, 1);
            ssImageReader.setOnImageAvailableListener(imageReader -> {
                try {
                    cutOutFrame();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }, ssHandler);
        }
    }


    public void createImageVirtualDisplay() {
        ssVirtualDisplay = ssMediaProjection.createVirtualDisplay("mediaprojection", ssWidth, ssHeight, ssDensity, getVirtualDisplayFlags(), ssImageReader.getSurface(), null, ssHandler);
    }

    public void cutOutFrame() {
        Image image = ssImageReader.acquireLatestImage();
        if (image == null) {
            return;
        }
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * ssWidth;
        Bitmap bitmap = Bitmap.createBitmap(ssWidth +
                rowPadding / pixelStride, ssHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        String path = Environment.DIRECTORY_PICTURES + File.separator + "ScreenRecorder";
        screenshotOutput = Environment.getExternalStoragePublicDirectory(path).getPath();

        String name = "ss_" + Utils.getUnixTimeStamp() + ".png";
        String finalPath = screenshotOutput + File.separator + name;

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(finalPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            IMAGES_PRODUCED = 1 + IMAGES_PRODUCED;
            MediaScannerConnection.scanFile(this, new String[]{finalPath}, null, (s, uri) -> {
                Log.d("ExternalStorage ", s);
                Log.d("uri ", uri.toString());
            });
        } catch (Exception exception) {
            bitmap = null;
        }

        if (outputStream != null) {
            try {
                outputStream.close();
                image.close();
            } catch (IOException iOException) {
                iOException.printStackTrace();
            }
        }
        if (bitmap != null) {
            bitmap.recycle();
        }
        stopProjection();

        if (bitmap != null)
            bitmap.recycle();
    }

    private void stopProjection() {
        if (ssHandler != null) {
            ssHandler.post(() -> {
                if (ssMediaProjection != null) {
                    ssMediaProjection.stop();
                }
            });
        }
    }

    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        private MediaProjectionStopCallback() {
        }

        @Override
        public void onStop() {
            Log.e("screenshot", "stopping projection.");
            ssHandler.post(() -> {
                if (ssVirtualDisplay != null) {
                    ssVirtualDisplay.release();
                }
                if (ssImageReader != null) {
                    ssImageReader.setOnImageAvailableListener(null, null);
                    ssImageReader = null;
                }
                ssMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                Utils.toast(getApplicationContext(), "Screenshot Saved", 0);
            });
        }
    }

    private void setOutputPath() {
        String generateFileName = generateFileName();
        if (Build.VERSION.SDK_INT >= 29) {
            resolver = getContentResolver();
            contentValues = new ContentValues();
            contentValues.put("relative_path", "DCIM/ScreenRecorder");
            contentValues.put("title", generateFileName);
            contentValues.put("_display_name", generateFileName);
            contentValues.put("mime_type", MimeTypes.VIDEO_MP4);
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            hbRecorder.setFileName(generateFileName);
            hbRecorder.setOutputUri(mUri);
            return;
        }
        Utils.createFolder();
        hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + Utils.VIDEO_DIRECTORY_NAME);
    }

    private String generateFileName() {
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date(System.currentTimeMillis())).replace(" ", "");
    }


   /* private void updateGalleryUri() {
        contentValues.clear();
        contentValues.put("is_pending", 0);
        getContentResolver().update(mUri, contentValues, null, null);
    }*/

}
