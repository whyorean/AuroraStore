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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.activity.ManualDownloadActivity;
import com.aurora.store.adapter.InstalledAppsAdapter;
import com.aurora.store.adapter.UpdatableAppsAdapter;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.installer.Installer;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.manager.FavouriteListManager;
import com.aurora.store.model.App;
import com.aurora.store.utility.ApkCopier;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.view.CustomBottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AppMenuSheet extends CustomBottomSheetDialogFragment {

    @BindView(R.id.menu_title)
    TextView txtTitle;
    @BindView(R.id.btn_fav)
    MaterialButton btnFav;
    @BindView(R.id.btn_blacklist)
    MaterialButton btnBlacklist;
    @BindView(R.id.btn_local_apk)
    MaterialButton btnLocal;
    @BindView(R.id.btn_manual)
    MaterialButton btnManual;
    @BindView(R.id.btn_uninstall)
    MaterialButton btnUninstall;

    private App app;
    private Context context;
    private RecyclerView.Adapter adapter;
    private CompositeDisposable disposable = new CompositeDisposable();

    public AppMenuSheet() {
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        this.adapter = adapter;
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
        txtTitle.setText(app.getDisplayName());

        btnUninstall.setVisibility(PackageUtil.isInstalled(context, app) ? View.VISIBLE : View.GONE);
        btnLocal.setVisibility(PackageUtil.isInstalled(context, app) ? View.VISIBLE : View.GONE);

        final FavouriteListManager favouriteListManager = new FavouriteListManager(context);
        boolean isFav = favouriteListManager.contains(app.getPackageName());
        btnFav.setText(isFav ? R.string.details_favourite_remove : R.string.details_favourite_add);
        btnFav.setOnClickListener(v -> {
            if (isFav) {
                favouriteListManager.remove(app.getPackageName());
            } else {
                favouriteListManager.add(app.getPackageName());
            }
            dismissAllowingStateLoss();
        });

        final BlacklistManager blacklistManager = new BlacklistManager(context);
        boolean isBlacklisted = blacklistManager.contains(app.getPackageName());
        btnBlacklist.setText(isBlacklisted ? R.string.action_whitelist : R.string.action_blacklist);
        btnBlacklist.setOnClickListener(v -> {
            if (isBlacklisted) {
                blacklistManager.remove(app.getPackageName());
                Toast.makeText(context, context.getString(R.string.toast_apk_whitelisted),
                        Toast.LENGTH_SHORT).show();
            } else {
                blacklistManager.add(app.getPackageName());
                Toast.makeText(context, context.getString(R.string.toast_apk_blacklisted),
                        Toast.LENGTH_SHORT).show();
                if (adapter instanceof InstalledAppsAdapter)
                    ((InstalledAppsAdapter) adapter).remove(app);
                if (adapter instanceof UpdatableAppsAdapter)
                    ((UpdatableAppsAdapter) adapter).remove(app);
            }
            dismissAllowingStateLoss();
        });

        btnLocal.setOnClickListener(v -> {
            disposable.add(Observable.fromCallable(() -> new ApkCopier(app)
                    .copy())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(success -> {
                        Toast.makeText(context, success
                                ? context.getString(R.string.toast_apk_copy_success)
                                : context.getString(R.string.toast_apk_copy_failure), Toast.LENGTH_SHORT)
                                .show();
                    }));
            dismissAllowingStateLoss();
        });

        btnManual.setOnClickListener(v -> {
            DetailsFragment.app = app;
            context.startActivity(new Intent(context, ManualDownloadActivity.class));
            dismissAllowingStateLoss();
        });

        btnUninstall.setOnClickListener(v -> {
            new Installer(context).uninstall(app);
            dismissAllowingStateLoss();
        });
    }
}
