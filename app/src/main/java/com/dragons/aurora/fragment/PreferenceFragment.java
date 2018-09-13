/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.dragons.aurora.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.AuroraPermissionManager;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.database.Jessie;
import com.dragons.aurora.fragment.preference.Blacklist;
import com.dragons.aurora.fragment.preference.CheckUpdates;
import com.dragons.aurora.fragment.preference.DownloadDirectory;
import com.dragons.aurora.fragment.preference.InstallationMethod;
import com.dragons.aurora.helpers.Prefs;
import com.percolate.caffeine.ToastUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import timber.log.Timber;

public class PreferenceFragment extends androidx.preference.PreferenceFragment {

    static public boolean getBoolean(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
    }

    static public String getString(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
    }

    static public int getUpdateInterval(Context context) {
        return Util.parseInt(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                        Aurora.PREFERENCE_BACKGROUND_UPDATE_INTERVAL, "-1"),
                -1
        );
    }

    static public boolean canInstallInBackground(Context context) {
        return getString(context, Aurora.PREFERENCE_INSTALLATION_METHOD).equals(Aurora.INSTALLATION_METHOD_ROOT)
                || getString(context, Aurora.PREFERENCE_INSTALLATION_METHOD).equals(Aurora.INSTALLATION_METHOD_PRIVILEGED)
                ;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.getActivity().setTitle(R.string.action_settings);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        setupThemes();
        setupSubCategory();
        setupSwitches(getActivity());
        drawBlackList();
        drawUpdatesCheck();
        drawInstallationMethod();
        setupDatabaseClear(getActivity());
        setupDatabaseValidity();
        new DownloadDirectory(this).setPreference((EditTextPreference) findPreference(Aurora.PREFERENCE_DOWNLOAD_DIRECTORY)).draw();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (!AuroraPermissionManager.isGranted(requestCode, permissions, grantResults)) {
            Timber.i("User denied the write permission");
            getActivity().finish();
        }
    }

    private void drawBlackList() {
        Blacklist blacklistFragment = new Blacklist(this);
        blacklistFragment.setBlackOrWhite((ListPreference) findPreference(Aurora.PREFERENCE_UPDATE_LIST_WHITE_OR_BLACK));
        blacklistFragment.setAppList((MultiSelectListPreference) findPreference(Aurora.PREFERENCE_UPDATE_LIST));
        blacklistFragment.setAutoWhitelist((CheckBoxPreference) findPreference(Aurora.PREFERENCE_AUTO_WHITELIST));
        blacklistFragment.draw();
    }

    private void drawUpdatesCheck() {
        CheckUpdates checkUpdatesFragment = new CheckUpdates(this);
        checkUpdatesFragment.setCheckForUpdates((ListPreference) findPreference(Aurora.PREFERENCE_BACKGROUND_UPDATE_INTERVAL));
        checkUpdatesFragment.setAlsoInstall((CheckBoxPreference) findPreference(Aurora.PREFERENCE_BACKGROUND_UPDATE_INSTALL));
        checkUpdatesFragment.setAlsoDownload((CheckBoxPreference) findPreference(Aurora.PREFERENCE_BACKGROUND_UPDATE_DOWNLOAD));
        checkUpdatesFragment.draw();
    }

    private void drawInstallationMethod() {
        InstallationMethod installationMethodFragment = new InstallationMethod(this);
        installationMethodFragment.setInstallationMethodPreference((ListPreference) findPreference(Aurora.PREFERENCE_INSTALLATION_METHOD));
        installationMethodFragment.draw();
    }

    private void setupSwitches(Context context) {
        SwitchPreference colors = (SwitchPreference) this.findPreference(Aurora.PREFERENCE_COLOR_UI);
        colors.setChecked(Prefs.getBoolean(context, Aurora.PREFERENCE_COLOR_UI));

        colors.setOnPreferenceChangeListener((preference, newValue) -> {
            Prefs.putBoolean(context, Aurora.PREFERENCE_COLOR_UI, (boolean) newValue);
            return true;
        });

        SwitchPreference bottom_colors = (SwitchPreference) this.findPreference(Aurora.PREFERENCE_COLOR_NAV);
        bottom_colors.setChecked(Prefs.getBoolean(context, Aurora.PREFERENCE_COLOR_NAV));
        bottom_colors.setOnPreferenceChangeListener((preference, newValue) -> {
            Prefs.putBoolean(context, Aurora.PREFERENCE_COLOR_NAV, (boolean) newValue);
            return true;
        });

        SwitchPreference ime = (SwitchPreference) this.findPreference(Aurora.PREFERENCE_SHOW_IME);
        ime.setChecked(Prefs.getBoolean(context, Aurora.PREFERENCE_SHOW_IME));
        ime.setOnPreferenceChangeListener((preference, newValue) -> {
            Prefs.putBoolean(context, Aurora.PREFERENCE_SHOW_IME, (boolean) newValue);
            return true;
        });

        SwitchPreference swipe_pages = (SwitchPreference) this.findPreference(Aurora.PREFERENCE_SWIPE_PAGES);
        swipe_pages.setChecked(Prefs.getBoolean(context, Aurora.PREFERENCE_SWIPE_PAGES));
        swipe_pages.setOnPreferenceChangeListener((preference, newValue) -> {
            Prefs.putBoolean(context, Aurora.PREFERENCE_SWIPE_PAGES, (boolean) newValue);
            return true;
        });
    }

    private void setupThemes() {
        ListPreference preference_theme = (ListPreference) this.findPreference(Aurora.PREFERENCE_THEME);
        preference_theme.setSummary(preference_theme.getEntry());

        preference_theme.setOnPreferenceChangeListener((preference, value) -> {
            getPreferenceManager().getSharedPreferences().edit().putString(Aurora.PREFERENCE_THEME, (String) value).apply();
            restartHome();
            getActivity().finishAndRemoveTask();
            return false;
        });
    }

    private void setupSubCategory() {
        ListPreference preference_subcategory = (ListPreference) this.findPreference(Aurora.PREFERENCE_SUBCATEGORY);
        preference_subcategory.setSummary(preference_subcategory.getEntry());

        preference_subcategory.setOnPreferenceChangeListener((preference, value) -> {
            getPreferenceManager().getSharedPreferences().edit().putString(Aurora.PREFERENCE_SUBCATEGORY, (String) value).apply();
            restartHome();
            getActivity().finishAndRemoveTask();
            return false;
        });
    }

    private void setupDatabaseClear(Context mContext) {
        Preference mPreference = (Preference) this.findPreference(Aurora.PREFERENCE_DATABASE_CLEAR);
        mPreference.setOnPreferenceClickListener(preference -> {
            Jessie mJessie = new Jessie(mContext);
            mJessie.removeDatabase();
            ToastUtils.quickToast(mContext, mContext.getString(R.string.pref_database_cleared));
            return false;
        });
    }

    private void setupDatabaseValidity() {
        ListPreference mListPreference = (ListPreference) this.findPreference(Aurora.PREFERENCE_DATABASE_VALIDITY);
        mListPreference.setSummary(mListPreference.getEntry());
        mListPreference.setOnPreferenceChangeListener((preference, value) -> {
            getPreferenceManager().getSharedPreferences().edit().putString(Aurora.PREFERENCE_DATABASE_VALIDITY, (String) value).apply();
            return false;
        });

    }

    private void restartHome() {
        Intent i = new Intent(this.getActivity(), AuroraActivity.class);
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}