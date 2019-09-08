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

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
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
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.CustomSwipeToRefresh;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tonyodev.fetch2.Fetch;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class UpdatesFragment extends BaseFragment {

    @BindView(R.id.swipe_layout)
    CustomSwipeToRefresh customSwipeToRefresh;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.btn_action)
    Button btnAction;
    @BindView(R.id.txt_update_all)
    TextView txtUpdateAll;

    private Context context;
    private List<App> updatableAppList = new ArrayList<>();
    private UpdatableAppsAdapter adapter;
    private Fetch fetch;
    private UpdatableAppsTask updatableAppTask;

    private BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getStringExtra("PACKAGE_NAME");
            try {
                removeInstalledApp(packageName);
            } catch (Exception ignored) {
            }
        }
    };

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_updates, container, false);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context.registerReceiver(installReceiver, new IntentFilter("ACTION_INSTALL"));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter.isDataEmpty()) {
            fetchAppsFromCache();
            drawButtons();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        customSwipeToRefresh.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        try {
            context.unregisterReceiver(installReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    protected void fetchData() {
        disposable.add(Observable.fromCallable(updatableAppTask::getUpdatableApps)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> customSwipeToRefresh.setRefreshing(true))
                .doOnTerminate(() -> customSwipeToRefresh.setRefreshing(false))
                .subscribe((appList) -> {
                    updatableAppList = appList;
                    if (appList.isEmpty()) {
                        PrefUtil.putString(context, Constants.PREFERENCE_UPDATABLE_APPS, "");
                        setErrorView(ErrorType.NO_UPDATES);
                        switchViews(true);
                    } else {
                        switchViews(false);
                        if (adapter != null)
                            adapter.addData(appList);
                        saveToCache(appList);
                        drawButtons();
                        updateCounter();
                    }
                }, err -> {
                    Log.d(err.getMessage());
                    processException(err);
                }));
    }

    private void saveToCache(List<App> appList) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(appList);
        PrefUtil.putString(context, Constants.PREFERENCE_UPDATABLE_APPS, jsonString);
    }

    private void fetchAppsFromCache() {
        Gson gson = new Gson();
        Type type = new TypeToken<List<App>>() {
        }.getType();
        String jsonString = PrefUtil.getString(context, Constants.PREFERENCE_UPDATABLE_APPS);
        updatableAppList = gson.fromJson(jsonString, type);
        if (updatableAppList == null || updatableAppList.isEmpty())
            fetchData();
        else {
            adapter.addData(updatableAppList);
            switchViews(false);
            drawButtons();
            updateCounter();
        }
    }

    private void removeInstalledApp(String packageName) {
        adapter.remove(packageName);
        AuroraApplication.removeFromOngoingUpdateList(packageName);
        saveToCache(adapter.getAppList());
        drawButtons();
        updateCounter();
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

    private void drawButtons() {
        btnAction.setEnabled(true);
        if (updatableAppList != null && updatableAppList.isEmpty()) {
            ViewUtil.hideWithAnimation(btnAction);
            txtUpdateAll.setText(context.getString(R.string.list_empty_updates));
            setErrorView(ErrorType.NO_UPDATES);
            switchViews(true);
        } else if (AuroraApplication.getOnGoingUpdate()) {
            btnAction.setOnClickListener(cancelAllListener());
        } else {
            btnAction.setOnClickListener(updateAllListener());
        }
    }

    private void updateCounter() {
        txtUpdateAll.setText(new StringBuilder()
                .append(adapter.getItemCount())
                .append(StringUtils.SPACE)
                .append(context.getString(R.string.list_update_all_txt)));
    }

    private View.OnClickListener updateAllListener() {
        ViewUtil.showWithAnimation(btnAction);
        btnAction.setText(getString(R.string.list_update_all));
        return v -> {
            updateAllApps();
            btnAction.setText(getString(R.string.list_updating));
            btnAction.setEnabled(false);
        };
    }

    private View.OnClickListener cancelAllListener() {
        ViewUtil.showWithAnimation(btnAction);
        btnAction.setText(getString(R.string.action_cancel));
        return v -> {
            cancelAllRequests();
            AuroraApplication.setOngoingUpdateList(new ArrayList<>());
            AuroraApplication.setOnGoingUpdate(false);
            ContextUtil.runOnUiThread(() -> drawButtons());
        };
    }

    private void updateAllApps() {
        AuroraApplication.setOngoingUpdateList(updatableAppList);
        AuroraApplication.setOnGoingUpdate(true);
        disposable.add(Observable.fromIterable(updatableAppList)
                .flatMap(app -> new ObservableDeliveryData(context).getDeliveryData(app))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> drawButtons())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(deliveryDataBundle -> new LiveUpdate(context)
                        .enqueueUpdate(deliveryDataBundle.getApp(),
                                deliveryDataBundle.getAndroidAppDeliveryData()))
                .doOnError(throwable -> {
                    if (throwable instanceof MalformedRequestException) {
                        ContextUtil.runOnUiThread(() -> btnAction.setOnClickListener(updateAllListener()));
                        notifyStatusBlacklist(coordinatorLayout, throwable.getMessage());
                    } else
                        Log.e(throwable.getMessage());
                })
                .subscribe());
    }

    private void cancelAllRequests() {
        List<App> ongoingUpdateList = AuroraApplication.getOngoingUpdateList();
        for (App app : ongoingUpdateList) {
            Log.i("Cancelled -> %s", app.getDisplayName());
            fetch.cancelGroup(app.getPackageName().hashCode());
        }
    }

    private void notifyStatusBlacklist(CoordinatorLayout coordinatorLayout, String packageName) {
        final StringBuilder message = new StringBuilder()
                .append(packageName)
                .append(context.getString(R.string.error_app_download));
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.action_blacklist, v -> new BlacklistManager(context).add(packageName));
        snackbar.setActionTextColor(context.getResources().getColor(R.color.colorGold));
        snackbar.show();
    }
}
