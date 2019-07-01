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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.exception.MalformedRequestException;
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
import com.aurora.store.receiver.DetailsInstallReceiver;
import com.aurora.store.task.DetailsApp;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PackageUtil;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class DetailsFragment extends BaseFragment {

    public static App app;

    @BindView(R.id.scroll_view)
    NestedScrollView mScrollView;

    private Context context;
    private ActionButton actionButton;
    private String packageName;
    private DetailsInstallReceiver mInstallReceiver;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details, container, false);
        ButterKnife.bind(this, view);

        Bundle arguments = getArguments();
        if (arguments != null) {
            packageName = arguments.getString("PackageName");
            fetchData();
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mInstallReceiver = new DetailsInstallReceiver(packageName);
        setErrorView(ErrorType.NO_APPS);
    }

    @Override
    public void onResume() {
        super.onResume();
        context.registerReceiver(mInstallReceiver, mInstallReceiver.getFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            context.unregisterReceiver(mInstallReceiver);
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
        if (e instanceof MalformedRequestException) {
            setErrorView(ErrorType.MALFORMED);
            switchViews(true);
        } else
            super.processException(e);
    }
}