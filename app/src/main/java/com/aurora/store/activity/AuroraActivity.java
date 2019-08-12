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

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.adapter.ViewPagerAdapter;
import com.aurora.store.fragment.AppsFragment;
import com.aurora.store.fragment.HomeFragment;
import com.aurora.store.fragment.SearchFragment;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.CustomViewPager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;

public class AuroraActivity extends AppCompatActivity {


    public static String externalQuery;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.viewpager)
    CustomViewPager viewPager;
    @BindView(R.id.bottom_navigation)
    BottomNavigationView bottomNavigationView;
    private ActionBar actionBar;
    private ViewPagerAdapter pagerAdapter;
    private ThemeUtil themeUtil = new ThemeUtil();
    private CompositeDisposable disposable = new CompositeDisposable();
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
            PrefUtil.putBoolean(this, Constants.PREFERENCE_DO_NOT_SHOW_INTRO, true);
            startActivity(new Intent(this, IntroActivity.class));
            finish();
        } else {
            if (Accountant.isLoggedIn(this))
                init();
            else
                startActivity(new Intent(this, AccountsActivity.class));
        }

        if (Util.isCacheObsolete(this))
            Util.clearCache(this);

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
            fragmentCur = 2;
            isSearchIntent = true;
            if (intent.getData() != null)
                externalQuery = intent.getData().getQueryParameter("q");
        } else
            fragmentCur = Util.getDefaultTab(this);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = pagerAdapter.getItem(viewPager.getCurrentItem());
        if (fragment.isDetached())
            super.onBackPressed();
        if (fragment instanceof SearchFragment || fragment instanceof HomeFragment) {
            FragmentManager fragmentManager = fragment.getChildFragmentManager();
            if (!fragmentManager.getFragments().isEmpty())
                fragmentManager.popBackStack();
            else
                super.onBackPressed();
        } else
            super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return super.onSupportNavigateUp();
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
        if (pagerAdapter == null)
            init();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        Util.toggleSoftInput(this, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
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
        pagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(0, new HomeFragment());
        pagerAdapter.addFragment(1, new AppsFragment());
        pagerAdapter.addFragment(2, new SearchFragment());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setScroll(false);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setCurrentItem(fragmentCur, true);
    }

    private void setupBottomNavigation() {
        @ColorInt
        int backGroundColor = ViewUtil.getStyledAttribute(this, android.R.attr.colorBackground);
        bottomNavigationView.setBackgroundColor(ColorUtils.setAlphaComponent(backGroundColor, 245));
        bottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            viewPager.setCurrentItem(menuItem.getOrder(), true);
            switch (menuItem.getItemId()) {
                case R.id.action_home:
                    Util.toggleSoftInput(this, false);
                    actionBar.setTitle(getString(R.string.title_home));
                    break;
                case R.id.action_apps:
                    Util.toggleSoftInput(this, false);
                    actionBar.setTitle(getString(R.string.title_installed));
                    break;
                case R.id.action_search:
                    if (Util.isIMEEnabled(this))
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
