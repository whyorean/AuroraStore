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

package com.aurora.store.ui.main;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.aurora.store.BuildConfig;
import com.aurora.store.Constants;
import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.Update;
import com.aurora.store.service.SelfUpdateService;
import com.aurora.store.task.NetworkTask;
import com.aurora.store.ui.accounts.AccountsActivity;
import com.aurora.store.ui.installed.InstalledAppActivity;
import com.aurora.store.ui.preference.SettingsActivity;
import com.aurora.store.ui.search.activity.SearchActivity;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.ui.single.activity.DownloadsActivity;
import com.aurora.store.ui.single.activity.GenericActivity;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.CertUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.NetworkUtil;
import com.aurora.store.util.TextUtil;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AuroraActivity extends BaseActivity {

    public static String externalQuery;

    @BindView(R.id.bottom_navigation)
    BottomNavigationView bottomNavigationView;
    @BindView(R.id.navigation)
    NavigationView navigation;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.action1)
    AppCompatImageView action1;
    @BindView(R.id.search_bar)
    RelativeLayout searchBar;

    private CompositeDisposable disposable = new CompositeDisposable();
    private int fragmentCur = 0;

    static boolean matchDestination(@NonNull NavDestination destination, @IdRes int destId) {
        NavDestination currentDestination = destination;
        while (currentDestination.getId() != destId && currentDestination.getParent() != null) {
            currentDestination = currentDestination.getParent();
        }
        return currentDestination.getId() == destId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        fragmentCur = Util.getDefaultTab(this);
        onNewIntent(getIntent());

        if (Accountant.isLoggedIn(this))
            populateData();
        else
            startActivity(new Intent(this, AccountsActivity.class));

        if (NetworkUtil.isConnected(this)) {
            if (Util.isCacheObsolete(this))
                Util.clearCache(this);

            if (Util.shouldCheckUpdate(this) && !SelfUpdateService.isServiceRunning())
                checkSelfUpdate();
        }
        checkPermissions();
    }

    private void populateData() {
        setupNavigation();
        setupDrawer();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle bundle = intent.getExtras();
        if (bundle != null)
            fragmentCur = bundle.getInt(Constants.INTENT_FRAGMENT_POSITION);
        else if (intent.getScheme() != null && intent.getScheme().equals("market")) {
            fragmentCur = 2;
            if (intent.getData() != null)
                externalQuery = intent.getData().getQueryParameter("q");
        } else
            fragmentCur = Util.getDefaultTab(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.toggleSoftInput(this, false);

        //Check & start notification service
        Util.startNotificationService(this);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        Util.toggleSoftInput(this, false);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START, true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        disposable.clear();
        super.onDestroy();
    }

    @OnClick({R.id.search_bar, R.id.action2})
    public void openSearchActivity() {
        Intent intent = new Intent(this, SearchActivity.class);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
        startActivity(intent, options.toBundle());
    }

    private void setupNavigation() {
        int backGroundColor = ViewUtil.getStyledAttribute(this, android.R.attr.colorBackground);
        bottomNavigationView.setBackgroundColor(ColorUtils.setAlphaComponent(backGroundColor, 245));

        NavController navController = Navigation.findNavController(this, R.id.nav_host_main);

        //Avoid Adding same fragment to NavController, if clicked on current BottomNavigation item
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == bottomNavigationView.getSelectedItemId())
                return false;
            NavigationUI.onNavDestinationSelected(item, navController);
            return true;
        });

        //Check correct BottomNavigation item, if navigation_main is done programmatically
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            final Menu menu = bottomNavigationView.getMenu();
            final int size = menu.size();
            for (int i = 0; i < size; i++) {
                MenuItem item = menu.getItem(i);
                if (matchDestination(destination, item.getItemId())) {
                    item.setChecked(true);
                }
            }
        });

        //Check default tab to open, if configured
        switch (fragmentCur) {
            case 0:
                navController.navigate(R.id.homeFragment);
                break;
            case 1:
                navController.navigate(R.id.updatesFragment);
                break;
            case 2:
                navController.navigate(R.id.categoriesFragment);
                break;
        }
    }

    private void setupDrawer() {
        action1.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.openDrawer(GravityCompat.START, true);
        });

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                ImageView backgroundView = drawerView.findViewById(R.id.background);
                ImageView imageView = drawerView.findViewById(R.id.img);
                TextView textView1 = drawerView.findViewById(R.id.line1);
                TextView textView2 = drawerView.findViewById(R.id.line2);

                backgroundView.setColorFilter(ContextCompat.getColor(
                        AuroraActivity.this,
                        R.color.colorScrimBlack),
                        PorterDuff.Mode.SRC_OVER
                );

                GlideApp
                        .with(AuroraActivity.this)
                        .load(Accountant.getImageURL(AuroraActivity.this))
                        .placeholder(R.drawable.circle_bg)
                        .circleCrop()
                        .into(imageView);

                GlideApp
                        .with(AuroraActivity.this)
                        .load(Accountant.getBackgroundImageURL(AuroraActivity.this))
                        .into(backgroundView);

                textView1.setText(Accountant.isAnonymous(AuroraActivity.this)
                        ? getText(R.string.account_dummy)
                        : Accountant.getUserName(AuroraActivity.this));
                textView2.setText(Accountant.isAnonymous(AuroraActivity.this)
                        ? "auroraoss@gmail.com"
                        : Accountant.getEmail(AuroraActivity.this));
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        navigation.setNavigationItemSelectedListener(item -> {
            Intent intent = new Intent(this, GenericActivity.class);
            switch (item.getItemId()) {
                case R.id.action_accounts:
                    startActivity(new Intent(this, AccountsActivity.class),
                            ViewUtil.getEmptyActivityBundle(this));
                    break;
                case R.id.action_all_apps:
                    startActivity(new Intent(this, InstalledAppActivity.class),
                            ViewUtil.getEmptyActivityBundle(this));
                    break;
                case R.id.action_download:
                    startActivity(new Intent(this, DownloadsActivity.class),
                            ViewUtil.getEmptyActivityBundle(this));
                    break;
                case R.id.action_setting:
                    startActivity(new Intent(this, SettingsActivity.class),
                            ViewUtil.getEmptyActivityBundle(this));
                    break;
                case R.id.action_about:
                    intent.putExtra(Constants.FRAGMENT_NAME, Constants.FRAGMENT_ABOUT);
                    startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
                    break;
                case R.id.action_favourite:
                    intent.putExtra(Constants.FRAGMENT_NAME, Constants.FRAGMENT_FAV_LIST);
                    startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
                    break;
                case R.id.action_blacklist:
                    intent.putExtra(Constants.FRAGMENT_NAME, Constants.FRAGMENT_BLACKLIST);
                    startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
                    break;
                case R.id.action_spoof:
                    intent.putExtra(Constants.FRAGMENT_NAME, Constants.FRAGMENT_SPOOF);
                    startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
                    break;
            }
            return false;
        });
    }

    private void checkSelfUpdate() {
        Log.d("Checking updates for Aurora Store");
        disposable.add(Observable.fromCallable(() -> new NetworkTask(this)
                .get(Constants.UPDATE_URL))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    try {
                        Util.setSelfUpdateTime(this, Calendar.getInstance().getTimeInMillis());
                        Update update = gson.fromJson(response, Update.class);

                        if (update.getVersionCode() > BuildConfig.VERSION_CODE) {
                            if (CertUtil.isFDroidApp(this, BuildConfig.APPLICATION_ID)
                                    && TextUtil.emptyIfNull(update.getFdroidBuild()).isEmpty()) {
                                Log.d("FDroid build of latest version is not published yet");
                                return;
                            }

                            if (!update.getAuroraBuild().isEmpty())
                                showUpdatesDialog(update);
                            else
                                Log.d("No new update available");

                        }
                    } catch (Exception e) {
                        Log.e("Error checking updates");
                    }
                }));
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    1337);
        }
    }

    protected void showUpdatesDialog(Update update) {
        final String changelog = TextUtil.emptyIfNull(update.getChangelog());
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_title_self_update))
                .setMessage(new StringBuilder()
                        .append(update.getVersionName())
                        .append("\n\n")
                        .append(changelog.isEmpty() ? getString(R.string.details_no_changes) : changelog)
                        .append("\n\n")
                        .append(getString(R.string.dialog_desc_self_update))
                        .toString())
                .setPositiveButton(getString(android.R.string.yes), (dialog, which) -> {
                    Intent intent = new Intent(this, SelfUpdateService.class);
                    startService(intent);
                })
                .setNegativeButton(getString(android.R.string.no), (dialog, which) -> {
                    dialog.dismiss();
                });
        int backGroundColor = ViewUtil.getStyledAttribute(this, android.R.attr.colorBackground);
        builder.setBackground(new ColorDrawable(backGroundColor));
        builder.create();
        builder.show();
    }
}
