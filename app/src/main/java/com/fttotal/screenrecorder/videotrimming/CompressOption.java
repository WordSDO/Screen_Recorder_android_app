package com.fttotal.screenrecorder.videotrimming;

import android.os.Parcel;
import android.os.Parcelable;

public class CompressOption implements Parcelable {
    public static final Parcelable.Creator<CompressOption> CREATOR = new Parcelable.Creator<CompressOption>() {
        public CompressOption createFromParcel(Parcel parcel) {
            return new CompressOption(parcel);
        }

        public CompressOption[] newArray(int i) {
            return new CompressOption[i];
        }
    };
    private String bitRate = "0k";
    private int frameRate = 30;
    private int height = 0;
    private int width = 0;

    public int describeContents() {
        return 0;
    }

    public CompressOption() {
    }

    public CompressOption(int frameRate, String bitRate, int width, int height) {
        this.frameRate = frameRate;
        this.bitRate = bitRate;
        this.width = width;
        this.height = height;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public void setBitRate(String bitRate) {
        this.bitRate = bitRate;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFrameRate() {
        return this.frameRate;
    }

    public String getBitRate() {
        return this.bitRate;
    }

    public static Parcelable.Creator<CompressOption> getCREATOR() {
        return CREATOR;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(frameRate);
        parcel.writeString(bitRate);
        parcel.writeInt(width);
        parcel.writeInt(height);
    }

    protected CompressOption(Parcel parcel) {
        frameRate = parcel.readInt();
        bitRate = parcel.readString();
        width = parcel.readInt();
        height = parcel.readInt();
    }
}
