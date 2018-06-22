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

import android.util.Pair;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dragons.aurora.task.RepeatingTask;

import java.lang.ref.WeakReference;

public class DownloadProgressBarUpdater extends RepeatingTask {

    private String packageName;
    private WeakReference<ProgressBar> progressBar = new WeakReference<>(null);
    private WeakReference<TextView> progressCents = new WeakReference<>(null);

    public DownloadProgressBarUpdater(String packageName, ProgressBar progressBar, TextView progressCents) {
        this.packageName = packageName;
        this.progressBar = new WeakReference<>(progressBar);
        this.progressCents = new WeakReference<>(progressCents);
    }

    @Override
    protected boolean shouldRunAgain() {
        DownloadState state = DownloadState.get(packageName);
        return null != state && !state.isEverythingFinished();
    }

    @Override
    protected void payload() {
        ProgressBar progressBar = this.progressBar.get();
        TextView progressCents = this.progressCents.get();
        if (null == progressBar) {
            return;
        }
        DownloadState state = DownloadState.get(packageName);
        if (null == state || state.isEverythingFinished()) {
            progressBar.setVisibility(View.INVISIBLE);
            progressCents.setVisibility(View.INVISIBLE);
            progressBar.setIndeterminate(true);
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
