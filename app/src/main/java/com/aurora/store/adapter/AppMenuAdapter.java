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

package com.aurora.store.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.ListType;
import com.aurora.store.R;
import com.aurora.store.activity.ManualDownloadActivity;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.installer.Installer;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.manager.FavouriteListManager;
import com.aurora.store.model.App;
import com.aurora.store.model.MenuEntry;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.utility.ApkCopier;
import com.aurora.store.utility.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AppMenuAdapter extends RecyclerView.Adapter<AppMenuAdapter.ViewHolder> {

    private AppMenuSheet menuSheet;
    private ListType listType;
    private Context context;
    private List<MenuEntry> menuEntryList;
    private App app;

    public AppMenuAdapter(AppMenuSheet menuSheet, App app, ListType listType) {
        this.menuSheet = menuSheet;
        this.context = menuSheet.getContext();
        this.app = app;
        this.listType = listType;
        switch (listType) {
            case INSTALLED:
            case UPDATES:
                menuEntryList = ViewUtil.parseMenu(context, R.menu.menu_app_single);
                break;
            case ENDLESS:
                menuEntryList = ViewUtil.parseMenu(context, R.menu.menu_app_endless);
                break;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sheet_menu, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final MenuEntry menuEntry = menuEntryList.get(position);
        holder.menu_title.setText(menuEntry.getTitle());
        attachMenuAction(menuEntry, holder);
    }

    private void attachMenuAction(MenuEntry menuEntry, ViewHolder holder) {
        View view = holder.itemView;
        switch (menuEntry.getResId()) {
            case R.id.action_favourite:
                FavouriteListManager favouriteListManager = new FavouriteListManager(context);
                if (favouriteListManager.contains(app.getPackageName())) {
                    holder.menu_title.setText(R.string.details_favourite_remove);
                    view.setOnClickListener(v -> {
                        favouriteListManager.remove(app.getPackageName());
                        menuSheet.dismissAllowingStateLoss();
                    });
                } else {
                    holder.menu_title.setText(R.string.details_favourite_add);
                    view.setOnClickListener(v -> {
                        favouriteListManager.add(app.getPackageName());
                        menuSheet.dismissAllowingStateLoss();
                    });
                }
                break;
            case R.id.action_uninstall:
                view.setOnClickListener(v -> {
                    new Installer(context).uninstall(app);
                    menuSheet.dismissAllowingStateLoss();
                });
                break;
            case R.id.action_blacklist:
                BlacklistManager blacklistManager = new BlacklistManager(context);
                if (blacklistManager.isBackListed(app.getPackageName())) {
                    holder.menu_title.setText(R.string.action_whitelist);
                    view.setOnClickListener(v -> {
                        blacklistManager.removeFromBlacklist(app.getPackageName());
                        menuSheet.dismissAllowingStateLoss();
                        Toast.makeText(context, context.getString(R.string.toast_apk_whitelisted),
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    holder.menu_title.setText(R.string.action_blacklist);
                    view.setOnClickListener(v -> {
                        blacklistManager.addToBlacklist(app.getPackageName());
                        menuSheet.dismissAllowingStateLoss();
                        Toast.makeText(context, context.getString(R.string.toast_apk_blacklisted),
                                Toast.LENGTH_SHORT).show();
                    });
                }
                break;
            case R.id.action_manual:
                view.setOnClickListener(v -> {
                    DetailsFragment.app = app;
                    context.startActivity(new Intent(context, ManualDownloadActivity.class));
                    menuSheet.dismissAllowingStateLoss();
                });
                break;
            case R.id.action_getLocal:
                view.setOnClickListener(v -> {
                    ApkCopier apkCopier = new ApkCopier();
                    boolean success = apkCopier.copy(app);
                    Toast.makeText(context, success
                            ? context.getString(R.string.toast_apk_copy_success)
                            : context.getString(R.string.toast_apk_copy_failure), Toast.LENGTH_SHORT)
                            .show();
                    menuSheet.dismissAllowingStateLoss();
                });
                break;
        }
    }

    @Override
    public int getItemCount() {
        return menuEntryList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.menu_title)
        TextView menu_title;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}

