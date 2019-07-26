/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Split APK Installer (SAI)
 * Copyright (C) 2018, Aefyr
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

package com.aurora.store.installer;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class InstallerService extends Service {

    public static final String ACTION_INSTALLATION_STATUS_NOTIFICATION = "com.aurora.store.action.INSTALLATION_STATUS_NOTIFICATION";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
        String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);

        //Send broadcast for the installation status of the package
        sendStatusBroadcast(status, packageName);

        //Launch user confirmation activity
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(confirmationIntent);
            } catch (Exception e) {
                sendStatusBroadcast(PackageInstaller.STATUS_FAILURE, packageName);
            }
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    private void sendStatusBroadcast(int status, String packageName) {
        Intent statusIntent = new Intent(ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(PackageInstaller.EXTRA_STATUS, status);
        statusIntent.putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, packageName);
        sendBroadcast(statusIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
