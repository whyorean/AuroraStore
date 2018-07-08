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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.dragons.aurora.model.App;

import java.util.HashMap;
import java.util.Map;

public class InstalledAppsTask extends TaskWithProgress<Map<String, App>> {

    private static App getInstalledApp(PackageManager pm, String packageName) {
        try {
            App app = new App(pm.getPackageInfo(packageName, PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS));
            app.setDisplayName(pm.getApplicationLabel(app.getPackageInfo().applicationInfo).toString());
            return app;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public Map<String, App> getInstalledApps() {
        Map<String, App> installedApps = new HashMap<>();
        PackageManager pm = context.getPackageManager();
        for (PackageInfo reducedPackageInfo : pm.getInstalledPackages(0)) {
            if (null != reducedPackageInfo.applicationInfo && !reducedPackageInfo.applicationInfo.enabled) {
                continue;
            }
            App app = getInstalledApp(pm, reducedPackageInfo.packageName);
            if (null != app) {
                installedApps.put(app.getPackageName(), app);
            }
        }
        return installedApps;
    }

    @Override
    protected Map<String, App> doInBackground(String... strings) {
        return getInstalledApps();
    }
}
