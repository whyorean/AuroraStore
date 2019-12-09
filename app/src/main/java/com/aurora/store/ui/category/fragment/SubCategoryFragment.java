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

package com.aurora.store.ui.category.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.EndlessScrollListener;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.section.EndlessResultSection;
import com.aurora.store.ui.category.CategoryAppsActivity;
import com.aurora.store.ui.category.CategoryAppsModel;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class SubCategoryFragment extends Fragment implements EndlessResultSection.ClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    private Context context;
    private GooglePlayAPI.SUBCATEGORY subcategory = GooglePlayAPI.SUBCATEGORY.TOP_FREE;
    private SharedPreferences sharedPreferences;

    private CategoryAppsModel model;
    private EndlessResultSection section;
    private SectionedRecyclerViewAdapter adapter;

    private GooglePlayAPI.SUBCATEGORY getSubcategory() {
        return subcategory;
    }

    private void setSubcategory(Bundle bundle) {
        String category = bundle.getString("SUBCATEGORY");
        if (category != null)
            switch (category) {
                case "TOP_FREE":
                    subcategory = GooglePlayAPI.SUBCATEGORY.TOP_FREE;
                    break;
                case "TOP_GROSSING":
                    subcategory = GooglePlayAPI.SUBCATEGORY.TOP_GROSSING;
                    break;
                default:
                    subcategory = GooglePlayAPI.SUBCATEGORY.MOVERS_SHAKERS;
            }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = Util.getPrefs(context);
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
        Bundle bundle = getArguments();
        if (bundle != null)
            setSubcategory(bundle);
        setupRecycler();

        model = ViewModelProviders.of(this).get(CategoryAppsModel.class);
        model.getCategoryApps().observe(this, appList -> {
            dispatchAppsToAdapter(appList);
        });
        model.fetchCategoryApps(CategoryAppsActivity.categoryId, getSubcategory(), false);
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    private void dispatchAppsToAdapter(List<App> newList) {
        List<App> oldList = section.getList();
        if (oldList.isEmpty()) {
            section.updateList(newList);
            adapter.notifyDataSetChanged();
        } else {
            if (!newList.isEmpty()) {
                for (App app : newList)
                    section.add(app);
                adapter.notifyItemInserted(section.getCount() - 1);
            }
        }
    }

    private void setupRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        adapter = new SectionedRecyclerViewAdapter();
        section = new EndlessResultSection(context, this);
        adapter.addSection(section);

        recyclerView.setAdapter(adapter);

        EndlessScrollListener endlessScrollListener = new EndlessScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                model.fetchCategoryApps(CategoryAppsActivity.categoryId, getSubcategory(), true);
            }
        };
        recyclerView.addOnScrollListener(endlessScrollListener);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void purgeAdapterData() {
        section.purgeData();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(App app) {
        DetailsActivity.app = app;
        Intent intent = new Intent(context, DetailsActivity.class);
        intent.putExtra(Constants.INTENT_PACKAGE_NAME, app.getPackageName());
        context.startActivity(intent, ViewUtil.getEmptyActivityBundle((AppCompatActivity) context));
    }

    @Override
    public void onLongClick(App app) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREFERENCE_FILTER_APPS)) {
            purgeAdapterData();
            model.fetchCategoryApps(CategoryAppsActivity.categoryId, getSubcategory(), false);
        }
    }
}