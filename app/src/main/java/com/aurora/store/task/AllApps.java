package com.aurora.store.task;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.aurora.store.model.App;
import com.aurora.store.utility.CertUtil;
import com.aurora.store.utility.Util;

import java.util.HashMap;
import java.util.Map;

public class AllApps extends BaseTask {

    public AllApps(Context context) {
        super(context);
    }

    Map<String, App> getInstalledApps(Context context) {
        Map<String, App> installedApps = new HashMap<>();
        PackageManager pm = context.getPackageManager();
        for (PackageInfo packageInfo : pm.getInstalledPackages(0)) {
            if (null != packageInfo.applicationInfo && !packageInfo.applicationInfo.enabled) {
                continue;
            }
            App app = Util.getInstalledApp(pm, packageInfo.packageName);
            if (null != app) {
                if (Util.filterFDroidAppsEnabled(context) && CertUtil.isFDroidApp(context, app.getPackageName()))
                    continue;
                installedApps.put(app.getPackageName(), app);
            }
        }
        return installedApps;
    }
}
