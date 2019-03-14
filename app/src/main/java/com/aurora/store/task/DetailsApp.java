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
import android.content.pm.PackageManager;

import com.aurora.store.model.App;
import com.aurora.store.model.AppBuilder;
import com.aurora.store.model.ReviewBuilder;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;

public class DetailsApp extends BaseTask {

    private App app;

    public DetailsApp(Context context) {
        super(context);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public App getInfo(String packageName) throws Exception {
        api = getApi();
        DetailsResponse response = api.details(packageName);
        app = AppBuilder.build(response);
        if (response.hasUserReview()) {
            app.setUserReview(ReviewBuilder.build(response.getUserReview()));
        }
        if (context != null) {
            try {
                PackageManager pm = context.getPackageManager();
                app.getPackageInfo().applicationInfo = pm.getApplicationInfo(packageName, 0);
                app.getPackageInfo().versionCode = pm.getPackageInfo(packageName, 0).versionCode;
                app.setInstalled(true);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return app;
    }


}