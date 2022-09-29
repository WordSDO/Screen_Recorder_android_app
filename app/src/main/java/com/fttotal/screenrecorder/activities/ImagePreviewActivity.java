package com.fttotal.screenrecorder.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.jsibbold.zoomage.ZoomageView;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.helpers.Utils;

import java.io.File;
import java.util.ArrayList;

public class ImagePreviewActivity extends AppCompatActivity {
    private static final String TAG = "ImagePreviewActivity";
    private ZoomageView demoView;
    String screenshotPath = "";
    private ActivityResultLauncher<IntentSenderRequest> resolveLauncherFriendsConsent;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_image_preview);
        demoView = findViewById(R.id.iv_preview);
        screenshotPath = getIntent().getStringExtra("path");

        Glide.with(this).load(screenshotPath).into(demoView);
        findViewById(R.id.btn_close).setOnClickListener(view -> {
            finish();
        });
        findViewById(R.id.btn_delete).setOnClickListener(view -> {
            showDeleteDialog();
        });
        findViewById(R.id.btn_share).setOnClickListener(view -> {
            Uri uriForFile = FileProvider.getUriForFile(ImagePreviewActivity.this, getPackageName() + ".fileprovider", new File(screenshotPath));
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("image/*");
            intent.putExtra("android.intent.extra.STREAM", uriForFile);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });

        resolveLauncherFriendsConsent =
                registerForActivityResult(
                        new ActivityResultContracts.StartIntentSenderForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                Utils.toast(ImagePreviewActivity.this, getString(R.string.screenshotsDeleted), Toast.LENGTH_LONG);
                                setResult(-1);
                                finish();
                            }
                        });
    }

    public void showDeleteDialog() {
        new AlertDialog.Builder(this, R.style.MyDialogStyle)
                .setTitle(getString(R.string.caution))
                .setMessage(getString(R.string.deleteSSMsg))
                .setPositiveButton(getString(R.string.yes), (dialogInterface, i) -> {
                    File file = new File(screenshotPath);
                    deleteFile(file.getAbsolutePath());
                })
                .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                }).show();
    }


    public boolean deleteFile(String fileName) {
        String[] selectionArgs = {fileName};
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        Cursor query = contentResolver.query(uri, new String[]{"_id"}, "_data = ?", selectionArgs, null);
        if (query != null) {
            if (query.moveToFirst()) {
                Uri withAppendedId = ContentUris.withAppendedId(uri, query.getLong(query.getColumnIndexOrThrow("_id")));
                delete(resolveLauncherFriendsConsent, withAppendedId);
                Log.e("TAG", "deletFile: " + withAppendedId);
            } else {
                Log.e("TAG", "deletFile:file not found ");
            }
            query.close();
        }
        return false;
    }

    public void delete(ActivityResultLauncher<IntentSenderRequest> launcher, Uri uri) {

        ContentResolver contentResolver = getContentResolver();

        try {

            //delete object using resolver
            int del = contentResolver.delete(uri, null, null);
            if (del > 0) {
                Utils.toast(this, getString(R.string.screenshotsDeleted), Toast.LENGTH_LONG);
                setResult(-1);
                finish();
            }


        } catch (SecurityException e) {

            PendingIntent pendingIntent = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ArrayList<Uri> collection = new ArrayList<>();
                collection.add(uri);
                pendingIntent = MediaStore.createDeleteRequest(contentResolver, collection);

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                //if exception is recoverable then again send delete request using intent
                if (e instanceof RecoverableSecurityException) {
                    RecoverableSecurityException exception = (RecoverableSecurityException) e;
                    pendingIntent = exception.getUserAction().getActionIntent();
                }
            }
            if (pendingIntent != null) {
                IntentSender sender = pendingIntent.getIntentSender();
                IntentSenderRequest request = new IntentSenderRequest.Builder(sender).build();
                launcher.launch(request);

            }
        }
    }

}
