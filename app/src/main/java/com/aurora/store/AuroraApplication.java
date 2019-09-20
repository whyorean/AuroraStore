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

package com.aurora.store;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.IntentFilter;

import com.aurora.store.installer.Installer;
import com.aurora.store.installer.InstallerService;
import com.aurora.store.installer.Uninstaller;
import com.aurora.store.model.App;
import com.aurora.store.utility.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.plugins.RxJavaPlugins;

public class AuroraApplication extends Application {

    public static boolean tokenRefreshing = false;
    public static boolean anonymousLogging = false;
    public static boolean updating = false;
    public static List<App> ongoingUpdateList = new ArrayList<>();

    @SuppressLint("StaticFieldLeak")
    public static Installer installer;
    @SuppressLint("StaticFieldLeak")
    public static Uninstaller uninstaller;

    public static boolean getOnGoingUpdate() {
        return updating;
    }

    public static void setOnGoingUpdate(boolean updating) {
        AuroraApplication.updating = updating;
    }

    public static List<App> getOngoingUpdateList() {
        return ongoingUpdateList;
    }

    public static void setOngoingUpdateList(List<App> ongoingUpdateList) {
        AuroraApplication.ongoingUpdateList = ongoingUpdateList;
    }

    public static void removeFromOngoingUpdateList(String packageName) {
        Iterator<App> iterator = ongoingUpdateList.iterator();
        while (iterator.hasNext()) {
            if (packageName.equals(iterator.next().getPackageName()))
                iterator.remove();
        }
        if (ongoingUpdateList.isEmpty())
            setOnGoingUpdate(false);
    }

    public static Installer getInstaller() {
        return installer;
    }

    public static Uninstaller getUninstaller() {
        return uninstaller;
    }

    public static boolean isAnonymousLogging() {
        return anonymousLogging;
    }

    public static void setAnonymousLogging(boolean anonymousLogging) {
        AuroraApplication.anonymousLogging = anonymousLogging;
    }

    public static boolean isTokenRefreshing() {
        return tokenRefreshing;
    }

    public static void setTokenRefreshing(boolean tokenRefreshing) {
        AuroraApplication.tokenRefreshing = tokenRefreshing;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        installer = new Installer(this);
        uninstaller = new Uninstaller(this);
        Util.clearOldInstallationSessions(this);
        registerReceiver(installer.getPackageInstaller().getBroadcastReceiver(),
                new IntentFilter(InstallerService.ACTION_INSTALLATION_STATUS_NOTIFICATION));
        RxJavaPlugins.setErrorHandler(err -> {
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        try {
            unregisterReceiver(installer.getPackageInstaller().getBroadcastReceiver());
        } catch (Exception ignored) {
        }
    }
}
