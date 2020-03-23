package com.aurora.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.manager.FavouritesManager;
import com.aurora.store.model.items.FavouriteItem;
import com.aurora.store.task.BulkDetailsTask;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FavouriteAppsModel extends BaseViewModel {

    private List<String> packageList;

    private MutableLiveData<List<FavouriteItem>> data = new MutableLiveData<>();

    public FavouriteAppsModel(@NonNull Application application) {
        super(application);
        fetchFavouriteApps();
    }

    public LiveData<List<FavouriteItem>> getFavouriteApps() {
        return data;
    }

    public void fetchFavouriteApps() {
        final FavouritesManager favouritesManager = new FavouritesManager(getApplication());
        packageList = favouritesManager.getFavouritePackages();
        if (packageList.size() > 0) {
            api = AuroraApplication.api;
            Observable.fromCallable(() -> new BulkDetailsTask(api)
                    .getRemoteAppList(packageList))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(apps -> Observable
                            .fromIterable(apps)
                            .map(FavouriteItem::new))
                    .toList()
                    .doOnSuccess(favouriteItems -> data.setValue(favouriteItems))
                    .doOnError(this::handleError)
                    .subscribe();
        } else {
            data.setValue(new ArrayList<>());
        }
    }
}
