/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
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

package com.dragons.aurora.recievers;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.AuroraApplication;
import com.dragons.aurora.BlackWhiteListManager;
import com.dragons.aurora.InstallationState;
import com.dragons.aurora.Paths;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;

import java.io.File;

import timber.log.Timber;

public class GlobalInstallReceiver extends BroadcastReceiver {

    static public void updateDetails(boolean installed) {
        if (installed) {
            DetailsFragment.app.getPackageInfo().versionCode = DetailsFragment.app.getVersionCode();
            DetailsFragment.app.setInstalled(true);
        } else {
            DetailsFragment.app.getPackageInfo().versionCode = 0;
            DetailsFragment.app.setInstalled(false);
        }
    }

    static public boolean actionIsInstall(Intent intent) {
        return !TextUtils.isEmpty(intent.getAction())
                && (intent.getAction().equals(Intent.ACTION_PACKAGE_INSTALL)
                || intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)
                || intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)
                || intent.getAction().equals(DetailsInstallReceiver.ACTION_PACKAGE_REPLACED_NON_SYSTEM)
        )
                ;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    static private boolean expectedAction(String action) {
        return action.equals(Intent.ACTION_PACKAGE_INSTALL)
                || action.equals(Intent.ACTION_PACKAGE_ADDED)
                || action.equals(Intent.ACTION_PACKAGE_REPLACED)
                || action.equals(Intent.ACTION_PACKAGE_REMOVED)
                || action.equals(Intent.ACTION_PACKAGE_FULLY_REMOVED)
                || action.equals(DetailsInstallReceiver.ACTION_PACKAGE_REPLACED_NON_SYSTEM)
                ;
    }

    static private boolean needToRemoveApk(Context context) {
        return PreferenceFragment.getBoolean(context, Aurora.PREFERENCE_DELETE_APK_AFTER_INSTALL);
    }

    static private boolean needToAutoWhitelist(Context context) {
        return PreferenceFragment.getBoolean(context, Aurora.PREFERENCE_AUTO_WHITELIST);
    }

    static private App getApp(Context context, String packageName) {
        App app = new App();
        PackageManager pm = context.getPackageManager();
        try {
            app = new App(pm.getPackageInfo(packageName, PackageManager.GET_META_DATA));
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e("Install broadcast received, but package " + packageName + " not found");
        }
        return app;
    }

    static private boolean wasInstalled(Context context, String packageName) {
        return InstallationState.isInstalled(packageName)
                || (PreferenceFragment.getString(context, Aurora.INSTALLATION_METHOD_DEFAULT).equals(Aurora.INSTALLATION_METHOD_DEFAULT)
                && DownloadState.get(packageName).isEverythingFinished()
        )
                ;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!expectedAction(action) || null == intent.getData()) {
            return;
        }
        String packageName = intent.getData().getSchemeSpecificPart();
        Timber.i("Finished installation of %s", packageName);
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        BlackWhiteListManager manager = new BlackWhiteListManager(context);
        if (actionIsInstall(intent) && wasInstalled(context, packageName) && needToAutoWhitelist(context) && !manager.isBlack()) {
            Timber.i("Whitelisting %s", packageName);
            manager.add(packageName);
        }
        if (null != DetailsFragment.app && packageName.equals(DetailsFragment.app.getPackageName())) {
            updateDetails(actionIsInstall(intent));
        }
        ((AuroraApplication) context.getApplicationContext()).removePendingUpdate(packageName, actionIsInstall(intent));
        if (needToRemoveApk(context) && actionIsInstall(intent)) {
            App app = getApp(context, packageName);
            File apkPath = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
            boolean deleted = apkPath.delete();
            Timber.i("Removed " + apkPath + " successfully: " + deleted);
        }
    }
}
