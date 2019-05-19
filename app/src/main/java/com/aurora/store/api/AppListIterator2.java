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

import com.dragons.aurora.playstoreapiv2.DocV2;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.IteratorGooglePlayException;
import com.dragons.aurora.playstoreapiv2.ListResponse;
import com.dragons.aurora.playstoreapiv2.Payload;
import com.dragons.aurora.playstoreapiv2.SearchResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

abstract public class AppListIterator2 implements Iterator {

    protected GooglePlayAPI googlePlayApi;
    protected boolean firstQuery = true;
    protected String firstPageUrl;
    protected String nextPageUrl;

    public AppListIterator2(GooglePlayAPI googlePlayApi) {
        setGooglePlayApi(googlePlayApi);
    }

    public void setGooglePlayApi(GooglePlayAPI googlePlayApi) {
        this.googlePlayApi = googlePlayApi;
    }

    public List<DocV2> next() {
        Payload payload;
        DocV2 rootDoc;
        try {
            payload = getPayload();
            rootDoc = getRootDoc(payload);
            this.firstQuery = false;
        } catch (IOException e) {
            throw new IteratorGooglePlayException(e);
        }
        nextPageUrl = findNextPageUrl(payload);
        if (null == nextPageUrl && null != rootDoc) {
            nextPageUrl = findNextPageUrl(rootDoc);
        }
        if (nextPageStartsFromZero()) {
            return next();
        }
        if (null != rootDoc) {
            return rootDoc.getChildList();
        } else {
            return new ArrayList<DocV2>();
        }
    }

    public boolean hasNext() {
        return this.firstQuery || (null != this.nextPageUrl && this.nextPageUrl.length() > 0);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    protected Payload getPayload() throws IOException {
        String url;
        if (firstQuery && null != firstPageUrl) {
            url = firstPageUrl;
        } else if (null != nextPageUrl && nextPageUrl.length() > 0) {
            url = nextPageUrl;
        } else {
            throw new NoSuchElementException();
        }
        return googlePlayApi.genericGet(url, null);
    }

    protected String findNextPageUrl(Payload payload) {
        if (null == payload) {
            return null;
        }
        if (payload.hasSearchResponse()) {
            return findNextPageUrl(payload.getSearchResponse());
        } else if (payload.hasListResponse()) {
            return findNextPageUrl(payload.getListResponse());
        }
        return null;
    }

    protected String findNextPageUrl(SearchResponse searchResponse) {
        if (searchResponse.hasNextPageUrl()) {
            return GooglePlayAPI.FDFE_URL + searchResponse.getNextPageUrl();
        } else if (searchResponse.getDocCount() > 0) {
            return findNextPageUrl(searchResponse.getDoc(0));
        }
        return null;
    }

    protected String findNextPageUrl(ListResponse listResponse) {
        if (listResponse.getDocCount() > 0) {
            return findNextPageUrl(listResponse.getDoc(0));
        }
        return null;
    }

    protected String findNextPageUrl(DocV2 rootDoc) {
        if (rootDoc.hasContainerMetadata() && rootDoc.getContainerMetadata().hasNextPageUrl()) {
            return GooglePlayAPI.FDFE_URL + rootDoc.getContainerMetadata().getNextPageUrl();
        }
        if (rootDoc.hasRelatedLinks()
                && rootDoc.getRelatedLinks().hasUnknown1()
                && rootDoc.getRelatedLinks().getUnknown1().hasUnknown2()
                && rootDoc.getRelatedLinks().getUnknown1().getUnknown2().hasNextPageUrl()
        ) {
            return GooglePlayAPI.FDFE_URL + rootDoc.getRelatedLinks().getUnknown1().getUnknown2().getNextPageUrl();
        }
        for (DocV2 child : rootDoc.getChildList()) {
            if (!isRootDoc(child)) {
                continue;
            }
            String nextPageUrl = findNextPageUrl(child);
            if (null != nextPageUrl) {
                return nextPageUrl;
            }
        }
        return null;
    }

    /**
     * Sometimes not a list of apps is returned by search, but a list of content types (music and apps, for example)
     * each of them having a list of items
     * In this case we have to find the apps list and return it
     */
    protected DocV2 getRootDoc(Payload payload) {
        if (null == payload) {
            return null;
        }
        if (payload.hasSearchResponse() && payload.getSearchResponse().getDocCount() > 0) {
            return getRootDoc(payload.getSearchResponse().getDoc(0));
        } else if (payload.hasListResponse() && payload.getListResponse().getDocCount() > 0) {
            return getRootDoc(payload.getListResponse().getDoc(0));
        }
        return null;
    }

    protected DocV2 getRootDoc(DocV2 doc) {
        if (isRootDoc(doc)) {
            return doc;
        }
        for (DocV2 child : doc.getChildList()) {
            DocV2 root = getRootDoc(child);
            if (null != root) {
                return root;
            }
        }
        return null;
    }

    protected boolean isRootDoc(DocV2 doc) {
        return doc.getChildCount() > 0 && doc.getChild(0).getBackendId() == 3 && doc.getChild(0).getDocType() == 1;
    }

    private boolean nextPageStartsFromZero() {
        if (null == nextPageUrl) {
            return false;
        }
        try {
            return new URI(nextPageUrl).getQuery().contains("o=0");
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
