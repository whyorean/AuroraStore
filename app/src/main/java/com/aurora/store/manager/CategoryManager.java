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
import com.aurora.store.model.CategoryModel;
import com.aurora.store.util.PrefUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CategoryManager {

    private Context context;
    private Gson gson;

    public CategoryManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public static void clear(Context context) {
        PrefUtil.remove(context, Constants.CATEGORY_APPS);
        PrefUtil.remove(context, Constants.CATEGORY_GAME);
        PrefUtil.remove(context, Constants.CATEGORY_FAMILY);
    }

    public String getCategoryName(String categoryId) {
        if (null == categoryId) {
            return "";
        }
        if (categoryId.equals(Constants.TOP)) {
            return context.getString(R.string.title_all_apps);
        }
        return StringUtils.EMPTY;
    }

    public boolean fits(String appCategoryId, String chosenCategoryId) {
        return null == chosenCategoryId
                || chosenCategoryId.equals(Constants.TOP)
                || appCategoryId.equals(chosenCategoryId);
    }

    public boolean categoryListEmpty() {
        return PrefUtil.getString(context, Constants.CATEGORY_APPS).isEmpty();
    }

    public List<CategoryModel> getCategories(String categoryId) {
        return getCategoryById(categoryId);
    }

    public List<CategoryModel> getCategoryById(String categoryId) {
        Type type = new TypeToken<List<CategoryModel>>() {
        }.getType();
        String jsonString = PrefUtil.getString(context, categoryId);
        List<CategoryModel> categoryList = gson.fromJson(jsonString, type);
        if (categoryList == null || categoryList.isEmpty())
            return new ArrayList<>();
        else
            return categoryList;
    }
}
