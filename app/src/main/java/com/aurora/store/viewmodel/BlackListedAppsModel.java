package com.aurora.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.model.items.BlacklistItem;
import com.aurora.store.task.InstalledAppsTask;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BlackListedAppsModel extends BaseViewModel {

    private MutableLiveData<List<BlacklistItem>> data = new MutableLiveData<>();

    public BlackListedAppsModel(@NonNull Application application) {
        super(application);
        fetchBlackListedApps();
    }

    public LiveData<List<BlacklistItem>> getBlacklistedItems() {
        return data;
    }

    public void fetchBlackListedApps() {
        Observable.fromCallable(() -> new InstalledAppsTask(getApplication())
                .getAllLocalApps())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(apps -> Observable
                        .fromIterable(apps)
                        .map(BlacklistItem::new))
                .toList()
                .doOnSuccess(blacklistItems -> data.setValue(blacklistItems))
                .doOnError(this::handleError)
                .subscribe();
    }
}
