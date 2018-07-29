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

import android.app.NotificationManager;
import android.content.Context;
import android.view.View;

import com.dragons.aurora.InstallationState;
import com.dragons.aurora.Paths;
import com.dragons.aurora.R;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.InstallTask;

public class ButtonInstall extends Button {

    public ButtonInstall(Context context, View view, App app) {
        super(context, view, app);
    }

    @Override
    public void draw() {
        super.draw();
        ((android.widget.Button) button).setText(R.string.details_install);
        if (InstallationState.isInstalling(app.getPackageName())) {
            disable(R.string.details_installing);
        }
    }

    @Override
    protected android.widget.Button getButton() {
        return (android.widget.Button) view.findViewById(R.id.install);
    }

    @Override
    protected boolean shouldBeVisible() {
        return Paths.getApkPath(context, app.getPackageName(), app.getVersionCode()).exists()
                && DownloadState.get(app.getPackageName()).isEverythingSuccessful()
                ;
    }

    @Override
    protected void onButtonClick(View v) {
        disable(R.string.details_installing);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(app.getDisplayName().hashCode());
        new InstallTask(context, app).execute();
    }
}
