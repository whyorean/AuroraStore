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

import android.content.Context;
import android.content.Intent;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.AuroraApplication;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.DeltaPatcherFactory;
import com.dragons.aurora.InstallerAbstract;
import com.dragons.aurora.InstallerFactory;
import com.dragons.aurora.Paths;
import com.dragons.aurora.R;
import com.dragons.aurora.downloader.DownloadManagerFactory;
import com.dragons.aurora.downloader.DownloadManagerInterface;
import com.dragons.aurora.downloader.DownloadReceiver;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.notification.NotificationManagerWrapper;

import java.io.File;

import timber.log.Timber;

public class GlobalDownloadReceiver extends DownloadReceiver {

    private NotificationManagerWrapper notificationManager;

    @Override
    protected void process(final Context c, final Intent i) {
        new Thread(() -> processInBackground(c, i)).start();
    }

    private void processInBackground(Context c, Intent i) {
        notificationManager = new NotificationManagerWrapper(c);
        App app = state.getApp();

        DownloadManagerInterface dm = DownloadManagerFactory.get(context);
        if (!dm.finished(downloadId)) {
            return;
        }
        boolean patchSuccess = false;
        if (isDelta(app)) {
            patchSuccess = DeltaPatcherFactory.get(context, app).patch();
            if (!patchSuccess) {
                Timber.e("Delta patching failed for %s", app.getPackageName());
                return;
            }
        }
        state.setFinished(downloadId);
        if (actionIs(i, DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED)) {
            return;
        } else if (dm.success(downloadId)) {
            state.setSuccessful(downloadId);
        } else {
            String error = dm.getError(downloadId);
            ContextUtil.toastLong(context.getApplicationContext(), error);
            notificationManager.show(new Intent(), app.getDisplayName(), error);
        }

        if (!state.isEverythingFinished() || !state.isEverythingSuccessful()) {
            return;
        }
        verifyAndInstall(app, state.getTriggeredBy());
        if (patchSuccess) {
            Intent patchingCompleteIntent = new Intent(ACTION_DELTA_PATCHING_COMPLETE);
            patchingCompleteIntent.putExtra(DownloadManagerInterface.EXTRA_DOWNLOAD_ID, downloadId);
            context.sendBroadcast(patchingCompleteIntent);
        }
    }

    private void verifyAndInstall(App app, DownloadState.TriggeredBy triggeredBy) {
        if (shouldInstall(triggeredBy)) {
            Timber.i("Launching installer for %s", app.getPackageName());
            InstallerAbstract installer = InstallerFactory.get(context);
            if (triggeredBy.equals(DownloadState.TriggeredBy.DOWNLOAD_BUTTON)
                    && PreferenceFragment.getBoolean(context, Aurora.PREFERENCE_AUTO_INSTALL)
            ) {
                installer.setBackground(false);
            }
            installer.verifyAndInstall(app);
        } else {
            Timber.i("Notifying about download completion of %s", app.getPackageName());
            notifyDownloadComplete(app);
            ((AuroraApplication) context.getApplicationContext()).removePendingUpdate(app.getPackageName());
        }
    }

    private void notifyDownloadComplete(App app) {
        notifyAndToast(
                R.string.notification_download_complete,
                R.string.notification_download_complete_toast,
                app
        );
    }

    private void notifyAndToast(int notificationStringId, int toastStringId, App app) {
        File file = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
        Intent openApkIntent = InstallerAbstract.getOpenApkIntent(context, file);
        notificationManager.show(
                openApkIntent,
                app.getDisplayName(),
                context.getString(notificationStringId)
        );
        ContextUtil.toast(context.getApplicationContext(), toastStringId, app.getDisplayName());
    }

    private boolean shouldInstall(DownloadState.TriggeredBy triggeredBy) {
        switch (triggeredBy) {
            case DOWNLOAD_BUTTON:
                return Prefs.getBoolean(context, Aurora.PREFERENCE_AUTO_INSTALL);
            case UPDATE_ALL_BUTTON:
                return PreferenceFragment.canInstallInBackground(context);
            case SCHEDULED_UPDATE:
                return PreferenceFragment.canInstallInBackground(context) && Prefs.getBoolean(context, Aurora.PREFERENCE_BACKGROUND_UPDATE_INSTALL);
            case MANUAL_DOWNLOAD_BUTTON:
            default:
                return false;
        }
    }
}
