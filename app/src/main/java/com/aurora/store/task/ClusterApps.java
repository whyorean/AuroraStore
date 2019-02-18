package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.dragons.aurora.playstoreapiv2.UrlIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClusterApps extends BaseTask {

    public ClusterApps(Context context) {
        super(context);
    }

    public List<App> getApps(String clusterUrl) throws IOException {
        List<App> appList = new ArrayList<>();
        CustomAppListIterator iterator = new CustomAppListIterator(
                new UrlIterator(new PlayStoreApiAuthenticator(context).getApi(), clusterUrl));
        iterator.setGooglePlayApi(new PlayStoreApiAuthenticator(context).getApi());
        if (!iterator.hasNext()) {
            return new ArrayList<>();
        }
        while (iterator.hasNext() && appList.isEmpty()) {
            appList.addAll(iterator.next());
        }
        return appList;
    }
}
