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

import com.aurora.store.model.App;
import com.aurora.store.utility.CertUtil;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.utility.Util;

import java.util.HashMap;
import java.util.Map;

public class AllApps extends BaseTask {

    public AllApps(Context context) {
        super(context);
    }

    Map<String, App> getInstalledApps(Context context) {
        Map<String, App> installedApps = new HashMap<>();
        PackageManager pm = context.getPackageManager();
        for (PackageInfo packageInfo : pm.getInstalledPackages(0)) {
            if (null != packageInfo.applicationInfo && !packageInfo.applicationInfo.enabled) {
                continue;
            }
            App app = PackageUtil.getInstalledApp(pm, packageInfo.packageName);
            if (null != app) {
                if (Util.filterFDroidAppsEnabled(context) && CertUtil.isFDroidApp(context, app.getPackageName()))
                    continue;
                installedApps.put(app.getPackageName(), app);
            }
        }
        return installedApps;
    }
}
