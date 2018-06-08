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
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.task.AppProvidedCredentialsTask;
import com.dragons.aurora.task.UserProvidedCredentialsTask;

public abstract class AccountsHelper extends Fragment {

    protected boolean isLoggedIn() {
        return PreferenceFragment.getBoolean(getContext(), "LOGGED_IN");
    }

    protected boolean isDummy() {
        return PreferenceFragment.getBoolean(getContext(), "DUMMY_ACC");
    }

    protected boolean isGoogle() {
        return PreferenceFragment.getBoolean(getContext(), "GOOGLE_ACC");
    }

    protected void checkOut() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("LOGGED_IN", false).apply();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("GOOGLE_NAME", "").apply();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("GOOGLE_URL", "").apply();
    }

    protected void LoginFirst() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.action_login)
                .setMessage(R.string.header_usr_noEmail)
                .setPositiveButton(R.string.action_login, (dialogInterface, i) -> startActivity(new Intent(getActivity(), LoginActivity.class)))
                .setCancelable(false)
                .show();
    }

    public void switchDummy() {
        if (isLoggedIn())
            Util.completeCheckout(getContext());

        AppProvidedCredentialsTask.LoginTask task = new AppProvidedCredentialsTask.LoginTask(getContext());
        task.setContext(getContext());
        task.prepareDialog(R.string.dialog_message_switching_in_predefined, R.string.dialog_title_logging_in);
        task.execute();
    }

    public void switchGoogle() {
        new UserProvidedCredentialsTask(getContext()).logInWithGoogleAccount();
    }

    public void loginWithDummy() {
        if (isLoggedIn())
            Util.completeCheckout(getContext());
        new AppProvidedCredentialsTask(getContext()).logInWithPredefinedAccount();
    }

    protected void refreshMyToken() {
        new AppProvidedCredentialsTask(getContext()).refreshToken();
    }

}
