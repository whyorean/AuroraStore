package com.aurora.store.iterator;

import com.aurora.store.model.App;
import com.aurora.store.model.AppBuilder;
import com.aurora.store.model.Filter;
import com.aurora.store.utility.Log;
import com.dragons.aurora.playstoreapiv2.AppListIterator;
import com.dragons.aurora.playstoreapiv2.DocV2;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CustomAppListIterator implements Iterator {

    protected boolean enableFilter = false;
    protected Filter filter = new Filter();
    protected AppListIterator iterator;

    public CustomAppListIterator(com.dragons.aurora.playstoreapiv2.AppListIterator iterator) {
        this.iterator = iterator;
    }

    public void setEnableFilter(boolean enableFilter) {
        this.enableFilter = enableFilter;
    }

    public void setGooglePlayApi(GooglePlayAPI googlePlayApi) {
        iterator.setGooglePlayApi(googlePlayApi);
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public List<App> next() {
        List<App> apps = new ArrayList<>();
        for (DocV2 details : iterator.next()) {
            addApp(apps, AppBuilder.build(details));
        }
        return apps;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    private boolean shouldSkip(App app) {
        return (!filter.isPaidApps() && !app.isFree())
                || (!filter.isAppsWithAds() && app.containsAds())
                || (!filter.isGsfDependentApps() && !app.getDependencies().isEmpty())
                || (filter.getRating() > 0 && app.getRating().getAverage() < filter.getRating())
                || (filter.getDownloads() > 0 && app.getInstalls() < filter.getDownloads());
    }

    private void addApp(List<App> apps, App app) {
        if (enableFilter && shouldSkip(app)) {
            Log.i("Filtering out " + app.getPackageName());
        } else {
            apps.add(app);
        }
    }
}
