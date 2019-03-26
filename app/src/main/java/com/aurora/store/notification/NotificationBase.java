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

package com.aurora.store.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.aurora.store.R;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.model.App;
import com.aurora.store.receiver.DownloadCancelReceiver;
import com.aurora.store.receiver.DownloadPauseReceiver;
import com.aurora.store.receiver.DownloadResumeReceiver;
import com.aurora.store.receiver.InstallReceiver;
import com.aurora.store.utility.Util;

public class NotificationBase {

    public static final String INTENT_PACKAGE_NAME = "INTENT_PACKAGE_NAME";
    public static final String INTENT_APP_VERSION = "INTENT_APP_VERSION";
    public static final String REQUEST_ID = "REQUEST_ID";

    protected NotificationCompat.Builder builder;
    protected NotificationChannel channel;
    protected NotificationManager manager;

    protected Context context;
    protected App app;

    public NotificationBase(Context context) {
        this.context = context;
    }

    public NotificationBase(Context context, App app) {
        this.context = context;
        this.app = app;
    }

    protected NotificationCompat.Builder getBuilder() {
        return new NotificationCompat.Builder(context, app.getPackageName())
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setColorized(true)
                .setColor(context.getResources().getColor(R.color.colorAccent))
                .setContentIntent(getContentIntent())
                .setContentTitle(app.getDisplayName())
                .setOnlyAlertOnce(true)
                .setPriority(Util.getNotificationPriority(context))
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    /*
     *
     * All Pending Intents to handle App Download & App Installations
     * getContentIntent() to launch DetailsActivity for the App
     * getInstallIntent() to broadcast Install action on download complete
     * getCancelIntent() to broadcast Download Cancel action
     * getPauseIntent() to broadcast Download Pause action
     *
     */

    protected PendingIntent getContentIntent() {
        Intent intent = new Intent(context, DetailsActivity.class);
        intent.putExtra(INTENT_PACKAGE_NAME, app.getPackageName());
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected PendingIntent getInstallIntent() {
        Intent intent = new Intent(context, InstallReceiver.class);
        intent.putExtra(INTENT_PACKAGE_NAME, app.getPackageName());
        intent.putExtra(INTENT_APP_VERSION, app.getVersionCode());
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected PendingIntent getCancelIntent(int requestId) {
        Intent intent = new Intent(context, DownloadCancelReceiver.class);
        intent.putExtra(REQUEST_ID, requestId);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected PendingIntent getPauseIntent(int requestId) {
        Intent intent = new Intent(context, DownloadPauseReceiver.class);
        intent.putExtra(REQUEST_ID, requestId);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected PendingIntent getResumeIntent(int requestId) {
        Intent intent = new Intent(context, DownloadResumeReceiver.class);
        intent.putExtra(REQUEST_ID, requestId);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
