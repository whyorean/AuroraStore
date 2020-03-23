package com.aurora.store.ui.category;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.manager.FilterManager;
import com.aurora.store.model.items.EndlessItem;
import com.aurora.store.task.CategoryAppsTask;
import com.aurora.store.viewmodel.BaseViewModel;
import com.dragons.aurora.playstoreapiv2.CategoryAppsIterator;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class CategoryAppsModel extends BaseViewModel {

    private GooglePlayAPI api;
    private CategoryAppsTask categoryAppsTask;
    private CustomAppListIterator iterator;

    private MutableLiveData<List<EndlessItem>> data = new MutableLiveData<>();

    public CategoryAppsModel(@NonNull Application application) {
        super(application);
        this.api = AuroraApplication.api;
        this.categoryAppsTask = new CategoryAppsTask(application);
    }

    public LiveData<List<EndlessItem>> getCategoryApps() {
        return data;
    }

    public void fetchCategoryApps(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory, boolean shouldIterate) {
        if (!shouldIterate)
            getIterator(categoryId, subcategory);

        Observable.fromCallable(() -> categoryAppsTask
                .getApps(iterator))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(apps -> Observable.fromIterable(apps)
                        .map(EndlessItem::new)
                        .toList()
                        .toObservable()
                )
                .doOnNext(endlessItems -> data.setValue(endlessItems))
                .doOnError(this::handleError)
                .subscribe();
    }

    private void getIterator(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory) {
        try {
            api = AuroraApplication.api;
            final CategoryAppsIterator categoryAppsIterator = new CategoryAppsIterator(api, categoryId, subcategory);
            iterator = new CustomAppListIterator(categoryAppsIterator);
            iterator.setFilterEnabled(true);
            iterator.setFilterModel(FilterManager.getFilterPreferences(getApplication()));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}
