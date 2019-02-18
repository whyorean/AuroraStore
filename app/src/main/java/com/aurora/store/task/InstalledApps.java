package com.aurora.store.task;

import android.content.Context;
import android.text.TextUtils;

import com.aurora.store.model.App;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstalledApps extends UpdatableApps {

    public InstalledApps(Context context) {
        super(context);
    }

    public List<App> getInstalledApps(boolean removeSystem) throws IOException {
        List<App> allMarketApps = new ArrayList<>();
        api = getApi();
        api.toc();
        allMarketApps.clear();
        Map<String, App> installedApps = getInstalledApps(context);
        if (removeSystem)
            installedApps = filterSystemApps(installedApps);
        for (App appFromMarket : getAppsFromPlayStore(api, installedApps.keySet())) {
            String packageName = appFromMarket.getPackageName();
            if (TextUtils.isEmpty(packageName) || !installedApps.containsKey(packageName)) {
                continue;
            }
            App installedApp = installedApps.get(packageName);
            appFromMarket = addInstalledAppInfo(appFromMarket, installedApp);
            allMarketApps.add(appFromMarket);

        }
        return allMarketApps;
    }

    private Map<String, App> filterSystemApps(Map<String, App> apps) {
        Map<String, App> installedApps = new HashMap<>();
        for (App app : apps.values()) {
            if (!app.isSystem()) {
                installedApps.put(app.getPackageName(), app);
            }
        }
        return installedApps;
    }
}