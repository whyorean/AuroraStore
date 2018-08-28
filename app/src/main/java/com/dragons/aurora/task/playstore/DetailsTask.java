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

import android.content.pm.PackageManager;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.AppBuilder;
import com.dragons.aurora.model.ReviewBuilder;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.GooglePlayException;

import java.io.IOException;

public class DetailsTask extends PlayStorePayloadTask<App> {

    protected String packageName;

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    protected void processIOException(IOException e) {
        if (e instanceof GooglePlayException && ((GooglePlayException) e).getCode() == 404) {
            ContextUtil.toast(this.context, R.string.details_not_available_on_play_store);
        }
    }

    @Override
    protected App getResult(GooglePlayAPI api, String... arguments) throws IOException {
        DetailsResponse response = api.details(packageName);
        App app = AppBuilder.build(response.getDocV2());
        if (response.hasUserReview()) {
            app.setUserReview(ReviewBuilder.build(response.getUserReview()));
        }
        PackageManager pm = context.getPackageManager();
        try {
            app.getPackageInfo().applicationInfo = pm.getApplicationInfo(packageName, 0);
            app.getPackageInfo().versionCode = pm.getPackageInfo(packageName, 0).versionCode;
            app.setInstalled(true);
        } catch (PackageManager.NameNotFoundException e) {
            // App is not installed
        }
        return app;
    }

    @Override
    protected App doInBackground(String... arguments) {
        return super.doInBackground(arguments);
    }
}
