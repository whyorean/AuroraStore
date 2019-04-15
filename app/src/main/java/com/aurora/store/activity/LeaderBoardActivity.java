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

package com.aurora.store.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.EndlessScrollListener;
import com.aurora.store.R;
import com.aurora.store.adapter.EndlessAppsAdapter;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.aurora.store.task.CategoryAppsTask;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.CategoryAppsIterator;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class LeaderBoardActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.category_recycler)
    RecyclerView recyclerView;

    private ThemeUtil themeUtil = new ThemeUtil();
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private CustomAppListIterator iterator;
    private EndlessAppsAdapter endlessAppsAdapter;

    private String category;
    private String subCategory;
    private String title = Constants.TAG;

    public CustomAppListIterator getIterator() {
        return iterator;
    }

    public void setIterator(CustomAppListIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeUtil.onCreate(this);
        setContentView(R.layout.activity_categories);
        ButterKnife.bind(this);

        Intent mIntent = getIntent();
        if (mIntent != null && mIntent.getExtras() != null) {
            category = mIntent.getExtras().getString("INTENT_CATEGORY");
            subCategory = mIntent.getExtras().getString("INTENT_SUBCATEGORY");
            title = mIntent.getExtras().getString("INTENT_TITLE");
        }

        setupActionbar();
        setIterator(setupIterator(category, Util.getSubCategory(subCategory)));
        setupListView();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_download:
                startActivity(new Intent(this, DownloadsActivity.class));
                return true;
            case R.id.action_account:
                startActivity(new Intent(this, AccountsActivity.class));
                return true;
            case R.id.action_setting:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtil.onResume(this);
        if (endlessAppsAdapter == null || endlessAppsAdapter.isDataEmpty())
            fetchCategoryApps(false);
    }

    private void setupActionbar() {
        setSupportActionBar(toolbar);
        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setTitle(title);
            mActionBar.setDisplayShowCustomEnabled(true);
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setElevation(0f);
        }
    }

    public CustomAppListIterator setupIterator(String category, GooglePlayAPI.SUBCATEGORY subcategory) {
        try {
            final GooglePlayAPI api = new PlayStoreApiAuthenticator(this).getApi();
            return new CustomAppListIterator(new CategoryAppsIterator(api, category, subcategory));
        } catch (Exception err) {
            return null;
        }
    }

    private void setupListView() {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        endlessAppsAdapter = new EndlessAppsAdapter(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.anim_falldown));
        recyclerView.setAdapter(endlessAppsAdapter);
        EndlessScrollListener mEndlessRecyclerViewScrollListener = new EndlessScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchCategoryApps(true);
            }
        };
        recyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);
    }

    public void fetchCategoryApps(boolean shouldIterate) {
        mDisposable.add(Observable.fromCallable(() -> new CategoryAppsTask(this).getApps(getIterator()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appList -> {
                    if (shouldIterate) {
                        addApps(appList);
                    } else
                        endlessAppsAdapter.addData(appList);
                }, err -> Log.e(err.getMessage())));
    }

    private void addApps(List<App> appsToAdd) {
        if (!appsToAdd.isEmpty()) {
            for (App app : appsToAdd)
                endlessAppsAdapter.add(app);
            endlessAppsAdapter.notifyItemInserted(endlessAppsAdapter.getItemCount() - 1);
        }
        if (iterator.hasNext() && endlessAppsAdapter.getItemCount() < 10) {
            iterator.next();
        }
    }
}
