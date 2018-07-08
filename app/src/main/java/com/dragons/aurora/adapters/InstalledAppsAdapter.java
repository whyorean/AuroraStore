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
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.fragment.details.ButtonDownload;
import com.dragons.aurora.fragment.details.ButtonUninstall;
import com.dragons.aurora.fragment.details.DownloadOptions;
import com.dragons.aurora.model.App;
import com.dragons.aurora.view.InstalledAppBadge;

import java.util.List;

public class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {

    public List<App> appsToAdd;
    private AuroraActivity activity;
    private ViewHolder viewHolder;

    public InstalledAppsAdapter(AuroraActivity activity, List<App> appsToAdd) {
        this.activity = activity;
        this.appsToAdd = appsToAdd;
    }

    public void setViewHolder(ViewHolder viewHolder) {
        this.viewHolder = viewHolder;
    }

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
        View view = inflater.inflate(R.layout.installed_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstalledAppsAdapter.ViewHolder holder, int position) {
        setViewHolder(holder);
        final App app = appsToAdd.get(position);
        final InstalledAppBadge installedAppBadge = new InstalledAppBadge();

        installedAppBadge.setApp(app);
        installedAppBadge.setView(viewHolder.view);
        installedAppBadge.draw();

        viewHolder.list_container.setOnClickListener(v -> {
            Context context = viewHolder.view.getContext();
            context.startActivity(DetailsActivity.getDetailsIntent(context, app.getPackageName()));
        });

        setup3dotMenu(viewHolder, app, position);
    }

    void setup3dotMenu(ViewHolder viewHolder, App app, int position) {
        viewHolder.menu_3dot.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.menu_download);
            new DownloadOptions(activity, app).inflate(popup.getMenu());
            popup.getMenu().findItem(R.id.action_download).setVisible(new ButtonDownload(activity, app).shouldBeVisible());
            popup.getMenu().findItem(R.id.action_uninstall).setVisible(app.isInstalled());
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.action_download:
                        new ButtonDownload(activity, app).checkAndDownload();
                        break;
                    case R.id.action_uninstall:
                        new ButtonUninstall(activity, app).uninstall();
                        remove(position);
                        break;
                    default:
                        return new DownloadOptions(activity, app).onContextItemSelected(item);
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
        public CardView list_container;
        public ImageView menu_3dot;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            list_container = view.findViewById(R.id.list_container);
            menu_3dot = view.findViewById(R.id.menu_3dot);
        }
    }

}
