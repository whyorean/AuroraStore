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

package com.aurora.store.task;

import android.content.Context;
import android.content.ContextWrapper;

import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.model.App;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseTask extends ContextWrapper {

    protected Context context;
    protected GooglePlayAPI api;

    public BaseTask(Context context) {
        super(context);
        this.context = context;
    }

    public GooglePlayAPI getApi() throws Exception {
        return PlayStoreApiAuthenticator.getApi(context);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public List<App> filterGoogleApps(List<App> apps) {
        Set<String> gAppsSet = new HashSet<>();
        gAppsSet.add("com.chrome.beta");
        gAppsSet.add("com.chrome.canary");
        gAppsSet.add("com.chrome.dev");
        gAppsSet.add("com.android.chrome");
        gAppsSet.add("com.niksoftware.snapseed");
        gAppsSet.add("com.google.toontastic");

        List<App> appList = new ArrayList<>();
        for (App app : apps) {
            if (!app.getPackageName().startsWith("com.google") && !gAppsSet.contains(app.getPackageName())) {
                appList.add(app);
            }
        }
        return appList;
    }
}