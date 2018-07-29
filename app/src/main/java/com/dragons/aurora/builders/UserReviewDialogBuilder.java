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

package com.dragons.aurora.builders;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;

import com.dragons.aurora.R;
import com.dragons.aurora.fragment.details.Review;
import com.dragons.aurora.task.playstore.ReviewAddTask;

public class UserReviewDialogBuilder {

    private Context context;
    private Review manager;
    private String packageName;

    private Dialog dialog;

    public UserReviewDialogBuilder(Context context, Review manager, String packageName) {
        this.context = context;
        this.manager = manager;
        this.packageName = packageName;
    }

    public Dialog show(final com.dragons.aurora.model.Review review) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_review);

        getCommentView().setText(review.getComment());
        getTitleView().setText(review.getTitle());

        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        dialog.setTitle(R.string.details_review_dialog_title);
        dialog.findViewById(R.id.review_dialog_done).setOnClickListener(new DoneOnClickListener(review));
        dialog.findViewById(R.id.review_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        return dialog;
    }

    private EditText getCommentView() {
        return (EditText) dialog.findViewById(R.id.review_dialog_review_comment);
    }

    private EditText getTitleView() {
        return (EditText) dialog.findViewById(R.id.review_dialog_review_title);
    }

    private class DoneOnClickListener implements View.OnClickListener {

        private final com.dragons.aurora.model.Review review;

        private DoneOnClickListener(com.dragons.aurora.model.Review review) {
            this.review = review;
        }

        @Override
        public void onClick(View v) {
            ReviewAddTask task = new ReviewAddTask();
            task.setContext(v.getContext());
            task.setPackageName(packageName);
            task.setFragment(manager);
            review.setComment(getCommentView().getText().toString());
            review.setTitle(getTitleView().getText().toString());
            task.setReview(review);
            task.execute();
            dialog.dismiss();
        }
    }
}
