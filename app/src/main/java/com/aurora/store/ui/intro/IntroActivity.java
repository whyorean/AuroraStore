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

package com.aurora.store.ui.intro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.ui.main.AuroraActivity;
import com.aurora.store.ui.preference.SettingsActivity;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.ui.single.activity.GoogleLoginActivity;
import com.aurora.store.ui.single.activity.SplashActivity;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.NetworkUtil;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.button.MaterialButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class IntroActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    NavController navController;
    @BindView(R.id.btn_next)
    MaterialButton btnNext;
    @BindView(R.id.btn_anonymous)
    MaterialButton btnAnonymous;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        ButterKnife.bind(this);
        setupActionbar();
        setupNavigation();

        //Do not show this again. Unless asked.
        PrefUtil.putBoolean(this, Constants.PREFERENCE_DO_NOT_SHOW_INTRO, true);
        Accountant.completeCheckout(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_intro, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_setting) {
            startActivity(new Intent(this, SettingsActivity.class), ViewUtil.getEmptyActivityBundle(this));
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Accountant.isLoggedIn(this)) {
            startActivity(new Intent(this, AuroraActivity.class));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void setupActionbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setElevation(0f);
        }
    }

    private void setupNavigation() {
        navController = Navigation.findNavController(this, R.id.nav_host_intro);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            switch (destination.getId()) {
                case R.id.welcomeFragment: {
                    btnNext.setOnClickListener(v -> moveForward(2));
                    break;
                }
                case R.id.permFragment: {
                    btnNext.setOnClickListener(askPermListener());
                    if (isPermissionGranted())
                        moveForward(3);
                    break;
                }
                case R.id.loginFragment: {
                    btnNext.setText(R.string.account_google);
                    btnAnonymous.setVisibility(View.VISIBLE);
                    btnNext.setOnClickListener(v -> {
                        loginGoogle();
                    });

                    btnAnonymous.setOnClickListener(v -> {
                        loginAnonymous();
                    });
                }

            }
        });
    }

    private void moveForward(int pos) {
        switch (pos) {
            case 1:
                navController.navigate(R.id.welcomeFragment);
                break;
            case 2:
                navController.navigate(R.id.permFragment);
                break;
            case 3:
                navController.navigate(R.id.loginFragment);
                break;
            case 4:
                finishAfterTransition();
                break;
        }
    }

    public void loginAnonymous() {
        if (!NetworkUtil.isConnected(this)) {
            Toast.makeText(this, getString(R.string.error_no_network), Toast.LENGTH_SHORT).show();
            return;
        }

        Disposable disposable = Observable.fromCallable(() -> PlayStoreApiAuthenticator
                .login(this))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> {
                    btnAnonymous.setText(getText(R.string.action_logging_in));
                    btnAnonymous.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                })
                .subscribe(api -> {
                    if (api != null) {
                        Toast.makeText(this, getText(R.string.toast_login_success), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, SplashActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
                        supportFinishAfterTransition();
                    } else {
                        Toast.makeText(this, getText(R.string.toast_login_failed), Toast.LENGTH_LONG).show();
                        ContextUtil.runOnUiThread(this::resetAnonymousLogin);
                    }
                }, err -> {
                    Toast.makeText(this, getText(R.string.toast_login_failed), Toast.LENGTH_LONG).show();
                    ContextUtil.runOnUiThread(this::resetAnonymousLogin);
                });
        compositeDisposable.add(disposable);
    }

    public void loginGoogle() {
        if (!NetworkUtil.isConnected(this)) {
            Toast.makeText(this, getString(R.string.error_no_network), Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, GoogleLoginActivity.class), ViewUtil.getEmptyActivityBundle(this));
    }

    private void resetAnonymousLogin() {
        btnAnonymous.setEnabled(true);
        btnAnonymous.setText(getText(R.string.account_dummy));
        progressBar.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener askPermListener() {
        btnNext.setText(R.string.action_ask);
        return v -> {
            checkPermissions();
        };
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1337);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1337: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    navController.navigate(R.id.loginFragment);
                } else {
                    Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
