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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.exception.AppNotFoundException;
import com.aurora.store.fragment.details.AbstractHelper;
import com.aurora.store.fragment.details.ActionButton;
import com.aurora.store.fragment.details.AppLinks;
import com.aurora.store.fragment.details.Beta;
import com.aurora.store.fragment.details.ExodusPrivacy;
import com.aurora.store.fragment.details.GeneralDetails;
import com.aurora.store.fragment.details.Reviews;
import com.aurora.store.fragment.details.Screenshot;
import com.aurora.store.fragment.details.Video;
import com.aurora.store.model.App;
import com.aurora.store.task.DetailsApp;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PackageUtil;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class DetailsFragment extends BaseFragment {

    private static final String ACTION_PACKAGE_REPLACED_NON_SYSTEM = "ACTION_PACKAGE_REPLACED_NON_SYSTEM";
    private static final String ACTION_PACKAGE_INSTALLATION_FAILED = "ACTION_PACKAGE_INSTALLATION_FAILED";
    private static final String ACTION_UNINSTALL_PACKAGE_FAILED = "ACTION_UNINSTALL_PACKAGE_FAILED";

    public static App app;

    @BindView(R.id.scroll_view)
    NestedScrollView mScrollView;

    private Context context;
    private ActionButton actionButton;
    private String packageName;

    private BroadcastReceiver localInstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getStringExtra("PACKAGE_NAME");
            int statusCode = intent.getIntExtra("STATUS_CODE", -1);
            if (packageName != null && packageName.equals(app.getPackageName()))
                ContextUtil.runOnUiThread(() -> drawButtons());
            if (statusCode == 0)
                ContextUtil.toastLong(context, getString(R.string.installer_status_failure));
        }
    };

    private BroadcastReceiver globalInstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getData() == null || !TextUtils.equals(packageName, intent.getData().getSchemeSpecificPart())) {
                return;
            }
            ContextUtil.runOnUiThread(() -> drawButtons());
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details, container, false);
        ButterKnife.bind(this, view);
        setErrorView(ErrorType.NO_APPS);
        Bundle arguments = getArguments();
        if (arguments != null) {
            packageName = arguments.getString("PackageName");
            fetchData();
        }
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context.registerReceiver(localInstallReceiver, new IntentFilter("ACTION_INSTALL"));
        context.registerReceiver(globalInstallReceiver, getFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            context.unregisterReceiver(localInstallReceiver);
            context.unregisterReceiver(globalInstallReceiver);
            actionButton = null;
            disposable.clear();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void fetchData() {
        disposable.add(Observable.fromCallable(() -> new DetailsApp(getContext())
                .getInfo(packageName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(app -> {
                    switchViews(false);
                    draw(app);
                }, err -> {
                    Log.e(err.getMessage());
                    processException(err);
                }));
    }

    private void draw(App mApp) {
        app = mApp;
        actionButton = new ActionButton(this, app);
        disposable.add(Observable.just(
                new GeneralDetails(this, app),
                new Screenshot(this, app),
                new Reviews(this, app),
                new ExodusPrivacy(this, app),
                new Video(this, app),
                new Beta(this, app),
                new AppLinks(this, app))
                .zipWith(Observable.interval(16, TimeUnit.MILLISECONDS), (abstractHelper, interval) -> abstractHelper)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(AbstractHelper::draw)
                .subscribe());
        drawButtons();
    }

    public void drawButtons() {
        if (PackageUtil.isInstalled(context, app))
            app.setInstalled(true);
        actionButton.draw();
    }

    @Override
    protected View.OnClickListener errRetry() {
        return v -> {
            fetchData();
            ((Button) v).setText(getString(R.string.action_retry_ing));
            ((Button) v).setEnabled(false);
        };
    }

    @Override
    protected View.OnClickListener errClose() {
        return v -> {
            if (getActivity() != null)
                getActivity().onBackPressed();
        };
    }

    @Override
    public void processException(Throwable e) {
        disposable.clear();
        if (e instanceof AppNotFoundException) {
            setErrorView(ErrorType.APP_NOT_FOUND);
            switchViews(true);
        } else
            super.processException(e);
    }

    public IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_INSTALL);
        filter.addAction(Intent.ACTION_UNINSTALL_PACKAGE);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(ACTION_PACKAGE_REPLACED_NON_SYSTEM);
        filter.addAction(ACTION_PACKAGE_INSTALLATION_FAILED);
        filter.addAction(ACTION_UNINSTALL_PACKAGE_FAILED);
        return filter;
    }
}