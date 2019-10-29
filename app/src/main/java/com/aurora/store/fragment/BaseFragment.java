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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.aurora.store.AnonymousLoginService;
import com.aurora.store.AnonymousRefreshService;
import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.activity.AccountsActivity;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.events.Event;
import com.aurora.store.events.Events;
import com.aurora.store.events.RxBus;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.InvalidApiException;
import com.aurora.store.exception.MalformedRequestException;
import com.aurora.store.exception.TooManyRequestsException;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.view.ErrorView;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.IteratorGooglePlayException;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.aurora.store.utility.Util.noNetwork;

public abstract class BaseFragment extends Fragment {

    protected CompositeDisposable disposable = new CompositeDisposable();

    @BindView(R.id.coordinator)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.view_switcher)
    ViewSwitcher viewSwitcher;
    @BindView(R.id.content_view)
    ViewGroup layoutContent;
    @BindView(R.id.err_view)
    ViewGroup layoutError;

    private CompositeDisposable disposableBus = new CompositeDisposable();
    private Context context;

    protected abstract View.OnClickListener errRetry();

    protected abstract void fetchData();

    /*UI handling methods*/

    protected void notifyLoggedIn() {
        ContextUtil.runOnUiThread(() -> {
            fetchData();
            notifyStatus(coordinatorLayout, context.getResources().getString(R.string.action_logging_in_success));
        });
    }

    protected void notifyNetworkFailure() {
        setErrorView(ErrorType.NO_NETWORK);
        notifyStatus(coordinatorLayout, context.getString(R.string.error_no_network));
        switchViews(true);
    }

    protected void notifyPermanentFailure() {
        setErrorView(ErrorType.UNKNOWN);
        switchViews(true);
    }

    protected void notifyLoggedOut() {
        setErrorView(ErrorType.LOGOUT_ERR);
        switchViews(true);
        notifyStatus(coordinatorLayout, context.getString(R.string.error_logged_out));
    }

    protected void notifyTokenExpired() {
        setErrorView(ErrorType.SESSION_EXPIRED);
        switchViews(true);
        notifyStatus(coordinatorLayout, context.getString(R.string.action_token_expired));
    }

    @Override
    public void onStart() {
        super.onStart();
        disposableBus.add(RxBus.get().toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event instanceof Event) {
                        Events eventEnum = ((Event) event).getEvent();
                        switch (eventEnum) {
                            case LOGGED_IN:
                            case TOKEN_REFRESHED:
                                notifyLoggedIn();
                                break;
                            case LOGGED_OUT:
                                notifyLoggedOut();
                                break;
                            case TOKEN_EXPIRED:
                                notifyTokenExpired();
                                break;
                            case NET_DISCONNECTED:
                                notifyNetworkFailure();
                                break;
                            case PERMANENT_FAIL:
                                notifyPermanentFailure();
                                break;
                        }
                    }
                }));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onStop() {
        super.onStop();
        disposableBus.clear();
    }

    private void notifyStatus(@NonNull CoordinatorLayout coordinatorLayout, String message) {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    /*ErrorView UI handling methods*/

    protected void setErrorView(ErrorType errorType) {
        layoutError.removeAllViews();
        layoutError.addView(new ErrorView(context, errorType, getAction(errorType)));
    }

    protected void switchViews(boolean showError) {
        if (viewSwitcher.getCurrentView() == layoutContent && showError)
            viewSwitcher.showNext();
        else if (viewSwitcher.getCurrentView() == layoutError && !showError)
            viewSwitcher.showPrevious();
    }

    private View.OnClickListener errLogin() {
        return v -> {
            ((Button) v).setText(getString(R.string.action_logging_in));
            ((Button) v).setEnabled(false);
            if (Accountant.isLoggedIn(context)) {
                RxBus.publish(new Event(Events.LOGGED_IN));
                return;
            }
            if (Accountant.isGoogle(context))
                context.startActivity(new Intent(context, AccountsActivity.class));
            else
                logInWithDummy();
        };
    }

    protected View.OnClickListener errClose() {
        return v -> {

        };
    }

    private View.OnClickListener getAction(ErrorType errorType) {
        switch (errorType) {
            case LOGOUT_ERR:
                return errLogin();
            case APP_NOT_FOUND:
                return errClose();
            default:
                return errRetry();
        }
    }

    /*Exception handling methods*/

    protected void processException(Throwable e) {
        if (e instanceof AuthException) {
            processAuthException((AuthException) e);
        } else if (e instanceof IteratorGooglePlayException) {
            processException(e.getCause());
        } else if (e instanceof TooManyRequestsException) {
            processAuthException(new AuthException("Too many request", 429));
        } else if (e instanceof MalformedRequestException) {
            processAuthException(new AuthException("Malformed Request", 401));
        } else if (e instanceof IOException) {
            processIOException((IOException) e);
        } else {
            Log.e("Unknown exception " + e.getClass().getName() + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processIOException(IOException e) {
        String message;
        if (noNetwork(e)) {
            message = context.getString(R.string.error_no_network);
            Log.i(message);
            RxBus.publish(new Event(Events.NET_DISCONNECTED));
        } else {
            message = TextUtils.isEmpty(e.getMessage())
                    ? context.getString(R.string.error_network_other)
                    : e.getMessage();
            Log.i(message);
        }
    }

    private void processAuthException(AuthException e) {
        PlayStoreApiAuthenticator.destroyInstance();
        if (e instanceof CredentialsEmptyException || e instanceof InvalidApiException) {
            Accountant.completeCheckout(context);
            RxBus.publish(new Event(Events.LOGGED_OUT));
        } else if (e.getCode() == 401 && Accountant.isDummy(context)) {
            RxBus.publish(new Event(Events.TOKEN_EXPIRED));
            refreshToken();
        } else if (e.getCode() == 429 && Accountant.isDummy(context)) {
            ContextUtil.toastLong(context, context.getString(R.string.toast_rate_limit));
            Accountant.completeCheckout(context);
            logInWithDummy();
        } else {
            ContextUtil.toast(context, R.string.error_incorrect_password);
            Accountant.completeCheckout(context);
        }
    }

    /*
     * Anonymous accounts handling methods
     *
     */

    private void logInWithDummy() {
        Intent intent = new Intent(context, AnonymousLoginService.class);
        if (!AnonymousLoginService.isServiceRunning())
            context.startService(intent);
    }

    private void refreshToken() {
        Intent intent = new Intent(context, AnonymousRefreshService.class);
        if (!AnonymousRefreshService.isServiceRunning())
            context.startService(intent);
    }
}
