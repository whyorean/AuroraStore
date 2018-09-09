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

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.FirstLaunchChecker;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayException;
import com.dragons.aurora.playstoreapiv2.TokenDispenserException;
import com.dragons.aurora.task.playstore.CloneableTask;
import com.dragons.aurora.task.playstore.PlayStoreTask;

import java.io.IOException;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

public abstract class CheckCredentialsTask extends PlayStoreTask<Void> {

    static private final String APP_PASSWORDS_URL = "https://security.google.com/settings/security/apppasswords";
    private PlayStoreTask caller;

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (success()) {
            new FirstLaunchChecker(context).setLoggedIn();
            if (caller instanceof CloneableTask) {
                Timber.i("%s is cloneable. Retrying.", caller.getClass().getSimpleName());
                ((PlayStoreTask) ((CloneableTask) caller).clone()).execute((Object[]) new String[]{});
            }
        }
    }

    @Override
    protected void processException(Throwable e) {
        super.processException(e);
        if ((e instanceof GooglePlayException
                && ((GooglePlayException) e).getCode() == 500) || (e instanceof AuthException
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
            Prefs.putString(context, Aurora.PREFERENCE_BACKGROUND_UPDATE_INTERVAL, "-1");
            ContextUtil.toast(context, R.string.error_invalid_device_definition);
            context.startActivity(new Intent(context, PreferenceFragment.class));
        }
    }

    @Override
    protected void processAuthException(AuthException e) {
        if (e instanceof CredentialsEmptyException) {
            Timber.w("Credentials empty");
        } else if (null != e.getTwoFactorUrl() && context instanceof AppCompatActivity) {
            getTwoFactorAuthDialog().show();
        } else {
            ContextUtil.toast(context, R.string.error_incorrect_password);
            new PlayStoreApiAuthenticator(context).logout();
            Accountant.completeCheckout(context);
        }
    }

    private AlertDialog getTwoFactorAuthDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder
                .setMessage(R.string.dialog_message_two_factor)
                .setTitle(R.string.dialog_title_two_factor)
                .setPositiveButton(
                        R.string.dialog_two_factor_create_password,
                        (dialog, which) -> {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(APP_PASSWORDS_URL));
                            if (i.resolveActivity(context.getPackageManager()) != null) {
                                context.startActivity(i);
                            } else {
                                Timber.e("No application available to handle http links... very strange");
                            }
                            dialog.dismiss();
                        }
                )
                .setNegativeButton(R.string.dialog_two_factor_cancel, (dialog, which) -> dialog.dismiss())
                .create();
    }
}
