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
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.CategoryFilterAdapter;
import com.dragons.aurora.dialogs.FilterDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class CategoryAppsFragment extends BaseFragment {

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
            Timber.e("No category id provided");
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
    public void onDestroy() {
        Glide.with(this).pauseAllRequests();
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
            viewPager.setAdapter(categoryFilterAdapter);
        });
        filterDialog.show(ft, "dialog");
    }
}