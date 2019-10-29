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

package com.aurora.store.task;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aurora.store.Filter;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.model.App;
import com.aurora.store.utility.Util;

import java.util.ArrayList;
import java.util.List;

public class SearchTask extends BaseTask {

    public SearchTask(Context context) {
        super(context);
    }

    public List<App> getSearchResults(CustomAppListIterator iterator) {
        if (iterator == null || !iterator.hasNext()) {
            return new ArrayList<>();
        }
        List<App> apps = new ArrayList<>();
        while (iterator.hasNext() && apps.isEmpty()) {
            apps.addAll(getNextBatch(iterator));
        }
        return apps;
    }

    public List<App> getNextBatch(CustomAppListIterator iterator) {
        CategoryManager categoryManager = new CategoryManager(getContext());
        com.aurora.store.model.Filter filter = new Filter(getContext()).getFilterPreferences();
        List<App> apps = new ArrayList<>();
        for (App app : iterator.next()) {
            if (categoryManager.fits(app.getCategoryId(), filter.getCategory())) {
                apps.add(app);
            }
        }
        if (Util.filterGoogleAppsEnabled(context))
            return filterGoogleApps(apps);
        else
            return apps;
    }
}
