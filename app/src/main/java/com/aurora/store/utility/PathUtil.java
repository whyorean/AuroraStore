package com.aurora.store.utility;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.aurora.store.Constants;
import com.aurora.store.model.App;

import java.io.File;

public class PathUtil {

    static public String getRootApkPath(Context context) {
        if (isCustomPath(context))
            return PrefUtil.getString(context, Constants.PREFERENCE_DOWNLOAD_DIRECTORY);
        else
            return getBaseDirectory(context);
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
}
