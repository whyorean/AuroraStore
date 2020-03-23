package com.aurora.store.ui.devapps;

import android.app.Application;

import androidx.annotation.NonNull;

import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.ui.search.SearchAppsModel;
import com.dragons.aurora.playstoreapiv2.SearchIterator;

public class DevAppsModel extends SearchAppsModel {

    public DevAppsModel(@NonNull Application application) {
        super(application);
    }

    @Override
    public void getIterator(String query) {
        try {
            searchIterator = new SearchIterator(api, query);
            iterator = new CustomAppListIterator(searchIterator);
        } catch (Exception e) {
            handleError(e);
        }
    }
}
