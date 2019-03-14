/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
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

package com.aurora.store;

import android.content.Context;
import android.content.SharedPreferences;

import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;

public class Filter {

    private Context context;

    public Filter(Context context) {
        this.context = context;
    }

    public com.aurora.store.model.Filter getFilterPreferences() {
        com.aurora.store.model.Filter filter = new com.aurora.store.model.Filter();
        SharedPreferences prefs = Util.getPrefs(context);
        filter.setSystemApps(prefs.getBoolean(Constants.FILTER_SYSTEM_APPS, false));
        filter.setAppsWithAds(prefs.getBoolean(Constants.FILTER_APPS_WITH_ADS, true));
        filter.setPaidApps(prefs.getBoolean(Constants.FILTER_PAID_APPS, true));
        filter.setGsfDependentApps(prefs.getBoolean(Constants.FILTER_GSF_DEPENDENT_APPS, true));
        filter.setCategory(prefs.getString(Constants.FILTER_CATEGORY, Constants.TOP));
        filter.setRating(prefs.getFloat(Constants.FILTER_RATING, 0.0f));
        filter.setDownloads(prefs.getInt(Constants.FILTER_DOWNLOADS, 0));
        return filter;
    }

    public void resetFilterPreferences() {
        SharedPreferences prefs = Util.getPrefs(context);
        PrefUtil.putBoolean(context, Constants.FILTER_SYSTEM_APPS, false);
        PrefUtil.putBoolean(context, Constants.FILTER_APPS_WITH_ADS, true);
        PrefUtil.putBoolean(context, Constants.FILTER_PAID_APPS, true);
        PrefUtil.putBoolean(context, Constants.FILTER_GSF_DEPENDENT_APPS, true);
        PrefUtil.putString(context, Constants.FILTER_CATEGORY, Constants.TOP);
        PrefUtil.putFloat(context, Constants.FILTER_RATING, 0.0f);
        PrefUtil.putInteger(context, Constants.FILTER_DOWNLOADS, 0);
    }
}
