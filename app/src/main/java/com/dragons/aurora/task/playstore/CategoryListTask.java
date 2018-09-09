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
import android.text.TextUtils;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.playstoreapiv2.DocV2;
import com.dragons.aurora.playstoreapiv2.ListResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CategoryListTask extends ExceptionTask {

    public CategoryListTask(Context context) {
        super(context);
    }

    public boolean getResult() {
        CategoryManager categoryManager = new CategoryManager(context);
        try {
            api = getApi();
            api.setLocale(getLocale(context));
            Map<String, String> topCategories = buildCategoryMap(api.categoriesList());
            categoryManager.save(Aurora.TOP, topCategories);
            for (String categoryId : topCategories.keySet()) {
                categoryManager.save(categoryId, buildCategoryMap(api.categoriesList(categoryId)));
            }
        } catch (IOException e) {
            processException(e);
        }
        return true;
    }

    private Map<String, String> buildCategoryMap(ListResponse response) {
        Map<String, String> categories = new HashMap<>();
        for (DocV2 categoryCluster : response.getDoc(0).getChildList()) {
            if (!categoryCluster.getBackendDocid().equals("category_list_cluster")) {
                continue;
            }
            for (DocV2 category : categoryCluster.getChildList()) {
                if (!category.hasUnknownCategoryContainer()
                        || !category.getUnknownCategoryContainer().hasCategoryIdContainer()
                        || !category.getUnknownCategoryContainer().getCategoryIdContainer().hasCategoryId()
                ) {
                    continue;
                }
                String categoryId = category.getUnknownCategoryContainer().getCategoryIdContainer().getCategoryId();
                if (TextUtils.isEmpty(categoryId)) {
                    continue;
                }
                categories.put(categoryId, category.getTitle());
            }
        }
        return categories;
    }

    private Locale getLocale(Context context) {
        String locale = Prefs.getString(context, Aurora.PREFERENCE_REQUESTED_LANGUAGE);
        if (TextUtils.isEmpty(locale))
            return Locale.getDefault();
        else
            return new Locale(locale);
    }
}