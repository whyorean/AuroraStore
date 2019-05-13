package com.aurora.store.api;

import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

public class UrlIterator2 extends AppListIterator2 {

    public UrlIterator2(GooglePlayAPI googlePlayApi) {
        super(googlePlayApi);
    }

    public UrlIterator2(GooglePlayAPI googlePlayApi, String firstPageUrl) {
        this(googlePlayApi);
        if (!firstPageUrl.startsWith(GooglePlayAPI.FDFE_URL)) {
            firstPageUrl = GooglePlayAPI.FDFE_URL + firstPageUrl;
        }
        this.firstPageUrl = firstPageUrl;
    }
}
