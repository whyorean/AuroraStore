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

package com.dragons.aurora;

import android.content.Context;
import android.preference.PreferenceManager;

import com.dragons.aurora.helpers.Prefs;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CategoryManager {
    private Context context;
    private SharedPreferencesTranslator translator;

    public CategoryManager(Context context) {
        this.context = context;
        translator = new SharedPreferencesTranslator(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public String getCategoryName(String categoryId) {
        if (null == categoryId) {
            return "";
        }
        if (categoryId.equals(Aurora.TOP)) {
            return context.getString(R.string.search_filter);
        }
        return translator.getString(categoryId);
    }

    public void save(String parent, Map<String, String> categories) {
        Prefs.putStringSet(context, parent, categories.keySet());
        for (String categoryId : categories.keySet()) {
            translator.putString(categoryId, categories.get(categoryId));
        }
    }

    public boolean fits(String appCategoryId, String chosenCategoryId) {
        return null == chosenCategoryId
                || chosenCategoryId.equals(Aurora.TOP)
                || appCategoryId.equals(chosenCategoryId)
                || Prefs.getStringSet(context, chosenCategoryId).contains(appCategoryId)
                ;
    }

    public boolean categoryListEmpty() {
        Set<String> topSet = Prefs.getStringSet(context, Aurora.TOP);
        if (topSet.isEmpty()) {
            return true;
        }
        int size = topSet.size();
        String categoryId = topSet.toArray(new String[size])[size - 1];
        return translator.getString(categoryId).equals(categoryId);
    }

    public Map<String, String> getCategoriesFromSharedPreferences() {
        Map<String, String> categories = new TreeMap<>();
        Set<String> topSet = Prefs.getStringSet(context, Aurora.TOP);
        for (String topCategoryId : topSet) {
            categories.put(topCategoryId, translator.getString(topCategoryId));
        }
        //return Util.sort(categories);
        return categories;
    }
}
