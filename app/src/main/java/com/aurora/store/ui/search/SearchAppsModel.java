package com.aurora.store.ui.search;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.enums.ErrorType;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.manager.FilterManager;
import com.aurora.store.model.items.EndlessItem;
import com.aurora.store.task.SearchTask;
import com.aurora.store.util.NetworkUtil;
import com.aurora.store.viewmodel.BaseViewModel;
import com.dragons.aurora.playstoreapiv2.SearchIterator;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SearchAppsModel extends BaseViewModel {

    protected CustomAppListIterator iterator;
    protected SearchIterator searchIterator;

    protected MutableLiveData<List<EndlessItem>> listMutableLiveData = new MutableLiveData<>();
    protected MutableLiveData<List<String>> relatedMutableLiveData = new MutableLiveData<>();

    public SearchAppsModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<List<String>> getRelatedTags() {
        return relatedMutableLiveData;
    }

    public LiveData<List<EndlessItem>> getQueriedApps() {
        return listMutableLiveData;
    }

    public void fetchQueriedApps(String query, boolean shouldIterate) {

        if (!NetworkUtil.isConnected(getApplication())) {
            errorData.setValue(ErrorType.NO_NETWORK);
            return;
        }

        api = AuroraApplication.api;
        if (!shouldIterate)
            getIterator(query);

        Observable.fromCallable(() -> new SearchTask(getApplication())
                .getSearchResults(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(apps -> Observable.fromIterable(apps)
                        .map(EndlessItem::new)
                        .toList()
                        .toObservable()
                )
                .doOnNext(searchItems -> listMutableLiveData.setValue(searchItems))
                .doOnError(this::handleError)
                .subscribe();
    }

    public void getIterator(String query) {
        try {
            searchIterator = new SearchIterator(api, query);
            iterator = new CustomAppListIterator(searchIterator);
            iterator.setFilterEnabled(true);
            iterator.setFilterModel(FilterManager.getFilterPreferences(getApplication()));
            //relatedMutableLiveData.setValue(iterator.getRelatedTags());
        } catch (Exception err) {
            handleError(err);
        }
    }
}
