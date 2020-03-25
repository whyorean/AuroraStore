package com.aurora.store.ui.search.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RelativeLayout;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.RecyclerDataObserver;
import com.aurora.store.manager.FilterManager;
import com.aurora.store.model.App;
import com.aurora.store.model.FilterModel;
import com.aurora.store.model.items.EndlessItem;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.sheet.FilterBottomSheet;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.search.SearchAppsModel;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener;
import com.mikepenz.fastadapter.ui.items.ProgressItem;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchResultActivity extends BaseActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @BindView(R.id.search_view)
    TextInputEditText searchView;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.filter_fab)
    ExtendedFloatingActionButton filterFab;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.empty_layout)
    RelativeLayout emptyLayout;
    @BindView(R.id.progress_layout)
    RelativeLayout progressLayout;

    private String query;
    private SharedPreferences sharedPreferences;

    private SearchAppsModel model;
    private RecyclerDataObserver dataObserver;

    private FastAdapter fastAdapter;
    private ItemAdapter<EndlessItem> itemAdapter;
    private ItemAdapter<ProgressItem> progressItemAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
        ButterKnife.bind(this);
        setupSearch();
        setupResultRecycler();

        sharedPreferences = Util.getPrefs(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        model = new ViewModelProvider(this).get(SearchAppsModel.class);
        model.getQueriedApps().observe(this, this::dispatchAppsToAdapter);
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

        AuroraApplication
                .getRelayBus()
                .doOnNext(event -> {
                    switch (event.getSubType()) {
                        case BLACKLIST:
                            int adapterPosition = event.getIntExtra();
                            if (adapterPosition >= 0 && itemAdapter != null) {
                                itemAdapter.remove(adapterPosition);
                            }
                            break;
                        case API_SUCCESS:
                            model.fetchQueriedApps(query, false);
                            break;
                    }
                })
                .subscribe();

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
        recyclerView.post(() -> {
            progressItemAdapter.clear();
            itemAdapter.clear();
        });
    }

    private void setupSearch() {
        searchView.setFocusable(false);
        searchView.setOnClickListener(v -> onBackPressed());
    }

    private void dispatchAppsToAdapter(List<EndlessItem> endlessItemList) {
        itemAdapter.add(endlessItemList);
        recyclerView.post(() -> {
            progressItemAdapter.clear();
        });

        if (dataObserver != null)
            dataObserver.checkIfEmpty();
    }

    private void setupResultRecycler() {
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();
        progressItemAdapter = new ItemAdapter<>();

        fastAdapter.addAdapter(0, itemAdapter);
        fastAdapter.addAdapter(1, progressItemAdapter);

        fastAdapter.setOnClickListener((view, iAdapter, item, integer) -> {
            if (item instanceof EndlessItem) {
                final App app = ((EndlessItem) item).getApp();
                final Intent intent = new Intent(this, DetailsActivity.class);
                intent.putExtra(Constants.INTENT_PACKAGE_NAME, app.getPackageName());
                intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app));
                startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
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
                menuSheet.show(getSupportFragmentManager(), AppMenuSheet.TAG);
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
                model.fetchQueriedApps(query, true);
            }
        };

        dataObserver = new RecyclerDataObserver(recyclerView, emptyLayout, progressLayout);
        fastAdapter.registerAdapterDataObserver(dataObserver);

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

        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(fastAdapter);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREFERENCE_FILTER_APPS)) {
            purgeAdapterData();
            model.fetchQueriedApps(query, false);
        }
    }
}
