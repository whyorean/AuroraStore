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

import com.aurora.store.Constants;
import com.aurora.store.model.CategoryModel;
import com.aurora.store.util.PrefUtil;
import com.dragons.aurora.playstoreapiv2.DocV2;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.ListResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryListTask {

    private Context context;
    private GooglePlayAPI api;

    public CategoryListTask(Context context, GooglePlayAPI api) {
        this.context = context;
        this.api = api;
    }

    public boolean getResult() throws Exception {
        api.setLocale(getLocale(context));

        ListResponse response = api.categoriesList();
        buildAllCategories(response, Constants.CATEGORY_APPS);

        response = api.categoriesList(Constants.CATEGORY_GAME);
        buildAllCategories(response, Constants.CATEGORY_GAME);

        response = api.categoriesList(Constants.CATEGORY_FAMILY);
        buildAllCategories(response, Constants.CATEGORY_FAMILY);

        return true;
    }

    private void buildAllCategories(ListResponse response, String categoryPrefId) {
        List<CategoryModel> categoryModels = new ArrayList<>();
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
                CategoryModel categoryModel = new CategoryModel(categoryId, category.getTitle(), category.getImage(0).getImageUrl());
                categoryModels.add(categoryModel);
            }
        }
        Gson gson = new Gson();
        String jsonString = gson.toJson(categoryModels);
        PrefUtil.putString(context, categoryPrefId, jsonString);
    }

    private Locale getLocale(Context context) {
        String locale = PrefUtil.getString(context, Constants.PREFERENCE_SPOOF_LOCALE);
        if (TextUtils.isEmpty(locale))
            return Locale.getDefault();
        else
            return new Locale(locale);
    }
}