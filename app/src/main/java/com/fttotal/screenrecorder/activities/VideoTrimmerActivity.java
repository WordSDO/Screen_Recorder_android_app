package com.fttotal.screenrecorder.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.videotrimming.CompressOption;
import com.fttotal.screenrecorder.videotrimming.CustomProgressView;
import com.fttotal.screenrecorder.videotrimming.LogMessage;
import com.fttotal.screenrecorder.videotrimming.TrimVideo;
import com.fttotal.screenrecorder.videotrimming.TrimVideoOptions;
import com.fttotal.screenrecorder.videotrimming.TrimmerUtils;
import com.fttotal.screenrecorder.widgets.rangeseekbar.CrystalRangeSeekbar;
import com.fttotal.screenrecorder.widgets.rangeseekbar.CrystalSeekbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class VideoTrimmerActivity extends AppCompatActivity {
    private static final int PER_REQ_CODE = 115;
    private CompressOption compressOption;

    public long currentDuration;
    private String destinationPath;
    private Dialog dialog;
    private String fileName;
    private long fixedGap;
    private boolean hidePlayerSeek;

    public ImageView imagePlayPause;
    private ImageView[] imageViews;
    private boolean isAccurateCut;
    private boolean isValidVideo = true;

    public boolean isVideoEnded;
    private long lastClickedTime;

    public long lastMaxValue = 0;
    private long lastMinValue = 0;
    private long maxToGap;
    private MenuItem menuDone;
    private long minFromGap;
    private long minGap;
    private String outputPath;
    private PlayerView playerView;
    private CustomProgressView progressView;

    public Handler seekHandler;
    private CrystalRangeSeekbar seekbar;

    public CrystalSeekbar seekbarController;
    private long totalDuration;
    private int trimType;
    private TextView txtEndDuration;
    private TextView txtStartDuration;
    Runnable updateSeekbar = new Runnable() {
        @Override
        public void run() {
            try {
                currentDuration = videoPlayer.getCurrentPosition() / 1000;
                if (videoPlayer.getPlayWhenReady()) {
                    if (currentDuration <= lastMaxValue) {
                        seekbarController.setMinStartValue((float) ((int) currentDuration)).apply();
                    } else {
                        videoPlayer.setPlayWhenReady(false);
                    }
                    seekHandler.postDelayed(updateSeekbar, 1000);
                }
            } finally {
                seekHandler.postDelayed(updateSeekbar, 1000);
            }
        }
    };
    private Uri uri;

    public ExoPlayer videoPlayer;
    private ActivityResultLauncher<IntentSenderRequest> resolveLauncherFriendsConsent;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_video_trimmer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setUpToolBar(getSupportActionBar(), getString(R.string.txt_edt_video));
        toolbar.setNavigationOnClickListener(view -> finish());
        progressView = new CustomProgressView(this);

        resolveLauncherFriendsConsent =
                registerForActivityResult(
                        new ActivityResultContracts.StartIntentSenderForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {

                            } else {

                            }
                        });
    }

    @Override
    public void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        playerView = findViewById(R.id.player_view_lib);
        imagePlayPause = findViewById(R.id.image_play_pause);
        seekbar = findViewById(R.id.range_seek_bar);
        txtStartDuration = findViewById(R.id.txt_start_duration);
        txtEndDuration = findViewById(R.id.txt_end_duration);
        seekbarController = findViewById(R.id.seekbar_controller);
        imageViews = new ImageView[]{findViewById(R.id.image_one), findViewById(R.id.image_two), findViewById(R.id.image_three), findViewById(R.id.image_four), findViewById(R.id.image_five), findViewById(R.id.image_six), findViewById(R.id.image_seven), findViewById(R.id.image_eight)};
        seekHandler = new Handler();
        initPlayer();
        if (checkStoragePermission()) {
            setDataInView();
        }
    }

    private void setUpToolBar(ActionBar actionBar, String title) {
        try {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPlayer() {
        try {
            videoPlayer = new ExoPlayer.Builder(this).build();
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            playerView.setPlayer(videoPlayer);
            videoPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.CONTENT_TYPE_MOVIE).build(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDataInView() {
        try {
            uri = Uri.parse(getIntent().getStringExtra(TrimVideo.TRIM_VIDEO_URI));
            Log.d("EditVideo", "URI: " + uri);
            totalDuration = TrimmerUtils.getDuration(this, uri);
            imagePlayPause.setOnClickListener(view -> onVideoClicked());
            View videoSurfaceView = playerView.getVideoSurfaceView();
            Objects.requireNonNull(videoSurfaceView);
            videoSurfaceView.setOnClickListener(view -> onVideoClicked());
            initTrimData();
            buildMediaSource(uri);
            loadThumbnails();
            setUpSeekBar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTrimData() {
        try {
            TrimVideoOptions trimVideoOptions = getIntent().getParcelableExtra(TrimVideo.TRIM_VIDEO_OPTION);
            trimType = TrimmerUtils.getTrimType(trimVideoOptions.trimType);
            destinationPath = trimVideoOptions.destination;
            fileName = trimVideoOptions.fileName;
            hidePlayerSeek = trimVideoOptions.hideSeekBar;
            isAccurateCut = trimVideoOptions.accurateCut;
            compressOption = trimVideoOptions.compressOption;
            long duration = trimVideoOptions.fixedDuration;
            fixedGap = duration;
            if (duration == 0) {
                duration = totalDuration;
            }
            fixedGap = duration;
            long j2 = trimVideoOptions.minDuration;
            minGap = j2;
            if (j2 == 0) {
                j2 = totalDuration;
            }
            minGap = j2;
            if (trimType == 3) {
                minFromGap = trimVideoOptions.minToMax[0];
                long j3 = trimVideoOptions.minToMax[1];
                maxToGap = j3;
                long j4 = minFromGap;
                if (j4 == 0) {
                    j4 = totalDuration;
                }
                minFromGap = j4;
                if (j3 == 0) {
                    j3 = totalDuration;
                }
                maxToGap = j3;
            }
            if (destinationPath != null) {
                File file = new File(destinationPath);
                file.mkdirs();
                destinationPath = String.valueOf(file);
                if (!file.isDirectory()) {
                    throw new IllegalArgumentException("Destination file path error " + destinationPath);
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void onVideoClicked() {
        try {
            boolean playWhenReady = true;
            if (isVideoEnded) {
                seekTo(lastMinValue);
                videoPlayer.setPlayWhenReady(true);
                return;
            }
            if (currentDuration - lastMaxValue > 0) {
                seekTo(lastMinValue);
            }
            if (videoPlayer.getPlayWhenReady()) {
                playWhenReady = false;
            }
            videoPlayer.setPlayWhenReady(playWhenReady);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seekTo(long positionMs) {
        if (videoPlayer != null) {
            videoPlayer.seekTo(positionMs * 1000);
        }
    }

    private void buildMediaSource(Uri uri2) {
        try {
            videoPlayer.addMediaSource(new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(this, getString(R.string.app_name))).createMediaSource(MediaItem.fromUri(uri2)));
            videoPlayer.prepare();
            videoPlayer.setPlayWhenReady(true);
            videoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                    imagePlayPause.setVisibility(playWhenReady ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_IDLE) {
                        LogMessage.v("onPlayerStateChanged: STATE_IDLE.");
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        LogMessage.v("onPlayerStateChanged: STATE_BUFFERING.");
                    } else if (playbackState == Player.STATE_READY) {
                        isVideoEnded = false;
                        startProgress();
                        LogMessage.v("onPlayerStateChanged: Ready to play.");
                    } else if (playbackState == Player.EVENT_PLAYBACK_STATE_CHANGED) {
                        LogMessage.v("onPlayerStateChanged: Video ended.");
                        imagePlayPause.setVisibility(View.VISIBLE);
                        isVideoEnded = true;
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadThumbnails() {
        try {
            long dur = totalDuration / 8;
            int i = 1;
            for (ImageView into : imageViews) {
                long j2 = (long) i;
                Glide.with(this).load(getIntent().getStringExtra(TrimVideo.TRIM_VIDEO_URI)).apply((RequestOptions) new RequestOptions().frame(dur * j2 * 1000000)).transition(DrawableTransitionOptions.withCrossFade(300)).into(into);
                if (j2 < totalDuration) {
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpSeekBar() {
        seekbar.setVisibility(View.VISIBLE);
        txtStartDuration.setVisibility(View.VISIBLE);
        txtEndDuration.setVisibility(View.VISIBLE);
        seekbarController.setMaxValue((float) totalDuration).apply();
        seekbar.setMaxValue((float) totalDuration).apply();
        seekbar.setMaxStartValue((float) totalDuration).apply();
        if (trimType == 1) {
            seekbar.setFixGap((float) fixedGap).apply();
            lastMaxValue = totalDuration;
        } else if (trimType == 2) {
            seekbar.setMaxStartValue((float) minGap);
            seekbar.setGap((float) minGap).apply();
            lastMaxValue = totalDuration;
        } else if (trimType == 3) {
            seekbar.setMaxStartValue((float) maxToGap);
            seekbar.setGap((float) minFromGap).apply();
            lastMaxValue = maxToGap;
        } else {
            seekbar.setGap(2.0f).apply();
            lastMaxValue = totalDuration;
        }
        if (hidePlayerSeek) {
            seekbarController.setVisibility(View.GONE);
        }
        seekbar.setOnRangeSeekbarFinalValueListener((number, number2) -> {
            if (!hidePlayerSeek) {
                seekbarController.setVisibility(View.VISIBLE);
            }
        });
        seekbar.setOnRangeSeekbarChangeListener((minValue, maxValue) -> {
            long minVal = (long) minValue;
            long maxVal = (long) maxValue;
            if (lastMinValue != minVal) {
                seekTo((long) minValue);
                if (!hidePlayerSeek)
                    seekbarController.setVisibility(View.INVISIBLE);
            }
            lastMinValue = minVal;
            lastMaxValue = maxVal;
            txtStartDuration.setText(TrimmerUtils.formatSeconds(minVal));
            txtEndDuration.setText(TrimmerUtils.formatSeconds(maxVal));
            if (trimType == 3)
                setDoneColor(minVal, maxVal);
        });
        seekbarController.setOnSeekbarFinalValueListener(value -> {
            long value1 = (long) value;
            if (value1 < lastMaxValue && value1 > lastMinValue) {
                seekTo(value1);
                return;
            }
            if (value1 > lastMaxValue)
                seekbarController.setMinStartValue((int) lastMaxValue).apply();
            else if (value1 < lastMinValue) {
                seekbarController.setMinStartValue((int) lastMinValue).apply();
                if (videoPlayer.getPlayWhenReady())
                    seekTo(lastMinValue);
            }
        });

    }

    /**
     * will be called whenever seekBar range changes
     * it checks max duration is exceed or not.
     * and disabling and enabling done menuItem
     *
     * @param minVal left thumb value of seekBar
     * @param maxVal right thumb value of seekBar
     */
    private void setDoneColor(long minVal, long maxVal) {
        try {
            if (menuDone == null)
                return;
            //changed value is less than maxDuration
            if ((maxVal - minVal) <= maxToGap) {
                menuDone.getIcon().setColorFilter(
                        new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorWhite)
                                , PorterDuff.Mode.SRC_IN)
                );
                isValidVideo = true;
            } else {
                menuDone.getIcon().setColorFilter(
                        new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorWhiteLt)
                                , PorterDuff.Mode.SRC_IN)
                );
                isValidVideo = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PER_REQ_CODE) {
            if (isPermissionOk(grantResults))
                setDataInView();
            else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoPlayer != null) videoPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoPlayer != null)
            videoPlayer.release();
        if (progressView != null && progressView.isShowing())
            progressView.dismiss();
        deleteFile("temp_file");
        stopRepeatingTask();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuDone = menu.findItem(R.id.action_done);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() != R.id.action_done) {
            return super.onOptionsItemSelected(menuItem);
        }
        if (SystemClock.elapsedRealtime() - lastClickedTime < 800) {
            return true;
        }
        lastClickedTime = SystemClock.elapsedRealtime();
        trimVideo();
        return true;
    }

    private void trimVideo() {
        String[] complexCommand;
        if (isValidVideo) {
            outputPath = getFileName();
            videoPlayer.setPlayWhenReady(false);
            showProcessingDialog();
            if (compressOption != null) {
                complexCommand = getDefaultCmd();
            } else {
                complexCommand = isAccurateCut ? getAccurateCmd() : new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue), "-i", String.valueOf(uri), "-t", TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), "-async", "1", "-strict", "-2", "-c", "copy", outputPath};
            }
            execFFmpegBinary(complexCommand, true);
            return;
        }
        Toast.makeText(this, getString(R.string.txt_smaller) + " " + TrimmerUtils.getLimitedTimeFormatted(maxToGap), Toast.LENGTH_SHORT).show();
    }

    private String getFileName() {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "";
        String destPath = destinationPath;
        if (destPath != null) {
            path = destPath;
        }
        String fname = fileName;
        if (fname != null && !fname.isEmpty()) {
            fname = fileName;
        }
        int i = 0;
        if (fname.indexOf(".") > 0) {
            fname = fname.substring(0, fname.lastIndexOf("."));
        }
        Log.d("EditVideo", "=====================================");
        Log.d("EditVideo", "fileName: " + fileName);
        Log.d("EditVideo", "fName: " + fname);
        File file = new File(path + File.separator + fname + "." + TrimmerUtils.getFileExtension(this, uri));
        String sb = "newFile: " +
                file.getName();
        Log.d("EditVideo", sb);
        while (file.exists()) {
            i++;
            file = new File(path + File.separator + fname + "-" + i + "." + TrimmerUtils.getFileExtension(this, uri));
            String sb2 = "newFile: " +
                    file.getName();
            Log.d("EditVideo", sb2);
        }
        return String.valueOf(file);
    }

    private String[] getDefaultCmd() {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(String.valueOf(uri));
        String extractMetadata = mediaMetadataRetriever.extractMetadata(19);
        String extractMetadata2 = mediaMetadataRetriever.extractMetadata(18);
        int parseInt = TrimmerUtils.clearNull(extractMetadata2).isEmpty() ? 0 : Integer.parseInt(extractMetadata2);
        int parseInt2 = Integer.parseInt(extractMetadata);
        if (compressOption.getWidth() != 0 || compressOption.getHeight() != 0 || !compressOption.getBitRate().equals("0k")) {
            return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue), "-i", String.valueOf(uri), "-s", compressOption.getWidth() + "x" + compressOption.getHeight(), "-r", String.valueOf(compressOption.getFrameRate()), "-vcodec", "mpeg4", "-b:v", compressOption.getBitRate(), "-b:a", "48000", "-ac", ExifInterface.GPS_MEASUREMENT_2D, "-ar", "22050", "-t", TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
        } else if (parseInt >= 800) {
            return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue), "-i", String.valueOf(uri), "-s", (parseInt / 2) + "x" + (Integer.parseInt(extractMetadata) / 2), "-r", "30", "-vcodec", "mpeg4", "-b:v", "1M", "-b:a", "48000", "-ac", ExifInterface.GPS_MEASUREMENT_2D, "-ar", "22050", "-t", TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
        } else {
            return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue), "-i", String.valueOf(uri), "-s", parseInt + "x" + parseInt2, "-r", "30", "-vcodec", "mpeg4", "-b:v", "400K", "-b:a", "48000", "-ac", ExifInterface.GPS_MEASUREMENT_2D, "-ar", "22050", "-t", TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
        }
    }

    private void execFFmpegBinary(String[] command, boolean retry) {
        try {
            FFmpeg.executeAsync(command, (executionId, returnCode) -> trimVideo(retry, executionId, returnCode));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void trimVideo(boolean retry, long executionId, int returnCode) {
        if (returnCode == 0) {
            dialog.dismiss();
            Intent intent = new Intent();
            intent.putExtra(TrimVideo.TRIMMED_VIDEO_PATH, outputPath);
            setResult(-1, intent);
            finish();
        } else if (returnCode == 255) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } else if (!retry || isAccurateCut || compressOption != null) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            runOnUiThread(this::printToast);
        } else {
            File file = new File(outputPath);
            if (file.exists()) {
                deleteFile(file.getAbsolutePath());
            }
            execFFmpegBinary(getAccurateCmd(), false);
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


    public void printToast() {
        Toast.makeText(this, "Failed to trim", Toast.LENGTH_SHORT).show();
    }

    private String[] getAccurateCmd() {
        return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue), "-i", String.valueOf(uri), "-t", TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), "-async", "1", outputPath};
    }

    private void showProcessingDialog() {
        try {
            dialog = new Dialog(this);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.alert_convert);
            dialog.setCancelable(false);
            dialog.getWindow().setLayout(-1, -2);
            ((TextView) dialog.findViewById(R.id.txt_cancel)).setOnClickListener(view -> {
                dialog.dismiss();
                FFmpeg.cancel();
            });
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= 29) {
            return checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.ACCESS_MEDIA_LOCATION");
        }
        return checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE");
    }

    private boolean checkPermission(String... permissions) {
        boolean isGranted = false;
        for (String permission : permissions) {
            isGranted = ContextCompat.checkSelfPermission(this, permission) == 0;
            if (!isGranted) {
                break;
            }
        }
        if (isGranted) {
            return true;
        }
        ActivityCompat.requestPermissions(this, permissions, PER_REQ_CODE);
        return false;
    }

    private boolean isPermissionOk(int... results) {
        for (int i : results) {
            if (i != 0) {
                return false;
            }
        }
        return true;
    }


    public void startProgress() {
        updateSeekbar.run();
    }


    public void stopRepeatingTask() {
        seekHandler.removeCallbacks(updateSeekbar);
    }
}
