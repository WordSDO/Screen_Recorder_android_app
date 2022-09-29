package com.fttotal.screenrecorder.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.extractor.ts.TsExtractor;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.adapters.ScreenshotsAdapter;
import com.fttotal.screenrecorder.helpers.PermissionsUtil;
import com.fttotal.screenrecorder.helpers.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScreenshotsFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "ScreenshotsFragment";
    public final String[] EXTERNAL_PERMS = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    public final int EXTERNAL_REQUEST = TsExtractor.TS_STREAM_TYPE_DTS;
    ScreenshotsAdapter mScreenshotsAdapter;
    ArrayList<String> pathList = new ArrayList<>();
    ArrayList<String> pathListWithAds = new ArrayList<>();

    private ImageView button_permission;
    private RecyclerView rv_screenshots;
    private TextView tv_no_permission;
    private LinearLayout layout_no_screenshots;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_screenshots, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        button_permission = view.findViewById(R.id.button_permission);
        rv_screenshots = view.findViewById(R.id.rv_screenshots);
        tv_no_permission = view.findViewById(R.id.tv_no_permission);
        layout_no_screenshots = view.findViewById(R.id.layout_no_screenshots);

        button_permission.setOnClickListener(this);
        Log.d(TAG, "Read Permission: " + PermissionsUtil.checkReadStoragePermission());
        Log.d(TAG, "Write Permission: " + PermissionsUtil.checkWriteStoragePermission());
        rv_screenshots.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        mScreenshotsAdapter = new ScreenshotsAdapter(getActivity(), new ArrayList());
        rv_screenshots.setAdapter(this.mScreenshotsAdapter);
        setViews();
    }

    private void setViews() {
        if (!PermissionsUtil.checkReadStoragePermission()) {
            showPermissionViews();
        } else {
            getAllScreenshots();
        }
    }

    private void showPermissionViews() {
        if (tv_no_permission != null) tv_no_permission.setVisibility(View.VISIBLE);
        if (button_permission != null) button_permission.setVisibility(View.VISIBLE);
    }

    private void hidePermissionViews() {
        if (tv_no_permission != null) tv_no_permission.setVisibility(View.GONE);
        if (button_permission != null) button_permission.setVisibility(View.GONE);
    }

    private void getAllScreenshots() {
        pathList.clear();
        hidePermissionViews();
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), Utils.VIDEO_DIRECTORY_NAME);
        if (file.exists() && file.isDirectory()) {
            for (File file2 : Objects.requireNonNull(file.listFiles())) {
                if (!file2.isDirectory()) {
                    pathList.add(file2.getPath());
                }
            }
        }
        if (pathList.size() > 0) {
            if (layout_no_screenshots != null) layout_no_screenshots.setVisibility(View.GONE);
            if (rv_screenshots != null) rv_screenshots.setVisibility(View.VISIBLE);
            if (mScreenshotsAdapter != null) mScreenshotsAdapter.setData(pathList);
            return;
        }
        if (rv_screenshots != null) rv_screenshots.setVisibility(View.GONE);
        if (layout_no_screenshots != null) layout_no_screenshots.setVisibility(View.VISIBLE);
        if (tv_no_permission != null) tv_no_permission.setVisibility(View.GONE);
        if (button_permission != null) button_permission.setVisibility(View.GONE);
    }

    private void checkAndRequestPermission() {
        Dexter.withContext(getActivity()).withPermissions(Utils.storagePermissionList).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                if (multiplePermissionsReport != null) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        getAllScreenshots();
                    } else if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                        Utils.showPermissionAlert(getActivity(), getString(R.string.msg_for_storge_alert));
                    } else {
                        Toast.makeText(getActivity(), "Required Permissions not granted", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).withErrorListener(dexterError -> Log.e("Error ", dexterError.name())).check();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_permission) {
            checkAndRequestPermission();
        }
    }

    public void refreshScreenshots() {
        if (!PermissionsUtil.checkReadStoragePermission() && !PermissionsUtil.checkWriteStoragePermission()) {
            showPermissionViews();
        } else {
            getAllScreenshots();
        }
    }
}
