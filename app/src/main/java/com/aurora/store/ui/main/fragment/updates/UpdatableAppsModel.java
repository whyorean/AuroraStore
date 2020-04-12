package com.aurora.store.ui.main.fragment.updates;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.model.items.UpdatesItem;
import com.aurora.store.task.UpdatableAppsTask;
import com.aurora.store.viewmodel.BaseViewModel;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class UpdatableAppsModel extends BaseViewModel {

    private MutableLiveData<List<UpdatesItem>> data = new MutableLiveData<>();

    public UpdatableAppsModel(@NonNull Application application) {
        super(application);
        this.api = AuroraApplication.api;
        fetchUpdatableApps();
    }

    public LiveData<List<UpdatesItem>> getData() {
        return data;
    }

    public void fetchUpdatableApps() {
        disposable.add(Observable.fromCallable(() -> new UpdatableAppsTask(api, getApplication())
                .getUpdatableApps())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(this::sortList)
                .flatMap(apps -> Observable
                        .fromIterable(apps)
                        .map(UpdatesItem::new))
                .toList()
                .subscribe(updatesItems -> data.setValue(updatesItems), this::handleError));
    }

    @Override
    protected void onCleared() {
        disposable.dispose();
        super.onCleared();
    }
}
