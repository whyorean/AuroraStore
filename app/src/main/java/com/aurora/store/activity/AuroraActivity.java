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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.adapter.ViewPager2Adapter;
import com.aurora.store.fragment.HomeFragment;
import com.aurora.store.fragment.InstalledFragment;
import com.aurora.store.fragment.SearchFragment;
import com.aurora.store.fragment.UpdatesFragment;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.utility.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;

public class AuroraActivity extends AppCompatActivity {


    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.viewpager)
    ViewPager2 viewPager2;
    @BindView(R.id.bottom_navigation)
    BottomNavigationView bottomNavigationView;

    private ActionBar actionBar;
    private ViewPager2Adapter pager2Adapter;
    private ThemeUtil themeUtil = new ThemeUtil();
    private CompositeDisposable disposable = new CompositeDisposable();
    private int fragmentPos = 0;
    private int fragmentCur = 0;
    private boolean isSearchIntent = false;

    public BottomNavigationView getBottomNavigation() {
        return bottomNavigationView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeUtil.onCreate(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        fragmentCur = Util.getDefaultTab(this);
        onNewIntent(getIntent());

        if (!PrefUtil.getBoolean(this, Constants.PREFERENCE_DO_NOT_SHOW_INTRO)) {
            startActivity(new Intent(this, IntroActivity.class));
            finish();
        } else {
            if (Accountant.isLoggedIn(this))
                init();
            else
                startActivity(new Intent(this, AccountsActivity.class));
        }
        checkPermissions();
    }

    private void init() {
        setupActionbar();
        setupViewPager();
        setupBottomNavigation();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle mBundle = intent.getExtras();
        if (mBundle != null)
            fragmentCur = mBundle.getInt(Constants.INTENT_FRAGMENT_POSITION);
        if (intent.getScheme() != null && intent.getScheme().equals("market")) {
            fragmentCur = 3;
            isSearchIntent = true;
        } else
            fragmentCur = Util.getDefaultTab(this);
    }

    @Override
    public void onBackPressed() {
        Fragment mFragment = pager2Adapter.getItem(viewPager2.getCurrentItem());
        if (mFragment instanceof SearchFragment) {
            FragmentManager fm = mFragment.getChildFragmentManager();
            if (!fm.getFragments().isEmpty())
                fm.popBackStack();
            else
                super.onBackPressed();
        } else
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
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
        Util.toggleSoftInput(this, false);
        if (pager2Adapter == null)
            init();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        Util.toggleSoftInput(this, false);
    }

    private void setupActionbar() {
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setElevation(0f);
            actionBar.setTitle(getString(R.string.app_name));
        }
    }

    private void setupViewPager() {
        pager2Adapter = new ViewPager2Adapter(getSupportFragmentManager(), getLifecycle());
        pager2Adapter.addFragment(new HomeFragment());
        pager2Adapter.addFragment(new InstalledFragment());
        pager2Adapter.addFragment(new UpdatesFragment());
        pager2Adapter.addFragment(new SearchFragment());
        viewPager2.setAdapter(pager2Adapter);
        viewPager2.setUserInputEnabled(false);
        viewPager2.setCurrentItem(fragmentCur, false);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            viewPager2.setCurrentItem(menuItem.getOrder(), false);
            switch (menuItem.getItemId()) {
                case R.id.action_home:
                    Util.toggleSoftInput(this, false);
                    actionBar.setTitle(getString(R.string.title_home));
                    break;
                case R.id.action_installed:
                    Util.toggleSoftInput(this, false);
                    actionBar.setTitle(getString(R.string.title_installed));
                    break;
                case R.id.action_updates:
                    Util.toggleSoftInput(this, false);
                    actionBar.setTitle(getString(R.string.title_updates));
                    break;
                case R.id.action_search:
                    Util.toggleSoftInput(this, true);
                    actionBar.setTitle(getString(R.string.title_search));
                    break;
            }
            return true;
        });

        if (isSearchIntent)
            bottomNavigationView.setSelectedItemId(R.id.action_search);
        if (fragmentCur != 0 && !isSearchIntent)
            bottomNavigationView.setSelectedItemId(bottomNavigationView.getMenu()
                    .getItem(fragmentCur).getItemId());
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1337);
        }
    }
}
