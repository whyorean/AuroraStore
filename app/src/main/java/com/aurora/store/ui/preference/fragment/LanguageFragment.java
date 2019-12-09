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
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.manager.LocaleManager;
import com.aurora.store.ui.preference.SettingsActivity;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.TextUtil;
import com.aurora.store.util.Util;

import java.util.Locale;

public class LanguageFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context;
    private LocaleManager localeManager;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        localeManager = new LocaleManager(context);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.preferences_lang, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences preferences = Util.getPrefs(context);
        preferences.registerOnSharedPreferenceChangeListener(this);

        ListPreference localeList = findPreference(Constants.PREFERENCE_LOCALE_LIST);
        assert localeList != null;
        localeList.setOnPreferenceChangeListener((preference, newValue) -> {
            String choice = newValue.toString();
            if (TextUtil.isEmpty(choice)) {
                PrefUtil.putBoolean(context, Constants.PREFERENCE_LOCALE_CUSTOM, false);
                localeManager.setNewLocale(Locale.getDefault(), false);
            } else {
                String lang = choice.split("-")[0];
                String country = choice.split("-")[1];
                Locale locale = new Locale(lang, country);
                localeManager.setNewLocale(locale, true);
            }
            return true;
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREFERENCE_LOCALE_CUSTOM:
            case Constants.PREFERENCE_LOCALE_LIST: {
                CategoryManager.clear(context);
                SettingsActivity.shouldRestart = true;
                break;
            }
        }
    }
}
