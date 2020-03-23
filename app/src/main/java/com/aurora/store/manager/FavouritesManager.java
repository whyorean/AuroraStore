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

public class FavouritesManager {

    private Context context;
    private Gson gson;

    public FavouritesManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void addToFavourites(String packageName) {
        List<String> stringList = getFavouritePackages();
        if (!stringList.contains(packageName)) {
            stringList.add(packageName);
            saveFavourites(stringList);
        }
    }

    public void addToFavourites(List<String> packageNameList) {
        List<String> stringList = getFavouritePackages();
        for (String packageName : packageNameList){
            if (!stringList.contains(packageName)) {
                stringList.add(packageName);
            }
        }
        saveFavourites(stringList);
    }

    public void removeFromFavourites(String packageName) {
        List<String> stringList = getFavouritePackages();
        if (stringList.contains(packageName)) {
            stringList.remove(packageName);
            saveFavourites(stringList);
        }
    }

    public boolean isFavourite(String packageName) {
        return getFavouritePackages().contains(packageName);
    }

    public void clear() {
        saveFavourites(new ArrayList<>());
    }

    private void saveFavourites(List<String> stringList) {
        PrefUtil.putString(context, Constants.PREFERENCE_FAVOURITE_PACKAGE_LIST, gson.toJson(stringList));
    }

    public List<String> getFavouritePackages() {
        String rawList = PrefUtil.getString(context, Constants.PREFERENCE_FAVOURITE_PACKAGE_LIST);
        Type type = new TypeToken<List<String>>() {
        }.getType();
        List<String> stringList = gson.fromJson(rawList, type);

        if (stringList == null)
            return new ArrayList<>();
        else
            return stringList;
    }
}
