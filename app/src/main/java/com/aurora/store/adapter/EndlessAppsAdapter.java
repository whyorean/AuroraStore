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

import androidx.annotation.NonNull;

import com.aurora.store.ListType;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.utility.Util;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class EndlessAppsAdapter extends InstalledAppsAdapter {

    public EndlessAppsAdapter(Context context) {
        super(context, ListType.ENDLESS);
    }

    @Override
    public void onBindViewHolder(@NonNull InstalledAppsAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
    }

    @Override
    public void getDetails(List<String> Version, List<String> Extra, App app) {
        Version.add(Util.addSiPrefix(app.getSize()));
        if (!app.isEarlyAccess())
            Version.add(context.getString(R.string.details_rating, (app.getRating().getAverage())));
        if (PackageUtil.isInstalled(context, app.getPackageName()))
            Version.add(context.getString(R.string.action_installed));
        Extra.add(app.getPrice());
        Extra.add(context.getString(app.containsAds() ? R.string.list_app_has_ads : R.string.list_app_no_ads));
        Extra.add(context.getString(app.getDependencies().isEmpty() ? R.string.list_app_independent_from_gsf : R.string.list_app_depends_on_gsf));
        if (!StringUtils.isEmpty(app.getUpdated()))
            Extra.add(app.getUpdated());
    }
}
