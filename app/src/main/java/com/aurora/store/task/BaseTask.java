package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.model.App;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BaseTask {

    protected Context context;
    protected GooglePlayAPI api;

    public BaseTask(Context context) {
        this.context = context;
    }


    public GooglePlayAPI getApi() throws IOException {
        return new PlayStoreApiAuthenticator(context).getApi();
    }

    public void setApi(GooglePlayAPI api) {
        this.api = api;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public List<App> filterGoogleApps(List<App> apps) {
        Set<String> shitSet = new BlacklistManager(context).getGoogleApps();
        List<App> mApps = new ArrayList<>();
        for (App app : apps) {
            if (!shitSet.contains(app.getPackageName())) {
                mApps.add(app);
            }
        }
        return mApps;
    }

}