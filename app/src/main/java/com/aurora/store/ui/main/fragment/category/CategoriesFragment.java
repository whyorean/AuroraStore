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

package com.aurora.store.ui.main.fragment.category;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.model.items.CategoryItem;
import com.aurora.store.ui.category.CategoryAppsActivity;
import com.aurora.store.ui.main.AuroraActivity;
import com.aurora.store.ui.view.ViewFlipper2;
import com.aurora.store.util.Log;
import com.aurora.store.util.ViewUtil;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

public class CategoriesFragment extends Fragment {

    @BindView(R.id.swipe_layout)
    SwipeRefreshLayout swipeLayout;
    @BindView(R.id.view_flipper)
    ViewFlipper2 viewFlipper;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    private CategoryManager categoryManager;

    private FastAdapter<CategoryItem> fastAdapter;
    private ItemAdapter<CategoryItem> itemAdapter;
    private CompositeDisposable disposable = new CompositeDisposable();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_categories, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        categoryManager = new CategoryManager(requireContext());
        setupRecycler();

        CategoriesModel categoriesModel = new ViewModelProvider(this).get(CategoriesModel.class);
        categoriesModel.getData().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                dispatchDataToAdapter();
            }
            swipeLayout.setRefreshing(false);
        });
        categoriesModel.fetchCategories();

        swipeLayout.setOnRefreshListener(categoriesModel::fetchCategories);
    }

    @Override
    public void onPause() {
        swipeLayout.setRefreshing(false);
        super.onPause();
    }

    private void dispatchDataToAdapter() {
        disposable.add(Observable.fromIterable(categoryManager.getCategories(Constants.CATEGORY_APPS))
                .mergeWith(Observable.fromIterable(categoryManager.getCategories(Constants.CATEGORY_GAME)))
                .mergeWith(Observable.fromIterable(categoryManager.getCategories(Constants.CATEGORY_FAMILY)))
                .map(CategoryItem::new)
                .toList()
                .subscribe(categoryItems -> {
                            itemAdapter.add(categoryItems);
                            updatePageData();
                        },
                        throwable -> Log.e(throwable.getMessage())));
    }

    private void updatePageData() {
        if (itemAdapter != null && itemAdapter.getAdapterItems().size() > 0) {
            viewFlipper.switchState(ViewFlipper2.DATA);
        } else {
            viewFlipper.switchState(ViewFlipper2.EMPTY);
        }
    }

    private void setupRecycler() {
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();

        //TODO:Add section headers
        fastAdapter.addAdapter(0, itemAdapter);
        fastAdapter.setOnClickListener((view, categoryItemIAdapter, categoryItem, integer) -> {
            Intent intent = new Intent(requireContext(), CategoryAppsActivity.class);
            intent.putExtra("CategoryId", categoryItem.getCategoryModel().getCategoryId());
            intent.putExtra("CategoryName", categoryItem.getCategoryModel().getCategoryTitle());
            requireContext().startActivity(intent, ViewUtil.getEmptyActivityBundle((AuroraActivity) requireContext()));
            return false;
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(fastAdapter);
    }
}