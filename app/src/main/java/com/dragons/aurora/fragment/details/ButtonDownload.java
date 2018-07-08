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

package com.dragons.aurora.fragment.details;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dragons.aurora.AuroraPermissionManager;
import com.dragons.aurora.BuildConfig;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.Paths;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.activities.ManualDownloadActivity;
import com.dragons.aurora.downloader.DownloadProgressBarUpdater;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.task.playstore.PurchaseTask;
import com.percolate.caffeine.ViewUtils;

import java.io.File;

import static com.dragons.aurora.downloader.DownloadState.TriggeredBy.DOWNLOAD_BUTTON;
import static com.dragons.aurora.downloader.DownloadState.TriggeredBy.MANUAL_DOWNLOAD_BUTTON;

public class ButtonDownload extends Button {

    private ProgressBar progressBar;
    private TextView progressCents;

    public ButtonDownload(AuroraActivity activity, App app) {
        super(activity, app);
    }

    @Override
    protected android.widget.Button getButton() {
        if (app.getPrice() != null && !app.isFree()) {
            setText(R.id.download, R.string.details_purchase);
            return (android.widget.Button) activity.findViewById(R.id.download);
        } else
            return (android.widget.Button) activity.findViewById(R.id.download);
    }

    @Override
    public boolean shouldBeVisible() {
        File apk = Paths.getApkPath(activity, app.getPackageName(), app.getVersionCode());
        return (!apk.exists() || apk.length() != app.getSize() || !DownloadState.get(app.getPackageName()).isEverythingSuccessful())
                && (app.isInPlayStore() || app.getPackageName().equals(BuildConfig.APPLICATION_ID))
                && (getInstalledVersionCode() != app.getVersionCode() || activity instanceof ManualDownloadActivity);
    }

    @Override
    protected void onButtonClick(View v) {
        checkAndDownload();
    }

    public void checkAndDownload() {
        View buttonDownload = activity.findViewById(R.id.download);
        View buttonCancel = activity.findViewById(R.id.cancel);

        if (null != buttonDownload) buttonDownload.setVisibility(View.GONE);

        AuroraPermissionManager permissionManager = new AuroraPermissionManager(activity);

        if (app.getVersionCode() == 0 && !(activity instanceof ManualDownloadActivity)) {
            activity.startActivity(new Intent(activity, ManualDownloadActivity.class));
        } else if (permissionManager.checkPermission()) {
            Log.i(getClass().getSimpleName(), "Write permission granted");
            download();
            if (null != buttonCancel) {
                buttonCancel.setVisibility(View.VISIBLE);
            }
        } else {
            permissionManager.requestPermission();
            button.setVisibility(View.GONE);
            button.setEnabled(false);
            buttonCancel.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void draw() {
        super.draw();
        DownloadState state = DownloadState.get(app.getPackageName());
        if (Paths.getApkPath(activity, app.getPackageName(), app.getVersionCode()).exists()
                && !state.isEverythingSuccessful()
                ) {
            progressBar = ViewUtils.findViewById(activity, R.id.download_progress);
            progressCents = ViewUtils.findViewById(activity, R.id.progressCents);
            if (null != progressBar && null != progressCents) {
                new DownloadProgressBarUpdater(app.getPackageName(), progressBar, progressCents).execute(PurchaseTask.UPDATE_INTERVAL);
            }
        }
    }

    public void download() {
        boolean writePermission = new AuroraPermissionManager(activity).checkPermission();
        Log.i(getClass().getSimpleName(), "Write permission granted - " + writePermission);
        if (writePermission && prepareDownloadsDir()) {
            getPurchaseTask().execute();
        } else {
            File dir = Paths.getDownloadPath(activity);
            Log.i(getClass().getSimpleName(), dir.getAbsolutePath() + " exists=" + dir.exists() + ", isDirectory=" + dir.isDirectory() + ", writable=" + dir.canWrite());
            ContextUtil.toast(this.activity.getApplicationContext(), R.string.error_downloads_directory_not_writable);
        }
    }

    private boolean prepareDownloadsDir() {
        File dir = Paths.getDownloadPath(activity);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.exists() && dir.isDirectory() && dir.canWrite();
    }

    private LocalPurchaseTask getPurchaseTask() {
        LocalPurchaseTask purchaseTask = new LocalPurchaseTask();
        purchaseTask.setFragment(this);
        progressBar = ViewUtils.findViewById(activity, R.id.download_progress);
        progressCents = ViewUtils.findViewById(activity, R.id.progressCents);
        if (null != progressBar && null != progressCents) {
            purchaseTask.setDownloadProgressBarUpdater(new DownloadProgressBarUpdater(app.getPackageName(), progressBar, progressCents));
        }
        purchaseTask.setApp(app);
        purchaseTask.setContext(activity);
        purchaseTask.setTriggeredBy(activity instanceof ManualDownloadActivity ? MANUAL_DOWNLOAD_BUTTON : DOWNLOAD_BUTTON);
        return purchaseTask;
    }

    private int getInstalledVersionCode() {
        try {
            return activity.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    static class LocalPurchaseTask extends PurchaseTask {

        private ButtonDownload buttonDownload;

        public LocalPurchaseTask setFragment(ButtonDownload fragment) {
            this.buttonDownload = fragment;
            return this;
        }

        @Override
        public LocalPurchaseTask clone() {
            LocalPurchaseTask task = new LocalPurchaseTask();
            task.setDownloadProgressBarUpdater(progressBarUpdater);
            task.setTriggeredBy(triggeredBy);
            task.setApp(app);
            task.setContext(context);
            task.setFragment(buttonDownload);
            return task;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(AndroidAppDeliveryData deliveryData) {
            super.onPostExecute(deliveryData);
            if (!success()) {
                buttonDownload.draw();
                if (null != getRestrictionString()) {
                    ContextUtil.toastLong(context, getRestrictionString());
                    Log.i(getClass().getSimpleName(), "No download link returned, app restriction is " + app.getRestriction());
                }
            }
        }
    }
}
