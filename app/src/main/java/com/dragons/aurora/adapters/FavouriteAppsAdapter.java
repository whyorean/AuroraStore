/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.dragons.aurora.adapters;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.FavouriteListManager;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.fragment.FavouriteFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.FavouriteItem;

import java.util.ArrayList;
import java.util.List;

public class FavouriteAppsAdapter extends RecyclerView.Adapter implements SelectableViewHolder.OnItemSelectedListener {

    private List<FavouriteItem> favouriteItems = new ArrayList<>();
    private Fragment fragment;
    private FavouriteListManager manager;
    private SelectableViewHolder.OnItemSelectedListener listener;

    public FavouriteAppsAdapter(FavouriteFragment fragment, SelectableViewHolder.OnItemSelectedListener listener, List<App> appsToAdd) {
        this.listener = listener;
        this.fragment = fragment;
        manager = new FavouriteListManager(fragment.getContext());
        for (App app : appsToAdd) {
            favouriteItems.add(new FavouriteItem(app));
        }
    }

    public void add(int position, FavouriteItem app) {
        favouriteItems.add(position, app);
        notifyItemInserted(position);
    }

    public void add(FavouriteItem app) {
        favouriteItems.add(app);
    }

    public void remove(int position) {
        manager.remove(favouriteItems.get(position).getApp().getPackageName());
        favouriteItems.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public SelectableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_favorite, parent, false);
        return new SelectableViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        SelectableViewHolder holder = (SelectableViewHolder) viewHolder;
        FavouriteItem mFavouriteItem = favouriteItems.get(position);

        App app = mFavouriteItem.getApp();
        holder.AppTitle.setText(app.getDisplayName());
        holder.AppVersion.setText(app.getDeveloperName());

        if (isInstalled(app)) {
            holder.AppExtra.setText(fragment.getText(R.string.list_installed));
            holder.AppCheckbox.setEnabled(false);
        } else {
            holder.AppExtra.setText(fragment.getText(R.string.list_not_installd));
        }

        holder.viewForeground.setOnClickListener(v -> {
            Intent intent = new Intent(fragment.getContext(), DetailsActivity.class);
            intent.putExtra("INTENT_PACKAGE_NAME", app.getPackageName());
            fragment.startActivity(intent);
        });

        Glide
                .with(fragment.getContext())
                .load(app.getIconInfo().getUrl())
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                .transition(new DrawableTransitionOptions().crossFade())
                .into(holder.AppIcon);

        holder.favouriteItem = mFavouriteItem;
        holder.setChecked(holder.favouriteItem.getSelected());
    }

    private boolean isInstalled(App app) {
        try {
            fragment.getContext().getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return favouriteItems.size();
    }

    @Override
    public void onItemSelected(FavouriteItem favouriteItem) {

        listener.onItemSelected(favouriteItem);
    }

    public List<App> getSelectedItems() {
        List<App> selectedItems = new ArrayList<>();
        for (FavouriteItem favouriteItem : favouriteItems) {
            if (favouriteItem.getSelected()) {
                selectedItems.add(favouriteItem.getApp());
            }
        }
        return selectedItems;
    }
}
