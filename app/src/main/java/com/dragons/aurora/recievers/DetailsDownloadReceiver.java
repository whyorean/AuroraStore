/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
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

package com.dragons.aurora.recievers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.activities.ManualDownloadActivity;
import com.dragons.aurora.downloader.DownloadManagerFactory;
import com.dragons.aurora.downloader.DownloadManagerInterface;
import com.dragons.aurora.downloader.DownloadReceiver;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.helpers.Prefs;

import java.lang.ref.WeakReference;

public class DetailsDownloadReceiver extends DownloadReceiver {

    private WeakReference<Activity> activityRef;
    private String packageName;

    public DetailsDownloadReceiver(DetailsActivity activity, String packageName) {
        activityRef = new WeakReference<>(activity);
        this.packageName = packageName;
        activity.registerReceiver(this, getFilter());
    }

    public DetailsDownloadReceiver(ManualDownloadActivity activity, String packageName) {
        activityRef = new WeakReference<>(activity);
        this.packageName = packageName;

        activity.registerReceiver(this, getFilter());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Activity activity;
        if (context instanceof ManualDownloadActivity)
            activity = (ManualDownloadActivity) activityRef.get();
        else
            activity = (DetailsActivity) activityRef.get();

        if (!ContextUtil.isAlive(activity)) {
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
        if (actionIs(intent, DownloadManagerInterface.ACTION_DOWNLOAD_COMPLETE)
                && isDelta(state.getApp())) {
            return;
        }
        state.setFinished(downloadId);
        if (DownloadManagerFactory.get(context).success(downloadId)
                && !actionIs(intent, DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED)) {
            state.setSuccessful(downloadId);
        }
        if (!state.isEverythingFinished()) {
            return;
        }
        draw(context, state);
    }

    private IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DELTA_PATCHING_COMPLETE);
        filter.addAction(DownloadManagerInterface.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadManagerInterface.ACTION_DOWNLOAD_CANCELLED);
        return filter;
    }

    private void draw(Context context, DownloadState state) {
        cleanup();
        if (!state.isEverythingSuccessful()) {
            return;
        }

        ViewSwitcher mViewSwitcher = activityRef.get().findViewById(R.id.viewSwitcher);
        LinearLayout action_layout = activityRef.get().findViewById(R.id.view1);
        LinearLayout progress_layout = activityRef.get().findViewById(R.id.view2);
        Button buttonDownload = activityRef.get().findViewById(R.id.download);
        Button buttonInstall = activityRef.get().findViewById(R.id.install);

        if (mViewSwitcher.getCurrentView() == progress_layout)
            mViewSwitcher.showPrevious();

        buttonDownload.setVisibility(View.GONE);
        buttonInstall.setVisibility(View.VISIBLE);
        if (Prefs.getBoolean(context, Aurora.PREFERENCE_AUTO_INSTALL)
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
        Button buttonCancel = activityRef.get().findViewById(R.id.cancel);
        Button buttonDownload = activityRef.get().findViewById(R.id.download);
        if (null != buttonCancel && buttonDownload != null) {
            buttonCancel.setVisibility(View.GONE);
            buttonDownload.setEnabled(true);
        }
    }
}
