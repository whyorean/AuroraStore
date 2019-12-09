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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BlacklistManager {

    private Context context;
    private ArrayList<String> blackList;

    public BlacklistManager(Context context) {
        this.context = context;
        this.blackList = PrefUtil.getListString(context, Constants.PREFERENCE_BLACKLIST_APPS_LIST);
    }

    public void add(String packageName) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(packageName);
        addAll(arrayList);
        save();
    }

    public void addAll(ArrayList<String> arrayList) {
        blackList.addAll(arrayList);
        Set<String> mAppSet = new HashSet<>(blackList);
        blackList.clear();
        blackList.addAll(mAppSet);
        save();
    }

    public ArrayList<String> get() {
        return blackList;
    }

    public boolean contains(String packageName) {
        return blackList.contains(packageName);
    }

    public void remove(String packageName) {
        blackList.remove(packageName);
        save();
    }

    public void removeAll(ArrayList<String> packageList) {
        blackList.removeAll(packageList);
        save();
    }

    public void removeAll() {
        blackList = new ArrayList<>();
        save();
    }

    private void save() {
        PrefUtil.putListString(context, Constants.PREFERENCE_BLACKLIST_APPS_LIST, blackList);
    }
}
