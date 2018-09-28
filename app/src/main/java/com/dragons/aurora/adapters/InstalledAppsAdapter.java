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

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.BlackWhiteListManager;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.fragment.InstalledAppsFragment;
import com.dragons.aurora.fragment.MoreCategoryApps;
import com.dragons.aurora.fragment.SearchAppsFragment;
import com.dragons.aurora.fragment.TopFreeApps;
import com.dragons.aurora.fragment.details.ButtonDownload;
import com.dragons.aurora.fragment.details.ButtonUninstall;
import com.dragons.aurora.fragment.details.DownloadOptions;
import com.dragons.aurora.model.App;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {

    public List<App> appsToAdd;
    private BlackWhiteListManager manager;
    private Fragment fragment;

    //Constructors

    public InstalledAppsAdapter(MoreCategoryApps fragment, List<App> appsToAdd) {
        this.fragment = fragment;
        this.appsToAdd = appsToAdd;
        manager = new BlackWhiteListManager(fragment.getContext());
    }

    public InstalledAppsAdapter(InstalledAppsFragment fragment, List<App> appsToAdd) {
        this.fragment = fragment;
        this.appsToAdd = appsToAdd;
        manager = new BlackWhiteListManager(fragment.getContext());
    }

    public InstalledAppsAdapter(SearchAppsFragment fragment, List<App> appsToAdd) {
        this.fragment = fragment;
        this.appsToAdd = appsToAdd;
        manager = new BlackWhiteListManager(fragment.getContext());
    }

    public InstalledAppsAdapter(TopFreeApps fragment, List<App> appsToAdd) {
        this.fragment = fragment;
        this.appsToAdd = appsToAdd;
        manager = new BlackWhiteListManager(fragment.getContext());
    }

    //Methods to Update List (Add or Remove)

    public void add(int position, App app) {
        appsToAdd.add(position, app);
        notifyItemInserted(position);
    }

    public void add(App app) {
        appsToAdd.add(app);
    }

    public void remove(int position) {
        appsToAdd.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public InstalledAppsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_installed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstalledAppsAdapter.ViewHolder holder, int position) {
        App app = appsToAdd.get(position);
        List<String> Version = new ArrayList<>();
        List<String> Extra = new ArrayList<>();

        holder.AppTitle.setText(app.getDisplayName());
        getDetails(fragment.getContext(), Version, Extra, app);
        setText(holder.AppVersion, TextUtils.join(" • ", Version));
        setText(holder.AppExtra, TextUtils.join(" • ", Extra));

        holder.AppContainer.setOnClickListener(v -> {
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

        setup3dotMenu(holder, app, position);
    }

    public void getDetails(Context mContext, List<String> Version, List<String> Extra, App app) {
        Version.add("v" + app.getVersionName() + "." + app.getVersionCode());
        if (app.isSystem())
            Extra.add(mContext.getString(R.string.list_app_system));
        else
            Extra.add(mContext.getString(R.string.list_app_user));

        if (manager.contains(app.getPackageName())) {
            Extra.add(mContext.getString(manager.isBlack() ? R.string.list_app_blacklisted : R.string.list_app_whitelisted));
        }
    }

    protected void setText(TextView textView, String text) {
        if (!TextUtils.isEmpty(text)) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void setup3dotMenu(ViewHolder viewHolder, App app, int position) {
        viewHolder.AppMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(fragment.getContext(), v);
            popup.inflate(R.menu.menu_download);
            new DownloadOptions(fragment.getContext(), fragment.getView(), app).inflate(popup.getMenu());
            popup.getMenu().findItem(R.id.action_download).setVisible(new ButtonDownload(fragment.getContext(), fragment.getView(), app).shouldBeVisible());
            popup.getMenu().findItem(R.id.action_uninstall).setVisible(app.isInstalled());
            popup.getMenu().findItem(R.id.action_manual).setVisible(true);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.action_download:
                        new ButtonDownload(fragment.getContext(), fragment.getView(), app).checkAndDownload();
                        break;
                    case R.id.action_uninstall:
                        new ButtonUninstall(fragment.getContext(), fragment.getView(), app).uninstall();
                        remove(position);
                        break;
                    default:
                        return new DownloadOptions(fragment.getContext(), fragment.getView(), app).onContextItemSelected(item);
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return appsToAdd.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public View view;
        private LinearLayout AppContainer;
        private ImageView AppMenu;
        private ImageView AppIcon;
        private TextView AppTitle;
        private TextView AppVersion;
        private TextView AppExtra;


        public ViewHolder(View view) {
            super(view);
            this.view = view;
            AppContainer = view.findViewById(R.id.app_card);
            AppIcon = view.findViewById(R.id.app_icon);
            AppMenu = view.findViewById(R.id.menu_3dot);
            AppTitle = view.findViewById(R.id.app_title);
            AppVersion = view.findViewById(R.id.app_version);
            AppExtra = view.findViewById(R.id.app_extra);
        }
    }

}
