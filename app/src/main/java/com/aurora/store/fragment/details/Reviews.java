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

package com.aurora.store.fragment.details;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.iterator.ReviewStorageIterator;
import com.aurora.store.model.App;
import com.aurora.store.model.Review;
import com.aurora.store.sheet.ReviewsBottomSheet;
import com.aurora.store.sheet.UserReviewBottomSheet;
import com.aurora.store.task.BaseTask;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.RatingView;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class Reviews extends AbstractHelper {

    @BindView(R.id.img_review)
    ImageButton imgReview;
    @BindView(R.id.average_rating)
    TextView txtAverageRating;
    @BindView(R.id.count_stars)
    TextView txtCountStars;
    @BindView(R.id.average_rating_star)
    RatingBar averageRatingBar;
    @BindView(R.id.reviews_container)
    LinearLayout reviewLayout;
    @BindView(R.id.avg_rating_layout)
    LinearLayout avgRatingLayout;
    @BindView(R.id.reviews_list)
    LinearLayout reviewListLayout;
    @BindView(R.id.user_review_layout)
    LinearLayout userReviewLayout;
    @BindView(R.id.user_stars)
    RatingBar userRatingBar;
    @BindView(R.id.user_comment_layout)
    LinearLayout userCommentLayout;
    @BindView(R.id.review_delete)
    Chip chipDelete;

    private ReviewStorageIterator iterator;
    private CompositeDisposable disposable = new CompositeDisposable();

    public Reviews(DetailsFragment fragment, App app) {
        super(fragment, app);
        iterator = new ReviewStorageIterator();
        iterator.setPackageName(app.getPackageName());
        iterator.setContext(fragment.getActivity());
    }

    @Override
    public void draw() {
        if (fragment != null && fragment.isVisible()) {
            ButterKnife.bind(this, view);
            if (!app.isInPlayStore() || app.isEarlyAccess())
                return;
            else
                disposable.add(Observable.fromCallable(() -> new ReviewLoader(context, iterator)
                        .getReviews())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::showReviews, err -> Log.e(err.getMessage())));

            averageRatingBar.setRating(app.getRating().getAverage());
            txtAverageRating.setText(String.format(Locale.getDefault(), "%.1f", app.getRating().getAverage()));

            int totalStars = 0;
            for (int starNum = 1; starNum <= 5; starNum++) {
                totalStars += app.getRating().getStars(starNum);
            }

            txtCountStars.setText(String.valueOf(totalStars));
            avgRatingLayout.addView(addAvgReviews(5, totalStars, app.getRating().getStars(5)));
            avgRatingLayout.addView(addAvgReviews(4, totalStars, app.getRating().getStars(4)));
            avgRatingLayout.addView(addAvgReviews(3, totalStars, app.getRating().getStars(3)));
            avgRatingLayout.addView(addAvgReviews(2, totalStars, app.getRating().getStars(2)));
            avgRatingLayout.addView(addAvgReviews(1, totalStars, app.getRating().getStars(1)));

            ViewUtil.setVisibility(reviewLayout, true);
            ViewUtil.setVisibility(userReviewLayout, isReviewable(app));

            Review review = app.getUserReview();
            if (null != review) {
                fillUserReview(review);
            }

            initUserReviewControls(app);
            setupLoadMore();
        }
    }

    private RelativeLayout addAvgReviews(int number, int max, int rating) {
        return new RatingView(context, number, max, rating);
    }

    private boolean isReviewable(App app) {
        return app.isInstalled() && Accountant.isGoogle(context) && !app.isTestingProgramOptedIn();
    }

    private void fillUserReview(Review review) {
        clearUserReview();
        app.setUserReview(review);
        userRatingBar.setRating(review.getRating());
        setTextOrHide(R.id.user_comment, review.getComment());
        setTextOrHide(R.id.user_title, review.getTitle());
        setText(fragment.getView(), R.id.rate, R.string.details_you_rated_this_app);
        chipDelete.setVisibility(View.VISIBLE);
        userCommentLayout.setVisibility(View.VISIBLE);
    }

    private void clearUserReview() {
        userRatingBar.setRating(0);
        setText(fragment.getView(), R.id.user_title, "");
        setText(fragment.getView(), R.id.user_comment, "");
        setText(fragment.getView(), R.id.rate, R.string.details_rate_this_app);
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

    private void showReviews(List<Review> reviews) {
        reviewListLayout.removeAllViews();
        addReviewToList(reviews.get(0), reviewListLayout);
        addReviewToList(reviews.get(1), reviewListLayout);
    }

    private void addReviewToList(Review review, ViewGroup parent) {
        RelativeLayout reviewLayout = (RelativeLayout) fragment.getLayoutInflater()
                .inflate(R.layout.item_review_list, parent, false);
        TextView txtAuthor = reviewLayout.findViewById(R.id.author);
        txtAuthor.setText(review.getUserName());
        RatingBar ratingBar = reviewLayout.findViewById(R.id.rating);
        ratingBar.setRating(review.getRating());
        TextView txtComment = reviewLayout.findViewById(R.id.comment);
        txtComment.setText(review.getComment());
        ImageView imageView = reviewLayout.findViewById(R.id.avatar);

        GlideApp
                .with(context)
                .load(review.getUserPhotoUrl())
                .placeholder(R.color.colorTransparent)
                .circleCrop()
                .transition(new DrawableTransitionOptions().crossFade())
                .into(imageView);

        reviewLayout.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setIcon(imageView.getDrawable())
                    .setTitle(review.getUserName())
                    .setMessage(review.getComment())
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
            builder.create();
            builder.show();
        });
        parent.addView(reviewLayout);
    }

    private void initUserReviewControls(final App app) {
        userRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (!fromUser) {
                return;
            }
            UserReviewBottomSheet reviewsBottomSheet = new UserReviewBottomSheet();
            reviewsBottomSheet.setApp(app);
            reviewsBottomSheet.setRating((int) rating);
            reviewsBottomSheet.show(fragment.getChildFragmentManager(), "USER_REVIEWS");
        });

        chipDelete.setOnClickListener(v -> disposable.add(Observable.fromCallable(() -> new ReviewRemover(context)
                .delete(app.getPackageName()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    if (success) {
                        clearUserReview();
                    } else {
                        Log.e("Error deleting the review");
                    }
                }, err -> Log.e(err.getMessage()))));
    }

    private void setTextOrHide(int viewId, String text) {
        TextView textView = view.findViewById(viewId);
        if (!TextUtils.isEmpty(text)) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void setupLoadMore() {
        imgReview.setOnClickListener(v -> {
            ReviewsBottomSheet reviewsBottomSheet = new ReviewsBottomSheet(app);
            reviewsBottomSheet.show(fragment.getChildFragmentManager(), "REVIEWS");
        });
    }

    static class ReviewRemover extends BaseTask {
        ReviewRemover(Context context) {
            super(context);
        }

        private boolean delete(String packageName) {
            try {
                GooglePlayAPI api = getApi();
                api.deleteReview(packageName);
                return true;
            } catch (Exception e) {
                Log.e(e.getMessage());
                return false;
            }
        }
    }

    static class ReviewLoader extends BaseTask {
        private ReviewStorageIterator iterator;

        ReviewLoader(Context context, ReviewStorageIterator iterator) {
            super(context);
            this.iterator = iterator;
        }

        private List<Review> getReviews() {
            return iterator.next();
        }
    }
}