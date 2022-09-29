package com.hbisoft.hbrecorder;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

public class Const {

    public static final String PREFS_CAMERA_OVERLAY_POS = "camera_overlay_pos";
    public static final String PREFS_INTERNAL_AUDIO_DIALOG_KEY = "int_audio_diag";
    public static final String PREFS_TOOLS_BRUSH = "tools_brush";
    public static final String PREFS_TOOLS_CAMERA = "tools_camera";
    public static final String PREFS_TOOLS_CAPTURE = "tools_capture";
    public static final int CAMERA_SYSTEM_WINDOWS_CODE = 1117;
    public static final int CAMERA_REQUEST_CODE = 1116;
    public static final String ACTION_SCREEN_SHOT = "acction screen shot";
    public static final String SCREEN_SHORT_START = "action.screenshort";

    public static boolean isMyServiceRunning(Class<?> cls, Context context) {
        for (ActivityManager.RunningServiceInfo runningServiceInfo : ((ActivityManager) context.getSystemService("activity")).getRunningServices(Integer.MAX_VALUE)) {
            if (cls.getName().equals(runningServiceInfo.service.getClassName())) {
                Log.i("isMyServiceRunning?", "true");
                return true;
            }
        }
        Log.i("isMyServiceRunning?", "false");
        return false;
    }
}
