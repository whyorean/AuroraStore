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

import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.aurora.store.util.Util;

import java.util.ArrayList;
import java.util.List;

public class CategoryAppsTask extends BaseTask {

    public CategoryAppsTask(Context context) {
        super(context);
    }

    public List<App> getApps(@NonNull CustomAppListIterator iterator) {
        if (!iterator.hasNext()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(getNextBatch(iterator));
    }

    public List<App> getNextBatch(CustomAppListIterator iterator) {
        List<App> apps = new ArrayList<>(iterator.next());
        if (Util.filterGoogleAppsEnabled(context))
            return filterGoogleApps(apps);
        else
            return apps;
    }
}
