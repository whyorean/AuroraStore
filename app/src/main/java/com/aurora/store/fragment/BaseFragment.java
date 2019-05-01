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

package com.aurora.store.fragment;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.aurora.store.R;
import com.aurora.store.activity.AccountsActivity;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.ViewUtil;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.SearchIterator;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.UnknownHostException;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.aurora.store.utility.Util.noNetwork;

public abstract class BaseFragment extends Fragment {


    protected CustomAppListIterator iterator;
    protected CompositeDisposable disposable = new CompositeDisposable();

    private Context context;
    private EventListenerImpl eventListenerImpl;
    private boolean isRefreshing = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        eventListenerImpl = (EventListenerImpl) this;
        this.context = context;
    }

    protected CustomAppListIterator getIterator(String query) {
        CustomAppListIterator iterator;
        try {
            iterator = new CustomAppListIterator(new SearchIterator(new PlayStoreApiAuthenticator(getContext()).getApi(), query));
            return iterator;
        } catch (Exception e) {
            processException(e);
            return null;
        }
    }

    public void notifyStatus(CoordinatorLayout coordinatorLayout, View anchorView, String message) {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(anchorView);
        snackbar.show();
    }

    public void notifyExpiredToken(CoordinatorLayout coordinatorLayout, View anchorView, String message) {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        if (anchorView != null)
            snackbar.setAnchorView(anchorView);
        snackbar.setActionTextColor(ViewUtil.getStyledAttribute(context, R.attr.colorBackgroundFloating));
        snackbar.setAction(context.getString(R.string.action_token_force_request), v -> refreshToken());
        snackbar.show();
    }

    public void processException(Throwable e) {
        disposable.clear();
        if (e instanceof AuthException) {
            processAuthException((AuthException) e);
        } else if (e instanceof IOException) {
            processIOException((IOException) e);
        } else if (e instanceof NullPointerException)
            Log.e("NPE ? Oh yeah !");
        else {
            Log.e("Unknown exception " + e.getClass().getName() + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processIOException(IOException e) {
        String message;
        if (context != null) {
            if (noNetwork(e) || e instanceof UnknownHostException) {
                message = context.getString(R.string.error_no_network);
                ContextUtil.toastShort(context, message);
                eventListenerImpl.notifyNetworkFailure();
            } else {
                message = TextUtils.isEmpty(e.getMessage())
                        ? context.getString(R.string.error_network_other)
                        : e.getMessage();
                Log.i(message);
            }
        } else Log.i("No Network Connection");
    }

    private void processAuthException(AuthException e) {
        if (e instanceof CredentialsEmptyException) {
            Log.i("Credentials Empty : Requesting New Token");
            if (context != null && Accountant.isGoogle(context))
                context.startActivity(new Intent(context, AccountsActivity.class));
            else
                logInWithDummy();
        } else if (e.getCode() == 401 && Accountant.isDummy(context)) {
            Log.i("Token is stale");
            eventListenerImpl.notifyTokenExpired();
            refreshToken();
        } else {
            ContextUtil.toast(context, R.string.error_incorrect_password);
            new PlayStoreApiAuthenticator(context).logout();
            Accountant.completeCheckout(context);
        }
    }

    private void logInWithDummy() {
        disposable.add(Observable.fromCallable(() ->
                new PlayStoreApiAuthenticator(context).login())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe((success) -> {
                    if (success) {
                        Log.i("Dummy Login Successful");
                        Accountant.saveDummy(context);
                        eventListenerImpl.notifyLoggedIn();
                    } else
                        Log.e("Dummy Login Failed Permanently");
                }, err -> Log.e("Dummy Login failed %s", err.getMessage())));
    }

    private synchronized void refreshToken() {
        if (!isRefreshing)
            disposable.add(Flowable.fromCallable(() ->
                    new PlayStoreApiAuthenticator(context).refreshToken())
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe(subscription -> isRefreshing = true)
                    .doOnTerminate(() -> isRefreshing = false)
                    .observeOn(Schedulers.computation())
                    .subscribe((success) -> {
                        if (success) {
                            Log.i("Token Refreshed");
                            eventListenerImpl.notifyLoggedIn();
                        } else {
                            Log.e("Token Refresh Failed Permanently");
                            eventListenerImpl.notifyPermanentFailure();
                        }
                    }, err -> {
                        Log.e("Token Refresh Login failed %s", err.getMessage());
                        eventListenerImpl.notifyPermanentFailure();
                    }));
    }

    public interface EventListenerImpl {
        void notifyLoggedIn();

        void notifyPermanentFailure();

        void notifyNetworkFailure();

        void notifyTokenExpired();
    }
}
