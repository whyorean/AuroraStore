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

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.playstore.BetaToggleTask;
import com.dragons.aurora.task.playstore.PlayStorePayloadTask;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Beta extends AbstractHelper {

    @BindView(R.id.beta_comment)
    EditText editText;
    @BindView(R.id.beta_card)
    LinearLayout beta_card;
    @BindView(R.id.beta_feedback)
    LinearLayout beta_feedback;
    @BindView(R.id.beta_subscribe_button)
    Button beta_subscribe_button;
    @BindView(R.id.beta_submit_button)
    Button beta_submit_button;
    @BindView(R.id.beta_delete_button)
    Button beta_delete_button;

    public Beta(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        if (Accountant.isDummy(context) && app.isTestingProgramAvailable() && app.isTestingProgramOptedIn()) {
            BetaToggleTask mBetaToggleTask = new BetaToggleTask(app);
            mBetaToggleTask.setContext(context);
            mBetaToggleTask.execute();
            return;
        }
        if (!app.isInstalled() || !app.isTestingProgramAvailable() || Accountant.isDummy(context)) {
            return;
        }

        setText(view, R.id.beta_header, app.isTestingProgramOptedIn()
                ? R.string.testing_program_section_opted_in_title
                : R.string.testing_program_section_opted_out_title);

        setText(view, R.id.beta_message, app.isTestingProgramOptedIn()
                ? R.string.testing_program_section_opted_in_message
                : R.string.testing_program_section_opted_out_message);

        setText(view, R.id.beta_subscribe_button, app.isTestingProgramOptedIn()
                ? R.string.testing_program_opt_out
                : R.string.testing_program_opt_in);

        setText(fragment.getView(), R.id.beta_email, app.getTestingProgramEmail());

        beta_card.setVisibility(View.VISIBLE);
        beta_feedback.setVisibility(app.isTestingProgramOptedIn() ? View.VISIBLE : View.GONE);
        beta_subscribe_button.setOnClickListener(new BetaOnClickListener(view.findViewById(R.id.beta_message), app));
        beta_submit_button.setOnClickListener(v -> initBetaTask(new BetaFeedbackSubmitTask()).execute());
        beta_delete_button.setOnClickListener(v -> initBetaTask(new BetaFeedbackDeleteTask()).execute());

        if (null != app.getUserReview() && !TextUtils.isEmpty(app.getUserReview().getComment())) {
            editText.setText(app.getUserReview().getComment());
            show(view, R.id.beta_delete_button);
        }
    }

    private BetaFeedbackTask initBetaTask(BetaFeedbackTask task) {
        task.setPackageName(app.getPackageName());
        task.setEditText(editText);
        task.setDeleteButton(view.findViewById(R.id.beta_delete_button));
        return task;
    }

    static private class BetaOnClickListener implements View.OnClickListener {

        private TextView messageView;
        private App app;

        private BetaOnClickListener(TextView messageView, App app) {
            this.messageView = messageView;
            this.app = app;
        }

        @Override
        public void onClick(View view) {
            view.setEnabled(false);
            messageView.setText(app.isTestingProgramOptedIn()
                    ? R.string.testing_program_section_opted_out_propagating_message
                    : R.string.testing_program_section_opted_in_propagating_message);

            new BetaToggleTask(app).execute();
        }
    }

    static abstract private class BetaFeedbackTask extends PlayStorePayloadTask<Void> {

        protected String packageName;
        protected EditText editText;
        protected View deleteButton;

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        private void setEditText(EditText editText) {
            this.editText = editText;
            setContext(editText.getContext());
        }

        private void setDeleteButton(View deleteButton) {
            this.deleteButton = deleteButton;
        }
    }

    static private class BetaFeedbackSubmitTask extends BetaFeedbackTask {

        @Override
        protected Void getResult(GooglePlayAPI api, String... arguments) throws IOException {
            api.betaFeedback(packageName, editText.getText().toString());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (success()) {
                ContextUtil.toastShort(context, context.getString(R.string.done));
                deleteButton.setVisibility(View.VISIBLE);
            }
            deleteButton.setVisibility(View.VISIBLE);
        }
    }

    static private class BetaFeedbackDeleteTask extends BetaFeedbackTask {

        @Override
        protected Void getResult(GooglePlayAPI api, String... arguments) throws IOException {
            api.deleteBetaFeedback(packageName);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (success()) {
                editText.setText("");
                ContextUtil.toastShort(context, context.getString(R.string.done));
                deleteButton.setVisibility(View.GONE);
            }
        }
    }

}