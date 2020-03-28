package com.aurora.store.ui.single.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.service.ValidateApiService;
import com.aurora.store.events.Event;
import com.aurora.store.ui.accounts.AccountsActivity;
import com.aurora.store.ui.intro.IntroActivity;
import com.aurora.store.ui.main.AuroraActivity;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.NetworkUtil;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;
import com.google.android.material.button.MaterialButton;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;

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

    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        //Check if intro was shown to user & launch intro activity first
        if (!PrefUtil.getBoolean(this, Constants.PREFERENCE_DO_NOT_SHOW_INTRO)) {
            startActivity(new Intent(this, IntroActivity.class));
            supportFinishAfterTransition();
        } else {
            awaiting = true;
            disposable.add(AuroraApplication
                    .getRelayBus()
                    .doOnNext(event -> {
                        switch (event.getSubType()) {
                            case NETWORK_AVAILABLE: {
                                if (awaiting)
                                    buildAndTestApi();
                                break;
                            }
                            case NETWORK_UNAVAILABLE: {
                                status.setText(getString(R.string.error_no_network));
                                break;
                            }
                            case API_SUCCESS: {
                                status.setText(getString(R.string.toast_api_all_ok));
                                awaiting = false;
                                launchAuroraActivity();
                                break;
                            }
                            case API_FAILED: {
                                status.setText(getString(R.string.toast_api_build_retrying));
                                awaiting = true;
                                break;
                            }
                            case API_ERROR: {
                                status.setText(getString(R.string.toast_api_build_failed));
                                awaiting = false;
                                launchAccountsActivity();
                                break;
                            }
                        }
                    })
                    .subscribe());

            if (NetworkUtil.isConnected(this)) {
                buildAndTestApi();
            } else {
                awaiting = true;
                AuroraApplication.rxNotify(new Event(Event.SubType.NETWORK_UNAVAILABLE));
            }
        }
    }

    @Override
    protected void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }

    private void buildAndTestApi() {
        //Setup a timer for 10 sec, to allow user to skip Splash screen
        setupTimer();

        //Stop service if already running, anyway it will never complete.
        if (ValidateApiService.isServiceRunning())
            stopService();

        //Start new Validation service, if logged in.
        if (Accountant.isLoggedIn(this)) {
            status.setText(R.string.toast_api_build_api);
            Util.startValidationService(this);
        } else {
            launchAccountsActivity();
        }
    }

    private void stopService() {
        Log.i("Validation Service Stopped");
        stopService(new Intent(this, ValidateApiService.class));
    }

    private void launchAuroraActivity() {
        disposable.clear();
        final Intent intent = new Intent(this, AuroraActivity.class);
        startActivity(intent);
        supportFinishAfterTransition();
    }

    private void launchAccountsActivity() {
        disposable.clear();
        final Intent intent = new Intent(this, AccountsActivity.class);
        startActivity(intent);
        supportFinishAfterTransition();
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
        launchAuroraActivity();
    }
}
