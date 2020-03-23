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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.aurora.store.events.Event;
import com.aurora.store.events.RxBus;
import com.aurora.store.installer.Installer;
import com.aurora.store.installer.InstallerService;
import com.aurora.store.model.App;
import com.aurora.store.report.AcraLogSenderFactory;
import com.aurora.store.report.AcraReportSenderFactory;
import com.aurora.store.util.Log;
import com.aurora.store.util.Util;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.facebook.stetho.Stetho;
import com.jakewharton.rxrelay2.Relay;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.file.Directory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.plugins.RxJavaPlugins;

@AcraCore(logcatArguments = {"-t", "200", "-v", "time"})
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

    public static Relay<Event> getRelayBus() {
        return rxBus.getBus();
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

    @SuppressWarnings("unchecked")
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this)
                .setReportSenderFactoryClasses(AcraReportSenderFactory.class, AcraLogSenderFactory.class)
                .setBuildConfigClass(BuildConfig.class)
                .setAlsoReportToAndroidFramework(false)
                .setApplicationLogFileDir(Directory.EXTERNAL_CACHE)
                .setApplicationLogFile("Aurora.log")
                .setReportFormat(StringFormat.KEY_VALUE_LIST);

        builder.setDeleteOldUnsentReportsOnApplicationStart(true);
        builder.setDeleteUnapprovedReportsOnApplicationStart(true);

        ACRA.init(this, builder);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        rxBus = new RxBus();
        installer = new Installer(this);

        //Create notification channels
        createNotificationChannel();

        //Clear all old installation sessions.
        Util.clearOldInstallationSessions(this);

        //Register global install broadcast receiver.
        registerReceiver(installer.getPackageInstaller().getBroadcastReceiver(),
                new IntentFilter(InstallerService.ACTION_INSTALLATION_STATUS_NOTIFICATION));

        //Global RX-Error handler, just simply logs, I make sure all errors are handled at origin.
        RxJavaPlugins.setErrorHandler(throwable -> {
            Log.e(throwable.getMessage());
            if (BuildConfig.DEBUG) {
                throwable.printStackTrace();
            }
        });

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);
        }
    }

    @Override
    public void onTerminate() {
        try {
            unregisterReceiver(installer.getPackageInstaller().getBroadcastReceiver());
        } catch (Exception ignored) {
        }
        super.onTerminate();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel alertChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ALERT,
                    getString(R.string.notification_channel_alert),
                    NotificationManager.IMPORTANCE_HIGH);

            NotificationChannel generalChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_GENERAL,
                    getString(R.string.notification_channel_general),
                    NotificationManager.IMPORTANCE_MIN);

            alertChannel.enableLights(true);
            alertChannel.enableVibration(true);
            alertChannel.setShowBadge(true);
            alertChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            generalChannel.enableLights(false);
            generalChannel.enableVibration(false);
            generalChannel.setShowBadge(false);
            generalChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(alertChannel);
                notificationManager.createNotificationChannel(generalChannel);
            }
        }
    }
}
