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

import com.aurora.store.exception.InvalidApiException;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.aurora.store.util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchTask {

    private Context context;

    public SearchTask(Context context) {
        this.context = context;
    }

    public List<App> getSearchResults(CustomAppListIterator iterator) throws Exception {
        if (iterator == null)
            throw new InvalidApiException();
        if (!iterator.hasNext()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(getNextBatch(iterator));
    }

    private List<App> getNextBatch(CustomAppListIterator iterator) {
        List<App> apps = new ArrayList<>();

        if (!iterator.hasNext())
            return apps;

        apps.addAll(iterator.next());

        if (Util.filterGoogleAppsEnabled(context))
            return filterGoogleApps(apps);
        else
            return apps;
    }

    private List<App> filterGoogleApps(List<App> apps) {
        Set<String> gAppsSet = new HashSet<>();
        gAppsSet.add("com.chrome.beta");
        gAppsSet.add("com.chrome.canary");
        gAppsSet.add("com.chrome.dev");
        gAppsSet.add("com.android.chrome");
        gAppsSet.add("com.niksoftware.snapseed");
        gAppsSet.add("com.google.toontastic");

        List<App> appList = new ArrayList<>();
        for (App app : apps) {
            if (!app.getPackageName().startsWith("com.google") && !gAppsSet.contains(app.getPackageName())) {
                appList.add(app);
            }
        }
        return appList;
    }
}
