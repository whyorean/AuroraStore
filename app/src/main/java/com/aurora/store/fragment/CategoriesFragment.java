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
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.SharedPreferencesTranslator;
import com.aurora.store.adapter.CategoriesListAdapter;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.task.CategoryList;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.Util;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class CategoriesFragment extends Fragment implements CategoriesListAdapter.onClickListener {

    public static final String APPS = "APPLICATION";
    public static final String GAME = "GAME";
    public static final String FAMILY = "FAMILY";


    @BindView(R.id.category_recycler)
    RecyclerView recyclerView;

    private Context context;
    private CategoryManager categoryManager;
    private SharedPreferencesTranslator translator;
    private Map<String, String> categories = new HashMap<>();
    private CompositeDisposable disposable = new CompositeDisposable();
    private CategoriesListAdapter categoriesListAdapter;
    private String categoryType = APPS;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        categoryManager = new CategoryManager(context);
        translator = new SharedPreferencesTranslator(Util.getPrefs(context));
        categoriesListAdapter = new CategoriesListAdapter(context);
        categoriesListAdapter.setOnItemClickListener(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_categories, container, false);
        ButterKnife.bind(this, view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            categoryType = arguments.getString("CATEGORY_TYPE");
        } else
            Log.e("No category id provided");
        setupAllCategories();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (categoriesListAdapter == null || categoriesListAdapter.isDataEmpty())
            getCategories();
    }

    @Override
    public void onDestroy() {
        GlideApp.with(this).pauseAllRequests();
        super.onDestroy();
    }

    private void setupAllCategories() {
        recyclerView.setLayoutManager(new GridLayoutManager(context, getSpanCount()));
        recyclerView.setAdapter(categoriesListAdapter);
    }

    private int getSpanCount() {
        int width = Resources.getSystem().getConfiguration().screenWidthDp;
        return width / 200;
    }

    private void getCategories() {
        if (categoryManager.categoryListEmpty()) {
            getCategoriesFromAPI();
            return;
        }
        categories = categoryManager.getAllCategories();
        switch (categoryType) {
            case APPS:
                categoriesListAdapter.addData(categoryManager.getAllCategories());
                break;
            case GAME:
                categoriesListAdapter.addData(categoryManager.getAllGames());
                break;
            case FAMILY:
                categoriesListAdapter.addData(categoryManager.getAllFamily());
                break;
            default:
                categoriesListAdapter.addData(categoryManager.getAllCategories());
        }
    }

    private void getCategoriesFromAPI() {
        disposable.add(Observable.fromCallable(() -> new CategoryList(context)
                .getResult())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    if (success) {
                        Log.i("CategoryList fetch completed");
                        ContextUtil.runOnUiThread(() -> {
                            getCategories();
                            Log.i("Categories populated");
                        });
                    }
                }, err -> Log.e(err.getMessage())));
    }

    @Override
    public void onItemClick(int position, View v) {
        Bundle bundle = new Bundle();
        bundle.putString("CategoryId", new ArrayList<>(categories.keySet()).get(position));
        bundle.putString("CategoryName", translator.getString(new ArrayList<>(categories.keySet()).get(position)));
        NavHostFragment.findNavController(this).navigate(R.id.category_to_applist, bundle);
    }

    @Override
    public void onItemLongClick(int position, View v) {

    }
}