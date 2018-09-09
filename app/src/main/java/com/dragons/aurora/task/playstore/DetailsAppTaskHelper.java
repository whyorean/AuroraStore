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

import android.content.Context;
import android.content.pm.PackageManager;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.AppBuilder;
import com.dragons.aurora.model.ReviewBuilder;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;
import com.dragons.aurora.playstoreapiv2.GooglePlayException;

import java.io.IOException;

import timber.log.Timber;

public class DetailsAppTaskHelper extends ExceptionTask {

    private App app;

    public DetailsAppTaskHelper(Context context) {
        super(context);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public App getResult(String packageName) {
        try {
            api = getApi();
            DetailsResponse response = api.details(packageName);
            app = AppBuilder.build(response);
            if (response.hasUserReview()) {
                app.setUserReview(ReviewBuilder.build(response.getUserReview()));
            }
            if (context != null) {
                PackageManager pm = context.getPackageManager();
                app.getPackageInfo().applicationInfo = pm.getApplicationInfo(packageName, 0);
                app.getPackageInfo().versionCode = pm.getPackageInfo(packageName, 0).versionCode;
                app.setInstalled(true);
            }
        } catch (IOException e) {
            processException(e);
            Timber.e(e.getMessage());
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e.getMessage());
        }
        return app;
    }

    @Override
    protected void processIOException(IOException e) {
        if (e instanceof GooglePlayException && ((GooglePlayException) e).getCode() == 404) {
            ContextUtil.toast(context, R.string.details_not_available_on_play_store);
        }
    }
}