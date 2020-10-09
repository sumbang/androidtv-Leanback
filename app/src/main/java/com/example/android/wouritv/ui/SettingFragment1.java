package com.example.android.wouritv.ui;

import androidx.leanback.preference.LeanbackSettingsFragment;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;

/**
 * Created by Sumbang on 10/09/2017.
 */

public class SettingFragment1 extends LeanbackSettingsFragment implements DialogPreference.TargetFragment {

    @Override
    public void onPreferenceStartInitialScreen() {

    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment caller, PreferenceScreen pref) {
        return false;
    }

    @Override
    public Preference findPreference(CharSequence key) {
        return null;
    }
}
