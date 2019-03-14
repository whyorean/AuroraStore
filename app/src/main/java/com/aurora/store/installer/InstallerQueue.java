/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Split APK Installer (SAI)
 * Copyright (C) 2018, Aefyr
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

package com.aurora.store.installer;

import android.content.Context;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class InstallerQueue {

    private Context mContext;
    private File mZipWithApkFiles;
    private boolean mShouldExtractZip;
    private List<File> mApkFiles;
    private File mCacheDirectory;
    private long mId;

    InstallerQueue(Context c, List<File> apkFiles, long id) {
        mContext = c;
        mApkFiles = apkFiles;
        mId = id;
    }

    InstallerQueue(Context c, File zipWithApkFiles, long id) {
        mContext = c;
        mZipWithApkFiles = zipWithApkFiles;
        mShouldExtractZip = true;
        mId = id;
    }

    long getId() {
        return mId;
    }

    List<File> getApkFiles() throws Exception {
        if (mShouldExtractZip)
            extractZip();
        return mApkFiles;
    }

    void clear() {
        if (mCacheDirectory != null) {
            deleteFile(mCacheDirectory);
        }
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles())
                deleteFile(child);
        }
        file.delete();
    }

    private void extractZip() throws Exception {
        createCacheDir();

        ZipFile zipFile = new ZipFile(mZipWithApkFiles);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        mApkFiles = new ArrayList<>(zipFile.size());

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.isDirectory() || !entry.getName().endsWith(".apk"))
                throw new IllegalArgumentException("No APKs in ZIP");

            File tempApkFile = new File(mCacheDirectory, entry.getName());

            FileOutputStream outputStream = new FileOutputStream(tempApkFile);
            InputStream inputStream = zipFile.getInputStream(entry);
            IOUtils.copy(inputStream, outputStream);

            outputStream.close();
            inputStream.close();

            mApkFiles.add(tempApkFile);
        }
        zipFile.close();
    }

    private void createCacheDir() {
        mCacheDirectory = new File(mContext.getCacheDir(), String.valueOf(System.currentTimeMillis()));
        mCacheDirectory.mkdirs();
    }
}
