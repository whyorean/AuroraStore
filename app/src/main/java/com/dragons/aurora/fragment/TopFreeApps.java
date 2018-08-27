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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.dragons.aurora.AppListIterator;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.EndlessRecyclerViewScrollListener;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.EndlessAppsAdapter;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.CategoryAppsIterator;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.playstore.CategoryAppsTask;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.isConnected;

public class TopFreeApps extends CategoryAppsTask {

    @BindView(R.id.endless_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.unicorn)
    RelativeLayout unicorn;
    @BindView(R.id.ohhSnap)
    RelativeLayout ohhSnap;
    @BindView(R.id.progress)
    RelativeLayout progress;
    @BindView(R.id.ohhSnap_retry)
    Button ohhSnap_retry;
    @BindView(R.id.recheck_query)
    Button retry_query;

    private View view;
    private AppListIterator iterator;
    private EndlessAppsAdapter endlessAppsAdapter;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    public RelativeLayout getUnicorn() {
        return unicorn;
    }

    public RelativeLayout getOhhSnap() {
        return ohhSnap;
    }

    public RelativeLayout getProgress() {
        return progress;
    }

    public void setProgress(RelativeLayout progress) {
        this.progress = progress;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public AppListIterator getIterator() {
        return iterator;
    }

    public void setIterator(AppListIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_endless_categorized, container, false);
        ButterKnife.bind(this,view);
        setRecyclerView(recyclerView);
        setIterator(setupIterator(CategoryAppsFragment.categoryId, GooglePlayAPI.SUBCATEGORY.TOP_FREE));
        fetchCategoryApps(false);
        return view;
    }

    private void init() {
        ohhSnap_retry.setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                hide(view, R.id.ohhSnap);
                fetchCategoryApps(false);
            }
        });

        retry_query.setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                hide(view, R.id.unicorn);
                fetchCategoryApps(false);
            }
        });
    }

    protected AppListIterator setupIterator(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory) {
        AppListIterator iterator;
        try {
            iterator = new AppListIterator(new CategoryAppsIterator(
                    new PlayStoreApiAuthenticator(getContext()).getApi(),
                    categoryId,
                    subcategory));
            iterator.setFilter(new FilterMenu(getContext()).getFilterPreferences());
            return iterator;
        } catch (Exception e) {
            if (e instanceof CredentialsEmptyException)
                Log.e(getClass().getSimpleName(), "Credentials Empty Exception");
            else
                Log.e(getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    private void setupListView(List<App> appsToAdd) {
        getProgress().setVisibility(View.GONE);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        endlessAppsAdapter = new EndlessAppsAdapter(this, appsToAdd);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_falldown));
        recyclerView.setAdapter(endlessAppsAdapter);
        EndlessRecyclerViewScrollListener mEndlessRecyclerViewScrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchCategoryApps(true);
            }
        };
        recyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);
    }

    protected void fetchCategoryApps(boolean shouldIterate) {
        mDisposable.add(Observable.fromCallable(() -> getResult(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(start -> progress.setVisibility(View.VISIBLE))
                .doOnTerminate(() -> progress.setVisibility(View.GONE))
                .subscribe(appList -> {
                    if (shouldIterate) {
                        addApps(appList);
                    } else
                        setupListView(appList);
                }, err -> {
                    processException(err);
                    getOhhSnap().setVisibility(View.VISIBLE);
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
                    fetchCategoryApps(true);
                    cancel();
                }
            }, 1500, 1000);
        }
    }
}