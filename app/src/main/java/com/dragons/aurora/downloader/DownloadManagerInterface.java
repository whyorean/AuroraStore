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

import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;

public interface DownloadManagerInterface {

    String EXTRA_DOWNLOAD_ID = "extra_download_id";
    String ACTION_DOWNLOAD_COMPLETE = "android.intent.action.DOWNLOAD_COMPLETE";
    String ACTION_DOWNLOAD_CANCELLED = "ACTION_DOWNLOAD_CANCELLED";

    int SUCCESS = 1;
    int IN_PROGRESS = 0;
    int ERROR_UNKNOWN = 1000;
    int ERROR_FILE_ERROR = 1001;
    int ERROR_UNHANDLED_HTTP_CODE = 1002;
    int ERROR_HTTP_DATA_ERROR = 1004;
    int ERROR_TOO_MANY_REDIRECTS = 1005;
    int ERROR_INSUFFICIENT_SPACE = 1006;
    int ERROR_DEVICE_NOT_FOUND = 1007;
    int ERROR_CANNOT_RESUME = 1008;
    int ERROR_FILE_ALREADY_EXISTS = 1009;
    int ERROR_BLOCKED = 1010;

    long enqueue(App app, AndroidAppDeliveryData deliveryData, Type type);

    boolean finished(long downloadId);

    boolean success(long downloadId);

    String getError(long downloadId);

    void cancel(long downloadId);

    enum Type {
        APK, DELTA, OBB_MAIN, OBB_PATCH
    }
}
