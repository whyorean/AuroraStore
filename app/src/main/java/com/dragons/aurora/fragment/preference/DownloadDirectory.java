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
import android.content.pm.PackageManager;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.AuroraPermissionManager;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.Paths;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.PreferenceFragment;

import java.io.File;
import java.io.IOException;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import timber.log.Timber;

public class DownloadDirectory extends Abstract {

    private EditTextPreference preference;

    public DownloadDirectory(PreferenceFragment activity) {
        super(activity);
    }

    public DownloadDirectory setPreference(EditTextPreference preference) {
        this.preference = preference;
        return this;
    }

    @Override
    public void draw() {
        preference.setSummary(Paths.getDownloadPath(activity.getActivity()).getAbsolutePath());
        preference.setOnPreferenceClickListener(preference -> {
            AuroraPermissionManager permissionManager = new AuroraPermissionManager(activity.getActivity());
            if (!permissionManager.checkPermission()) {
                permissionManager.requestPermission();
            }
            return true;
        });
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                String newValue = (String) o;
                boolean result = checkNewValue(newValue);
                if (!result) {
                    if (ContextUtil.isAlive(activity.getActivity()) && !((EditTextPreference) preference).getText().equals(Aurora.FALLBACK_DIRECTORY)) {
                        getFallbackDialog().show();
                    } else {
                        ContextUtil.toast(activity.getActivity(), R.string.error_downloads_directory_not_writable);
                    }
                } else {
                    try {
                        preference.setSummary(new File(Paths.getStorageRoot(activity.getActivity()), newValue).getCanonicalPath());
                    } catch (IOException e) {
                        Timber.i("checkNewValue returned true, but drawing the path \"" + newValue + "\" in the summary failed... strange");
                        return false;
                    }
                }
                return result;
            }

            private boolean checkNewValue(String newValue) {
                try {
                    File storageRoot = Paths.getStorageRoot(activity.getActivity());
                    File newDir = new File(storageRoot, newValue).getCanonicalFile();
                    if (!newDir.getCanonicalPath().startsWith(storageRoot.getCanonicalPath())) {
                        return false;
                    }
                    if (newDir.exists()) {
                        return newDir.canWrite();
                    }
                    if (activity.getActivity().checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        return newDir.mkdirs();
                    }
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }

            private AlertDialog getFallbackDialog() {
                return new AlertDialog.Builder(activity.getActivity())
                        .setMessage(
                                activity.getString(R.string.error_downloads_directory_not_writable)
                                        + "\n\n"
                                        + activity.getString(R.string.pref_message_fallback, Aurora.FALLBACK_DIRECTORY)
                        )
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            preference.setText(Aurora.FALLBACK_DIRECTORY);
                            preference.getOnPreferenceChangeListener().onPreferenceChange(preference, Aurora.FALLBACK_DIRECTORY);
                            dialog.dismiss();
                        })
                        .create();
            }
        });
    }
}
