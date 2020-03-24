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

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aurora.store.AuroraApplication;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.task.BaseTask;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.ReviewResponse;
import com.dragons.aurora.playstoreapiv2.TestingProgramResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class Beta extends AbstractDetails {

    @BindView(R.id.root_layout)
    LinearLayout rootLayout;
    @BindView(R.id.beta_comment)
    TextInputEditText editText;
    @BindView(R.id.beta_layout)
    RelativeLayout beta_card;
    @BindView(R.id.beta_feedback)
    RelativeLayout beta_feedback;
    @BindView(R.id.beta_message)
    TextView txt_beta_message;
    @BindView(R.id.beta_submit_button)
    Button beta_submit_button;
    @BindView(R.id.beta_delete_button)
    Button beta_delete_button;

    private CompositeDisposable disposable = new CompositeDisposable();

    public Beta(DetailsActivity activity, App app) {
        super(activity, app);
    }

    static private void restartActivity(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(activity.getIntent());
        activity.overridePendingTransition(0, 0);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, activity);
        if (Accountant.isAnonymous(context) || !app.isTestingProgramAvailable())
            return;

        setText(R.id.beta_header, app.isTestingProgramOptedIn()
                ? R.string.testing_program_section_opted_in_title
                : R.string.testing_program_section_opted_out_title);

        setText(R.id.beta_message, app.isTestingProgramOptedIn()
                ? R.string.testing_program_section_opted_in_message
                : R.string.testing_program_section_opted_out_message);

        setText(R.id.beta_subscribe_button, app.isTestingProgramOptedIn()
                ? R.string.testing_program_opt_out
                : R.string.testing_program_opt_in);

        setText(R.id.beta_email, app.getTestingProgramEmail());

        beta_card.setVisibility(View.VISIBLE);
        beta_feedback.setVisibility(app.isTestingProgramOptedIn() ? View.VISIBLE : View.GONE);

        beta_delete_button.setOnClickListener(v ->
                disposable.add(Observable.fromCallable(() -> new BetaFeedbackDeleteTask(context)
                        .deleteFeedback(app.getPackageName()))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((success) -> {
                            editText.setText("");
                            ContextUtil.toastShort(context, context.getString(R.string.action_done));
                            beta_delete_button.setEnabled(false);
                        })));

        if (null != app.getUserReview() && !TextUtils.isEmpty(app.getUserReview().getComment())) {
            editText.setText(app.getUserReview().getComment());
            show(R.id.beta_delete_button);
        }
    }

    @OnClick(R.id.beta_subscribe_button)
    public void subscribeToBeta(MaterialButton button) {
        button.setEnabled(false);
        disposable.add(Observable.fromCallable(() -> new BetaFeedbackToggleTask(context)
                .toggle(app))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.hasResult()) {
                        if (response.getResult().getDetails().hasFlag1()) {
                            ContextUtil.toastLong(context, context.getString(response.getResult().getDetails().getFlag1()
                                    ? R.string.testing_program_opt_in_success
                                    : R.string.testing_program_opt_in_failed));
                            Beta.restartActivity(activity);
                        }

                        if (response.getResult().getDetails().hasUnsubscribed()) {
                            ContextUtil.toastLong(context, context.getString(response.getResult().getDetails().getUnsubscribed()
                                    ? R.string.testing_program_opt_out_success
                                    : R.string.testing_program_opt_out_failed));
                            Beta.restartActivity(activity);
                        }
                    }
                }, err -> {
                    ContextUtil.toastLong(context, context.getString(R.string.download_failed));
                    Log.d(err.getMessage());
                }));
    }

    @OnClick(R.id.beta_submit_button)
    public void submitBetaReview(MaterialButton button) {
        button.setEnabled(false);
        disposable.add(Observable.fromCallable(() -> new BetaFeedbackSubmitTask(context)
                .addFeedback(app.getPackageName(), editText.getText().toString()))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response != null && response.hasUserReview()) {
                        beta_delete_button.setVisibility(View.VISIBLE);
                        beta_delete_button.setEnabled(true);
                    }
                }, err -> {
                    ContextUtil.toastLong(context, context.getString(R.string.download_failed));
                    Log.d(err.getMessage());
                }));
    }

    static private class BetaFeedbackToggleTask extends BaseTask {

        BetaFeedbackToggleTask(Context context) {
            super(context);
        }

        private TestingProgramResponse toggle(App app) {
            try {
                GooglePlayAPI api = AuroraApplication.api;
                return api.testingProgram(app.getPackageName(), !app.isTestingProgramOptedIn());
            } catch (IOException e) {
                return null;
            }
        }
    }

    static private class BetaFeedbackSubmitTask extends BaseTask {

        BetaFeedbackSubmitTask(Context context) {
            super(context);
        }

        private ReviewResponse addFeedback(String packageName, String feedback) {
            try {
                GooglePlayAPI api = AuroraApplication.api;
                return api.betaFeedback(packageName, feedback);
            } catch (Exception e) {
                Log.e(e.getMessage());
                return null;
            }
        }
    }

    static private class BetaFeedbackDeleteTask extends BaseTask {

        BetaFeedbackDeleteTask(Context context) {
            super(context);
        }

        private boolean deleteFeedback(String packageName) {
            try {
                GooglePlayAPI api = AuroraApplication.api;
                api.deleteBetaFeedback(packageName);
                return true;
            } catch (Exception e) {
                Log.e(e.getMessage());
                return false;
            }
        }
    }

}