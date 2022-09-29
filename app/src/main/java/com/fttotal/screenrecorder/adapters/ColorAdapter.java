package com.fttotal.screenrecorder.adapters;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.fttotal.screenrecorder.R;

import java.util.ArrayList;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> {

    public ArrayList<Integer> colors;
    private Context context;

    public int itemCheck = 0;

    public OnClick onClick;

    public interface OnClick {
        void onClickColor(int i);
    }

    public ColorAdapter(Context context2, ArrayList<Integer> arrayList, OnClick onClick2) {
        this.colors = arrayList;
        this.context = context2;
        this.onClick = onClick2;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate(R.layout.item_color, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        Drawable background = viewHolder.imv_color.getBackground();
        if (background instanceof ShapeDrawable) {
            ((ShapeDrawable) background).getPaint().setColor(colors.get(i));
        } else if (background instanceof GradientDrawable) {
            ((GradientDrawable) background).setColor(colors.get(i));
        } else if (background instanceof ColorDrawable) {
            ((ColorDrawable) background).setColor(colors.get(i));
        }
        if (itemCheck == colors.get(i)) {
            viewHolder.imv_check.setVisibility(View.VISIBLE);
        } else {
            viewHolder.imv_check.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return this.colors.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imv_check;

        public ImageView imv_color;

        ViewHolder(View view) {
            super(view);
            imv_color = view.findViewById(R.id.imv_color);
            imv_check = view.findViewById(R.id.imv_check);
            itemView.setOnClickListener(view1 -> {
                itemCheck = colors.get(getAbsoluteAdapterPosition());
                onClick.onClickColor(colors.get(getAbsoluteAdapterPosition()));
                notifyDataSetChanged();
            });
        }
    }
}
