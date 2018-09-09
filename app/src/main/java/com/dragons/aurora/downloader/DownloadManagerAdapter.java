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

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.util.Pair;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;

import timber.log.Timber;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class DownloadManagerAdapter extends DownloadManagerAbstract {

    static private final int PROGRESS_INTERVAL = 100;

    private DownloadManager dm;

    public DownloadManagerAdapter(Context context) {
        super(context);
        dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    public long enqueue(App app, AndroidAppDeliveryData deliveryData, Type type) {
        DownloadManager.Request request;
        Timber.i("Downloading " + type.name() + " for " + app.getPackageName());
        switch (type) {
            case APK:
                request = new DownloadRequestBuilderApk(context, app, deliveryData).build();
                break;
            case DELTA:
                request = new DownloadRequestBuilderDelta(context, app, deliveryData).build();
                break;
            case OBB_MAIN:
                request = new DownloadRequestBuilderObb(context, app, deliveryData).setMain(true).build();
                break;
            case OBB_PATCH:
                request = new DownloadRequestBuilderObb(context, app, deliveryData).setMain(false).build();
                break;
            default:
                throw new RuntimeException("Unknown request type");
        }
        if (DownloadState.get(app.getPackageName()).getTriggeredBy().equals(DownloadState.TriggeredBy.SCHEDULED_UPDATE)
                && Prefs.getBoolean(context, Aurora.PREFERENCE_BACKGROUND_UPDATE_WIFI_ONLY)
        ) {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        }
        long downloadId = dm.enqueue(request);
        new DownloadManagerProgressUpdater(downloadId, this).execute(PROGRESS_INTERVAL);
        return downloadId;
    }

    @Override
    public boolean finished(long downloadId) {
        return null != getCursor(downloadId);
    }

    @Override
    public boolean success(long downloadId) {
        Cursor cursor = getCursor(downloadId);
        if (null == cursor) {
            return false;
        }
        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
        cursor.close();
        return status == DownloadManager.STATUS_SUCCESSFUL || reason == DownloadManager.ERROR_FILE_ALREADY_EXISTS;
    }

    @Override
    public String getError(long downloadId) {
        Cursor cursor = getCursor(downloadId);
        if (null == cursor) {
            return getErrorString(context, DownloadManagerInterface.ERROR_UNKNOWN);
        }
        int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
        cursor.close();
        return getErrorString(context, reason);
    }

    @Override
    public void cancel(long downloadId) {
        super.cancel(downloadId);
        dm.remove(downloadId);
    }

    public Pair<Float, Float> getProgress(long downloadId) {
        Cursor cursor = getCursor(downloadId);
        if (null == cursor) {
            return null;
        }
        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
        float total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        float complete = status == DownloadManager.STATUS_SUCCESSFUL || reason == DownloadManager.ERROR_FILE_ALREADY_EXISTS
                ? total
                : cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        cursor.close();
        return new Pair<>(complete, total);
    }

    private Cursor getCursor(long downloadId) {
        Cursor cursor = dm.query(new DownloadManager.Query().setFilterById(downloadId));
        if (null == cursor) {
            return null;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }
}
