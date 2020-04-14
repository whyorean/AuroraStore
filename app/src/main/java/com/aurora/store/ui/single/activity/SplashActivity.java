package com.aurora.store.ui.single.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.diff.NavigationUtil;
import com.aurora.store.worker.ApiValidator;
import com.google.android.material.button.MaterialButton;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SplashActivity extends BaseActivity {

    @BindView(R.id.img)
    AppCompatImageView img;
    @BindView(R.id.title)
    AppCompatTextView title;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.status)
    AppCompatTextView status;
    @BindView(R.id.action)
    MaterialButton action;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        if (!PrefUtil.getBoolean(this, Constants.PREFERENCE_DO_NOT_SHOW_INTRO)) {
            NavigationUtil.launchIntroActivity(this);
            finish();
        } else if (!Accountant.isLoggedIn(this)) {
            NavigationUtil.launchAccountsActivity(this);
            finish();
        } else {
            buildAndTestApi();
        }
    }

    private void buildAndTestApi() {
        //Setup a timer for 10 sec, to allow user to skip Splash screen
        setupTimer();

        final Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ApiValidator.class)
                .setConstraints(constraints)
                .addTag(ApiValidator.TAG)
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.getId())
                .observe(this, workInfo -> {
                    switch (workInfo.getState()) {
                        case ENQUEUED:
                            status.setText(R.string.toast_api_build_api);
                            break;

                        case FAILED:
                            status.setText(R.string.toast_api_build_failed);
                            break;

                        case SUCCEEDED:
                            status.setText(R.string.toast_api_all_ok);
                            NavigationUtil.launchAuroraActivity(this);
                            finish();
                            break;

                        case BLOCKED:
                            status.setText(R.string.error_no_network);
                            break;
                    }
                });
    }

    private void setupTimer() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                ContextUtil.runOnUiThread(() -> {
                    action.setVisibility(View.VISIBLE);
                    status.setText(getString(R.string.toast_api_build_delayed));
                });
            }
        }, 10000 /*10 seconds timeout*/);
    }

    @OnClick(R.id.action)
    public void getThroughSplash() {
        NavigationUtil.launchAuroraActivity(this);
        finish();
    }
}
