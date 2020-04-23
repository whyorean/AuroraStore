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

package com.aurora.store.ui.details;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.OneTimeWorkRequest;

import com.aurora.store.AuroraApplication;
import com.aurora.store.AutoDisposable;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.manager.FavouritesManager;
import com.aurora.store.model.App;
import com.aurora.store.ui.details.views.AbstractDetails;
import com.aurora.store.ui.details.views.ActionButton;
import com.aurora.store.ui.details.views.AppLinks;
import com.aurora.store.ui.details.views.Beta;
import com.aurora.store.ui.details.views.ExodusPrivacy;
import com.aurora.store.ui.details.views.GeneralDetails;
import com.aurora.store.ui.details.views.Reviews;
import com.aurora.store.ui.details.views.Screenshot;
import com.aurora.store.ui.details.views.Video;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.ui.single.activity.DownloadsActivity;
import com.aurora.store.ui.single.activity.ManualDownloadActivity;
import com.aurora.store.ui.view.ViewFlipper2;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.ViewUtil;
import com.aurora.store.util.WorkerUtil;
import com.aurora.store.util.diff.NavigationUtil;
import com.aurora.store.worker.ApiValidator;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DetailsActivity extends BaseActivity {

    private static final String PLAY_STORE_PACKAGE_NAME = "com.android.vending";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.view_flipper)
    ViewFlipper2 viewFlipper;

    @BindView(R.id.icon)
    AppCompatImageView icon;
    @BindView(R.id.displayName)
    AppCompatTextView txtDisplayName;
    @BindView(R.id.devName)
    AppCompatTextView txtDevName;
    @BindView(R.id.packageName)
    AppCompatTextView txtPackageName;
    @BindView(R.id.no_app_img)

    AppCompatImageView noAppImg;
    @BindView(R.id.no_app_line1)
    AppCompatTextView noAppLine1;
    @BindView(R.id.no_app_line2)
    AppCompatTextView noAppLine2;
    @BindView(R.id.no_app_layout)
    RelativeLayout noAppLayout;

    private ActionButton actionButton;

    private String packageName;
    private App app;

    private FavouritesManager favouritesManager;
    private DetailsAppModel model;

    private AutoDisposable autoDisposable = new AutoDisposable();

    private BroadcastReceiver globalInstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getData() == null || !TextUtils.equals(packageName, intent.getData().getSchemeSpecificPart())) {
                return;
            }

            if (app != null)
                ContextUtil.runOnUiThread(() -> draw(app));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        ButterKnife.bind(this);
        setupActionBar();

        autoDisposable.bindTo(getLifecycle());
        favouritesManager = new FavouritesManager(this);

        model = new ViewModelProvider(this).get(DetailsAppModel.class);
        model.getAppDetails().observe(this, this::draw);

        model.getError().observe(this, errorType -> {
            switch (errorType) {
                case NO_API:
                case SESSION_EXPIRED:
                    buildAndTestApi();
                    break;
                case NO_NETWORK:
                    showSnackBar(coordinator, R.string.error_no_network, -2, v -> {
                        model.fetchAppDetails(packageName);
                    });
                    break;
                case APP_NOT_FOUND:
                    if (app != null) {
                        drawMinimalDetails(app);
                    } else {
                        ContextUtil.toastLong(this, getString(R.string.error_app_not_found));
                        finish();
                    }
                    break;
            }
        });

        registerReceiver(globalInstallReceiver, PackageUtil.getFilter());

        Disposable disposable = AuroraApplication
                .getRxBus()
                .getBus()
                .subscribe(event -> {
                    switch (event.getSubType()) {
                        case INSTALLED:
                        case UNINSTALLED:
                            if (app != null)
                                draw(app);
                            break;
                        case API_SUCCESS:
                            model.fetchAppDetails(packageName);
                            break;
                    }
                });
        autoDisposable.add(disposable);
        onNewIntent(getIntent());
    }

    private void buildAndTestApi() {
        final OneTimeWorkRequest workRequest = WorkerUtil.getWorkRequest(ApiValidator.TAG,
                WorkerUtil.getNetworkConstraints(),
                ApiValidator.class);

        WorkerUtil.enqueue(this, this, workRequest, workInfo -> {
            switch (workInfo.getState()) {
                case FAILED:
                    Toast.makeText(this, "You were logged out!", Toast.LENGTH_SHORT).show();
                    NavigationUtil.launchAccountsActivity(this);
                    finish();
                    break;

                case SUCCEEDED:
                    model.fetchAppDetails(packageName);
                    break;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        packageName = getIntentPackageName(intent);
        if (TextUtils.isEmpty(packageName)) {
            Log.d("No package name provided");
            finishAfterTransition();
        } else {
            stringExtra = intent.getStringExtra(Constants.STRING_EXTRA);
            if (stringExtra != null) {
                app = gson.fromJson(stringExtra, App.class);
            }
            Log.i("Getting info about %s", packageName);
            model.fetchAppDetails(packageName);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_details, menu);
        menu.findItem(R.id.action_favourite).setIcon(favouritesManager.isFavourite(packageName)
                ? R.drawable.ic_favourite_red
                : R.drawable.ic_favourite);
        MenuItem blackList = menu.findItem(R.id.action_blacklist);
        if (!PackageUtil.isInstalled(this, packageName))
            blackList.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_favourite:
                if (favouritesManager.isFavourite(packageName)) {
                    favouritesManager.removeFromFavourites(packageName);
                    menuItem.setIcon(R.drawable.ic_favourite);
                } else {
                    favouritesManager.addToFavourites(packageName);
                    menuItem.setIcon(R.drawable.ic_favourite_red);
                }
                return true;
            case R.id.action_manual:
                Intent manualIntent = new Intent(this, ManualDownloadActivity.class);
                manualIntent.putExtra(Constants.STRING_EXTRA, gson.toJson(app));
                startActivity(manualIntent, ViewUtil.getEmptyActivityBundle(this));
                return true;
            case R.id.action_downloads:
                startActivity(new Intent(this, DownloadsActivity.class));
                return true;
            case R.id.action_blacklist:
                new BlacklistManager(this).addToBlacklist(packageName);
                return true;
            case R.id.action_share:
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, app.getDisplayName());
                i.putExtra(Intent.EXTRA_TEXT, Constants.APP_DETAIL_URL + app.getPackageName());
                startActivity(Intent.createChooser(i, getString(R.string.details_share)));
                return true;
            case R.id.action_playstore:
                if (!isPlayStoreInstalled() || !app.isInPlayStore()) {
                    return false;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(Constants.APP_DETAIL_URL + app.getPackageName()));
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onDestroy() {
        try {
            app = null;
            unregisterReceiver(globalInstallReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private String getIntentPackageName(Intent intent) {
        if (intent.hasExtra(Constants.INTENT_PACKAGE_NAME)) {
            return intent.getStringExtra(Constants.INTENT_PACKAGE_NAME);
        } else if (intent.getScheme() != null
                && (intent.getScheme().equals("market")
                || intent.getScheme().equals("http")
                || intent.getScheme().equals("https"))) {
            return intent.getData().getQueryParameter("id");
        } else if (intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            return bundle.getString(Constants.INTENT_PACKAGE_NAME);
        }
        return null;
    }

    private void drawMinimalDetails(App app) {
        ContextUtil.runOnUiThread(() -> {
            try {
                final PackageManager packageManager = getPackageManager();
                final Drawable drawable = packageManager.getApplicationIcon(app.getPackageName());
                noAppImg.setImageDrawable(drawable);
            } catch (PackageManager.NameNotFoundException e) {
                noAppImg.setImageDrawable(getDrawable(R.drawable.ic_placeholder));
            }

            noAppLine1.setText(app.getDisplayName());
            noAppLine2.setText(app.getPackageName());
            noAppLayout.setVisibility(View.VISIBLE);
        });
    }

    private void draw(App appFromMarket) {
        if (appFromMarket != null) {
            app = appFromMarket;
            Disposable disposable = Observable.just(
                    new GeneralDetails(this, app),
                    new Screenshot(this, app),
                    new Reviews(this, app),
                    new ExodusPrivacy(this, app),
                    new Video(this, app),
                    new Beta(this, app),
                    new AppLinks(this, app),
                    new ActionButton(this, app))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(AbstractDetails::draw)
                    .subscribe();
            autoDisposable.add(disposable);
            viewFlipper.switchState(ViewFlipper2.DATA);
        } else {
            viewFlipper.switchState(ViewFlipper2.EMPTY);
        }
    }

    protected boolean isPlayStoreInstalled() {
        return PackageUtil.isInstalled(this, PLAY_STORE_PACKAGE_NAME);
    }
}
