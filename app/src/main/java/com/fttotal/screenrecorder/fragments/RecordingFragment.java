package com.fttotal.screenrecorder.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.adapters.RecordingsAdapter;
import com.fttotal.screenrecorder.entities.Recording;
import com.fttotal.screenrecorder.helpers.PermissionsUtil;
import com.fttotal.screenrecorder.helpers.Utils;
import com.fttotal.screenrecorder.videotrimming.CompressOption;
import com.fttotal.screenrecorder.videotrimming.TrimVideo;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecordingFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "RecordingsFragment";
    public final String[] EXTERNAL_PERMS = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    public final int PERMISSION_REQUEST_CODE = 143;
    Recording latestRecording;
    List<Recording> mRecordingsList;
    List<Recording> recordings = new ArrayList();
    RecordingsAdapter recordingsAdapter;
    private boolean showVideoRecordingDialog = false;

    private RecyclerView rv_recordings;
    private ImageView button_recording_permission;
    private LinearLayout layout_no_recordings;
    private TextView tv_no_permission;
    private ActivityResultLauncher<IntentSenderRequest> resolveLauncherFriendsConsent;
    private static TextView txtDeleteTitle;
    private static RelativeLayout topDeleteLayout;
    public ImageView imgDelete;

    public RecordingFragment() {
    }

    public RecordingFragment(boolean isVideoRecorded) {
        showVideoRecordingDialog = isVideoRecorded;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_recordings, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        imgDelete = view.findViewById(R.id.imgDelete);
        txtDeleteTitle = view.findViewById(R.id.txtDeleteTitle);
        topDeleteLayout = view.findViewById(R.id.topDeleteLayout);
        rv_recordings = view.findViewById(R.id.rv_recordings);
        button_recording_permission = view.findViewById(R.id.button_recording_permission);
        layout_no_recordings = view.findViewById(R.id.layout_no_recordings);
        tv_no_permission = view.findViewById(R.id.tv_no_permission);

        rv_recordings.setLayoutManager(new LinearLayoutManager(getActivity()));
        recordingsAdapter = new RecordingsAdapter(getActivity(), recordings, this);
        rv_recordings.setAdapter(recordingsAdapter);
        recordingsAdapter.setOnClickListener(position -> {
            File file = new File(recordings.get(position).getPath());
            deletFile(file.getAbsolutePath());
        });

        button_recording_permission.setOnClickListener(this);
        imgDelete.setOnClickListener(this);

        mRecordingsList = new ArrayList();

        if (!PermissionsUtil.checkReadStoragePermission()) {
            layout_no_recordings.setVisibility(View.VISIBLE);
            rv_recordings.setVisibility(View.GONE);
        } else {
            layout_no_recordings.setVisibility(View.GONE);
            rv_recordings.setVisibility(View.VISIBLE);
            getAllRecordings();
        }
        if (showVideoRecordingDialog && mRecordingsList.size() > 0) {
            showVideoCompleteDialog();
        }


        resolveLauncherFriendsConsent =
                registerForActivityResult(
                        new ActivityResultContracts.StartIntentSenderForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                refreshRecordings();
                                Utils.toast(getActivity(), getActivity().getResources().getString(R.string.recordingDeleted), Toast.LENGTH_LONG);
                            } else {

                            }
                        });
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void deletFile(String fileName) {
        String[] selectionArgs = {fileName};
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getActivity().getContentResolver();
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
    }

    public void delete(ActivityResultLauncher<IntentSenderRequest> launcher, Uri uri) {

        ContentResolver contentResolver = requireContext().getContentResolver();

        try {

            //delete object using resolver
            int del = contentResolver.delete(uri, null, null);
            if (del > 0) {
                refreshRecordings();
                Utils.toast(getActivity(), getActivity().getResources().getString(R.string.recordingDeleted), Toast.LENGTH_LONG);
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

    public void getAllRecordings() {
        mRecordingsList.clear();
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), Utils.VIDEO_DIRECTORY_NAME);
        if (file.exists() && file.isDirectory()) {
            List<File> asList = Arrays.asList(file.listFiles());
            Collections.sort(asList, (file1, file2) -> {
                int i = ((file1.lastModified() - file2.lastModified()) > 0 ? 1 : ((file1.lastModified() - file2.lastModified()) == 0 ? 0 : -1));
                if (i > 0) {
                    return 1;
                }
                return i == 0 ? 0 : -1;
            });
            for (File file2 : asList) {
                if (!file2.isDirectory() && Utils.getVideoDuration(file2) > 0) {
                    Recording recording = new Recording();
                    recording.setPath(file2.getAbsolutePath());
                    recording.setTitle(file2.getName());
                    recording.setDuration(millisecondsToTime(Utils.getVideoDuration(file2)));
                    recording.setSize(getFileSize(file2.length()));
                    recording.setFormattedDate(Utils.getFormattedDate(file2.lastModified()));
                    recording.setCreatedDate(String.valueOf(file2.lastModified()));
                    recording.setWidth(Utils.getVideoWidth(file2));
                    recording.setHeight(Utils.getVideoHeight(file2));
                    mRecordingsList.add(recording);
                }
            }
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < mRecordingsList.size(); i++) {
                if (i > 0 && i % 5 == 0) {
                    arrayList.add(new Recording());
                }
                arrayList.add(mRecordingsList.get(i));
            }
            mRecordingsList.clear();
            mRecordingsList.addAll(arrayList);
        }
        if (mRecordingsList.size() > 0) {
            recordingsAdapter.setData(mRecordingsList);
            rv_recordings.setVisibility(View.VISIBLE);
            layout_no_recordings.setVisibility(View.GONE);
            return;
        }
        rv_recordings.setVisibility(View.GONE);
        tv_no_permission.setVisibility(View.GONE);
        button_recording_permission.setVisibility(View.GONE);
        layout_no_recordings.setVisibility(View.VISIBLE);
    }

    private String millisecondsToTime(long j) {
        String time;
        long j2 = j / 1000;
        long j3 = j2 / 60;
        String l = Long.toString(j2 % 60);
        if (l.length() >= 2) {
            time = l.substring(0, 2);
        } else {
            time = "0" + l;
        }
        return j3 + ":" + time;
    }

    public static String getFileSize(long j) {
        if (j <= 0) {
            return "0";
        }
        double d = (double) j;
        int log10 = (int) (Math.log10(d) / Math.log10(1024.0d));
        return new DecimalFormat("#,##0.#").format(d / Math.pow(1024.0d, (double) log10)) + " " + new String[]{"B", "KB", "MB", "GB", "TB"}[log10];
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_recording_permission) {
            checkStoragePermission();
        } else if (view.getId() == R.id.imgDelete) {
            deleteSelectedItems();
        }
    }


    private void deleteSelectedItems() {
        if (mRecordingsList != null) {
            for (int i = 0; i < mRecordingsList.size(); i++) {
                if (mRecordingsList.get(i).isSelected()) {
                    Log.d("testingTAG", String.valueOf(i));
                    mRecordingsList.remove(i);
                    recordingsAdapter.notifyItemRemoved(i);
                    recordingsAdapter.notifyItemRangeChanged(i, mRecordingsList.size());
                    i--;
                }
            }
        }

    }

    private void checkStoragePermission() {
        Dexter.withContext(getActivity()).withPermissions(Utils.storagePermissionList).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                if (multiplePermissionsReport != null) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        getAllRecordings();
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


    private void showVideoCompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View inflate = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_video_complete, requireActivity().findViewById(android.R.id.content), false);
        inflate.setBackgroundResource(android.R.color.transparent);
        builder.setView(inflate);
        final AlertDialog create = builder.create();

        create.setCancelable(false);
        List<Recording> list = mRecordingsList;
        latestRecording = list.get(list.size() - 1);
        final VideoView videoView = inflate.findViewById(R.id.video_view);
        final ImageView button = inflate.findViewById(R.id.button_play);
        videoView.setVideoURI(Uri.parse(latestRecording.getPath()));
        videoView.seekTo(1);
        button.setOnClickListener(view -> {
            button.setVisibility(View.INVISIBLE);
            videoView.start();
        });
        videoView.setOnCompletionListener(mediaPlayer -> button.setVisibility(View.VISIBLE));
        inflate.findViewById(R.id.button_close).setOnClickListener(view -> create.dismiss());
        inflate.findViewById(R.id.button_edit_video).setOnClickListener(view -> {
            String directoryPath = getDirectoryPath();
            Log.d("EditVideo", "path: " + directoryPath);
            TrimVideo.activity(latestRecording.getPath()).setCompressOption(new CompressOption()).setDestination(directoryPath).setFileName(latestRecording.getTitle()).start((Activity) requireActivity());
            create.dismiss();
        });
        inflate.findViewById(R.id.button_delete).setOnClickListener(view -> new AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getResources().getString(R.string.caution))
                .setMessage(getActivity().getResources().getString(R.string.deletingMsg))
                .setPositiveButton(getActivity().getResources().getString(R.string.yes), (dialogInterface, i) -> {
                    File file = new File(latestRecording.getPath());
                    deletFile(file.getAbsolutePath());
                    create.dismiss();
                }).setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                }).show());
        create.show();
    }

    public String getDirectoryPath() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), Utils.VIDEO_DIRECTORY_NAME);
        return file.exists() ? file.getAbsolutePath() : "";
    }

    public void refreshRecordings() {
        getAllRecordings();
    }
}
