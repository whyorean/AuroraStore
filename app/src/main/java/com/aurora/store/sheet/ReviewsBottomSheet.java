package com.aurora.store.sheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.EndlessScrollListener;
import com.aurora.store.R;
import com.aurora.store.adapter.ReviewsAdapter;
import com.aurora.store.iterator.ReviewStorageIterator;
import com.aurora.store.model.App;
import com.aurora.store.model.Review;
import com.aurora.store.task.ReviewsHelper;
import com.aurora.store.utility.Log;
import com.aurora.store.view.CustomBottomSheetDialogFragment;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ReviewsBottomSheet extends CustomBottomSheetDialogFragment {

    @BindView(R.id.reviews_recycler)
    RecyclerView mRecyclerView;

    private App app;
    private ReviewsAdapter mReviewsAdapter;
    private ReviewStorageIterator iterator;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    public ReviewsBottomSheet(App app) {
        this.app = app;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_reviews, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        iterator = new ReviewStorageIterator();
        iterator.setPackageName(app.getPackageName());
        iterator.setContext(getActivity());
        getReviews(false);
    }

    private void getReviews(boolean shouldIterate) {
        ReviewsHelper mTask = new ReviewsHelper(getContext());
        mTask.setIterator(iterator);
        mDisposable.add(Observable.fromCallable(mTask::getReviews)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(err -> Log.e(err.getMessage()))
                .subscribe(mReviewList -> {
                    if (shouldIterate)
                        addReviews(mReviewList);
                    else
                        setupRecycler(mReviewList);
                }));
    }

    private void setupRecycler(List<Review> mReviewList) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        mReviewsAdapter = new ReviewsAdapter(getContext(), mReviewList);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mReviewsAdapter);
        EndlessScrollListener mEndlessRecyclerViewScrollListener = new EndlessScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                getReviews(true);
            }
        };
        mRecyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);
    }

    private void addReviews(List<Review> mReviews) {
        if (!mReviews.isEmpty() && mReviewsAdapter != null) {
            for (Review mReview : mReviews)
                mReviewsAdapter.add(mReview);
            mReviewsAdapter.notifyItemInserted(mReviewsAdapter.getItemCount() - 1);
        }
    }
}
