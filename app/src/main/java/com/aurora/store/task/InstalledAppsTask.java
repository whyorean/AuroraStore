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
import com.aurora.store.util.PackageUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.ArrayList;
import java.util.List;

public class InstalledAppsTask extends UpdatableAppsTask {

    public InstalledAppsTask(GooglePlayAPI api, Context context) {
        super(api, context);
    }

    public List<App> getInstalledApps() throws Exception {
        List<App> appList = new ArrayList<>();
        List<String> packageList = getLocalInstalledApps();
        packageList = filterBlacklistedApps(packageList);
        for (App app : getAppsFromPlayStore(packageList)) {
            final String packageName = app.getPackageName();

            if (TextUtils.isEmpty(packageName)) {
                continue;
            }

            final App installedApp = PackageUtil.getAppFromPackageName(getPackageManager(), packageName);
            app = addInstalledAppInfo(app, installedApp);
            appList.add(app);
        }
        return appList;
    }

    public List<App> getAllApps() throws Exception {
        List<App> appList = new ArrayList<>();
        List<String> packageList = getLocalInstalledApps();
        for (App app : getAppsFromPlayStore(packageList)) {
            final String packageName = app.getPackageName();

            if (TextUtils.isEmpty(packageName)) {
                continue;
            }

            final App installedApp = PackageUtil.getAppFromPackageName(getPackageManager(), packageName);
            app = addInstalledAppInfo(app, installedApp);
            appList.add(app);
        }
        return appList;
    }
}