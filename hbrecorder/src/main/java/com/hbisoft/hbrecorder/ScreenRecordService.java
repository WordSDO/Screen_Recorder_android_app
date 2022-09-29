package com.hbisoft.hbrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.Objects;

public class ScreenRecordService extends Service {
    public static final String BUNDLED_LISTENER = "listener";
    private static final String TAG = "ScreenRecordService";
    private static String fileName;
    private static String filePath;
    private int audioBitrate;
    private int audioSamplingRate;
    private int audioSourceAsInt;
    private boolean isAudioEnabled;
    private boolean isCustomSettingsEnabled;
    private boolean isVideoHD;
    private Intent mIntent;
    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private int mResultCode;
    private Intent mResultData;
    private int mScreenDensity;
    private int mScreenHeight;
    private int mScreenWidth;
    private VirtualDisplay mVirtualDisplay;
    private String name;
    private int orientationHint;
    private int outputFormatAsInt;
    private String path;
    private Uri returnedUri = null;
    private int videoBitrate;
    private int videoEncoderAsInt;
    private int videoFrameRate;

    private ServiceConnection floatingCameraConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ((FloatingCameraViewService.ServiceBinder) iBinder).getService();
        }

        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private boolean alreadyStarted = false;
    private boolean showCameraOverlay;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
//        showCameraOverlay = prefs.getBoolean(getString(R.string.preference_camera_overlay_key), false);

        Notification notification;
        String pauseResumeAction = intent.getAction();
        if (pauseResumeAction == null || !pauseResumeAction.equals("pause")) {
            if (pauseResumeAction == null || !pauseResumeAction.equals("resume")) {
                this.mIntent = intent;
                byte[] notificationSmallIcon = intent.getByteArrayExtra("notificationSmallBitmap");
                String notificationTitle = intent.getStringExtra("notificationTitle");
                String notificationDescription = intent.getStringExtra("notificationDescription");
                String notificationButtonText = intent.getStringExtra("notificationButtonText");
                orientationHint = intent.getIntExtra("orientation", 400);
                mResultCode = intent.getIntExtra("code", -1);
                mResultData = (Intent) intent.getParcelableExtra("data");
                mScreenWidth = intent.getIntExtra("width", 0);
                mScreenHeight = intent.getIntExtra("height", 0);
                if (intent.getStringExtra("mUri") != null) {
                    returnedUri = Uri.parse(intent.getStringExtra("mUri"));
                }
                if (mScreenHeight == 0 || mScreenWidth == 0) {
                    HBRecorderCodecInfo hBRecorderCodecInfo = new HBRecorderCodecInfo();
                    hBRecorderCodecInfo.setContext(this);
                    mScreenHeight = hBRecorderCodecInfo.getMaxSupportedHeight();
                    mScreenWidth = hBRecorderCodecInfo.getMaxSupportedWidth();
                }
                mScreenDensity = intent.getIntExtra("density", 1);
                isVideoHD = intent.getBooleanExtra("quality", true);
                isAudioEnabled = intent.getBooleanExtra("audio", true);
                path = intent.getStringExtra("path");
                name = intent.getStringExtra("fileName");
                String audioSource = intent.getStringExtra("audioSource");
                String videoEncoder = intent.getStringExtra("videoEncoder");
                videoFrameRate = intent.getIntExtra("videoFrameRate", 30);
                videoBitrate = intent.getIntExtra("videoBitrate", 40000000);
                if (audioSource != null) {
                    setAudioSourceAsInt(audioSource);
                }
                if (videoEncoder != null) {
                    setvideoEncoderAsInt(videoEncoder);
                }
                filePath = name;
                audioBitrate = intent.getIntExtra("audioBitrate", 128000);
                audioSamplingRate = intent.getIntExtra("audioSamplingRate", 44100);
                String outputFormat = intent.getStringExtra("outputFormat");
                if (outputFormat != null) {
                    setOutputFormatAsInt(outputFormat);
                }
                isCustomSettingsEnabled = intent.getBooleanExtra("enableCustomSettings", false);
                if (notificationButtonText == null) {
                    notificationButtonText = "STOP RECORDING";
                }
                if (audioBitrate == 0) {
                    audioBitrate = 128000;
                }
                if (audioSamplingRate == 0) {
                    audioSamplingRate = 44100;
                }
                if (notificationTitle == null || notificationTitle.equals("")) {
                    notificationTitle = "Recording your screen";
                }
                if (notificationDescription == null || notificationDescription.equals("")) {
                    notificationDescription = "Drag down to stop the recording";
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    String channelId = "001";
                    String channelName = "RecordChannel";
                    NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
                    notificationChannel.setLightColor(Color.BLUE);
                    notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        notificationManager.createNotificationChannel(notificationChannel);
                        Notification.Action build = new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.presence_video_online), notificationButtonText, PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationReceiver.class), 0)).build();
                        if (notificationSmallIcon != null) {
                            notification = new Notification.Builder(getApplicationContext(), channelId).setOngoing(true).setSmallIcon(Icon.createWithBitmap(BitmapFactory.decodeByteArray(notificationSmallIcon, 0, notificationSmallIcon.length))).setContentTitle(notificationTitle).setContentText(notificationDescription).addAction(build).build();
                        } else {
                            notification = new Notification.Builder(getApplicationContext(), channelId).setOngoing(true).setSmallIcon(R.drawable.icon).setContentTitle(notificationTitle).setContentText(notificationDescription).addAction(build).build();
                        }
                        startForeground(102, notification);
                    }
                } else {
                    startForeground(102, new Notification());
                }
                if (this.returnedUri == null && this.path == null) {
                    this.path = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
                }
                try {
                    initRecorder();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("EEEEEEE ", e.getMessage());
                    ResultReceiver resultReceiver = (ResultReceiver) intent.getParcelableExtra("listener");
                    Bundle bundle = new Bundle();
                    bundle.putString("errorReason", Log.getStackTraceString(e));
                    if (resultReceiver != null) {
                        resultReceiver.send(-1, bundle);
                    }
                }
                try {
                    initMediaProjection();
                } catch (Exception e2) {
                    ResultReceiver resultReceiver2 = (ResultReceiver) intent.getParcelableExtra("listener");
                    Bundle bundle2 = new Bundle();
                    bundle2.putString("errorReason", Log.getStackTraceString(e2));
                    if (resultReceiver2 != null) {
                        resultReceiver2.send(-1, bundle2);
                    }
                }
                try {
                    initVirtualDisplay();
                } catch (Exception e3) {
                    ResultReceiver resultReceiver3 = (ResultReceiver) intent.getParcelableExtra("listener");
                    Bundle bundle3 = new Bundle();
                    bundle3.putString("errorReason", Log.getStackTraceString(e3));
                    if (resultReceiver3 != null) {
                        resultReceiver3.send(-1, bundle3);
                    }
                }


                mMediaRecorder.setOnErrorListener((mediaRecorder, what, extra) -> {
                    ResultReceiver resultReceiver = (ResultReceiver) intent.getParcelableExtra("listener");
                    Bundle bundle = new Bundle();
                    bundle.putString("error", "38");
                    bundle.putString("errorReason", String.valueOf(what));
                    if (resultReceiver != null) {
                        resultReceiver.send(-1, bundle);
                    }
                });

                    try {
                        mMediaRecorder.start();

                        /*Intent intent2 = new Intent(this, FloatingCameraViewService.class);
                        startService(intent2);
                        bindService(intent2, this.floatingCameraConnection, 1);*/

                        ResultReceiver resultReceiver4 = (ResultReceiver) intent.getParcelableExtra("listener");
                        Bundle bundle4 = new Bundle();
                        bundle4.putString("onStart", "111");
                        if (resultReceiver4 != null) {
                            resultReceiver4.send(-1, bundle4);
                        }
                    } catch (Exception e4) {
                        ResultReceiver resultReceiver5 = (ResultReceiver) intent.getParcelableExtra("listener");
                        Bundle bundle5 = new Bundle();
                        bundle5.putString("error", "38");
                        bundle5.putString("errorReason", Log.getStackTraceString(e4));
                        if (resultReceiver5 != null) {
                            resultReceiver5.send(-1, bundle5);
                        }
                    }


            } else if (Build.VERSION.SDK_INT >= 24) {
                resumeRecording();
            }
        } else if (Build.VERSION.SDK_INT >= 24) {
            pauseRecording();
        }
        return Service.START_STICKY;
    }

    //Pause Recording
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void pauseRecording() {
        if (mMediaRecorder != null) mMediaRecorder.pause();
    }

    //Resume Recording
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resumeRecording() {
        if (mMediaRecorder != null) mMediaRecorder.resume();
    }

    //Set output format as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private void setOutputFormatAsInt(String outputFormat) {
        switch (outputFormat) {
            case "DEFAULT":
                outputFormatAsInt = 0;
                break;
            case "THREE_GPP":
                outputFormatAsInt = 1;
                break;
            case "AMR_NB":
                outputFormatAsInt = 3;
                break;
            case "AMR_WB":
                outputFormatAsInt = 4;
                break;
            case "AAC_ADTS":
                outputFormatAsInt = 6;
                break;
            case "MPEG_2_TS":
                outputFormatAsInt = 8;
                break;
            case "WEBM":
                outputFormatAsInt = 9;
                break;
            case "OGG":
                outputFormatAsInt = 11;
                break;
            case "MPEG_4":
            default:
                outputFormatAsInt = 2;
        }
    }


    //Set video encoder as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private void setvideoEncoderAsInt(String encoder) {
        switch (encoder) {
            case "DEFAULT":
                videoEncoderAsInt = 0;
                break;
            case "H263":
                videoEncoderAsInt = 1;
                break;
            case "H264":
                videoEncoderAsInt = 2;
                break;
            case "MPEG_4_SP":
                videoEncoderAsInt = 3;
                break;
            case "VP8":
                videoEncoderAsInt = 4;
                break;
            case "HEVC":
                videoEncoderAsInt = 5;
                break;
        }
    }

    //Set audio source as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private void setAudioSourceAsInt(String audioSource) {
        switch (audioSource) {
            case "DEFAULT":
                audioSourceAsInt = 0;
                break;
            case "MIC":
                audioSourceAsInt = 1;
                break;
            case "VOICE_UPLINK":
                audioSourceAsInt = 2;
                break;
            case "VOICE_DOWNLINK":
                audioSourceAsInt = 3;
                break;
            case "VOICE_CALL":
                audioSourceAsInt = 4;
                break;
            case "CAMCODER":
                audioSourceAsInt = 5;
                break;
            case "VOICE_RECOGNITION":
                audioSourceAsInt = 6;
                break;
            case "VOICE_COMMUNICATION":
                audioSourceAsInt = 7;
                break;
            case "REMOTE_SUBMIX":
                audioSourceAsInt = 8;
                break;
            case "UNPROCESSED":
                audioSourceAsInt = 9;
                break;
            case "VOICE_PERFORMANCE":
                audioSourceAsInt = 10;
                break;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initMediaProjection() {
        mMediaProjection = ((MediaProjectionManager) Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE))).getMediaProjection(mResultCode, mResultData);
    }

    public static String getFilePath() {
        return filePath;
    }

    public static String getFileName() {
        return fileName;
    }

    private long getUnixTimeStamp() {
        return System.currentTimeMillis() / 1000;
    }

    private void initRecorder() throws Exception {
        String videoQuality = !isVideoHD ? "SD-" : "HD-";
        if (name == null) {
            name = videoQuality + getUnixTimeStamp();
        }

        filePath = path + "/" + name + ".mp4";
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(".mp4");
        fileName = sb.toString();
        mMediaRecorder = new MediaRecorder();
//        Log.e("SSSSSS ", String.valueOf(audioSourceAsInt));
        if (isAudioEnabled) {
            Log.e("audioSourceAsInt ", String.valueOf(audioSourceAsInt));
//            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setAudioSource(audioSourceAsInt);
            mMediaRecorder.setAudioChannels(1);
//            mMediaRecorder.setAudioSource(audioSourceAsInt);
//            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(outputFormatAsInt);

        if (orientationHint != 400) {
            mMediaRecorder.setOrientationHint(orientationHint);
        }
        if (isAudioEnabled) {
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setAudioEncodingBitRate(256000); //24
            mMediaRecorder.setAudioSamplingRate(88200);  //24
//            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//            mMediaRecorder.setAudioEncodingBitRate(audioBitrate);
//            mMediaRecorder.setAudioSamplingRate(audioSamplingRate);
        }
//        mMediaRecorder.setVideoEncoder(videoEncoderAsInt);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if (returnedUri != null) {
            try {
                ParcelFileDescriptor openFileDescriptor = getContentResolver().openFileDescriptor(returnedUri, "rw");
                Objects.requireNonNull(openFileDescriptor);
                mMediaRecorder.setOutputFile(openFileDescriptor.getFileDescriptor());
            } catch (Exception e) {
                ResultReceiver resultReceiver = (ResultReceiver) mIntent.getParcelableExtra("listener");
                Bundle bundle = new Bundle();
                bundle.putString("errorReason", Log.getStackTraceString(e));
                if (resultReceiver != null) {
                    resultReceiver.send(-1, bundle);
                }
            }
        } else {
            mMediaRecorder.setOutputFile(filePath);
        }
        mMediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);
        if (isCustomSettingsEnabled) {
            mMediaRecorder.setVideoEncodingBitRate(videoBitrate);
            mMediaRecorder.setVideoFrameRate(videoFrameRate);
        } else if (!isVideoHD) {
            mMediaRecorder.setVideoEncodingBitRate(12000000);
            mMediaRecorder.setVideoFrameRate(30);
        } else {
            mMediaRecorder.setVideoEncodingBitRate(mScreenWidth * 5 * mScreenHeight);
            mMediaRecorder.setVideoFrameRate(60);
        }
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Audio", "unable to prepare");
        }

    }

    private void initVirtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG, mScreenWidth, mScreenHeight, mScreenDensity, 16, mMediaRecorder.getSurface(), null, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetAll();
        callOnComplete();
    }

    private void callOnComplete() {
        ResultReceiver resultReceiver = (ResultReceiver) mIntent.getParcelableExtra("listener");
        Bundle bundle = new Bundle();
        bundle.putString("onComplete", "Uri was passed");
        if (resultReceiver != null) {
            resultReceiver.send(-1, bundle);
        }
    }

    private void resetAll() {
        stopForeground(true);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener((MediaRecorder.OnErrorListener) null);
            mMediaRecorder.reset();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }
}
