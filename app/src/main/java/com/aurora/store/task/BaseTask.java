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

import java.io.IOException;
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


    public GooglePlayAPI getApi() throws IOException {
        return new PlayStoreApiAuthenticator(context).getApi();
    }

    public void setApi(GooglePlayAPI api) {
        this.api = api;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public List<App> filterGoogleApps(List<App> apps) {
        Set<String> shitSet = new HashSet<>();
        shitSet.add("com.chrome.beta");
        shitSet.add("com.chrome.canary");
        shitSet.add("com.chrome.dev");
        shitSet.add("com.android.chrome");
        shitSet.add("com.niksoftware.snapseed");
        shitSet.add("com.google.toontastic");

        List<App> mApps = new ArrayList<>();
        for (App app : apps) {
            if (!app.getPackageName().startsWith("com.google") && !shitSet.contains(app.getPackageName())) {
                mApps.add(app);
            }
        }
        return mApps;
    }

}