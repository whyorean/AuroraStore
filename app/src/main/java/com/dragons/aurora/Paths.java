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
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

public class Paths {
    static public File getStorageRoot(Context context) {
        File storageRoot = Environment.getExternalStorageDirectory();
        File[] externalFilesDirs = getExternalFilesDirs(context);
        if (externalFilesDirs.length < 2 || Environment.isExternalStorageEmulated(storageRoot)
                && !Environment.isExternalStorageRemovable(storageRoot)) {
            return storageRoot;
        }
        for (File file : externalFilesDirs) {
            try {
                if (Environment.isExternalStorageEmulated(file) && !Environment.isExternalStorageRemovable(file)) {
                    return new File(file.getAbsolutePath().replace(Aurora.FALLBACK_DIRECTORY, ""));
                }
            } catch (IllegalArgumentException e) {
                // The checks throw exceptions if Environment.getStorageVolume(File path) returns null
                // It would be great to know why
            }
        }
        return storageRoot;
    }

    static public File getDownloadPath(Context context) {
        return new File(
                getStorageRoot(context),
                PreferenceManager.getDefaultSharedPreferences(context).getString(Aurora.PREFERENCE_DOWNLOAD_DIRECTORY, "")
        );
    }

    static public File getApkPath(Context context, String packageName, int version) {
        String filename = packageName + "." + String.valueOf(version) + ".apk";
        return new File(getDownloadPath(context), filename);
    }

    static public File getDeltaPath(Context context, String packageName, int version) {
        return new File(getApkPath(context, packageName, version).getAbsolutePath() + ".delta");
    }

    static public File getObbPath(String packageName, int version, boolean main) {
        File obbDir = new File(new File(Environment.getExternalStorageDirectory(), "Android/obb"), packageName);
        String filename = (main ? "main" : "patch") + "." + String.valueOf(version) + "." + packageName + ".obb";
        return new File(obbDir, filename);
    }

    static private File[] getExternalFilesDirs(Context context) {
        return context.getExternalFilesDirs(null);
    }
}
