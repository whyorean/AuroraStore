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

import com.dragons.aurora.model.App;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import timber.log.Timber;

public class InstallerRoot extends InstallerBackground {

    public InstallerRoot(Context context) {
        super(context);
    }

    @Override
    protected void install(App app) {
        InstallationState.setInstalling(app.getPackageName());
        boolean success = shellInstall(Paths.getApkPath(context, app.getPackageName(), app.getVersionCode()).toString());
        if (success) {
            InstallationState.setSuccess(app.getPackageName());
        } else {
            InstallationState.setFailure(app.getPackageName());
        }
        sendBroadcast(app.getPackageName(), true);
        postInstallationResult(app, success);
    }

    private boolean shellInstall(String file) {
        List<String> lines = Shell.SU.run("pm install -i \"" + BuildConfig.APPLICATION_ID + "\" -r " + file);
        if (null != lines) {
            for (String line : lines) {
                Timber.i(line);
            }
        }
        return null != lines && lines.size() == 1 && lines.get(0).equals("Success");
    }
}