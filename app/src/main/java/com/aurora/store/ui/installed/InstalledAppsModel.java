package com.aurora.store.ui.installed;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.Constants;
import com.aurora.store.model.App;
import com.aurora.store.model.items.InstalledItem;
import com.aurora.store.task.InstalledAppsTask;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;
import com.aurora.store.viewmodel.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class InstalledAppsModel extends BaseViewModel implements SharedPreferences.OnSharedPreferenceChangeListener {

    private boolean userOnly;
    private SharedPreferences sharedPreferences;

    private MutableLiveData<List<InstalledItem>> data = new MutableLiveData<>();

    public InstalledAppsModel(@NonNull Application application) {
        super(application);
        sharedPreferences = Util.getPrefs(getApplication());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        userOnly = PrefUtil.getBoolean(application, Constants.PREFERENCE_INCLUDE_SYSTEM);
        fetchInstalledApps(userOnly);
    }

    public LiveData<List<InstalledItem>> getData() {
        return data;
    }

    public void fetchInstalledApps(boolean userOnly) {
        Observable.fromCallable(() -> new InstalledAppsTask(getApplication())
                .getInstalledApps())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(apps -> filterList(apps, userOnly))
                .map(apps -> sortList(apps))
                .flatMap(apps -> Observable
                        .fromIterable(apps)
                        .map(InstalledItem::new))
                .toList()
                .doOnSuccess(installedItems -> data.setValue(installedItems))
                .doOnError(this::handleError)
                .subscribe();
    }

    private List<App> filterList(List<App> appList, boolean userOnly) {
        List<App> filteredList = new ArrayList<>();
        for (App app : appList) {
            if (userOnly && app.isSystem())
                continue;
            filteredList.add(app);

        }
        return filteredList;
    }

    @Override
    protected void onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onCleared();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Accountant.DATA) && Accountant.isLoggedIn(getApplication()))
            fetchInstalledApps(userOnly);
        if (key.equals(Constants.PREFERENCE_INCLUDE_SYSTEM)) {
            userOnly = PrefUtil.getBoolean(getApplication(), Constants.PREFERENCE_INCLUDE_SYSTEM);
            fetchInstalledApps(userOnly);
        }
    }
}
