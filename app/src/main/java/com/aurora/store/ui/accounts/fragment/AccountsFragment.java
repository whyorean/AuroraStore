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

package com.aurora.store.ui.accounts.fragment;

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
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.ui.single.activity.GoogleLoginActivity;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.NetworkUtil;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.Image;
import com.dragons.aurora.playstoreapiv2.UserProfile;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AccountsFragment extends Fragment {

    private static final String URL_TOS = "https://www.google.com/mobile/android/market-tos.html";
    private static final String URL_LICENSE = "https://gitlab.com/AuroraOSS/AuroraStore/raw/master/LICENSE";
    private static final String URL_DISCLAIMER = "https://gitlab.com/AuroraOSS/AuroraStore/raw/master/DISCLAIMER";

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
    @BindView(R.id.img)
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
    @BindView(R.id.btn_anonymous)
    Button btnAnonymous;
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
    public void onResume() {
        super.onResume();
        if (Accountant.isLoggedIn(context)) {
            init();
            Util.validateApi(context);
        }
    }

    @Override
    public void onDestroy() {
        disposable.clear();
        super.onDestroy();
    }

    @OnClick(R.id.btn_positive)
    public void openLoginActivity() {
        context.startActivity(new Intent(context, GoogleLoginActivity.class));
    }

    @OnClick(R.id.btn_negative)
    public void clearAccountantData() {
        Accountant.completeCheckout(context);
        init();
    }

    @OnClick(R.id.btn_anonymous)
    public void loginAnonymous() {
        if (!NetworkUtil.isConnected(context)) {
            Toast.makeText(context, getString(R.string.error_no_network), Toast.LENGTH_SHORT).show();
            return;
        }

        disposable.add(Observable.fromCallable(() -> PlayStoreApiAuthenticator
                .login(context))
                .map(api -> api.userProfile().getUserProfile())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> {
                    btnAnonymous.setText(getText(R.string.action_logging_in));
                    btnAnonymous.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                })
                .doOnComplete(() -> ContextUtil.runOnUiThread(() -> resetAnonymousLogin()))
                .subscribe(userProfile -> {
                    Toast.makeText(context, "Successfully logged in", Toast.LENGTH_LONG).show();
                    Accountant.setAnonymous(context, true);
                    updateUI(userProfile);
                }, err -> {
                    Toast.makeText(context, err.getMessage(), Toast.LENGTH_LONG).show();
                    ContextUtil.runOnUiThread(() -> resetAnonymousLogin());
                }));
    }

    private void init() {
        boolean isLoggedIn = Accountant.isLoggedIn(context);
        progressBar.setVisibility(View.INVISIBLE);
        switchTopViews(isLoggedIn);
        switchBottomViews(isLoggedIn);
        setupChips();
        setupProfile();
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

    private void updateUI(UserProfile userProfile) {
        PrefUtil.putString(context, Accountant.PROFILE_NAME, userProfile.getName());
        for (Image image : userProfile.getImageList()) {
            if (image.getImageType() == GooglePlayAPI.IMAGE_TYPE_APP_ICON) {
                PrefUtil.putString(context, Accountant.PROFILE_AVATAR, image.getImageUrl());
            }
        }
        setupProfile();
        init();
    }

    private void setupProfile() {
        GlideApp
                .with(this)
                .load(Accountant.getImageURL(context))
                .placeholder(R.drawable.circle_bg)
                .circleCrop()
                .into(imgAvatar);
        txtName.setText(Accountant.isAnonymous(context) ? getText(R.string.account_dummy) : Accountant.getUserName(context));
        txtMail.setText(Accountant.isAnonymous(context) ? "auroraoss@gmail.com" : Accountant.getEmail(context));
    }

    private void resetAnonymousLogin() {
        btnAnonymous.setEnabled(true);
        btnAnonymous.setText(getText(R.string.account_dummy));
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void openWebView(String URL) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
        } catch (Exception e) {
            Log.e("No WebView found !");
        }
    }
}
