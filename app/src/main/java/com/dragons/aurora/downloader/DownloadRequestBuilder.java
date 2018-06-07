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
import android.net.Uri;
import android.os.Build;

import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.HttpCookie;

import java.io.File;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public abstract class DownloadRequestBuilder {

    protected Context context;
    protected App app;
    protected AndroidAppDeliveryData deliveryData;

    public DownloadRequestBuilder(Context context, App app, AndroidAppDeliveryData deliveryData) {
        this.context = context;
        this.app = app;
        this.deliveryData = deliveryData;
    }

    abstract protected File getDestinationFile();

    abstract protected String getNotificationTitle();

    abstract protected String getDownloadUrl();

    public DownloadManager.Request build() {
        DownloadManager.Request request = new DownloadManager.Request(getDownloadUri());
        if (deliveryData.getDownloadAuthCookieCount() > 0) {
            HttpCookie cookie = deliveryData.getDownloadAuthCookie(0);
            request.addRequestHeader("Cookie", cookie.getName() + "=" + cookie.getValue());
        }
        request.setDestinationUri(getDestinationUri());
        request.setDescription(app.getPackageName());
        request.setTitle(getNotificationTitle());
        return request;
    }

    private Uri getDestinationUri() {
        return Uri.fromFile(getDestinationFile());
    }

    private Uri getDownloadUri() {
        return Uri.parse(getDownloadUrl());
    }
}
