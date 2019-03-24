package com.aurora.store.fragment.preference;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.activity.SettingsActivity;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.PrefUtil;

import java.io.File;
import java.io.IOException;

public class DownloaderFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.preferences_downloader, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDownloadPath();
        setupActiveDownloads();

        ListPreference strategyList = findPreference(Constants.PREFERENCE_DOWNLOAD_STRATEGY);
        assert strategyList != null;
        strategyList.setOnPreferenceChangeListener((preference, newValue) -> {
            PrefUtil.putString(context, Constants.PREFERENCE_DOWNLOAD_STRATEGY, (String) newValue);
            return true;
        });
    }

    private void setupDownloadPath() {
        EditTextPreference editTextPreference = findPreference(Constants.PREFERENCE_DOWNLOAD_DIRECTORY);
        assert editTextPreference != null;
        editTextPreference.setText(PathUtil.getRootApkPath(context));
        editTextPreference.setSummaryProvider(preference -> PathUtil.getRootApkPath(context));
        editTextPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean success = checkIfValid(newValue.toString());
            if (success)
                PrefUtil.putString(context, Constants.PREFERENCE_DOWNLOAD_DIRECTORY, newValue.toString());
            else {
                PrefUtil.putString(context, Constants.PREFERENCE_DOWNLOAD_DIRECTORY, "");
                Toast.makeText(context, "Could not set download path", Toast.LENGTH_LONG).show();
            }
            return true;
        });
    }

    private void setupActiveDownloads() {
        SeekBarPreference seekBarPreference = findPreference(Constants.PREFERENCE_DOWNLOAD_ACTIVE);
        assert seekBarPreference != null;
        seekBarPreference.setShowSeekBarValue(true);
        seekBarPreference.setMin(1);
        seekBarPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int value = (Integer) newValue;
            PrefUtil.putInteger(context, Constants.PREFERENCE_DOWNLOAD_ACTIVE, value - 1);
            return true;
        });
    }

    private boolean checkIfValid(String newValue) {
        try {
            File newDir = new File(newValue).getCanonicalFile();
            if (newDir.exists()) {
                return newDir.canWrite();
            }
            if (context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return newDir.mkdirs();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREFERENCE_DOWNLOAD_STRATEGY:
                SettingsActivity.shouldRestart = true;
                break;
        }
    }
}
