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

package com.dragons.aurora.fragment.preference;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.InstalledAppsTask;

import java.util.HashMap;
import java.util.Map;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

public class Blacklist extends Abstract {

    private ListPreference blackOrWhite;
    private MultiSelectListPreference appList;
    private CheckBoxPreference autoWhitelist;

    public Blacklist(PreferenceFragment activity) {
        super(activity);
    }

    public void setBlackOrWhite(ListPreference blackOrWhite) {
        this.blackOrWhite = blackOrWhite;
    }

    public void setAppList(MultiSelectListPreference appList) {
        this.appList = appList;
    }

    public void setAutoWhitelist(CheckBoxPreference autoWhitelist) {
        this.autoWhitelist = autoWhitelist;
    }

    @Override
    public void draw() {
        AppListTask task = new AppListTask(appList);
        task.execute();
        Preference.OnPreferenceChangeListener listener = new BlackListOnPreferenceChangeListener(appList, autoWhitelist);
        blackOrWhite.setOnPreferenceChangeListener(listener);
        listener.onPreferenceChange(blackOrWhite, blackOrWhite.getValue());
    }

    static class BlackListOnPreferenceChangeListener implements Preference.OnPreferenceChangeListener {

        private MultiSelectListPreference appList;
        private CheckBoxPreference autoWhitelist;

        public BlackListOnPreferenceChangeListener(MultiSelectListPreference appList, CheckBoxPreference autoWhitelist) {
            this.appList = appList;
            this.autoWhitelist = autoWhitelist;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String value = (String) newValue;
            boolean isBlackList = null != value && value.equals(Aurora.LIST_BLACK);
            appList.setTitle(appList.getContext().getString(
                    isBlackList
                            ? R.string.pref_update_list_black
                            : R.string.pref_update_list_white
            ));
            appList.setDialogTitle(appList.getTitle());
            preference.setSummary(appList.getContext().getString(
                    isBlackList
                            ? R.string.pref_update_list_white_or_black_black
                            : R.string.pref_update_list_white_or_black_white
            ));
            autoWhitelist.setEnabled(!isBlackList);
            return true;
        }
    }

    static class AppListTask extends InstalledAppsTask {

        private MultiSelectListPreference appList;
        private Map<String, String> appNames;

        public AppListTask(MultiSelectListPreference appList) {
            this.appList = appList;
            setContext(appList.getContext());
        }

        @Override
        protected void onPreExecute() {
            appList.setEntries(new String[0]);
            appList.setEntryValues(new String[0]);
        }

        @Override
        protected void onPostExecute(Map<String, App> installedApps) {
            int count = appNames.size();
            appList.setEntries(appNames.values().toArray(new String[count]));
            appList.setEntryValues(appNames.keySet().toArray(new String[count]));
        }

        @Override
        protected Map<String, App> doInBackground(String... strings) {
            Map<String, App> installedApps = super.doInBackground(strings);
            Map<String, String> appNames = new HashMap<>();
            for (String packageName : installedApps.keySet()) {
                appNames.put(packageName, installedApps.get(packageName).getDisplayName());
            }
            this.appNames = Util.sort(appNames);
            return installedApps;
        }
    }
}
