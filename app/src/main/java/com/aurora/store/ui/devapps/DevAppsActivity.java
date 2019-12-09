package com.aurora.store.ui.devapps;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.EndlessScrollListener;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.section.SearchResultSection;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.util.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class DevAppsActivity extends BaseActivity implements SearchResultSection.ClickListener {

    private static final String TAG_DEV_APPS = "TAG_DEV_APPS";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    private DevAppsModel model;
    private SearchResultSection section;
    private SectionedRecyclerViewAdapter adapter;

    private String query;
    private String title;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_apps);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (intent != null) {
            query = intent.getStringExtra("SearchQuery");
            title = intent.getStringExtra("SearchTitle");

            if (query != null) {
                setupActionBar();
                setupRecycler();
                model = ViewModelProviders.of(this).get(DevAppsModel.class);
                model.getQueriedApps().observe(this, appList -> {
                    dispatchAppsToAdapter(appList);
                });
                model.fetchQueriedApps(query, false);
            } else {
                Toast.makeText(this, "No dev name received", Toast.LENGTH_SHORT).show();
                supportFinishAfterTransition();
            }
        }
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

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(title);
        }
    }

    private void dispatchAppsToAdapter(List<App> newList) {
        List<App> oldList = section.getList();
        if (oldList.isEmpty()) {
            section.updateList(newList);
            adapter.notifyDataSetChanged();
        } else {
            if (!newList.isEmpty()) {
                for (App app : newList)
                    section.add(app);
                adapter.notifyItemInserted(section.getCount() - 1);
            }
        }
    }

    private void setupRecycler() {
        section = new SearchResultSection(this, this);
        adapter = new SectionedRecyclerViewAdapter();
        adapter.addSection(TAG_DEV_APPS, section);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        EndlessScrollListener endlessScrollListener = new EndlessScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                model.fetchQueriedApps(query, true);
            }
        };
        recyclerView.addOnScrollListener(endlessScrollListener);
        recyclerView.setLayoutManager(layoutManager);
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
