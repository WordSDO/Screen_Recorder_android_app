package com.fttotal.screenrecorder.videotrimming;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.fttotal.screenrecorder.activities.VideoTrimmerActivity;

import java.util.Objects;

public class TrimVideo {
    public static final String TRIMMED_VIDEO_PATH = "trimmed_video_path";
    public static final String TRIM_VIDEO_OPTION = "trim_video_option";
    public static final String TRIM_VIDEO_URI = "trim_video_uri";
    public static int VIDEO_TRIMMER_REQ_CODE = 324;

    public static ActivityBuilder activity(String activity) {
        return new ActivityBuilder(activity);
    }

    public static String getTrimmedVideoPath(Intent intent) {
        return intent.getStringExtra(TRIMMED_VIDEO_PATH);
    }

    public static final class ActivityBuilder {
        private final TrimVideoOptions options;
        private final String videoUri;

        public ActivityBuilder(String uri) {
            this.videoUri = uri;
            TrimVideoOptions trimVideoOptions = new TrimVideoOptions();
            this.options = trimVideoOptions;
            trimVideoOptions.trimType = TrimType.DEFAULT;
        }

        public ActivityBuilder setTrimType(TrimType trimType) {
            this.options.trimType = trimType;
            return this;
        }

        public ActivityBuilder setHideSeekBar(boolean hideSeekBar) {
            this.options.hideSeekBar = hideSeekBar;
            return this;
        }

        public ActivityBuilder setCompressOption(CompressOption compressOption) {
            this.options.compressOption = compressOption;
            return this;
        }

        public ActivityBuilder setFileName(String fileName) {
            this.options.fileName = fileName;
            return this;
        }

        public ActivityBuilder setAccurateCut(boolean accurateCut) {
            this.options.accurateCut = accurateCut;
            return this;
        }

        public ActivityBuilder setMinDuration(long minDuration) {
            this.options.minDuration = minDuration;
            return this;
        }

        public ActivityBuilder setFixedDuration(long fixedDuration) {
            this.options.fixedDuration = fixedDuration;
            return this;
        }

        public ActivityBuilder setMinToMax(long min, long max) {
            long[] minToMaxArr = {min, max};
            this.options.minToMax = minToMaxArr;
            return this;
        }

        public ActivityBuilder setDestination(String destination) {
            this.options.destination = destination;
            return this;
        }

        public void start(Activity activity) {
            validate();
            activity.startActivityForResult(getIntent(activity), TrimVideo.VIDEO_TRIMMER_REQ_CODE);
        }

        public void start(Fragment fragment) {
            validate();
            fragment.startActivityForResult(getIntent(fragment.getActivity()), TrimVideo.VIDEO_TRIMMER_REQ_CODE);
        }

        private void validate() {
            Objects.requireNonNull(videoUri, "VideoUri cannot be null.");
            if (!videoUri.isEmpty()) {
                Objects.requireNonNull(options.trimType, "TrimType cannot be null");
                if (options.minDuration < 0) {
                    throw new IllegalArgumentException("Cannot set min duration to a number < 1");
                } else if (options.fixedDuration < 0) {
                    throw new IllegalArgumentException("Cannot set fixed duration to a number < 1");
                } else if (options.trimType == TrimType.MIN_MAX_DURATION && options.minToMax == null) {
                    throw new IllegalArgumentException("Used trim type is TrimType.MIN_MAX_DURATION.Give the min and max duration");
                } else if (options.minToMax == null) {
                } else {
                    if (options.minToMax[0] < 0 || options.minToMax[1] < 0) {
                        throw new IllegalArgumentException("Cannot set min to max duration to a number < 1");
                    } else if (options.minToMax[0] > options.minToMax[1]) {
                        throw new IllegalArgumentException("Minimum duration cannot be larger than max duration");
                    } else if (options.minToMax[0] == options.minToMax[1]) {
                        throw new IllegalArgumentException("Minimum duration cannot be same as max duration.Use Fixed duration");
                    }
                }
            } else {
                throw new IllegalArgumentException("VideoUri cannot be empty");
            }
        }

        private Intent getIntent(Activity activity) {
            Intent intent = new Intent(activity, VideoTrimmerActivity.class);
            intent.putExtra(TrimVideo.TRIM_VIDEO_URI, videoUri);
            intent.putExtra(TrimVideo.TRIM_VIDEO_OPTION, options);
            return intent;
        }
    }
}
