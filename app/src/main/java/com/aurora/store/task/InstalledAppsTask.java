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

package com.aurora.store.task;

import android.content.Context;
import android.text.TextUtils;

import com.aurora.store.model.App;
import com.aurora.store.utility.PackageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InstalledAppsTask extends UpdatableAppsTask {

    public InstalledAppsTask(Context context) {
        super(context);
    }

    public List<App> getInstalledApps(boolean removeSystemApps) throws Exception {
        List<App> appList = new ArrayList<>();
        List<String> packageList = getInstalledApps();
        if (removeSystemApps)
            packageList = filterSystemApps(packageList);
        packageList = filterBlacklistedApps(packageList);
        for (App app : getAppsFromPlayStore(packageList)) {
            final String packageName = app.getPackageName();
            if (TextUtils.isEmpty(packageName) || !packageList.contains(packageName)) {
                continue;
            }

            final App installedApp = getInstalledApp(packageName);
            app = addInstalledAppInfo(app, installedApp);
            appList.add(app);
        }
        return appList;
    }

    public List<App> getAllApps() throws Exception {
        List<App> appList = new ArrayList<>();
        List<String> packageList = getInstalledApps();
        for (App app : getAppsFromPlayStore(packageList)) {
            final String packageName = app.getPackageName();
            if (TextUtils.isEmpty(packageName) || !packageList.contains(packageName)) {
                continue;
            }

            final App installedApp = getInstalledApp(packageName);
            app = addInstalledAppInfo(app, installedApp);
            appList.add(app);
        }
        return appList;
    }

    private List<String> filterSystemApps(List<String> packageList) {
        List<String> newPackageList = new ArrayList<>();
        for (String packageName : packageList) {
            if (!PackageUtil.isSystemApp(getPackageManager(), packageName)) {
                newPackageList.add(packageName);
            }
        }
        return newPackageList;
    }
}