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

import com.dragons.aurora.R;
import com.dragons.aurora.model.App;
import com.dragons.aurora.view.UpdatableAppBadge;

import java.util.List;

public class UpdatableAppsAdapter extends RecyclerView.Adapter<UpdatableAppsAdapter.ViewHolder> {

    public List<App> appsToAdd;

    public UpdatableAppsAdapter(List<App> appsToAdd) {
        this.appsToAdd = appsToAdd;
    }

    public void add(int position, App app) {
        appsToAdd.add(position, app);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        appsToAdd.remove(position);
        notifyItemRemoved(position);
    }

    public void remove(String packageName) {
        int i = 0;
        for (App app : appsToAdd) {
            if (app.getPackageName().equals(packageName)) {
                remove(i);
                break;
            }
            i++;
        }
    }

    @NonNull
    @Override
    public UpdatableAppsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.updatable_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpdatableAppsAdapter.ViewHolder holder, int position) {
        final App app = appsToAdd.get(position);
        final UpdatableAppBadge updatableAppBadge = new UpdatableAppBadge();
        holder.app = app;
        updatableAppBadge.setApp(app);
        updatableAppBadge.setView(holder.view);
        updatableAppBadge.draw();
    }

    @Override
    public int getItemCount() {
        return appsToAdd.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public CardView viewForeground;
        public App app;
        private View view;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            viewForeground = view.findViewById(R.id.view_foreground);
        }
    }
}
