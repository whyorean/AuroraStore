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

package com.dragons.aurora;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.GooglePlayException;
import com.dragons.aurora.playstoreapiv2.SearchSuggestEntry;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class AuroraSuggestionProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return SearchManager.SUGGEST_MIME_TYPE;
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
            if (e.getCode() == 401
                    && PreferenceFragment.getBoolean(getContext(), Aurora.PREFERENCE_APP_PROVIDED_EMAIL)
            ) {
                refreshAndRetry(cursor, uri);
            } else {
                Timber.e(Aurora.TAG, e.getMessage());
            }
        } catch (Throwable e) {
            Timber.e(Aurora.TAG, e.getMessage());
        }
        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private void refreshAndRetry(MatrixCursor cursor, Uri uri) {
        try {
            new PlayStoreApiAuthenticator(getContext()).refreshToken();
            fill(cursor, uri);
        } catch (Throwable e) {
            Timber.e(Aurora.TAG, e.getMessage());
        }
    }

    private void fill(MatrixCursor cursor, Uri uri) throws IOException {
        String query = uri.getLastPathSegment();
        if (TextUtils.isEmpty(query) || query.equals("search_suggest_query")) {
            return;
        }
        int i = 0;
        for (SearchSuggestEntry entry : new PlayStoreApiAuthenticator(getContext()).getApi().searchSuggest(query).getEntryList()) {
            cursor.addRow(constructRow(entry, i++));
        }
    }

    private Object[] constructRow(SearchSuggestEntry entry, int id) {
        return entry.getType() == GooglePlayAPI.SEARCH_SUGGESTION_TYPE.APP.value ? constructAppRow(entry, id) : constructSuggestionRow(entry, id);
    }

    private Object[] constructAppRow(SearchSuggestEntry entry, int id) {
        File file = new BitmapManager(getContext()).downloadAndGetFile(entry.getImageContainer().getImageUrl());
        return new Object[]{id, entry.getTitle(), entry.getPackageNameContainer().getPackageName(), null != file ? Uri.fromFile(file) : R.drawable.ic_placeholder};
    }

    private Object[] constructSuggestionRow(SearchSuggestEntry entry, int id) {
        return new Object[]{id, entry.getSuggestedQuery(), entry.getSuggestedQuery(), R.drawable.ic_search};
    }
}
