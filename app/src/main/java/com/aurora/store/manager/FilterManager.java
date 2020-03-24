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

package com.aurora.store.manager;

import android.content.Context;

import com.aurora.store.Constants;
import com.aurora.store.model.FilterModel;
import com.aurora.store.util.PrefUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;

public class FilterManager {

    public static FilterModel getFilterPreferences(Context context) {
        final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        final FilterModel filterModel = gson.fromJson(PrefUtil
                .getString(context, Constants.PREFERENCE_FILTER_APPS), FilterModel.class);
        if (filterModel == null) {
            FilterModel defaultModel = new FilterModel();
            PrefUtil.putString(context, Constants.PREFERENCE_FILTER_APPS, gson.toJson(defaultModel));
            return defaultModel;
        } else
            return filterModel;
    }

    public static void saveFilterPreferences(Context context, FilterModel filterModel) {
        final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        final String filterJSONString = gson.toJson(filterModel);
        PrefUtil.putString(context, Constants.PREFERENCE_FILTER_APPS, filterJSONString);
    }
}
