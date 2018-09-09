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

package com.dragons.aurora;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.http.HttpResponseCache;
import android.os.Handler;
import android.os.HandlerThread;

import com.dragons.aurora.downloader.DownloadManagerInterface;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.recievers.DetailsInstallReceiver;
import com.dragons.aurora.recievers.GlobalDownloadReceiver;
import com.dragons.aurora.recievers.GlobalInstallReceiver;
import com.dragons.aurora.recievers.UpdateAllReceiver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.preference.PreferenceManager;
import timber.log.Timber;

public class AuroraApplication extends Application {

    public static boolean COLOR_UI;

    private boolean isBackgroundUpdating = false;
    private List<String> pendingUpdates = new ArrayList<>();

    public boolean isBackgroundUpdating() {
        return isBackgroundUpdating;
    }

    public void setBackgroundUpdating(boolean backgroundUpdating) {
        isBackgroundUpdating = backgroundUpdating;
    }

    public void addPendingUpdate(String packageName) {
        pendingUpdates.add(packageName);
    }

    public void removePendingUpdate(String packageName) {
        removePendingUpdate(packageName, false);
    }

    public void removePendingUpdate(String packageName, boolean installed) {
        pendingUpdates.remove(packageName);
        Intent appIntent = new Intent(UpdateAllReceiver.ACTION_APP_UPDATE_COMPLETE);
        appIntent.putExtra(UpdateAllReceiver.EXTRA_PACKAGE_NAME, packageName);
        appIntent.putExtra(UpdateAllReceiver.EXTRA_UPDATE_ACTUALLY_INSTALLED, installed);
        sendBroadcast(appIntent, null);
        if (pendingUpdates.isEmpty()) {
            isBackgroundUpdating = false;
            Intent allIntent = new Intent(UpdateAllReceiver.ACTION_ALL_UPDATES_COMPLETE);
            sendBroadcast(allIntent, null);
        }
    }

    public void clearPendingUpdates() {
        pendingUpdates.clear();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new AuroraLogTree());
        try {
            HttpResponseCache.install(new File(getCacheDir(), "http"), 5 * 1024 * 1024);
        } catch (IOException e) {
            Timber.e("Could not register cache %s", e.getMessage());
        }

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        registerDownloadReceiver();
        registerInstallReceiver();
        getSavedPrefs();
    }

    private void registerDownloadReceiver() {
        HandlerThread handlerThread = new HandlerThread("handlerThread");
        handlerThread.start();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED);
        filter.addAction(DownloadManagerInterface.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(new GlobalDownloadReceiver(), filter, null, new Handler(handlerThread.getLooper()));
    }

    private void registerInstallReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction(Intent.ACTION_INSTALL_PACKAGE);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        filter.addAction(DetailsInstallReceiver.ACTION_PACKAGE_REPLACED_NON_SYSTEM);
        registerReceiver(new GlobalInstallReceiver(), filter);
    }

    public boolean isTv() {
        int uiMode = getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    public void getSavedPrefs() {
        COLOR_UI = Prefs.getBoolean(getApplicationContext(), Aurora.PREFERENCE_COLOR_UI);
    }
}
