package com.fttotal.screenrecorder.ads.formats;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.lifecycle.LifecycleObserver;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.fttotal.screenrecorder.ads.AdsService;
import java.util.Date;

public class OpenAd implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    public static String TAG = "tag_ad_manager";
    public static boolean isShowingAd = false;
    private static OpenAd openAd;

    public AppOpenAd appOpenAd = null;
    private Activity currentActivity;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;

    public long loadTime = 0;
    private Context mContext;

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    public static OpenAd getInstance() {
        if (openAd == null) {
            openAd = new OpenAd();
        }
        return openAd;
    }

    public void initOpenAd(Context context) {
        this.mContext = context;
    }

    public void fetchAd() {
        if (!isAdAvailable()) {
            this.loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            };
            AppOpenAd.load(this.mContext, AdsService.getInstance().setOpenAdId(), new AdRequest.Builder().build(), 1, this.loadCallback);
        }
    }

    public void displayAd(Activity activity, FullScreenContentCallback fullScreenContentCallback) {
        this.appOpenAd.show(activity);
    }

    public boolean isAdAvailable() {
        return this.appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long j) {
        return new Date().getTime() - this.loadTime < j * 3600000;
    }

    public boolean getIsShowingAd() {
        return isShowingAd;
    }

    public void setIsShowingAd(boolean isShowingAd) {
        OpenAd.isShowingAd = isShowingAd;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        this.currentActivity = activity;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        this.currentActivity = activity;
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        this.currentActivity = null;
    }
}
