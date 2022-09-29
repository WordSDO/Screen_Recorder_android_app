package com.fttotal.screenrecorder.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.activities.ImagePreviewActivity;
import com.fttotal.screenrecorder.helpers.Utils;

import java.util.ArrayList;

public class ScreenshotsAdapter extends RecyclerView.Adapter<ScreenshotsAdapter.ViewHolder> {
    public final int REFRESH__SCREENSHOT_REQUEST = 122;
    private Context mContext;
    private ArrayList<String> mScreenshotsList;

    public ScreenshotsAdapter(Context context, ArrayList<String> arrayList) {
        this.mContext = context;
        this.mScreenshotsList = arrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.screenshot_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        Glide.with(mContext).load(mScreenshotsList.get(position)).into(viewHolder.iv_screenshot);
        viewHolder.iv_screenshot.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, ImagePreviewActivity.class);
            intent.putExtra("path", mScreenshotsList.get(position));
            ((Activity) mContext).startActivityForResult(intent, REFRESH__SCREENSHOT_REQUEST);
        });
        viewHolder.rl_delete.setOnClickListener(view -> Utils.toast(mContext, "Delete clicked", Toast.LENGTH_LONG));
    }

    @Override
    public int getItemCount() {
        return mScreenshotsList.size();
    }

    public void setData(ArrayList<String> arrayList) {
        mScreenshotsList.clear();
        mScreenshotsList.addAll(arrayList);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_screenshot;
        RelativeLayout rl_delete;

        public ViewHolder(View view) {
            super(view);
            iv_screenshot = view.findViewById(R.id.iv_screenshot);
            rl_delete = view.findViewById(R.id.rl_delete);
        }
    }
}
