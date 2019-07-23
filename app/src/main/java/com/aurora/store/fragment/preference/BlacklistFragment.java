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

package com.aurora.store.fragment.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.adapter.BlacklistAdapter;
import com.aurora.store.fragment.BaseFragment;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.model.App;
import com.aurora.store.task.InstalledAppsTask;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.CustomSwipeToRefresh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class BlacklistFragment extends BaseFragment implements BlacklistAdapter.ItemClickListener {

    @BindView(R.id.swipe_layout)
    CustomSwipeToRefresh customSwipeToRefresh;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.btn_clear_all)
    Button btnClearAll;
    @BindView(R.id.txt_blacklist)
    TextView txtBlacklist;

    private Context context;
    private BlacklistAdapter blacklistAdapter;
    private BlacklistManager blacklistManager;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_blacklist, container, false);
        ButterKnife.bind(this, view);
        customSwipeToRefresh.setOnRefreshListener(() -> fetchData());
        blacklistManager = new BlacklistManager(context);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setErrorView(ErrorType.UNKNOWN);
        fetchData();
        setupClearAll();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void updateBlackListedApps() {
        blacklistAdapter.addSelectionsToBlackList();
    }

    private void clearBlackListedApps() {
        if (blacklistAdapter != null) {
            blacklistAdapter.removeSelectionsFromBlackList();
            blacklistAdapter.notifyDataSetChanged();
            txtBlacklist.setText(getString(R.string.list_blacklist_none));
        }
    }

    @Override
    protected void fetchData() {
        InstalledAppsTask mTaskHelper = new InstalledAppsTask(context);
        disposable.add(Observable.fromCallable(mTaskHelper::getAllApps)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> customSwipeToRefresh.setRefreshing(true))
                .doOnTerminate(() -> customSwipeToRefresh.setRefreshing(false))
                .subscribe((appList) -> {
                    if (appList.isEmpty()) {
                        setErrorView(ErrorType.NO_APPS);
                        switchViews(true);
                    } else {
                        switchViews(false);
                        setupRecycler(appList);
                    }
                }, err -> {
                    Log.e(err.getMessage());
                    processException(err);
                }));
    }

    private List<App> sortBlackListedApps(List<App> appList) {
        final List<App> blackListedApps = new ArrayList<>();
        final List<App> whiteListedApps = new ArrayList<>();
        final List<App> sortListedApps = new ArrayList<>();

        //Sort Apps by Names
        Collections.sort(appList, (App1, App2) -> App1.getDisplayName().compareTo(App2.getDisplayName()));

        //Sort Apps by blacklist status
        for (App app : appList) {
            if (blacklistManager.contains(app.getPackageName()))
                blackListedApps.add(app);
            else
                whiteListedApps.add(app);
        }
        sortListedApps.addAll(blackListedApps);
        sortListedApps.addAll(whiteListedApps);
        return sortListedApps;
    }

    private void setupRecycler(List<App> appList) {
        appList = sortBlackListedApps(appList);
        blacklistAdapter = new BlacklistAdapter(context, appList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(blacklistAdapter);
        updateCount();
    }

    @Override
    protected View.OnClickListener errRetry() {
        return v -> {
            fetchData();
            ((Button) v).setText(getString(R.string.action_retry_ing));
            ((Button) v).setEnabled(false);
        };
    }

    private void setupClearAll() {
        btnClearAll.setOnClickListener(v -> {
            clearBlackListedApps();
        });
    }

    private void updateCount() {
        int count = blacklistAdapter.getSelectedCount();
        String txtCount = new StringBuilder()
                .append(getResources().getString(R.string.list_blacklist))
                .append(" : ")
                .append(count).toString();
        txtBlacklist.setText(count > 0 ? txtCount : getString(R.string.list_blacklist_none));
        ViewUtil.setVisibility(btnClearAll, count > 0, true);
    }

    @Override
    public void onItemClicked(int position) {
        blacklistAdapter.toggleSelection(position);
        updateBlackListedApps();
        updateCount();
    }

    @Override
    public void notifyLoggedIn() {
        fetchData();
    }

    @Override
    public void notifyLoggedOut() {

    }

    @Override
    public void notifyPermanentFailure() {
        setErrorView(ErrorType.UNKNOWN);
        switchViews(true);
    }

    @Override
    public void notifyNetworkFailure() {
        setErrorView(ErrorType.NO_NETWORK);
        switchViews(true);
    }

    @Override
    public void notifyTokenExpired() {

    }
}
