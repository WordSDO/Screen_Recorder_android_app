package com.fttotal.screenrecorder.videotrimming;

import android.util.Log;

public class LogMessage {
    public static final boolean IS_LOG = true;

    public static void v(String message) {
        Log.v("VIDEO_TRIMMER ::", message);
    }

    public static void e(String message) {
        Log.e("VIDEO_TRIMMER ::", message);
    }
}
