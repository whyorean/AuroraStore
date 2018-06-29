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
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.dragons.aurora.AuroraPermissionManager;
import com.dragons.aurora.MultiSelectListPreference;
import com.dragons.aurora.OnListPreferenceChangeListener;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.fragment.preference.Blacklist;
import com.dragons.aurora.fragment.preference.CheckUpdates;
import com.dragons.aurora.fragment.preference.DownloadDirectory;
import com.dragons.aurora.fragment.preference.InstallationMethod;

public class PreferenceFragment extends android.preference.PreferenceFragment {

    public static final String PREFERENCE_AUTO_INSTALL = "PREFERENCE_AUTO_INSTALL";
    public static final String PREFERENCE_UPDATE_LIST_WHITE_OR_BLACK = "PREFERENCE_UPDATE_LIST_WHITE_OR_BLACK";
    public static final String PREFERENCE_UPDATE_LIST = "PREFERENCE_UPDATE_LIST";
    public static final String PREFERENCE_BACKGROUND_UPDATE_INTERVAL = "PREFERENCE_BACKGROUND_UPDATE_INTERVAL";
    public static final String PREFERENCE_DELETE_APK_AFTER_INSTALL = "PREFERENCE_DELETE_APK_AFTER_INSTALL";
    public static final String PREFERENCE_BACKGROUND_UPDATE_DOWNLOAD = "PREFERENCE_BACKGROUND_UPDATE_DOWNLOAD";
    public static final String PREFERENCE_BACKGROUND_UPDATE_WIFI_ONLY = "PREFERENCE_BACKGROUND_UPDATE_WIFI_ONLY";
    public static final String PREFERENCE_BACKGROUND_UPDATE_INSTALL = "PREFERENCE_BACKGROUND_UPDATE_INSTALL";
    public static final String PREFERENCE_REQUESTED_LANGUAGE = "PREFERENCE_REQUESTED_LANGUAGE";
    public static final String PREFERENCE_REQUESTED_LANGUAGE_INDEX = "PREFERENCE_REQUESTED_LANGUAGE_INDEX";
    public static final String PREFERENCE_DEVICE_TO_PRETEND_TO_BE = "PREFERENCE_DEVICE_TO_PRETEND_TO_BE";
    public static final String PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX = "PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX";
    public static final String PREFERENCE_REQUESTED_LOCATION_INDEX = "PREFERENCE_REQUESTED_LOCATION_INDEX";
    public static final String PREFERENCE_INSTALLATION_METHOD = "PREFERENCE_INSTALLATION_METHOD";
    public static final String PREFERENCE_NO_IMAGES = "PREFERENCE_NO_IMAGES";
    public static final String PREFERENCE_DOWNLOAD_DIRECTORY = "PREFERENCE_DOWNLOAD_DIRECTORY";
    public static final String PREFERENCE_DOWNLOAD_DELTAS = "PREFERENCE_DOWNLOAD_DELTAS";
    public static final String PREFERENCE_AUTO_WHITELIST = "PREFERENCE_AUTO_WHITELIST";

    public static final String INSTALLATION_METHOD_DEFAULT = "default";
    public static final String INSTALLATION_METHOD_ROOT = "root";
    public static final String INSTALLATION_METHOD_PRIVILEGED = "privileged";

    public static final String LIST_BLACK = "black";

    static public boolean getBoolean(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
    }

    static public String getString(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
    }

    static public int getUpdateInterval(Context context) {
        return Util.parseInt(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                        PreferenceFragment.PREFERENCE_BACKGROUND_UPDATE_INTERVAL, "-1"),
                -1
        );
    }

    static public boolean canInstallInBackground(Context context) {
        return getString(context, PREFERENCE_INSTALLATION_METHOD).equals(INSTALLATION_METHOD_ROOT)
                || getString(context, PREFERENCE_INSTALLATION_METHOD).equals(INSTALLATION_METHOD_PRIVILEGED)
                ;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.getActivity().setTitle(R.string.action_settings);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageView toolbar_back = this.getActivity().findViewById(R.id.toolbar_back);
        toolbar_back.setOnClickListener(click -> getActivity().onBackPressed());
        addPreferencesFromResource(R.xml.settings);
        setupThemes();
        setupSubCategory();
        setupSwitches(getActivity());
        drawBlackList();
        drawUpdatesCheck();
        drawInstallationMethod();
        new DownloadDirectory(this).setPreference((EditTextPreference) findPreference(PREFERENCE_DOWNLOAD_DIRECTORY)).draw();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (!AuroraPermissionManager.isGranted(requestCode, permissions, grantResults)) {
            Log.i(getClass().getSimpleName(), "User denied the write permission");
            getActivity().finish();
        }
    }

    private void drawBlackList() {
        Blacklist blacklistFragment = new Blacklist(this);
        blacklistFragment.setBlackOrWhite((ListPreference) findPreference(PREFERENCE_UPDATE_LIST_WHITE_OR_BLACK));
        blacklistFragment.setAppList((MultiSelectListPreference) findPreference(PREFERENCE_UPDATE_LIST));
        blacklistFragment.setAutoWhitelist((CheckBoxPreference) findPreference(PREFERENCE_AUTO_WHITELIST));
        blacklistFragment.draw();
    }

    private void drawUpdatesCheck() {
        CheckUpdates checkUpdatesFragment = new CheckUpdates(this);
        checkUpdatesFragment.setCheckForUpdates((ListPreference) findPreference(PREFERENCE_BACKGROUND_UPDATE_INTERVAL));
        checkUpdatesFragment.setAlsoInstall((CheckBoxPreference) findPreference(PREFERENCE_BACKGROUND_UPDATE_INSTALL));
        checkUpdatesFragment.setAlsoDownload((CheckBoxPreference) findPreference(PREFERENCE_BACKGROUND_UPDATE_DOWNLOAD));
        checkUpdatesFragment.draw();
    }

    private void drawInstallationMethod() {
        InstallationMethod installationMethodFragment = new InstallationMethod(this);
        installationMethodFragment.setInstallationMethodPreference((ListPreference) findPreference(PREFERENCE_INSTALLATION_METHOD));
        installationMethodFragment.draw();
    }

    private void setupSwitches(Context context) {
        SwitchPreference colors = (SwitchPreference) this.findPreference("COLOR_UI");
        colors.setChecked(Util.getBoolean(context, "COLOR_UI"));
        colors.setOnPreferenceChangeListener(new OnListPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Util.putBoolean(context, "COLOR_UI", (boolean) newValue);
                return true;
            }
        });

        SwitchPreference ime = (SwitchPreference) this.findPreference("SHOW_IME");
        ime.setChecked(Util.getBoolean(context, "SHOW_IME"));
        ime.setOnPreferenceChangeListener(new OnListPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Util.putBoolean(context, "SHOW_IME", (boolean) newValue);
                return true;
            }
        });

        SwitchPreference swipe_pages = (SwitchPreference) this.findPreference("SWIPE_PAGES");
        swipe_pages.setChecked(Util.getBoolean(context, "SWIPE_PAGES"));
        swipe_pages.setOnPreferenceChangeListener(new OnListPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Util.putBoolean(context, "SWIPE_PAGES", (boolean) newValue);
                return true;
            }
        });
    }

    private void setupThemes() {
        ListPreference preference_theme = (ListPreference) this.findPreference("PREFERENCE_THEME");
        preference_theme.setSummary(preference_theme.getEntry());

        preference_theme.setOnPreferenceChangeListener((preference, newTheme) -> {
            getPreferenceManager().getSharedPreferences().edit().putString("PREFERENCE_THEME", (String) newTheme).apply();
            restartHome();
            getActivity().finishAndRemoveTask();
            return false;
        });
    }

    private void setupSubCategory() {
        ListPreference preference_subcategory = (ListPreference) this.findPreference("PREFERENCE_SUBCATEGORY");
        preference_subcategory.setSummary(preference_subcategory.getEntry());

        preference_subcategory.setOnPreferenceChangeListener((preference, newTheme) -> {
            getPreferenceManager().getSharedPreferences().edit().putString("PREFERENCE_SUBCATEGORY", (String) newTheme).apply();
            restartHome();
            getActivity().finishAndRemoveTask();
            return false;
        });
    }

    private void restartHome() {
        Intent i = new Intent(this.getActivity(), AuroraActivity.class);
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}