package com.aurora.store.fragment.preference;

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
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;

public class InstallationFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.preferences_installation, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences mPrefs = Util.getPrefs(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        ListPreference mThemeStyle = findPreference(Constants.PREFERENCE_INSTALLATION_METHOD);
        assert mThemeStyle != null;
        mThemeStyle.setOnPreferenceChangeListener((preference, newValue) -> {
            PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, (String) newValue);
            return true;
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
}