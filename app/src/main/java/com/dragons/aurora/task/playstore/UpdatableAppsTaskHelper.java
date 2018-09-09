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

package com.dragons.aurora.task.playstore;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.BlackWhiteListManager;
import com.dragons.aurora.BuildConfig;
import com.dragons.aurora.CertUtils;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.AppBuilder;
import com.dragons.aurora.playstoreapiv2.BulkDetailsEntry;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.InstalledAppsTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class UpdatableAppsTaskHelper extends ExceptionTask {

    public UpdatableAppsTaskHelper(Context context) {
        super(context);
    }

    public List<App> getUpdatableApps() {
        List<App> updatableApps = new ArrayList<>();
        try {
            api = getApi();
            api.toc();
            updatableApps.clear();
            Map<String, App> installedApps = getInstalledApps();
            for (App appFromMarket : getAppsFromPlayStore(api, filterBlacklistedApps(installedApps).keySet())) {
                String packageName = appFromMarket.getPackageName();
                if (TextUtils.isEmpty(packageName) || !installedApps.containsKey(packageName)) {
                    continue;
                }
                if (CertUtils.isFDroidApp(getContext(), packageName))
                    continue;
                App installedApp = installedApps.get(packageName);
                appFromMarket = addInstalledAppInfo(appFromMarket, installedApp);
                if (installedApp != null && installedApp.getVersionCode() < appFromMarket.getVersionCode()) {
                    updatableApps.add(appFromMarket);
                }
            }
            if (!new BlackWhiteListManager(context).isUpdatable(BuildConfig.APPLICATION_ID)) {
                return updatableApps;
            }
        } catch (IOException e) {
            processException(e);
        }
        return updatableApps;
    }

    protected List<App> getAppsFromPlayStore(GooglePlayAPI api, Collection<String> packageNames) throws IOException {
        List<App> appsFromPlayStore = new ArrayList<>();
        boolean builtInAccount = PreferenceFragment.getBoolean(context, Aurora.PREFERENCE_APP_PROVIDED_EMAIL);
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

    protected Map<String, App> getInstalledApps() {
        InstalledAppsTask task = new InstalledAppsTask();
        task.setContext(getContext());
        return task.getInstalledApps();
    }

    protected App addInstalledAppInfo(App appFromMarket, App installedApp) {
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
        if (Objects.equals(PreferenceManager.getDefaultSharedPreferences(context).getString(
                Aurora.PREFERENCE_UPDATE_LIST_WHITE_OR_BLACK,
                Aurora.LIST_BLACK), Aurora.LIST_BLACK)) {
            packageNames.removeAll(new BlackWhiteListManager(context).get());
        } else {
            packageNames.retainAll(new BlackWhiteListManager(context).get());
        }
        Map<String, App> result = new HashMap<>();
        for (App app : apps.values()) {
            if (packageNames.contains(app.getPackageName())) {
                result.put(app.getPackageName(), app);
            }
        }
        return result;
    }
}