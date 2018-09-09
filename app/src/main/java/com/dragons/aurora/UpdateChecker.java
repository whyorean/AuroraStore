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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dragons.aurora.task.playstore.BackgroundUpdatableAppsTask;

import timber.log.Timber;

public class UpdateChecker extends BroadcastReceiver {

    static public void enable(Context context, int interval) {
        Intent intent = new Intent(context, UpdateChecker.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        if (interval > 0) {
            Timber.i("Enabling periodic update checks");
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    interval,
                    pendingIntent
            );
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.i("Started");
        BackgroundUpdatableAppsTask task = new BackgroundUpdatableAppsTask();
        task.setForceUpdate(context instanceof Activity);
        task.setContext(context);
        task.execute();
    }
}
