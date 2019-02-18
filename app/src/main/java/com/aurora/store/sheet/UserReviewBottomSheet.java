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
