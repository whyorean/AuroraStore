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
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.utility.Util;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.jetbrains.annotations.NotNull;

public class GeneralNotification extends NotificationBase {

    public GeneralNotification(Context context, App app) {
        super(context, app);
        init();
    }

    private void init() {
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        builder = getBuilder();
    }

    public void notifyResume(int requestId) {
        builder = getBuilder();
        builder.setContentText(context.getString(R.string.download_paused));
        builder.addAction(R.drawable.ic_resume, context.getString(R.string.action_resume),
                getResumeIntent(requestId));
        builder.addAction(R.drawable.ic_cancel, context.getString(R.string.action_cancel),
                getCancelIntent(requestId));
        show();
    }

    public void notifyProgress(int progress, long downloadedBytesPerSecond, int requestId) {
        builder = getBuilder();
        builder.setProgress(100, progress, false);
        builder.setSubText(new StringBuilder().append(Util.humanReadableByteSpeed(downloadedBytesPerSecond, true)));
        builder.setContentText(new StringBuilder().append(progress).append("%"));
        builder.addAction(R.drawable.ic_resume, context.getString(R.string.action_pause),
                getPauseIntent(requestId));
        builder.addAction(R.drawable.ic_cancel, context.getString(R.string.action_cancel),
                getCancelIntent(requestId));
        show();
    }

    public void notifyQueued() {
        builder = getBuilder();
        builder.setContentText(context.getString(R.string.download_queued));
        show();
    }

    public void notifyFailed() {
        builder = getBuilder();
        builder.setContentText(context.getString(R.string.download_failed));
        show();
    }

    public void notifyCompleted() {
        builder = getBuilder();
        builder.setContentText(context.getString(R.string.download_completed));
        builder.setProgress(0, 0, false);
        builder.addAction(R.drawable.ic_installation, context.getString(R.string.details_install),
                getInstallIntent());
        builder.setAutoCancel(true);
        show();
    }

    public void notifyCancelled() {
        builder = getBuilder();
        builder.setContentText(context.getString(R.string.download_canceled));
        builder.setProgress(0, 0, false);
        show();
    }

    public void show() {
        if (Util.isNotificationEnabled(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(context.getPackageName(),
                        context.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Aurora Store Notification Channel");
                manager.createNotificationChannel(channel);
                builder.setChannelId(channel.getId());
            }
            Glide.with(context)
                    .asBitmap()
                    .load(app.getIconInfo().getUrl())
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NotNull Bitmap bitmap, Transition<? super Bitmap> transition) {
                            builder.setLargeIcon(bitmap);
                        }
                    });
            manager.notify(app.getPackageName().hashCode(), builder.build());
        }
    }
}
