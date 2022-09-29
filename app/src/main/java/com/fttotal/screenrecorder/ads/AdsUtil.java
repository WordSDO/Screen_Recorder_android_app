package com.fttotal.screenrecorder.ads;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.ArrayList;

public class AdsUtil {
    private static String TAG = "tag_ad_manager";

    public static AdSize getAdSize(Context context) {
        Display defaultDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(displayMetrics);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, (int) (((float) displayMetrics.widthPixels) / displayMetrics.density));
    }

    public static AdRequest appendUserConsent(boolean isEnabled) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("B3EEABB8EE11C2BE770B684D95219ECB");
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder().setTestDeviceIds(arrayList).build());
        if (isEnabled) {
            Bundle bundle = new Bundle();
            bundle.putString("npa", "1");
            Log.d(TAG, "consent status: npa");
            return new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, bundle).build();
        }
        Log.d(TAG, "consent status: pa");
        return new AdRequest.Builder().build();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
}
