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
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.model.Review;
import com.aurora.store.model.ReviewBuilder;
import com.aurora.store.task.BaseTask;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.ReviewResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class UserReviewBottomSheet extends BottomSheetDialogFragment {

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

    private Context context;
    private App app;
    private CompositeDisposable disposable = new CompositeDisposable();

    public void setApp(App app) {
        this.app = app;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
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
            submitUserReview(getReview());
            dismissAllowingStateLoss();
        });
        btnCancel.setOnClickListener(v -> dismissAllowingStateLoss());
    }

    private void submitUserReview(Review review) {
        disposable.add(Observable.fromCallable(() -> new ReviewAdder(context)
                .submit(app.getPackageName(), review))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    if (success) {
                        ContextUtil.toastShort(context, "Review updated");
                    } else {
                        ContextUtil.toastShort(context, "Review failed");
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
                GooglePlayAPI api = getApi();
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
