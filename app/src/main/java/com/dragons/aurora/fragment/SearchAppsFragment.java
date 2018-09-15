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
import android.widget.TextView;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.bumptech.glide.Glide;
import com.dragons.aurora.AppListIterator;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.EndlessRecyclerViewScrollListener;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.EndlessAppsAdapter;
import com.dragons.aurora.dialogs.FilterDialog;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.SearchIterator;
import com.dragons.aurora.task.playstore.SearchTask;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.isConnected;

public class SearchAppsFragment extends BaseFragment {

    @BindView(R.id.search_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.unicorn)
    RelativeLayout unicorn;
    @BindView(R.id.ohhSnap)
    RelativeLayout ohhSnap;
    @BindView(R.id.progress)
    RelativeLayout progress;
    @BindView(R.id.filter_fab)
    FloatingActionButton filter_fab;
    @BindView(R.id.ohhSnap_retry)
    Button ohhSnap_retry;
    @BindView(R.id.recheck_query)
    Button retry_query;
    @BindView(R.id.searchTitle)
    TextView searchTitle;

    private AHBottomNavigation mBottomNavigationView;

    private View view;
    private String query;
    private AppListIterator iterator;
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private EndlessAppsAdapter endlessAppsAdapter;
    private SearchTask mTask;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_search_list, container, false);
        ButterKnife.bind(this, view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            setQuery(arguments.getString("SearchQuery"));
            searchTitle.setText(arguments.getString("SearchTitle"));
            mTask = new SearchTask(getContext());
            iterator = setupIterator(getQuery());
            fetchSearchAppsList(false);
        } else
            Timber.e("No category id provided");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ohhSnap_retry.setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                hide(view, R.id.ohhSnap);
                iterator = setupIterator(getQuery());
                fetchSearchAppsList(false);
            }
        });
        retry_query.setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                hide(view, R.id.unicorn);
                iterator = setupIterator(getQuery());
                fetchSearchAppsList(false);
            }
        });
        filter_fab.show();
        filter_fab.setOnClickListener(v -> getFilterDialog());
        mBottomNavigationView = Objects.requireNonNull(getActivity()).findViewById(R.id.navigation);
    }

    @Override
    public void onDestroy() {
        Glide.with(this).pauseAllRequests();
        mDisposable.dispose();
        super.onDestroy();
    }

    private void getFilterDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        FilterDialog filterDialog = new FilterDialog();
        filterDialog.setOnApplyListener(v -> {
            filterDialog.dismiss();
            iterator = setupIterator(getQuery());
            fetchSearchAppsList(false);
        });
        filterDialog.show(ft, "dialog");
    }

    private AppListIterator setupIterator(String query) {
        AppListIterator iterator;
        try {
            iterator = new AppListIterator(new SearchIterator(new PlayStoreApiAuthenticator(getContext()).getApi(), query));
            iterator.setFilter(new FilterMenu(getContext()).getFilterPreferences());
            return iterator;
        } catch (Exception e) {
            if (e instanceof CredentialsEmptyException)
                Timber.e("Credentials Empty Exception");
            else
                e.printStackTrace();
            return null;
        }
    }

    private void setupListView(List<App> appsToAdd) {
        progress.setVisibility(View.GONE);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        endlessAppsAdapter = new EndlessAppsAdapter(this, appsToAdd);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this.getActivity(), R.anim.anim_falldown));
        DividerItemDecoration itemDecorator = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(getResources().getDrawable(R.drawable.list_divider));
        recyclerView.addItemDecoration(itemDecorator);
        recyclerView.setAdapter(endlessAppsAdapter);
        EndlessRecyclerViewScrollListener mEndlessRecyclerViewScrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchSearchAppsList(true);
            }
        };
        recyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);
        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (velocityY < 0) {
                    filter_fab.show();
                    if (mBottomNavigationView != null)
                        mBottomNavigationView.restoreBottomNavigation(true);
                } else if (velocityY > 0) {
                    filter_fab.hide();
                    if (mBottomNavigationView != null)
                        mBottomNavigationView.hideBottomNavigation(true);
                }
                return false;
            }
        });
    }

    private void fetchSearchAppsList(boolean shouldIterate) {
        mDisposable.add(Observable.fromCallable(() -> mTask.getResult(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(err -> ohhSnap.setVisibility(View.VISIBLE))
                .subscribe(appList -> {
                    if (view != null) {
                        if (shouldIterate) {
                            addApps(appList);
                        } else
                            setupListView(appList);
                    }
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
                    fetchSearchAppsList(true);
                    cancel();
                }
            }, 2500, 1000);
        }
    }
}