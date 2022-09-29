package com.fttotal.screenrecorder.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.InputDeviceCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.fttotal.screenrecorder.BaseApplication;
import com.fttotal.screenrecorder.R;
import com.fttotal.screenrecorder.ads.AdsService;
import com.fttotal.screenrecorder.helpers.Utils;
import com.fttotal.screenrecorder.managers.SharedPreferencesManager;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.drawer_menu_button).setOnClickListener(view -> {
            /*if (Utils.isServiceRunning(FloatingSSCapService.class.getName(), getApplicationContext())) {
                Intent stopIntent = new Intent(SettingsActivity.this, FloatingSSCapService.class);
                stopService(stopIntent);
            }*/
            finish();
        });
       // AdsService.getInstance().showNativeAd(findViewById(R.id.layout_native_ad), R.layout.admob_native_ad, AdsService.NativeAdType.NATIVE_AD_TYPE_MEDIUM);
        getSupportFragmentManager().beginTransaction().replace(R.id.content, new MainPreferenceFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        AdsService.getInstance().showInterstitialAd(this);

       /* if (Utils.isServiceRunning(FloatingSSCapService.class.getName(), getApplicationContext())) {
            Intent stopIntent = new Intent(SettingsActivity.this, FloatingSSCapService.class);
            stopService(stopIntent);
        }*/

        super.onBackPressed();
    }

    public static class MainPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
        private ListPreference key_audio_source;
        private EditTextPreference key_common_countdown;
        private ListPreference key_output_format;
        private SwitchPreference key_record_audio;
        private SwitchPreference key_reward_video;
        private ListPreference key_video_bitrate;
        private ListPreference key_video_encoder;
        private ListPreference key_video_fps;
        private ListPreference key_video_resolution;
        private String previous_countdown;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        }


        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            view.setBackgroundColor(getResources().getColor(R.color.appBgColor));
            view.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            return view;
        }

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            addPreferencesFromResource(R.xml.pref_main);
            key_record_audio = findPreference(getString(R.string.key_record_audio));
            key_reward_video = findPreference(getString(R.string.key_reward_video));
            if (key_reward_video != null) {
                key_reward_video.setOnPreferenceChangeListener(this);
            }
            key_common_countdown = findPreference(getString(R.string.key_common_countdown));


            if (key_common_countdown != null) {
                key_common_countdown.setOnPreferenceChangeListener(this);
                key_common_countdown.setOnBindEditTextListener(editText -> editText.setInputType(InputDeviceCompat.SOURCE_TOUCHSCREEN));
            }
            key_audio_source = findPreference(getString(R.string.key_audio_source));
            if (key_audio_source != null) {
                key_audio_source.setOnPreferenceChangeListener(this);
            }
            key_video_encoder = findPreference(getString(R.string.key_video_encoder));

            if (key_video_encoder != null) {
                key_video_encoder.setOnPreferenceChangeListener(this);
            }
            key_video_resolution = findPreference(getString(R.string.key_video_resolution));
            if (key_video_resolution != null) {
                key_video_resolution.setOnPreferenceChangeListener(this);
            }
            key_video_fps = findPreference(getString(R.string.key_video_fps));
            if (key_video_fps != null) {
                key_video_fps.setOnPreferenceChangeListener(this);
            }
            key_video_bitrate = findPreference(getString(R.string.key_video_bitrate));
            if (key_video_bitrate != null) {
                key_video_bitrate.setOnPreferenceChangeListener(this);
            }
            key_output_format = findPreference(getString(R.string.key_output_format));
            if (key_output_format != null) {
                key_output_format.setOnPreferenceChangeListener(this);
            }
            setPreviousSelectedAsSummary();
        }


        private void setColorPreferencesTitle(EditTextPreference textPref, int color) {
            CharSequence cs = (CharSequence) textPref.getTitle();
            String plainTitle = cs.subSequence(0, cs.length()).toString();
            Spannable coloredTitle = new SpannableString(plainTitle);
            coloredTitle.setSpan(new ForegroundColorSpan(color), 0, coloredTitle.length(), 0);
            textPref.setTitle(coloredTitle);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            ListPreference listPreference;
            switch (key) {
                case "key_common_countdown":
                    if (Integer.valueOf(newValue.toString()).intValue() <= 15) {
                        key_common_countdown = findPreference(getString(R.string.key_common_countdown));
                        key_common_countdown.setSummary("" + newValue.toString() + "s");
                        previous_countdown = newValue.toString();
                        break;
                    } else {
                        Toast.makeText(BaseApplication.getContext(), "Maximum value for countdown is 15 seconds", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                case "key_audio_source":
                    listPreference = findPreference(getString(R.string.key_audio_source));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        break;
                    }
                    break;
                case "key_video_encoder":
                    listPreference = findPreference(getString(R.string.key_video_encoder));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }
                    break;
                case "key_video_resolution":
                    listPreference = findPreference(getString(R.string.key_video_resolution));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }
                    break;
                case "key_video_fps":
                    listPreference = findPreference(getString(R.string.key_video_fps));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }
                    break;
                case "key_video_bitrate":
                    listPreference = findPreference(getString(R.string.key_video_bitrate));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }
                    break;
                case "key_output_format":
                    listPreference = findPreference(getString(R.string.key_output_format));
                    if (listPreference != null) {
                        listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(newValue.toString())]);
                        listPreference.setValue(newValue.toString());
                    }
                    break;
                case "key_reward_video":
                    if (Boolean.parseBoolean(newValue.toString())) {
                        AdsService.getInstance().showRewardVideo(getActivity());
                    }
                    break;
            }
            return true;
        }

        private void setPreviousSelectedAsSummary() {
            if (getActivity() != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String video_resolution = prefs.getString("key_video_resolution", null);
                boolean audio_enabled = prefs.getBoolean("key_record_audio", true);
                boolean reward_video = SharedPreferencesManager.getInstance().getBoolean(Utils.IS_REWARD_VIDEO, false);
                String audio_source = prefs.getString("key_audio_source", null);
                String video_encoder = prefs.getString("key_video_encoder", null);
                String video_fps = prefs.getString("key_video_fps", null);
                String video_bitrate = prefs.getString("key_video_bitrate", null);
                String output_format = prefs.getString("key_output_format", null);
                previous_countdown = prefs.getString("key_common_countdown", null);
                Log.d("Summary", "common_countdown: " + previous_countdown);
                Log.d("Summary", "reward_video_enabled: " + reward_video);
                Log.d("Summary", "audio_enabled: " + audio_enabled);
                Log.d("Summary", "audio_source: " + audio_source);
                Log.d("Summary", "video_resolution: " + video_resolution);
                Log.d("Summary", "video_encoder: " + video_encoder);
                Log.d("Summary", "video_frame_rate: " + video_fps);
                Log.d("Summary", "video_bit_rate: " + video_bitrate);
                Log.d("Summary", "output_format: " + output_format);
                key_record_audio.setChecked(audio_enabled);
                key_reward_video.setChecked(reward_video);

                SharedPreferencesManager.getInstance().setBoolean(Utils.IS_REWARD_VIDEO, reward_video);
                if (previous_countdown != null) {
                    key_common_countdown.setSummary("" + previous_countdown + "s");
                }
                if (audio_source != null) {
                    int index = key_audio_source.findIndexOfValue(audio_source);
                    key_audio_source.setSummary(key_audio_source.getEntries()[index]);

                } else {
                    String defaultSummary = PreferenceManager.getDefaultSharedPreferences(key_audio_source.getContext()).getString(key_audio_source.getKey(), "");
                    key_audio_source.setSummary(defaultSummary);
                }

                if (video_encoder != null) {
                    int findIndexOfValue2 = key_video_encoder.findIndexOfValue(video_encoder);
                    key_video_encoder.setSummary(key_video_encoder.getEntries()[findIndexOfValue2]);
                } else {
                    key_video_encoder.setSummary(android.preference.PreferenceManager.getDefaultSharedPreferences(key_video_encoder.getContext()).getString(this.key_video_encoder.getKey(), ""));
                }
                if (video_resolution != null) {
                    int findIndexOfValue3 = key_video_resolution.findIndexOfValue(video_resolution);
                    key_video_resolution.setSummary(key_video_resolution.getEntries()[findIndexOfValue3]);
                } else {
                    key_video_resolution.setSummary(android.preference.PreferenceManager.getDefaultSharedPreferences(key_video_resolution.getContext()).getString(this.key_video_resolution.getKey(), ""));
                }
                if (video_fps != null) {
                    int findIndexOfValue4 = key_video_fps.findIndexOfValue(video_fps);
                    key_video_fps.setSummary(key_video_fps.getEntries()[findIndexOfValue4]);
                } else {
                    key_video_fps.setSummary(android.preference.PreferenceManager.getDefaultSharedPreferences(key_video_fps.getContext()).getString(key_video_fps.getKey(), ""));
                }
                if (video_bitrate != null) {
                    int findIndexOfValue5 = key_video_bitrate.findIndexOfValue(video_bitrate);
                    key_video_bitrate.setSummary(key_video_bitrate.getEntries()[findIndexOfValue5]);
                } else {
                    key_video_bitrate.setSummary(android.preference.PreferenceManager.getDefaultSharedPreferences(key_video_bitrate.getContext()).getString(key_video_bitrate.getKey(), ""));
                }
                if (output_format != null) {
                    int findIndexOfValue6 = key_output_format.findIndexOfValue(output_format);
                    key_output_format.setSummary(key_output_format.getEntries()[findIndexOfValue6]);
                    return;
                }
                key_output_format.setSummary(PreferenceManager.getDefaultSharedPreferences(key_output_format.getContext()).getString(key_output_format.getKey(), ""));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
