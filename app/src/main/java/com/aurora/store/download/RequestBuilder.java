package com.aurora.store.download;

import android.content.Context;

import com.aurora.store.model.App;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.Split;
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
        Request request;
        request = new Request(split.getDownloadUrl(),
                PathUtil.getLocalSplitPath(context, app, split.getName()));
        request.setPriority(Priority.HIGH);
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
     * @param List<Split> -  List of Split Objects
     * @return RequestList
     *
     */

    public static List<Request> buildSplitRequestList(Context context, App app,
                                                      List<Split> splitList) {
        List<Request> requestList = new ArrayList<>();
        for (Split split : splitList) {
            final Request splitRequest = buildSplitRequest(context, app, split);
            requestList.add(splitRequest);
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
            requestList.add(splitRequest);
            splitRequest.setGroupId(id);
            requestList.add(splitRequest);
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
        int index = 0;
        for (AndroidAppDeliveryData deliveryData : deliveryDataList) {
            List<Split> splitList = deliveryData.getSplitList();
            if (!splitList.isEmpty())
                mRequestList.addAll(buildSplitRequestList(context,
                        appList.get(index), splitList, groupId));
            mRequestList.add(buildRequest(context, appList.get(index),
                    deliveryData.getDownloadUrl(), groupId));
            index++;
        }
        return mRequestList;
    }
}

