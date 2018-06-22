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
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.CategoryFilterAdapter;
import com.dragons.aurora.adapters.SingleDownloadsAdapter;
import com.dragons.aurora.adapters.SingleRatingsAdapter;
import com.dragons.aurora.task.playstore.CategoryAppsTask;
import com.dragons.aurora.view.AdaptiveToolbar;
import com.github.florent37.shapeofview.shapes.RoundRectView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CategoryAppsFragment extends CategoryAppsTask implements SingleDownloadsAdapter.SingleClickListener, SingleRatingsAdapter.SingleClickListener {

    public static String categoryId;

    @BindView(R.id.view_pager)
    ViewPager viewPager;
    @BindView(R.id.category_tabs)
    TabLayout tabLayout;
    @BindView(R.id.adaptive_toolbar)
    AdaptiveToolbar adaptiveToolbar;
    @BindView(R.id.filter_fab)
    FloatingActionButton filter_fab;
    @BindView(R.id.filter_sheet)
    LinearLayout filter_sheet;

    BottomSheetBehavior filter_Behavior;
    CategoryFilterAdapter categoryFilterAdapter;
    SingleDownloadsAdapter singleDownloadAdapter;
    SingleRatingsAdapter singleRatingAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_endless, container, false);

        ButterKnife.bind(this, view);

        adaptiveToolbar.getAction_icon().setOnClickListener((v -> this.getActivity().onBackPressed()));
        adaptiveToolbar.getTitle0().setText(new CategoryManager(getContext()).getCategoryName(categoryId));
        adaptiveToolbar.getTitle1().setVisibility(View.GONE);

        categoryFilterAdapter = new CategoryFilterAdapter(getActivity(), getActivity().getSupportFragmentManager());

        viewPager.setAdapter(categoryFilterAdapter);
        viewPager.setOffscreenPageLimit(3);
        tabLayout.setupWithViewPager(viewPager);

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

        filter_fab.show();
        filter_fab.setOnClickListener(v -> toggleBottomSheet());

        setupDownloadsFilter();
        setupRatingsFilter();

        Button filter_apply = view.findViewById(R.id.filter_apply);
        filter_apply.setOnClickListener(click -> {
            toggleBottomSheet();
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    viewPager.setAdapter(categoryFilterAdapter), 500);
        });

        ImageView close_sheet = filter_sheet.findViewById(R.id.close_sheet);
        close_sheet.setOnClickListener(v -> toggleBottomSheet());

        return view;
    }

    @Override
    public void onDownloadBadgeClickListener() {
        singleDownloadAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRatingBadgeClickListener() {
        singleRatingAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null)
            categoryId = arguments.getString("CategoryId");
        else
            Log.e(this.getClass().getName(), "No category id provided");
    }

    public void setupDownloadsFilter() {
        RecyclerView filter_downloads = filter_sheet.findViewById(R.id.filter_downloads);
        singleDownloadAdapter = new SingleDownloadsAdapter(getContext(),
                getResources().getStringArray(R.array.filterDownloadsLabels),
                getResources().getStringArray(R.array.filterDownloadsValues));
        singleDownloadAdapter.setOnDownloadBadgeClickListener(this);
        filter_downloads.setItemViewCacheSize(10);
        filter_downloads.setAdapter(singleDownloadAdapter);
    }

    public void setupRatingsFilter() {
        RecyclerView filter_ratings = filter_sheet.findViewById(R.id.filter_ratings);
        singleRatingAdapter = new SingleRatingsAdapter(getContext(),
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
}