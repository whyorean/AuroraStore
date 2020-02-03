package com.aurora.store.ui.search;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.task.SuggestionTask;
import com.aurora.store.viewmodel.BaseViewModel;
import com.dragons.aurora.playstoreapiv2.SearchSuggestEntry;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SearchSuggestionModel extends BaseViewModel {

    private MutableLiveData<List<SearchSuggestEntry>> listMutableLiveData = new MutableLiveData<>();

    public SearchSuggestionModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<SearchSuggestEntry>> getSuggestions() {
        return listMutableLiveData;
    }

    public void fetchSuggestions(String query) {
        api = AuroraApplication.api;
        compositeDisposable.clear();
        compositeDisposable.add(Observable.fromCallable(() -> new SuggestionTask(api)
                .getSearchSuggestions(query))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    listMutableLiveData.setValue(appList);
                }, err -> handleError(err)));
    }

    public void discardRequests() {
        compositeDisposable.clear();
    }

    @Override
    protected void onCleared() {
        compositeDisposable.dispose();
        super.onCleared();
    }
}
