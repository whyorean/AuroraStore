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
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.model.items.CategoryItem;
import com.aurora.store.ui.category.CategoryAppsActivity;
import com.aurora.store.ui.main.AuroraActivity;
import com.aurora.store.util.ViewUtil;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;

public class CategoriesFragment extends Fragment {

    @BindView(R.id.category_recycler)
    RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    private CategoryManager categoryManager;

    private FastAdapter<CategoryItem> fastAdapter;
    private ItemAdapter<CategoryItem> categoryItemAdapter;

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
        CategoriesModel categoriesModel = new ViewModelProvider(this).get(CategoriesModel.class);
        categoriesModel.getFetchCompleted().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                setupRecycler();
                progressBar.setVisibility(View.GONE);
            }
        });
        categoriesModel.fetchCategories();
    }

    private void setupRecycler() {
        fastAdapter = new FastAdapter<>();
        categoryItemAdapter = new ItemAdapter<>();

        Observable.fromIterable(categoryManager.getCategories(Constants.CATEGORY_APPS))
                .mergeWith(Observable.fromIterable(categoryManager.getCategories(Constants.CATEGORY_GAME)))
                .mergeWith(Observable.fromIterable(categoryManager.getCategories(Constants.CATEGORY_FAMILY)))
                .map(CategoryItem::new)
                .toList()
                .doOnSuccess(categoryItems -> {
                    categoryItemAdapter.add(categoryItems);
                })
                .subscribe();

        //TODO:Add section headers
        fastAdapter.addAdapter(0, categoryItemAdapter);
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