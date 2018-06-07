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

package com.dragons.aurora;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.downloader.DownloadManagerFactory;
import com.dragons.aurora.downloader.DownloadManagerInterface;
import com.dragons.aurora.downloader.DownloadReceiver;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.fragment.PreferenceFragment;

import java.lang.ref.WeakReference;

public class DetailsDownloadReceiver extends DownloadReceiver {

    private WeakReference<DetailsActivity> activityRef = new WeakReference<>(null);
    private String packageName;

    public DetailsDownloadReceiver(DetailsActivity activity, String packageName) {
        activityRef = new WeakReference<>(activity);
        this.packageName = packageName;
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DELTA_PATCHING_COMPLETE);
        filter.addAction(DownloadManagerInterface.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED);
        activity.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        DetailsActivity activity = activityRef.get();
        if (null == activity || !ContextUtil.isAlive(activity)) {
            return;
        }
        if (null == state) {
            if (actionIs(intent, DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED)) {
                cleanup();
            }
        }
    }

    @Override
    protected void process(Context context, Intent intent) {
        if (!state.getApp().getPackageName().equals(packageName)) {
            return;
        }
        if (actionIs(intent, DownloadManagerInterface.ACTION_DOWNLOAD_COMPLETE) && isDelta(state.getApp())) {
            return;
        }
        state.setFinished(downloadId);
        if (DownloadManagerFactory.get(context).success(downloadId) && !actionIs(intent, DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED)) {
            state.setSuccessful(downloadId);
        }
        if (!state.isEverythingFinished()) {
            return;
        }
        draw(context, state);
    }

    private void draw(Context context, DownloadState state) {
        cleanup();
        if (!state.isEverythingSuccessful()) {
            return;
        }
        Button buttonDownload = (Button) activityRef.get().findViewById(R.id.download);
        buttonDownload.setVisibility(View.GONE);
        Button buttonInstall = (Button) activityRef.get().findViewById(R.id.install);
        buttonInstall.setVisibility(View.VISIBLE);
        if (PreferenceFragment.getBoolean(context, PreferenceFragment.PREFERENCE_AUTO_INSTALL)
                && !state.getTriggeredBy().equals(DownloadState.TriggeredBy.MANUAL_DOWNLOAD_BUTTON)
                ) {
            buttonInstall.setEnabled(false);
            buttonInstall.setText(R.string.details_installing);
        } else {
            buttonInstall.setEnabled(true);
            buttonInstall.setText(R.string.details_install);
        }
    }

    private void cleanup() {
        ProgressBar progressBar = (ProgressBar) activityRef.get().findViewById(R.id.download_progress);
        if (null != progressBar) {
            progressBar.setVisibility(View.GONE);
            progressBar.setProgress(0);
        }
        Button buttonCancel = (Button) activityRef.get().findViewById(R.id.cancel);
        if (null != buttonCancel) {
            buttonCancel.setVisibility(View.GONE);
        }
        Button buttonDownload = (Button) activityRef.get().findViewById(R.id.download);
        buttonDownload.setEnabled(true);
    }
}
