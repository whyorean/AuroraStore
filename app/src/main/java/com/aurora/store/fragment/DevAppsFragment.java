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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.EndlessScrollListener;
import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.adapter.EndlessAppsAdapter;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.aurora.store.task.SearchTask;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.NetworkUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.SearchIterator;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class DevAppsFragment extends BaseFragment {

    @BindView(R.id.search_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.dev_name)
    Chip chipDevName;

    private Context context;
    private String query;
    private EndlessAppsAdapter endlessAppsAdapter;
    private CustomAppListIterator iterator;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        endlessAppsAdapter = new EndlessAppsAdapter(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dev_applist, container, false);
        ButterKnife.bind(this, view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            query = arguments.getString("SearchQuery");
            chipDevName.setText(arguments.getString("SearchTitle"));
            if (NetworkUtil.isConnected(context))
                setupRecycler();
            else
                notifyNetworkFailure();
        } else
            Log.e("No category id provided");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setErrorView(ErrorType.UNKNOWN);
        fetchDevAppsList(false);
    }

    @Override
    protected void fetchData() {
        fetchDevAppsList(true);
    }

    private void fetchDevAppsList(boolean shouldIterate) {
        if (!shouldIterate)
            getIterator();
        disposable.add(Observable.fromCallable(() -> new SearchTask(context)
                .getSearchResults(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appList -> {
                    if (shouldIterate) {
                        addApps(appList);
                    } else if (appList.isEmpty()) {
                        setErrorView(ErrorType.NO_SEARCH);
                        switchViews(true);
                    } else {
                        switchViews(false);
                        endlessAppsAdapter.addData(appList);
                    }
                }, err -> {
                    Log.e(err.getMessage());
                    processException(err);
                }));
    }

    private void getIterator() {
        try {
            GooglePlayAPI api = PlayStoreApiAuthenticator.getApi(context);
            iterator = new CustomAppListIterator(new SearchIterator(api, query));
        } catch (Exception e) {
            processException(e);
        }
    }

    private void addApps(List<App> appsToAdd) {
        if (!appsToAdd.isEmpty()) {
            for (App app : appsToAdd)
                endlessAppsAdapter.add(app);
            endlessAppsAdapter.notifyItemInserted(endlessAppsAdapter.getItemCount() - 1);
        }
        if (iterator.hasNext() && endlessAppsAdapter.getItemCount() < 10) {
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    fetchDevAppsList(true);
                    cancel();
                }
            }, 2500, 1000);
        }
    }

    private void setupRecycler() {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this.getActivity(), R.anim.anim_falldown));
        recyclerView.setAdapter(endlessAppsAdapter);
        EndlessScrollListener mScrollListener = new EndlessScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchDevAppsList(true);
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }

    @Override
    protected View.OnClickListener errRetry() {
        return v -> {
            if (NetworkUtil.isConnected(context)) {
                fetchDevAppsList(false);
            } else {
                setErrorView(ErrorType.NO_NETWORK);
            }
            ((Button) v).setText(getString(R.string.action_retry_ing));
            ((Button) v).setEnabled(false);
        };
    }
}