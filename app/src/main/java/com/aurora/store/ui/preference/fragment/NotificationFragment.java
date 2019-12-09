package com.aurora.store.ui.preference.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.ui.preference.SettingsActivity;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;

public class NotificationFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.preferences_notification, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences mPrefs = Util.getPrefs(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        ListPreference priorityList = findPreference(Constants.PREFERENCE_NOTIFICATION_PRIORITY);
        assert priorityList != null;
        priorityList.setOnPreferenceChangeListener((preference, newValue) -> {
            PrefUtil.putString(context, Constants.PREFERENCE_NOTIFICATION_PRIORITY, (String) newValue);
            return true;
        });

        /*ListPreference providerList = findPreference(Constants.PREFERENCE_NOTIFICATION_PROVIDER);
        assert providerList != null;
        providerList.setOnPreferenceChangeListener((preference, newValue) -> {
            PrefUtil.putString(context, Constants.PREFERENCE_NOTIFICATION_PROVIDER, (String) newValue);
            return true;
        });*/
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREFERENCE_NOTIFICATION_TOGGLE:
            case Constants.PREFERENCE_NOTIFICATION_PROVIDER:
                SettingsActivity.shouldRestart = true;
                break;
        }
    }
}
