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