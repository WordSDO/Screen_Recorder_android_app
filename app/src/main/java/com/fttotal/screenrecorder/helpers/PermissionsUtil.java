package com.fttotal.screenrecorder.helpers;

import androidx.core.content.ContextCompat;
import com.fttotal.screenrecorder.BaseApplication;

public class PermissionsUtil {
    public static boolean checkWriteStoragePermission() {
        return ContextCompat.checkSelfPermission(BaseApplication.getContext(), "android.permission.WRITE_EXTERNAL_STORAGE") == 0;
    }

    public static boolean checkReadStoragePermission() {
        return ContextCompat.checkSelfPermission(BaseApplication.getContext(), "android.permission.READ_EXTERNAL_STORAGE") == 0;
    }
}
