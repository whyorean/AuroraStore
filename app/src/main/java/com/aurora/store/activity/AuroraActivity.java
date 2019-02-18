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

import com.aurora.store.utility.Accountant;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.adapter.CustomViewPagerAdapter;
import com.aurora.store.fragment.HomeFragment;
import com.aurora.store.fragment.InstalledFragment;
import com.aurora.store.fragment.SearchFragment;
import com.aurora.store.fragment.UpdatesFragment;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.view.CustomViewPager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;

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
        }
    }

    private void setupViewPager() {
        mViewPagerAdapter = new CustomViewPagerAdapter(getSupportFragmentManager());
        mViewPagerAdapter.addFragment(0, new HomeFragment());
        mViewPagerAdapter.addFragment(1, new InstalledFragment());
        mViewPagerAdapter.addFragment(2, new UpdatesFragment());
        mViewPagerAdapter.addFragment(3, new SearchFragment());

        mViewPager.setPagingEnabled(false);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.setCurrentItem(fragmentPos, true);
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
