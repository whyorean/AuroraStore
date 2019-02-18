package com.aurora.store.task;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aurora.store.manager.CategoryManager;
import com.aurora.store.Filter;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.aurora.store.utility.Util;

import java.util.ArrayList;
import java.util.List;

public class SearchTask extends BaseTask {

    public SearchTask(Context context) {
        super(context);
    }

    public List<App> getSearchResults(@NonNull CustomAppListIterator iterator) {
        if (!iterator.hasNext()) {
            return new ArrayList<>();
        }
        List<App> apps = new ArrayList<>();
        while (iterator.hasNext() && apps.isEmpty()) {
            apps.addAll(getNextBatch(iterator));
        }
        return apps;
    }

    public List<App> getNextBatch(CustomAppListIterator iterator) {
        CategoryManager categoryManager = new CategoryManager(getContext());
        com.aurora.store.model.Filter filter = new Filter(getContext()).getFilterPreferences();
        List<App> apps = new ArrayList<>();
        for (App app : iterator.next()) {
            if (categoryManager.fits(app.getCategoryId(), filter.getCategory())) {
                apps.add(app);
            }
        }
        if (Util.filterGoogleAppsEnabled(context))
            return filterGoogleApps(apps);
        else
            return apps;
    }
}
