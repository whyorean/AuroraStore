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

import com.aurora.store.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class AppInstaller extends AppInstallerAbstract {

    private static AppInstaller instance;

    public AppInstaller(Context context) {
        super(context);
        instance = this;
    }

    public static AppInstaller getInstance(Context context) {
        if (instance == null) {
            synchronized (AppInstaller.class) {
                if (instance == null)
                    instance = new AppInstaller(context);
            }
        }
        return instance;
    }

    @Override
    protected void installApkFiles(String packageName, List<File> apkFiles) {
        final PackageInstaller packageInstaller = getContext().getPackageManager().getPackageInstaller();
        try {
            final PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            final int sessionID = packageInstaller.createSession(sessionParams);
            final PackageInstaller.Session session = packageInstaller.openSession(sessionID);
            for (File apkFile : apkFiles) {
                final InputStream inputStream = new FileInputStream(apkFile);
                final OutputStream outputStream = session.openWrite(apkFile.getName(), 0, apkFile.length());
                IOUtils.copy(inputStream, outputStream);
                session.fsync(outputStream);
                inputStream.close();
                outputStream.close();
            }
            final Intent callbackIntent = new Intent(getContext(), InstallerService.class);
            final PendingIntent pendingIntent = PendingIntent.getService(
                    getContext(),
                    sessionID,
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pendingIntent.getIntentSender());
            session.close();
        } catch (Exception e) {
            Log.w(e.getMessage());
        }
    }
}
