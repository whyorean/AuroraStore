/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
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

package com.aurora.store.download;

import android.content.Context;

import com.aurora.store.Constants;
import com.aurora.store.model.App;
import com.aurora.store.util.PathUtil;
import com.aurora.store.util.Util;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.AppFileMetadata;
import com.dragons.aurora.playstoreapiv2.SplitDeliveryData;
import com.tonyodev.fetch2.EnqueueAction;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.Extras;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestBuilder {

    /*
     *
     * Build Simple App Download Request from URL
     * @param Context - Application Context
     * @param App -  App object
     * @param Url -  APK Url
     * @return Request
     *
     */

    public static Request buildRequest(Context context, App app, String Url) {
        Request request;
        request = new Request(Url, PathUtil.getLocalApkPath(context, app));
        request.setPriority(Priority.HIGH);
        request.setEnqueueAction(EnqueueAction.UPDATE_ACCORDINGLY);
        request.setGroupId(app.getPackageName().hashCode());
        addBasicHeaders(request, app);

        if (Util.isDownloadWifiOnly(context))
            request.setNetworkType(NetworkType.WIFI_ONLY);
        else
            request.setNetworkType(NetworkType.ALL);
        request.setTag(app.getPackageName());
        return request;
    }

    /*
     *
     * Build Bundled App Download Request from URL
     * @param Context - Application Context
     * @param App -  App object
     * @param Url -  APK Url
     * @return Request
     *
     */

    public static Request buildSplitRequest(Context context, App app, SplitDeliveryData split) {
        Request request = new Request(split.getDownloadUrl(),
                PathUtil.getLocalSplitPath(context, app, split.getName()));
        request.setPriority(Priority.HIGH);
        request.setEnqueueAction(EnqueueAction.UPDATE_ACCORDINGLY);
        request.setGroupId(app.getPackageName().hashCode());
        addBasicHeaders(request, app);

        if (Util.isDownloadWifiOnly(context))
            request.setNetworkType(NetworkType.WIFI_ONLY);
        else
            request.setNetworkType(NetworkType.ALL);
        request.setTag(app.getPackageName());
        return request;
    }

    /*
     *
     * Build Bundled App Download RequestList from SplitList
     * @param Context - Application Context
     * @param App -  App object
     * @param AndroidAppDeliveryData -  DeliveryData Objects
     * @return RequestList
     *
     */

    public static List<Request> buildSplitRequestList(Context context, App app,
                                                      AndroidAppDeliveryData deliveryData) {
        List<SplitDeliveryData> splitList = deliveryData.getSplitDeliveryDataList();
        List<Request> requestList = new ArrayList<>();
        for (SplitDeliveryData split : splitList) {
            final Request request = buildSplitRequest(context, app, split);
            requestList.add(request);
        }
        return requestList;
    }

    /*
     *
     * Build Obb Download Request from URL
     * @param Context - Application Context
     * @param App -  App object
     * @param Url -  APK Url
     * @param isMain - boolean to determine obb type
     * @return Request
     *
     */

    public static Request buildObbRequest(Context context, App app, String Url, boolean isMain, boolean isGZipped) {
        Request request;
        request = new Request(Url, PathUtil.getObbPath(app, isMain, isGZipped));
        request.setEnqueueAction(EnqueueAction.UPDATE_ACCORDINGLY);
        request.setPriority(Priority.HIGH);
        request.setGroupId(app.getPackageName().hashCode());
        addBasicHeaders(request, app);
        if (Util.isDownloadWifiOnly(context))
            request.setNetworkType(NetworkType.WIFI_ONLY);
        else
            request.setNetworkType(NetworkType.ALL);
        request.setTag(app.getPackageName());
        return request;
    }


    /*
     *
     * Build Obb App Download RequestList from DeliveryDataList and GroupId
     * @param Context - Application Context
     * @param AndroidAppDeliveryData -  App object
     * @param groupId - Request GroupId
     * @return RequestList
     *
     */

    public static List<Request> buildObbRequestList(Context context, App app, AndroidAppDeliveryData appDeliveryData) {
        List<Request> requestList = new ArrayList<>();
        List<AppFileMetadata> appFileMetadataList = appDeliveryData.getAdditionalFileList();

        for (AppFileMetadata appFileMetadata : appFileMetadataList) {
            requestList.add(buildObbRequest(context, app, appFileMetadata.getDownloadUrl(),
                    appFileMetadata.getFileType() == 0,
                    false));
        }
        return requestList;
    }

    public static void addBasicHeaders(Request request, App app) {
        final Map<String, String> stringMap = new HashMap<>();
        stringMap.put(Constants.DOWNLOAD_PACKAGE_NAME, app.getPackageName());
        stringMap.put(Constants.DOWNLOAD_DISPLAY_NAME, app.getDisplayName());
        stringMap.put(Constants.DOWNLOAD_VERSION_NAME, app.getVersionName());
        stringMap.put(Constants.DOWNLOAD_VERSION_CODE, String.valueOf(app.getVersionCode()));
        stringMap.put(Constants.DOWNLOAD_ICON_URL, app.getIconUrl());

        final Extras extras = new Extras(stringMap);
        request.setExtras(extras);
    }
}

