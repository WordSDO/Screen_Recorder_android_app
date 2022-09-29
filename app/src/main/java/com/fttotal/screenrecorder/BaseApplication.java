package com.fttotal.screenrecorder;

import android.app.Application;
import android.content.Context;

import com.fttotal.screenrecorder.ads.AdsManagerConfiguration;
import com.fttotal.screenrecorder.ads.AdsService;
import com.fttotal.screenrecorder.helpers.Utils;
import com.fttotal.screenrecorder.managers.SharedPreferencesManager;

public class BaseApplication extends Application {
    public static Context context;
    private static BaseApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        context = this;
        AdsService.getInstance().initAdsService(this, new AdsManagerConfiguration.Builder().setDebug(false).setAdMobAppId(getString(R.string.admob_app_id)).setAdMobBannerId(getString(R.string.admob_banner_ad_id)).setAdMobNativeId(getString(R.string.admob_native_ad_unit)).setAdMobInterstialId(getString(R.string.admob_interstial_ad_id)).setAdMobOpenAdId(getString(R.string.admob_openapp_ad_unit)).setAdMobRewardVideoId(getString(R.string.admob_reward_video_ad)).setDarkThemeMode(true).setAdsEnabled(!(SharedPreferencesManager.getInstance().contains(Utils.IS_ADS_DISABLED) ? SharedPreferencesManager.getInstance().getBoolean(Utils.IS_ADS_DISABLED) : false)).setPersonalizedAdsEnabled(true).setInitInterstitialAd(true).setNativeAdLayoutDefault(false).build());

    }

    public static Context getContext() {
        return context;
    }

    public static BaseApplication getApp() {
        return instance;
    }


}
