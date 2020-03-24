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

package com.aurora.store.ui.details.views;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aurora.store.AuroraApplication;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.model.Review;
import com.aurora.store.sheet.UserReviewBottomSheet;
import com.aurora.store.task.BaseTask;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.details.ReviewsActivity;
import com.aurora.store.ui.view.RatingView;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.Log;
import com.aurora.store.util.ViewUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.google.android.material.chip.Chip;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class Reviews extends AbstractDetails {

    @BindView(R.id.root_layout)
    LinearLayout rootLayout;
    @BindView(R.id.average_rating)
    TextView txtAverageRating;
    @BindView(R.id.count_stars)
    TextView txtCountStars;
    @BindView(R.id.average_rating_star)
    RatingBar averageRatingBar;
    @BindView(R.id.avg_rating_layout)
    LinearLayout avgRatingLayout;
    @BindView(R.id.user_stars)
    RatingBar userRatingBar;
    @BindView(R.id.user_comment_layout)
    LinearLayout userCommentLayout;
    @BindView(R.id.review_delete)
    Chip chipDelete;

    private CompositeDisposable disposable = new CompositeDisposable();

    public Reviews(DetailsActivity activity, App app) {
        super(activity, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, activity);

        if (!app.isInPlayStore() || app.isEarlyAccess())
            return;

        averageRatingBar.setRating(app.getRating().getAverage());
        txtAverageRating.setText(String.format(Locale.getDefault(), "%.1f", app.getRating().getAverage()));

        int totalStars = 0;
        for (int starNum = 1; starNum <= 5; starNum++) {
            totalStars += app.getRating().getStars(starNum);
        }

        txtCountStars.setText(String.valueOf(totalStars));
        avgRatingLayout.removeAllViews();
        avgRatingLayout.addView(addAvgReviews(5, totalStars, app.getRating().getStars(5)));
        avgRatingLayout.addView(addAvgReviews(4, totalStars, app.getRating().getStars(4)));
        avgRatingLayout.addView(addAvgReviews(3, totalStars, app.getRating().getStars(3)));
        avgRatingLayout.addView(addAvgReviews(2, totalStars, app.getRating().getStars(2)));
        avgRatingLayout.addView(addAvgReviews(1, totalStars, app.getRating().getStars(1)));

        show(R.id.reviews_container);
        if (isReviewable(app))
            show(R.id.user_review_layout);

        Review review = app.getUserReview();
        if (null != review) {
            fillUserReview(review);
        }

        initUserReviewControls(app);
    }

    private RelativeLayout addAvgReviews(int number, int max, int rating) {
        return new RatingView(activity, number, max, rating);
    }

    private boolean isReviewable(App app) {
        return !Accountant.isAnonymous(context) && app.isInstalled() && !app.isTestingProgramOptedIn();
    }

    private void fillUserReview(Review review) {
        clearUserReview();
        app.setUserReview(review);
        userRatingBar.setRating(review.getRating());
        setTextOrHide(R.id.user_comment, review.getComment());
        setTextOrHide(R.id.user_title, review.getTitle());
        setText(R.id.rate, R.string.details_you_rated_this_app);
        chipDelete.setVisibility(View.VISIBLE);
        userCommentLayout.setVisibility(View.VISIBLE);
    }

    private void clearUserReview() {
        userRatingBar.setRating(0);
        setText(R.id.user_title, "");
        setText(R.id.user_comment, "");
        setText(R.id.rate, R.string.details_rate_this_app);
        chipDelete.setVisibility(View.GONE);
        userCommentLayout.setVisibility(View.GONE);
    }

    private Review getUpdatedUserReview(Review oldReview, int stars) {
        Review review = new Review();
        review.setRating(stars);
        if (null != oldReview) {
            review.setComment(oldReview.getComment());
            review.setTitle(oldReview.getTitle());
        }
        return review;
    }

    private void initUserReviewControls(final App app) {
        userRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (!fromUser) {
                return;
            }
            UserReviewBottomSheet reviewsBottomSheet = new UserReviewBottomSheet();
            reviewsBottomSheet.setApp(app);
            reviewsBottomSheet.setRating((int) rating);
            reviewsBottomSheet.show(activity.getSupportFragmentManager(), "USER_REVIEWS");
        });

        chipDelete.setOnClickListener(v -> {
            disposable.add(Observable.fromCallable(() -> new ReviewRemover(context)
                    .delete(app.getPackageName()))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((success) -> {
                        if (success) {
                            clearUserReview();
                        } else {
                            Log.e("Error deleting the review");
                        }
                    }, err -> Log.e(err.getMessage())));
        });
    }

    private void setTextOrHide(int viewId, String text) {
        TextView textView = activity.findViewById(viewId);
        if (!TextUtils.isEmpty(text)) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.more_reviews_layout)
    public void openReviewsActivity() {
        Intent intent = new Intent(activity, ReviewsActivity.class);
        intent.putExtra("INTENT_PACKAGE_NAME", app.getPackageName());
        activity.startActivity(intent, ViewUtil.getEmptyActivityBundle(activity));
    }

    static class ReviewRemover extends BaseTask {

        ReviewRemover(Context context) {
            super(context);
        }

        private boolean delete(String packageName) {
            try {
                GooglePlayAPI api = AuroraApplication.api;
                api.deleteReview(packageName);
                return true;
            } catch (Exception e) {
                Log.e(e.getMessage());
                return false;
            }
        }
    }
}