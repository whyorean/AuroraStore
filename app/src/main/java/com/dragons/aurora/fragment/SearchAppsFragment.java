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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.dragons.aurora.AppListIterator;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.adapters.EndlessAppsAdapter;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.SearchIterator;
import com.dragons.aurora.task.playstore.SearchTask;
import com.dragons.aurora.view.AdaptiveToolbar;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SearchAppsFragment extends SearchTask {

    @BindView(R.id.search_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.adaptive_toolbar)
    AdaptiveToolbar adaptiveToolbar;
    @BindView(R.id.unicorn)
    RelativeLayout unicorn;
    @BindView(R.id.ohhSnap)
    RelativeLayout ohhSnap;
    @BindView(R.id.progress)
    RelativeLayout progress;

    private String title;
    private boolean setLooper = true;
    private boolean loading = true;
    private int oldItems, visibleItems, totalItems;
    private View view;
    private AppListIterator iterator;
    private EndlessAppsAdapter endlessAppsAdapter;
    private Disposable disposable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_search_list, container, false);
        ButterKnife.bind(this, view);
        adaptiveToolbar.getAction_icon().setOnClickListener((v -> this.getActivity().onBackPressed()));
        adaptiveToolbar.getTitle0().setText(title);
        adaptiveToolbar.getTitle1().setVisibility(View.GONE);
        Button ohhSnap_retry = view.findViewById(R.id.ohhSnap_retry);
        ohhSnap_retry.setOnClickListener(click -> {
            if (isLoggedIn() && isConnected(getContext())) {
                hide(view, R.id.ohhSnap);
                fetchSearchAppsList(false);
            }
        });
        Button retry_querry = view.findViewById(R.id.recheck_query);
        retry_querry.setOnClickListener(click -> {
            if (isLoggedIn() && isConnected(getContext())) {
                hide(view, R.id.unicorn);
                fetchSearchAppsList(false);
            }
        });
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            String query = arguments.getString("SearchQuery");
            title = arguments.getString("SearchTitle");
            iterator = setupIterator(query);
            fetchSearchAppsList(false);
        } else
            Log.e(this.getClass().getName(), "No category id provided");
    }

    protected AppListIterator setupIterator(String query) {
        AppListIterator iterator;
        try {
            iterator = new AppListIterator(new SearchIterator(new PlayStoreApiAuthenticator(getContext()).getApi(), query));
            iterator.setFilter(new FilterMenu((AuroraActivity) getContext()).getFilterPreferences());
            return iterator;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void setupListView(List<App> appsToAdd) {
        progress.setVisibility(View.GONE);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this.getActivity());
        endlessAppsAdapter = new EndlessAppsAdapter(getActivity(), appsToAdd);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this.getActivity(), R.anim.layout_anim));
        recyclerView.setAdapter(endlessAppsAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if ((visibleItems + oldItems) >= totalItems + 2 || endlessAppsAdapter.getItemCount() > 20)
                    setLooper = false;
                if (dy > 0) {
                    visibleItems = mLayoutManager.getChildCount();
                    totalItems = mLayoutManager.getItemCount();
                    oldItems = mLayoutManager.findFirstVisibleItemPosition();

                    if (loading && !setLooper) {
                        if ((visibleItems + oldItems) >= totalItems - 2) {
                            loading = false;
                            fetchSearchAppsList(true);
                        }
                    }
                }
            }
        });
        getLooper();
    }

    public void fetchSearchAppsList(boolean loadMore) {
        disposable = Observable.fromCallable(() -> getResult(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appList -> {
                    if (view != null) {
                        if (loadMore) {
                            loading = true;
                            addApps(appList);
                        } else
                            setupListView(appList);
                    }
                }, err -> {
                    processException(err);
                    ohhSnap.setVisibility(View.VISIBLE);
                });
    }

    public void addApps(List<App> appsToAdd) {
        if (!appsToAdd.isEmpty()) {
            for (App app : appsToAdd)
                endlessAppsAdapter.add(app);
            endlessAppsAdapter.notifyItemInserted(endlessAppsAdapter.getItemCount() - 1);
        }
        getLooper();
    }

    public void getLooper() {
        if (iterator.hasNext() && setLooper)
            fetchSearchAppsList(true);
        else if (!iterator.hasNext() && endlessAppsAdapter.getItemCount() <= 0)
            unicorn.setVisibility(View.VISIBLE);
    }
}