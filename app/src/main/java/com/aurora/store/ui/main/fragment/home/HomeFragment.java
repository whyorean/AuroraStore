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

package com.aurora.store.ui.main.fragment.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.SharedPreferencesTranslator;
import com.aurora.store.model.App;
import com.aurora.store.model.items.ClusterItem;
import com.aurora.store.ui.category.CategoryAppsActivity;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.main.AuroraActivity;
import com.aurora.store.ui.single.fragment.BaseFragment;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.button.MaterialButton;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeFragment extends BaseFragment {

    @BindView(R.id.recycler_top_apps)
    RecyclerView recyclerTopApps;
    @BindView(R.id.recycler_top_games)
    RecyclerView recyclerTopGames;
    @BindView(R.id.recycler_top_family)
    RecyclerView recyclerTopFamily;

    @BindView(R.id.btn_top_apps)
    MaterialButton btnTopApps;
    @BindView(R.id.btn_top_games)
    MaterialButton btnTopGames;
    @BindView(R.id.btn_top_family)
    MaterialButton btnTopFamily;

    private SharedPreferencesTranslator translator;

    private HomeAppsModel model;
    private FastItemAdapter<ClusterItem> topAppItemAdapter;
    private FastItemAdapter<ClusterItem> topGameItemAdapter;
    private FastItemAdapter<ClusterItem> topFamilyItemAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        translator = new SharedPreferencesTranslator(Util.getPrefs(requireContext()));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupRecyclers();

        model = new ViewModelProvider(this).get(HomeAppsModel.class);
        model.getTopApps().observe(getViewLifecycleOwner(), clusterItems -> topAppItemAdapter.add(clusterItems));
        model.getTopGames().observe(getViewLifecycleOwner(), clusterItems -> topGameItemAdapter.add(clusterItems));
        model.getTopFamily().observe(getViewLifecycleOwner(), clusterItems -> topFamilyItemAdapter.add(clusterItems));
        model.getError().observe(getViewLifecycleOwner(), errorType -> {

        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void init() {
        setupButtons();
    }

    private void setupButtons() {
        btnTopApps.setOnClickListener(v -> openCategoryAppsActivity(Constants.CATEGORY_APPS));
        btnTopGames.setOnClickListener(v -> openCategoryAppsActivity(Constants.CATEGORY_GAME));
        btnTopFamily.setOnClickListener(v -> openCategoryAppsActivity(Constants.CATEGORY_FAMILY));
    }

    private void setupRecyclers() {
        topAppItemAdapter = new FastItemAdapter<>();
        topGameItemAdapter = new FastItemAdapter<>();
        topFamilyItemAdapter = new FastItemAdapter<>();

        recyclerTopApps.setAdapter(topAppItemAdapter);
        recyclerTopApps.setLayoutManager(new GridLayoutManager(requireContext(), 2, RecyclerView.HORIZONTAL, false));

        recyclerTopGames.setAdapter(topGameItemAdapter);
        recyclerTopGames.setLayoutManager(new GridLayoutManager(requireContext(), 2, RecyclerView.HORIZONTAL, false));

        recyclerTopFamily.setAdapter(topFamilyItemAdapter);
        recyclerTopFamily.setLayoutManager(new GridLayoutManager(requireContext(), 2, RecyclerView.HORIZONTAL, false));

        topAppItemAdapter.setOnClickListener((view, clusterItemIAdapter, clusterItem, integer) -> {
            openDetailsActivity(clusterItem.getApp());
            return false;
        });
        topGameItemAdapter.setOnClickListener((view, clusterItemIAdapter, clusterItem, integer) -> {
            openDetailsActivity(clusterItem.getApp());
            return false;
        });
        topFamilyItemAdapter.setOnClickListener((view, clusterItemIAdapter, clusterItem, integer) -> {
            openDetailsActivity(clusterItem.getApp());
            return false;
        });
    }

    private void openDetailsActivity(App app) {
        final Intent intent = new Intent(requireContext(), DetailsActivity.class);
        intent.putExtra(Constants.INTENT_PACKAGE_NAME, app.getPackageName());
        intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app));
        startActivity(intent, ViewUtil.getEmptyActivityBundle((AppCompatActivity) requireActivity()));
    }

    private void openCategoryAppsActivity(String categoryId) {
        Intent intent = new Intent(requireContext(), CategoryAppsActivity.class);
        intent.putExtra("CategoryId", categoryId);
        intent.putExtra("CategoryName", translator.getString(categoryId));
        requireContext().startActivity(intent, ViewUtil.getEmptyActivityBundle((AuroraActivity) requireContext()));
    }
}
