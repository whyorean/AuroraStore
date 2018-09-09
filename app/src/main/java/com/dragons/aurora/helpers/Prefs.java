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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

public class Prefs {

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
        return getStringSet(getDefaultSharedPreferences(context), key);
    }

    public static Set<String> getStringSet(SharedPreferences preferences, String key) {
        try {
            return preferences.getStringSet(key, new HashSet<>());
        } catch (ClassCastException e) {
            return getStringSetCompat(preferences, key);
        }
    }

    public static Set<String> getStringSetCompat(SharedPreferences preferences, String key) {
        return new HashSet<>(Arrays.asList(TextUtils.split(
                preferences.getString(key, ""),
                DELIMITER
        )));
    }


    public static void putListString(Context context,String key, ArrayList<String> stringList) {
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }

    static public void putStringSet(Context context, String key, Set<String> set) {
        putStringSet(getDefaultSharedPreferences(context), key, set);
    }

    static public void putStringSet(SharedPreferences preferences, String key, Set<String> set) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(key, set).apply();
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

    public static ArrayList<String> getListString(Context context,String key) {
        return new ArrayList<String>(Arrays.asList(TextUtils.split(
                PreferenceManager.getDefaultSharedPreferences(context).getString(key, ""), "‚‗‚")));
    }

}
