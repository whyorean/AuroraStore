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

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.dragons.aurora.AuroraApplication;
import com.dragons.aurora.downloader.DownloadManagerFactory;
import com.dragons.aurora.downloader.DownloadManagerInterface;
import com.dragons.aurora.downloader.DownloadState;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class CancelDownloadService extends IntentService {

    static public final String DOWNLOAD_ID = "DOWNLOAD_ID";
    static public final String PACKAGE_NAME = "PACKAGE_NAME";

    private DownloadManagerInterface dm;

    public CancelDownloadService() {
        super("CancelDownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        dm = DownloadManagerFactory.get(getApplicationContext());
        long downloadId = intent.getLongExtra(DOWNLOAD_ID, 0L);
        String packageName = intent.getStringExtra(PACKAGE_NAME);
        if (downloadId == 0 && TextUtils.isEmpty(packageName)) {
            Timber.w("No download id or package name provided in the intent");
        }
        List<Long> downloadIds = new ArrayList<>();
        if (downloadId != 0) {
            downloadIds.add(downloadId);
        }
        if (!TextUtils.isEmpty(packageName)) {
            ((AuroraApplication) getApplicationContext()).removePendingUpdate(packageName);
            downloadIds.addAll(DownloadState.get(packageName).getDownloadIds());
        }
        for (long id : downloadIds) {
            cancel(id);
        }
    }

    private void cancel(long downloadId) {
        Timber.i("Cancelling download %s", downloadId);
        dm.cancel(downloadId);
    }
}
