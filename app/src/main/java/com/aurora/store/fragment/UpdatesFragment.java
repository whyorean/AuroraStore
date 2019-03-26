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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.aurora.store.installer.Installer;
import com.aurora.store.model.App;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.receiver.InstallReceiver;
import com.aurora.store.task.BulkDeliveryData;
import com.aurora.store.task.UpdatableApps;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.SplitUtil;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.CustomSwipeToRefresh;
import com.aurora.store.view.ErrorView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
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


public class UpdatesFragment extends BaseFragment implements BaseFragment.EventListenerImpl {

    private static final int UPDATE_GROUP_ID = 1337;

    @BindView(R.id.view_switcher)
    ViewSwitcher mViewSwitcher;
    @BindView(R.id.content_view)
    LinearLayout layoutContent;
    @BindView(R.id.err_view)
    LinearLayout layoutError;
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
    private UpdatableApps updatableAppTask;

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
        fetch = new DownloadManager(context).getFetchInstance();
        setErrorView(ErrorType.UNKNOWN);
        customSwipeToRefresh.setOnRefreshListener(() -> fetchData());
        if (getActivity() instanceof AuroraActivity)
            bottomNavigationView = ((AuroraActivity) getActivity()).getBottomNavigation();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter == null && updatableAppList.isEmpty())
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
        fetch.close();
        disposable.clear();
        pseudoPackageAppMap = null;
        updatableAppList = null;
        updatableAppTask = null;
        adapter = null;
        requestList = null;
    }

    private void fetchData() {
        updatableAppTask = new UpdatableApps(context);
        disposable.add(Observable.fromCallable(updatableAppTask::getUpdatableApps)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> customSwipeToRefresh.setRefreshing(true))
                .subscribe((appList) -> {
                    if (view != null) {
                        updatableAppList = appList;
                        pseudoPackageAppMap = getPackageAppMap(appList);
                        if (appList.isEmpty()) {
                            setErrorView(ErrorType.NO_APPS);
                            switchViews(true);
                        } else {
                            switchViews(false);
                            setupRecycler(appList);
                            updateCounter();
                            setupUpdateAll();
                        }
                    }
                }, err -> processException(err)));
    }

    private void checkOnGoingUpdates() {
        fetch.getDownloadsInGroup(UPDATE_GROUP_ID, mDownloadList ->
                onGoingUpdate = !mDownloadList.isEmpty());
    }

    private void setupRecycler(List<App> mApps) {
        customSwipeToRefresh.setRefreshing(false);
        adapter = new UpdatableAppsAdapter(context, mApps, ListType.UPDATES);
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

    private void setErrorView(ErrorType errorType) {
        layoutError.removeAllViews();
        layoutError.addView(new ErrorView(context, errorType, retry()));
    }

    private View.OnClickListener retry() {
        return v -> {
            fetchData();
            ((Button) v).setText(getString(R.string.action_retry_ing));
            ((Button) v).setEnabled(false);
        };
    }

    private void switchViews(boolean showError) {
        if (mViewSwitcher.getCurrentView() == layoutContent && showError)
            mViewSwitcher.showNext();
        else if (mViewSwitcher.getCurrentView() == layoutError && !showError)
            mViewSwitcher.showPrevious();
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
            fetch.pauseGroup(UPDATE_GROUP_ID);
            fetch.cancelGroup(UPDATE_GROUP_ID);
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
                }, err -> Log.e(err.getMessage())));
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

    private FetchListener getListener() {
        return new AbstractFetchListener() {
            @Override
            public void onCompleted(@NotNull Download download) {
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
                fetch.getDownloadsInGroup(UPDATE_GROUP_ID, groupDownloads ->
                        groupDownloads.remove(download));
            }

            @Override
            public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
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
        };
    }

    @Override
    public void onLoggedIn() {
        fetchData();
    }

    @Override
    public void onLoginFailed() {
        setErrorView(ErrorType.UNKNOWN);
        switchViews(true);
    }

    @Override
    public void onNetworkFailed() {
        setErrorView(ErrorType.NO_NETWORK);
        switchViews(true);
    }
}
