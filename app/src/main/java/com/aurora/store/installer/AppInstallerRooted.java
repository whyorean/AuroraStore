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

import com.aurora.store.utility.Log;
import com.aurora.store.utility.Root;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppInstallerRooted extends AppInstaller {

    private static AppInstallerRooted instance;
    private static Root root;

    AppInstallerRooted(Context context) {
        super(context);
        instance = this;
    }

    public static AppInstaller getInstance(Context context) {
        if (instance == null) {
            synchronized (AppInstaller.class) {
                if (instance == null) {
                    instance = new AppInstallerRooted(context);
                    root = new Root();
                }
            }
        }
        return instance;
    }

    @Override
    protected void installApkFiles(List<File> apkFiles) {
        try {
            final String packageName = apkFiles.get(0).getName();
            if (root.isTerminated() || !root.isAcquired()) {
                root = new Root();
                if (!root.isAcquired()) {
                    Log.e("Root not available");
                    return;
                }
            }

            int totalSize = 0;
            for (File apkFile : apkFiles)
                totalSize += apkFile.length();

            String result = ensureCommandSucceeded(root.exec(String.format(Locale.getDefault(),
                    "pm install-create -i com.android.vending -r -S %d",
                    totalSize)));

            Pattern sessionIdPattern = Pattern.compile("(\\d+)");
            Matcher sessionIdMatcher = sessionIdPattern.matcher(result);
            boolean found = sessionIdMatcher.find();
            int sessionId = Integer.parseInt(sessionIdMatcher.group(1));

            for (File apkFile : apkFiles)
                ensureCommandSucceeded(root.exec(String.format(Locale.getDefault(),
                        "cat \"%s\" | pm install-write -S %d %d \"%s\"",
                        apkFile.getAbsolutePath(),
                        apkFile.length(),
                        sessionId,
                        apkFile.getName())));

            result = ensureCommandSucceeded(root.exec(String.format(Locale.getDefault(),
                    "pm install-commit %d ",
                    sessionId)));

            if (result.toLowerCase().contains("success"))
                dispatchSessionUpdate(android.content.pm.PackageInstaller.STATUS_SUCCESS, packageName);
            else
                dispatchSessionUpdate(android.content.pm.PackageInstaller.STATUS_FAILURE, packageName);
        } catch (Exception e) {
            Log.w(e.getMessage());
        }
    }

    private String ensureCommandSucceeded(String result) {
        if (result == null || result.length() == 0)
            throw new RuntimeException(root.readError());
        return result;
    }
}
