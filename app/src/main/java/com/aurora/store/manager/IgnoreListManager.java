/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
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

package com.aurora.store.manager;

import android.content.Context;

import com.aurora.store.Constants;
import com.aurora.store.util.PrefUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;

public class IgnoreListManager {

    private Context context;
    private Gson gson;

    public IgnoreListManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void addToIgnoreList(String packageName, int versionCode) {
        final HashMap<String, Integer> ignoreMap = getIgnoreList();
        ignoreMap.remove(packageName);
        ignoreMap.put(packageName, versionCode);
        saveIgnoreList(ignoreMap);
    }

    public void removeFromIgnoreList(String packageName) {
        final HashMap<String, Integer> ignoreMap = getIgnoreList();
        ignoreMap.remove(packageName);
        saveIgnoreList(ignoreMap);
    }

    public boolean isIgnored(String packageName, Integer versionCode) {
        final HashMap<String, Integer> ignoreMap = getIgnoreList();
        if (ignoreMap.containsKey(packageName)) {
            Integer ignoredVersionCode = ignoreMap.get(packageName);
            return ignoredVersionCode != null && ignoredVersionCode.equals(versionCode);
        } else
            return false;
    }

    public void clear() {
        saveIgnoreList(new HashMap<>());
    }

    private void saveIgnoreList(HashMap<String, Integer> ignoreMap) {
        PrefUtil.putString(context, Constants.PREFERENCE_IGNORE_PACKAGE_LIST, gson.toJson(ignoreMap));
    }

    private HashMap<String, Integer> getIgnoreList() {
        String rawList = PrefUtil.getString(context, Constants.PREFERENCE_IGNORE_PACKAGE_LIST);
        Type type = new TypeToken<HashMap<String, Integer>>() {
        }.getType();

        HashMap<String, Integer> ignoreMap = gson.fromJson(rawList, type);
        if (ignoreMap == null)
            return new HashMap<>();
        else
            return ignoreMap;
    }
}
