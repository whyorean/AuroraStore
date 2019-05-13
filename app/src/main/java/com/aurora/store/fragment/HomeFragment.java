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
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.SharedPreferencesTranslator;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.adapter.FeaturedAppsAdapter;
import com.aurora.store.adapter.TopCategoriesAdapter;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.task.CategoryList;
import com.aurora.store.task.FeaturedAppsTask;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HomeFragment extends BaseFragment {

    @BindView(R.id.recycler_top_categories)
    RecyclerView recyclerTopCategories;
    @BindView(R.id.recycler_top_apps)
    RecyclerView recyclerTopApps;
    @BindView(R.id.recycler_top_games)
    RecyclerView recyclerTopGames;
    @BindView(R.id.recycler_top_family)
    RecyclerView recyclerTopFamily;

    @BindView(R.id.btn_all_categories)
    MaterialButton btnAllCategories;
    @BindView(R.id.btn_top_apps)
    MaterialButton btnTopApps;
    @BindView(R.id.btn_top_games)
    MaterialButton btnTopGames;
    @BindView(R.id.btn_top_family)
    MaterialButton btnTopFamily;

    private Context context;
    private FeaturedAppsAdapter topAppsAdapter;
    private FeaturedAppsAdapter topGamesAdapter;
    private FeaturedAppsAdapter topFamilyAdapter;
    private BottomNavigationView bottomNavigationView;
    private CategoryManager categoryManager;
    private FeaturedAppsTask featuredAppsTask;
    private SharedPreferencesTranslator translator;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        featuredAppsTask = new FeaturedAppsTask(context);
        categoryManager = new CategoryManager(context);
        translator = new SharedPreferencesTranslator(Util.getPrefs(context));
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
        setErrorView(ErrorType.UNKNOWN);
        init();
        if (getActivity() instanceof AuroraActivity) {
            bottomNavigationView = ((AuroraActivity) getActivity()).getBottomNavigation();
            setBaseBottomNavigationView(bottomNavigationView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (topAppsAdapter.isDataEmpty())
            addApps();
        if (topGamesAdapter.isDataEmpty())
            addGames();
        if (topFamilyAdapter.isDataEmpty())
            addFamily();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.clear();
        featuredAppsTask = null;
    }

    private void init() {
        setupButtons();
        setupRecyclers();
        if (categoryManager.categoryListEmpty())
            getCategoriesFromAPI();
    }

    private void setupButtons() {
        btnAllCategories.setOnClickListener(v -> getAllCategories());
        btnTopApps.setOnClickListener(v -> getCategoryApps("APPLICATION"));
        btnTopGames.setOnClickListener(v -> getCategoryApps("GAME"));
        btnTopFamily.setOnClickListener(v -> getCategoryApps("FAMILY"));
    }

    private void setupRecyclers() {
        topAppsAdapter = new FeaturedAppsAdapter(context);
        topGamesAdapter = new FeaturedAppsAdapter(context);
        topFamilyAdapter = new FeaturedAppsAdapter(context);

        recyclerTopCategories.setAdapter(new TopCategoriesAdapter(this));
        recyclerTopCategories.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        recyclerTopApps.setAdapter(topAppsAdapter);
        recyclerTopApps.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        recyclerTopGames.setAdapter(topGamesAdapter);
        recyclerTopGames.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        recyclerTopFamily.setAdapter(topFamilyAdapter);
        recyclerTopFamily.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));

        Util.attachSnapPager(context, recyclerTopApps);
        Util.attachSnapPager(context, recyclerTopGames);
        Util.attachSnapPager(context, recyclerTopFamily);
    }

    private void addApps() {
        disposable.add(Observable.fromCallable(() -> featuredAppsTask
                .getApps("APPLICATION", GooglePlayAPI.SUBCATEGORY.TOP_FREE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    switchViews(false);
                    topAppsAdapter.addData(appList);
                }, err -> {
                    processException(err);
                    Log.d(err.getMessage());
                }));
    }

    private void addGames() {
        disposable.add(Observable.fromCallable(() -> featuredAppsTask
                .getApps("GAME", GooglePlayAPI.SUBCATEGORY.TOP_GROSSING))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    switchViews(false);
                    topGamesAdapter.addData(appList);
                }, err -> Log.d(err.getMessage())));
    }

    private void addFamily() {
        disposable.add(Observable.fromCallable(() -> featuredAppsTask
                .getApps("FAMILY", GooglePlayAPI.SUBCATEGORY.TOP_GROSSING))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    switchViews(false);
                    topFamilyAdapter.addData(appList);
                }, err -> Log.d(err.getMessage())));
    }

    private void getAllCategories() {
        CategoriesFragment categoryAppsFragment = new CategoriesFragment();
        Bundle arguments = new Bundle();
        arguments.putString("CATEGORY_TYPE", "APPLICATION");
        categoryAppsFragment.setArguments(arguments);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.coordinator, categoryAppsFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    private void getCategoryApps(String categoryID) {
        CategoryAppsFragment categoryAppsFragment = new CategoryAppsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("CategoryId", categoryID);
        arguments.putString("CategoryName", translator.getString(categoryID));
        categoryAppsFragment.setArguments(arguments);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.coordinator, categoryAppsFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    private void getCategoriesFromAPI() {
        disposable.add(Observable.fromCallable(() -> new CategoryList(getContext())
                .getResult())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                }, err -> Log.e(err.getMessage())));
    }

    @Override
    protected View.OnClickListener errRetry() {
        return v -> {
            addApps();
            addGames();
            ((Button) v).setText(getString(R.string.action_retry_ing));
            ((Button) v).setEnabled(false);
        };
    }

    @Override
    protected void fetchData() {
        ContextUtil.runOnUiThread(() -> {
            addApps();
            addGames();
            addFamily();
        });
    }
}
