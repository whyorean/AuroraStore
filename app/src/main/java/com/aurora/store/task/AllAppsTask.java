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

import com.aurora.store.utility.CertUtil;
import com.aurora.store.utility.Util;

import java.util.ArrayList;
import java.util.List;

public class AllAppsTask extends BaseTask {

    public AllAppsTask(Context context) {
        super(context);
    }

    List<String> getInstalledApps() {
        List<String> packageList = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        for (PackageInfo packageInfo : pm.getInstalledPackages(0)) {
            final String packageName = packageInfo.packageName;
            if (null != packageInfo.applicationInfo && !packageInfo.applicationInfo.enabled)
                continue;
            if (Util.filterFDroidAppsEnabled(context) && CertUtil.isFDroidApp(context, packageName))
                continue;
            packageList.add(packageName);
        }
        return packageList;
    }
}
