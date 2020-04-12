package com.aurora.store.ui.devapps;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.model.items.EndlessItem;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.ui.view.ViewFlipper2;
import com.aurora.store.util.ViewUtil;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener;
import com.mikepenz.fastadapter.ui.items.ProgressItem;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DevAppsActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.viewFlipper)
    ViewFlipper2 viewFlipper;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    private DevAppsModel model;
    private FastAdapter fastAdapter;
    private ItemAdapter<EndlessItem> itemAdapter;
    private ItemAdapter<ProgressItem> progressItemAdapter;

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
                model = new ViewModelProvider(this).get(DevAppsModel.class);
                model.getQueriedApps().observe(this, this::dispatchAppsToAdapter);
                model.fetchQueriedApps(query, false);
            } else {
                Toast.makeText(this, "No dev name received", Toast.LENGTH_SHORT).show();
                supportFinishAfterTransition();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void dispatchAppsToAdapter(List<EndlessItem> endlessItemList) {
        itemAdapter.add(endlessItemList);
        recyclerView.post(() -> {
            progressItemAdapter.clear();
        });

        if (itemAdapter != null && itemAdapter.getAdapterItems().size() > 0) {
            viewFlipper.switchState(ViewFlipper2.DATA);
        } else {
            viewFlipper.switchState(ViewFlipper2.EMPTY);
        }
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

    private void setupRecycler() {
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

        recyclerView.addOnScrollListener(endlessScrollListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(fastAdapter);
    }
}
