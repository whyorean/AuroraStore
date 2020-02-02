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

import com.aurora.store.events.Event;
import com.aurora.store.events.RxBus;
import com.aurora.store.installer.Installer;
import com.aurora.store.installer.InstallerService;
import com.aurora.store.model.App;
import com.aurora.store.util.Log;
import com.aurora.store.util.Util;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.plugins.RxJavaPlugins;

public class AuroraApplication extends Application {

    public static GooglePlayAPI api = null;

    private static RxBus rxBus = null;
    private static boolean bulkUpdateAlive = false;
    private static List<App> ongoingUpdateList = new ArrayList<>();

    @SuppressLint("StaticFieldLeak")
    private static Installer installer;

    public static RxBus getRxBus() {
        return rxBus;
    }

    public static boolean isBulkUpdateAlive() {
        return bulkUpdateAlive;
    }

    public static void setBulkUpdateAlive(boolean updating) {
        AuroraApplication.bulkUpdateAlive = updating;
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
            setBulkUpdateAlive(false);
    }

    public static Installer getInstaller() {
        return installer;
    }

    public static void rxNotify(Event event) {
        rxBus.getBus().accept(event);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        rxBus = new RxBus();
        installer = new Installer(this);

        //Clear all old installation sessions.
        Util.clearOldInstallationSessions(this);

        //Register global install broadcast receiver.
        registerReceiver(installer.getPackageInstaller().getBroadcastReceiver(),
                new IntentFilter(InstallerService.ACTION_INSTALLATION_STATUS_NOTIFICATION));

        //Global RX-Error handler, just simply logs, I make sure all errors are handled at origin.
        RxJavaPlugins.setErrorHandler(err -> Log.i(err.getMessage()));
    }

    @Override
    public void onTerminate() {
        try {
            unregisterReceiver(installer.getPackageInstaller().getBroadcastReceiver());
        } catch (Exception ignored) {
        }
        super.onTerminate();
    }
}
