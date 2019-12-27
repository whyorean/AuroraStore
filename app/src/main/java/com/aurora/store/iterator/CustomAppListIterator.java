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

package com.aurora.store.iterator;

import com.aurora.store.model.App;
import com.aurora.store.model.AppBuilder;
import com.aurora.store.model.FilterModel;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.AppListIterator;
import com.dragons.aurora.playstoreapiv2.DocV2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CustomAppListIterator implements Iterator {

    private boolean filterEnabled = false;
    private FilterModel filterModel = new FilterModel();
    private AppListIterator iterator;
    private List<String> relatedTags = new ArrayList<>();

    public CustomAppListIterator(AppListIterator iterator) {
        this.iterator = iterator;
    }

    public List<String> getRelatedTags() {
        Set<String> set = new HashSet<>(relatedTags);
        relatedTags.clear();
        relatedTags.addAll(set);
        return relatedTags;
    }

    public void setFilterEnabled(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public void setFilterModel(FilterModel filterModel) {
        this.filterModel = filterModel;
    }

    @Override
    public List<App> next() {
        List<App> apps = new ArrayList<>();
        for (DocV2 docV2 : iterator.next()) {
            if (docV2.getDocType() == 53)
                relatedTags.add(docV2.getTitle());
            else if (docV2.getDocType() == 1) {
                addApp(apps, AppBuilder.build(docV2));
            } else {
                /*
                 * This one is either a movie, music or book, will exploit it at some point of time
                 * Movies = 6, Music = 2, Audio Books = 64
                 */
                continue;
            }
        }
        return apps;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    private boolean shouldSkip(App app) {
        return (!filterModel.isPaidApps() && !app.isFree())
                || (!filterModel.isAppsWithAds() && app.isContainsAds())
                || (!filterModel.isGsfDependentApps() && !app.getDependencies().isEmpty())
                || (filterModel.getRating() > 0 && app.getRating().getAverage() < filterModel.getRating())
                || (filterModel.getDownloads() > 0 && app.getInstalls() < filterModel.getDownloads());
    }

    private void addApp(List<App> apps, App app) {
        if (filterEnabled && shouldSkip(app)) {
            Log.i("Filtering out " + app.getPackageName());
        } else {
            apps.add(app);
        }
    }
}
