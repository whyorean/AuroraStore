package com.aurora.store.ui.main.fragment.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.enums.ErrorType;
import com.aurora.store.model.App;
import com.aurora.store.task.FeaturedAppsTask;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HomeAppsModel extends AndroidViewModel {

    private Application application;
    private CompositeDisposable disposable = new CompositeDisposable();

    private GooglePlayAPI api;
    private Gson gson = new Gson();

    private MutableLiveData<List<App>> mutableTopGames = new MutableLiveData<>();
    private MutableLiveData<List<App>> mutableTopApps = new MutableLiveData<>();
    private MutableLiveData<List<App>> mutableTopFamily = new MutableLiveData<>();
    private MutableLiveData<ErrorType> mutableError = new MutableLiveData<>();

    public HomeAppsModel(@NonNull Application application) {
        super(application);
        this.application = application;
        this.api = AuroraApplication.api;

        //Fetch top chart apps
        fetchAppsFromCache(Constants.TOP_APPS);
        fetchAppsFromCache(Constants.TOP_GAME);
        fetchAppsFromCache(Constants.TOP_FAMILY);
    }

    public LiveData<ErrorType> getError() {
        return mutableError;
    }

    public LiveData<List<App>> getTopGames() {
        return mutableTopGames;
    }

    public LiveData<List<App>> getTopApps() {
        return mutableTopApps;
    }

    public LiveData<List<App>> getTopFamily() {
        return mutableTopFamily;
    }

    private void fetchAppsFromCache(String categoryId) {
        Type type = new TypeToken<List<App>>() {
        }.getType();
        String jsonString = PrefUtil.getString(application, categoryId);
        List<App> appList = gson.fromJson(jsonString, type);
        if (appList != null && !appList.isEmpty()) {
            switch (categoryId) {
                case Constants.TOP_APPS:
                    mutableTopApps.setValue(appList);
                    break;
                case Constants.TOP_GAME:
                    mutableTopGames.setValue(appList);
                    break;
                case Constants.TOP_FAMILY:
                    mutableTopFamily.setValue(appList);
                    break;
            }
        } else
            fetchApps(categoryId);
    }

    private void fetchApps(String categoryId) {
        disposable.add(Observable.fromCallable(() -> new FeaturedAppsTask(application)
                .getApps(api, getPlayCategoryId(categoryId), GooglePlayAPI.SUBCATEGORY.TOP_FREE))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    switch (categoryId) {
                        case Constants.TOP_APPS:
                            mutableTopApps.setValue(appList);
                            break;
                        case Constants.TOP_GAME:
                            mutableTopGames.setValue(appList);
                            break;
                        case Constants.TOP_FAMILY:
                            mutableTopFamily.setValue(appList);
                            break;
                    }
                    saveToCache(appList, categoryId);
                    Util.setCacheCreateTime(application, Calendar.getInstance().getTimeInMillis());
                }, err -> {
                    mutableError.setValue(ErrorType.UNKNOWN);
                }));
    }

    private String getPlayCategoryId(String categoryId) {
        switch (categoryId) {
            case Constants.TOP_FAMILY:
                return Constants.CATEGORY_FAMILY;
            case Constants.TOP_GAME:
                return Constants.CATEGORY_GAME;
            default:
                return Constants.CATEGORY_APPS;
        }
    }

    private void saveToCache(List<App> appList, String key) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(appList);
        PrefUtil.putString(application, key, jsonString);
    }

    @Override
    protected void onCleared() {
        disposable.dispose();
        super.onCleared();
    }
}
