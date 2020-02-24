package com.aurora.store.ui.search;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.task.SuggestionTask;
import com.aurora.store.util.Log;
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
        Observable.fromCallable(() -> new SuggestionTask(api)
                .getSearchSuggestions(query))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(searchSuggestEntries -> listMutableLiveData.setValue(searchSuggestEntries))
                .doOnError(throwable -> Log.e(throwable.getMessage()))
                .subscribe();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
