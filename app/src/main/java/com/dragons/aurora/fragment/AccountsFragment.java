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

package com.dragons.aurora.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.Aurora;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.dialogs.GenericDialog;
import com.dragons.aurora.dialogs.LoginDialog;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.task.UserProvidedCredentialsTask;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import butterknife.BindView;
import butterknife.ButterKnife;

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.isConnected;
import static com.dragons.aurora.Util.setText;
import static com.dragons.aurora.Util.show;

public class AccountsFragment extends BaseFragment {

    private static final String urlTOS = "https://www.google.com/mobile/android/market-tos.html";

    @BindView(R.id.chip_tos)
    Chip chipTOS;
    @BindView(R.id.chip_add)
    Chip chipAdd;
    @BindView(R.id.btn_logout)
    Button logout_dummy;
    @BindView(R.id.btn_logoutG)
    Button logout_google;
    @BindView(R.id.btn_refresh)
    Button refresh_dummy;
    @BindView(R.id.btn_switch)
    Button switch_dummy;
    @BindView(R.id.btn_switchG)
    Button switch_google;
    @BindView(R.id.btn_remove)
    Button remove_google;
    @BindView(R.id.avatar_dummy)
    ImageView avatar_dummy;
    @BindView(R.id.avatar_google)
    ImageView avatar_google;
    @BindView(R.id.dummyCard)
    MaterialCardView layout_dummy;
    @BindView(R.id.googleCard)
    MaterialCardView layout_google;

    private boolean isSecAvailable;
    private String myEmail;
    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_accounts, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        myEmail = Prefs.getString(getActivity(), Aurora.PREFERENCE_EMAIL);
        isSecAvailable = Prefs.getBoolean(getActivity(), Aurora.SEC_ACCOUNT);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        if (Accountant.isLoggedIn(getContext()) && Accountant.isGoogle(getContext()))
            drawGoogle();
        else if (Accountant.isLoggedIn(getContext()) && Accountant.isDummy(getContext()))
            drawDummy();
        else
            askLogin();

        chipTOS.setChipStrokeWidth(2);
        chipAdd.setChipStrokeWidth(2);
        chipTOS.setOnClickListener(v -> getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse(urlTOS))));
        chipAdd.setOnClickListener(v -> showGoogleDialog());
    }

    private void drawDummy() {
        avatar_dummy.setImageResource(R.drawable.ic_dummy_avatar);
        setText(view, R.id.account_name, R.string.acc_dummy_name);
        setText(view, R.id.account_email, myEmail);
        setText(view, R.id.account_gsf, R.string.device_gsfID, Prefs.getString(getActivity(),
                Aurora.PREFERENCE_GSF_ID));
        if (isSecAvailable)
            drawEmptyGoogle();
        else
            show(view, R.id.chip_add);

        drawDummyButtons();
    }

    private void drawGoogle() {
        drawEmptyDummy();

        hide(view, R.id.chip_add);
        show(view, R.id.googleCard);

        setText(view, R.id.account_nameG, Prefs.getString(getActivity(), Aurora.GOOGLE_NAME));
        setText(view, R.id.account_emailG, myEmail);
        setText(view, R.id.account_gsf, R.string.device_gsfID, Prefs.getString(getActivity(),
                Aurora.PREFERENCE_GSF_ID));

        switch_google.setOnClickListener(view -> showGoogleDialog());

        if (isConnected(getActivity()))
            loadAvatar(Prefs.getString(getActivity(), Aurora.GOOGLE_URL));

        drawGoogleButtons();
    }

    private void drawEmptyDummy() {
        show(view, R.id.dummy_tapToSwitch);
        setText(view, R.id.account_name, R.string.acc_dummy_name);
        setText(view, R.id.account_email, R.string.account_dummy_email);
        layout_dummy.setOnClickListener(v -> Accountant.loginWithDummy(getContext()));
    }

    private void drawEmptyGoogle() {
        show(view, R.id.googleCard);
        show(view, R.id.btn_remove);
        show(view, R.id.google_tapToSwitch);

        setText(view, R.id.account_nameG, Prefs.getString(getActivity(), Aurora.GOOGLE_NAME));
        setText(view, R.id.account_emailG, Prefs.getString(getActivity(), Aurora.GOOGLE_EMAIL));

        layout_google.setOnClickListener(click -> new UserProvidedCredentialsTask(getContext()).withSavedGoogle());
        remove_google.setOnClickListener(click -> {
            new UserProvidedCredentialsTask(getContext()).removeGooglePrefs();
            hide(view, R.id.googleCard);
            show(view, R.id.chip_add);
        });
    }

    private void showGoogleDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        LoginDialog loginDialog = new LoginDialog();
        loginDialog.show(ft, "dialog");
    }


    private void drawDummyButtons() {
        if (Accountant.isDummy(getContext())) {
            show(view, R.id.btn_logout);
            show(view, R.id.btn_switch);
            show(view, R.id.btn_refresh);
        }
        logout_dummy.setOnClickListener(view -> showLogOutDialog());
        switch_dummy.setOnClickListener(view -> Accountant.switchDummy(getContext()));
        refresh_dummy.setOnClickListener(view -> Accountant.refreshMyToken(getContext()));
    }

    private void drawGoogleButtons() {
        if (Accountant.isGoogle(getContext())) {
            show(view, R.id.btn_logoutG);
            show(view, R.id.btn_switchG);
        }
        logout_google.setOnClickListener(view -> showLogOutDialog());
        switch_google.setOnClickListener(view -> showGoogleDialog());
    }

    private void loadAvatar(String Url) {
        Glide
                .with(getContext())
                .load(Url)
                .apply(new RequestOptions()
                        .placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_user_placeholder))
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.DATA))
                .into(avatar_google);
    }

    private void showLogOutDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("login");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        GenericDialog mDialog = new GenericDialog();
        mDialog.setDialogTitle(getString(R.string.dialog_title_logout));
        mDialog.setDialogMessage(getString(R.string.dialog_message_logout));
        mDialog.setPositiveButton(getString(android.R.string.yes), v -> {
            Accountant.completeCheckout(getContext());
            mDialog.dismiss();
            getActivity().finish();
            startActivity(new Intent(getContext(), LoginActivity.class));
        });
        mDialog.setNegativeButton(getString(android.R.string.cancel), v -> {
            mDialog.dismiss();
        });
        mDialog.show(ft, "login");
    }

    private void askLogin() {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("login");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            GenericDialog mDialog = new GenericDialog();
            mDialog.setDialogTitle(getString(R.string.action_login));
            mDialog.setDialogMessage(getString(R.string.header_usr_noEmail));
            mDialog.setPositiveButton(getString(R.string.action_login), v -> {
                mDialog.dismiss();
                getContext().startActivity(new Intent(getContext(), LoginActivity.class));
            });
            mDialog.setNegativeButton("Not now", v -> getActivity().finishAndRemoveTask());
            mDialog.show(ft, "login");
    }
}