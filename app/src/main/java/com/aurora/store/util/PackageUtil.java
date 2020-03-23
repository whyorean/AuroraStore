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

package com.aurora.store.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.aurora.store.model.App;

import java.util.Map;

public class PackageUtil {

    private static final String ACTION_PACKAGE_REPLACED_NON_SYSTEM = "ACTION_PACKAGE_REPLACED_NON_SYSTEM";
    private static final String ACTION_PACKAGE_INSTALLATION_FAILED = "ACTION_PACKAGE_INSTALLATION_FAILED";
    private static final String ACTION_UNINSTALL_PACKAGE_FAILED = "ACTION_UNINSTALL_PACKAGE_FAILED";

    private static final String PSEUDO_PACKAGE_MAP = "PSEUDO_PACKAGE_MAP";
    private static final String PSEUDO_URL_MAP = "PSEUDO_URL_MAP";

    public static String getDisplayName(Context context, String packageName) {
        Map<String, String> pseudoMap = getPseudoPackageMap(context);
        return TextUtil.emptyIfNull(pseudoMap.get(packageName));
    }

    public static String getIconURL(Context context, String packageName) {
        Map<String, String> pseudoMap = getPseudoURLMap(context);
        return TextUtil.emptyIfNull(pseudoMap.get(packageName));
    }

    private static Map<String, String> getPseudoPackageMap(Context context) {
        return PrefUtil.getMap(context, PSEUDO_PACKAGE_MAP);
    }

    private static Map<String, String> getPseudoURLMap(Context context) {
        return PrefUtil.getMap(context, PSEUDO_URL_MAP);
    }

    public static void addToPseudoPackageMap(Context context, String packageName, String displayName) {
        Map<String, String> pseudoMap = getPseudoPackageMap(context);
        pseudoMap.put(packageName, displayName);
        PrefUtil.saveMap(context, pseudoMap, PSEUDO_PACKAGE_MAP);
    }

    public static void addToPseudoURLMap(Context context, String packageName, String iconURL) {
        Map<String, String> pseudoMap = getPseudoURLMap(context);
        pseudoMap.put(packageName, iconURL);
        PrefUtil.saveMap(context, pseudoMap, PSEUDO_URL_MAP);
    }

    public static App getAppFromPackageName(PackageManager packageManager, String packageName, boolean extended) {
        try {
            final App app = new App();
            final PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            app.setPackageName(packageName);
            app.setDisplayName(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString());
            app.setVersionName(packageInfo.versionName);
            app.setVersionCode(packageInfo.versionCode);
            if (extended) {
                app.setSystem((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                app.setIconDrawable(packageManager.getApplicationIcon(packageName));
            }
            return app;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static boolean isSystemApp(PackageManager packageManager, String packageName) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isPackageLaunchable(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        return (packageManager.getLaunchIntentForPackage(packageName)) != null;
    }

    public static boolean isInstalled(Context context, App app) {
        try {
            context.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_INSTALL);
        filter.addAction(Intent.ACTION_UNINSTALL_PACKAGE);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(ACTION_PACKAGE_REPLACED_NON_SYSTEM);
        filter.addAction(ACTION_PACKAGE_INSTALLATION_FAILED);
        filter.addAction(ACTION_UNINSTALL_PACKAGE_FAILED);
        return filter;
    }
}
