package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.AuroraApplication;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.download.RequestBuilder;
import com.aurora.store.model.App;
import com.aurora.store.util.Log;
import com.aurora.store.util.Util;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.tonyodev.fetch2.AbstractFetchGroupListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchGroup;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LiveUpdate {

    private Context context;
    private Fetch fetch;

    public LiveUpdate(Context context) {
        this.context = context;
    }

    public void enqueueUpdate(App app, AndroidAppDeliveryData deliveryData) {
        fetch = DownloadManager.getFetchInstance(context);

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

        fetch.enqueue(requestList, updatedRequestList ->
                Log.i("Updating -> %s", app.getDisplayName()));

        fetch.addListener(new AbstractFetchGroupListener() {
            @Override
            public void onCompleted(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                super.onCompleted(groupId, download, fetchGroup);
                if (groupId == app.getPackageName().hashCode() && fetchGroup.getGroupDownloadProgress() == 100) {
                    if (Util.shouldAutoInstallApk(context)) {
                        //Call the installer
                        AuroraApplication.getInstaller().install(app);
                    }
                    fetch.removeListener(this);
                }
            }
        });
    }
}
