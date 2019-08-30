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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.aurora.store.Constants;
import com.aurora.store.GlideApp;
import com.aurora.store.MenuType;
import com.aurora.store.R;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.manager.FavouriteListManager;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Status;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;

import static com.aurora.store.Constants.INTENT_PACKAGE_NAME;

public class AuroraActivity extends AppCompatActivity {


    public static String externalQuery;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.bottom_navigation)
    BottomNavigationView bottomNavigationView;

    private ActionBar actionBar;
    private ThemeUtil themeUtil = new ThemeUtil();
    private CompositeDisposable disposable = new CompositeDisposable();
    private String packageName = null;
    private FavouriteListManager favouriteListManager;
    private Fetch fetch;
    private int fragmentCur = 0;
    private boolean isSearchIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeUtil.onCreate(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        fragmentCur = Util.getDefaultTab(this);
        favouriteListManager = new FavouriteListManager(this);
        fetch = DownloadManager.getFetchInstance(this);

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
        setupNavigation();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        packageName = getIntentPackageName(intent);
        if (!TextUtils.isEmpty(packageName)) {
            Bundle bundle = new Bundle();
            bundle.putString("PACKAGE_NAME", packageName);
            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.detailsFragment, bundle);
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle != null)
            fragmentCur = bundle.getInt(Constants.INTENT_FRAGMENT_POSITION);
        if (intent.getScheme() != null && intent.getScheme().equals("market")) {
            fragmentCur = 2;
            isSearchIntent = true;
            if (intent.getData() != null)
                externalQuery = intent.getData().getQueryParameter("q");
        } else
            fragmentCur = Util.getDefaultTab(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_download:
            case R.id.action_downloads:
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.downloadsFragment);
                return true;
            case R.id.action_setting:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_favourite:
                if (favouriteListManager.contains(packageName)) {
                    favouriteListManager.remove(packageName);
                    menuItem.setIcon(R.drawable.ic_favourite_remove);
                } else {
                    favouriteListManager.add(packageName);
                    menuItem.setIcon(R.drawable.ic_favourite_red);
                }
                return true;
            case R.id.action_manual:
                startActivity(new Intent(this, ManualDownloadActivity.class));
                return true;
            case R.id.action_blacklist:
                new BlacklistManager(this).add(packageName);
                return true;
            case R.id.action_pause_all:
                fetch.pauseAll();
                return true;
            case R.id.action_resume_all:
                fetch.resumeAll();
                return true;
            case R.id.action_cancel_all:
                fetch.cancelAll();
                return true;
            case R.id.action_clear_completed:
                fetch.removeAllWithStatus(Status.COMPLETED);
                return true;
            case R.id.action_force_clear_all:
                forceClearAll();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtil.onResume(this);
        Util.toggleSoftInput(this, false);
    }

    @Override
    protected void onUserLeaveHint() {
        Util.toggleSoftInput(this, false);
        super.onUserLeaveHint();
    }

    @Override
    protected void onDestroy() {
        GlideApp.with(this).pauseAllRequests();
        disposable.clear();
        super.onDestroy();
    }

    private void forceClearAll() {
        fetch.deleteAllWithStatus(Status.ADDED);
        fetch.deleteAllWithStatus(Status.QUEUED);
        fetch.deleteAllWithStatus(Status.CANCELLED);
        fetch.deleteAllWithStatus(Status.COMPLETED);
        fetch.deleteAllWithStatus(Status.DOWNLOADING);
        fetch.deleteAllWithStatus(Status.FAILED);
        fetch.deleteAllWithStatus(Status.PAUSED);
    }

    private void redrawMenu(MenuType type) {
        Menu menu = toolbar.getMenu();
        menu.clear();
        switch (type) {
            case GLOBAL:
                getMenuInflater().inflate(R.menu.menu_main, menu);
                break;
            case DETAILS:
                getMenuInflater().inflate(R.menu.menu_app_details, menu);
                break;
            case DOWNLOADS:
                getMenuInflater().inflate(R.menu.menu_download_main, menu);
                break;
            case DEV_APPS:
                /*No Menu*/
                break;
            default:
                getMenuInflater().inflate(R.menu.menu_main, menu);
        }

        MenuItem menuItem = menu.findItem(R.id.action_favourite);
        if (menuItem != null)
            menuItem.setIcon(favouriteListManager.contains(packageName)
                    ? R.drawable.ic_favourite_red
                    : R.drawable.ic_favourite_remove);
        onPrepareOptionsMenu(menu);
    }

    private String getIntentPackageName(Intent intent) {
        if (intent.hasExtra(INTENT_PACKAGE_NAME)) {
            return intent.getStringExtra(INTENT_PACKAGE_NAME);
        } else if (intent.getScheme() != null
                && (intent.getScheme().equals("market")
                || intent.getScheme().equals("http")
                || intent.getScheme().equals("https"))) {
            return intent.getData().getQueryParameter("id");
        } else if (intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            return bundle.getString(INTENT_PACKAGE_NAME);
        }
        return null;
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

    private void setupNavigation() {
        int backGroundColor = ViewUtil.getStyledAttribute(this, android.R.attr.colorBackground);
        bottomNavigationView.setBackgroundColor(ColorUtils.setAlphaComponent(backGroundColor, 245));
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        if (isSearchIntent)
            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.searchFragment);

        switch (fragmentCur) {
            case 0:
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.homeFragment);
                break;
            case 1:
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.appsFragment);
                break;
            case 2:
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.searchFragment);
                break;
        }

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            switch (destination.getId()) {
                case R.id.detailsFragment:
                    ViewUtil.hideBottomNav(bottomNavigationView, true);
                    actionBar.setTitle("");
                    redrawMenu(MenuType.DETAILS);
                    break;
                case R.id.downloadsFragment:
                    redrawMenu(MenuType.DOWNLOADS);
                    break;
                case R.id.devFragment:
                    if (DetailsFragment.app != null)
                        actionBar.setTitle(DetailsFragment.app.getDeveloperName());
                    redrawMenu(MenuType.DEV_APPS);
                    break;
                default:
                    ViewUtil.showBottomNav(bottomNavigationView, true);
                    redrawMenu(MenuType.GLOBAL);
                    break;

            }
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
