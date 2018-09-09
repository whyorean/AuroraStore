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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
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
import com.dragons.aurora.task.playstore.ExceptionTask;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.dragons.aurora.Util.isConnected;

public class TopFreeApps extends BaseFragment {

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
    private FloatingActionButton filter_fab;
    private AHBottomNavigation mBottomNavigationView;
    private AppListIterator iterator;
    private EndlessAppsAdapter endlessAppsAdapter;
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private CategoryAppsTask mTask;

    public Button getOhhSnap_retry() {
        return ohhSnap_retry;
    }

    public Button getRetry_query() {
        return retry_query;
    }

    public AppListIterator getIterator() {
        return iterator;
    }

    public void setIterator(AppListIterator iterator) {
        this.iterator = iterator;
    }

    public CategoryAppsTask getTask() {
        return mTask;
    }

    public void setTask(CategoryAppsTask mTask) {
        this.mTask = mTask;
    }

    public RelativeLayout getUnicorn() {
        return unicorn;
    }

    public RelativeLayout getOhhSnap() {
        return ohhSnap;
    }

    public RelativeLayout getProgress() {
        return progress;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_endless_categorized, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
        setTask(new CategoryAppsTask(getContext()));
        fetchCategoryApps(false);

        getOhhSnap_retry().setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                getOhhSnap().setVisibility(View.GONE);
                fetchCategoryApps(false);
            }
        });
        getRetry_query().setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                getUnicorn().setVisibility(View.GONE);
                fetchCategoryApps(false);
            }
        });

        filter_fab = Objects.requireNonNull(getActivity()).findViewById(R.id.filter_fab);
        mBottomNavigationView = Objects.requireNonNull(getActivity()).findViewById(R.id.navigation);
    }

    public void init() {
        setIterator(setupIterator(CategoryAppsFragment.categoryId, GooglePlayAPI.SUBCATEGORY.TOP_FREE));
    }

    private void setupListView(List<App> appsToAdd) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        endlessAppsAdapter = new EndlessAppsAdapter(this, appsToAdd);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_falldown));
        DividerItemDecoration itemDecorator = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(getResources().getDrawable(R.drawable.list_divider));
        recyclerView.addItemDecoration(itemDecorator);
        recyclerView.setAdapter(endlessAppsAdapter);
        EndlessRecyclerViewScrollListener mEndlessRecyclerViewScrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager) {
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
                    filter_fab.show();
                    mBottomNavigationView.restoreBottomNavigation(true);
                } else if (velocityY > 0) {
                    filter_fab.hide();
                    mBottomNavigationView.hideBottomNavigation(true);
                }
                return false;
            }
        });
    }

    protected AppListIterator setupIterator(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory) {
        try {
            iterator = new AppListIterator(new CategoryAppsIterator(
                    new PlayStoreApiAuthenticator(getContext()).getApi(),
                    categoryId,
                    subcategory));
            iterator.setFilter(new FilterMenu(getContext()).getFilterPreferences());
            return iterator;
        } catch (Exception e) {
            if (e instanceof CredentialsEmptyException) {
                new ExceptionTask(getContext()).processException(e);
                Timber.e("Credentials Empty Exception");
            } else
                Timber.e(e.getMessage());
            return null;
        }
    }

    private void fetchCategoryApps(boolean shouldIterate) {
        mDisposable.add(Observable.fromCallable(() -> getTask().getResult(getIterator()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(() -> progress.setVisibility(View.GONE))
                .doOnError(err -> ohhSnap.setVisibility(View.VISIBLE))
                .subscribe(appList -> {
                    if (shouldIterate) {
                        addApps(appList);
                    } else
                        setupListView(appList);
                }, err -> Timber.e(err.getMessage())));
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
            }, 2500, 1000);
        }
    }

}