package com.aurora.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.iterator.ReviewRetrieverIterator;
import com.aurora.store.iterator.ReviewStorageIterator;
import com.aurora.store.model.Review;
import com.aurora.store.task.ReviewsHelper;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ReviewsModel extends BaseViewModel {

    private ReviewStorageIterator iterator;

    private MutableLiveData<List<Review>> listMutableLiveData = new MutableLiveData<>();

    public ReviewsModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Review>> getReviews() {
        return listMutableLiveData;
    }

    public void fetchReviews(String packageName, GooglePlayAPI.REVIEW_SORT reviewSort, boolean rebuild) {

        if (iterator == null || rebuild)
            getIterator(packageName, reviewSort);

        disposable.add(Observable.fromCallable(() -> new ReviewsHelper(iterator)
                .getReviews())
                .subscribeOn(Schedulers.computation())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((reviewList) -> {
                    listMutableLiveData.setValue(reviewList);
                }, err -> handleError(err)));
    }

    private void getIterator(String packageName, GooglePlayAPI.REVIEW_SORT reviewSort) {
        try {
            iterator = new ReviewStorageIterator();
            iterator.setPackageName(packageName);

            ReviewRetrieverIterator retrieverIterator = new ReviewRetrieverIterator();
            retrieverIterator.setPackageName(packageName);
            retrieverIterator.setReviewSort(reviewSort);

            iterator.setRetrievingIterator(retrieverIterator);
        } catch (Exception err) {
            handleError(err);
        }
    }

    @Override
    protected void onCleared() {
        disposable.dispose();
        super.onCleared();
    }
}
