package com.aurora.store.ui.installed;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.model.App;
import com.aurora.store.task.InstalledAppsTask;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.viewmodel.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class InstalledAppsModel extends BaseViewModel implements SharedPreferences.OnSharedPreferenceChangeListener {

    private boolean userOnly;

    private MutableLiveData<List<App>> listMutableLiveData = new MutableLiveData<>();

    public InstalledAppsModel(@NonNull Application application) {
        super(application);
        this.userOnly = PrefUtil.getBoolean(application, Constants.PREFERENCE_INCLUDE_SYSTEM);
    }

    public LiveData<List<App>> getListMutableLiveData() {
        return listMutableLiveData;
    }

    public void fetchInstalledApps(boolean userOnly) {
        api = AuroraApplication.api;
        disposable.add(Observable.fromCallable(() -> new InstalledAppsTask(api, getApplication())
                .getInstalledApps())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    List<App> filteredList = getFilteredList(appList, userOnly);
                    listMutableLiveData.setValue(filteredList);
                }, err -> handleError(err)));
    }

    private List<App> getFilteredList(List<App> appList, boolean userOnly) {
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
        disposable.dispose();
        super.onCleared();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Accountant.DATA) && Accountant.isLoggedIn(getApplication()))
            fetchInstalledApps(userOnly);
    }
}
