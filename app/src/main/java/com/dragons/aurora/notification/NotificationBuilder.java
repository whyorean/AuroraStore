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

package com.dragons.aurora.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

abstract public class NotificationBuilder {

    protected Context context;

    public NotificationBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    abstract public NotificationBuilder setTitle(String title);

    abstract public NotificationBuilder setMessage(String message);

    abstract public NotificationBuilder setIntent(Intent intent);

    abstract public Notification build();

    public NotificationBuilder addAction(int iconId, int stringId, PendingIntent pendingIntent) {
        return this;
    }

    public NotificationBuilder setProgress(int max, int progress) {
        return this;
    }

    protected PendingIntent getPendingIntent(Intent intent) {
        return PendingIntent.getActivity(context, 1, intent, 0);
    }
}
