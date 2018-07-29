/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
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

package com.dragons.aurora.helpers;

import android.content.Context;
import android.content.Intent;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.task.AppProvidedCredentialsTask;
import com.dragons.aurora.task.UserProvidedCredentialsTask;

import androidx.appcompat.app.AlertDialog;

import static com.dragons.aurora.helpers.Prefs.DUMMY_ACC;
import static com.dragons.aurora.helpers.Prefs.GOOGLE_ACC;
import static com.dragons.aurora.helpers.Prefs.GOOGLE_NAME;
import static com.dragons.aurora.helpers.Prefs.GOOGLE_URL;
import static com.dragons.aurora.helpers.Prefs.LOGGED_IN;
import static com.dragons.aurora.helpers.Prefs.REFRESH_ASKED;

public class Accountant {

    public static Boolean isGoogle(Context context) {
        return Prefs.getBoolean(context, GOOGLE_ACC);
    }

    public static Boolean isDummy(Context context) {
        return Prefs.getBoolean(context, DUMMY_ACC);
    }

    public static Boolean isLoggedIn(Context context) {
        return Prefs.getBoolean(context, LOGGED_IN);
    }

    public static void LoginFirst(Context context) {
        if (!Prefs.getBoolean(context, "LOGIN_PROMPTED")) {
            new AlertDialog.Builder(context, R.style.ThemeOverlay_MaterialComponents_Dialog)
                    .setTitle(R.string.action_login)
                    .setMessage(R.string.header_usr_noEmail)
                    .setPositiveButton(R.string.action_login, (dialogInterface, i) -> {
                        context.startActivity(new Intent(context, LoginActivity.class));
                        Prefs.putBoolean(context, "LOGIN_PROMPTED", false);
                    })
                    .setCancelable(false)
                    .show();
            Prefs.putBoolean(context, "LOGIN_PROMPTED", true);
        }
    }

    public static void switchDummy(Context context) {
        if (isLoggedIn(context))
            completeCheckout(context);

        AppProvidedCredentialsTask.LoginTask task = new AppProvidedCredentialsTask.LoginTask(context);
        task.setContext(context);
        task.prepareDialog(R.string.dialog_message_switching_in_predefined, R.string.dialog_title_logging_in);
        task.execute();
    }

    public static void switchGoogle(Context context) {
        new UserProvidedCredentialsTask(context).logInWithGoogleAccount();
    }

    public static void loginWithDummy(Context context) {
        if (isLoggedIn(context))
            completeCheckout(context);
        new AppProvidedCredentialsTask(context).logInWithPredefinedAccount();
    }

    public static void refreshMyToken(Context context) {
        new AppProvidedCredentialsTask(context).refreshToken();
    }

    public static void completeCheckout(Context context) {
        Prefs.putBoolean(context, LOGGED_IN, false);
        Prefs.putBoolean(context, REFRESH_ASKED, false);
        Prefs.putString(context, GOOGLE_NAME, "");
        Prefs.putString(context, GOOGLE_URL, "");
        new PlayStoreApiAuthenticator(context).logout();
    }
}
