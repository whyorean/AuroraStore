package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.CategoryAppsIterator;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FeaturedApps extends BaseTask {

    public FeaturedApps(Context context) {
        super(context);
    }

    public List<App> getApps(String categoryId, GooglePlayAPI.SUBCATEGORY subCategory) throws IOException {
        List<App> apps = new ArrayList<>();
        CustomAppListIterator iterator = new CustomAppListIterator(new CategoryAppsIterator(getApi(), categoryId, subCategory));
        iterator.setGooglePlayApi(new PlayStoreApiAuthenticator(context).getApi());
        while (iterator.hasNext() && apps.isEmpty()) {
            apps.addAll(iterator.next());
        }
        if (Util.filterGoogleAppsEnabled(context))
            return filterGoogleApps(apps);
        else
            return apps;
    }
}
