package com.aurora.store.fragment.details;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.aurora.store.task.BaseTask;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class Beta extends AbstractHelper {

    @BindView(R.id.beta_comment)
    EditText editText;
    @BindView(R.id.beta_layout)
    RelativeLayout beta_card;
    @BindView(R.id.beta_feedback)
    LinearLayout beta_feedback;
    @BindView(R.id.beta_message)
    TextView txt_beta_message;
    @BindView(R.id.beta_subscribe_button)
    Button beta_subscribe_button;
    @BindView(R.id.beta_submit_button)
    Button beta_submit_button;
    @BindView(R.id.beta_delete_button)
    Button beta_delete_button;

    private CompositeDisposable mDisposable = new CompositeDisposable();

    public Beta(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        if (Accountant.isDummy(context) && app.isTestingProgramAvailable() && app.isTestingProgramOptedIn()) {
            mDisposable.add(Observable.fromCallable(() -> new BetaFeedbackToggleTask(context).toggle(app))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe());
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
        beta_subscribe_button.setOnClickListener(new BetaOnClickListener(txt_beta_message, app));
        beta_submit_button.setOnClickListener(v -> mDisposable.add(Observable.fromCallable(() ->
                new BetaFeedbackSubmitTask(context).deleteFeedback(app.getPackageName(), editText.getText().toString()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    beta_delete_button.setVisibility(View.VISIBLE);
                    beta_delete_button.setEnabled(true);
                })));
        beta_delete_button.setOnClickListener(v -> mDisposable.add(Observable.fromCallable(() ->
                new BetaFeedbackDeleteTask(context).deleteFeedback(app.getPackageName()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    editText.setText("");
                    ContextUtil.toastShort(context, context.getString(R.string.action_done));
                    beta_delete_button.setEnabled(false);
                })));

        if (null != app.getUserReview() && !TextUtils.isEmpty(app.getUserReview().getComment())) {
            editText.setText(app.getUserReview().getComment());
            show(view, R.id.beta_delete_button);
        }
    }

    static private class BetaOnClickListener implements View.OnClickListener {

        private TextView messageView;
        private App app;
        private CompositeDisposable mDisposable = new CompositeDisposable();

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
            mDisposable.add(Observable.fromCallable(() -> new BetaFeedbackToggleTask(messageView.getContext()).toggle(app))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe());
            mDisposable.dispose();
        }
    }

    static private class BetaFeedbackToggleTask extends BaseTask {

        BetaFeedbackToggleTask(Context context) {
            super(context);
        }

        private boolean toggle(App app) {
            try {
                GooglePlayAPI api = getApi();
                api.testingProgram(app.getPackageName(), !app.isTestingProgramOptedIn());
                return true;
            } catch (IOException e) {
                Log.e(e.getMessage());
                return false;
            }
        }
    }

    static private class BetaFeedbackSubmitTask extends BaseTask {

        BetaFeedbackSubmitTask(Context context) {
            super(context);
        }

        private boolean deleteFeedback(String packageName, String feedback) {
            try {
                GooglePlayAPI api = getApi();
                api.betaFeedback(packageName, feedback);
                return true;
            } catch (IOException e) {
                Log.e(e.getMessage());
                return false;
            }
        }
    }

    static private class BetaFeedbackDeleteTask extends BaseTask {

        BetaFeedbackDeleteTask(Context context) {
            super(context);
        }

        private boolean deleteFeedback(String packageName) {
            try {
                GooglePlayAPI api = getApi();
                api.deleteBetaFeedback(packageName);
                return true;
            } catch (IOException e) {
                Log.e(e.getMessage());
                return false;
            }
        }
    }

}