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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.task.UserProvidedCredentialsTask;
import com.github.florent37.shapeofview.shapes.CircleView;
import com.percolate.caffeine.ViewUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.isConnected;
import static com.dragons.aurora.Util.setText;
import static com.dragons.aurora.Util.show;

public class AccountsFragment extends BaseFragment {

    private boolean isSecAvailable;
    private String myEmail;
    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }

        view = inflater.inflate(R.layout.fragment_accounts, container, false);

        ImageView toolbar_back = view.findViewById(R.id.toolbar_back);
        toolbar_back.setOnClickListener(click -> getActivity().onBackPressed());

        myEmail = PreferenceFragment.getString(getActivity(), PlayStoreApiAuthenticator.PREFERENCE_EMAIL);
        isSecAvailable = PreferenceFragment.getBoolean(getActivity(), "SEC_ACCOUNT");

        if (Accountant.isLoggedIn(getContext()) && Accountant.isGoogle(getContext()))
            drawGoogle();
        else if (Accountant.isLoggedIn(getContext()) && Accountant.isDummy(getContext()))
            drawDummy();
        return view;
    }

    private void drawDummy() {
        show(view, R.id.dummyIndicator);
        setAvatar(R.drawable.ic_dummy_avatar);
        setText(view, R.id.account_name, R.string.acc_dummy_name);
        setText(view, R.id.account_email, myEmail);
        setText(view, R.id.account_gsf, R.string.device_gsfID, PreferenceFragment.getString(getActivity(),
                PlayStoreApiAuthenticator.PREFERENCE_GSF_ID));
        if (isSecAvailable)
            drawEmptyGoogle();
        else
            drawEmpty();

        drawDummyButtons();
    }

    private void drawGoogle() {
        drawEmptyDummy();

        hide(view, R.id.emptyCard);
        show(view, R.id.googleCard);
        show(view, R.id.googleIndicator);

        setText(view, R.id.account_nameG, PreferenceFragment.getString(getActivity(), "GOOGLE_NAME"));
        setText(view, R.id.account_emailG, myEmail);
        setText(view, R.id.account_gsf, R.string.device_gsfID, PreferenceFragment.getString(getActivity(),
                PlayStoreApiAuthenticator.PREFERENCE_GSF_ID));

        TextView switchGoogle = ViewUtils.findViewById(view, R.id.btn_switchG);
        switchGoogle.setOnClickListener(view -> Accountant.switchGoogle(getContext()));

        if (isConnected(getActivity()))
            loadAvatar(PreferenceFragment.getString(getActivity(), "GOOGLE_URL"));

        drawGoogleButtons();
    }

    private void drawEmptyDummy() {
        show(view, R.id.dummy_tapToSwitch);
        setText(view, R.id.account_name, R.string.acc_dummy_name);
        setText(view, R.id.account_email, R.string.account_dummy_email);
        LinearLayout dummyCard = ViewUtils.findViewById(view, R.id.dummyLayout);
        dummyCard.setOnClickListener(v -> Accountant.loginWithDummy(getContext()));
    }

    private void drawEmptyGoogle() {
        LinearLayout googleCard = ViewUtils.findViewById(view, R.id.googleLayout);
        TextView removeAccount = ViewUtils.findViewById(view, R.id.btn_remove);
        show(view, R.id.googleCard);
        show(view, R.id.btn_remove);
        show(view, R.id.google_tapToSwitch);
        setText(view, R.id.account_nameG, PreferenceFragment.getString(getActivity(), "GOOGLE_NAME"));
        setText(view, R.id.account_emailG, PreferenceFragment.getString(getActivity(), "GOOGLE_EMAIL"));
        googleCard.setOnClickListener(click -> new UserProvidedCredentialsTask(getContext()).withSavedGoogle());
        removeAccount.setOnClickListener(click -> {
            new UserProvidedCredentialsTask(getContext()).removeGooglePrefs();
            hide(view, R.id.googleCard);
            show(view, R.id.emptyCard);
        });
    }

    private void drawEmpty() {
        show(view, R.id.emptyCard);
        CircleView add_account = view.findViewById(R.id.add_account);
        add_account.setOnClickListener(v -> Accountant.switchGoogle(getContext()));
    }

    private void drawDummyButtons() {
        TextView logout = ViewUtils.findViewById(view, R.id.btn_logout);
        TextView switchDummy = ViewUtils.findViewById(view, R.id.btn_switch);
        TextView refreshToken = ViewUtils.findViewById(view, R.id.btn_refresh);

        if (Accountant.isDummy(getContext())) {
            show(view, R.id.btn_logout);
            show(view, R.id.btn_switch);
            show(view, R.id.btn_refresh);
        }

        logout.setOnClickListener(view -> showLogOutDialog());
        switchDummy.setOnClickListener(view -> Accountant.switchDummy(getContext()));
        refreshToken.setOnClickListener(view -> Accountant.refreshMyToken(getContext()));
    }

    private void drawGoogleButtons() {
        TextView logout = ViewUtils.findViewById(view, R.id.btn_logoutG);
        TextView switchDummy = ViewUtils.findViewById(view, R.id.btn_switchG);

        if (Accountant.isGoogle(getContext())) {
            show(view, R.id.btn_logoutG);
            show(view, R.id.btn_switchG);
        }

        logout.setOnClickListener(view -> showLogOutDialog());
        switchDummy.setOnClickListener(view -> Accountant.switchGoogle(getContext()));
    }

    private void setAvatar(int avatar) {
        ImageView avatar_view = view.findViewById(R.id.accounts_Avatar);
        avatar_view.setImageResource(avatar);
    }

    private void loadAvatar(String Url) {
        Glide
                .with(getContext())
                .load(Url)
                .apply(new RequestOptions()
                        .placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_dummy_avatar))
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.DATA))
                .into(((ImageView) view.findViewById(R.id.accounts_AvatarG)));
    }

    private void showLogOutDialog() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.dialog_message_logout)
                .setTitle(R.string.dialog_title_logout)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    Accountant.completeCheckout(getContext());
                    dialogInterface.dismiss();
                    getActivity().finish();
                    startActivity(new Intent(getContext(), LoginActivity.class));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}