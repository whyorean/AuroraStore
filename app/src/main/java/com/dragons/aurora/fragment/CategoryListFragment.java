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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.AllCategoriesAdapter;
import com.dragons.aurora.adapters.TopCategoriesAdapter;
import com.dragons.aurora.task.playstore.CategoryListTask;
import com.percolate.caffeine.ViewUtils;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.show;


public class CategoryListFragment extends CategoryListTask {

    private View v;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Disposable loadApps;
    private CategoryManager categoryManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        categoryManager = new CategoryManager(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (v != null) {
            if ((ViewGroup) v.getParent() != null)
                ((ViewGroup) v.getParent()).removeView(v);
            return v;
        }

        v = inflater.inflate(R.layout.fragment_categories, container, false);

        if (isLoggedIn() && categoryManager.categoryListEmpty())
            getAllCategories();
        else {
            setupAllCategories();
            setupTopCategories();
        }

        swipeRefreshLayout = ViewUtils.findViewById(v, R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isLoggedIn()) getAllCategories();
            else swipeRefreshLayout.setRefreshing(false);
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLoggedIn() && categoryManager.categoryListEmpty())
            getAllCategories();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    protected void setupTopCategories() {
        RecyclerView recyclerView = ViewUtils.findViewById(v, R.id.top_cat_view);
        recyclerView.setAdapter(new TopCategoriesAdapter(this.getActivity(), getResources().getStringArray(R.array.topCategories)));
    }

    protected void setupAllCategories() {
        show(v, R.id.all_cat_view);
        RecyclerView recyclerView = ViewUtils.findViewById(v, R.id.all_cat_view);
        recyclerView.setAdapter(new AllCategoriesAdapter(this.getActivity(), categoryManager.getCategoriesFromSharedPreferences()));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_anim));
    }

    protected void getAllCategories() {
        show(v, R.id.loading_cat);
        hide(v, R.id.all_cat_view);
        if (isDummy())
            refreshMyToken();
        loadApps = Observable.fromCallable(() -> getResult(getContext()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    if (result) {
                        if (v != null) {
                            setupTopCategories();
                            setupAllCategories();
                            swipeRefreshLayout.setRefreshing(false);
                            hide(v, R.id.loading_cat);
                        }
                    }
                }, this::processException);
    }
}
