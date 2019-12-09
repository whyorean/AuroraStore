package com.aurora.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.manager.FavouriteListManager;
import com.aurora.store.model.App;
import com.aurora.store.task.BulkDetailsTask;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FavouriteAppsModel extends BaseViewModel {

    private ArrayList<String> packageList;

    private MutableLiveData<List<App>> listMutableLiveData = new MutableLiveData<>();

    public FavouriteAppsModel(@NonNull Application application) {
        super(application);
        FavouriteListManager favouriteListManager = new FavouriteListManager(application);
        this.packageList = favouriteListManager.get();
    }

    public LiveData<List<App>> getFavouriteApps() {
        return listMutableLiveData;
    }

    public void fetchFavouriteApps() {
        api = AuroraApplication.api;
        disposable.add(Observable.fromCallable(() -> new BulkDetailsTask(api)
                .getRemoteAppList(packageList))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    listMutableLiveData.setValue(appList);
                }, err -> handleError(err)));
    }

    @Override
    protected void onCleared() {
        disposable.dispose();
        super.onCleared();
    }
}
