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

package com.aurora.store.sheet;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.events.Event;
import com.aurora.store.events.Events;
import com.aurora.store.events.RxBus;
import com.aurora.store.exception.UnknownException;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.google.android.material.textfield.TextInputEditText;

import java.net.UnknownHostException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.aurora.store.utility.ContextUtil.runOnUiThread;

public class CaptchaSheet extends DialogFragment {

    @BindView(R.id.img_captcha)
    ImageView imgCaptcha;
    @BindView(R.id.captcha_txt)
    TextInputEditText textInputEditText;

    private CompositeDisposable disposable = new CompositeDisposable();

    private Context context;

    public void setLoginInfo(LoginInfo loginInfo) {
        this.loginInfo = loginInfo;
    }

    private LoginInfo loginInfo;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_captcha, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        GlideApp
                .with(context)
                .load(loginInfo.getCaptchaUrl())
                .into(imgCaptcha);
    }

    @OnClick(R.id.btn_submit)
    public void login() {
        logInWithGoogle(loginInfo.getEmail(),
                loginInfo.getPassword(),
                loginInfo.getLoginToken(),
                textInputEditText.getText().toString());
    }

    private void logInWithGoogle(String email, String password, String loginToken, String loginCaptcha) {
        Log.e("Email : %s -> Password %s", email, password);
        Log.e("Token : %s -> Captcha %s", loginToken, loginCaptcha);
        disposable.add(Observable.fromCallable(() -> PlayStoreApiAuthenticator
                .login(context, email, password, loginToken, loginCaptcha))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    if (success) {
                        Log.i("Google Login Successful");
                        RxBus.publish(new Event(Events.LOGGED_IN));
                        runOnUiThread(() -> {
                            Accountant.setLoggedIn(context);
                            PrefUtil.putString(context, Accountant.ACCOUNT_EMAIL, email);
                            PrefUtil.putString(context, Accountant.ACCOUNT_PASSWORD, password);
                        });
                    } else {
                        Log.e("Google Login Failed Permanently");
                    }
                }, err -> {
                    if (err instanceof UnknownHostException) {
                        ContextUtil.toastLong(context, context.getString(R.string.error_no_network));
                    } else if (err instanceof UnknownException) {
                        ContextUtil.toastLong(context, getString(R.string.toast_unknown_reason));
                    } else {
                        ContextUtil.toastLong(context, "Invalid Captcha");
                        dismissAllowingStateLoss();
                    }
                    err.printStackTrace();
                }));
    }
}
