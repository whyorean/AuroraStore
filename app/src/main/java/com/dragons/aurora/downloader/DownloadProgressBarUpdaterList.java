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

package com.dragons.aurora.downloader;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dragons.aurora.Util;
import com.dragons.aurora.task.RepeatingTask;
import com.dragons.aurora.view.UpdatableAppBadge;

public class DownloadProgressBarUpdaterList extends RepeatingTask {

    private Context context;
    private String packageName;
    private UpdatableAppBadge updatableAppBadge;

    public DownloadProgressBarUpdaterList(Context context, UpdatableAppBadge updatableAppBadge) {
        this.context = context;
        this.packageName = updatableAppBadge.getApp().getPackageName();
        this.updatableAppBadge = updatableAppBadge;
    }

    @Override
    protected boolean shouldRunAgain() {
        DownloadState state = DownloadState.get(packageName);
        return null != state && !state.isEverythingFinished();
    }

    @Override
    protected void payload() {
        ProgressBar progressBar = updatableAppBadge.progressBar;
        TextView progressCents = updatableAppBadge.progressCents;

        if (null == progressBar) {
            return;
        }

        DownloadState state = DownloadState.get(packageName);
        if (null == state || state.isEverythingFinished()) {
            progressBar.setVisibility(View.GONE);
            progressCents.setVisibility(View.GONE);
            progressBar.setIndeterminate(true);

            if (Util.isAlreadyDownloaded(context, updatableAppBadge.getApp())) {
                updatableAppBadge.install.setVisibility(View.VISIBLE);
                updatableAppBadge.cancel.setVisibility(View.GONE);
            } else if (Util.shouldDownload(context, updatableAppBadge.getApp())
                    && !updatableAppBadge.isDownloading) {
                updatableAppBadge.update.setVisibility(View.VISIBLE);
                updatableAppBadge.cancel.setVisibility(View.GONE);
            }
            return;
        }

        Pair<Float, Float> progress = state.getProgress();

        progressBar.setVisibility(View.VISIBLE);
        progressCents.setVisibility(View.VISIBLE);

        progressBar.setIndeterminate(false);
        progressBar.setMax(100);

        progressBar.setProgress(getPercentage(progress.first, progress.second));
        progressCents.setText(String.valueOf(progressBar.getProgress()) + "%");
    }

    private int getPercentage(float cur, float total) {
        if (total != 0)
            return (int) ((cur * 100) / total);
        else
            return 0;
    }

}
