package com.aurora.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.model.App;
import com.aurora.store.task.InstalledAppsTask;
import com.aurora.store.util.PrefUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BlackListedAppsModel extends BaseViewModel {

    private Gson gson = new Gson();

    private MutableLiveData<List<App>> listMutableLiveData = new MutableLiveData<>();

    public BlackListedAppsModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<App>> getAllApps() {
        return listMutableLiveData;
    }

    public void getAllBlackListedApps() {
        Type type = new TypeToken<List<App>>() {
        }.getType();
        String jsonString = PrefUtil.getString(getApplication(), Constants.PREFERENCE_INSTALLED_APPS);
        List<App> appList = gson.fromJson(jsonString, type);
        if (appList == null || appList.isEmpty())
            fetchBlackListedApps();
        else {
            listMutableLiveData.setValue(appList);
        }
    }

    public void fetchBlackListedApps() {
        api = AuroraApplication.api;
        disposable.add(Observable.fromCallable(() -> new InstalledAppsTask(api, getApplication())
                .getAllApps())
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
