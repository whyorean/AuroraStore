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

import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.model.App;
import com.dragons.aurora.view.SearchResultAppBadge;

import java.util.List;

public class EndlessAppsAdapter extends InstalledAppsAdapter {

    private List<App> appsToAdd;
    private InstalledAppsAdapter.ViewHolder viewHolder;

    public EndlessAppsAdapter(Context context, List<App> appsToAdd) {
        super((AuroraActivity) context, appsToAdd);
        this.appsToAdd = appsToAdd;
    }

    @Override
    public void setViewHolder(ViewHolder viewHolder) {
        this.viewHolder = viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull InstalledAppsAdapter.ViewHolder holder, int position) {
        setViewHolder(holder);
        final App app = appsToAdd.get(position);
        final SearchResultAppBadge searchResultAppBadge = new SearchResultAppBadge();

        searchResultAppBadge.setApp(app);
        searchResultAppBadge.setView(holder.view);
        searchResultAppBadge.draw();

        viewHolder.list_container.setOnClickListener(v -> {
            Context context = viewHolder.view.getContext();
            context.startActivity(DetailsActivity.getDetailsIntent(context, app.getPackageName()));
        });

        setup3dotMenu(viewHolder, app, position);
    }
}
