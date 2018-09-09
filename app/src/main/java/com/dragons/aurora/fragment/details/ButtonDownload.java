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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.AuroraPermissionManager;
import com.dragons.aurora.BuildConfig;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.Paths;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.activities.ManualDownloadActivity;
import com.dragons.aurora.downloader.DownloadProgressBarUpdater;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.LocalPurchaseTask;
import com.dragons.aurora.task.playstore.PurchaseTask;

import java.io.File;

import static com.dragons.aurora.downloader.DownloadState.TriggeredBy.DOWNLOAD_BUTTON;
import static com.dragons.aurora.downloader.DownloadState.TriggeredBy.MANUAL_DOWNLOAD_BUTTON;

public class ButtonDownload extends Button {

    public ButtonDownload(Context context, View view, App app) {
        super(context, view, app);
    }


    @Override
    protected android.widget.Button getButton() {
        if (app.getPrice() != null && !app.isFree()) {
            setText(R.id.download, R.string.details_purchase);
            return (android.widget.Button) view.findViewById(R.id.download);
        } else if (context instanceof DetailsActivity
                && view.findViewById(R.id.cancel).getVisibility() == View.VISIBLE)
            return null;
        else
            return (android.widget.Button) view.findViewById(R.id.download);
    }

    @Override
    public boolean shouldBeVisible() {
        File apk = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
        return (!apk.exists() || apk.length() != app.getSize() || !DownloadState.get(app.getPackageName()).isEverythingSuccessful())
                && (app.isFree() || !Prefs.getBoolean(context, Aurora.PREFERENCE_APP_PROVIDED_EMAIL))
                && (app.isInPlayStore() || app.getPackageName().equals(BuildConfig.APPLICATION_ID))
                && (getInstalledVersionCode() != app.getVersionCode() || context instanceof ManualDownloadActivity);
    }

    @Override
    protected void onButtonClick(View v) {
        checkAndDownload();
        switchViews();
    }

    public void checkAndDownload() {
        View buttonDownload = view.findViewById(R.id.download);
        View buttonCancel = view.findViewById(R.id.cancel);

        if (null != buttonDownload) buttonDownload.setVisibility(View.GONE);

        AuroraPermissionManager permissionManager;
        if (context instanceof ManualDownloadActivity)
            permissionManager = new AuroraPermissionManager((ManualDownloadActivity) context);
        else
            permissionManager = new AuroraPermissionManager((AuroraActivity) context);

        if (app.getVersionCode() == 0 && !(context instanceof ManualDownloadActivity)) {
            context.startActivity(new Intent(context, ManualDownloadActivity.class));
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
        if (Paths.getApkPath(context, app.getPackageName(), app.getVersionCode()).exists()
                && !state.isEverythingSuccessful()) {
            if (null != progressBar && null != progressCents) {
                new DownloadProgressBarUpdater(app.getPackageName(), progressBar, progressCents).execute(PurchaseTask.UPDATE_INTERVAL);
            }
        }
    }

    public void download() {
        boolean writePermission;
        if (context instanceof ManualDownloadActivity)
            writePermission = new AuroraPermissionManager((ManualDownloadActivity) context).checkPermission();
        else
            writePermission = new AuroraPermissionManager((AuroraActivity) context).checkPermission();
        Log.i(getClass().getSimpleName(), "Write permission granted - " + writePermission);
        if (writePermission && prepareDownloadsDir()) {
            getPurchaseTask().execute();
        } else {
            File dir = Paths.getDownloadPath(context);
            Log.i(getClass().getSimpleName(), dir.getAbsolutePath() + " exists=" + dir.exists() + ", isDirectory=" + dir.isDirectory() + ", writable=" + dir.canWrite());
            ContextUtil.toast(context, R.string.error_downloads_directory_not_writable);
        }
    }

    private boolean prepareDownloadsDir() {
        File dir = Paths.getDownloadPath(context);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.exists() && dir.isDirectory() && dir.canWrite();
    }

    private int getInstalledVersionCode() {
        try {
            return context.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private LocalPurchaseTask getPurchaseTask() {
        LocalPurchaseTask purchaseTask = new LocalPurchaseTask();
        purchaseTask.setFragment(this);
        if (null != progressBar && null != progressCents) {
            purchaseTask.setDownloadProgressBarUpdater(new DownloadProgressBarUpdater(app.getPackageName(), progressBar, progressCents));
        }
        purchaseTask.setApp(app);
        purchaseTask.setContext(context);
        purchaseTask.setTriggeredBy(context instanceof ManualDownloadActivity ? MANUAL_DOWNLOAD_BUTTON : DOWNLOAD_BUTTON);
        return purchaseTask;
    }
}
