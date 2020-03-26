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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.RecyclerDataObserver;
import com.aurora.store.model.App;
import com.aurora.store.model.items.EndlessItem;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.ui.category.CategoryAppsActivity;
import com.aurora.store.ui.category.CategoryAppsModel;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.single.fragment.BaseFragment;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener;
import com.mikepenz.fastadapter.ui.items.ProgressItem;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SubCategoryFragment extends BaseFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.empty_layout)
    RelativeLayout emptyLayout;
    @BindView(R.id.progress_layout)
    RelativeLayout progressLayout;

    private GooglePlayAPI.SUBCATEGORY subcategory = GooglePlayAPI.SUBCATEGORY.TOP_FREE;
    private SharedPreferences sharedPreferences;

    private CategoryAppsModel model;
    private RecyclerDataObserver dataObserver;

    private FastAdapter fastAdapter;
    private ItemAdapter<EndlessItem> itemAdapter;
    private ItemAdapter<ProgressItem> progressItemAdapter;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = Util.getPrefs(requireContext());
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

        model = new ViewModelProvider(this).get(CategoryAppsModel.class);
        model.getCategoryApps().observe(getViewLifecycleOwner(), appList -> {
            dispatchAppsToAdapter(appList);
        });
        model.fetchCategoryApps(CategoryAppsActivity.categoryId, getSubcategory(), false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dataObserver != null && !itemAdapter.getAdapterItems().isEmpty()) {
            dataObserver.hideProgress();
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    private void dispatchAppsToAdapter(List<EndlessItem> endlessItemList) {
        itemAdapter.add(endlessItemList);
        recyclerView.post(() -> {
            progressItemAdapter.clear();
        });
    }

    private void purgeAdapterData() {
        recyclerView.post(() -> {
            progressItemAdapter.clear();
            itemAdapter.clear();
        });

        if (dataObserver != null)
            dataObserver.checkIfEmpty();
    }

    private void setupRecycler() {
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();
        progressItemAdapter = new ItemAdapter<>();

        fastAdapter.addAdapter(0, itemAdapter);
        fastAdapter.addAdapter(1, progressItemAdapter);

        fastAdapter.setOnClickListener((view, iAdapter, item, position) -> {
            if (item instanceof EndlessItem) {
                final App app = ((EndlessItem) item).getApp();
                final Intent intent = new Intent(requireContext(), DetailsActivity.class);
                intent.putExtra(Constants.INTENT_PACKAGE_NAME, app.getPackageName());
                intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app));
                startActivity(intent, ViewUtil.getEmptyActivityBundle((AppCompatActivity) requireActivity()));
            }
            return false;
        });

        fastAdapter.setOnLongClickListener((view, iAdapter, item, position) -> {
            if (item instanceof EndlessItem) {
                final App app = ((EndlessItem) item).getApp();
                final AppMenuSheet menuSheet = new AppMenuSheet();
                final Bundle bundle = new Bundle();
                bundle.putInt(Constants.INT_EXTRA, Integer.parseInt(position.toString()));
                bundle.putString(Constants.STRING_EXTRA, gson.toJson(app));
                menuSheet.setArguments(bundle);
                menuSheet.show(getChildFragmentManager(), AppMenuSheet.TAG);
            }
            return true;
        });

        EndlessRecyclerOnScrollListener endlessScrollListener = new EndlessRecyclerOnScrollListener(progressItemAdapter) {
            @Override
            public void onLoadMore(int currentPage) {
                recyclerView.post(() -> {
                    progressItemAdapter.clear();
                    progressItemAdapter.add(new ProgressItem());
                });
                model.fetchCategoryApps(CategoryAppsActivity.categoryId, getSubcategory(), true);
            }
        };

        dataObserver = new RecyclerDataObserver(recyclerView, emptyLayout, progressLayout);
        fastAdapter.registerAdapterDataObserver(dataObserver);

        recyclerView.addOnScrollListener(endlessScrollListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(fastAdapter);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREFERENCE_FILTER_APPS)) {
            purgeAdapterData();
            model.fetchCategoryApps(CategoryAppsActivity.categoryId, getSubcategory(), false);
        }
    }
}