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

package com.dragons.aurora.task.playstore;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.AuroraApplication;
import com.dragons.aurora.InstallerAbstract;
import com.dragons.aurora.InstallerFactory;
import com.dragons.aurora.Paths;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.UpdatableAppsActivity;
import com.dragons.aurora.downloader.DownloadManagerAdapter;
import com.dragons.aurora.downloader.DownloadManagerFactory;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.notification.NotificationManagerWrapper;
import com.dragons.aurora.recievers.UpdateAllReceiver;
import com.percolate.caffeine.PhoneUtils;

import java.util.List;

import timber.log.Timber;

public class BackgroundUpdatableAppsTask extends UpdatableAppsTask implements CloneableTask {

    private boolean forceUpdate = false;

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    @Override
    public CloneableTask clone() {
        BackgroundUpdatableAppsTask task = new BackgroundUpdatableAppsTask();
        task.setForceUpdate(forceUpdate);
        task.setContext(context);
        return task;
    }

    @Override
    protected void onPostExecute(List<App> apps) {
        super.onPostExecute(apps);
        if (!success()) {
            return;
        }
        int updatesCount = this.updatableApps.size();
        Timber.i("Found updates for " + updatesCount + " apps");
        if (updatesCount == 0) {
            context.sendBroadcast(new Intent(UpdateAllReceiver.ACTION_ALL_UPDATES_COMPLETE), null);
            return;
        }
        if (canUpdate()) {
            process(context, updatableApps);
        } else {
            notifyUpdatesFound(context, updatesCount);
        }
    }

    private boolean canUpdate() {
        boolean writePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writePermission = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        if (!writePermission) {
            Timber.i("Write permission not granted");
            return false;
        }
        return forceUpdate ||
                (PreferenceFragment.getBoolean(context, Aurora.PREFERENCE_BACKGROUND_UPDATE_DOWNLOAD)
                        && (DownloadManagerFactory.get(context) instanceof DownloadManagerAdapter
                        || !PreferenceFragment.getBoolean(context, Aurora.PREFERENCE_BACKGROUND_UPDATE_WIFI_ONLY)
                        || !PhoneUtils.isConnectedMobile(context)));
    }

    private void process(Context context, List<App> apps) {
        boolean canInstallInBackground = PreferenceFragment.canInstallInBackground(context);
        AuroraApplication application = (AuroraApplication) context.getApplicationContext();
        application.clearPendingUpdates();
        for (App app : apps) {
            application.addPendingUpdate(app.getPackageName());
            if (!Paths.getApkPath(context, app.getPackageName(), app.getVersionCode()).exists()) {
                download(context, app);
            } else if (canInstallInBackground) {
                // Not passing context because it might be an activity
                // and we want it to run in background
                InstallerFactory.get(context.getApplicationContext()).verifyAndInstall(app);
            } else {
                notifyDownloadedAlready(app);
                application.removePendingUpdate(app.getPackageName());
            }
        }
    }

    private void download(Context context, App app) {
        Timber.i("Starting download of update for %s", app.getPackageName());
        DownloadState state = DownloadState.get(app.getPackageName());
        state.setApp(app);
        getPurchaseTask(context, app).execute();
    }

    private BackgroundPurchaseTask getPurchaseTask(Context context, App app) {
        BackgroundPurchaseTask task = new BackgroundPurchaseTask();
        task.setApp(app);
        task.setContext(context);
        task.setTriggeredBy(context instanceof Activity
                ? DownloadState.TriggeredBy.UPDATE_ALL_BUTTON
                : DownloadState.TriggeredBy.SCHEDULED_UPDATE
        );
        return task;
    }

    private void notifyUpdatesFound(Context context, int updatesCount) {
        Intent i = new Intent(context, UpdatableAppsActivity.class);
        i.setAction(Intent.ACTION_VIEW);
        new NotificationManagerWrapper(context).show(i,
                context.getString(R.string.notification_updates_available_title),
                context.getString(R.string.notification_updates_available_message, updatesCount)
        );
    }

    private void notifyDownloadedAlready(App app) {
        new NotificationManagerWrapper(context).show(
                InstallerAbstract.getOpenApkIntent(context, Paths.getApkPath(context, app.getPackageName(), app.getVersionCode())),
                app.getDisplayName(),
                context.getString(R.string.notification_download_complete)
        );
    }
}
