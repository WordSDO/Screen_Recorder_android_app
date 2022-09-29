package com.fttotal.screenrecorder.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.fttotal.screenrecorder.MainActivity;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.ads.AdsService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class VideoPlayerActivity extends AppCompatActivity {
    private double current_pos;
    private TextView currentduration;
    private Handler handler;
    private ImageView iconplay;
    private boolean isPause = false;
    private boolean isfirst;
    private Handler mHandler;
    private String path;
    private ProgressDialog progressDialog;
    private SeekBar seekBar;
    private double total_duration;
    private TextView txtEnding;
    private VideoView videoView;
    private FrameLayout ad_view_container;
    private ActivityResultLauncher<IntentSenderRequest> resolveLauncherFriendsConsent;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_video_player);

        path = getIntent().getStringExtra("UriString");
        videoView = findViewById(R.id.video);
        ad_view_container = findViewById(R.id.ad_view_container);
        seekBar = findViewById(R.id.seekactualview);
        iconplay = findViewById(R.id.icon_video_play);
        currentduration = findViewById(R.id.starting);
        txtEnding = findViewById(R.id.txtEnding);
        isfirst = true;
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.deletingTitle));
        currentduration.setText("00:00");
        setVideo();

        AdsService.getInstance().showAdaptiveBannerAd(ad_view_container);

        findViewById(R.id.back).setOnClickListener(view -> onBackPressed());
        findViewById(R.id.delete).setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(VideoPlayerActivity.this, R.style.MyDialogStyle);
            builder.setMessage(getString(R.string.deletingMsg));
            builder.setCancelable(true);
            builder.setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                dialogInterface.cancel();
                try {
                    new DeleteVideo(new File(path)).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            builder.setNegativeButton(getString(R.string.txt_cancel), (dialogInterface, i) -> dialogInterface.cancel());
            final AlertDialog create = builder.create();
            create.setOnShowListener(dialogInterface -> {
                create.getButton(-2).setTextColor(Color.parseColor("#0b1c52"));
                create.getButton(-1).setTextColor(Color.parseColor("#0b1c52"));
            });
            create.show();
        });
        findViewById(R.id.share).setOnClickListener(view -> {
            File file = new File(path);
            Intent intent = new Intent();
            intent.setAction("android.intent.action.SEND");
            Uri uriForFile = FileProvider.getUriForFile(VideoPlayerActivity.this, getApplicationContext().getPackageName() + ".fileprovider", file);
            intent.setType("application/video");
            intent.putExtra("android.intent.extra.STREAM", uriForFile);
            startActivity(Intent.createChooser(intent, "Share using"));
        });
        iconplay.setOnClickListener(view -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                iconplay.setImageResource(R.drawable.ic_play_video);
                return;
            }
            videoView.start();
            iconplay.setImageResource(R.drawable.ic_pause);
        });
        videoView.setOnCompletionListener(mediaPlayer -> {
            iconplay.setImageResource(R.drawable.ic_play_preview);
            seekBar.setProgress(0);
        });

        resolveLauncherFriendsConsent =
                registerForActivityResult(
                        new ActivityResultContracts.StartIntentSenderForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {

                            } else {

                            }
                        });
    }


    public class DeleteVideo extends AsyncTask<String, String, Boolean> {
        File file;

        DeleteVideo(File file) throws IOException {
            this.file = file;
            progressDialog.show();
        }

        @Override
        public Boolean doInBackground(String... params) {
            deleteFile(file.getAbsolutePath());
            return true;
        }

        @Override
        public void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        public void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            finish();
            startActivity(new Intent(VideoPlayerActivity.this, MainActivity.class));
        }
    }

    public boolean deleteFile(String fileName) {
        String[] selectionArgs = {fileName};
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
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

    public void setVideo() {
        mHandler = new Handler();
        handler = new Handler();
        videoView.setOnCompletionListener(mediaPlayer -> {
            iconplay.setImageResource(R.drawable.ic_pause);
            currentduration.setText("00:00");
            seekBar.setProgress(0);
        });
        videoView.setOnPreparedListener(mediaPlayer -> setVideoProgress());
        playVideo();
    }

    public void setVideoProgress() {
        current_pos = videoView.getCurrentPosition();
        total_duration = videoView.getDuration();
        txtEnding.setText(timeConversion((long) total_duration));
        currentduration.setText(timeConversion((long) current_pos));
        seekBar.setMax((int) total_duration);
        final Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    current_pos = videoView.getCurrentPosition();
                    currentduration.setText(timeConversion((long) current_pos));
                    seekBar.setProgress((int) current_pos);
                    handler2.postDelayed(this, 1000);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                current_pos = seekBar.getProgress();
                videoView.seekTo((int) current_pos);
            }
        });
    }

    public String timeConversion(long j) {
        int i = (int) j;
        int i2 = i / 3600000;
        int i3 = (i / 60000) % 60000;
        int i4 = (i % 60000) / 1000;
        if (i2 > 0) {
            return String.format("%02d:%02d:%02d", new Object[]{Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4)});
        }
        return String.format("%02d:%02d", new Object[]{Integer.valueOf(i3), Integer.valueOf(i4)});
    }

    public void playVideo() {
        try {
            videoView.setVideoPath(path);
            videoView.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isPause) {
            videoView.seekTo(1);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            isPause = true;
            videoView.pause();
            iconplay.setImageResource(R.drawable.ic_play_video);
        }
    }
}
