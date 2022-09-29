package com.fttotal.screenrecorder.ads.formats;

import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.fttotal.screenrecorder.ads.AdsService;
import com.fttotal.screenrecorder.ads.AdsUtil;

public class Banner {

    public static String TAG = "tag_ad_manager";

    public static void getAdaptiveBanner(Context context, FrameLayout frameLayout) {
        AdView adView = new AdView(context);
        adView.setAdUnitId(AdsService.getInstance().setBannerId());
        frameLayout.addView(adView);
        adView.setAdSize(AdsUtil.getAdSize(context));
        adView.loadAd(AdsUtil.appendUserConsent(AdsService.getInstance().getConfiguration().isPersonalizedAdsEnabled()));
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                Log.d(Banner.TAG, "Adaptive Banner, onAdLoaded()");
            }
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                Log.d(Banner.TAG, "Adaptive Banner, onAdClicked()");
            }
            @Override
            public void onAdOpened() {
                super.onAdOpened();
                Log.d(Banner.TAG, "Adaptive Banner, onAdOpened()");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
            }
            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Log.d(Banner.TAG, "Adaptive Banner, onAdImpression()");
            }
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Log.d(Banner.TAG, "Adaptive Banner, onAdClosed()");
            }

        });
    }
}
