/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
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

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.aurora.store.Constants;
import com.aurora.store.model.App;

import java.io.File;

public class PathUtil {

    static public File getApkPath(String packageName, int version) {
        String filename = packageName + "." + String.valueOf(version) + ".apk";
        return new File(getRootApkCopyPath(), filename);
    }

    static public String getRootApkPath(Context context) {
        if (isCustomPath(context))
            return PrefUtil.getString(context, Constants.PREFERENCE_DOWNLOAD_DIRECTORY);
        else
            return getBaseDirectory(context);
    }

    static public String getRootApkCopyPath() {
        return getBaseCopyDirectory();
    }

    static public String getLocalApkPath(Context context, App app) {
        return getLocalApkPath(context, app.getPackageName(), app.getVersionCode());
    }

    static public String getLocalSplitPath(Context context, App app, String tag) {
        return getLocalSplitPath(context, app.getPackageName(), app.getVersionCode(), tag);
    }

    static public String getLocalApkPath(Context context, String packageName, int versionCode) {
        return getRootApkPath(context) + "/" + packageName + "." + versionCode + ".apk";
    }

    static private String getLocalSplitPath(Context context, String packageName, int versionCode, String tag) {
        return getRootApkPath(context) + "/" + packageName + "." + versionCode + "." + tag + ".apk";
    }

    static private boolean isCustomPath(Context context) {
        return (!getCustomPath(context).isEmpty());
    }

    static public String getCustomPath(Context context) {
        return PrefUtil.getString(context, Constants.PREFERENCE_DOWNLOAD_DIRECTORY);
    }

    public static boolean checkBaseDirectory(Context context) {
        boolean success = new File(getRootApkPath(context)).exists();
        return success || createBaseDirectory(context);
    }

    public static boolean createBaseDirectory(Context context) {
        return new File(getRootApkPath(context)).mkdir();
    }

    static public String getBaseDirectory(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return context.getFilesDir().getPath();
        } else
            return Environment.getExternalStorageDirectory().getPath() + "/Aurora";
    }

    static public String getBaseCopyDirectory() {
        return Environment.getExternalStorageDirectory().getPath() + "/Aurora/Copy/APK";
    }
}
