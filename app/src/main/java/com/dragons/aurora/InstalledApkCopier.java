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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

public class InstalledApkCopier {

    static public boolean copy(Context context, App app) {
        File destination = Paths.getApkPath(context, app.getPackageName(), app.getInstalledVersionCode());
        if (destination.exists()) {
            Timber.i("%s exists", destination.toString());
            return true;
        }
        File currentApk = getCurrentApk(app);
        if (null == currentApk) {
            Timber.e("applicationInfo.sourceDir is empty");
            return false;
        }
        if (!currentApk.exists()) {
            Timber.e("%s does not exist", currentApk);
            return false;
        }
        return copy(currentApk, destination);
    }

    static public File getCurrentApk(App app) {
        if (null != app.getPackageInfo() && null != app.getPackageInfo().applicationInfo) {
            return new File(app.getPackageInfo().applicationInfo.sourceDir);
        }
        return null;
    }

    static private boolean copy(File input, File output) {
        InputStream in = null;
        OutputStream out = null;
        File dir = output.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            in = new FileInputStream(input);
            out = new FileOutputStream(output);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            out.flush();
            return true;
        } catch (IOException e) {
            Timber.e(e.getClass().getName() + " " + e.getMessage());
            return false;
        } finally {
            Util.closeSilently(in);
            Util.closeSilently(out);
        }
    }
}
