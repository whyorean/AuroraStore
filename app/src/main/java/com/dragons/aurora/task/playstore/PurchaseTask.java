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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.WindowManager;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.NotPurchasedException;
import com.dragons.aurora.R;
import com.dragons.aurora.downloader.DownloadManagerInterface;
import com.dragons.aurora.downloader.DownloadProgressBarUpdater;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.downloader.Downloader;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;

import androidx.appcompat.app.AlertDialog;
import timber.log.Timber;

public class PurchaseTask extends DeliveryDataTask implements CloneableTask {

    static public final String URL_PURCHASE = "https://play.google.com/store/apps/details?id=";
    static public final long UPDATE_INTERVAL = 300;

    protected DownloadState.TriggeredBy triggeredBy = DownloadState.TriggeredBy.DOWNLOAD_BUTTON;
    protected DownloadProgressBarUpdater progressBarUpdater;

    @Override
    public CloneableTask clone() {
        PurchaseTask task = new PurchaseTask();
        task.setDownloadProgressBarUpdater(progressBarUpdater);
        task.setTriggeredBy(triggeredBy);
        task.setApp(app);
        task.setContext(context);
        return task;
    }

    public void setTriggeredBy(DownloadState.TriggeredBy triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public void setDownloadProgressBarUpdater(DownloadProgressBarUpdater progressBarUpdater) {
        this.progressBarUpdater = progressBarUpdater;
    }

    @Override
    protected AndroidAppDeliveryData getResult(GooglePlayAPI api, String... arguments) throws IOException {
        DownloadState state = DownloadState.get(app.getPackageName());
        if (null != state) {
            state.setTriggeredBy(triggeredBy);
        }
        super.getResult(api, arguments);
        if (null != deliveryData) {
            Downloader downloader = new Downloader(context);
            try {
                if (downloader.enoughSpace(deliveryData)) {
                    downloader.download(app, deliveryData);
                    if (null != progressBarUpdater) {
                        progressBarUpdater.execute(UPDATE_INTERVAL);
                    }
                } else {
                    context.sendBroadcast(new Intent(DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED));
                    Timber.e("%s not enough storage space", app.getPackageName());
                    throw new IOException(context.getString(R.string.download_manager_ERROR_INSUFFICIENT_SPACE));
                }
            } catch (IllegalArgumentException | SecurityException e) {
                context.sendBroadcast(new Intent(DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED));
                Timber.e(app.getPackageName() + " unknown storage error: " + e.getClass().getName() + ": " + e.getMessage());
                throw new IOException(context.getString(R.string.download_manager_ERROR_FILE_ERROR));
            }
        } else {
            context.sendBroadcast(new Intent(DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED));
            Timber.e("%s no download link returned", app.getPackageName());
        }
        return deliveryData;
    }

    @Override
    protected void processException(Throwable e) {
        super.processException(e);
        context.sendBroadcast(new Intent(DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED));
    }

    @Override
    protected void onPostExecute(AndroidAppDeliveryData deliveryData) {
        super.onPostExecute(deliveryData);
        if (getException() instanceof NotPurchasedException
                && triggeredBy.equals(DownloadState.TriggeredBy.DOWNLOAD_BUTTON)
                && triggeredBy.equals(DownloadState.TriggeredBy.MANUAL_DOWNLOAD_BUTTON)
        ) {
            try {
                getNotPurchasedDialog(context).show();
            } catch (WindowManager.BadTokenException e) {
                Timber.e("Could not create purchase error dialog: %s", e.getMessage());
            }
        }
    }

    @Override
    protected void processIOException(IOException e) {
        if (!(e instanceof NotPurchasedException)) {
            super.processIOException(e);
        }
    }

    @Override
    protected void processAuthException(AuthException e) {
        if (e.getCode() == 403) {
            if (ContextUtil.isAlive(context)) {
                ContextUtil.toast(context, R.string.details_download_not_available);
            } else {
                Timber.w("%s not available", app.getPackageName());
            }
        } else {
            super.processAuthException(e);
        }
    }

    private AlertDialog getNotPurchasedDialog(Context c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder
                .setMessage(R.string.error_not_purchased)
                .setPositiveButton(
                        android.R.string.ok,
                        (dialog, id) -> {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(URL_PURCHASE + app.getPackageName()));
                            context.startActivity(i);
                        }
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        (dialog, id) -> dialog.cancel()
                )
        ;
        return builder.create();
    }
}
