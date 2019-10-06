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

import com.aurora.store.model.App;
import com.aurora.store.model.AppBuilder;
import com.aurora.store.utility.PackageUtil;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;

public class DetailsApp extends BaseTask {

    public DetailsApp(Context context) {
        super(context);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public App getInfo(String packageName) throws Exception {
        api = getApi();
        DetailsResponse response = api.details(packageName);
        App app = AppBuilder.build(response);
        if (PackageUtil.isInstalled(context, app))
            app.setInstalled(true);
        return app;
    }
}