package com.aurora.store.ui.category;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.enums.ErrorType;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.InvalidApiException;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.manager.FilterManager;
import com.aurora.store.model.App;
import com.aurora.store.task.CategoryAppsTask;
import com.aurora.store.viewmodel.BaseViewModel;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.CategoryAppsIterator;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.net.UnknownHostException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CategoryAppsModel extends BaseViewModel {

    private Application application;

    private GooglePlayAPI api;
    private CategoryAppsTask categoryAppsTask;
    private CustomAppListIterator iterator;


    private MutableLiveData<List<App>> listMutableLiveData = new MutableLiveData<>();
    private MutableLiveData<ErrorType> errorTypeMutableLiveData = new MutableLiveData<>();

    public CategoryAppsModel(@NonNull Application application) {
        super(application);
        this.application = application;
        this.api = AuroraApplication.api;
        this.categoryAppsTask = new CategoryAppsTask(application);
    }

    public LiveData<ErrorType> getError() {
        return errorTypeMutableLiveData;
    }

    public LiveData<List<App>> getCategoryApps() {
        return listMutableLiveData;
    }

    public void fetchCategoryApps(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory, boolean shouldIterate) {
        if (!shouldIterate)
            getIterator(categoryId, subcategory);
        Disposable disposable = Observable.fromCallable(() -> categoryAppsTask
                .getApps(iterator))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    listMutableLiveData.setValue(appList);
                }, err -> {
                    err.printStackTrace();
                    if (err instanceof CredentialsEmptyException || err instanceof InvalidApiException)
                        errorTypeMutableLiveData.setValue(ErrorType.LOGOUT_ERR);
                    else if (err instanceof AuthException)
                        errorTypeMutableLiveData.setValue(ErrorType.SESSION_EXPIRED);
                    else if (err instanceof UnknownHostException)
                        errorTypeMutableLiveData.setValue(ErrorType.NO_NETWORK);
                    else
                        errorTypeMutableLiveData.setValue(ErrorType.UNKNOWN);
                });
        compositeDisposable.add(disposable);
    }

    private void getIterator(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory) {
        try {
            CategoryAppsIterator categoryAppsIterator = new CategoryAppsIterator(api, categoryId, subcategory);
            iterator = new CustomAppListIterator(categoryAppsIterator);
            iterator.setFilterEnabled(true);
            iterator.setFilterModel(FilterManager.getFilterPreferences(application));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @Override
    protected void onCleared() {
        compositeDisposable.dispose();
        super.onCleared();
    }
}
