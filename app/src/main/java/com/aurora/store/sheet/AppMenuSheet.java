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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.events.Event;
import com.aurora.store.installer.Uninstaller;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.manager.FavouritesManager;
import com.aurora.store.model.App;
import com.aurora.store.ui.single.activity.ManualDownloadActivity;
import com.aurora.store.util.ApkCopier;
import com.aurora.store.util.Log;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.navigation.NavigationView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AppMenuSheet extends BaseBottomSheet {

    public static final String TAG = "APP_MENU_SHEET";

    @BindView(R.id.navigation_view)
    NavigationView navigationView;

    private App app;

    public AppMenuSheet() {
    }

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_app_menu, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            stringExtra = bundle.getString(Constants.STRING_EXTRA);
            intExtra = bundle.getInt(Constants.INT_EXTRA);
            //Get App from bundle
            app = gson.fromJson(stringExtra, App.class);
            setupNavigationView();
        } else {
            dismissAllowingStateLoss();
        }
    }

    private void setupNavigationView() {
        final FavouritesManager favouritesManager = new FavouritesManager(requireContext());
        final BlacklistManager blacklistManager = new BlacklistManager(requireContext());

        final boolean isFavourite = favouritesManager.isFavourite(app.getPackageName());
        final boolean isBlacklisted = blacklistManager.isBlacklisted(app.getPackageName());

        //Switch strings for Add/Remove Favourite
        final MenuItem favMenu = navigationView.getMenu().findItem(R.id.action_fav);
        favMenu.setTitle(isFavourite ? R.string.details_favourite_remove : R.string.details_favourite_add);

        //Switch strings for Add/Remove Blacklist
        final MenuItem blackListMenu = navigationView.getMenu().findItem(R.id.action_blacklist);
        blackListMenu.setTitle(isBlacklisted ? R.string.action_whitelist : R.string.menu_blacklist);

        //Show/Hide actions based on installed status
        final boolean installed = PackageUtil.isInstalled(requireContext(), app);
        navigationView.getMenu().findItem(R.id.action_uninstall).setVisible(installed);
        navigationView.getMenu().findItem(R.id.action_local).setVisible(installed);
        navigationView.getMenu().findItem(R.id.action_info).setVisible(installed);

        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_fav:
                    if (isFavourite) {
                        favouritesManager.removeFromFavourites(app.getPackageName());
                    } else {
                        favouritesManager.addToFavourites(app.getPackageName());
                    }
                    break;
                case R.id.action_blacklist:
                    if (isBlacklisted) {
                        blacklistManager.removeFromBlacklist(app.getPackageName());

                        AuroraApplication
                                .getRelayBus()
                                .accept(new Event(Event.SubType.WHITELIST, app.getPackageName()));
                    } else {
                        blacklistManager.addToBlacklist(app.getPackageName());

                        AuroraApplication
                                .getRelayBus()
                                .accept(new Event(Event.SubType.BLACKLIST, intExtra));
                    }
                    Toast.makeText(requireContext(), isBlacklisted ?
                                    requireContext().getString(R.string.toast_apk_whitelisted) :
                                    requireContext().getString(R.string.toast_apk_blacklisted),
                            Toast.LENGTH_SHORT).show();
                    break;
                case R.id.action_local:
                    Observable.fromCallable(() -> new ApkCopier(requireContext(), app)
                            .copy())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext(result -> {
                                Toast.makeText(requireContext(), result
                                        ? requireContext().getString(R.string.toast_apk_copy_success)
                                        : requireContext().getString(R.string.toast_apk_copy_failure), Toast.LENGTH_SHORT)
                                        .show();
                            })
                            .doOnError(throwable -> {
                                Log.e("Failed to copy app to local directory");
                            })
                            .subscribe();
                    break;
                case R.id.action_manual:
                    Intent intent = new Intent(requireContext(), ManualDownloadActivity.class);
                    intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app));
                    requireContext().startActivity(intent, ViewUtil.getEmptyActivityBundle((AppCompatActivity) requireContext()));
                    break;
                case R.id.action_uninstall:
                    new Uninstaller(requireContext()).uninstall(app);
                    break;
                case R.id.action_info:
                    try {
                        requireContext().startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS",
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
