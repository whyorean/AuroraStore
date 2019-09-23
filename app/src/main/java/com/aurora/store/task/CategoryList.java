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
import android.text.TextUtils;

import com.aurora.store.manager.CategoryManager;
import com.aurora.store.Constants;
import com.aurora.store.utility.PrefUtil;
import com.dragons.aurora.playstoreapiv2.DocV2;
import com.dragons.aurora.playstoreapiv2.ListResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CategoryList extends BaseTask {

    public CategoryList(Context context) {
        super(context);
    }

    public boolean getResult() throws Exception {
        CategoryManager categoryManager = new CategoryManager(context);
        api = getApi();
        api.setLocale(getLocale(context));
        Map<String, String> categoryMap = buildCategoryMap(api.categoriesList());
        categoryManager.save(Constants.TOP, categoryMap);
        for (String categoryId : categoryMap.keySet()) {
            categoryManager.save(categoryId, buildCategoryMap(api.categoriesList(categoryId)));
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
                        || !category.getUnknownCategoryContainer().getCategoryIdContainer().hasCategoryId()) {
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
        String locale = PrefUtil.getString(context, Constants.PREFERENCE_REQUESTED_LANGUAGE);
        if (TextUtils.isEmpty(locale))
            return Locale.getDefault();
        else
            return new Locale(locale);
    }
}