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

package com.dragons.aurora.task.playstore;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.task.AppProvidedCredentialsTask;
import com.dragons.aurora.task.TaskWithProgress;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import timber.log.Timber;

abstract public class PlayStoreTask<T> extends TaskWithProgress<T> {

    protected Throwable exception;

    static public boolean noNetwork(Throwable e) {
        return e instanceof UnknownHostException
                || e instanceof SSLHandshakeException
                || e instanceof ConnectException
                || e instanceof SocketException
                || e instanceof SocketTimeoutException
                || (null != e && null != e.getCause() && noNetwork(e.getCause()))
                ;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean success() {
        return null == exception;
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
    }

    @Override
    protected void onPostExecute(T result) {
        super.onPostExecute(result);
        if (exception != null) {
            processException(exception);
        }
    }

    protected void processException(Throwable e) {
        Timber.d("Caught during a google api request: %s", e.getMessage());
        if (e instanceof AuthException) {
            processAuthException((AuthException) e);
        } else if (e instanceof IOException) {
            processIOException((IOException) e);
        } else {
            Timber.e("Unknown exception : %s", e.getMessage());
            e.printStackTrace();
        }
    }

    protected void processIOException(IOException e) {
        String message;
        if (context != null) {
            if (noNetwork(e)) {
                message = context.getString(R.string.error_no_network);
            } else {
                message = TextUtils.isEmpty(e.getMessage())
                        ? context.getString(R.string.error_network_other, Aurora.TAG)
                        : e.getMessage();
            }
            ContextUtil.toastLong(this.context, message);
        } else Timber.i("No Network Connection");
    }

    protected void processAuthException(AuthException e) {
        if (e instanceof CredentialsEmptyException) {
            Timber.i("Credentials empty");
            new AppProvidedCredentialsTask(context).logInWithPredefinedAccount();
            return;
        } else if (e.getCode() == 401 && PreferenceFragment.getBoolean(context, Aurora.PREFERENCE_APP_PROVIDED_EMAIL)) {
            Timber.i("Token is stale");
            new AppProvidedCredentialsTask(context).refreshToken();
            return;
        } else {
            ContextUtil.toast(context, R.string.error_incorrect_password);
            new PlayStoreApiAuthenticator(context).logout();
            Accountant.completeCheckout(context);
        }
        if (ContextUtil.isAlive(context))
            context.startActivity(new Intent(context, LoginActivity.class));
        else
            Timber.e("AuthException happened and the provided context is not ui capable");
    }
}
