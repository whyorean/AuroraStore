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

import android.content.Context;
import android.view.View;

import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.activities.ManualDownloadActivity;
import com.dragons.aurora.model.App;
import com.dragons.aurora.recievers.DetailsDownloadReceiver;
import com.dragons.aurora.recievers.DetailsInstallReceiver;

public class DownloadOrInstall extends Abstract {

    private DetailsDownloadReceiver downloadReceiver;
    private DetailsInstallReceiver installReceiver;

    public DownloadOrInstall(Context context, View view, App app) {
        super(context, view, app);
    }

    @Override
    public void draw() {
        new ButtonUninstall(context, view, app).draw();
        new ButtonCancel(context, view, app).draw();
        new ButtonDownload(context, view, app).draw();
        new ButtonInstall(context, view, app).draw();
        new ButtonRun(context, view, app).draw();
        new ButtonRedirect(context, view, app).draw();
    }

    public void download() {
        new ButtonDownload(context, view, app).download();
    }

    public void unregisterReceivers() {
        context.unregisterReceiver(downloadReceiver);
        downloadReceiver = null;
        context.unregisterReceiver(installReceiver);
        installReceiver = null;
    }

    public void registerReceivers() {
        if (null == downloadReceiver) {
            if (context instanceof ManualDownloadActivity)
                downloadReceiver = new DetailsDownloadReceiver((ManualDownloadActivity) context, app.getPackageName());
            else
                downloadReceiver = new DetailsDownloadReceiver((DetailsActivity) context, app.getPackageName());
        }
        if (null == installReceiver) {
            if (context instanceof ManualDownloadActivity)

                installReceiver = new DetailsInstallReceiver((ManualDownloadActivity) context, app.getPackageName());
            else
                installReceiver = new DetailsInstallReceiver((DetailsActivity) context, app.getPackageName());
        }
    }
}
