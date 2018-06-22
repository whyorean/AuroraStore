/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.dragons.aurora.fragment.details;

import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.ReviewStorageIterator;
import com.dragons.aurora.builders.UserReviewDialogBuilder;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.ReviewDeleteTask;
import com.dragons.aurora.task.playstore.ReviewLoadTask;
import com.squareup.picasso.Picasso;

import java.util.List;

public class Review extends AbstractHelper {

    static private int[] averageStarIds = new int[]{R.id.average_stars1, R.id.average_stars2, R.id.average_stars3, R.id.average_stars4, R.id.average_stars5};

    private ReviewStorageIterator iterator;

    public Review(DetailsFragment fragment, App app) {
        super(fragment, app);
        iterator = new ReviewStorageIterator();
        iterator.setPackageName(app.getPackageName());
        iterator.setContext(fragment.getActivity());
    }

    @Override
    public void draw() {
        if (!app.isInPlayStore() || app.isEarlyAccess())
            return;
        else
            getTask(true).execute();

        TextView viewHeader = fragment.getActivity().findViewById(R.id.review_header);
        LinearLayout viewContainer = fragment.getActivity().findViewById(R.id.reviews_container);
        show(fragment.getView(), R.id.rev_card);
        viewHeader.setOnClickListener(v -> {
            boolean isExpanded = viewContainer.getVisibility() == View.VISIBLE;
            if (isExpanded) {
                viewContainer.setVisibility(View.GONE);
                ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0);
            } else {
                viewContainer.setVisibility(View.VISIBLE);
                ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_less, 0);
            }
        });

        initReviewListControls();
        ((RatingBar) fragment.getActivity().findViewById(R.id.average_rating_star)).setRating(app.getRating().getAverage() / 5.0f);
        setText(fragment.getView(), R.id.average_rating, R.string.details_rating, app.getRating().getAverage());
        for (int starNum = 1; starNum <= 5; starNum++) {
            setText(fragment.getView(), averageStarIds[starNum - 1], R.string.details_rating_specific, starNum, app.getRating().getStars(starNum));
        }

        fragment.getActivity().findViewById(R.id.user_review_container).setVisibility(isReviewable(app) ? View.VISIBLE : View.GONE);
        com.dragons.aurora.model.Review review = app.getUserReview();
        initUserReviewControls(app);
        if (null != review) {
            fillUserReview(review);
        }
    }

    private boolean isReviewable(App app) {
        return app.isInstalled()
                && !app.isTestingProgramOptedIn()
                && !PreferenceManager.getDefaultSharedPreferences(fragment.getActivity()).getBoolean(PlayStoreApiAuthenticator.PREFERENCE_APP_PROVIDED_EMAIL, false)
                ;
    }

    public void fillUserReview(com.dragons.aurora.model.Review review) {
        clearUserReview();
        app.setUserReview(review);
        ((RatingBar) fragment.getActivity().findViewById(R.id.user_stars)).setRating(review.getRating());
        setTextOrHide(R.id.user_comment, review.getComment());
        setTextOrHide(R.id.user_title, review.getTitle());
        setText(fragment.getView(), R.id.rate, R.string.details_you_rated_this_app);
        fragment.getActivity().findViewById(R.id.user_review_edit_delete).setVisibility(View.VISIBLE);
        fragment.getActivity().findViewById(R.id.user_review).setVisibility(View.VISIBLE);
    }

    public void clearUserReview() {
        ((RatingBar) fragment.getActivity().findViewById(R.id.user_stars)).setRating(0);
        setText(fragment.getView(), R.id.user_title, "");
        setText(fragment.getView(), R.id.user_comment, "");
        setText(fragment.getView(), R.id.rate, R.string.details_rate_this_app);
        fragment.getActivity().findViewById(R.id.user_review_edit_delete).setVisibility(View.GONE);
        fragment.getActivity().findViewById(R.id.user_review).setVisibility(View.GONE);
    }

    private com.dragons.aurora.model.Review getUpdatedUserReview(com.dragons.aurora.model.Review oldReview, int stars) {
        com.dragons.aurora.model.Review review = new com.dragons.aurora.model.Review();
        review.setRating(stars);
        if (null != oldReview) {
            review.setComment(oldReview.getComment());
            review.setTitle(oldReview.getTitle());
        }
        return review;
    }

    public void showReviews(List<com.dragons.aurora.model.Review> reviews) {
        if (fragment.getActivity() != null) {
            fragment.getActivity().findViewById(R.id.reviews_previous).setVisibility(iterator.hasPrevious() ? View.VISIBLE : View.INVISIBLE);
            fragment.getActivity().findViewById(R.id.reviews_next).setVisibility(iterator.hasNext() ? View.VISIBLE : View.INVISIBLE);
            LinearLayout listView = (LinearLayout) fragment.getActivity().findViewById(R.id.reviews_list);
            listView.removeAllViews();
            for (com.dragons.aurora.model.Review review : reviews) {
                addReviewToList(review, listView);
            }
        }
    }

    private ReviewLoadTask getTask(boolean next) {
        ReviewLoadTask task = new ReviewLoadTask();
        task.setIterator(iterator);
        task.setFragment(this);
        task.setNext(next);
        task.setContext(fragment.getActivity());
        task.setProgressIndicator(fragment.getActivity().findViewById(R.id.progress));
        return task;
    }

    private void addReviewToList(com.dragons.aurora.model.Review review, ViewGroup parent) {
        LinearLayout reviewLayout = (LinearLayout) fragment.getActivity().getLayoutInflater().inflate(R.layout.review_list_item, parent, false);
        ((TextView) reviewLayout.findViewById(R.id.author)).setText(review.getUserName());
        ((TextView) reviewLayout.findViewById(R.id.title)).setText(fragment.getActivity().getString(
                R.string.two_items,
                fragment.getActivity().getString(R.string.details_rating, (double) review.getRating()),
                review.getTitle()
        ));
        ((TextView) reviewLayout.findViewById(R.id.comment)).setText(review.getComment());
        Picasso
                .with(fragment.getActivity())
                .load(review.getUserPhotoUrl())
                .placeholder(R.color.transparent)
                .into((ImageView) reviewLayout.findViewById(R.id.avatar));

        parent.addView(reviewLayout);
    }

    private void initReviewListControls() {
        View.OnClickListener listener = v -> getTask(v.getId() == R.id.reviews_next).execute();
        fragment.getActivity().findViewById(R.id.reviews_previous).setOnClickListener(listener);
        fragment.getActivity().findViewById(R.id.reviews_next).setOnClickListener(listener);
    }

    private void initUserReviewControls(final App app) {
        ((RatingBar) fragment.getActivity().findViewById(R.id.user_stars)).setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (!fromUser) {
                return;
            }
            new UserReviewDialogBuilder(fragment.getActivity(), Review.this, app.getPackageName())
                    .show(getUpdatedUserReview(app.getUserReview(), (int) rating));
        });
        fragment.getActivity().findViewById(R.id.user_review_edit).setOnClickListener(v ->
                new UserReviewDialogBuilder(fragment.getActivity(), Review.this, app.getPackageName())
                        .show(app.getUserReview()));
        fragment.getActivity().findViewById(R.id.user_review_delete).setOnClickListener(v -> {
            ReviewDeleteTask task = new ReviewDeleteTask();
            task.setFragment(Review.this);
            task.setContext(v.getContext());
            task.execute(app.getPackageName());
        });
    }

    private void setTextOrHide(int viewId, String text) {
        TextView textView = (TextView) fragment.getActivity().findViewById(viewId);
        if (!TextUtils.isEmpty(text)) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }
}