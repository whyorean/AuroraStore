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

public class UIFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.preferences_ui, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences mPrefs = Util.getPrefs(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        ListPreference mThemeStyle = findPreference(Constants.PREFERENCE_THEME);
        assert mThemeStyle != null;
        mThemeStyle.setOnPreferenceChangeListener((preference, newValue) -> {
            PrefUtil.putString(context, Constants.PREFERENCE_THEME, newValue.toString());
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
                getActivity().recreate();
            }
            return true;
        });

        ListPreference defaultTabList = findPreference(Constants.PREFERENCE_DEFAULT_TAB);
        assert defaultTabList != null;
        defaultTabList.setOnPreferenceChangeListener((preference, newValue) -> {
            PrefUtil.putString(context, Constants.PREFERENCE_DEFAULT_TAB, newValue.toString());
            return true;
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREFERENCE_FEATURED_SNAP:
            case Constants.PREFERENCE_UI_CARD_STYLE:
                SettingsActivity.shouldRestart = true;
                break;
        }
    }
}
