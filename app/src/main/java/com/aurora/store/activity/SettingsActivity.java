/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.activity;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.utility.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static boolean shouldRestart = false;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private ThemeUtil mThemeUtil = new ThemeUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeUtil.onCreate(this);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        setupActionBar();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setTitle(R.string.action_settings);
                if (shouldRestart)
                    askRestart();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mThemeUtil.onResume(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager()
                .getFragmentFactory()
                .instantiate(this.getClassLoader(), pref.getFragment(), args);
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    private void setupActionBar() {
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0f);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void askRestart() {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_title_logout))
                .setMessage(getString(R.string.pref_dialog_to_apply_restart))
                .setPositiveButton(getString(R.string.action_restart), (dialog, which) -> {
                    Util.restartApp(this);
                })
                .setNegativeButton(getString(R.string.action_later), (dialog, which) -> {
                    dialog.dismiss();
                });
        mBuilder.create();
        mBuilder.show();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_main, rootKey);
        }
    }

    public static class DownloaderFragment extends PreferenceFragmentCompat {

        Context context;
        EditTextPreference editTextPreference;
        SeekBarPreference seekBarPreference;

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
        }

        private void setupDownloadPath() {
            editTextPreference = findPreference(Constants.PREFERENCE_DOWNLOAD_DIRECTORY);
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
            seekBarPreference = findPreference(Constants.PREFERENCE_DOWNLOAD_ACTIVE);
            seekBarPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                PrefUtil.putInteger(context, Constants.PREFERENCE_DOWNLOAD_ACTIVE, seekBarPreference.getValue());
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
    }

    public static class FilterFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.preferences_filter, rootKey);
        }
    }

    public static class InstallationFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        Context context;
        SharedPreferences mPrefs;

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
            mPrefs = Util.getPrefs(context);
            mPrefs.registerOnSharedPreferenceChangeListener(this);

            ListPreference mThemeStyle = findPreference(Constants.PREFERENCE_INSTALLATION_METHOD);
            mThemeStyle.setOnPreferenceChangeListener((preference, newValue) -> {
                PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, (String) newValue);
                return true;
            });
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }
    }

    public static class NotificationFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        Context context;
        SharedPreferences mPrefs;

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
            mPrefs = Util.getPrefs(context);
            mPrefs.registerOnSharedPreferenceChangeListener(this);

            ListPreference mThemeStyle = findPreference(Constants.PREFERENCE_NOTIFICATION_PRIORITY);
            mThemeStyle.setOnPreferenceChangeListener((preference, newValue) -> {
                PrefUtil.putString(context, Constants.PREFERENCE_NOTIFICATION_PRIORITY, (String) newValue);
                return true;
            });
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }
    }

    public static class NetworkFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        Context context;
        SharedPreferences mPrefs;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            this.context = context;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.preferences_network, rootKey);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mPrefs = Util.getPrefs(context);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case Constants.PREFERENCE_ENABLE_PROXY:
                    SettingsActivity.shouldRestart = true;
                    break;
            }
        }
    }

    public static class UIFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        Context context;
        SharedPreferences mPrefs;

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
            mPrefs = Util.getPrefs(context);
            mPrefs.registerOnSharedPreferenceChangeListener(this);

            ListPreference mThemeStyle = findPreference(Constants.PREFERENCE_THEME);
            mThemeStyle.setOnPreferenceChangeListener((preference, newValue) -> {
                PrefUtil.putString(context, Constants.PREFERENCE_THEME, newValue.toString());
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    getActivity().recreate();
                }
                return true;
            });
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case Constants.PREFERENCE_FEATURED_SNAP:
                    SettingsActivity.shouldRestart = true;
                    break;
            }
        }
    }
}
