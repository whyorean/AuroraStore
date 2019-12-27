package com.aurora.store.ui.main.fragment.updates;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.model.App;
import com.aurora.store.task.UpdatableAppsTask;
import com.aurora.store.viewmodel.BaseViewModel;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class UpdatableAppsModel extends BaseViewModel {

    private MutableLiveData<List<App>> listMutableLiveData = new MutableLiveData<>();

    public UpdatableAppsModel(@NonNull Application application) {
        super(application);
        this.api = AuroraApplication.api;
        fetchUpdatableApps();
    }


    public LiveData<List<App>> getListMutableLiveData() {
        return listMutableLiveData;
    }

    public void fetchUpdatableApps() {
        disposable.add(Observable.fromCallable(() -> new UpdatableAppsTask(api, getApplication())
                .getUpdatableApps())
                .subscribeOn(Schedulers.computation())
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
