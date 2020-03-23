package com.aurora.store.ui.details;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.model.App;
import com.aurora.store.task.AppDetailTask;
import com.aurora.store.viewmodel.BaseViewModel;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class DetailsAppModel extends BaseViewModel {

    private MutableLiveData<App> listMutableLiveData = new MutableLiveData<>();

    public DetailsAppModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<App> getAppDetails() {
        return listMutableLiveData;
    }

    public void fetchAppDetails(String packageName) {
        api = AuroraApplication.api;
        Observable.fromCallable(() -> new AppDetailTask(getApplication(), api)
                .getInfo(packageName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(app -> listMutableLiveData.setValue(app))
                .doOnError(this::handleError)
                .subscribe();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
