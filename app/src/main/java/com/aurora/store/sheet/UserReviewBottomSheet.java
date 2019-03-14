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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.model.Review;
import com.aurora.store.task.ReviewAdder;
import com.aurora.store.utility.Log;
import com.aurora.store.view.CustomBottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class UserReviewBottomSheet extends CustomBottomSheetDialogFragment {

    @BindView(R.id.review_title)
    TextInputEditText txtTitle;
    @BindView(R.id.review_comment)
    TextInputEditText txtComment;
    @BindView(R.id.btn_cancel)
    Button btnCancel;
    @BindView(R.id.btn_submit)
    Button btnSubmit;

    private String title;
    private String comment;
    private int rating;

    private App app;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    public void setApp(App app) {
        this.app = app;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_user_review, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        btnSubmit.setOnClickListener(v -> {
            Review review = getReview();
            submitUserReview(review);
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void submitUserReview(Review review) {
        mDisposable.add(Observable.fromCallable(() ->
                new ReviewAdder(getContext()).submit(app.getPackageName(), review))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    if (success) {
                        Log.e("Review Updated");
                    } else {
                        Log.e("Error deleting the review");
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
}
