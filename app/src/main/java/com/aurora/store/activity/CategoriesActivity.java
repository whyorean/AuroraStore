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
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.adapter.CategoriesListAdapter;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.task.CategoryList;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.CustomGridLayoutManager;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class CategoriesActivity extends AppCompatActivity {

    public static final String APPS = "APPLICATION";
    public static final String GAME = "GAME";
    public static final String FAMILY = "FAMILY";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.category_recycler)
    RecyclerView mRecyclerView;

    private ThemeUtil mThemeUtil = new ThemeUtil();
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private CategoryManager categoryManager;
    private String categoryType = APPS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeUtil.onCreate(this);
        setContentView(R.layout.activity_categories);
        ButterKnife.bind(this);
        categoryManager = new CategoryManager(this);

        Intent mIntent = getIntent();
        if (mIntent != null && mIntent.getExtras() != null)
            categoryType = mIntent.getExtras().getString("INTENT_CATEGORY");

        setupActionbar();
        if (categoryManager.categoryListEmpty())
            getCategoriesFromAPI();
        else
            setupAllCategories();
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
        mThemeUtil.onResume(this);
    }

    private void setupActionbar() {
        setSupportActionBar(mToolbar);
        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowCustomEnabled(true);
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setElevation(0f);
        }
    }

    private void setupAllCategories() {
        mRecyclerView.setLayoutManager(new CustomGridLayoutManager(this,
                ViewUtil.pxToDp(this, Resources.getSystem().getDisplayMetrics().widthPixels) / 3));
        mRecyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.anim_falldown));
        mRecyclerView.setAdapter(new CategoriesListAdapter(this, getCategories()));
    }

    private Map<String, String> getCategories() {
        switch (categoryType) {
            case APPS:
                return categoryManager.getAllCategories();
            case GAME:
                return categoryManager.getAllGames();
            case FAMILY:
                return categoryManager.getAllFamily();
            default:
                return categoryManager.getAllCategories();
        }
    }

    private void getCategoriesFromAPI() {
        mDisposable.add(Observable.fromCallable(() -> new CategoryList(this).getResult())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    if (success) {
                        Log.i("CategoryList fetch completed");
                        ContextUtil.runOnUiThread(() -> {
                            setupAllCategories();
                        });
                    }
                }, err -> Log.e(err.getMessage())));
    }
}
