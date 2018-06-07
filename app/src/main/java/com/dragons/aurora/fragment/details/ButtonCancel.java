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

package com.dragons.aurora.fragment.details;

import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.model.App;
import com.dragons.aurora.notification.CancelDownloadService;
import com.percolate.caffeine.ViewUtils;

public class ButtonCancel extends Button {

    ButtonCancel(AuroraActivity activity, App app) {
        super(activity, app);
    }

    @Override
    protected android.widget.Button getButton() {
        return (android.widget.Button) activity.findViewById(R.id.cancel);
    }

    @Override
    protected boolean shouldBeVisible() {
        return !DownloadState.get(app.getPackageName()).isEverythingFinished();
    }

    @Override
    protected void onButtonClick(View button) {
        Intent intentCancel = new Intent(activity.getApplicationContext(), CancelDownloadService.class);
        intentCancel.putExtra(CancelDownloadService.PACKAGE_NAME, app.getPackageName());
        activity.startService(intentCancel);
        button.setVisibility(View.GONE);

        android.widget.Button buttonDownload = ViewUtils.findViewById(activity, R.id.download);
        ProgressBar progressBar = ViewUtils.findViewById(activity, R.id.download_progress);
        TextView progressCents = ViewUtils.findViewById(activity, R.id.progressCents);

        buttonDownload.setVisibility(View.VISIBLE);
        buttonDownload.setEnabled(true);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        progressCents.setVisibility(View.GONE);
    }
}
