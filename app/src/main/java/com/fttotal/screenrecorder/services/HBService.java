package com.fttotal.screenrecorder.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.exoplayer2.util.MimeTypes;
import com.hbisoft.hbrecorder.Const;
import com.hbisoft.hbrecorder.FloatingControlCameraService;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;
import com.fttotal.screenrecorder.MainActivity;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.helpers.NotificationHelper;
import com.fttotal.screenrecorder.helpers.Utils;
import com.fttotal.screenrecorder.managers.SharedPreferencesManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class HBService extends Service implements HBRecorderListener {
    private static int IMAGES_PRODUCED;
    ContentValues contentValues;

    public HBRecorder hbRecorder;
    private Boolean isMenuVisible = false;
    boolean isScreenshotTaken = false;
    private int mCameraHeight = 120;
    private int mCameraWidth = 160;

    public View mCountdownLayout;
    private ImageView mImgClose;
    private ImageView mImgPause;
    private ImageView mImgRec;
    private ImageView mImgResume;
    private ImageView mImgScreenshot;
    private ImageView mImgSetting;
    private ImageView mImgStart;
    private ImageView mImgStop;
    private ImageView mImgToolbox;
    private int mMode;

    public Boolean mRecordingPaused = false;

    public Boolean mRecordingStarted = false;
    private Intent mScreenCaptureIntent = null;
    private int mScreenCaptureResultCode;
    private int mScreenHeight;

    public int mScreenWidth;

    public TextView mTvCountdown;
    Uri mUri;

    public View mViewRoot;

    public View mWarermarkLayout;

    public WindowManager mWindowManager;
    WindowManager.LayoutParams paramCountdown;
    WindowManager.LayoutParams paramViewRoot;
    WindowManager.LayoutParams paramWatermark;
    SharedPreferences prefs;
    ContentResolver resolver;
    String screenshotOutput;
    private int ssDensity;

    public Handler ssHandler;
    private int ssHeight;

    public ImageReader ssImageReader;

    public MediaProjection ssMediaProjection;

    public VirtualDisplay ssVirtualDisplay;
    private int ssWidth;


    private static int getVirtualDisplayFlags() {
        return 16;
    }

    public void HBRecorderOnStart() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            hbRecorder = new HBRecorder(this, this);
            setOutputPath();
            mScreenCaptureIntent = intent.getParcelableExtra("android.intent.extra.INTENT");
            if (hbRecorder.isBusyRecording()) {
                Utils.toast(getApplicationContext(), "Recording in progress", 1);
            } else {
                showCountDown();
            }


        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.createScreenshotFolder();
        updateScreenSize();
        if (paramViewRoot == null) {
            initParam();
            initServiceNotification();
        }
        if (mViewRoot == null) {
            initializeViews();
        }
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                ssHandler = new Handler(Looper.getMainLooper());
                Looper.loop();
            }
        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mViewRoot != null) {
            mWindowManager.removeViewImmediate(mViewRoot);
        }
    }


    public void initScreenshotRecorder() {
        int intExtra = mScreenCaptureIntent.getIntExtra(Utils.SCREEN_CAPTURE_INTENT_RESULT_CODE, Utils.RESULT_CODE_FAILED);
        Object systemService = getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Objects.requireNonNull(systemService);
        ssMediaProjection = ((MediaProjectionManager) systemService).getMediaProjection(intExtra, mScreenCaptureIntent);
        ssMediaProjection.registerCallback(new MediaProjectionStopCallback(), this.ssHandler);
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
            }, this.ssHandler);
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

    private void initServiceNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel notificationChannel = new NotificationChannel(NotificationHelper.CHANNEL_ID_SERVICE, NotificationHelper.CHANNEL_NAME_SERVICE, NotificationManager.IMPORTANCE_NONE);
            notificationChannel.setLightColor(-16776961);
            notificationChannel.setLockscreenVisibility(0);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
                startForeground(101, new Notification.Builder(this, NotificationHelper.CHANNEL_ID_SERVICE)
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle(getString(R.string.running))
                        .setContentText(getString(R.string.recordingNotificationMsg))
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .build());
                return;
            }
            return;
        }
        startForeground(101, new Notification());
    }

    private void initParam() {
        int i = Build.VERSION.SDK_INT >= 26 ? 2038 : 2002;
        paramViewRoot = new WindowManager.LayoutParams(-2, -2, i, 8, -3);
        paramCountdown = new WindowManager.LayoutParams(-2, -2, i, 8, -3);
        paramWatermark = new WindowManager.LayoutParams(-2, -2, i, 8, -3);
    }


    private void initializeViews() {

        mViewRoot = LayoutInflater.from(this).inflate(R.layout.layout_recording, null);
        View countDownView = LayoutInflater.from(this).inflate(R.layout.layout_countdown, null);
        View watermarkView = LayoutInflater.from(this).inflate(R.layout.layout_watermark, null);
        paramViewRoot.gravity = 8388627;
        paramViewRoot.x = 0;
        paramViewRoot.y = 0;
        paramWatermark.gravity = Gravity.BOTTOM | Gravity.END;
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(countDownView, paramCountdown);
        mWindowManager.addView(mViewRoot, paramViewRoot);
        mWindowManager.addView(watermarkView, paramWatermark);
        mCountdownLayout = countDownView.findViewById(R.id.countdown_container);
        mWarermarkLayout = watermarkView.findViewById(R.id.watermark_container);
        mTvCountdown = countDownView.findViewById(R.id.tvCountDown);
        toggleView(mCountdownLayout, View.GONE);
        toggleView(mWarermarkLayout, View.GONE);
        mImgRec = mViewRoot.findViewById(R.id.imgRec);
        mImgToolbox = mViewRoot.findViewById(R.id.imgToolbox);
        mImgClose = mViewRoot.findViewById(R.id.imgClose);
        mImgScreenshot = mViewRoot.findViewById(R.id.imgScreenshot);
        mImgPause = mViewRoot.findViewById(R.id.imgPause);
        mImgStart = mViewRoot.findViewById(R.id.imgStart);
        mImgSetting = mViewRoot.findViewById(R.id.imgSetting);
        mImgStop = mViewRoot.findViewById(R.id.imgStop);
        mImgResume = mViewRoot.findViewById(R.id.imgResume);

        toggleView(mImgResume, View.GONE);
        toggleView(mImgStop, View.GONE);
        toggleNavigationButton(View.GONE);
        mImgPause.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT > 24) {
                mRecordingPaused = true;
                toggleNavigationButton(View.GONE);
                hbRecorder.pauseScreenRecording();
                Utils.toast(getApplicationContext(), "Paused", 1);
                return;
            }
            Utils.toast(getApplicationContext(), "This feature is not available in your device", 1);
        });
        mImgResume.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT > 24) {
                mRecordingPaused = false;
                toggleNavigationButton(View.GONE);
                hbRecorder.resumeScreenRecording();
                Utils.toast(getApplicationContext(), "Resumed", 1);
                return;
            }
            Utils.toast(getApplicationContext(), "This feature is not available in your device", 1);
        });
        mImgStart.setOnClickListener(view -> {
            showCountDown();
            toggleNavigationButton(View.GONE);
        });
        mImgStop.setOnClickListener(view -> {
            toggleView(mWarermarkLayout, View.GONE);
            mRecordingStarted = false;
            toggleNavigationButton(View.GONE);
            new Handler().postDelayed(() -> {
                hbRecorder.stopScreenRecording();
                refreshRecordings();
            }, 100);
        });
        mImgScreenshot.setOnClickListener(view -> {
            if (!hbRecorder.isBusyRecording()) {
                toggleNavigationButton(View.GONE);
                Intent intent = new Intent(this, FloatingControlCaptureService.class);
                intent.putExtra("android.intent.extra.INTENT", mScreenCaptureIntent);
                startService(intent);
          /* new Handler().postDelayed(() -> {
                    initScreenshotRecorder();
                    initImageReader();
                    createImageVirtualDisplay();
                }, 300);*/
            }
        });
        mImgClose.setOnClickListener(view -> {

            if (Utils.isServiceRunning(FloatingControlCaptureService.class.getName(), getApplicationContext())) {
                Intent stopIntent = new Intent(HBService.this, FloatingControlCaptureService.class);
                stopService(stopIntent);
            }
            if (Utils.isServiceRunning(FloatingControlCameraService.class.getName(), getApplicationContext())) {
                Intent stopIntent = new Intent(HBService.this, FloatingControlCameraService.class);
                stopService(stopIntent);
            }
            if (Utils.isServiceRunning(FloatingControlBrushService.class.getName(), getApplicationContext())) {
                Intent stopIntent = new Intent(HBService.this, FloatingControlBrushService.class);
                stopService(stopIntent);
            }
           /* if (Utils.isServiceRunning(FloatingSSCapService.class.getName(), getApplicationContext())) {
                Intent stopIntent = new Intent(HBService.this, FloatingSSCapService.class);
                stopService(stopIntent);
            }*/

            if (hbRecorder.isBusyRecording()) {
                if (SharedPreferencesManager.getInstance().getBoolean(Utils.IS_REWARD_VIDEO, false)) {
                    SharedPreferencesManager.getInstance().setBoolean(Utils.IS_REWARD_VIDEO, false);
                }
                toggleView(mWarermarkLayout, View.GONE);
                mRecordingStarted = false;
                toggleNavigationButton(View.GONE);
                new Handler().postDelayed(() -> {
                    hbRecorder.stopScreenRecording();
                    refreshRecordings();
                    stopService();
                }, 100);
                return;
            }
            stopService();
        });
        mImgToolbox.setOnClickListener(view -> openTools());

        mViewRoot.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX;
            private float initialTouchY;
            private int initialX;
            private int initialY;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    initialX = paramViewRoot.x;
                    initialY = paramViewRoot.y;
                    initialTouchX = motionEvent.getRawX();
                    initialTouchY = motionEvent.getRawY();
                    return true;
                } else if (action == MotionEvent.ACTION_UP) {
                    if (motionEvent.getRawX() < ((float) (mScreenWidth / 2))) {
                        paramViewRoot.x = 0;
                    } else {
                        paramViewRoot.x = mScreenWidth;
                    }
                    paramViewRoot.y = initialY + ((int) (motionEvent.getRawY() - initialTouchY));
                    mWindowManager.updateViewLayout(mViewRoot, paramViewRoot);
                    int rawX = (int) (motionEvent.getRawX() - initialTouchX);
                    int rawY = (int) (motionEvent.getRawY() - initialTouchY);
                    if (rawX < 20 && rawY < 20) {
                        if (isViewCollapsed()) {
                            toggleNavigationButton(View.VISIBLE);
                        } else {
                            toggleNavigationButton(View.GONE);
                        }
                    }
                    return true;
                } else if (action != MotionEvent.ACTION_MOVE) {
                    return false;
                } else {
                    paramViewRoot.x = initialX + ((int) (motionEvent.getRawX() - initialTouchX));
                    paramViewRoot.y = initialY + ((int) (motionEvent.getRawY() - initialTouchY));
                    mWindowManager.updateViewLayout(mViewRoot, paramViewRoot);
                    return true;
                }
            }
        });

    }

    private void openCameraControlService() {
        Intent intent = new Intent(this, FloatingControlCameraService.class);
        if (hasCameraPermission()) {
            startService(intent);
        }
    }

    private boolean hasCameraPermission() {
        return ActivityCompat.checkSelfPermission(getBaseContext(), "android.permission.CAMERA") == 0;
    }

    private void openCaptureControlService() {
        Intent intent = new Intent(this, FloatingControlCaptureService.class);
        intent.putExtra("android.intent.extra.INTENT", mScreenCaptureIntent);
        startService(intent);
    }

    private void openBrushControlService() {
        startService(new Intent(this, FloatingControlBrushService.class));
    }


    public void openTools() {
        toggleNavigationButton(View.GONE);
        Intent intent = new Intent(this, ToolsService.class);
        intent.putExtra("android.intent.extra.INTENT", mScreenCaptureIntent);
        startService(intent);
    }

    public void toggleView(View view, int visibility) {
        view.setVisibility(visibility);
    }


    public void showCountDown() {
        toggleView(mCountdownLayout, View.VISIBLE);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        new CountDownTimer((Integer.parseInt(prefs.getString("key_common_countdown", ExifInterface.GPS_MEASUREMENT_3D)) + 1) * 1000L, 1000) {
            @Override
            public void onTick(long l) {
                toggleView(mViewRoot, View.GONE);
                mTvCountdown.setText("" + (l / 1000));
            }

            @Override
            public void onFinish() {
                toggleView(mCountdownLayout, View.GONE);
                toggleView(mViewRoot, View.VISIBLE);
                mRecordingStarted = true;
                toggleNavigationButton(View.GONE);
                if (SharedPreferencesManager.getInstance().getBoolean(Utils.IS_REWARD_VIDEO, false)) {
                    toggleView(mWarermarkLayout, View.GONE);
                } else {
                    toggleView(mWarermarkLayout, View.VISIBLE);
                }

                prefs = PreferenceManager.getDefaultSharedPreferences(HBService.this);
                if (prefs.getBoolean(Const.PREFS_TOOLS_BRUSH, false)) {
                    openBrushControlService();
                }
                if (prefs.getBoolean(Const.PREFS_TOOLS_CAPTURE, false)) {
                    openCaptureControlService();
                }
                if (prefs.getBoolean(Const.PREFS_TOOLS_CAMERA, false)) {
                    openCameraControlService();
                }

                initRecording();
            }
        }.start();
    }

    private void updateScreenSize() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        updateScreenSize();
        if (paramViewRoot != null) {
            paramViewRoot.gravity = 8388627;
            paramViewRoot.x = 0;
            paramViewRoot.y = 0;
        }
    }


    public void toggleNavigationButton(int visibility) {
        mImgStart.setVisibility(visibility);
        mImgPause.setVisibility(visibility);
//        mImgScreenshot.setVisibility(visibility);
        mImgClose.setVisibility(visibility);
        mImgToolbox.setVisibility(visibility);
        mImgStop.setVisibility(visibility);
        mImgResume.setVisibility(visibility);
        if (visibility == View.GONE) {
            mViewRoot.setPadding(25, 25, 25, 25);
            isMenuVisible = false;
            mImgRec.setImageResource(R.drawable.ic_recording_off_lite);
            return;
        }
        isMenuVisible = true;
        mImgRec.setImageResource(R.drawable.ic_close_bubble);
        if (mRecordingStarted) {
            mImgStart.setVisibility(View.GONE);
            mImgStop.setVisibility(View.VISIBLE);
            mImgPause.setVisibility(View.VISIBLE);
        } else {
            mImgStop.setVisibility(View.GONE);
            mImgPause.setVisibility(View.GONE);
            mImgStart.setVisibility(View.VISIBLE);
        }
        if (mRecordingPaused) {
            mImgPause.setVisibility(View.GONE);
            mImgResume.setVisibility(View.VISIBLE);
        } else {
            mImgResume.setVisibility(View.GONE);
//            mImgPause.setVisibility(View.VISIBLE);
        }
        mViewRoot.setPadding(20, 20, 20, 20);
    }


    public boolean isViewCollapsed() {
        return mViewRoot == null || mViewRoot.findViewById(R.id.imgToolbox).getVisibility() == View.GONE;
    }


    public void stopService() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                stopForeground(true);
            }
            stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
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

    private void updateGalleryUri() {
        contentValues.clear();
        contentValues.put("is_pending", 0);
        getContentResolver().update(mUri, contentValues, null, null);
    }

    private void refreshGalleryFile() {
        MediaScannerConnection.scanFile(this, new String[]{hbRecorder.getFilePath()}, null, (str, uri) -> {
            Log.i("ExternalStorage", "Scanned " + str + ":");
            String sb = "-> uri=" +
                    uri;
            Log.i("ExternalStorage", sb);
        });
    }

    private String generateFileName() {
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date(System.currentTimeMillis())).replace(" ", "");
    }


    public void initRecording() {
        mScreenCaptureResultCode = mScreenCaptureIntent.getIntExtra(Utils.SCREEN_CAPTURE_INTENT_RESULT_CODE, Utils.RESULT_CODE_FAILED);
        hbRecorder.enableCustomSettings();
        customSettings();
        hbRecorder.startScreenRecording(mScreenCaptureIntent, mScreenCaptureResultCode);
    }

    public void HBRecorderOnComplete() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (SharedPreferencesManager.getInstance().getBoolean(Utils.IS_REWARD_VIDEO, false)) {
                SharedPreferencesManager.getInstance().setBoolean(Utils.IS_REWARD_VIDEO, false);
            }
            Utils.toast(getApplicationContext(), "Recording Saved", 1);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(268468224);
            intent.setAction(Utils.ACTION_VIDEO_RECORDED);
            startActivity(intent);
        });

        refreshRecordings();
    }

    @Override
    public void HBRecorderOnError(final int errorCode, final String reason) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (errorCode == 38) {
                Utils.toast(getApplicationContext(), getString(R.string.settingsNotSupported), Toast.LENGTH_LONG);
                return;
            }
            Utils.toast(getApplicationContext(), getString(R.string.unableToRecord), Toast.LENGTH_LONG);
            Log.e("HBRecorderOnError", reason);
        });
    }


    public void refreshRecordings() {
        if (hbRecorder.wasUriSet()) {
            updateGalleryUri();
        } else {
            refreshGalleryFile();
        }
    }

    private void customSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //Is audio enabled
        boolean audio_enabled = prefs.getBoolean("key_record_audio", true);
        hbRecorder.isAudioEnabled(audio_enabled);

        //Audio Source
        String audio_source = prefs.getString("key_audio_source", null);
        if (audio_source != null) {
            switch (audio_source) {
                case "0":
                    hbRecorder.setAudioSource("DEFAULT");
                    break;
                case "1":
                    hbRecorder.setAudioSource("CAMCODER");
                    break;
                case "2":
                    hbRecorder.setAudioSource("MIC");
                    break;
            }
        }

        //Video Encoder
        String video_encoder = prefs.getString("key_video_encoder", null);
        if (video_encoder != null) {
            switch (video_encoder) {
                case "0":
                    hbRecorder.setVideoEncoder("DEFAULT");
                    break;
                case "1":
                    hbRecorder.setVideoEncoder("H264");
                    break;
                case "2":
                    hbRecorder.setVideoEncoder("H263");
                    break;
                case "3":
                    hbRecorder.setVideoEncoder("HEVC");
                    break;
                case "4":
                    hbRecorder.setVideoEncoder("MPEG_4_SP");
                    break;
                case "5":
                    hbRecorder.setVideoEncoder("VP8");
                    break;
            }
        }

        //NOTE - THIS MIGHT NOT BE SUPPORTED SIZES FOR YOUR DEVICE
        //Video Dimensions
        String video_resolution = prefs.getString("key_video_resolution", null);
        if (video_resolution != null) {
            switch (video_resolution) {
                case "0":
                    hbRecorder.setScreenDimensions(426, 240);
                    break;
                case "1":
                    hbRecorder.setScreenDimensions(640, 360);
                    break;
                case "2":
                    hbRecorder.setScreenDimensions(854, 480);
                    break;
                case "3":
                    hbRecorder.setScreenDimensions(1280, 720);
                    break;
                case "4":
                    hbRecorder.setScreenDimensions(1920, 1080);
                    break;
            }
        }

        //Video Frame Rate
        String video_frame_rate = prefs.getString("key_video_fps", null);
        if (video_frame_rate != null) {
            switch (video_frame_rate) {
                case "0":
                    hbRecorder.setVideoFrameRate(60);
                    break;
                case "1":
                    hbRecorder.setVideoFrameRate(50);
                    break;
                case "2":
                    hbRecorder.setVideoFrameRate(48);
                    break;
                case "3":
                    hbRecorder.setVideoFrameRate(30);
                    break;
                case "4":
                    hbRecorder.setVideoFrameRate(25);
                    break;
                case "5":
                    hbRecorder.setVideoFrameRate(24);
                    break;
            }
        }

        //Video Bitrate
        String video_bit_rate = prefs.getString("key_video_bitrate", null);
        if (video_bit_rate != null) {
            switch (video_bit_rate) {
                case "1":
                    hbRecorder.setVideoBitrate(12000000);
                    break;
                case "2":
                    hbRecorder.setVideoBitrate(8000000);
                    break;
                case "3":
                    hbRecorder.setVideoBitrate(7500000);
                    break;
                case "4":
                    hbRecorder.setVideoBitrate(5000000);
                    break;
                case "5":
                    hbRecorder.setVideoBitrate(4000000);
                    break;
                case "6":
                    hbRecorder.setVideoBitrate(2500000);
                    break;
                case "7":
                    hbRecorder.setVideoBitrate(1500000);
                    break;
                case "8":
                    hbRecorder.setVideoBitrate(1000000);
                    break;
            }
        }

        //Output Format
        String output_format = prefs.getString("key_output_format", null);
        if (output_format != null) {
            switch (output_format) {
                case "0":
                    hbRecorder.setOutputFormat("DEFAULT");
                    break;
                case "1":
                    hbRecorder.setOutputFormat("MPEG_4");
                    break;
                case "2":
                    hbRecorder.setOutputFormat("THREE_GPP");
                    break;
                case "3":
                    hbRecorder.setOutputFormat("WEBM");
                    break;
            }
        }

    }


}
