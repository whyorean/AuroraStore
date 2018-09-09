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

package com.dragons.aurora.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.dragons.aurora.AuroraPermissionManager;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.DetailsTask;
import com.dragons.aurora.task.playstore.PurchaseTask;

import timber.log.Timber;

public class DirectDownloadActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String packageName = getIntentPackageName();
        if (null == packageName) {
            finish();
            return;
        }
        if (!new AuroraPermissionManager(this).checkPermission()) {
            startActivity(DetailsActivity.getDetailsIntent(this, packageName));
            finish();
            return;
        }
        Timber.i("Getting package %s", packageName);

        DetailsAndPurchaseTask task = new DetailsAndPurchaseTask();
        task.setPackageName(packageName);
        task.setContext(this);
        task.execute();
        finish();
    }

    private String getIntentPackageName() {
        Intent intent = getIntent();
        if (!intent.hasExtra(Intent.EXTRA_TEXT) || TextUtils.isEmpty(intent.getStringExtra(Intent.EXTRA_TEXT))) {
            Timber.tag(getClass().getSimpleName()).w("Intent does not have %s", Intent.EXTRA_TEXT);
            return null;
        }
        try {
            return Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)).getQueryParameter("id");
        } catch (UnsupportedOperationException e) {
            Timber.tag(getClass().getSimpleName()).w("Could not parse URI " + intent.getStringExtra(Intent.EXTRA_TEXT) + ": " + e.getMessage());
            return null;
        }
    }

    static class DetailsAndPurchaseTask extends DetailsTask {

        @Override
        protected void onPostExecute(App app) {
            if (success()) {
                getPurchaseTask(app).execute();
            } else {
                context.startActivity(DetailsActivity.getDetailsIntent(context, packageName));
            }
        }

        private PurchaseTask getPurchaseTask(App app) {
            PurchaseTask task = new PurchaseTask();
            task.setApp(app);
            task.setContext(context);
            return task;
        }
    }
}
