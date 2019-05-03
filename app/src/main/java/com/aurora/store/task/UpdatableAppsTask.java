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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.model.App;
import com.aurora.store.model.AppBuilder;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.PackageUtil;
import com.dragons.aurora.playstoreapiv2.BulkDetailsEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UpdatableAppsTask extends AllAppsTask {

    public UpdatableAppsTask(Context context) {
        super(context);
    }

    public List<App> getUpdatableApps() throws IOException {
        List<App> appList = new ArrayList<>();
        List<String> packageList = getInstalledApps();
        packageList = filterBlacklistedApps(packageList);
        for (App app : getAppsFromPlayStore(packageList)) {
            final String packageName = app.getPackageName();
            if (TextUtils.isEmpty(packageName) || !packageList.contains(packageName)) {
                continue;
            }
            App installedApp = getInstalledApp(packageName);
            app = addInstalledAppInfo(app, installedApp);

            if (installedApp != null && installedApp.getVersionCode() < app.getVersionCode()) {
                appList.add(app);
            }
        }
        return appList;
    }

    public List<App> getAppsFromPlayStore(List<String> packageNames) throws IOException {
        List<App> appsFromPlayStore = new ArrayList<>();
        boolean builtInAccount = Accountant.isDummy(context);
        for (App app : getRemoteAppList(packageNames)) {
            if (!builtInAccount || app.isFree()) {
                appsFromPlayStore.add(app);
            }
        }
        return appsFromPlayStore;
    }

    private List<App> getRemoteAppList(List<String> packageNames) throws IOException {
        List<App> apps = new ArrayList<>();
        for (BulkDetailsEntry details : getApi().bulkDetails(packageNames).getEntryList()) {
            if (!details.hasDoc()) {
                continue;
            }
            apps.add(AppBuilder.build(details.getDoc()));
        }
        return apps;
    }

    public App addInstalledAppInfo(App appFromMarket, App installedApp) {
        if (null != installedApp) {
            appFromMarket.setPackageInfo(installedApp.getPackageInfo());
            appFromMarket.setVersionName(installedApp.getVersionName());
            appFromMarket.setDisplayName(installedApp.getDisplayName());
            appFromMarket.setSystem(installedApp.isSystem());
            appFromMarket.setInstalled(true);
        }
        return appFromMarket;
    }

    public App getInstalledApp(String packageName) {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
            return PackageUtil.getInstalledApp(getPackageManager(), packageInfo.packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public List<String> filterBlacklistedApps(List<String> packageList) {
        packageList.removeAll(new BlacklistManager(context).get());
        return packageList;
    }
}