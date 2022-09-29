package com.fttotal.screenrecorder.activities;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.hbisoft.hbrecorder.Const;
import com.fttotal.screenrecorder.helpers.Utils;
import com.fttotal.screenrecorder.services.FloatingSSCapService;

public class ScreenShotActivity extends AppCompatActivity {
    private int type = 0;
    private static final int PERMISSION_RECORD_DISPLAY = 3006;
    private Intent mScreenCaptureIntent = null;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        startScreenCapturing();
    }

    public void startScreenCapturing() {
        if (Settings.canDrawOverlays(this)) {
            startActivityForResult(((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE)).createScreenCaptureIntent(), PERMISSION_RECORD_DISPLAY);
            return;
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == PERMISSION_RECORD_DISPLAY) {
            if (resultCode != -1) {
//                Utils.showSnackBarNotification(iv_record, getString(R.string.recordingPermissionNotGranted), -1);
                mScreenCaptureIntent = null;
                finish();
                return;
            }
            mScreenCaptureIntent = intent;
            intent.putExtra(Utils.SCREEN_CAPTURE_INTENT_RESULT_CODE, resultCode);
            new Handler().postDelayed(() -> startCaptureScreen(resultCode, intent), 250);
        }
    }

    public void startCaptureScreen(int i, Intent intent) {
        try {
            Intent intent2 = new Intent(this, FloatingSSCapService.class);
            intent2.setAction(Const.SCREEN_SHORT_START);
            intent2.putExtra("screenshort_resultcode", i);
            intent2.putExtra("android.intent.extra.INTENT", mScreenCaptureIntent);
            intent2.putExtra("screenshort_type", this.type);
            startService(intent2);
          /*  if (!Utils.isServiceRunning(FloatingSSCapService.class.getSimpleName(), getApplicationContext())) {
                if (Build.VERSION.SDK_INT >= 26) {
                   startForegroundService(intent2);
                } else {
                    startService(intent2);
                }
            }*/
            finish();
        } catch (Exception e) {
            Log.d("TAG", "startCaptureScreen: BB " + e.getLocalizedMessage());
        }
    }
}

