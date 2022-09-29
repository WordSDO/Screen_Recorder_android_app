package com.fttotal.screenrecorder.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {
    private static final String CHANNEL_DESCRIPTION_SERVICE = "floating_channel_description";
    public static final String CHANNEL_ID_SERVICE = "service_channel_id";
    public static final String CHANNEL_NAME_SERVICE = "floating service";
    private static final NotificationHelper ourInstance = new NotificationHelper();

    public static NotificationHelper getInstance() {
        return ourInstance;
    }

    private NotificationHelper() {
    }

    public void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_SERVICE, CHANNEL_NAME_SERVICE, 3);
            notificationChannel.setDescription(CHANNEL_DESCRIPTION_SERVICE);
            ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannel(notificationChannel);
        }
    }
}
