package com.fttotal.screenrecorder.helpers;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.snackbar.Snackbar;
import com.fttotal.screenrecorder.BaseApplication;
import com.fttotal.screenrecorder.R;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Utils {
    public static final String ACTION_VIDEO_RECORDED = "ACTION_VIDEO_RECORDED";
    public static final String IS_ADS_DISABLED = "is_ad_disabled";
    public static final String IS_REWARD_VIDEO = "is_reward_video";
    public static final int RESULT_CODE_FAILED = -999999;
    public static final String SCREEN_CAPTURE_INTENT_RESULT_CODE = "SCREEN_CAPTURE_INTENT_RESULT_CODE";
    public static final String VIDEO_DIRECTORY_NAME = "ScreenRecorder";
    public static final List<String> storagePermissionList = Arrays.asList(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    public static void showPermissionAlert(Context context, String message) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.MyAlertDialogStyle)
                .setTitle("Need Permission")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("SETTINGS", (dialog1, which) -> {
                    openSettings(context);
                    dialog1.cancel();
                }).show();
    }


    public static void openSettings(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }



    public static void showSnackBarNotification(View view, String message, int result) {
        Snackbar.make(view, message, result).show();
    }

    public static void toast(Context context, String message, int duration) {
        Toast.makeText(context, message, duration).show();
    }

    public static void createFolder() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), VIDEO_DIRECTORY_NAME);
        if (!file.exists() && file.mkdirs()) {
            Log.i("Folder ", "created");
        }
    }

    public static void createScreenshotFolder() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), VIDEO_DIRECTORY_NAME);
        if (!file.exists() && file.mkdirs()) {
            Log.i("Folder ", "created");
        }
    }

    public static long getUnixTimeStamp() {
        return System.currentTimeMillis() / 1000;
    }

    public static String getFormattedDate(long datetime) {
        return new SimpleDateFormat("EEE, d MMM yyyy HH:mm a").format(new Date(Long.valueOf(datetime).longValue()));
    }

    public static void refreshSystemGallery(String paths) {
        MediaScannerConnection.scanFile(BaseApplication.getContext(), new String[]{paths}, null, (str1, uri) -> {
            Log.i("ExternalStorage", "Scanned " + str1 + ":");
            String sb = "-> uri=" +
                    uri;
            Log.i("ExternalStorage", sb);
        });
    }

    public static long getVideoDuration(File file) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(BaseApplication.getContext(), Uri.fromFile(file));
            long parseLong = Long.parseLong(mediaMetadataRetriever.extractMetadata(9));
            mediaMetadataRetriever.release();
            return parseLong;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int getVideoHeight(File file) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(BaseApplication.getContext(), Uri.fromFile(file));
        return Integer.parseInt(mediaMetadataRetriever.extractMetadata(19));
    }

    public static int getVideoWidth(File file) {
//        Log.e("SSSSS ",file.getPath());
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(BaseApplication.getContext(), Uri.fromFile(file));
        return Integer.parseInt(mediaMetadataRetriever.extractMetadata(18));
    }

    public static boolean isAppOnForeground(Context context) {
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses();
        if (runningAppProcesses == null) {
            return false;
        }
        String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo next : runningAppProcesses) {
            if (next.importance == 100 && next.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static void startActivityAllStage(Context context, Intent intent) {
        if (context instanceof Activity) {
            context.startActivity(intent);
            return;
        }
        try {
            PendingIntent.getActivity(context, (int) (Math.random() * 9999.0d), intent, 134217728).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            context.startActivity(intent);
        }
    }

    public static boolean isServiceRunning(String serviceName,Context context){
        boolean serviceRunning = false;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> l = am.getRunningServices(50);
        Iterator<ActivityManager.RunningServiceInfo> i = l.iterator();
        while (i.hasNext()) {
            ActivityManager.RunningServiceInfo runningServiceInfo = i
                    .next();

            if(runningServiceInfo.service.getClassName().equals(serviceName)){
                serviceRunning = true;

                if(runningServiceInfo.foreground)
                {
                    //service run in foreground
                }
            }
        }
        return serviceRunning;
    }
}
