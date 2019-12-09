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

package com.aurora.store.fragment.intro;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.activity.GoogleLoginActivity;
import com.aurora.store.activity.IntroActivity;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.NetworkUtil;
import com.google.android.material.button.MaterialButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class LoginFragment extends IntroBaseFragment {

    @BindView(R.id.btn_next)
    Button btnNext;
    @BindView(R.id.btn_anonymous)
    MaterialButton btnAnonymous;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_login, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.btn_next)
    public void openLoginActivity() {
        context.startActivity(new Intent(context, GoogleLoginActivity.class));
        if (getActivity() instanceof IntroActivity)
            ((IntroActivity) getActivity()).finish();

    }

    @OnClick(R.id.btn_anonymous)
    public void loginAnonymous() {
        if (!NetworkUtil.isConnected(context)) {
            Toast.makeText(context, getString(R.string.error_no_network), Toast.LENGTH_SHORT).show();
            return;
        }

        CompositeDisposable disposable = new CompositeDisposable();
        disposable.add(Observable.fromCallable(() -> PlayStoreApiAuthenticator
                .login(context))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> {
                    btnAnonymous.setText(getText(R.string.action_logging_in));
                    btnAnonymous.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                })
                .subscribe(api -> {
                    if (api != null) {
                        Toast.makeText(context, getText(R.string.toast_login_success), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(context, AuroraActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation((IntroActivity) context);
                        startActivity(intent, activityOptions.toBundle());
                        ((IntroActivity) getActivity()).finish();
                    } else {
                        Toast.makeText(context, getText(R.string.toast_login_failed), Toast.LENGTH_LONG).show();
                        ContextUtil.runOnUiThread(this::resetAnonymousLogin);
                    }
                }, err -> {
                    Toast.makeText(context, getText(R.string.toast_login_failed), Toast.LENGTH_LONG).show();
                    ContextUtil.runOnUiThread(this::resetAnonymousLogin);
                }));
    }

    private void resetAnonymousLogin() {
        btnAnonymous.setEnabled(true);
        btnAnonymous.setText(getText(R.string.account_dummy));
        progressBar.setVisibility(View.INVISIBLE);
    }
}
