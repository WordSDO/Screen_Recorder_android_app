package com.fttotal.screenrecorder.videotrimming;

import android.os.Parcel;
import android.os.Parcelable;

public class TrimVideoOptions implements Parcelable {
    public static final Parcelable.Creator<TrimVideoOptions> CREATOR = new Parcelable.Creator<TrimVideoOptions>() {
        @Override
        public TrimVideoOptions createFromParcel(Parcel parcel) {
            return new TrimVideoOptions(parcel);
        }

        @Override
        public TrimVideoOptions[] newArray(int i) {
            return new TrimVideoOptions[i];
        }
    };
    public boolean accurateCut;
    public CompressOption compressOption;
    public String destination;
    public String fileName;
    public long fixedDuration;
    public boolean hideSeekBar;
    public long minDuration;
    public long[] minToMax;
    public TrimType trimType = TrimType.DEFAULT;

    public int describeContents() {
        return 0;
    }

    public TrimVideoOptions() {
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(destination);
        parcel.writeString(fileName);
        parcel.writeInt(trimType == null ? -1 : trimType.ordinal());
        parcel.writeLong(minDuration);
        parcel.writeLong(fixedDuration);
        parcel.writeByte(hideSeekBar ? (byte) 1 : 0);
        parcel.writeByte(accurateCut ? (byte) 1 : 0);
        parcel.writeLongArray(minToMax);
        parcel.writeParcelable(compressOption, i);
    }

    protected TrimVideoOptions(Parcel parcel) {
        TrimType trimType2;
        destination = parcel.readString();
        fileName = parcel.readString();
        int readInt = parcel.readInt();
        if (readInt == -1) {
            trimType2 = null;
        } else {
            trimType2 = TrimType.values()[readInt];
        }
        trimType = trimType2;
        minDuration = parcel.readLong();
        fixedDuration = parcel.readLong();
        hideSeekBar = parcel.readByte() != 0;
        accurateCut = parcel.readByte() == 0 ? false : true;
        minToMax = parcel.createLongArray();
        compressOption = (CompressOption) parcel.readParcelable(CompressOption.class.getClassLoader());
    }
}
