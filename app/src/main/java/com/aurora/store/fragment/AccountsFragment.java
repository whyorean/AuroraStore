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

package com.aurora.store.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.activity.IntroActivity;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.events.Event;
import com.aurora.store.events.Events;
import com.aurora.store.events.RxBus;
import com.aurora.store.exception.CaptchaException;
import com.aurora.store.exception.TwoFactorAuthException;
import com.aurora.store.exception.UnknownException;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.sheet.CaptchaSheet;
import com.aurora.store.task.UserProfiler;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.Image;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.net.UnknownHostException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.aurora.store.utility.ContextUtil.runOnUiThread;

public class AccountsFragment extends Fragment {

    private static final String URL_TOS = "https://www.google.com/mobile/android/market-tos.html";
    private static final String URL_LICENSE = "https://gitlab.com/AuroraOSS/AuroraStore/raw/master/LICENSE";
    private static final String URL_DISCLAIMER = "https://gitlab.com/AuroraOSS/AuroraStore/raw/master/DISCLAIMER";
    private static final String URL_APP_PASS = "https://myaccount.google.com/apppasswords";

    @BindView(R.id.view_switcher_top)
    ViewSwitcher mViewSwitcherTop;
    @BindView(R.id.view_switcher_bottom)
    ViewSwitcher mViewSwitcherBottom;
    @BindView(R.id.init)
    LinearLayout initLayout;
    @BindView(R.id.info)
    LinearLayout infoLayout;
    @BindView(R.id.login)
    LinearLayout loginLayout;
    @BindView(R.id.logout)
    LinearLayout logoutLayout;
    @BindView(R.id.login_google)
    RelativeLayout loginGoogle;
    @BindView(R.id.avatar)
    ImageView imgAvatar;
    @BindView(R.id.user_name)
    TextView txtName;
    @BindView(R.id.user_mail)
    TextView txtMail;
    @BindView(R.id.txt_input_email)
    TextInputEditText txtInputEmail;
    @BindView(R.id.txt_input_password)
    TextInputEditText txtInputPassword;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.btn_positive)
    Button btnPositive;
    @BindView(R.id.btn_negative)
    Button btnNegative;
    @BindView(R.id.chip_tos)
    Chip chipTos;
    @BindView(R.id.chip_disclaimer)
    Chip chipDisclaimer;
    @BindView(R.id.chip_license)
    Chip chipLicense;
    @BindView(R.id.check_save_password)
    MaterialCheckBox materialCheckBox;

    private Context context;
    private CompositeDisposable disposable = new CompositeDisposable();
    private boolean isLoggedIn = false;

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }

    @OnClick(R.id.btn_unlock_captcha)
    void showUnlockCaptcha(){
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/accounts/DisplayUnlockCaptcha")));
        } catch (Exception e) {
            Log.e("No WebView found !");
        }
    }

    private void init() {
        isLoggedIn = Accountant.isLoggedIn(context);
        progressBar.setVisibility(View.INVISIBLE);
        setupView();
        setupCheckbox();
        setupChips();
        setupActions();
    }

    private void setupView() {
        if (isLoggedIn) {
            loadGoogleData();
        }
        switchTopViews(isLoggedIn);
        switchBottomViews(isLoggedIn);
        switchButtonState(isLoggedIn);
    }

    private void setupCheckbox() {
        boolean isChecked = Util.isPasswordSaved(context);

        if (isChecked) {
            txtInputEmail.setText(Accountant.getEmail(context));
            txtInputPassword.setText(Accountant.getPassword(context));
        }

        materialCheckBox.setChecked(isChecked);
        materialCheckBox.setOnCheckedChangeListener((buttonView, checked) -> {
            Util.setPasswordSaved(context, checked);
        });
    }

    private void setupChips() {
        chipTos.setOnClickListener(v -> {
            openWebView(URL_TOS);
        });
        chipDisclaimer.setOnClickListener(v -> {
            openWebView(URL_DISCLAIMER);
        });
        chipLicense.setOnClickListener(v -> {
            openWebView(URL_LICENSE);
        });
    }

    private void setupActions() {
        btnPositive.setOnClickListener(loginListener());
        btnNegative.setOnClickListener(logoutListener());
    }

    private void loadGoogleData() {
        GlideApp
                .with(this)
                .load(Accountant.getImageURL(context))
                .circleCrop()
                .into(imgAvatar);
        txtName.setText(Accountant.getUserName(context));
        txtMail.setText(Accountant.getEmail(context));
    }

    private void switchTopViews(boolean showInfo) {
        if (mViewSwitcherTop.getCurrentView() == initLayout && showInfo)
            mViewSwitcherTop.showNext();
        else if (mViewSwitcherTop.getCurrentView() == infoLayout && !showInfo)
            mViewSwitcherTop.showPrevious();
    }

    private void switchBottomViews(boolean showLogout) {
        if (mViewSwitcherBottom.getCurrentView() == loginLayout && showLogout)
            mViewSwitcherBottom.showNext();
        else if (mViewSwitcherBottom.getCurrentView() == logoutLayout && !showLogout)
            mViewSwitcherBottom.showPrevious();
    }

    private void switchButtonState(boolean logging) {
        btnPositive.setText(logging ? R.string.action_logging_in : R.string.action_login);
        btnPositive.setEnabled(!logging);
    }

    private View.OnClickListener logoutListener() {
        return v -> {
            Accountant.completeCheckout(context);
            init();
        };
    }

    private View.OnClickListener loginListener() {
        return v -> {
            final String email = txtInputEmail.getText() != null
                    ? txtInputEmail.getText().toString() : StringUtils.EMPTY;
            final String password = txtInputPassword.getText() != null
                    ? txtInputPassword.getText().toString() : StringUtils.EMPTY;
            if (email.isEmpty())
                txtInputEmail.setError("?");
            if (password.isEmpty())
                txtInputPassword.setError("?");
            if (!email.isEmpty() && !password.isEmpty()) {
                LoginInfo loginInfo = new LoginInfo();
                loginInfo.setEmail(email);
                loginInfo.setPassword(password);
                logInWithGoogle(loginInfo);
            }
        };
    }

    private void logInWithGoogle(LoginInfo loginInfo) {
        switchButtonState(true);
        disposable.add(Observable.fromCallable(() -> PlayStoreApiAuthenticator
                .login(context, loginInfo.getEmail(), loginInfo.getPassword()))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(sub -> progressBar.setVisibility(View.VISIBLE))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(() -> progressBar.setVisibility(View.INVISIBLE))
                .subscribe((success) -> {
                    if (success) {
                        Log.i("Google Login Successful");
                        RxBus.publish(new Event(Events.LOGGED_IN));
                        runOnUiThread(() -> {
                            Accountant.setLoggedIn(context);
                            PrefUtil.putString(context, Accountant.ACCOUNT_EMAIL, loginInfo.getEmail());
                            PrefUtil.putString(context, Accountant.ACCOUNT_PASSWORD, loginInfo.getPassword());
                            getUserInfo();
                            finishIntro();
                        });
                    } else {
                        Log.e("Google Login Failed Permanently");
                        switchButtonState(false);
                    }
                }, err -> {
                    if (err instanceof UnknownHostException) {
                        ContextUtil.toastLong(context, context.getString(R.string.error_no_network));
                    } else if (err instanceof TwoFactorAuthException) {
                        show2FADialog();
                    } else if (err instanceof UnknownException) {
                        ContextUtil.toastLong(context, getString(R.string.toast_unknown_reason));
                    } else if (err instanceof CaptchaException) {
                        Log.e("Captcha Error 2");
                        loginInfo.setLoginToken(((CaptchaException) err).getLoginToken());
                        loginInfo.setCaptchaUrl(((CaptchaException) err).getCaptchaURL());
                        showCaptchaDialog(loginInfo);
                    } else if (err instanceof AuthException) {
                        int code = ((AuthException) err).getCode();
                        if (code == 403) {
                            txtInputPassword.setError("Check your password");
                        }
                    } else
                        Log.e("Something went wrong -> err code : %s", err.getMessage());
                    ContextUtil.runOnUiThread(() -> switchButtonState(false));
                }));
    }


    private void getUserInfo() {
        disposable.add(Observable.fromCallable(() ->
                new UserProfiler(context).getUserProfile())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((profile) -> {
                    if (profile != null) {
                        PrefUtil.putString(context, Accountant.GOOGLE_NAME, profile.getName());
                        for (Image image : profile.getImageList()) {
                            if (image.getImageType() == GooglePlayAPI.IMAGE_TYPE_APP_ICON) {
                                PrefUtil.putString(context, Accountant.GOOGLE_URL, image.getImageUrl());
                            }
                        }
                        runOnUiThread(this::init);
                    }
                }, err -> Log.e("Google Login failed : %s", err.getMessage())));
    }

    private void finishIntro() {
        if (getActivity() instanceof IntroActivity) {
            getActivity().startActivity(new Intent(context, AuroraActivity.class));
            getActivity().finish();
        }
    }

    private void openWebView(String URL) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
        } catch (Exception e) {
            Log.e("No WebView found !");
        }
    }

    private void show2FADialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.dialog_2FA_title))
                .setMessage(getString(R.string.dialog_2FA_desc))
                .setPositiveButton(getString(R.string.dialog_2FA_positive), (dialog, which) -> {
                    openWebView(URL_APP_PASS);
                })
                .setNegativeButton(getString(R.string.action_later), (dialog, which) -> {
                    dialog.dismiss();
                });
        builder.create();
        builder.show();
    }

    private void showCaptchaDialog(LoginInfo loginInfo) {
        CaptchaSheet captchaSheet = new CaptchaSheet();
        captchaSheet.setLoginInfo(loginInfo);
        captchaSheet.show(getChildFragmentManager(), "");
    }
}
