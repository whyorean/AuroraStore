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

package com.dragons.aurora.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.task.AppProvidedCredentialsTask;
import com.dragons.aurora.task.UserProvidedCredentialsTask;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends BaseActivity {

    @BindView(R.id.btn_ok_anm)
    Button login_anonymous;
    @BindView(R.id.checkboxSave)
    CheckBox checkBox;
    @BindView(R.id.button_okg)
    Button login_google;
    @BindView(R.id.email_google)
    TextInputEditText editEmail;
    @BindView(R.id.password_google)
    TextInputEditText editPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        if (Util.isConnected(this)) {
            init();
        }

        if (Accountant.isLoggedIn(this)) {
            finish();
        }
    }

    private void init() {
        login_anonymous.setOnClickListener(v -> {
            new AppProvidedCredentialsTask(this).logInWithPredefinedAccount();
            watchLoggedIn();
        });

        login_google.setOnClickListener(v -> {
            String email = editEmail.getText().toString();
            String password = editPassword.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                ContextUtil.toast(v.getContext(), R.string.error_credentials_empty);
                return;
            }

            if (checkBox.isChecked()) {
                Prefs.putBoolean(this, Aurora.SEC_ACCOUNT, true);
                Prefs.putString(this, Aurora.GOOGLE_EMAIL, email);
                Prefs.putString(this, Aurora.GOOGLE_PASSWORD, password);
            }

            new UserProvidedCredentialsTask(this).getUserCredentialsTask().execute(email, password);
            watchLoggedIn();
        });

        /*ImageView toggle_password = findViewById(R.id.toggle_password_visibility);
        toggle_password.setOnClickListener(v -> {
            boolean passwordVisible = !TextUtils.isEmpty((String) v.getTag());
            v.setTag(passwordVisible ? null : "tag");
            ((ImageView) v).setImageResource(passwordVisible ? R.drawable.ic_visibility_on : R.drawable.ic_visibility_off);
            editPassword.setInputType(passwordVisible ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT);
        });*/
    }

    private void watchLoggedIn() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (Accountant.isLoggedIn(getApplicationContext()))
                    finish();
            }
        }, 0, 500);
    }
}
