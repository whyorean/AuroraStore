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

import com.aurora.store.model.App;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.SplitUtil;
import com.aurora.store.utility.TextUtil;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.AppFileMetadata;
import com.dragons.aurora.playstoreapiv2.Split;
import com.tonyodev.fetch2.EnqueueAction;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

import java.util.ArrayList;
import java.util.List;

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
        if (Util.isDownloadWifiOnly(context))
            request.setNetworkType(NetworkType.WIFI_ONLY);
        else
            request.setNetworkType(NetworkType.ALL);
        request.setTag(app.getPackageName());
        return request;
    }

    /*
     *
     * Build Simple App Download Request from URL and GroupId
     * @param Context - Application Context
     * @param App -  App object
     * @param Url -  APK Url
     * @param groupId - Request GroupId
     * @return Request
     *
     */

    public static Request buildRequest(Context context, App app, String apkURL, int groupId) {
        Request request = buildRequest(context, app, apkURL);
        request.setGroupId(groupId);
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

    public static Request buildSplitRequest(Context context, App app, Split split) {
        Request request = new Request(split.getDownloadUrl(),
                PathUtil.getLocalSplitPath(context, app, split.getName()));
        request.setPriority(Priority.HIGH);
        request.setEnqueueAction(EnqueueAction.UPDATE_ACCORDINGLY);
        request.setGroupId(app.getPackageName().hashCode());
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
        List<Split> splitList = deliveryData.getSplitList();
        List<Request> requestList = new ArrayList<>();
        for (Split split : splitList) {
            final Request request = buildSplitRequest(context, app, split);
            requestList.add(request);
        }
        return requestList;
    }

    /*
     *
     * Build Bundled App Download RequestList from SplitList and GroupId
     * Build Bundled App Download RequestList from SplitList
     * @param Context - Application Context
     * @param App -  App object
     * @param List<Split> -  List of Split Objects
     * @param groupId - Request GroupId
     * @return RequestList
     *
     */

    public static List<Request> buildSplitRequestList(Context context, App app,
                                                      List<Split> splitList, int id) {
        List<Request> requestList = new ArrayList<>();
        for (Split split : splitList) {
            final Request splitRequest = buildSplitRequest(context, app, split);
            splitRequest.setGroupId(id);
            requestList.add(splitRequest);
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

    public static Request buildObbRequest(Context context, App app, String Url, boolean isMain) {
        Request request;
        request = new Request(Url, PathUtil.getObbPath(app, isMain));
        request.setEnqueueAction(EnqueueAction.UPDATE_ACCORDINGLY);
        request.setPriority(Priority.HIGH);
        request.setGroupId(app.getPackageName().hashCode());
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
        if (appFileMetadataList.size() == 1) {
            AppFileMetadata obbFileMetadata = appDeliveryData.getAdditionalFile(0);
            if (TextUtil.isEmpty(obbFileMetadata.getDownloadUrlGzipped()))
                requestList.add(buildObbRequest(context, app, obbFileMetadata.getDownloadUrl(), true));
            else
                requestList.add(buildObbRequest(context, app, obbFileMetadata.getDownloadUrlGzipped(), true));
        }
        if (appFileMetadataList.size() == 2) {
            AppFileMetadata obbFileMetadata = appDeliveryData.getAdditionalFile(1);
            if (TextUtil.isEmpty(obbFileMetadata.getDownloadUrlGzipped()))
                requestList.add(buildObbRequest(context, app, obbFileMetadata.getDownloadUrl(), false));
            else
                requestList.add(buildObbRequest(context, app, obbFileMetadata.getDownloadUrlGzipped(), false));
        }
        return requestList;
    }

    /*
     *
     * Build Bulk RequestList from DeliveryDataList, AppList and GroupId
     * @param Context - Application Context
     * @param List<AndroidAppDeliveryData> -  List of AndroidAppDeliveryData objects
     * @param List<App> -  List of App Objects
     * @param groupId - Request GroupId
     *
     */
    public static List<Request> getBulkRequestList(Context context,
                                                   List<AndroidAppDeliveryData> deliveryDataList,
                                                   List<App> appList, int groupId) {
        List<Request> mRequestList = new ArrayList<>();
        for (int i = 0; i < deliveryDataList.size(); i++) {
            final List<Split> splitList = deliveryDataList.get(i).getSplitList();
            final App app = appList.get(i);
            if (!splitList.isEmpty()) {
                mRequestList.addAll(buildSplitRequestList(context, app, splitList, groupId));
                SplitUtil.addToList(context, app.getPackageName());
            }
            mRequestList.add(buildRequest(context, app,
                    deliveryDataList.get(i).getDownloadUrl(), groupId));

            //Add <PackageName,DisplayName> and <PackageName,IconURL> to PseudoMaps
            PackageUtil.addToPseudoPackageMap(context, app.getPackageName(), app.getDisplayName());
            PackageUtil.addToPseudoURLMap(context, app.getPackageName(), app.getIconInfo().getUrl());
        }
        return mRequestList;
    }
}

