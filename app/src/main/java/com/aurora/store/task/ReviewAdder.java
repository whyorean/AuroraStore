package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.model.Review;
import com.aurora.store.model.ReviewBuilder;
import com.dragons.aurora.playstoreapiv2.ReviewResponse;

import java.io.IOException;

public class ReviewAdder extends BaseTask {

    public ReviewAdder(Context context) {
        super(context);
    }

    public boolean submit(String packageName, Review review) throws IOException {
        ReviewResponse response = getApi().addOrEditReview(
                packageName,
                review.getComment(),
                review.getTitle(),
                review.getRating());
        Review apiReview = ReviewBuilder.build(response.getUserReview());
        return apiReview != null;
    }
}
