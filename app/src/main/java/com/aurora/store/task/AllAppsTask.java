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

import com.aurora.store.Constants;
import com.aurora.store.util.CertUtil;
import com.aurora.store.util.Util;

import java.util.ArrayList;
import java.util.List;

public class AllAppsTask {

    private Context context;
    private PackageManager packageManager;
    private boolean fDroidFilterEnabled;
    private boolean isExtendedUpdatesEnabled;

    public AllAppsTask(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        this.fDroidFilterEnabled = Util.filterFDroidAppsEnabled(context);
        this.isExtendedUpdatesEnabled = Util.isExtendedUpdatesEnabled(context);
    }

    public PackageManager getPackageManager() {
        return packageManager;
    }

    public Context getContext() {
        return context;
    }

    List<String> getLocalInstalledApps() {
        List<String> packageList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        for (PackageInfo packageInfo : packageManager.getInstalledPackages(PackageManager.GET_META_DATA)) {
            final String packageName = packageInfo.packageName;
            if (packageInfo.applicationInfo != null
                    && !packageInfo.applicationInfo.enabled
                    && !isExtendedUpdatesEnabled)
                continue;
            String packageInstaller = packageManager.getInstallerPackageName(packageName);
            if (fDroidFilterEnabled && packageInstaller != null
                    && packageInstaller.equals(Constants.PRIVILEGED_EXTENSION_PACKAGE_NAME_FDROID))
                continue;
            if (fDroidFilterEnabled && CertUtil.isFDroidApp(context, packageName))
                continue;
            packageList.add(packageName);
        }
        return packageList;
    }
}
