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

import com.aurora.store.AuroraApplication;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.model.Review;
import com.aurora.store.model.ReviewBuilder;
import com.aurora.store.task.BaseTask;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.ReviewResponse;
import com.google.android.material.textfield.TextInputEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class UserReviewBottomSheet extends BaseBottomSheet {

    @BindView(R.id.review_title)
    TextInputEditText txtTitle;
    @BindView(R.id.review_comment)
    TextInputEditText txtComment;

    private String title;
    private String comment;
    private int rating;

    private App app;
    private CompositeDisposable disposable = new CompositeDisposable();

    public UserReviewBottomSheet() {
    }

    public void setApp(App app) {
        this.app = app;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public View onCreateContentView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_user_review, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);
    }

    @OnClick(R.id.btn_positive)
    public void submitReview() {
        submitUserReview(getReview());
        dismissAllowingStateLoss();
    }

    @OnClick(R.id.btn_negative)
    public void closeReview() {
        dismissAllowingStateLoss();
    }

    private void submitUserReview(Review review) {
        disposable.add(Observable.fromCallable(() -> new ReviewAdder(requireContext())
                .submit(app.getPackageName(), review))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    if (success) {
                        ContextUtil.toastShort(requireContext(), "Review updated");
                    } else {
                        ContextUtil.toastShort(requireContext(), "Review failed");
                    }
                }, err -> Log.e(err.getMessage())));

    }

    private Review getReview() {
        if (txtTitle.getText() != null)
            title = txtTitle.getText().toString();
        if (txtComment.getText() != null)
            comment = txtComment.getText().toString();
        Review review = new Review();
        review.setTitle(title);
        review.setRating(rating);
        review.setComment(comment);
        return review;
    }

    static class ReviewAdder extends BaseTask {

        ReviewAdder(Context context) {
            super(context);
        }

        boolean submit(String packageName, Review review) {
            try {
                GooglePlayAPI api = AuroraApplication.api;
                ReviewResponse response = api.addOrEditReview(
                        packageName,
                        review.getComment(),
                        review.getTitle(),
                        review.getRating());
                ReviewBuilder.build(response.getUserReview());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
