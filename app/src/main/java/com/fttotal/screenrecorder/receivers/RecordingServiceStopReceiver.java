package com.fttotal.screenrecorder.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.fttotal.screenrecorder.services.HBService;

public class RecordingServiceStopReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Stop ", "Stopping Service Now ...");
        context.stopService(new Intent(context, HBService.class));
    }
}
