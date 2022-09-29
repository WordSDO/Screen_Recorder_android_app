package com.fttotal.screenrecorder.services;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.provider.MediaStore;
import android.util.Log;

import com.google.android.exoplayer2.util.MimeTypes;
import com.hbisoft.hbrecorder.HBRecorder;
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

public class FloatingSSCapService extends Service /*implements HBRecorderListener*/ {
    private IBinder binder = new FloatingSSCapService.ServiceBinder();
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

    private HBRecorder hbRecorder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
//            hbRecorder = new HBRecorder(this, this);
//            setOutputPath();
            mScreenCaptureIntent = intent.getParcelableExtra("android.intent.extra.INTENT");
            captureScreen();
        }

     /*   if (intent.getParcelableExtra("ss_ready") != null) {

        }*/

        return super.onStartCommand(intent, flags, startId);
    }

    private static int getVirtualDisplayFlags() {
        return 16;
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
    }

    public IBinder onBind(Intent intent) {
        Log.d("TAG", "Binding successful!");
        return binder;
    }

  /*  @Override
    public void HBRecorderOnStart() {

    }

    @Override
    public void HBRecorderOnComplete() {

    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {

    }*/

    public class ServiceBinder extends Binder {
        public ServiceBinder() {
        }

        public FloatingSSCapService getService() {
            return FloatingSSCapService.this;
        }
    }

    public void captureScreen() {
        new Handler().postDelayed(() -> {
            initScreenshotRecorder();
            initImageReader();
            createImageVirtualDisplay();
        }, 300);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            stopSelf();
        }
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

    @Override
    public void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            stopSelf();
        }
    }

}
