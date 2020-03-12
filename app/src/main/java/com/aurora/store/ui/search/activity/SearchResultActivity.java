package com.aurora.store.ui.search.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.AuroraApplication;
import com.aurora.store.AutoDisposable;
import com.aurora.store.Constants;
import com.aurora.store.EndlessScrollListener;
import com.aurora.store.R;
import com.aurora.store.manager.FilterManager;
import com.aurora.store.model.App;
import com.aurora.store.model.FilterModel;
import com.aurora.store.section.SearchResultSection;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.sheet.FilterBottomSheet;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.search.SearchAppsModel;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.reactivex.disposables.Disposable;

public class SearchResultActivity extends BaseActivity implements SearchResultSection.ClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @BindView(R.id.search_view)
    TextInputEditText searchView;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.filter_fab)
    ExtendedFloatingActionButton filterFab;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;

    private String query;
    private SearchAppsModel model;
    private SearchResultSection section;
    private SectionedRecyclerViewAdapter adapter;
    private AutoDisposable autoDisposable = new AutoDisposable();
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
        ButterKnife.bind(this);
        setupSearch();
        setupResultRecycler();
        sharedPreferences = Util.getPrefs(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        autoDisposable.bindTo(getLifecycle());

        model = new ViewModelProvider(this).get(SearchAppsModel.class);
        model.getQueriedApps().observe(this, appList -> {
            dispatchAppsToAdapter(appList);
        });

        model.getRelatedTags().observe(this, strings -> {
        });

        model.getError().observe(this, errorType -> {
            switch (errorType) {
                case NO_API:
                case SESSION_EXPIRED: {
                    Util.validateApi(this);
                    break;
                }
                case NO_NETWORK: {
                    showSnackBar(coordinator, R.string.error_no_network, v -> {
                        model.fetchQueriedApps(query, false);
                    });
                    break;
                }
            }
        });

        Disposable disposable = AuroraApplication
                .getRxBus()
                .getBus()
                .subscribe(event -> {
                    switch (event.getSubType()) {
                        case API_SUCCESS:
                            model.fetchQueriedApps(query, false);
                            break;
                    }
                });
        autoDisposable.add(disposable);
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            query = bundle.getString("QUERY");
            searchView.setText(query);
            model.fetchQueriedApps(query, false);
        } else
            finishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (Util.filterSearchNonPersistent(this))
            FilterManager.saveFilterPreferences(this, new FilterModel());
        super.onDestroy();
    }

    @OnClick(R.id.action1)
    public void goBack() {
        onBackPressed();
    }

    @OnClick(R.id.filter_fab)
    public void showFilterDialog() {
        FilterBottomSheet filterSheet = new FilterBottomSheet();
        filterSheet.show(getSupportFragmentManager(), "FILTER");
    }

    private void purgeAdapterData() {
        section.purgeData();
        adapter.notifyDataSetChanged();
    }

    private void setupSearch() {
        searchView.setFocusable(false);
        searchView.setOnClickListener(v -> onBackPressed());
    }

    private void dispatchAppsToAdapter(List<App> newList) {
        List<App> oldList = section.getList();
        boolean isUpdated = false;
        if (oldList.isEmpty()) {
            section.updateList(newList);
            adapter.getAdapterForSection(section).notifyAllItemsChanged();
        } else {
            if (!newList.isEmpty()) {
                for (App app : newList) {
                    if (oldList.contains(app)) {
                        continue;
                    }
                    section.add(app);
                    isUpdated = true;
                }
                if (isUpdated)
                    adapter.getAdapterForSection(section).notifyItemInserted(section.getCount() - 1);
            }
        }
    }

    private void setupResultRecycler() {
        adapter = new SectionedRecyclerViewAdapter();
        section = new SearchResultSection(this, this);
        adapter.addSection(section);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        EndlessScrollListener endlessScrollListener = new EndlessScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                model.fetchQueriedApps(query, true);
            }
        };

        recyclerView.addOnScrollListener(endlessScrollListener);
        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (velocityY < 0) {
                    filterFab.show();
                } else if (velocityY > 0) {
                    filterFab.hide();
                }
                return false;
            }
        });
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void onClick(App app) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra(Constants.INTENT_PACKAGE_NAME, app.getPackageName());
        startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
    }

    @Override
    public void onLongClick(App app) {
        AppMenuSheet menuSheet = new AppMenuSheet();
        menuSheet.setApp(app);
        menuSheet.show(getSupportFragmentManager(), "BOTTOM_MENU_SHEET");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREFERENCE_FILTER_APPS: {
                purgeAdapterData();
                model.fetchQueriedApps(query, false);
                break;
            }
        }
    }
}
