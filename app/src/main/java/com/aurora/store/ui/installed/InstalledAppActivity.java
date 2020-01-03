package com.aurora.store.ui.installed;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.AppDiffCallback;
import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.section.InstallAppSection;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.ui.view.CustomSwipeToRefresh;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.reactivex.disposables.CompositeDisposable;

public class InstalledAppActivity extends BaseActivity implements InstallAppSection.ClickListener {

    private static final String TAG_APPS = "TAG_APPS";

    @BindView(R.id.switch_system)
    SwitchMaterial switchSystem;
    @BindView(R.id.recycler)
    RecyclerView recycler;
    @BindView(R.id.swipe_layout)
    CustomSwipeToRefresh swipeLayout;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;

    private InstalledAppsModel model;
    private InstallAppSection section;
    private SectionedRecyclerViewAdapter adapter;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_apps);
        ButterKnife.bind(this);

        setupRecycler();
        switchSystem.setChecked(PrefUtil.getBoolean(this, Constants.PREFERENCE_INCLUDE_SYSTEM));
        switchSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PrefUtil.putBoolean(this, Constants.PREFERENCE_INCLUDE_SYSTEM, isChecked);
            model.fetchInstalledApps(isChecked);
        });

        model = ViewModelProviders.of(this).get(InstalledAppsModel.class);
        model.getListMutableLiveData().observe(this, appList -> {
            dispatchAppsToAdapter(appList);
            swipeLayout.setRefreshing(false);

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

        model.fetchInstalledApps(switchSystem.isChecked());
        swipeLayout.setOnRefreshListener(() -> fetchApps());

        disposable.add(AuroraApplication
                .getRxBus()
                .getBus()
                .subscribe(event -> {
                    switch (event.getSubType()) {
                        case BLACKLIST:
                            int pos = section.removeApp(event.getPackageName());
                            if (pos != -1)
                                adapter.notifyItemRemoved(pos);
                            break;
                        case API_SUCCESS:
                            fetchApps();
                            break;
                    }
                }));
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void fetchApps() {
        model.fetchInstalledApps(switchSystem.isChecked());
    }

    private void dispatchAppsToAdapter(List<App> newList) {
        List<App> oldList = section.getList();
        if (oldList.isEmpty()) {
            section.updateList(newList);
            adapter.notifyDataSetChanged();
        } else {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AppDiffCallback(newList, oldList));
            diffResult.dispatchUpdatesTo(adapter);
            section.updateList(newList);
        }
    }

    private void setupRecycler() {
        section = new InstallAppSection(this, this);
        adapter = new SectionedRecyclerViewAdapter();
        adapter.addSection(TAG_APPS, section);
        recycler.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recycler.setAdapter(adapter);
    }

    @OnClick(R.id.action1)
    public void goBack() {
        onBackPressed();
    }

    @Override
    public void onClick(App app) {
        DetailsActivity.app = app;
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
}
