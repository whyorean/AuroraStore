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
import android.text.TextUtils;

import com.dragons.aurora.CertUtils;
import com.dragons.aurora.model.App;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstalledAppsTaskHelper extends UpdatableAppsTaskHelper {

    public InstalledAppsTaskHelper(Context context) {
        super(context);
    }

    public List<App> getInstalledApps(boolean removeSystem) {
        List<App> allMarketApps = new ArrayList<>();
        try {
            api = getApi();
            api.toc();
            allMarketApps.clear();
            Map<String, App> installedApps = getInstalledApps();
            if (removeSystem)
                installedApps = filterSystemApps(installedApps);
            for (App appFromMarket : getAppsFromPlayStore(api, installedApps.keySet())) {
                String packageName = appFromMarket.getPackageName();
                if (TextUtils.isEmpty(packageName) || !installedApps.containsKey(packageName)) {
                    continue;
                }
                if (CertUtils.isFDroidApp(getContext(), packageName))
                    continue;
                App installedApp = installedApps.get(packageName);
                appFromMarket = addInstalledAppInfo(appFromMarket, installedApp);
                allMarketApps.add(appFromMarket);
            }
        } catch (IOException e) {
            processException(e);
        }
        return allMarketApps;
    }

    private Map<String, App> filterSystemApps(Map<String, App> apps) {
        Map<String, App> installedApps = new HashMap<>();
        for (App app : apps.values()) {
            if (!app.isSystem()) {
                installedApps.put(app.getPackageName(), app);
            }
        }
        return installedApps;
    }
}