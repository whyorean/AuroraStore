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

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.dragons.aurora.AppListIterator;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.EndlessRecyclerViewScrollListener;
import com.dragons.aurora.GridAutoFitLayoutManager;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.EndlessAppsAdapter;
import com.dragons.aurora.adapters.SingleDownloadsAdapter;
import com.dragons.aurora.adapters.SingleRatingsAdapter;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.SearchIterator;
import com.dragons.aurora.task.playstore.SearchTask;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class SearchAppsFragment extends SearchTask implements SingleDownloadsAdapter.SingleClickListener, SingleRatingsAdapter.SingleClickListener {

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

    private View view;
    private String title;
    private String query;
    private Boolean loading;
    private AppListIterator iterator;
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private EndlessAppsAdapter endlessAppsAdapter;
    private SingleDownloadsAdapter singleDownloadAdapter;
    private SingleRatingsAdapter singleRatingAdapter;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            setQuery(arguments.getString("SearchQuery"));
            title = arguments.getString("SearchTitle");
            iterator = setupIterator(getQuery());
            fetchSearchAppsList(false);
        } else
            Log.e(this.getClass().getName(), "No category id provided");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_search_list, container, false);
        ButterKnife.bind(this, view);
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
    }

    @Override
    public void onDestroy() {
        Glide.with(this).pauseAllRequests();
        mDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public void onDownloadBadgeClickListener() {
        singleDownloadAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRatingBadgeClickListener() {
        singleRatingAdapter.notifyDataSetChanged();
    }

    private void getFilterDialog() {
        Dialog ad = new Dialog(getContext());
        ad.setContentView(R.layout.dialog_filter);
        ad.setCancelable(true);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(ad.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER;

        ad.getWindow().setAttributes(layoutParams);

        RecyclerView filter_downloads = ad.findViewById(R.id.filter_downloads);
        singleDownloadAdapter = new SingleDownloadsAdapter(getContext(),
                getResources().getStringArray(R.array.filterDownloadsLabels),
                getResources().getStringArray(R.array.filterDownloadsValues));
        singleDownloadAdapter.setOnDownloadBadgeClickListener(this);
        filter_downloads.setItemViewCacheSize(10);
        filter_downloads.setLayoutManager(new GridAutoFitLayoutManager(getContext(), 120));
        filter_downloads.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_falldown));
        filter_downloads.setAdapter(singleDownloadAdapter);

        RecyclerView filter_ratings = ad.findViewById(R.id.filter_ratings);
        singleRatingAdapter = new SingleRatingsAdapter(getContext(),
                getResources().getStringArray(R.array.filterRatingLabels),
                getResources().getStringArray(R.array.filterRatingValues));
        singleRatingAdapter.setOnRatingBadgeClickListener(this);
        filter_ratings.setItemViewCacheSize(10);
        filter_ratings.setLayoutManager(new GridAutoFitLayoutManager(getContext(), 120));
        filter_ratings.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_falldown));
        filter_ratings.setAdapter(singleRatingAdapter);

        Button filter_apply = ad.findViewById(R.id.filter_apply);
        filter_apply.setOnClickListener(click -> {
            ad.dismiss();
            iterator = setupIterator(getQuery());
            fetchSearchAppsList(false);
        });

        ImageView close_sheet = ad.findViewById(R.id.close_sheet);
        close_sheet.setOnClickListener(v -> ad.dismiss());

        ad.show();

    }

    private AppListIterator setupIterator(String query) {
        AppListIterator iterator;
        try {
            iterator = new AppListIterator(new SearchIterator(new PlayStoreApiAuthenticator(getContext()).getApi(), query));
            iterator.setFilter(new FilterMenu(getContext()).getFilterPreferences());
            return iterator;
        } catch (Exception e) {
            if (e instanceof CredentialsEmptyException)
                Log.e(getClass().getSimpleName(), "Credentials Empty Exception");
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
        recyclerView.setAdapter(endlessAppsAdapter);
        EndlessRecyclerViewScrollListener mEndlessRecyclerViewScrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchSearchAppsList(true);
            }
        };
        recyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);
    }

    private void fetchSearchAppsList(boolean shouldIterate) {
        mDisposable.add(Observable.fromCallable(() -> getResult(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appList -> {
                    if (view != null) {
                        if (shouldIterate) {
                            loading = true;
                            addApps(appList);
                        } else
                            setupListView(appList);
                    }
                }, err -> {
                    processException(err);
                    ohhSnap.setVisibility(View.VISIBLE);
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
                    fetchSearchAppsList(true);
                    cancel();
                }
            }, 1500, 1000);
        }
    }
}