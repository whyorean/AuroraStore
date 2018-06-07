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

package com.dragons.aurora.task;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;

import com.dragons.aurora.BlackWhiteListManager;
import com.dragons.aurora.fragment.AppListFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListValidityCheckTask extends AsyncTask<String, Void, Set<String>> {

    private AppListFragment fragment;
    private boolean includeSystemApps = false;
    private boolean respectUpdateBlacklist = false;

    public AppListValidityCheckTask(AppListFragment fragment) {
        this.fragment = fragment;
    }

    public void setIncludeSystemApps(boolean includeSystemApps) {
        this.includeSystemApps = includeSystemApps;
    }

    public void setRespectUpdateBlacklist(boolean respectUpdateBlacklist) {
        this.respectUpdateBlacklist = respectUpdateBlacklist;
    }

    @Override
    protected void onPostExecute(Set<String> installedPackageNames) {
        super.onPostExecute(installedPackageNames);
        Set<String> newPackageNames = new HashSet<>(installedPackageNames);
        /*newPackageNames.removeAll(fragment.getListedPackageNames());
        if (!respectUpdateBlacklist && newPackageNames.size() > 0) {
            //activity.loadInstalledApps();
            return;
        }
        Set<String> removedPackageNames = new HashSet<>(fragment.getListedPackageNames());
        removedPackageNames.removeAll(installedPackageNames);
        for (String packageName : removedPackageNames) {
            fragment.removeApp(packageName);
        }*/
    }

    @Override
    protected Set<String> doInBackground(String... strings) {
        Set<String> installedApps = new HashSet<>();
        List<PackageInfo> installedPackages = new ArrayList<>();
        try {
            installedPackages.addAll(fragment.getActivity().getPackageManager().getInstalledPackages(0));
        } catch (RuntimeException e) {
            // Sometimes TransactionTooLargeException is thrown even though getInstalledPackages is
            // called with 0 flags. App list validity check is not essential, so this can be ignored
            // TODO: There might be a way to avoid this exception, although I doubt it
        }
        BlackWhiteListManager manager = new BlackWhiteListManager(fragment.getActivity());
        for (PackageInfo reducedPackageInfo : installedPackages) {
            if (!includeSystemApps
                    && null != reducedPackageInfo.applicationInfo
                    && (reducedPackageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    ) {
                continue;
            }
            if (respectUpdateBlacklist && !manager.isUpdatable(reducedPackageInfo.packageName)) {
                continue;
            }
            installedApps.add(reducedPackageInfo.packageName);
        }
        return installedApps;
    }
}
