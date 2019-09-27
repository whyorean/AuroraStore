package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.AuroraApplication;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.download.RequestBuilder;
import com.aurora.store.model.App;
import com.aurora.store.notification.GeneralNotification;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.tonyodev.fetch2.AbstractFetchGroupListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchGroup;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LiveUpdate extends BaseTask {

    private Fetch fetch;
    private FetchListener fetchListener;
    private GeneralNotification notification;
    private App app;
    private int hashCode;
    private int progress;

    public LiveUpdate(Context context) {
        super(context);
    }

    public void enqueueUpdate(App mApp, AndroidAppDeliveryData deliveryData) {
        app = mApp;
        fetch = DownloadManager.getFetchInstance(context);
        notification = new GeneralNotification(context, app);
        hashCode = app.getPackageName().hashCode();

        final Request request = RequestBuilder
                .buildRequest(context, app, deliveryData.getDownloadUrl());
        final List<Request> splitList = RequestBuilder
                .buildSplitRequestList(context, app, deliveryData);
        final List<Request> obbList = RequestBuilder
                .buildObbRequestList(context, app, deliveryData);

        List<Request> requestList = new ArrayList<>();
        requestList.add(request);
        requestList.addAll(splitList);
        requestList.addAll(obbList);

        fetchListener = getFetchListener();
        fetch.addListener(fetchListener);
        fetch.enqueue(requestList, updatedRequestList ->
                Log.i("Updating -> %s", app.getDisplayName()));

        //Add <PackageName,DisplayName> and <PackageName,IconURL> to PseudoMaps
        PackageUtil.addToPseudoPackageMap(context, app.getPackageName(), app.getDisplayName());
        PackageUtil.addToPseudoURLMap(context, app.getPackageName(), app.getIconInfo().getUrl());
    }

    private FetchListener getFetchListener() {
        return new AbstractFetchGroupListener() {

            @Override
            public void onQueued(int groupId, @NotNull Download download, boolean waitingNetwork, @NotNull FetchGroup fetchGroup) {
                if (groupId == hashCode) {
                    notification.notifyQueued();
                }
            }

            @Override
            public void onResumed(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                if (groupId == hashCode) {
                    progress = fetchGroup.getGroupDownloadProgress();
                    if (progress < 0) progress = 0;
                    notification.notifyProgress(progress, 0, hashCode);
                }
            }

            @Override
            public void onProgress(int groupId, @NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond, @NotNull FetchGroup fetchGroup) {
                if (groupId == hashCode) {
                    progress = fetchGroup.getGroupDownloadProgress();
                    if (progress < 0) progress = 0;
                    notification.notifyProgress(progress, downloadedBytesPerSecond, hashCode);
                }
            }

            @Override
            public void onError(int groupId, @NotNull Download download, @NotNull Error error,
                                @Nullable Throwable throwable, @NotNull FetchGroup fetchGroup) {
                if (groupId == hashCode) {
                    Log.e("Error updating %s", app.getDisplayName());
                }
            }

            @Override
            public void onCompleted(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                if (groupId == hashCode && fetchGroup.getGroupDownloadProgress() == 100) {
                    notification.notifyCompleted();
                    if (Util.shouldAutoInstallApk(context))
                        AuroraApplication.getInstaller().install(app);
                    if (fetchListener != null) {
                        fetch.removeListener(fetchListener);
                        fetchListener = null;
                    }
                }
            }

            @Override
            public void onCancelled(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                if (groupId == hashCode) {
                    notification.notifyCancelled();
                    Log.e("Cancelled %s", app.getDisplayName());
                }
            }
        };
    }
}
