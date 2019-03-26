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

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.adapter.CustomViewPagerAdapter;
import com.aurora.store.fragment.HomeFragment;
import com.aurora.store.fragment.InstalledFragment;
import com.aurora.store.fragment.SearchFragment;
import com.aurora.store.fragment.UpdatesFragment;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.view.CustomViewPager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AuroraActivity extends AppCompatActivity {


    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.viewpager)
    CustomViewPager mViewPager;
    @BindView(R.id.bottom_navigation)
    BottomNavigationView mBottomNavigationView;

    private ActionBar mActionBar;
    private CustomViewPagerAdapter mViewPagerAdapter;
    private ThemeUtil mThemeUtil = new ThemeUtil();
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private int fragmentPos = 0;
    private int fragmentCur = 0;
    private boolean isSearchIntent = false;

    public BottomNavigationView getBottomNavigation() {
        return mBottomNavigationView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeUtil.onCreate(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
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
            fragmentPos = mBundle.getInt(Constants.INTENT_FRAGMENT_POSITION);
        if (intent.getScheme() != null && intent.getScheme().equals("market")) {
            fragmentCur = 3;
            isSearchIntent = true;
        }
    }

    @Override
    public void onBackPressed() {
        Fragment mFragment = mViewPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
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
        mDisposable.clear();
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
        if (mViewPagerAdapter == null)
            init();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return super.onSupportNavigateUp();
    }

    private void setupActionbar() {
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowCustomEnabled(true);
            mActionBar.setElevation(0f);
            mActionBar.setTitle(getString(R.string.app_name));
        }
    }

    private void setupViewPager() {
        mViewPagerAdapter = new CustomViewPagerAdapter(getSupportFragmentManager());
        mDisposable.add(Observable.just(
                new HomeFragment(),
                new InstalledFragment(),
                new UpdatesFragment(),
                new SearchFragment())
                .zipWith(Observable.interval(16, TimeUnit.MILLISECONDS), (fragment, interval) -> fragment)
                .doOnNext(fragment -> mViewPagerAdapter.addFragment(fragmentPos++, fragment))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    mViewPager.setAdapter(mViewPagerAdapter);
                    mViewPager.setPagingEnabled(false);
                    mViewPager.setOffscreenPageLimit(2);
                    mViewPager.setCurrentItem(fragmentCur, true);
                })
                .subscribe());
    }

    private void setupBottomNavigation() {
        mBottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            mViewPager.setCurrentItem(menuItem.getOrder(), true);
            switch (menuItem.getItemId()) {
                case R.id.action_home:
                    mActionBar.setTitle(getString(R.string.title_home));
                    break;
                case R.id.action_installed:
                    mActionBar.setTitle(getString(R.string.title_installed));
                    break;
                case R.id.action_updates:
                    mActionBar.setTitle(getString(R.string.title_updates));
                    break;
                case R.id.action_search:
                    mActionBar.setTitle(getString(R.string.title_search));
                    break;
            }
            return true;
        });
        if (isSearchIntent)
            mBottomNavigationView.setSelectedItemId(R.id.action_search);
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
