package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.iterator.ReviewStorageIterator;
import com.aurora.store.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewsHelper extends BaseTask {

    private List<Review> mReviewList = new ArrayList<>();

    private ReviewStorageIterator iterator;

    public ReviewsHelper(Context context) {
        super(context);
    }

    public void setIterator(ReviewStorageIterator iterator) {
        this.iterator = iterator;
    }

    public List<Review> getReviews() {
        mReviewList.clear();
        mReviewList.addAll(iterator.next());
        return mReviewList;
    }
}