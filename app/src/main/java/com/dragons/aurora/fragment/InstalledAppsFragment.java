/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.dragons.aurora.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.adapters.InstalledAppsAdapter;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.InstalledAppsTaskHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.isConnected;
import static com.dragons.aurora.Util.show;

public class InstalledAppsFragment extends InstalledAppsTaskHelper {

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.installed_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.ohhSnap_retry)
    Button retry_update;
    @BindView(R.id.includeSystem)
    SwitchCompat includeSystem;

    private View view;
    private Disposable loadApps;
    private List<App> installedApps = new ArrayList<>(new HashSet<>());
    private InstalledAppsAdapter installedAppsAdapter;

    public static InstalledAppsFragment newInstance() {
        return new InstalledAppsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }

        view = inflater.inflate(R.layout.app_installed_inc, container, false);
        ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isLoggedIn() && isConnected(getContext()))
                loadMarketApps();
            else
                swipeRefreshLayout.setRefreshing(false);
        });

        retry_update.setOnClickListener(click -> {
            if (isLoggedIn() && isConnected(getContext())) {
                hide(view, R.id.ohhSnap);
                if (installedAppsAdapter == null || installedAppsAdapter.getItemCount() <= 0)
                    loadMarketApps();
            }
        });

        includeSystem.setChecked(PreferenceFragment.getBoolean(getContext(), "INCLUDE_SYSTEM"));
        includeSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                Util.putBoolean(getContext(), "INCLUDE_SYSTEM", true);
            else
                Util.putBoolean(getContext(), "INCLUDE_SYSTEM", false);
            loadMarketApps();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLoggedIn() && installedApps.isEmpty())
            loadMarketApps();
    }

    @Override
    public void onStop() {
        super.onStop();
        swipeRefreshLayout.setRefreshing(false);
    }

    protected void setupRecycler(List<App> appsToAdd) {
        installedAppsAdapter = new InstalledAppsAdapter((AuroraActivity) getActivity(), appsToAdd);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_anim));
        recyclerView.setAdapter(installedAppsAdapter);
    }

    public void loadMarketApps() {
        swipeRefreshLayout.setRefreshing(true);
        loadApps = Observable.fromCallable(() -> getInstalledApps(new PlayStoreApiAuthenticator(this.getActivity()).getApi(), includeSystem.isChecked()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    if (view != null) {
                        installedApps.clear();
                        installedApps.addAll(appList);
                        setupList(installedApps);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, err -> {
                    swipeRefreshLayout.setRefreshing(false);
                    processException(err);
                    show(view, R.id.ohhSnap);
                });
    }

    private void setupList(List<App> installedApps) {
        if (recyclerView.getAdapter() == null)
            setupRecycler(installedApps);
        else {
            installedAppsAdapter.appsToAdd = installedApps;
            Util.reloadRecycler(recyclerView);
        }
    }
}