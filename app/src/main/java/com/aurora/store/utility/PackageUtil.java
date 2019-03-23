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

package com.aurora.store.utility;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;

import com.aurora.store.model.App;

import java.util.Map;

public class PackageUtil {

    public static final String PSEUDO_PACKAGE_MAP = "PSEUDO_PACKAGE_MAP";

    public static String getDisplayName(Context context, String packageName) {
        Map<String, String> pseudoMap = getPseudoPackageMap(context);
        return TextUtil.emptyIfNull(pseudoMap.get(packageName));
    }

    public static Map<String, String> getPseudoPackageMap(Context context) {
        return PrefUtil.getMap(context, PSEUDO_PACKAGE_MAP);
    }

    public static void addToPseudoPackageMap(Context context, String packageName, String displayName) {
        Map<String, String> pseudoMap = getPseudoPackageMap(context);
        pseudoMap.put(packageName, displayName);
        PrefUtil.saveMap(context, pseudoMap, PSEUDO_PACKAGE_MAP);
    }

    public static App getInstalledApp(PackageManager packageManager, String packageName) {
        try {
            App app = new App(packageManager.getPackageInfo(packageName,
                    PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS));
            app.setDisplayName(packageManager.getApplicationLabel(
                    app.getPackageInfo().applicationInfo).toString());
            return app;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Nullable
    public static String getAppLabel(Context c, String packageName) {
        try {
            PackageManager pm = c.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isInstalled(Context context, App app) {
        try {
            context.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
