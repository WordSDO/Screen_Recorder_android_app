package com.fttotal.screenrecorder.activities;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hbisoft.hbrecorder.Const;
import com.hbisoft.hbrecorder.FloatingControlCameraService;

public class CheckPermissionActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getIntent() != null && getIntent().getExtras().containsKey("boolean")) {
            requestCameraPermission(getIntent().getExtras().getBoolean("boolean"));
            requestSystemWindowsPermission(Const.CAMERA_SYSTEM_WINDOWS_CODE);
        }
    }

    public void requestCameraPermission(boolean isPermission) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, Const.CAMERA_REQUEST_CODE);
            return;
        }
        Intent intent = new Intent(this, FloatingControlCameraService.class);
        intent.putExtra("facing", getIntent().getIntExtra("facing", 1));
        if (!isPermission) {
            stopService(intent);
        } else if (!Const.isMyServiceRunning(FloatingControlCameraService.class, getApplicationContext())) {
            startService(intent);
        }
        finish();
    }

    public void requestSystemWindowsPermission(int i) {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName())), i);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 1116) {
            return;
        }
        if (grantResults.length <= 0 || grantResults[0] == 0) {
            startService(new Intent(this, FloatingControlCameraService.class));
            finish();
            return;
        }
        finish();
    }
}

