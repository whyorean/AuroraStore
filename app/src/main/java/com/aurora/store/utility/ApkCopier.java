/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
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
 *
 *
 */

package com.aurora.store.utility;

import com.aurora.store.model.App;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ApkCopier {

    private App app;

    public ApkCopier(App app) {
        this.app = app;
    }

    public boolean copy() {
        File destination = new File(PathUtil.getRootApkCopyPath() + "/" +
                app.getPackageName() + "." + app.getVersionCode() + ".apk");
        File destinationDirectory = destination.getParentFile();

        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }

        if (destination.exists()) {
            Log.i("%s already backed up", destination.toString());
            return true;
        }

        File baseApk = getBaseApk();
        if (baseApk == null)
            return false;

        String[] splitSourceDirs = app.getPackageInfo().applicationInfo.splitSourceDirs;

        if (splitSourceDirs != null && splitSourceDirs.length > 0) {
            List<File> allApkList = getInstalledSplitApks();
            allApkList.add(baseApk);
            bundleAllApks(allApkList);
        } else {
            copy(baseApk, destination);
        }
        return true;
    }

    private void copy(File input, File output) {
        try {
            IOUtils.copy(new FileInputStream(input), new FileOutputStream(output));
        } catch (IOException e) {
            Log.e("Error copying APK : %s", e.getMessage());
        }
    }

    private File getBaseApk() {
        if (null != app.getPackageInfo() && null != app.getPackageInfo().applicationInfo) {
            return new File(app.getPackageInfo().applicationInfo.sourceDir);
        }
        return null;
    }

    private List<File> getInstalledSplitApks() {
        List<File> fileList = new ArrayList<>();
        String[] splitSourceDirs = app.getPackageInfo().applicationInfo.splitSourceDirs;
        if (app.getPackageInfo() != null
                && app.getPackageInfo().applicationInfo != null
                && splitSourceDirs != null) {
            for (String fileName : splitSourceDirs)
                fileList.add(new File(fileName));
        }
        return fileList;
    }

    private void bundleAllApks(List<File> fileList) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(
                    PathUtil.getRootApkCopyPath() + "/" + app.getPackageName() + ".zip");
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            for (File split : fileList) {
                ZipEntry zipEntry = new ZipEntry(split.getName());
                zipOutputStream.putNextEntry(zipEntry);
                IOUtils.copy(new FileInputStream(split), zipOutputStream);
                zipOutputStream.closeEntry();
            }
            zipOutputStream.close();
        } catch (Exception e) {
            Log.e("ApkCopier : %s", e.getMessage());
        }
    }
}
