/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Raccoon 4
 * Copyright 2019 Patrick Ahlbrecht
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

import com.aurora.store.exception.CredentialsEmptyException;
import com.dragons.aurora.playstoreapiv2.DocV2;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.Payload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchIterator2 extends AppListIterator2 {

    public SearchIterator2(GooglePlayAPI googlePlayApi, String query) {
        super(googlePlayApi);
        String url = GooglePlayAPI.SEARCH_URL;
        Map<String, String> params = new HashMap<>();
        params.put("c", "3");
        params.put("q", query);
        firstPageUrl = googlePlayApi.getClient().buildUrl(url, params);
    }

    @Override
    public List<DocV2> next() {
        try {
            Payload payload = getPayload();
            DocV2 rootDoc = getRootDoc(payload);
            SearchResultParser searchEngineResultPage = new SearchResultParser(SearchResultParser.SEARCH);
            searchEngineResultPage.append(rootDoc);
            nextPageUrl = searchEngineResultPage.getNextPageUrl();
            firstQuery = false;
            return searchEngineResultPage.getDocList();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    @Override
    protected boolean isRootDoc(DocV2 doc) {
        return doc != null && doc.getBackendId() == 3;
    }
}

