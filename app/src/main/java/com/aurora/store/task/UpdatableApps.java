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

import com.aurora.store.utility.Accountant;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.model.App;
import com.aurora.store.model.AppBuilder;
import com.aurora.store.utility.PrefUtil;
import com.dragons.aurora.playstoreapiv2.BulkDetailsEntry;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpdatableApps extends AllApps {

    public UpdatableApps(Context context) {
        super(context);
    }

    public List<App> getUpdatableApps() throws IOException {
        List<App> updatableApps = new ArrayList<>();
        api = getApi();
        api.toc();
        updatableApps.clear();
        Map<String, App> installedApps = getInstalledApps(context);
        for (App appFromMarket : getAppsFromPlayStore(api, filterBlacklistedApps(installedApps).keySet())) {
            String packageName = appFromMarket.getPackageName();
            if (TextUtils.isEmpty(packageName) || !installedApps.containsKey(packageName)) {
                continue;
            }
            App installedApp = installedApps.get(packageName);
            appFromMarket = addInstalledAppInfo(appFromMarket, installedApp);
            if (installedApp != null && installedApp.getVersionCode() < appFromMarket.getVersionCode()) {
                updatableApps.add(appFromMarket);
            }
        }
        return updatableApps;
    }

    public List<App> getAppsFromPlayStore(GooglePlayAPI api, Collection<String> packageNames) throws IOException {
        List<App> appsFromPlayStore = new ArrayList<>();
        boolean builtInAccount = PrefUtil.getBoolean(context, Accountant.DUMMY_ACCOUNT);
        for (App app : getRemoteAppList(api, new ArrayList<>(packageNames))) {
            if (!builtInAccount || app.isFree()) {
                appsFromPlayStore.add(app);
            }
        }
        return appsFromPlayStore;
    }

    private List<App> getRemoteAppList(GooglePlayAPI api, List<String> packageNames) throws IOException {
        List<App> apps = new ArrayList<>();
        for (BulkDetailsEntry details : api.bulkDetails(packageNames).getEntryList()) {
            if (!details.hasDoc()) {
                continue;
            }
            apps.add(AppBuilder.build(details.getDoc()));
        }
        Collections.sort(apps);
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

    private Map<String, App> filterBlacklistedApps(Map<String, App> apps) {
        Set<String> packageNames = new HashSet<>(apps.keySet());
        packageNames.removeAll(new BlacklistManager(context).getBlacklistedApps());
        Map<String, App> result = new HashMap<>();
        for (App app : apps.values()) {
            if (packageNames.contains(app.getPackageName())) {
                result.put(app.getPackageName(), app);
            }
        }
        return result;
    }
}