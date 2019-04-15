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
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.EndlessScrollListener;
import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.adapter.EndlessAppsAdapter;
import com.aurora.store.model.App;
import com.aurora.store.task.SearchTask;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.NetworkUtil;
import com.aurora.store.view.ErrorView;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class DevAppsFragment extends BaseFragment implements BaseFragment.EventListenerImpl {

    @BindView(R.id.view_switcher)
    ViewSwitcher mViewSwitcher;
    @BindView(R.id.content_view)
    LinearLayout layoutContent;
    @BindView(R.id.err_view)
    LinearLayout layoutError;
    @BindView(R.id.search_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.dev_name)
    Chip chipDevName;

    private Context context;
    private View view;
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private EndlessAppsAdapter endlessAppsAdapter;
    private SearchTask mSearchTask;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        mSearchTask = new SearchTask(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_dev_applist, container, false);
        ButterKnife.bind(this, view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            String query = arguments.getString("SearchQuery");
            chipDevName.setText(arguments.getString("SearchTitle"));
            iterator = getIterator(query);
            if (NetworkUtil.isConnected(context))
                setupRecycler();
            else
                onNetworkFailed();
        } else
            Log.e("No category id provided");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setErrorView(ErrorType.UNKNOWN);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (endlessAppsAdapter == null || endlessAppsAdapter.isDataEmpty())
            fetchDevAppsList(false);
    }

    @Override
    public void onDestroy() {
        Glide.with(this).pauseAllRequests();
        mDisposable.dispose();
        super.onDestroy();
    }

    private void fetchDevAppsList(boolean shouldIterate) {
        mDisposable.add(Observable.fromCallable(() -> mSearchTask.getSearchResults(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appList -> {
                    if (view != null) {
                        if (shouldIterate) {
                            addApps(appList);
                        } else if (appList.isEmpty()) {
                            setErrorView(ErrorType.NO_SEARCH);
                            switchViews(true);
                        } else {
                            switchViews(false);
                            endlessAppsAdapter.addData(appList);
                        }
                    }
                }, err -> {
                    Log.e(err.getMessage());
                    processException(err);
                }));
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
        endlessAppsAdapter = new EndlessAppsAdapter(context);
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

    private void switchViews(boolean showError) {
        if (mViewSwitcher.getCurrentView() == layoutContent && showError)
            mViewSwitcher.showNext();
        else if (mViewSwitcher.getCurrentView() == layoutError && !showError)
            mViewSwitcher.showPrevious();
    }

    private void setErrorView(ErrorType errorType) {
        layoutError.removeAllViews();
        layoutError.addView(new ErrorView(context, errorType, retry()));
    }

    private View.OnClickListener retry() {
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

    @Override
    public void onLoggedIn() {
        fetchDevAppsList(false);
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