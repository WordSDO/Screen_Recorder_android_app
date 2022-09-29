package com.hbisoft.hbrecorder;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class HBRecorder implements MyListener {
    private int audioBitrate = 0;
    private int audioSamplingRate = 0;
    private String audioSource = "MIC";
    private byte[] byteArray;

    public final Context context;
    private boolean enableCustomSettings = false;
    private String fileName;

    public final HBRecorderListener hbRecorderListener;
    private boolean isAudioEnabled = true;
    boolean isPaused = false;
    private boolean isVideoHDEnabled = true;
    private int mScreenDensity;
    private int mScreenHeight;
    private int mScreenWidth;
    Uri mUri;
    boolean mWasUriSet = false;
    private String notificationButtonText;
    private String notificationDescription;
    private String notificationTitle;

    public FileObserver observer;
    private int orientation;
    private String outputFormat = "DEFAULT";
    private String outputPath;
    private int resultCode;
    Intent service;
    private int videoBitrate = 40000000;
    private String videoEncoder = "DEFAULT";
    private int videoFrameRate = 30;
    boolean wasOnErrorCalled = false;

    public void initScreenshotCapture() {
    }

    public HBRecorder(Context context, HBRecorderListener hBRecorderListener) {
        this.context = context.getApplicationContext();
        this.hbRecorderListener = hBRecorderListener;
        setScreenDensity();
    }

    public void setOrientationHint(int orientationHint) {
        this.orientation = orientationHint;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public void setOutputUri(Uri uri) {
        this.mWasUriSet = true;
        this.mUri = uri;
    }

    public boolean wasUriSet() {
        return this.mWasUriSet;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public void setAudioSamplingRate(int audioSamplingRate) {
        this.audioSamplingRate = audioSamplingRate;
    }

    public void isAudioEnabled(boolean isAudioEnable) {
        this.isAudioEnabled = isAudioEnable;
    }

    public void setAudioSource(String audioSource) {
        this.audioSource = audioSource;
    }

    public void recordHDVideo(boolean isHdEnabled) {
        this.isVideoHDEnabled = isHdEnabled;
    }

    public void setVideoEncoder(String videoEncoder) {
        this.videoEncoder = videoEncoder;
    }

    public void enableCustomSettings() {
        this.enableCustomSettings = true;
    }

    public void setVideoFrameRate(int videoFrameRate) {
        this.videoFrameRate = videoFrameRate;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    private void setScreenDensity() {
        this.mScreenDensity = Resources.getSystem().getDisplayMetrics().densityDpi;
    }

    public int getDefaultWidth() {
        HBRecorderCodecInfo hBRecorderCodecInfo = new HBRecorderCodecInfo();
        hBRecorderCodecInfo.setContext(this.context);
        return hBRecorderCodecInfo.getMaxSupportedWidth();
    }

    public int getDefaultHeight() {
        HBRecorderCodecInfo hBRecorderCodecInfo = new HBRecorderCodecInfo();
        hBRecorderCodecInfo.setContext(this.context);
        return hBRecorderCodecInfo.getMaxSupportedHeight();
    }

    public void setScreenDimensions(int height, int width) {
        this.mScreenHeight = height;
        this.mScreenWidth = width;
    }

    public String getFilePath() {
        return ScreenRecordService.getFilePath();
    }

    public String getFileName() {
        return ScreenRecordService.getFileName();
    }

    public void startScreenRecording(Intent intent, int code) {
        this.resultCode = code;
        startService(intent);
    }

    public void stopScreenRecording() {
        context.stopService(new Intent(context, ScreenRecordService.class));
    }

    public void pauseScreenRecording() {
        if (service != null) {
            isPaused = true;
            service.setAction("pause");
            context.startService(service);
        }
    }

    public void resumeScreenRecording() {
        if (service != null) {
            isPaused = false;
            service.setAction("resume");
            context.startService(service);
        }
    }

    public boolean isRecordingPaused() {
        return this.isPaused;
    }

    public boolean isBusyRecording() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo runningServiceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (ScreenRecordService.class.getName().equals(runningServiceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void takeScreenshot(Intent intent, int i) {
        Toast.makeText(context, "takeScreenshot()-HBRecorder", Toast.LENGTH_LONG).show();
    }

    public void setNotificationSmallIcon(int i) {
        Bitmap decodeResource = BitmapFactory.decodeResource(this.context.getResources(), i);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        decodeResource.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        this.byteArray = byteArrayOutputStream.toByteArray();
    }

    public void setNotificationSmallIcon(byte[] bArr) {
        this.byteArray = bArr;
    }

    public void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public void setNotificationDescription(String notificationDescription) {
        this.notificationDescription = notificationDescription;
    }

    public void setNotificationButtonText(String notificationButtonText) {
        this.notificationButtonText = notificationButtonText;
    }

    private void startService(Intent intent) {
        try {
            if (!mWasUriSet) {
                if (outputPath != null) {
                    observer = new FileObserver(new File(outputPath).getParent(), this);
                } else {
                    observer = new FileObserver(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)), this);
                }
                observer.startWatching();
            }
            service = new Intent(context, ScreenRecordService.class);
            if (mWasUriSet) {
                service.putExtra("mUri", mUri.toString());
            }
            service.putExtra("code", resultCode);
            service.putExtra("data", intent);
            service.putExtra("audio", isAudioEnabled);
            service.putExtra("width", mScreenWidth);
            service.putExtra("height", mScreenHeight);
            service.putExtra("density", mScreenDensity);
            service.putExtra("quality", isVideoHDEnabled);
            service.putExtra("path", outputPath);
            service.putExtra("fileName", fileName);
            service.putExtra("orientation", orientation);
            service.putExtra("audioBitrate", audioBitrate);
            service.putExtra("audioSamplingRate", audioSamplingRate);
            service.putExtra("notificationSmallBitmap", byteArray);
            service.putExtra("notificationTitle", notificationTitle);
            service.putExtra("notificationDescription", notificationDescription);
            service.putExtra("notificationButtonText", notificationButtonText);
            service.putExtra("enableCustomSettings", enableCustomSettings);
            service.putExtra("audioSource", audioSource);
            service.putExtra("videoEncoder", videoEncoder);
            service.putExtra("videoFrameRate", videoFrameRate);
            service.putExtra("videoBitrate", videoBitrate);
            service.putExtra("outputFormat", outputFormat);
            service.putExtra("listener", new ResultReceiver(new Handler()) {
                @Override
                public void onReceiveResult(int resultCode, Bundle bundle) {
                    super.onReceiveResult(resultCode, bundle);
                    if (resultCode == Activity.RESULT_OK) {
                        String errorReason = bundle.getString("errorReason");
                        String complete = bundle.getString("onComplete");
                        String start = bundle.getString("onStart");
                        if (errorReason != null) {
                            if (!mWasUriSet) {
                                observer.stopWatching();
                            }
                            wasOnErrorCalled = true;
                            hbRecorderListener.HBRecorderOnError(100, errorReason);
                            try {
                                context.stopService(new Intent(context, ScreenRecordService.class));
                            } catch (Exception exception) {
                            }
                        } else if (complete != null) {
                            if (mWasUriSet && !wasOnErrorCalled) {
                                hbRecorderListener.HBRecorderOnComplete();
                            }
                            wasOnErrorCalled = false;
                        } else if (start != null) {
                            hbRecorderListener.HBRecorderOnStart();
                        }
                    }
                }
            });
            context.startService(service);
        } catch (Exception e) {
            hbRecorderListener.HBRecorderOnError(0, Log.getStackTraceString(e));
        }
    }

    public void callback() {
        observer.stopWatching();
        hbRecorderListener.HBRecorderOnComplete();
    }
}
