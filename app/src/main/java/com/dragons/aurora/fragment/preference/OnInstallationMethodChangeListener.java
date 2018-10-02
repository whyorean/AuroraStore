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

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.BuildConfig;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.InstallerAurora;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.CheckShellTask;
import com.dragons.aurora.task.CheckSuTask;
import com.dragons.aurora.task.ConvertToSystemTask;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import static com.dragons.aurora.Util.isExtensionAvailable;

class OnInstallationMethodChangeListener implements Preference.OnPreferenceChangeListener {

    private PreferenceFragment activity;

    public OnInstallationMethodChangeListener(PreferenceFragment activity) {
        this.activity = activity;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String oldValue = ((ListPreference) preference).getValue();
        if (null != oldValue && !oldValue.equals(newValue)) {
            if (Aurora.INSTALLATION_METHOD_AURORA.equals(newValue)) {
                if (!isExtensionAvailable(activity.getActivity())) {
                    ContextUtil.toast(activity.getActivity(), R.string.pref_installation_method_aurora_unavailable);
                    return false;
                }
            } else if (Aurora.INSTALLATION_METHOD_PRIVILEGED.equals(newValue)) {
                if (!checkPrivileged()) {
                    return false;
                }
            } else if (Aurora.INSTALLATION_METHOD_ROOT.equals(newValue)) {
                new CheckSuTask(activity).execute();
            }
        }
        preference.setSummary(activity.getString(getInstallationMethodSummaryStringId((String) newValue)));
        return true;
    }

    private int getInstallationMethodSummaryStringId(String installationMethod) {
        if (null == installationMethod) {
            return R.string.pref_installation_method_default;
        }
        int summaryId;
        switch (installationMethod) {
            case Aurora.INSTALLATION_METHOD_AURORA:
                summaryId = R.string.pref_installation_method_aurora;
                break;
            case Aurora.INSTALLATION_METHOD_PRIVILEGED:
                summaryId = R.string.pref_installation_method_privileged;
                break;
            case Aurora.INSTALLATION_METHOD_ROOT:
                summaryId = R.string.pref_installation_method_root;
                break;
            default:
                summaryId = R.string.pref_installation_method_default;
                break;
        }
        return summaryId;
    }

    private boolean checkPrivileged() {
        boolean privileged = activity.getActivity().getPackageManager().checkPermission(Manifest.permission.INSTALL_PACKAGES, BuildConfig.APPLICATION_ID) == PackageManager.PERMISSION_GRANTED;
        if (!privileged) {
            new LocalCheckSuTask(activity).execute();
        }
        return privileged;
    }

    static class LocalCheckSuTask extends CheckSuTask {

        public LocalCheckSuTask(PreferenceFragment activity) {
            super(activity);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!available) {
                ContextUtil.toast(fragment.getActivity(), R.string.pref_not_privileged);
                return;
            }
            showPrivilegedInstallationDialog();
        }

        private void showPrivilegedInstallationDialog() {
            CheckShellTask checkShellTask = new CheckShellTask(fragment.getActivity());
            checkShellTask.setPrimaryTask(new ConvertToSystemTask(fragment.getActivity(), getSelf()));
            checkShellTask.execute();
        }

        private App getSelf() {
            PackageInfo Aurora = new PackageInfo();
            Aurora.applicationInfo = fragment.getActivity().getApplicationInfo();
            Aurora.packageName = BuildConfig.APPLICATION_ID;
            Aurora.versionCode = BuildConfig.VERSION_CODE;
            return new App(Aurora);
        }
    }
}
