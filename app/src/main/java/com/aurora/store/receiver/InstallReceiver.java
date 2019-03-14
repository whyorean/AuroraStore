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

package com.aurora.store.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.aurora.store.installer.Installer;

import static com.aurora.store.notification.GeneralNotification.INTENT_APP_VERSION;
import static com.aurora.store.notification.GeneralNotification.INTENT_PACKAGE_NAME;

public class InstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if ((extras != null)) {
            final String packageName = extras.getString(INTENT_PACKAGE_NAME, "");
            final int appVersion = extras.getInt(INTENT_APP_VERSION, -1);
            if (!packageName.isEmpty() && appVersion != -1) {
                new Installer(context).installSplit(packageName, appVersion);
            }
        }
    }
}
