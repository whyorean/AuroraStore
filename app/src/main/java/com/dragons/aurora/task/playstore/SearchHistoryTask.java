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

package com.dragons.aurora.task.playstore;

import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.animation.AnimationUtils;

import com.dragons.aurora.R;
import com.dragons.aurora.adapters.RecyclerAppsAdapter;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.AppBuilder;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class SearchHistoryTask extends ExceptionTask {

    public Set<String> readFromPref(String Key) {
        Set<String> set = PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getStringSet(Key, null);
        if (set != null)
            return set;
        else
            return new HashSet<>();
    }

    public void writeToPref(String Key, Set<String> newAppSet) {
        PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .edit()
                .putStringSet(Key, newAppSet)
                .apply();
    }

    public void addHistory(String query) {
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String datedQuery = query + ":" + date;
        ArrayList<String> oldList = getHistoryList();
        oldList.add(datedQuery);
        Set<String> newSet = new HashSet<>();
        newSet.addAll(oldList);
        writeToPref("SEARCH_HISTORY", newSet);
    }

    public void addRecentApps(String packageName) {
        ArrayList<String> oldList = getAppHistoryList();
        oldList.add(packageName);
        Set<String> newSet = new HashSet<>();
        newSet.addAll(oldList);
        writeToPref("APP_HISTORY", newSet);
    }

    public ArrayList<String> getAppHistoryList() {
        Set<String> oldSet = readFromPref("APP_HISTORY");
        ArrayList<String> oldList = new ArrayList<>();
        oldList.addAll(oldSet);
        return oldList;
    }

    public ArrayList<String> getHistoryList() {
        Set<String> oldSet = readFromPref("SEARCH_HISTORY");
        ArrayList<String> oldList = new ArrayList<>();
        oldList.addAll(oldSet);
        return oldList;
    }

    public ArrayList<String> getRecentAppsList() {
        ArrayList<String> currList = new ArrayList<>();
        Set<String> savedAppSet = readFromPref("APP_HISTORY");
        currList.clear();
        currList.addAll(savedAppSet);
        return currList;
    }

    public List<App> getHistoryApps(GooglePlayAPI api, ArrayList<String> currList) throws IOException {
        List<App> apps = new ArrayList<>();
        for (String packageName : currList) {
            DetailsResponse response = api.details(packageName);
            App app = AppBuilder.build(response);
            apps.add(app);
        }
        return apps;
    }

    public void setupRecyclerView(RecyclerView recyclerView, List<App> appsToAdd) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_anim));
        recyclerView.setAdapter(new RecyclerAppsAdapter(getContext(), appsToAdd));
    }

    public boolean looksLikeAPackageId(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        String pattern = "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+[\\p{L}_$][\\p{L}\\p{N}_$]*";
        Pattern r = Pattern.compile(pattern);
        return r.matcher(query).matches();
    }

}
