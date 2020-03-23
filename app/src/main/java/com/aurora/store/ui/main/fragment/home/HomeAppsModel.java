package com.aurora.store.ui.main.fragment.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.enums.ErrorType;
import com.aurora.store.model.App;
import com.aurora.store.model.items.ClusterItem;
import com.aurora.store.task.FeaturedAppsTask;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;
import com.aurora.store.viewmodel.BaseViewModel;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HomeAppsModel extends BaseViewModel {

    private Application application;
    private CompositeDisposable disposable = new CompositeDisposable();

    private Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();

    private MutableLiveData<List<ClusterItem>> mutableTopGames = new MutableLiveData<>();
    private MutableLiveData<List<ClusterItem>> mutableTopApps = new MutableLiveData<>();
    private MutableLiveData<List<ClusterItem>> mutableTopFamily = new MutableLiveData<>();
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

    public LiveData<List<ClusterItem>> getTopGames() {
        return mutableTopGames;
    }

    public LiveData<List<ClusterItem>> getTopApps() {
        return mutableTopApps;
    }

    public LiveData<List<ClusterItem>> getTopFamily() {
        return mutableTopFamily;
    }

    private void fetchAppsFromCache(String categoryId) {
        Type type = new TypeToken<List<App>>() {
        }.getType();
        String jsonString = PrefUtil.getString(application, categoryId);
        List<App> appList = gson.fromJson(jsonString, type);
        if (appList != null && !appList.isEmpty()) {
            Observable
                    .fromIterable(appList)
                    .map(ClusterItem::new)
                    .toList()
                    .doOnSuccess(clusterItems -> {
                        switch (categoryId) {
                            case Constants.TOP_APPS:
                                mutableTopApps.setValue(clusterItems);
                                break;
                            case Constants.TOP_GAME:
                                mutableTopGames.setValue(clusterItems);
                                break;
                            case Constants.TOP_FAMILY:
                                mutableTopFamily.setValue(clusterItems);
                                break;
                        }
                    })
                    .doOnError(this::handleError)
                    .subscribe();

        } else
            fetchApps(categoryId);
    }

    private void fetchApps(String categoryId) {
        Observable.fromCallable(() -> new FeaturedAppsTask(application)
                .getApps(api, getPlayCategoryId(categoryId), GooglePlayAPI.SUBCATEGORY.TOP_FREE))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(apps -> {
                    saveToCache(apps, categoryId);
                    Util.setCacheCreateTime(application, Calendar.getInstance().getTimeInMillis());
                    return Observable.fromIterable(apps).map(ClusterItem::new);
                })
                .toList()
                .doOnSuccess(clusterItems -> {
                    switch (categoryId) {
                        case Constants.TOP_APPS:
                            mutableTopApps.setValue(clusterItems);
                            break;
                        case Constants.TOP_GAME:
                            mutableTopGames.setValue(clusterItems);
                            break;
                        case Constants.TOP_FAMILY:
                            mutableTopFamily.setValue(clusterItems);
                            break;
                    }
                })
                .doOnError(this::handleError)
                .subscribe();
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
        String jsonString = gson.toJson(appList);
        PrefUtil.putString(application, key, jsonString);
    }

    @Override
    protected void onCleared() {
        disposable.dispose();
        super.onCleared();
    }
}
