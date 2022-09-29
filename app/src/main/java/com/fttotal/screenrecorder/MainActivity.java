package com.fttotal.screenrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.fttotal.screenrecorder.activities.SettingsActivity;
import com.fttotal.screenrecorder.adapters.ViewPagerAdapter;
import com.fttotal.screenrecorder.fragments.RecordingFragment;
import com.fttotal.screenrecorder.fragments.ScreenshotsFragment;
import com.fttotal.screenrecorder.helpers.NonSwipeableViewPager;
import com.fttotal.screenrecorder.helpers.Utils;
import com.fttotal.screenrecorder.services.HBService;
import com.fttotal.screenrecorder.videotrimming.TrimVideo;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 23;
    public final int REFRESH__SCREENSHOT_REQUEST = 122;
    private boolean hasPermissions = false;
    private boolean isDrawOverlyAllowed = false;
    private boolean isVideoRecorded = false;
    private Intent mScreenCaptureIntent = null;

    private final ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            resetSelectedTab(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
    private RecordingFragment recordingFragment;
    private ScreenshotsFragment screenshotsFragment;

    public int selectedTab = 0;
    private ChipNavigationBar bottom_navigation_main;
    private NavigationView drawer_navigation_view;
    private RelativeLayout drawer_menu_button;
    private ImageView iv_record;
    private DrawerLayout drawer_layout;
    private NonSwipeableViewPager viewPager;
    private TextView tv_header_title, tv_storage;

    private LinearLayout feedbackLayout, tellFriendsLayout, moreAppsLayout, privacyPolicyLayout;

    private static final int PERMISSION_RECORD_DISPLAY = 3006;
    private static final int CUSTOM_OVERLAY_PERMISSION_REQUEST_CODE = 101;
    private int mScreenCaptureResultCode = Utils.RESULT_CODE_FAILED;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        feedbackLayout = findViewById(R.id.feedbackLayout);
        tellFriendsLayout = findViewById(R.id.tellFriendsLayout);
        moreAppsLayout = findViewById(R.id.moreAppsLayout);
        privacyPolicyLayout = findViewById(R.id.privacyPolicyLayout);

        bottom_navigation_main = findViewById(R.id.bottom_navigation_main);
        drawer_navigation_view = findViewById(R.id.drawer_navigation_view);
        drawer_menu_button = findViewById(R.id.drawer_menu_button);
        iv_record = findViewById(R.id.iv_record);
        drawer_layout = findViewById(R.id.drawer_layout);
        viewPager = findViewById(R.id.viewPager);
        tv_header_title = findViewById(R.id.tv_header_title);
        tv_storage = findViewById(R.id.tv_storage);

        bottom_navigation_main.setOnItemSelectedListener(i -> {
            switch (i) {
                case R.id.nav_photos:
                    viewPager.setCurrentItem(1);
                    selectedTab = 1;
                    return;
                case R.id.nav_settings:
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return;
                case R.id.nav_videos:
                    viewPager.setCurrentItem(0);
                    selectedTab = 0;
                    return;
                default:
                    return;
            }
        });
        drawer_navigation_view = findViewById(R.id.drawer_navigation_view);

        feedbackLayout.setOnClickListener(this);
        tellFriendsLayout.setOnClickListener(this);
        moreAppsLayout.setOnClickListener(this);
        privacyPolicyLayout.setOnClickListener(this);
        drawer_menu_button.setOnClickListener(this);
        iv_record.setOnClickListener(this);

        double freeSpace = (double) new File(getApplicationContext().getFilesDir().getAbsoluteFile().toString()).getFreeSpace();
        Log.d("storage", "availableSize: " + freeSpace);
        Log.d("storage", "Storage: " + String.format("%.0f", freeSpace / 1.073741824E9d));
        Log.d("storage", "========================================");
        Intent intent = getIntent();
        if (intent != null) {
            handleIncomingRequest(intent);
        }
        checkDrawOverlyPermission();
        loadViewPager();

    }


    private void handleIncomingRequest(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Objects.requireNonNull(action);
            action.hashCode();
            if (action.equals(Utils.ACTION_VIDEO_RECORDED)) {
                isVideoRecorded = true;
            }
        }
    }


    public void loadViewPager() {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        recordingFragment = new RecordingFragment(isVideoRecorded);
        screenshotsFragment = new ScreenshotsFragment();
        viewPagerAdapter.addFragment(recordingFragment, "Videos");
        viewPagerAdapter.addFragment(screenshotsFragment, "Screenshots");
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.addOnPageChangeListener(onPageChangeListener);
        // bottom_navigation_main.getMenu().findItem(R.id.nav_hidden_option).setEnabled(false);
    }


    public void resetSelectedTab(int position) {
        if (position == 0) {
            bottom_navigation_main.setItemSelected(R.id.nav_videos, true);
            // bottom_navigation_main.getMenu().findItem(R.id.nav_videos).setChecked(true);
            tv_header_title.setText(getString(R.string.videos));
        } else if (position == 1) {
            bottom_navigation_main.setItemSelected(R.id.nav_photos, true);
            //bottom_navigation_main.getMenu().findItem(R.id.nav_photos).setChecked(true);
            tv_header_title.setText(getString(R.string.screenshots));
            if (screenshotsFragment != null) {
                screenshotsFragment.refreshScreenshots();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resetSelectedTab(selectedTab);
        getRemainingStorage();
    }


    public long getInternalAvailableSpace(String name) {
        File file;
        if (name.equalsIgnoreCase("sdcard")) {
            file = Environment.getExternalStorageDirectory();
        } else {
            file = Environment.getDataDirectory();
        }
        StatFs statFs = new StatFs(file.getAbsolutePath());
        return statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong();
    }


    public void getRemainingStorage() {
        if (checkSDCardAvailable(this)) {
            Log.d("storage", "SD Card Available");
            Log.d("storage", "SD Card: " + convertBytes(getInternalAvailableSpace("sdcard")));
        }
        long internalAvailableSpace = 0 + getInternalAvailableSpace("");
        Log.d("storage", "Internal: " + convertBytes(internalAvailableSpace));
        tv_storage.setText(convertBytes(getInternalAvailableSpace("")));
    }

    public static boolean checkSDCardAvailable(Activity activity) {
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(activity, null);
        return externalFilesDirs.length > 1 && externalFilesDirs[0] != null && externalFilesDirs[1] != null;
    }


    public static String convertBytes(long j) {
        int i = (Long.compare(j, PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID));
        if (i < 0) {
            return floatForm((double) j) + " byte";
        } else if (j < PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED) {
            return floatForm(((double) j) / ((double) PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)) + " KB";
        } else if (j < 1073741824) {
            return floatForm(((double) j) / ((double) PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED)) + " MB";
        } else if (j < 1099511627776L) {
            return floatForm(((double) j) / ((double) 1073741824)) + " GB";
        } else if (j < 1125899906842624L) {
            return floatForm(((double) j) / ((double) 1099511627776L)) + " TB";
        } else if (j < 1152921504606846976L) {
            return floatForm(((double) j) / ((double) 1125899906842624L)) + " PB";
        } else {
            return floatForm(((double) j) / ((double) 1152921504606846976L)) + " EB";
        }
    }

    public static String floatForm(double d) {
        return new DecimalFormat("#.##").format(d);
    }

    public void startRecording() {
        if (checkSelfPermission("android.permission.RECORD_AUDIO", PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE", PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
            hasPermissions = true;
        }
        if (!hasPermissions) {
            return;
        }
        if (isDrawOverlyAllowed) {
            startActivityForResult(((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE)).createScreenCaptureIntent(), PERMISSION_RECORD_DISPLAY);
            return;
        }
        startActivityForResult(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName())), CUSTOM_OVERLAY_PERMISSION_REQUEST_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PERMISSION_RECORD_DISPLAY) {
            if (resultCode != -1) {
                Utils.showSnackBarNotification(iv_record, getString(R.string.recordingPermissionNotGranted), -1);
                mScreenCaptureIntent = null;
                return;
            }
            mScreenCaptureIntent = intent;
            intent.putExtra(Utils.SCREEN_CAPTURE_INTENT_RESULT_CODE, resultCode);
            mScreenCaptureResultCode = resultCode;
            startFloatingService2();
        } else if (requestCode == CUSTOM_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    isDrawOverlyAllowed = false;
                    Utils.toast(getApplicationContext(), getString(R.string.drwaerOverlayPermission), Toast.LENGTH_LONG);
                    return;
                }
            }
            isDrawOverlyAllowed = true;
            startRecording();
        } else if (requestCode == TrimVideo.VIDEO_TRIMMER_REQ_CODE && intent != null) {
            Log.d("EditVideo", "onActivityResult() after trimming");
            if (recordingFragment != null) {
                Utils.refreshSystemGallery(TrimVideo.getTrimmedVideoPath(intent));
                Utils.toast(this, getString(R.string.trimming_video_suceess), Toast.LENGTH_LONG);
                recordingFragment.refreshRecordings();
            }
        } else if (requestCode != REFRESH__SCREENSHOT_REQUEST) {
            super.onActivityResult(requestCode, resultCode, intent);
        } else if (resultCode == -1 && screenshotsFragment != null) {
            screenshotsFragment.refreshScreenshots();
        }
    }

    private void startFloatingService2() {
        Intent intent = new Intent(this, HBService.class);
        intent.putExtra("android.intent.extra.INTENT", mScreenCaptureIntent);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        finish();
    }


    private void checkDrawOverlyPermission() {
        isDrawOverlyAllowed = Settings.canDrawOverlays(this);
    }


    private boolean checkSelfPermission(String permission, int reqCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == 0) {
            return true;
        }
        ActivityCompat.requestPermissions(this, new String[]{permission}, reqCode);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQ_ID_RECORD_AUDIO) {
            if (requestCode == PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE) {
                if (grantResults[0] == 0) {
                    hasPermissions = true;
                    startRecording();
                    return;
                }
                hasPermissions = false;
                Utils.toast(getApplicationContext(), "No permission for android.permission.WRITE_EXTERNAL_STORAGE", 1);
            }
        } else if (grantResults[0] == 0) {
            checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE", PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
        } else {
            hasPermissions = false;
            Utils.toast(getApplicationContext(), "No permission for android.permission.RECORD_AUDIO", 1);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void sendFeedback() {
        Intent intent = new Intent("android.intent.action.SENDTO");
        intent.setData(Uri.parse("mailto:" + getString(R.string.feedback_mail_id)));
        intent.putExtra("android.intent.extra.SUBJECT", "");
        startActivity(Intent.createChooser(intent, "Send feedback"));
    }

    public void tellFriends() {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.TEXT", getString(R.string.shareApp) + "\nDownload\nhttps://play.google.com/store/apps/details?id=" + getPackageName());
        intent.putExtra("android.intent.extra.SUBJECT", getString(R.string.app_name));
        intent.putExtra("android.intent.extra.TITLE", getString(R.string.app_name));
        intent.setType("text/plain");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, "Share"));
    }

    public void moreApps() {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/developer?id=" + getString(R.string.more_app_id)));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(intent);
    }

    public void privacyPolicy() {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(getString(R.string.privacy_policy_link)));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.feedbackLayout:
                sendFeedback();
                break;
            case R.id.tellFriendsLayout:
                tellFriends();
                break;
            case R.id.moreAppsLayout:
                moreApps();
                break;
            case R.id.privacyPolicyLayout:
                privacyPolicy();
                break;
            case R.id.drawer_menu_button:
                drawer_layout.openDrawer(GravityCompat.START);
                break;
            case R.id.iv_record:
                startRecording();
                break;
        }
    }
}
