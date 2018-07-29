/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
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

package com.dragons.aurora.helpers;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Prefs {

    public static final String REFRESH_ASKED = "REFRESH_ASKED";
    public static final String LOGGED_IN = "LOGGED_IN";
    public static final String GOOGLE_ACC = "GOOGLE_ACC";
    public static final String DUMMY_ACC = "DUMMY_ACC";
    public static final String GOOGLE_NAME = "GOOGLE_NAME";
    public static final String GOOGLE_URL = "GOOGLE_URL";
    public static final String GOOGLE_EMAIL = "GOOGLE_EMAIL";
    public static final String GOOGLE_PASSWORD = "GOOGLE_PASSWORD";
    public static final String SEC_ACCOUNT = "SEC_ACCOUNT";
    public static final String USED_EMAILS_SET = "USED_EMAILS_SET";

    private static final String DELIMITER = ",";

    public static void putString(Context context, String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    public static void putInteger(Context context, String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
    }

    public static void putBoolean(Context context, String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    public static Set<String> getStringSet(Context context, String key) {
        return new HashSet<>(Arrays.asList(TextUtils.split(
                PreferenceManager.getDefaultSharedPreferences(context).getString(key, ""),
                DELIMITER
        )));
    }

    public static void putStringSet(Context context, String key, Set<String> set) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, TextUtils.join(DELIMITER, set)).apply();
    }

    public static int getInteger(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, 0);
    }

    public static Boolean getBoolean(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
    }

    public static String getString(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
    }

}
