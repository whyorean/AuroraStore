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
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.adapter.UpdatableAppsAdapter;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.exception.MalformedRequestException;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.model.App;
import com.aurora.store.task.LiveUpdate;
import com.aurora.store.task.ObservableDeliveryData;
import com.aurora.store.task.UpdatableAppsTask;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.CustomSwipeToRefresh;
import com.google.android.material.snackbar.Snackbar;
import com.tonyodev.fetch2.Fetch;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class UpdatesFragment extends BaseFragment {

    private static final int UPDATE_GROUP_ID = 1337;
    @BindView(R.id.swipe_layout)
    CustomSwipeToRefresh customSwipeToRefresh;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.btn_update_all)
    Button btnUpdateAll;
    @BindView(R.id.txt_update_all)
    TextView txtUpdateAll;

    private Context context;
    private View view;
    private List<App> updatableAppList = new ArrayList<>();
    private UpdatableAppsAdapter adapter;
    private Fetch fetch;
    private boolean onGoingUpdate = false;
    private UpdatableAppsTask updatableAppTask;

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_updates, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetch = DownloadManager.getFetchInstance(context);
        adapter = new UpdatableAppsAdapter(context);
        updatableAppTask = new UpdatableAppsTask(context);
        setErrorView(ErrorType.UNKNOWN);
        customSwipeToRefresh.setOnRefreshListener(() -> fetchData());
        setupRecycler();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null && adapter.isDataEmpty())
            fetchData();
    }

    @Override
    public void onPause() {
        super.onPause();
        customSwipeToRefresh.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        updatableAppTask = null;
        adapter = null;
    }

    @Override
    protected void fetchData() {
        disposable.add(Observable.fromCallable(updatableAppTask::getUpdatableApps)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> customSwipeToRefresh.setRefreshing(true))
                .doOnTerminate(() -> customSwipeToRefresh.setRefreshing(false))
                .subscribe((appList) -> {
                    if (view != null) {
                        updatableAppList = appList;
                        if (appList.isEmpty()) {
                            setErrorView(ErrorType.NO_UPDATES);
                            switchViews(true);
                        } else {
                            switchViews(false);
                            if (adapter != null)
                                adapter.addData(appList);
                            updateCounter();
                            setupUpdateAll();
                        }
                    }
                }, err -> {
                    Log.d(err.getMessage());
                    processException(err);
                }));
    }

    private void setupRecycler() {
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.anim_falldown));
    }

    @Override
    protected View.OnClickListener errRetry() {
        return v -> {
            fetchData();
            if (updatableAppList != null) {
                ((Button) v).setText(updatableAppList.isEmpty()
                        ? getString(R.string.action_recheck_ing)
                        : getString(R.string.action_retry_ing));
                ((Button) v).setEnabled(false);
            }
        };
    }

    private void setupUpdateAll() {
        if (updatableAppList.isEmpty()) {
            ViewUtil.hideWithAnimation(btnUpdateAll);
            txtUpdateAll.setText(context.getString(R.string.list_empty_updates));
        } else if (onGoingUpdate) {
            btnUpdateAll.setOnClickListener(cancelAllListener());
        } else {
            btnUpdateAll.setOnClickListener(updateAllListener());
        }
    }

    private void updateCounter() {
        txtUpdateAll.setText(new StringBuilder()
                .append(updatableAppList.size())
                .append(StringUtils.SPACE)
                .append(context.getString(R.string.list_update_all_txt)));
    }

    private View.OnClickListener updateAllListener() {
        btnUpdateAll.setText(getString(R.string.list_update_all));
        btnUpdateAll.setEnabled(true);
        return v -> {
            updateAllApps();
            onGoingUpdate = true;
            btnUpdateAll.setText(getString(R.string.list_updating));
            btnUpdateAll.setEnabled(false);
        };
    }

    private View.OnClickListener cancelAllListener() {
        btnUpdateAll.setText(getString(R.string.action_cancel));
        btnUpdateAll.setEnabled(true);
        return v -> {
            fetch.deleteGroup(UPDATE_GROUP_ID);
            setupUpdateAll();
        };
    }

    private void updateAllApps() {
        disposable.add(Observable.fromIterable(updatableAppList)
                .flatMap(app -> new ObservableDeliveryData(context).getDeliveryData(app))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(deliveryDataBundle -> new LiveUpdate(context)
                        .enqueueUpdate(deliveryDataBundle.getApp(),
                                deliveryDataBundle.getAndroidAppDeliveryData()))
                .doOnError(throwable -> {
                    if (throwable instanceof MalformedRequestException) {
                        ContextUtil.runOnUiThread(() -> btnUpdateAll.setOnClickListener(updateAllListener()));
                        notifyStatusBlacklist(coordinatorLayout, null, throwable.getMessage());
                    } else
                        Log.e(throwable.getMessage());
                })
                .subscribe());
    }

    public void notifyStatusBlacklist(CoordinatorLayout coordinatorLayout, View anchorView, String packageName) {
        final StringBuilder message = new StringBuilder()
                .append(packageName)
                .append(context.getString(R.string.error_app_download));
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(anchorView);
        snackbar.setAction(R.string.action_blacklist, v -> new BlacklistManager(context).add(packageName));
        snackbar.setActionTextColor(context.getResources().getColor(R.color.colorGold));
        snackbar.show();
    }
}
