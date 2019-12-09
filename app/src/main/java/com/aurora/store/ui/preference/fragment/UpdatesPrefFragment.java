package com.aurora.store.ui.preference.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.SelfUpdateService;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;

public class UpdatesPrefFragment extends PreferenceFragmentCompat {

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.preferences_updates, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListPreference updatesIntervalList = findPreference(Constants.PREFERENCE_UPDATES_INTERVAL);
        assert updatesIntervalList != null;
        updatesIntervalList.setOnPreferenceChangeListener((preference, newValue) -> {
            String value = newValue.toString();
            int interval = Util.parseInt(value, 0);
            PrefUtil.putString(context, Constants.PREFERENCE_UPDATES_INTERVAL, value);
            Util.setUpdatesInterval(context, interval);
            return true;
        });

        Preference preferenceUpdate = findPreference(Constants.PREFERENCE_SELF_UPDATE);
        assert preferenceUpdate != null;
        preferenceUpdate.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(context, SelfUpdateService.class);
            context.startService(intent);
            return true;
        });
    }
}
