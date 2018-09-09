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

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

public class DeltaPatcherGDiff extends DeltaPatcherAbstract {

    public DeltaPatcherGDiff(Context context, App app) {
        super(context, app);
    }

    @Override
    public boolean patch() {
        Timber.i("Preparing to apply delta patch to %s", app.getPackageName());
        File originalApk = InstalledApkCopier.getCurrentApk(app);
        if (null == originalApk || !originalApk.exists()) {
            Timber.e("Could not find existing apk to patch it: %s", originalApk);
            return false;
        }
        Timber.i("Patching with %s", patch);
        com.nothome.delta.GDiffPatcher patcher = new com.nothome.delta.GDiffPatcher();
        try {
            File destinationApk = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
            patcher.patch(originalApk, patch, destinationApk);
            Timber.i("Patching successfully completed");
            return true;
        } catch (IOException e) {
            Timber.e("Patching failed: " + e.getClass().getName() + " " + e.getMessage());
            return false;
        } finally {
            Timber.i("Deleting %s", patch);
            patch.delete();
        }
    }
}
