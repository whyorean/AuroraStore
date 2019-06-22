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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static com.aurora.store.Constants.TAG;

public class SplitPackageInstaller extends SplitPackageInstallerAbstract {

    public SplitPackageInstaller(Context context) {
        super(context);
    }

    @Override
    protected void installApkFiles(List<File> apkFiles) {
        PackageInstaller packageInstaller = getContext().getPackageManager().getPackageInstaller();
        try {
            PackageInstaller.SessionParams sessionParams =
                    new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionID = packageInstaller.createSession(sessionParams);
            PackageInstaller.Session session = packageInstaller.openSession(sessionID);
            for (File apkFile : apkFiles) {
                InputStream inputStream = new FileInputStream(apkFile);
                OutputStream outputStream = session.openWrite(apkFile.getName(), 0, apkFile.length());
                IOUtils.copy(inputStream, outputStream);
                session.fsync(outputStream);
                inputStream.close();
                outputStream.close();
            }
            Intent callbackIntent = new Intent(getContext(), SplitService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getContext(), sessionID, callbackIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pendingIntent.getIntentSender());
            session.close();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }
}
