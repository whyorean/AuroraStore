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
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.aurora.store.AuroraApplication;
import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.activity.AccountsActivity;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.api.SearchIterator2;
import com.aurora.store.events.Event;
import com.aurora.store.events.Events;
import com.aurora.store.events.RxBus;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.MalformedRequestException;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.view.ErrorView;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.IteratorGooglePlayException;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import butterknife.BindView;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.aurora.store.utility.Util.noNetwork;

public abstract class BaseFragment extends Fragment {

    protected CustomAppListIterator iterator;
    protected CompositeDisposable disposable = new CompositeDisposable();

    @BindView(R.id.coordinator)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.view_switcher)
    ViewSwitcher viewSwitcher;
    @BindView(R.id.content_view)
    ViewGroup layoutContent;
    @BindView(R.id.err_view)
    ViewGroup layoutError;

    private SearchIterator2 searchIterator;
    private CompositeDisposable disposableBus = new CompositeDisposable();
    private Context context;
    private BottomNavigationView bottomNavigationView;
    private PlayStoreApiAuthenticator playStoreApiAuthenticator;

    void setBaseBottomNavigationView(BottomNavigationView bottomNavigationView) {
        this.bottomNavigationView = bottomNavigationView;
    }

    CustomAppListIterator getIterator(String query) {
        try {
            searchIterator = new SearchIterator2(new PlayStoreApiAuthenticator(context).getApi(), query);
            iterator = new CustomAppListIterator(searchIterator);
            return iterator;
        } catch (Exception e) {
            processException(e);
            return null;
        }
    }

    protected abstract View.OnClickListener errRetry();

    protected abstract void fetchData();

    /*UI handling methods*/

    protected void notifyLoggedIn() {
        ContextUtil.runOnUiThread(() -> {
            fetchData();
            notifyStatus(coordinatorLayout, null, context.getResources().getString(R.string.action_logging_in_success));
        });
    }

    protected void notifyNetworkFailure() {
        setErrorView(ErrorType.NO_NETWORK);
        notifyStatus(coordinatorLayout, bottomNavigationView, context.getString(R.string.error_no_network));
        switchViews(true);
    }

    protected void notifyPermanentFailure() {
        setErrorView(ErrorType.UNKNOWN);
        switchViews(true);
    }

    protected void notifyLoggedOut() {
        setErrorView(ErrorType.LOGOUT_ERR);
        switchViews(true);
        notifyStatus(coordinatorLayout, bottomNavigationView, context.getString(R.string.error_logged_out));
    }

    protected void notifyTokenExpired() {
        notifyStatus(coordinatorLayout, bottomNavigationView, context.getString(R.string.action_token_expired));
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
                                notifyLoggedIn();
                                break;
                            case LOGGED_OUT:
                                notifyLoggedOut();
                                break;
                            case TOKEN_REFRESHED:
                                notifyLoggedIn();
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playStoreApiAuthenticator = new PlayStoreApiAuthenticator(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        searchIterator = null;
        iterator = null;
        disposable.clear();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposableBus.clear();
    }

    protected void notifyStatus(@NonNull CoordinatorLayout coordinatorLayout, @Nullable View anchorView,
                                @NonNull String message) {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        if (anchorView != null)
            snackbar.setAnchorView(anchorView);
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
        disposable.clear();
        if (e instanceof AuthException) {
            processAuthException((AuthException) e);
        } else if (e instanceof IteratorGooglePlayException) {
            processException(e.getCause());
        } else if (e instanceof MalformedRequestException) {
            processAuthException(new AuthException("Malformed Request", 401));
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
        } else Log.i("No Network Connection");
    }

    private void processAuthException(AuthException e) {
        if (e instanceof CredentialsEmptyException) {
            Log.i("Logged out");
            RxBus.publish(new Event(Events.LOGGED_OUT));
        } else if (e.getCode() == 401 && Accountant.isDummy(context)) {
            Log.i("Token is stale");
            refreshToken();
        } else {
            ContextUtil.toast(context, R.string.error_incorrect_password);
            playStoreApiAuthenticator.logout();
            Accountant.completeCheckout(context);
        }
    }

    /*Anonymous accounts handling methods*/

    private synchronized void logInWithDummy() {
        if (!AuroraApplication.isAnonymousLogging())
            disposable.add(Observable.fromCallable(() -> playStoreApiAuthenticator
                    .login())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .doOnSubscribe(disposable1 -> {
                        Log.i("Credentials Empty : Requesting Anonymous Login");
                        AuroraApplication.setAnonymousLogging(true);
                    })
                    .doOnTerminate(() -> AuroraApplication.setAnonymousLogging(false))
                    .subscribe((success) -> {
                        if (success) {
                            Log.i("Anonymous Login Successful");
                            Accountant.saveDummy(context);
                            RxBus.publish(new Event(Events.LOGGED_IN));
                        } else
                            Log.e("Anonymous Login Failed Permanently");
                    }, err -> {
                        ContextUtil.runOnUiThread(() -> notifyStatus(coordinatorLayout, bottomNavigationView, err.getMessage()));
                        Log.e(err.getMessage());
                        AuroraApplication.setAnonymousLogging(false);
                    }));
    }

    private synchronized void refreshToken() {
        if (!AuroraApplication.isTokenRefreshing())
            disposable.add(Flowable.fromCallable(() -> playStoreApiAuthenticator
                    .refreshToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .doOnSubscribe(subscription -> {
                        RxBus.publish(new Event(Events.TOKEN_EXPIRED));
                        AuroraApplication.setTokenRefreshing(true);
                    })
                    .doOnTerminate(() -> AuroraApplication.setTokenRefreshing(false))
                    .subscribe((success) -> {
                        if (success) {
                            Log.i("Token Refreshed");
                            RxBus.publish(new Event(Events.TOKEN_REFRESHED));
                        } else {
                            Log.e("Token Refresh Failed Permanently");
                            RxBus.publish(new Event(Events.NET_DISCONNECTED));
                        }
                    }, err -> {
                        Log.e("Token Refresh Login failed %s", err.getMessage());
                        RxBus.publish(new Event(Events.PERMANENT_FAIL));
                    }));
    }

}
