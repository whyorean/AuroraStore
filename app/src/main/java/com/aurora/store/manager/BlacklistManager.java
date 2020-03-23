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
import java.util.ArrayList;
import java.util.List;

public class BlacklistManager {

    private Context context;
    private Gson gson;

    public BlacklistManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void addToBlacklist(String packageName) {
        List<String> stringList = getBlacklistedPackages();
        if (!stringList.contains(packageName)) {
            stringList.add(packageName);
            saveBlacklist(stringList);
        }
    }

    public void removeFromBlacklist(String packageName) {
        List<String> stringList = getBlacklistedPackages();
        if (stringList.contains(packageName)) {
            stringList.remove(packageName);
            saveBlacklist(stringList);
        }
    }

    public boolean isBlacklisted(String packageName) {
        return getBlacklistedPackages().contains(packageName);
    }

    public void clear() {
        saveBlacklist(new ArrayList<>());
    }

    private void saveBlacklist(List<String> stringList) {
        PrefUtil.putString(context, Constants.PREFERENCE_BLACKLIST_PACKAGE_LIST, gson.toJson(stringList));
    }

    public List<String> getBlacklistedPackages() {
        String rawList = PrefUtil.getString(context, Constants.PREFERENCE_BLACKLIST_PACKAGE_LIST);
        Type type = new TypeToken<List<String>>() {
        }.getType();
        List<String> stringList = gson.fromJson(rawList, type);

        if (stringList == null)
            return new ArrayList<>();
        else
            return stringList;
    }
}
