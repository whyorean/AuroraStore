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
import android.text.TextUtils;
import android.util.Log;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.AppProvidedCredentialsTask;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

public class ExceptionTask {

    protected Context context;
    protected GooglePlayAPI api;
    protected Throwable exception;

    public ExceptionTask(Context context) {
        this.context = context;
    }

    public static boolean noNetwork(Throwable e) {
        return e instanceof UnknownHostException
                || e instanceof SSLHandshakeException
                || e instanceof ConnectException
                || e instanceof SocketException
                || e instanceof SocketTimeoutException
                || (null != e && null != e.getCause() && noNetwork(e.getCause()))
                ;
    }

    public GooglePlayAPI getApi() throws IOException {
        return new PlayStoreApiAuthenticator(context).getApi();
    }

    public void setApi(GooglePlayAPI api) {
        this.api = api;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    protected boolean success() {
        return null == exception;
    }

    public void processException(Throwable e) {
        Log.d(getClass().getSimpleName(), e.getClass().getName() + " caught during a google api request: " + e.getMessage());
        if (e instanceof AuthException) {
            processAuthException((AuthException) e);
        } else if (e instanceof IOException) {
            processIOException((IOException) e);
        } else if (e instanceof NullPointerException)
            Log.e("Null Exception", "Probably App Switched");
        else {
            Log.e(getClass().getSimpleName(), "Unknown exception " + e.getClass().getName() + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void processIOException(IOException e) {
        String message;
        if (noNetwork(e) && context != null) {
            message = context.getString(R.string.error_no_network);
        } else {
            message = TextUtils.isEmpty(e.getMessage()) ? context.getString(R.string.error_network_other, e.getClass().getName()) : e.getMessage();
        }
        ContextUtil.toastShort(getContext(), message);
    }

    protected void processAuthException(AuthException e) {
        if (e instanceof CredentialsEmptyException) {
            Log.i(getClass().getSimpleName(), "Credentials empty");
            //TODO:Let user decide between dummy or google account
            Accountant.loginWithDummy(getContext());
        } else if (e.getCode() == 401 && PreferenceFragment.getBoolean(context, PlayStoreApiAuthenticator.PREFERENCE_APP_PROVIDED_EMAIL)) {
            Log.i(getClass().getSimpleName(), "Token is stale");
            new AppProvidedCredentialsTask(getContext()).refreshToken();
        } else {
            ContextUtil.toast(context, R.string.error_incorrect_password);
            new PlayStoreApiAuthenticator(context).logout();
        }
    }
}