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

package com.aurora.store.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.manager.BitmapManager;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.GooglePlayException;
import com.dragons.aurora.playstoreapiv2.SearchSuggestEntry;

import java.io.File;

public class AuroraSuggestionProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return SearchManager.SUGGEST_MIME_TYPE;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        MatrixCursor cursor = new MatrixCursor(new String[]{
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                SearchManager.SUGGEST_COLUMN_ICON_1
        });
        try {
            fill(cursor, uri);
        } catch (GooglePlayException e) {
            if (e.getCode() == 401 && PrefUtil.getBoolean(getContext(), Accountant.DUMMY_ACCOUNT)) {
                refreshAndRetry(cursor, uri);
            } else {
                Log.e(e.getMessage());
            }
        } catch (Throwable e) {
            Log.e(e.getMessage());
        }
        return cursor;
    }

    private void refreshAndRetry(MatrixCursor cursor, Uri uri) {
        try {
            PlayStoreApiAuthenticator.refreshToken(getContext());
            fill(cursor, uri);
        } catch (Throwable e) {
            Log.e(e.getMessage());
        }
    }

    private void fill(MatrixCursor cursor, Uri uri) throws Exception {
        String query = uri.getLastPathSegment();
        if (TextUtils.isEmpty(query) || query.equals("search_suggest_query")) {
            return;
        }
        int i = 0;
        GooglePlayAPI api = PlayStoreApiAuthenticator.getApi(getContext());
        for (SearchSuggestEntry entry : api.searchSuggest(query).getEntryList()) {
            cursor.addRow(constructRow(entry, i++));
        }
    }

    private Object[] constructRow(SearchSuggestEntry entry, int id) {
        return entry.getType() == GooglePlayAPI.SEARCH_SUGGESTION_TYPE.APP.value ?
                constructAppRow(entry, id) : constructSuggestionRow(entry, id);
    }

    private Object[] constructAppRow(SearchSuggestEntry entry, int id) {
        File file = new BitmapManager(getContext()).downloadAndGetFile(entry.getImageContainer().getImageUrl());
        return new Object[]{id, entry.getTitle(), entry.getPackageNameContainer().getPackageName(),
                null != file ? Uri.fromFile(file) : R.drawable.ic_placeholder};
    }

    private Object[] constructSuggestionRow(SearchSuggestEntry entry, int id) {
        return new Object[]{id, entry.getSuggestedQuery(), entry.getSuggestedQuery(), R.drawable.ic_round_search};
    }
}
