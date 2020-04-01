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

import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.MalformedRequestException;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.model.App;
import com.aurora.store.model.AppBuilder;
import com.aurora.store.util.Log;
import com.aurora.store.util.PackageUtil;
import com.dragons.aurora.playstoreapiv2.BulkDetailsEntry;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.ArrayList;
import java.util.List;

public class UpdatableAppsTask extends AllAppsTask {

    private GooglePlayAPI api;

    public UpdatableAppsTask(GooglePlayAPI api, Context context) {
        super(context);
        this.api = api;
    }

    public List<App> getUpdatableApps() throws Exception {
        List<App> appList = new ArrayList<>();
        List<String> packageList = getLocalInstalledApps();
        packageList = filterBlacklistedApps(packageList);
        for (App app : getAppsFromPlayStore(packageList)) {
            final String packageName = app.getPackageName();
            if (TextUtils.isEmpty(packageName)) {
                continue;
            }

            final App installedApp = PackageUtil.getAppFromPackageName(getPackageManager(), packageName, false);
            app = addInstalledAppInfo(app, installedApp);
            if (installedApp != null && installedApp.getVersionCode() < app.getVersionCode()) {
                appList.add(app);
            }
        }
        return appList;
    }

    public List<App> getAppsFromPlayStore(List<String> packageNames) throws Exception {
        return new ArrayList<>(getRemoteAppList(packageNames));
    }

    private List<App> getRemoteAppList(List<String> packageNames) throws Exception {
        final List<App> appList = new ArrayList<>();
        try {
            final List<BulkDetailsEntry> bulkDetailsEntries = api.bulkDetails(packageNames).getEntryList();
            for (BulkDetailsEntry bulkDetailsEntry : bulkDetailsEntries) {
                if (!bulkDetailsEntry.hasDoc()) {
                    continue;
                }
                appList.add(AppBuilder.build(bulkDetailsEntry.getDoc()));
            }
        } catch (Exception e) {
            if (e instanceof MalformedRequestException) {
                Log.e("Malformed Request : %s", e.getMessage());
            } else if (e instanceof NullPointerException)
                throw new CredentialsEmptyException();
            else
                throw e;
        }
        return appList;
    }

    public App addInstalledAppInfo(App appFromMarket, App installedApp) {
        if (installedApp != null) {
            appFromMarket.setDisplayName(installedApp.getDisplayName());
            appFromMarket.setSystem(installedApp.isSystem());
        }
        return appFromMarket;
    }

    public List<String> filterBlacklistedApps(List<String> packageList) {
        packageList.removeAll(new BlacklistManager(getContext()).getBlacklistedPackages());
        return packageList;
    }
}