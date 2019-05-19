/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
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

package com.aurora.store.api;

import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.HashMap;
import java.util.Map;

public class CategoryAppsIterator2 extends AppListIterator2 {

    private String categoryId;

    public CategoryAppsIterator2(GooglePlayAPI googlePlayApi, String categoryId, GooglePlayAPI.SUBCATEGORY subcategory) {
        super(googlePlayApi);
        this.categoryId = categoryId;
        String url = GooglePlayAPI.LIST_URL;
        Map<String, String> params = new HashMap<String, String>();
        params.put("cat", categoryId);
        params.put("ctr", subcategory.value);
        params.put("c", "3");
        firstPageUrl = googlePlayApi.getClient().buildUrl(url, params);
    }

    public String getCategoryId() {
        return categoryId;
    }
}