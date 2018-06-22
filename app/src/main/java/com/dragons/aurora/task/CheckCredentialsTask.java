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

package com.dragons.aurora.task;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.FirstLaunchChecker;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayException;
import com.dragons.aurora.playstoreapiv2.TokenDispenserException;
import com.dragons.aurora.task.playstore.CloneableTask;
import com.dragons.aurora.task.playstore.PlayStoreTask;

import java.io.IOException;

public abstract class CheckCredentialsTask extends PlayStoreTask<Void> {

    static private final String APP_PASSWORDS_URL = "https://security.google.com/settings/security/apppasswords";
    private PlayStoreTask caller;

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (success()) {
            new FirstLaunchChecker(context).setLoggedIn();
            if (caller instanceof CloneableTask) {
                Log.i(getClass().getSimpleName(), caller.getClass().getSimpleName() + " is cloneable. Retrying.");
                ((PlayStoreTask) ((CloneableTask) caller).clone()).execute((Object[]) new String[]{});
            }
        }
    }

    @Override
    protected void processException(Throwable e) {
        super.processException(e);
        if ((e instanceof GooglePlayException && ((GooglePlayException) e).getCode() == 500)
                || (e instanceof AuthException
                && !TextUtils.isEmpty(((AuthException) e).getTwoFactorUrl()))) {
            return;
        }
    }

    @Override
    protected void processIOException(IOException e) {
        super.processIOException(e);
        if (e instanceof TokenDispenserException) {
            ContextUtil.toast(context, R.string.error_token_dispenser_problem);
        } else if (e instanceof GooglePlayException && ((GooglePlayException) e).getCode() == 500) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PreferenceFragment.PREFERENCE_BACKGROUND_UPDATE_INTERVAL, "-1").apply();
            ContextUtil.toast(context, R.string.error_invalid_device_definition);
            context.startActivity(new Intent(context, PreferenceFragment.class));
        }
    }

    @Override
    protected void processAuthException(AuthException e) {
        if (e instanceof CredentialsEmptyException) {
            Log.w(getClass().getSimpleName(), "Credentials empty");
        } else if (null != e.getTwoFactorUrl()) {
            getTwoFactorAuthDialog().show();
        } else {
            ContextUtil.toast(context, R.string.error_incorrect_password);
            Util.completeCheckout(context);
        }
    }

    private AlertDialog getTwoFactorAuthDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder
                .setMessage(R.string.dialog_message_two_factor)
                .setTitle(R.string.dialog_title_two_factor)
                .setPositiveButton(
                        R.string.dialog_two_factor_create_password,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(APP_PASSWORDS_URL));
                                if (i.resolveActivity(context.getPackageManager()) != null) {
                                    context.startActivity(i);
                                } else {
                                    Log.e(getClass().getSimpleName(), "No application available to handle http links... very strange");
                                }
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        }
                )
                .setNegativeButton(
                        R.string.dialog_two_factor_cancel,
                        (dialog, which) -> android.os.Process.killProcess(android.os.Process.myPid())
                )
                .create();
    }
}
