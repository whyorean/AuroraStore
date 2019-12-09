package com.aurora.store.ui.main.fragment.category;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.task.CategoryListTask;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class CategoriesModel extends AndroidViewModel {

    private Application application;
    private CompositeDisposable disposable = new CompositeDisposable();

    private GooglePlayAPI api;
    private CategoryManager categoryManager;
    private MutableLiveData<Boolean> fetchCompleted = new MutableLiveData<>();

    public CategoriesModel(@NonNull Application application) {
        super(application);
        this.application = application;
        this.api = AuroraApplication.api;
        this.categoryManager = new CategoryManager(application);
    }

    public MutableLiveData<Boolean> getFetchCompleted() {
        return fetchCompleted;
    }

    public void fetchCategories() {
        if (categoryManager.categoryListEmpty())
            getCategoriesFromAPI();
        else
            fetchCompleted.setValue(true);
    }

    private void getCategoriesFromAPI() {
        disposable.add(Observable.fromCallable(() -> new CategoryListTask(application, api)
                .getResult())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    fetchCompleted.setValue(status);
                }, err -> {

                }));
    }

    @Override
    protected void onCleared() {
        disposable.dispose();
        super.onCleared();
    }
}
