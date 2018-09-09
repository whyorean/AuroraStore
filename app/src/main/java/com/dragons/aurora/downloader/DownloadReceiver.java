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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.dragons.aurora.Paths;
import com.dragons.aurora.model.App;

import timber.log.Timber;

abstract public class DownloadReceiver extends BroadcastReceiver {

    public final static String ACTION_DELTA_PATCHING_COMPLETE = "ACTION_DELTA_PATCHING_COMPLETE";

    protected Context context;
    protected long downloadId;
    protected DownloadState state;

    static protected boolean actionIs(Intent intent, String action) {
        return !TextUtils.isEmpty(intent.getAction()) && intent.getAction().equals(action);
    }

    abstract protected void process(Context context, Intent intent);

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        downloadId = intent.getLongExtra(DownloadManagerInterface.EXTRA_DOWNLOAD_ID, 0L);
        Timber.i(intent.getAction() + " (" + downloadId + ") received");
        if (downloadId == 0) {
            return;
        }
        state = DownloadState.get(downloadId);
        if (null != state) {
            process(context, intent);
        }
    }

    protected boolean isDelta(App app) {
        return null != app
                && !Paths.getApkPath(context, app.getPackageName(), app.getVersionCode()).exists()
                && Paths.getDeltaPath(context, app.getPackageName(), app.getVersionCode()).exists()
                ;
    }
}
