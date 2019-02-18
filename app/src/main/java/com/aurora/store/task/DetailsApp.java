package com.aurora.store.task;

import android.content.Context;
import android.content.pm.PackageManager;

import com.aurora.store.model.App;
import com.aurora.store.model.AppBuilder;
import com.aurora.store.model.ReviewBuilder;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;

public class DetailsApp extends BaseTask {

    private App app;

    public DetailsApp(Context context) {
        super(context);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public App getInfo(String packageName) throws Exception {
        api = getApi();
        DetailsResponse response = api.details(packageName);
        app = AppBuilder.build(response);
        if (response.hasUserReview()) {
            app.setUserReview(ReviewBuilder.build(response.getUserReview()));
        }
        if (context != null) {
            try {
                PackageManager pm = context.getPackageManager();
                app.getPackageInfo().applicationInfo = pm.getApplicationInfo(packageName, 0);
                app.getPackageInfo().versionCode = pm.getPackageInfo(packageName, 0).versionCode;
                app.setInstalled(true);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return app;
    }


}