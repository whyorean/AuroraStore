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

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.task.AppProvidedCredentialsTask;
import com.dragons.aurora.task.UserProvidedCredentialsTask;

import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends AuroraActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        if (isConnected()) {
            init();
        }

        if (isLoggedIn()) {
            finish();
        }
    }

    private void init() {
        Button login_anonymous = findViewById(R.id.btn_ok_anm);
        CheckBox checkBox = findViewById(R.id.checkboxSave);
        EditText editPassword = findViewById(R.id.passwordg);
        Button login_google = findViewById(R.id.button_okg);
        AutoCompleteTextView editEmail = findViewById(R.id.emailg);

        login_anonymous.setOnClickListener(v -> {
            new AppProvidedCredentialsTask(this).logInWithPredefinedAccount();
            watchLoggedIn();
        });

        login_google.setOnClickListener(view -> {
            Context c = view.getContext();
            String email = editEmail.getText().toString();
            String password = editPassword.getText().toString();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                ContextUtil.toast(c.getApplicationContext(), R.string.error_credentials_empty);
                return;
            }
            if (checkBox.isChecked()) {
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("SEC_ACCOUNT", true).apply();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString("GOOGLE_EMAIL", email).apply();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString("GOOGLE_PASSWORD", password).apply();
            }

            new UserProvidedCredentialsTask(this).getUserCredentialsTask().execute(email, password);
            watchLoggedIn();
        });
        ImageView toggle_password = findViewById(R.id.toggle_password_visibility);
        toggle_password.setOnClickListener(v -> {
            boolean passwordVisible = !TextUtils.isEmpty((String) v.getTag());
            v.setTag(passwordVisible ? null : "tag");
            ((ImageView) v).setImageResource(passwordVisible ? R.drawable.ic_visibility_on : R.drawable.ic_visibility_off);
            editPassword.setInputType(passwordVisible ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT);
        });
    }

    private void watchLoggedIn() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isLoggedIn())
                    finish();
            }
        }, 0, 500);
    }
}
