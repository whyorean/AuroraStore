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

import android.content.Context;

import com.dragons.aurora.helpers.Prefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FavouriteListManager {

    private Context context;
    private ArrayList<String> favouriteList;

    public FavouriteListManager(Context context) {
        this.context = context;
        favouriteList = Prefs.getListString(context, Aurora.PREFERENCE_FAV_LIST);
    }

    public boolean add(String s) {
        boolean result = favouriteList.add(s);
        save();
        return result;
    }

    public boolean addAll(ArrayList<String> arrayList) {
        boolean result = favouriteList.addAll(arrayList);
        //-----------------Remove Dupes--------------------//
        Set<String> mAppSet = new HashSet<>(favouriteList);
        favouriteList.clear();
        favouriteList.addAll(mAppSet);
        //-------------------------------------------------//
        save();
        return result;
    }

    public ArrayList<String> get() {
        return favouriteList;
    }

    public boolean contains(String packageName) {
        return favouriteList.contains(packageName);
    }

    public boolean remove(String packageName) {
        boolean result = favouriteList.remove(packageName);
        save();
        return result;
    }

    private void save() {
        Prefs.putListString(context, Aurora.PREFERENCE_FAV_LIST, favouriteList);
    }
}
