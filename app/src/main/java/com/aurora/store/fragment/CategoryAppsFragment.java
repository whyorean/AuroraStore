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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.aurora.store.Filter;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.adapter.SubCategoryAdapter;
import com.aurora.store.sheet.FilterBottomSheet;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CategoryAppsFragment extends Fragment {

    public static String categoryId;

    @BindView(R.id.view_pager)
    ViewPager viewPager;
    @BindView(R.id.category_tabs)
    TabLayout tabLayout;
    @BindView(R.id.filter_fab)
    FloatingActionButton filterFab;

    private Context context;
    private BottomNavigationView bottomNavigationView;
    private ActionBar actionBar;

    public FloatingActionButton getFilterFab() {
        return filterFab;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_category_container, container, false);
        ButterKnife.bind(this, view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            categoryId = arguments.getString("CategoryId");
            if (getActivity() instanceof AuroraActivity) {
                actionBar = ((AuroraActivity) getActivity()).getSupportActionBar();
                actionBar.setTitle(arguments.getString("CategoryName"));
            }
        } else
            Log.e("No category id provided");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SubCategoryAdapter subCategoryAdapter = new SubCategoryAdapter(getContext(), getChildFragmentManager());
        viewPager.setAdapter(subCategoryAdapter);
        tabLayout.setupWithViewPager(viewPager);
        filterFab.setOnClickListener(v -> {
            getFilterDialog();
        });
        if (getActivity() instanceof AuroraActivity) {
            bottomNavigationView = ((AuroraActivity) getActivity()).getBottomNavigation();
            ViewUtil.hideBottomNav(bottomNavigationView, true);
        }
    }

    @Override
    public void onDestroy() {
        Glide.with(this).pauseAllRequests();
        if (actionBar != null)
            actionBar.setTitle(getString(R.string.app_name));
        if (bottomNavigationView != null)
            ViewUtil.showBottomNav(bottomNavigationView, true);
        if (Util.filterSearchNonPersistent(context))
            new Filter(context).resetFilterPreferences();
        super.onDestroy();
    }

    private void getFilterDialog() {
        FilterBottomSheet filterSheet = new FilterBottomSheet();
        filterSheet.setOnApplyListener(v -> {
            filterSheet.dismiss();
            viewPager.removeAllViews();
            viewPager.setAdapter(new SubCategoryAdapter(getContext(), getChildFragmentManager()));
        });
        filterSheet.show(getChildFragmentManager(), "FILTER");
    }
}