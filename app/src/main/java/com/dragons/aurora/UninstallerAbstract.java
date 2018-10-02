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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.dragons.aurora.model.App;

import timber.log.Timber;

import static com.dragons.aurora.recievers.DetailsInstallReceiver.ACTION_UNINSTALL_PACKAGE_FAILED;

public abstract class UninstallerAbstract {

    protected Context context;
    protected boolean background;

    public UninstallerAbstract(Context context) {
        Timber.i("Uninstaller chosen");
        this.context = context;
        background = !(context instanceof Activity);
    }

    abstract protected void uninstall(App app);

    public void setBackground(boolean background) {
        this.background = background;
    }

    public void verifyAndUninstall(App app) {
        if (verify(app)) {
            Timber.i("Uninstalling %s", app.getPackageName());
            uninstall(app);
        } else {
            sendBroadcast(app.getPackageName(), false);
        }
    }

    protected boolean verify(App app) {
        return app.isInstalled();
    }

    protected void sendBroadcast(String packageName, boolean success) {
        Intent intent = new Intent(
                success
                        ? Intent.ACTION_UNINSTALL_PACKAGE
                        : ACTION_UNINSTALL_PACKAGE_FAILED);
        intent.setData(new Uri.Builder().scheme("package").opaquePart(packageName).build());
        context.sendBroadcast(intent);
    }
}
