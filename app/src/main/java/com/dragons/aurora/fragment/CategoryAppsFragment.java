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
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.dragons.aurora.GridAutoFitLayoutManager;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.CategoryFilterAdapter;
import com.dragons.aurora.adapters.SingleDownloadsAdapter;
import com.dragons.aurora.adapters.SingleRatingsAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;

public class CategoryAppsFragment extends BaseFragment implements SingleDownloadsAdapter.SingleClickListener, SingleRatingsAdapter.SingleClickListener {

    public static String categoryId;

    @BindView(R.id.view_pager)
    ViewPager viewPager;
    @BindView(R.id.category_tabs)
    TabLayout tabLayout;
    @BindView(R.id.filter_fab)
    FloatingActionButton filter_fab;
    @BindView(R.id.categoryTitle)
    TextView categoryTitle;

    private CategoryFilterAdapter categoryFilterAdapter;
    private SingleDownloadsAdapter singleDownloadAdapter;
    private SingleRatingsAdapter singleRatingAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_endless, container, false);
        ButterKnife.bind(this, view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            categoryId = arguments.getString("CategoryId");
            categoryTitle.setText(arguments.getString("CategoryName"));
        } else
            Log.e(this.getClass().getName(), "No category id provided");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        categoryFilterAdapter = new CategoryFilterAdapter(getActivity(), getActivity().getSupportFragmentManager());
        viewPager.setAdapter(categoryFilterAdapter);
        viewPager.setOffscreenPageLimit(3);
        tabLayout.setupWithViewPager(viewPager);
        filter_fab.show();
        filter_fab.setOnClickListener(v -> getFilterDialog());
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
    public void onDestroy() {
        Glide.with(this).pauseAllRequests();
        super.onDestroy();
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
            viewPager.setAdapter(categoryFilterAdapter);
        });

        ImageView close_sheet = ad.findViewById(R.id.close_sheet);
        close_sheet.setOnClickListener(v -> ad.dismiss());

        ad.show();
    }
}