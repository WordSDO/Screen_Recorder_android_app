package com.fttotal.screenrecorder.ads;

import android.util.Log;

public class AdsManagerConfiguration {

    public static String TAG = "tag_ad_manager";
    private String adMobAppId;
    private String adMobBannerId;
    private String adMobInterstialId;
    private String adMobNativeId;
    private String adMobOpenAdId;
    private String adMobRewardVideoId;
    private boolean adsEnabled;
    private boolean darkThemeMode;
    private boolean debug;
    private boolean initInterstitialAd;
    private boolean nativeAdLayoutDefault;
    private boolean personalizedAdsEnabled;

    private AdsManagerConfiguration(Builder builder) {
        adMobAppId = "";
        adMobBannerId = "";
        adMobNativeId = "";
        adMobInterstialId = "";
        adMobOpenAdId = "";
        adMobRewardVideoId = "";
        darkThemeMode = false;
        nativeAdLayoutDefault = true;
        adsEnabled = false;
        personalizedAdsEnabled = false;
        initInterstitialAd = true;
        debug = true;
        adMobAppId = builder.adMobAppId;
        adMobBannerId = builder.adMobBannerId;
        adMobNativeId = builder.adMobNativeId;
        adMobInterstialId = builder.adMobInterstialId;
        adMobOpenAdId = builder.adMobOpenAdId;
        adMobRewardVideoId = builder.adMobRewardVideoId;
        darkThemeMode = builder.darkThemeMode;
        nativeAdLayoutDefault = builder.nativeAdLayoutDefault;
        adsEnabled = builder.adsEnabled;
        personalizedAdsEnabled = builder.personalizedAdsEnabled;
        initInterstitialAd = builder.initInterstitialAd;
        debug = builder.debug;
    }

    public String getAdMobAppId() {
        return this.adMobAppId;
    }

    public String getAdMobBannerId() {
        return this.adMobBannerId;
    }

    public String getAdMobNativeId() {
        return this.adMobNativeId;
    }

    public String getAdMobInterstialId() {
        return this.adMobInterstialId;
    }

    public String getAdMobRewardVideoId() {
        return this.adMobRewardVideoId;
    }

    public String getAdMobOpenAdId() {
        return this.adMobOpenAdId;
    }

    public boolean isDarkThemeMode() {
        return this.darkThemeMode;
    }

    public boolean isNativeAdLayoutDefault() {
        return this.nativeAdLayoutDefault;
    }

    public boolean isAdsEnabled() {
        return this.adsEnabled;
    }

    public boolean isPersonalizedAdsEnabled() {
        return this.personalizedAdsEnabled;
    }

    public boolean isInitInterstitialAd() {
        return this.initInterstitialAd;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public static class Builder {

        public String adMobAppId = "";

        public String adMobBannerId = "";

        public String adMobInterstialId = "";

        public String adMobNativeId = "";

        public String adMobOpenAdId = "";

        public String adMobRewardVideoId = "";

        public boolean adsEnabled = true;

        public boolean darkThemeMode = false;

        public boolean debug = true;

        public boolean initInterstitialAd = true;

        public boolean nativeAdLayoutDefault = true;

        public boolean personalizedAdsEnabled = false;

        public Builder() {
            Log.d(AdsManagerConfiguration.TAG, "inside Builder static class, constructor()");
        }

        public AdsManagerConfiguration build() {
            Log.d(AdsManagerConfiguration.TAG, "inside Builder static class, build()");
            return new AdsManagerConfiguration(this);
        }

        public Builder setAdMobAppId(String adMobAppId) {
            this.adMobAppId = adMobAppId;
            return this;
        }

        public Builder setAdMobBannerId(String adMobBannerId) {
            this.adMobBannerId = adMobBannerId;
            return this;
        }

        public Builder setAdMobNativeId(String adMobNativeId) {
            this.adMobNativeId = adMobNativeId;
            return this;
        }

        public Builder setAdMobInterstialId(String adMobInterstialId) {
            this.adMobInterstialId = adMobInterstialId;
            return this;
        }

        public Builder setAdMobOpenAdId(String adMobOpenAdId) {
            this.adMobOpenAdId = adMobOpenAdId;
            return this;
        }

        public Builder setAdMobRewardVideoId(String adMobRewardVideoId) {
            this.adMobRewardVideoId = adMobRewardVideoId;
            return this;
        }

        public Builder setDarkThemeMode(boolean darkThemeMode) {
            this.darkThemeMode = darkThemeMode;
            return this;
        }

        public Builder setNativeAdLayoutDefault(boolean nativeAdLayoutDefault) {
            this.nativeAdLayoutDefault = nativeAdLayoutDefault;
            return this;
        }

        public Builder setAdsEnabled(boolean adsEnabled) {
            this.adsEnabled = adsEnabled;
            return this;
        }

        public Builder setPersonalizedAdsEnabled(boolean personalizedAdsEnabled) {
            this.personalizedAdsEnabled = personalizedAdsEnabled;
            return this;
        }

        public Builder setInitInterstitialAd(boolean initInterstitialAd) {
            this.initInterstitialAd = initInterstitialAd;
            return this;
        }

        public Builder setDebug(boolean debug) {
            this.debug = debug;
            return this;
        }
    }
}
