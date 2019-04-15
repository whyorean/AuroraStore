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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.EndlessScrollListener;
import com.aurora.store.R;
import com.aurora.store.adapter.EndlessAppsAdapter;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.aurora.store.task.CategoryAppsTask;
import com.aurora.store.utility.Log;
import com.dragons.aurora.playstoreapiv2.CategoryAppsIterator;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class TopFreeApps extends BaseFragment implements BaseFragment.EventListenerImpl {

    @BindView(R.id.endless_apps_list)
    RecyclerView recyclerView;

    private FloatingActionButton filterFab;
    private CustomAppListIterator iterator;
    private EndlessAppsAdapter endlessAppsAdapter;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    public CustomAppListIterator getIterator() {
        return iterator;
    }

    public void setIterator(CustomAppListIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_applist, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
        setupListView();

        if (getParentFragment() != null && getParentFragment() instanceof CategoryAppsFragment) {
            filterFab = ((CategoryAppsFragment) getParentFragment()).getFilterFab();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (endlessAppsAdapter == null || endlessAppsAdapter.isDataEmpty())
            fetchCategoryApps(false);
    }

    public void init() {
        setIterator(setupIterator(CategoryAppsFragment.categoryId, GooglePlayAPI.SUBCATEGORY.TOP_FREE));
    }

    private void setupListView() {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        endlessAppsAdapter = new EndlessAppsAdapter(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_falldown));
        recyclerView.setAdapter(endlessAppsAdapter);
        EndlessScrollListener mEndlessRecyclerViewScrollListener = new EndlessScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchCategoryApps(true);
            }
        };
        recyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);
        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (velocityY < 0) {
                    filterFab.show();
                } else if (velocityY > 0) {
                    filterFab.hide();
                }
                return false;
            }
        });
    }

    public CustomAppListIterator setupIterator(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory) {
        try {
            final GooglePlayAPI api = new PlayStoreApiAuthenticator(getContext()).getApi();
            final CategoryAppsIterator iterator = new CategoryAppsIterator(api, categoryId, subcategory);
            return new CustomAppListIterator(iterator);
        } catch (Exception err) {
            processException(err);
            return null;
        }
    }

    public void fetchCategoryApps(boolean shouldIterate) {
        mDisposable.add(Observable.fromCallable(() -> new CategoryAppsTask(getContext())
                .getApps(getIterator()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appList -> {
                    if (shouldIterate) {
                        addApps(appList);
                    } else
                        endlessAppsAdapter.addData(appList);
                }, err -> Log.e(err.getMessage())));
    }

    private void addApps(List<App> appsToAdd) {
        if (!appsToAdd.isEmpty()) {
            for (App app : appsToAdd)
                endlessAppsAdapter.add(app);
            endlessAppsAdapter.notifyItemInserted(endlessAppsAdapter.getItemCount() - 1);
        }
        if (iterator.hasNext() && endlessAppsAdapter.getItemCount() < 10) {
            iterator.next();
        }
    }

    @Override
    public void onLoggedIn() {
        fetchCategoryApps(false);
    }

    @Override
    public void onLoginFailed() {

    }

    @Override
    public void onNetworkFailed() {

    }
}