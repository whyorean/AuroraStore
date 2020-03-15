package com.aurora.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.model.App;
import com.aurora.store.task.InstalledAppsTask;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BlackListedAppsModel extends BaseViewModel {

    private MutableLiveData<List<App>> listMutableLiveData = new MutableLiveData<>();

    public BlackListedAppsModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<App>> getAllApps() {
        return listMutableLiveData;
    }

    public void fetchBlackListedApps() {
        api = AuroraApplication.api;
        compositeDisposable.add(Observable.fromCallable(() -> new InstalledAppsTask(api, getApplication())
                .getAllLocalApps())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    listMutableLiveData.setValue(appList);
                }, err -> handleError(err)));
    }

    @Override
    protected void onCleared() {
        compositeDisposable.dispose();
        super.onCleared();
    }
}
