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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.section.CategoriesSection;
import com.aurora.store.ui.category.CategoryAppsActivity;
import com.aurora.store.ui.main.AuroraActivity;
import com.aurora.store.util.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class CategoriesFragment extends Fragment implements CategoriesSection.ClickListener {

    private static final String TAG_CATEGORIES_ALL = "TAG_CATEGORIES_ALL";
    private static final String TAG_CATEGORIES_GAME = "TAG_CATEGORIES_GAME";
    private static final String TAG_CATEGORIES_FAMILY = "TAG_CATEGORIES_FAMILY";

    @BindView(R.id.category_recycler)
    RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    private AppCompatActivity activity;
    private Context context;
    private CategoryManager categoryManager;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_categories, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (AuroraActivity) getActivity();
        categoryManager = new CategoryManager(context);
        CategoriesModel categoriesModel = ViewModelProviders.of(activity).get(CategoriesModel.class);
        categoriesModel.getFetchCompleted().observe(activity, success -> {
            if (success) {
                setupRecycler();
                progressBar.setVisibility(View.GONE);
            }
        });
        categoriesModel.fetchCategories();
    }

    private void setupRecycler() {
        SectionedRecyclerViewAdapter viewAdapter = new SectionedRecyclerViewAdapter();
        viewAdapter.addSection(TAG_CATEGORIES_ALL, new CategoriesSection(context,
                categoryManager.getCategories(Constants.CATEGORY_APPS), getString(R.string.category_all), this));
        viewAdapter.addSection(TAG_CATEGORIES_GAME, new CategoriesSection(context,
                categoryManager.getCategories(Constants.CATEGORY_GAME), getString(R.string.category_games), this));
        viewAdapter.addSection(TAG_CATEGORIES_FAMILY, new CategoriesSection(context,
                categoryManager.getCategories(Constants.CATEGORY_FAMILY), getString(R.string.category_family), this));
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(viewAdapter);
    }

    @Override
    public void onClick(String categoryId, String categoryName) {
        Intent intent = new Intent(context, CategoryAppsActivity.class);
        intent.putExtra("CategoryId", categoryId);
        intent.putExtra("CategoryName", categoryName);
        context.startActivity(intent, ViewUtil.getEmptyActivityBundle((AuroraActivity) context));
    }
}