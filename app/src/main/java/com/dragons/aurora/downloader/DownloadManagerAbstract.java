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

import com.dragons.aurora.R;

public abstract class DownloadManagerAbstract implements DownloadManagerInterface {

    protected Context context;

    public DownloadManagerAbstract(Context context) {
        this.context = context;
    }

    static protected String getErrorString(Context context, int reason) {
        int stringId;
        switch (reason) {
            case DownloadManagerInterface.ERROR_CANNOT_RESUME:
                stringId = R.string.download_manager_ERROR_CANNOT_RESUME;
                break;
            case DownloadManagerInterface.ERROR_DEVICE_NOT_FOUND:
                stringId = R.string.download_manager_ERROR_DEVICE_NOT_FOUND;
                break;
            case DownloadManagerInterface.ERROR_FILE_ERROR:
                stringId = R.string.download_manager_ERROR_FILE_ERROR;
                break;
            case DownloadManagerInterface.ERROR_HTTP_DATA_ERROR:
                stringId = R.string.download_manager_ERROR_HTTP_DATA_ERROR;
                break;
            case DownloadManagerInterface.ERROR_INSUFFICIENT_SPACE:
                stringId = R.string.download_manager_ERROR_INSUFFICIENT_SPACE;
                break;
            case DownloadManagerInterface.ERROR_TOO_MANY_REDIRECTS:
                stringId = R.string.download_manager_ERROR_TOO_MANY_REDIRECTS;
                break;
            case DownloadManagerInterface.ERROR_UNHANDLED_HTTP_CODE:
                stringId = R.string.download_manager_ERROR_UNHANDLED_HTTP_CODE;
                break;
            case DownloadManagerInterface.ERROR_BLOCKED:
                stringId = R.string.download_manager_ERROR_BLOCKED;
                break;
            case DownloadManagerInterface.ERROR_UNKNOWN:
            default:
                stringId = R.string.download_manager_ERROR_UNKNOWN;
                break;
        }
        return context.getString(stringId);
    }

    @Override
    public void cancel(long downloadId) {
        DownloadState state = DownloadState.get(downloadId);
        if (null != state) {
            state.setCancelled(downloadId);
        }
    }
}
