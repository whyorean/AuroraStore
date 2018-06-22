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

package com.dragons.aurora.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dragons.aurora.R;
import com.dragons.aurora.adapters.SingleDownloadsAdapter;
import com.dragons.aurora.adapters.SingleRatingsAdapter;
import com.dragons.aurora.fragment.SearchAppsFragment;
import com.github.florent37.shapeofview.shapes.RoundRectView;

import java.util.regex.Pattern;

public class SearchActivity extends AuroraActivity implements SingleDownloadsAdapter.SingleClickListener, SingleRatingsAdapter.SingleClickListener {

    public static final String PUB_PREFIX = "pub:";

    private String query;
    private FloatingActionButton filter_fab;
    private LinearLayout filter_sheet;

    private BottomSheetBehavior filter_Behavior;
    private SingleDownloadsAdapter singleDownloadAdapter;
    private SingleRatingsAdapter singleRatingAdapter;

    static protected boolean actionIs(Intent intent, String action) {
        return null != intent && null != intent.getAction() && intent.getAction().equals(action);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        filter_sheet = findViewById(R.id.filter_sheet);
        filter_Behavior = BottomSheetBehavior.from(filter_sheet);
        filter_Behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        filter_fab.hide();
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        filter_fab.show();
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        filter_fab = findViewById(R.id.filter_fab);
        filter_fab.show();
        filter_fab.setOnClickListener(v -> toggleBottomSheet());

        setupDownloadsFilter();
        setupRatingsFilter();

        Button filter_apply = findViewById(R.id.filter_apply);
        filter_apply.setOnClickListener(click -> {
            toggleBottomSheet();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                getCategoryApps(query, getTitleString());
            }, 500);
        });

        ImageView close_sheet = filter_sheet.findViewById(R.id.close_sheet);
        close_sheet.setOnClickListener(v -> toggleBottomSheet());

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String newQuery = getQuery(intent);
        if (looksLikeAPackageId(newQuery)) {
            Log.i(getClass().getSimpleName(), "Following search suggestion to app page: " + newQuery);
            startActivity(DetailsActivity.getDetailsIntent(this, newQuery));
            finish();
            return;
        }

        Log.i(getClass().getSimpleName(), "Searching: " + newQuery);
        if (null != newQuery && !newQuery.equals(this.query)) {
            this.query = newQuery;
            setTitle(getTitleString());
            getCategoryApps(query, getTitleString());
        }
    }

    @Override
    public void onDownloadBadgeClickListener() {
        singleDownloadAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRatingBadgeClickListener() {
        singleRatingAdapter.notifyDataSetChanged();
    }


    public void setupDownloadsFilter() {
        RecyclerView filter_downloads = filter_sheet.findViewById(R.id.filter_downloads);
        singleDownloadAdapter = new SingleDownloadsAdapter(this,
                getResources().getStringArray(R.array.filterDownloadsLabels),
                getResources().getStringArray(R.array.filterDownloadsValues));
        singleDownloadAdapter.setOnDownloadBadgeClickListener(this);
        filter_downloads.setItemViewCacheSize(10);
        filter_downloads.setAdapter(singleDownloadAdapter);
    }

    public void setupRatingsFilter() {
        RecyclerView filter_ratings = filter_sheet.findViewById(R.id.filter_ratings);
        singleRatingAdapter = new SingleRatingsAdapter(this,
                getResources().getStringArray(R.array.filterRatingLabels),
                getResources().getStringArray(R.array.filterRatingValues));
        singleRatingAdapter.setOnRatingBadgeClickListener(this);
        filter_ratings.setItemViewCacheSize(10);
        filter_ratings.setAdapter(singleRatingAdapter);
    }

    public void toggleBottomSheet() {
        if (filter_Behavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            filter_Behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            filter_fab.hide();
        } else {
            filter_Behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            filter_fab.show();
        }
    }

    private String getTitleString() {
        return query.startsWith(PUB_PREFIX)
                ? getString(R.string.apps_by, query.substring(PUB_PREFIX.length()))
                : getString(R.string.activity_title_search, query)
                ;
    }

    private String getQuery(Intent intent) {
        if (intent.getScheme() != null
                && (intent.getScheme().equals("market")
                || intent.getScheme().equals("http")
                || intent.getScheme().equals("https")
        )
                ) {
            return intent.getData().getQueryParameter("q");
        }
        if (actionIs(intent, Intent.ACTION_SEARCH)) {
            return intent.getStringExtra(SearchManager.QUERY);
        } else if (actionIs(intent, Intent.ACTION_VIEW)) {
            return intent.getDataString();
        }
        return null;
    }

    private boolean looksLikeAPackageId(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        String pattern = "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+[\\p{L}_$][\\p{L}\\p{N}_$]*";
        Pattern r = Pattern.compile(pattern);
        return r.matcher(query).matches();
    }

    public void getCategoryApps(String query, String title) {
        SearchAppsFragment searchAppsFragment = new SearchAppsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("SearchQuery", query);
        arguments.putString("SearchTitle", title);
        searchAppsFragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, searchAppsFragment).commit();
    }

}
