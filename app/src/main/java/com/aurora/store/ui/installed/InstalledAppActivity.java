package com.aurora.store.ui.installed;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.util.diff.InstalledDiffCallback;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.model.items.InstalledItem;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.ui.view.CustomSwipeToRefresh;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class InstalledAppActivity extends BaseActivity {

    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.switch_system)
    SwitchMaterial switchSystem;
    @BindView(R.id.swipe_layout)
    CustomSwipeToRefresh swipeToRefresh;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    private InstalledAppsModel model;
    private FastAdapter<InstalledItem> fastAdapter;
    private ItemAdapter<InstalledItem> itemAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_apps);
        ButterKnife.bind(this);

        setupRecycler();

        switchSystem.setChecked(PrefUtil.getBoolean(this, Constants.PREFERENCE_INCLUDE_SYSTEM));
        switchSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PrefUtil.putBoolean(this, Constants.PREFERENCE_INCLUDE_SYSTEM, isChecked);
        });

        model = new ViewModelProvider(this).get(InstalledAppsModel.class);
        model.getData().observe(this, appList -> {
            dispatchAppsToAdapter(appList);
            swipeToRefresh.setRefreshing(false);
        });

        model.getError().observe(this, errorType -> {
            switch (errorType) {
                case NO_API:
                case SESSION_EXPIRED:
                    Util.validateApi(this);
                    break;
                case NO_NETWORK: {
                    showSnackBar(coordinator, R.string.error_no_network, v -> fetchApps());
                    break;
                }
            }
        });

        swipeToRefresh.setRefreshing(true);
        swipeToRefresh.setOnRefreshListener(this::fetchApps);

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
                            fetchApps();
                            break;
                    }
                })
                .subscribe();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void fetchApps() {
        model.fetchInstalledApps(switchSystem.isChecked());
    }

    private void dispatchAppsToAdapter(List<InstalledItem> installedItemList) {
        final FastAdapterDiffUtil fastAdapterDiffUtil = FastAdapterDiffUtil.INSTANCE;
        final InstalledDiffCallback diffCallback = new InstalledDiffCallback();
        final DiffUtil.DiffResult diffResult = fastAdapterDiffUtil.calculateDiff(itemAdapter, installedItemList, diffCallback);
        fastAdapterDiffUtil.set(itemAdapter, diffResult);
    }

    private void setupRecycler() {
        itemAdapter = new ItemAdapter<>();
        fastAdapter = new FastAdapter<>();
        fastAdapter.addAdapter(0, itemAdapter);

        fastAdapter.setOnClickListener((view, itemIAdapter, installedItem, position) -> {
            final App app = installedItem.getApp();
            final Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra(Constants.INTENT_PACKAGE_NAME, app.getPackageName());
            intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app));
            startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
            return false;
        });

        fastAdapter.setOnLongClickListener((view, itemIAdapter, installedItem, position) -> {
            final AppMenuSheet menuSheet = new AppMenuSheet();
            final Bundle bundle = new Bundle();
            bundle.putInt(Constants.INT_EXTRA, position);
            bundle.putString(Constants.STRING_EXTRA, gson.toJson(installedItem.getApp()));
            menuSheet.setArguments(bundle);
            menuSheet.show(getSupportFragmentManager(), AppMenuSheet.TAG);
            return true;
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(fastAdapter);
    }

    @OnClick(R.id.action1)
    public void goBack() {
        onBackPressed();
    }
}
