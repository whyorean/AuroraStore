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

package com.dragons.aurora.downloader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.dragons.aurora.NetworkState;

import timber.log.Timber;

public class DownloadManagerFactory {

    private static final String DOWNLOAD_MANAGER_PACKAGE_NAME = "com.android.providers.downloads";

    static public DownloadManagerInterface get(Context context) {
        if (!nativeDownloadManagerEnabled(context) || nougatVpn(context)
        ) {
            return new DownloadManagerFake(context);
        } else {
            return new DownloadManagerAdapter(context);
        }
    }

    static private boolean nativeDownloadManagerEnabled(Context context) {
        int state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        try {
            state = context.getPackageManager().getApplicationEnabledSetting(DOWNLOAD_MANAGER_PACKAGE_NAME);
        } catch (Throwable e) {
            Timber.w("Could not check DownloadManager status: %s", e.getMessage());
        }
        return !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        );
    }

    static private boolean nougatVpn(Context context) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.N && Build.VERSION.SDK_INT != Build.VERSION_CODES.N_MR1) {
            return false;
        }
        return NetworkState.isVpn(context);
    }
}
