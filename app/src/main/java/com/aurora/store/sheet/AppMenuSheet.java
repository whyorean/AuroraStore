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

package com.aurora.store.sheet;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aurora.store.AuroraApplication;
import com.aurora.store.R;
import com.aurora.store.events.Event;
import com.aurora.store.events.RxBus;
import com.aurora.store.installer.Uninstaller;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.manager.FavouriteListManager;
import com.aurora.store.model.App;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.single.activity.ManualDownloadActivity;
import com.aurora.store.util.ApkCopier;
import com.aurora.store.util.Log;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.navigation.NavigationView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AppMenuSheet extends BottomSheetDialogFragment {

    @BindView(R.id.navigation_view)
    NavigationView navigationView;

    private App app;
    private Context context;
    private CompositeDisposable disposable = new CompositeDisposable();
    private RxBus rxBus;

    public AppMenuSheet() {
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        this.rxBus = AuroraApplication.getRxBus();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_app_menu, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final FavouriteListManager favouriteListManager = new FavouriteListManager(context);
        boolean isFav = favouriteListManager.contains(app.getPackageName());
        MenuItem favMenu = navigationView.getMenu().findItem(R.id.action_fav);
        favMenu.setTitle(isFav ? R.string.details_favourite_remove : R.string.details_favourite_add);

        final BlacklistManager blacklistManager = new BlacklistManager(context);
        boolean isBlacklisted = blacklistManager.contains(app.getPackageName());
        MenuItem blackListMenu = navigationView.getMenu().findItem(R.id.action_blacklist);
        blackListMenu.setTitle(isBlacklisted ? R.string.action_whitelist : R.string.menu_blacklist);

        boolean installed = PackageUtil.isInstalled(context, app);
        navigationView.getMenu().findItem(R.id.action_uninstall).setVisible(installed);
        navigationView.getMenu().findItem(R.id.action_local).setVisible(installed);
        navigationView.getMenu().findItem(R.id.action_info).setVisible(installed);

        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_fav:
                    if (isFav) {
                        favouriteListManager.remove(app.getPackageName());
                    } else {
                        favouriteListManager.add(app.getPackageName());
                    }
                    break;
                case R.id.action_blacklist:
                    if (isBlacklisted) {
                        blacklistManager.remove(app.getPackageName());
                        Toast.makeText(context, context.getString(R.string.toast_apk_whitelisted),
                                Toast.LENGTH_SHORT).show();
                        rxBus
                                .getBus()
                                .accept(new Event(Event.SubType.WHITELIST, app.getPackageName()));
                    } else {
                        blacklistManager.add(app.getPackageName());
                        Toast.makeText(context, context.getString(R.string.toast_apk_blacklisted),
                                Toast.LENGTH_SHORT).show();
                        rxBus
                                .getBus()
                                .accept(new Event(Event.SubType.BLACKLIST, app.getPackageName()));
                    }
                    break;
                case R.id.action_local:
                    disposable.add(Observable.fromCallable(() -> new ApkCopier(context, app)
                            .copy())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(success -> {
                                Toast.makeText(context, success
                                        ? context.getString(R.string.toast_apk_copy_success)
                                        : context.getString(R.string.toast_apk_copy_failure), Toast.LENGTH_SHORT)
                                        .show();
                            }));
                    break;
                case R.id.action_manual:
                    DetailsActivity.app = app;
                    context.startActivity(new Intent(context, ManualDownloadActivity.class), ViewUtil.getEmptyActivityBundle((AppCompatActivity) context));
                    break;
                case R.id.action_uninstall:
                    new Uninstaller(context).uninstall(app);
                    break;
                case R.id.action_info:
                    try {
                        context.startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS",
                                Uri.parse("package:" + app.getPackageName())));
                    } catch (ActivityNotFoundException e) {
                        Log.e("Could not find system app activity");
                    }
                    break;
            }
            dismissAllowingStateLoss();
            return false;
        });
    }
}
