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
import android.util.Log;

import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.utility.Util;

import java.util.Locale;

public class SplitService extends Service {

    public static final String ACTION_INSTALLATION_STATUS_NOTIFICATION = "com.aurora.store.action.INSTALLATION_STATUS_NOTIFICATION";
    public static final String EXTRA_INSTALLATION_STATUS = "com.aurora.store.extra.INSTALLATION_STATUS";
    public static final String EXTRA_SESSION_ID = "com.aurora.store.extra.SESSION_ID";
    public static final String EXTRA_PACKAGE_NAME = "com.aurora.store.extra.PACKAGE_NAME";
    public static final String EXTRA_ERROR_DESCRIPTION = "com.aurora.store.extra.ERROR_DESCRIPTION";

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_CONFIRMATION_PENDING = 1;
    public static final int STATUS_FAILURE = 2;

    private static final String TAG = "Split Service";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Log.d(TAG, "Requesting user confirmation for installation");
                sendStatusChangeBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1),
                        STATUS_CONFIRMATION_PENDING, intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(confirmationIntent);
                } catch (Exception e) {
                    sendErrorBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), "Error");
                }
                break;
            case PackageInstaller.STATUS_SUCCESS:
                Log.d(TAG, "Installation succeed");
                sendStatusChangeBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1),
                        STATUS_SUCCESS,
                        intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                break;
            default:
                Log.d(TAG, "Installation failed");
                sendErrorBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1),
                        getErrorString(status, intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)));
                break;
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    private void sendStatusChangeBroadcast(int sessionID, int status, String packageName) {
        Intent statusIntent = new Intent(ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(EXTRA_INSTALLATION_STATUS, status);
        statusIntent.putExtra(EXTRA_SESSION_ID, sessionID);

        if (packageName != null)
            statusIntent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        sendBroadcast(statusIntent);
    }

    private void sendErrorBroadcast(int sessionID, String error) {
        Intent statusIntent = new Intent(ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(EXTRA_INSTALLATION_STATUS, STATUS_FAILURE);
        statusIntent.putExtra(EXTRA_SESSION_ID, sessionID);
        statusIntent.putExtra(EXTRA_ERROR_DESCRIPTION, error);
        sendBroadcast(statusIntent);
    }

    public String getErrorString(int status, String blockingPackage) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                return "Installation Cancelled";

            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                String blocker = "Installation Blocked";
                if (blockingPackage != null) {
                    String appLabel = Util.getAppLabel(getApplicationContext(), blockingPackage);
                    if (appLabel != null)
                        blocker = appLabel;
                }
                return String.format(Locale.getDefault(), "Installation Blocked By %s", blocker);

            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return "Conflicting Package Name Exists";

            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                return "Incompatible APK";

            case PackageInstaller.STATUS_FAILURE_INVALID:
                return "Corrupted APK files";

            case PackageInstaller.STATUS_FAILURE_STORAGE:
                return "Insufficient Storage Space";
        }
        return "Installation Failed";
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
