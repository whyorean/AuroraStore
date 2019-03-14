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
import com.aurora.store.R;
import com.aurora.store.SharedPreferencesTranslator;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CategoryManager {

    private static final String SUBCATEGORY_ID_GAMES = "GAME";
    private static final String SUBCATEGORY_ID_FAMILY = "FAMILY";

    private Context context;
    private SharedPreferencesTranslator translator;

    public CategoryManager(Context context) {
        this.context = context;
        translator = new SharedPreferencesTranslator(Util.getPrefs(context));
    }

    public String getCategoryName(String categoryId) {
        if (null == categoryId) {
            return "";
        }
        if (categoryId.equals(Constants.TOP)) {
            return context.getString(R.string.title_all_apps);
        }
        return translator.getString(categoryId);
    }

    public void save(String parent, Map<String, String> categories) {
        PrefUtil.putStringSet(context, parent, categories.keySet());
        for (String categoryId : categories.keySet()) {
            translator.putString(categoryId, categories.get(categoryId));
        }
    }

    public boolean fits(String appCategoryId, String chosenCategoryId) {
        return null == chosenCategoryId
                || chosenCategoryId.equals(Constants.TOP)
                || appCategoryId.equals(chosenCategoryId)
                || PrefUtil.getStringSet(context, chosenCategoryId).contains(appCategoryId);
    }

    public boolean categoryListEmpty() {
        Set<String> topSet = PrefUtil.getStringSet(context, Constants.TOP);
        if (topSet.isEmpty()) {
            return true;
        }
        int size = topSet.size();
        String categoryId = topSet.toArray(new String[size])[size - 1];
        return translator.getString(categoryId).equals(categoryId);
    }

    public Map<String, String> getAllCategories() {
        Map<String, String> categories = new TreeMap<>();
        Set<String> topSet = PrefUtil.getStringSet(context, Constants.TOP);
        for (String topCategoryId : topSet) {
            categories.put(topCategoryId, translator.getString(topCategoryId));
        }
        return categories;
    }

    public Map<String, String> getAllGames() {
        Map<String, String> games = new TreeMap<>();
        Set<String> topSet = PrefUtil.getStringSet(context, Constants.TOP);
        for (String topCategoryId : topSet) {
            Set<String> subSet = PrefUtil.getStringSet(context, topCategoryId);
            for (String subCategoryId : subSet) {
                if (subCategoryId.startsWith(SUBCATEGORY_ID_GAMES))
                    games.put(subCategoryId, games.get(topCategoryId) + " - " + translator.getString(subCategoryId));
            }

        }
        return games;
    }

    public Map<String, String> getAllFamily() {
        Map<String, String> family = new TreeMap<>();
        Set<String> topSet = PrefUtil.getStringSet(context, Constants.TOP);
        for (String topCategoryId : topSet) {
            Set<String> subSet = PrefUtil.getStringSet(context, topCategoryId);
            for (String subCategoryId : subSet) {
                if (subCategoryId.startsWith(SUBCATEGORY_ID_FAMILY))
                    family.put(subCategoryId, family.get(topCategoryId) + " - " + translator.getString(subCategoryId));
            }
        }
        return family;
    }

    public void clearAll() {
        Set<String> emptySet = new TreeSet<>();
        PrefUtil.putStringSet(context, Constants.TOP, emptySet);
    }
}
