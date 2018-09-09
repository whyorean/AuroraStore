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

import com.dragons.aurora.Paths;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.HttpCookie;
import com.dragons.aurora.task.HttpURLConnectionDownloadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class DownloadManagerFake extends DownloadManagerAbstract {

    static private final Map<Long, Integer> statuses = new HashMap<>();

    public DownloadManagerFake(Context context) {
        super(context);
    }

    static public void putStatus(long downloadId, int status) {
        statuses.put(downloadId, status);
    }

    @Override
    public long enqueue(App app, AndroidAppDeliveryData deliveryData, Type type) {
        Timber.i("Downloading " + type.name() + " for " + app.getPackageName());
        String url = getUrl(deliveryData, type);
        long downloadId = url.hashCode();
        statuses.put(downloadId, DownloadManagerInterface.IN_PROGRESS);
        DownloadState.get(app.getPackageName()).setStarted(downloadId);

        HttpURLConnectionDownloadTask task = new HttpURLConnectionDownloadTask();
        task.setContext(context);
        task.setDownloadId(downloadId);
        task.setTargetFile(getDestinationFile(app, type));
        String cookieString = null;
        if (deliveryData.getDownloadAuthCookieCount() > 0) {
            HttpCookie cookie = deliveryData.getDownloadAuthCookie(0);
            cookieString = cookie.getName() + "=" + cookie.getValue();
        }
        task.execute(url, cookieString);
        return downloadId;
    }

    @Override
    public boolean finished(long downloadId) {
        return statuses.containsKey(downloadId) && statuses.get(downloadId) != DownloadManagerInterface.IN_PROGRESS;
    }

    @Override
    public boolean success(long downloadId) {
        return statuses.containsKey(downloadId) && statuses.get(downloadId) == DownloadManagerInterface.SUCCESS;
    }

    @Override
    public String getError(long downloadId) {
        return getErrorString(context, statuses.get(downloadId));
    }

    private String getUrl(AndroidAppDeliveryData deliveryData, Type type) {
        switch (type) {
            case APK:
                return deliveryData.getDownloadUrl();
            case DELTA:
                return deliveryData.getPatchData().getDownloadUrl();
            case OBB_MAIN:
                return deliveryData.getAdditionalFile(0).getDownloadUrl();
            case OBB_PATCH:
                return deliveryData.getAdditionalFile(1).getDownloadUrl();
            default:
                throw new RuntimeException("Unknown request type");
        }
    }

    private File getDestinationFile(App app, Type type) {
        switch (type) {
            case APK:
                return Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
            case DELTA:
                return Paths.getDeltaPath(context, app.getPackageName(), app.getVersionCode());
            case OBB_MAIN:
                return Paths.getObbPath(app.getPackageName(), app.getVersionCode(), true);
            case OBB_PATCH:
                return Paths.getObbPath(app.getPackageName(), app.getVersionCode(), false);
            default:
                throw new RuntimeException("Unknown request type");
        }
    }
}
