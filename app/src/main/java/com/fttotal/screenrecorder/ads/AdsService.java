package com.fttotal.screenrecorder.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleObserver;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.ads.formats.Banner;
import com.fttotal.screenrecorder.ads.formats.Interstitial;
import com.fttotal.screenrecorder.ads.formats.NativeAdApp;
import com.fttotal.screenrecorder.ads.formats.OpenAd;
import com.fttotal.screenrecorder.helpers.Utils;

import static com.fttotal.screenrecorder.R.*;

public class AdsService implements LifecycleObserver {

    public static String TAG = "tag_ad_manager";
    private static AdsService adsService;
    private AdsManagerConfiguration adsManagerConfiguration;

    private Context mContext;


    public RewardedAd mRewardedAd;

    public enum NativeAdType {
        NATIVE_AD_TYPE_MEDIUM,
        NATIVE_AD_TYPE_MEDIA
    }

    public static AdsService getInstance() {
        if (adsService == null) {
            adsService = new AdsService();
        }
        return adsService;
    }

    public void initAdsService(final Context context, AdsManagerConfiguration adsManagerConfiguration2) {
        MobileAds.initialize(context, initializationStatus -> {
            Log.d(AdsService.TAG, "inside AdsService class, onInitializationComplete()");
            OpenAd.getInstance().initOpenAd(context);
            OpenAd.getInstance().fetchAd();
            loadRewardVideo();
        });
        this.mContext = context;
        this.adsManagerConfiguration = adsManagerConfiguration2;
        if (adsManagerConfiguration2.isInitInterstitialAd()) {
            Log.d(TAG, "default: initialise interstitial ad on start");
            Interstitial.initialize(this.mContext);
        }
    }

    public void setConfiguration(AdsManagerConfiguration adsManagerConfiguration2) {
        Log.d(TAG, "inside AdsService class, setConfiguration()");
        this.adsManagerConfiguration = adsManagerConfiguration2;
    }

    public AdsManagerConfiguration getConfiguration() {
        Log.d(TAG, "inside AdsService class, getConfiguration()");
        return this.adsManagerConfiguration;
    }

    public boolean isEnableAds() {
        return this.adsManagerConfiguration.isAdsEnabled();
    }

    public void showInterstitialAd(Activity mActivity) {
        if (AdsUtil.isNetworkAvailable(this.mContext) && isEnableAds()) {
            Interstitial.getInterstitialAd(mActivity);
        }
    }

    public void showAdaptiveBannerAd(FrameLayout frameLayout) {
        if (AdsUtil.isNetworkAvailable(this.mContext) && isEnableAds()) {
            Banner.getAdaptiveBanner(this.mContext, frameLayout);
        }
    }

    public void showOpenAdIfAvailable(final Activity activity) {
        if (OpenAd.getInstance().getIsShowingAd() || !OpenAd.getInstance().isAdAvailable()) {
            OpenAd.getInstance().fetchAd();
            return;
        }
        Log.d(TAG, "showOpenAdIfAvailable()");
        OpenAd.getInstance().displayAd(activity, new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                OpenAd.getInstance().setIsShowingAd(false);
                try {
                    activity.finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                try {
                    OpenAd.getInstance().setIsShowingAd(false);
                    activity.finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                OpenAd.getInstance().setIsShowingAd(true);
            }
        });
    }

    public void showNativeAd(FrameLayout frameLayout, int i, NativeAdType nativeAdType) {
        if (AdsUtil.isNetworkAvailable(this.mContext) && isEnableAds()) {
            NativeAdApp.getNativeAd(this.mContext, frameLayout, i, nativeAdType);
        }
    }


    public void loadRewardVideo() {
        if (AdsUtil.isNetworkAvailable(this.mContext) && isEnableAds()) {
            AdManagerAdRequest adRequest = new AdManagerAdRequest.Builder().build();
            RewardedAd.load(
                    mContext,
                    "ca-app-pub-3247218134412060/9393043512",
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error.
                            Log.d(TAG, loadAdError.getMessage());
                            mRewardedAd = null;
//                            Toast.makeText(mContext, "onAdFailedToLoad", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            mRewardedAd = rewardedAd;
//                            Toast.makeText(mContext, "onAdLoaded", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public void showRewardVideo(Activity activity) {
        if (mRewardedAd == null) {
            Log.d("TAG", "The rewarded ad wasn't ready yet.");
            return;
        }
        mRewardedAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                        Log.d(TAG, "onAdShowedFullScreenContent");
//                        Toast.makeText(mContext, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT)
//                                .show();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        // Called when ad fails to show.
                        Log.d(TAG, "onAdFailedToShowFullScreenContent");
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mRewardedAd = null;
//                        Toast.makeText(
//                                mContext, "onAdFailedToShowFullScreenContent", Toast.LENGTH_SHORT)
//                                .show();
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when ad is dismissed.
                        Log.d(TAG, "onAdDismissedFullScreenContent");
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mRewardedAd = null;
//                        Toast.makeText(mContext, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT)
//                                .show();
                        // Preload the next rewarded ad.
                        loadRewardVideo();
                    }
                });
        mRewardedAd.show(activity
                ,
                new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        // Handle the reward.
                        Log.d("TAG", "The user earned the reward.");
                        int rewardAmount = rewardItem.getAmount();
                        String rewardType = rewardItem.getType();
                        mContext.getSharedPreferences("Preferences", 0).edit().putBoolean(Utils.IS_REWARD_VIDEO, true).apply();
                        loadRewardVideo();
                    }
                });
    }


    public String setBannerId() {
        if (this.adsManagerConfiguration.isDebug()) {
            return "ca-app-pub-3247218134412060/5864862041";
        }
        return this.adsManagerConfiguration.getAdMobBannerId();
    }

    public String setNativeId() {
        if (this.adsManagerConfiguration.isDebug()) {
            return "ca-app-pub-3247218134412060/2663983638";
        }
        return this.adsManagerConfiguration.getAdMobNativeId();
    }

    public String setInterstialId() {
        if (this.adsManagerConfiguration.isDebug()) {
            return "ca-app-pub-3247218134412060/2280840253";
        }
        return this.adsManagerConfiguration.getAdMobInterstialId();
    }

    public String setOpenAdId() {
        if (this.adsManagerConfiguration.isDebug()) {
            return "ca-app-pub-3247218134412060/3210778543";
        }
        return this.adsManagerConfiguration.getAdMobAppId();
    }

    public String setRewardVideoAdId() {
        if (this.adsManagerConfiguration.isDebug()) {
            return "ca-app-pub-3247218134412060/9393043512";
        }
        return this.adsManagerConfiguration.getAdMobRewardVideoId();
    }
}
