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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import com.aurora.store.ListType;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.activity.DownloadsActivity;
import com.aurora.store.adapter.UpdatableAppsAdapter;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.download.RequestBuilder;
import com.aurora.store.exception.MalformedRequestException;
import com.aurora.store.installer.Installer;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.model.App;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.receiver.InstallReceiver;
import com.aurora.store.task.BulkDeliveryData;
import com.aurora.store.task.UpdatableAppsTask;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.SplitUtil;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.CustomSwipeToRefresh;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.tonyodev.fetch2.AbstractFetchGroupListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchGroup;
import com.tonyodev.fetch2.FetchGroupListener;
import com.tonyodev.fetch2.Request;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.aurora.store.notification.NotificationBase.INTENT_APP_VERSION;
import static com.aurora.store.notification.NotificationBase.INTENT_PACKAGE_NAME;


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
    private BottomNavigationView bottomNavigationView;
    private View view;
    private List<App> updatableAppList = new ArrayList<>();
    private List<Request> requestList;
    private Map<String, App> pseudoPackageAppMap;
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
        setErrorView(ErrorType.UNKNOWN);
        customSwipeToRefresh.setOnRefreshListener(() -> fetchData());
        if (getActivity() instanceof AuroraActivity) {
            bottomNavigationView = ((AuroraActivity) getActivity()).getBottomNavigation();
            setBaseBottomNavigationView(bottomNavigationView);
        }
        setupRecycler();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter == null || adapter.isDataEmpty())
            fetchData();
        checkOnGoingUpdates();
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
        updatableAppTask = new UpdatableAppsTask(context);
        disposable.add(Observable.fromCallable(updatableAppTask::getUpdatableApps)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> customSwipeToRefresh.setRefreshing(true))
                .doOnTerminate(() -> customSwipeToRefresh.setRefreshing(false))
                .subscribe((appList) -> {
                    if (view != null) {
                        updatableAppList = appList;
                        pseudoPackageAppMap = getPackageAppMap(appList);
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

    private void checkOnGoingUpdates() {
        fetch.getFetchGroup(UPDATE_GROUP_ID, downloadList -> {
            if (!downloadList.getDownloads().isEmpty())
                onGoingUpdate = downloadList.getGroupDownloadProgress() < 100;
        });
    }

    private void setupRecycler() {
        adapter = new UpdatableAppsAdapter(context, ListType.UPDATES);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.anim_falldown));
        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (velocityY < 0) {
                    if (bottomNavigationView != null)
                        ViewUtil.showBottomNav(bottomNavigationView, true);
                } else if (velocityY > 0) {
                    if (bottomNavigationView != null)
                        ViewUtil.hideBottomNav(bottomNavigationView, true);
                }
                return false;
            }
        });
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
        checkOnGoingUpdates();
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
        disposable.add(Observable.fromCallable(() -> new BulkDeliveryData(context)
                .getDeliveryData(updatableAppList))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(deliveryDataList -> {
                    requestList = RequestBuilder.getBulkRequestList(context, deliveryDataList,
                            updatableAppList, UPDATE_GROUP_ID);
                    if (!requestList.isEmpty()) {
                        fetch.enqueue(requestList, updatedRequestList -> {
                            String updateTxt = new StringBuilder()
                                    .append(updatableAppList.size())
                                    .append(StringUtils.SPACE)
                                    .append(context.getString(R.string.list_update_all_queue_txt)).toString();
                            QuickNotification.show(
                                    context,
                                    context.getString(R.string.action_updates),
                                    updateTxt,
                                    getContentIntent());
                            btnUpdateAll.setOnClickListener(cancelAllListener());
                        });
                        fetch.addListener(getListener());
                    }
                }, err -> {
                    if (err instanceof MalformedRequestException) {
                        ContextUtil.runOnUiThread(() -> btnUpdateAll.setOnClickListener(updateAllListener()));
                        notifyStatusBlacklist(coordinatorLayout, bottomNavigationView, err.getMessage());
                    } else
                        Log.e(err.getMessage());
                }));
    }

    private Map<String, App> getPackageAppMap(List<App> appList) {
        Map<String, App> pseudoPackageAppMap = new HashMap<>();
        for (App app : appList)
            pseudoPackageAppMap.put(app.getPackageName(), app);
        return pseudoPackageAppMap;
    }

    private PendingIntent getContentIntent() {
        Intent intent = new Intent(context, DownloadsActivity.class);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getInstallIntent(App app) {
        Intent intent = new Intent(context, InstallReceiver.class);
        intent.putExtra(INTENT_PACKAGE_NAME, app.getPackageName());
        intent.putExtra(INTENT_APP_VERSION, app.getVersionCode());
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getDetailsIntent(App app) {
        Intent intent = new Intent(context, DetailsActivity.class);
        intent.putExtra(DetailsActivity.INTENT_PACKAGE_NAME, app.getPackageName());
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private FetchGroupListener getListener() {
        return new AbstractFetchGroupListener() {
            @Override
            public void onCompleted(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                super.onCompleted(groupId, download, fetchGroup);
                if (groupId == UPDATE_GROUP_ID) {
                    if (fetchGroup.getDownloadingDownloads().isEmpty()) {
                        QuickNotification.show(
                                context,
                                context.getString(R.string.action_updates),
                                "All updates downloaded",
                                getContentIntent());
                        ContextUtil.runOnUiThread(() -> btnUpdateAll.setOnClickListener(updateAllListener()));
                    }
                }
            }

            @Override
            public void onCompleted(@NotNull Download download) {
                super.onCompleted(download);
                final String packageName = download.getTag();
                if (pseudoPackageAppMap.containsKey(packageName)) {
                    final App app = pseudoPackageAppMap.get(packageName);
                    assert app != null;
                    final boolean isSplit = SplitUtil.isSplit(context, app.getPackageName());
                    if (Util.shouldAutoInstallApk(context) && !isSplit)
                        new Installer(context).install(app);
                    else
                        QuickNotification.show(
                                context,
                                app.getDisplayName(),
                                isSplit ? context.getString(R.string.notification_installation_auto)
                                        : context.getString(R.string.download_completed),
                                isSplit ? getDetailsIntent(app)
                                        : getInstallIntent(app)
                        );
                }
            }

            @Override
            public void onProgress(int groupId, @NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond, @NotNull FetchGroup fetchGroup) {
                super.onProgress(groupId, download, etaInMilliSeconds, downloadedBytesPerSecond, fetchGroup);
            }

            @Override
            public void onQueued(int groupId, @NotNull Download download, boolean waitingNetwork, @NotNull FetchGroup fetchGroup) {
                super.onQueued(groupId, download, waitingNetwork, fetchGroup);
                if (groupId == UPDATE_GROUP_ID) {
                    final String packageName = download.getTag();
                    if (pseudoPackageAppMap.containsKey(packageName)) {
                        final App app = pseudoPackageAppMap.get(packageName);
                        assert app != null;
                        QuickNotification.show(
                                context,
                                app.getDisplayName(),
                                context.getString(R.string.download_queued),
                                getContentIntent());
                    }
                }
            }
        };
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
