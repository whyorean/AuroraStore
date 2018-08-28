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

package com.dragons.aurora.task.playstore;

import android.content.Context;

import com.dragons.aurora.AppListIterator;
import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.fragment.FilterMenu;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.Filter;

import java.util.ArrayList;
import java.util.List;

public class SearchTask extends CategoryAppsTask {

    public SearchTask(Context context) {
        super(context);
    }

    @Override
    public List<App> getNextBatch(AppListIterator iterator) {
        CategoryManager categoryManager = new CategoryManager(getContext());
        Filter filter = new FilterMenu((AuroraActivity) getContext()).getFilterPreferences();
        List<App> apps = new ArrayList<>();
        for (App app : iterator.next()) {
            if (categoryManager.fits(app.getCategoryId(), filter.getCategory())) {
                apps.add(app);
            }
        }
        return apps;
    }
}
