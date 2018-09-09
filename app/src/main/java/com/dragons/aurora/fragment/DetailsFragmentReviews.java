package com.dragons.aurora.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.dragons.aurora.EndlessRecyclerViewScrollListener;
import com.dragons.aurora.R;
import com.dragons.aurora.ReviewStorageIterator;
import com.dragons.aurora.adapters.ReviewsAdapter;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.Review;
import com.dragons.aurora.task.playstore.ReviewLoadTaskHelper;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class DetailsFragmentReviews extends BaseFragment {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.reviews_recycler)
    RecyclerView mRecyclerView;

    private App app;
    private ReviewsAdapter mReviewsAdapter;
    private ReviewStorageIterator iterator;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    public DetailsFragmentReviews() {
    }

    public void setApp(App app) {
        this.app = app;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details_reviews, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mToolbar.setNavigationIcon(R.drawable.ic_cancel);
        mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        mToolbar.setTitle(app.getDisplayName());

        iterator = new ReviewStorageIterator();
        iterator.setPackageName(app.getPackageName());
        iterator.setContext(getActivity());
        getReviews(false);
    }

    private void getReviews(boolean shouldIterate) {
        ReviewLoadTaskHelper mTask = new ReviewLoadTaskHelper(getContext());
        mTask.setIterator(iterator);
        mDisposable.add(Observable.fromCallable(() -> mTask.getReviews())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(err -> Timber.e(err.getMessage()))
                .subscribe(mReviewList -> {
                    if (shouldIterate)
                        addReviews(mReviewList);
                    else
                        setupRecycler(mReviewList);
                }));
    }

    private void setupRecycler(List<Review> mReviewList) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        DividerItemDecoration itemDecorator = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(getResources().getDrawable(R.drawable.list_divider));
        mReviewsAdapter = new ReviewsAdapter(getContext(), mReviewList);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_falldown));
        mRecyclerView.addItemDecoration(itemDecorator);
        mRecyclerView.setAdapter(mReviewsAdapter);
        EndlessRecyclerViewScrollListener mEndlessRecyclerViewScrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager) {
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
