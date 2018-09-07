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

import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.fragment.MoreCategoryApps;
import com.dragons.aurora.fragment.SearchAppsFragment;
import com.dragons.aurora.fragment.TopFreeApps;
import com.dragons.aurora.model.App;

import java.util.List;

import androidx.annotation.NonNull;

public class EndlessAppsAdapter extends InstalledAppsAdapter {

    public EndlessAppsAdapter(MoreCategoryApps fragment, List<App> appsToAdd) {
        super(fragment, appsToAdd);
    }

    public EndlessAppsAdapter(SearchAppsFragment fragment, List<App> appsToAdd) {
        super(fragment, appsToAdd);
    }

    public EndlessAppsAdapter(TopFreeApps fragment, List<App> appsToAdd) {
        super(fragment, appsToAdd);
    }


    @Override
    public void onBindViewHolder(@NonNull InstalledAppsAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
    }

    @Override
    public void getDetails(Context mContext, List<String> Version, List<String> Extra, App app) {
        Version.add(Util.addSiPrefix((int) app.getSize()));
        if (!app.isEarlyAccess())
            Version.add(mContext.getString(R.string.details_rating, (app.getRating().getAverage())) + " ★");
        Extra.add(app.getPrice());
        Extra.add(mContext.getString(app.containsAds() ? R.string.list_app_has_ads : R.string.list_app_no_ads));
        Extra.add(mContext.getString(app.getDependencies().isEmpty() ? R.string.list_app_independent_from_gsf : R.string.list_app_depends_on_gsf));
    }
}
