package com.fttotal.screenrecorder.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.google.android.exoplayer2.util.MimeTypes;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.activities.VideoPlayerActivity;
import com.fttotal.screenrecorder.ads.AdsService;
import com.fttotal.screenrecorder.entities.Recording;
import com.fttotal.screenrecorder.fragments.RecordingFragment;
import com.fttotal.screenrecorder.helpers.Utils;
import com.fttotal.screenrecorder.videotrimming.CompressOption;
import com.fttotal.screenrecorder.videotrimming.TrimVideo;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class RecordingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public Context context;
    private final RecordingFragment fragment;

    public List<Recording> recordingsList;
    private OnClickListener onClickListener;

    public interface OnClickListener {
        void onRecyItemClick(int position);
    }

    public RecordingsAdapter(Context context2, List<Recording> list, RecordingFragment recordingFragment) {
        this.recordingsList = list;
        this.context = context2;
        this.fragment = recordingFragment;

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater from = LayoutInflater.from(viewGroup.getContext());
        if (viewType == -1) {
            viewHolder = new NativeAdViewHolder(from.inflate(R.layout.admob_list_item, viewGroup, false));
        } else if (viewType != 1) {
            return null;
        } else {
            viewHolder = new RecordingHolder(from.inflate(R.layout.recording_list_item, viewGroup, false));
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        Recording recording = recordingsList.get(position);
        int itemViewType = viewHolder.getItemViewType();
        if (itemViewType == -1) {
            NativeAdViewHolder nativeAdViewHolder = (NativeAdViewHolder) viewHolder;
            if (nativeAdViewHolder.ad_frame.getTag() == null) {
                nativeAdViewHolder.ad_frame.setTag("1");
                AdsService.getInstance().showNativeAd(nativeAdViewHolder.ad_frame, R.layout.admob_native_ad, AdsService.NativeAdType.NATIVE_AD_TYPE_MEDIUM);
            }
        } else if (itemViewType == 1) {
            final RecordingHolder recordingHolder = (RecordingHolder) viewHolder;
            recordingHolder.mTitle.setText(recording.getTitle());
            recordingHolder.mDateTime.setText(recording.getFormattedDate());
            recordingHolder.mDuration.setText(recording.getDuration());
            recordingHolder.mSize.setText(recording.getSize());
            ((RequestBuilder) Glide.with(context).load(recording.getPath()).placeholder(R.drawable.ic_placeholder)).into(recordingHolder.mThumbnail);
            recordingHolder.mCardView.setOnClickListener(view -> {
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("UriString", Uri.parse(recordingsList.get(position).getPath()).toString());
                context.startActivity(intent);
            });

           /* recordingHolder.mainCard.setBackgroundColor(recording.isSelected() ? context.getColor(R.color.textColor) : context.getColor(R.color.listBgColor));
            recordingHolder.mainCard.setOnClickListener(view -> {
                recording.setSelected(!recording.isSelected());
                recordingHolder.mainCard.setBackgroundColor(recording.isSelected() ? context.getColor(R.color.textColor) : context.getColor(R.color.listBgColor));

            });*/

            recordingHolder.mMoreMenu.setOnClickListener(view -> {

                //init the popup
                PopupMenu popup = new PopupMenu(context, recordingHolder.mMoreMenu);

                try {
                    Field[] fields = popup.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        if ("mPopup".equals(field.getName())) {
                            field.setAccessible(true);
                            Object menuPopupHelper = field.get(popup);
                            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                            Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                            setForceIcons.invoke(menuPopupHelper, true);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                popup.setOnMenuItemClickListener(menuItem -> {
                    int itemId = menuItem.getItemId();
                    if (itemId == R.id.delete_item) {
                        showDeleteDialog(position);
                        return true;
                    } else if (itemId == R.id.edit_item) {
                        String directoryPath = getDirectoryPath();
                        Log.d("EditVideo", "Trim Path: " + directoryPath);
                        TrimVideo.activity(recordingsList.get(position).getPath()).setCompressOption(new CompressOption()).setDestination(directoryPath).setFileName(((Recording) recordingsList.get(position)).getTitle()).start((Activity) context);
                        return true;
                    } else if (itemId != R.id.share_item) {
                        return false;
                    } else {
                        Intent intent = new Intent("android.intent.action.SEND");
                        intent.setType(MimeTypes.VIDEO_MP4);
                        intent.putExtra("android.intent.extra.STREAM", Uri.parse(((Recording) recordingsList.get(position)).getPath()));
                        intent.putExtra("android.intent.extra.TEXT", "");
                        context.startActivity(Intent.createChooser(intent, "Share Video"));
                        return true;
                    }
                });
                popup.setGravity(5);
                popup.inflate(R.menu.popup_menu);
                popup.show();
            });
        }


    }

    public String getDirectoryPath() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), Utils.VIDEO_DIRECTORY_NAME);
        return file.exists() ? file.getAbsolutePath() : "";
    }

    public void setOnClickListener(OnClickListener listener) {
        this.onClickListener = listener;
    }

    public void showDeleteDialog(final int pos) {
        new AlertDialog.Builder(context, R.style.MyDialogStyle)
                .setTitle(context.getResources().getString(R.string.caution))
                .setMessage(context.getResources().getString(R.string.deletingMsg))
                .setPositiveButton(context.getResources().getString(R.string.yes), (dialogInterface, i) -> {
                    if (onClickListener != null) onClickListener.onRecyItemClick(pos);
                })
                .setNegativeButton(context.getResources().getString(R.string.no), (dialogInterface, i) -> {
                }).show();
    }


    @Override
    public int getItemCount() {
        return recordingsList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return recordingsList.get(position).getTitle() == null ? -1 : 1;
    }

    public void setData(List<Recording> list) {
        recordingsList.clear();
        recordingsList.addAll(list);
        notifyDataSetChanged();
    }

    public class RecordingHolder extends RecyclerView.ViewHolder {
        private final CardView mCardView;
        private final TextView mDateTime;
        private final TextView mDuration;
        private final ImageView mMoreMenu;
        private final TextView mSize;
        private final ImageView mThumbnail;
        private final TextView mTitle;
        private final CardView mainCard;

        public RecordingHolder(View view) {
            super(view);
            mThumbnail = view.findViewById(R.id.iv_thumbnail);
            mTitle = view.findViewById(R.id.tv_title);
            mDateTime = view.findViewById(R.id.tv_date_time);
            mDuration = view.findViewById(R.id.tv_duration);
            mSize = view.findViewById(R.id.tv_size);
            mMoreMenu = view.findViewById(R.id.button_more);
            mCardView = view.findViewById(R.id.layout_thumbnail);
            mainCard = view.findViewById(R.id.mainCard);
        }
    }

    public class NativeAdViewHolder extends RecyclerView.ViewHolder {

        public FrameLayout ad_frame;

        public NativeAdViewHolder(View view) {
            super(view);
            context = view.getContext();
            ad_frame = view.findViewById(R.id.layout_native_ad);
        }
    }
}
