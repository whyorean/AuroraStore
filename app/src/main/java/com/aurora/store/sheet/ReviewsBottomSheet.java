/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.sheet;

import android.content.Context;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ReviewsBottomSheet extends BottomSheetDialogFragment {

    @BindView(R.id.reviews_recycler)
    RecyclerView recyclerView;

    private Context context;
    private App app;
    private ReviewsAdapter adapter;
    private ReviewStorageIterator iterator;
    private CompositeDisposable disposable = new CompositeDisposable();

    public ReviewsBottomSheet(App app) {
        this.app = app;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
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
        iterator.setContext(context);
        getReviews(false);
    }

    private void getReviews(boolean shouldIterate) {
        ReviewsHelper reviewsHelper = new ReviewsHelper(getContext());
        reviewsHelper.setIterator(iterator);
        disposable.add(Observable.fromCallable(reviewsHelper::getReviews)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(err -> Log.e(err.getMessage()))
                .subscribe(reviewList -> {
                    if (shouldIterate)
                        addReviews(reviewList);
                    else
                        setupRecycler(reviewList);
                }));
    }

    private void setupRecycler(List<Review> reviewList) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        adapter = new ReviewsAdapter(getContext(), reviewList);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
        EndlessScrollListener endlessScrollListener = new EndlessScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                getReviews(true);
            }
        };
        recyclerView.addOnScrollListener(endlessScrollListener);
    }

    private void addReviews(List<Review> reviewList) {
        if (!reviewList.isEmpty() && adapter != null) {
            for (Review mReview : reviewList)
                adapter.add(mReview);
            adapter.notifyItemInserted(adapter.getItemCount() - 1);
        }
    }
}
