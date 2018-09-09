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

package com.dragons.aurora.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.SearchAppsFragment;

import java.util.regex.Pattern;

import timber.log.Timber;

public class SearchActivity extends AuroraActivity {

    private String query;

    static protected boolean actionIs(Intent intent, String action) {
        return null != intent && null != intent.getAction() && intent.getAction().equals(action);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_alt);
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String newQuery = getQuery(intent);
        if (looksLikeAPackageId(newQuery)) {
            Timber.i("Following search suggestion to app page: %s", newQuery);
            startActivity(DetailsActivity.getDetailsIntent(this, newQuery));
            finish();
            return;
        }

        Timber.i("Searching: %s", newQuery);
        if (null != newQuery && !newQuery.equals(this.query)) {
            this.query = newQuery;
            setTitle(getTitleString());
            getCategoryApps(query, getTitleString());
        }
    }

    private String getTitleString() {
        return query.startsWith(Aurora.PUB_PREFIX)
                ? getString(R.string.apps_by, query.substring(Aurora.PUB_PREFIX.length()))
                : getString(R.string.activity_title_search, query)
                ;
    }

    private String getQuery(Intent intent) {
        if (intent.getScheme() != null
                && (intent.getScheme().equals("market")
                || intent.getScheme().equals("http")
                || intent.getScheme().equals("https"))) {
            return intent.getData().getQueryParameter("q");
        }
        if (actionIs(intent, Intent.ACTION_SEARCH)) {
            return intent.getStringExtra(SearchManager.QUERY);
        } else if (actionIs(intent, Intent.ACTION_VIEW)) {
            return intent.getDataString();
        }
        return null;
    }

    private boolean looksLikeAPackageId(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        String pattern = "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+[\\p{L}_$][\\p{L}\\p{N}_$]*";
        Pattern r = Pattern.compile(pattern);
        return r.matcher(query).matches();
    }

    public void getCategoryApps(String query, String title) {
        SearchAppsFragment searchAppsFragment = new SearchAppsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("SearchQuery", query);
        arguments.putString("SearchTitle", title);
        searchAppsFragment.setArguments(arguments);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, searchAppsFragment).commit();
    }
}
