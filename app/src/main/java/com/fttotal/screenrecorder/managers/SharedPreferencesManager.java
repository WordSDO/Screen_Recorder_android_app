package com.fttotal.screenrecorder.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.fttotal.screenrecorder.BaseApplication;
import com.fttotal.screenrecorder.R;

public class SharedPreferencesManager {
    private static SharedPreferencesManager appPreferences;
    private SharedPreferences sharedPreferences;

    public static SharedPreferencesManager getInstance() {
        if (appPreferences == null) {
            appPreferences = new SharedPreferencesManager();
            if (appPreferences.sharedPreferences == null) {
                Context context = BaseApplication.getContext();
                appPreferences.sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs), 0);
            }
        }
        return appPreferences;
    }

    public boolean contains(String name) {
        return this.sharedPreferences.contains(name);
    }

    public boolean getBoolean(String name, boolean b) {
        return this.sharedPreferences.getBoolean(name, b);
    }

    public void setBoolean(String name, boolean b) {
        this.sharedPreferences.edit().putBoolean(name, b).apply();
    }

    public boolean getBoolean(String name) {
        return this.sharedPreferences.getBoolean(name, false);
    }
}
