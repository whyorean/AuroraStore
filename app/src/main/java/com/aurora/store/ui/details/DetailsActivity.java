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
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProviders;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.manager.FavouriteListManager;
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
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.Util;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class DetailsActivity extends BaseActivity {

    public static App app;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.icon)
    AppCompatImageView icon;
    @BindView(R.id.displayName)
    AppCompatTextView txtDisplayName;
    @BindView(R.id.devName)
    AppCompatTextView txtDevName;
    @BindView(R.id.packageName)
    AppCompatTextView txtPackageName;

    private ActionButton actionButton;
    private String packageName;
    private FavouriteListManager favouriteListManager;
    private DetailsAppModel model;

    private CompositeDisposable disposable = new CompositeDisposable();

    private BroadcastReceiver globalInstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getData() == null || !TextUtils.equals(packageName, intent.getData().getSchemeSpecificPart())) {
                return;
            }
            ContextUtil.runOnUiThread(() -> drawButtons());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        ButterKnife.bind(this);
        setupActionBar();

        favouriteListManager = new FavouriteListManager(this);

        model = ViewModelProviders.of(this).get(DetailsAppModel.class);
        model.getAppDetails().observe(this, detailApp -> {
            draw(detailApp);
        });

        model.getError().observe(this, errorType -> {
            switch (errorType) {
                case NO_API:
                case SESSION_EXPIRED:
                    Util.validateApi(this);
                    break;
                case NO_NETWORK: {
                    showSnackBar(coordinator, R.string.error_no_network, -2, v -> {
                        model.fetchAppDetails(packageName);
                    });
                    break;
                }
            }
        });

        registerReceiver(globalInstallReceiver, PackageUtil.getFilter());
        disposable.add(AuroraApplication
                .getRxBus()
                .getBus()
                .subscribe(event -> {
                    switch (event.getSubType()) {
                        case INSTALLED:
                        case UNINSTALLED:
                            drawButtons();
                            break;
                        case API_SUCCESS:
                            model.fetchAppDetails(packageName);
                            break;
                    }
                }));

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        packageName = getIntentPackageName(intent);
        if (TextUtils.isEmpty(packageName)) {
            Log.d("No package name provided");
            finish();
            return;
        }
        Log.i("Getting info about %s", packageName);
        model.fetchAppDetails(packageName);
        if (app != null)
            drawBasic();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_details, menu);
        menu.findItem(R.id.action_favourite).setIcon(favouriteListManager.contains(packageName)
                ? R.drawable.ic_favourite_red
                : R.drawable.ic_favourite_remove);
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
            case R.id.action_downloads:
                startActivity(new Intent(this, DownloadsActivity.class));
                return true;
            case R.id.action_blacklist:
                new BlacklistManager(this).add(packageName);
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onDestroy() {
        try {
            app = null;
            unregisterReceiver(globalInstallReceiver);
            disposable.clear();
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

    private void drawBasic() {
        GlideApp.with(this)
                .asBitmap()
                .load(app.getIconUrl())
                .transition(new BitmapTransitionOptions().crossFade())
                .transforms(new CenterCrop(), new RoundedCorners(50))
                .into(icon);
        txtDisplayName.setText(app.getDisplayName());
        txtPackageName.setText(app.getPackageName());
    }

    private void draw(App appFromMarket) {
        app = appFromMarket;
        actionButton = new ActionButton(this, app);
        disposable.add(Observable.just(
                new GeneralDetails(this, app),
                new Screenshot(this, app),
                new Reviews(this, app),
                new ExodusPrivacy(this, app),
                new Video(this, app),
                new Beta(this, app),
                new AppLinks(this, app))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(AbstractDetails::draw)
                .subscribe());
        drawButtons();
    }

    public void drawButtons() {
        if (PackageUtil.isInstalled(this, app))
            app.setInstalled(true);
        if (actionButton != null)
            actionButton.draw();
    }
}
