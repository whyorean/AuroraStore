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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.model.App;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.TextUtil;
import com.aurora.store.utility.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Installer implements AppInstallerAbstract.InstallationStatusListener {

    private Context context;
    private Map<String, App> appHashMap = new HashMap<>();
    private AppInstallerAbstract packageInstaller;

    public Installer(Context context) {
        this.context = context;
        packageInstaller = getInstallationMethod(context.getApplicationContext());
    }

    public AppInstallerAbstract getPackageInstaller() {
        return packageInstaller;
    }

    public void install(App app) {
        appHashMap.put(app.getPackageName(), app);
        String packageName = app.getPackageName();
        int versionCode = app.getVersionCode();

        if (Util.isNativeInstallerEnforced(context))
            install(packageName, versionCode);
        else
            installSplit(packageName, versionCode);
    }

    public void install(String packageName, int versionCode) {
        Log.i("Native Installer Called");
        Intent intent;
        File file = new File(PathUtil.getLocalApkPath(context, packageName, versionCode));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(FileProvider.getUriForFile(context, "com.aurora.store.fileProvider", file));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void installSplit(String packageName, int versionCode) {
        Log.i("Split Installer Called");
        List<File> apkFiles = new ArrayList<>();
        File apkDirectory = new File(PathUtil.getRootApkPath(context));
        for (File splitApk : apkDirectory.listFiles()) {
            if (splitApk.getPath().contains(new StringBuilder()
                    .append(packageName)
                    .append(".")
                    .append(versionCode))) {
                apkFiles.add(splitApk);
            }
        }

        packageInstaller.addInstallationStatusListener((status, intentPackageName) -> {
            final String statusMessage = getStatusString(status);
            final App app = appHashMap.get(intentPackageName);
            final String displayName = (app != null)
                    ? TextUtil.emptyIfNull(app.getDisplayName())
                    : TextUtil.emptyIfNull(intentPackageName);

            Log.i("Package Installer -> %s : %s", displayName, TextUtil.emptyIfNull(statusMessage));

            if (app != null)
                clearNotification(app);

            switch (status) {
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    QuickNotification.show(
                            context,
                            displayName,
                            statusMessage,
                            getContentIntent(intentPackageName));
                    break;
                case PackageInstaller.STATUS_SUCCESS:
                    QuickNotification.show(
                            context,
                            displayName,
                            statusMessage,
                            getContentIntent(intentPackageName));
                    if (app != null) {
                        clearInstallationFiles(app);
                        appHashMap.remove(intentPackageName);
                    }
                    break;
            }
        });
        packageInstaller.installApkFiles(apkFiles);
    }

    private void clearInstallationFiles(@NonNull App app) {
        boolean success = false;
        File apkDirectory = new File(PathUtil.getRootApkPath(context));
        for (File file : apkDirectory.listFiles()) {
            if (file.getName().contains(app.getPackageName() + "." + app.getVersionCode())) {
                success = file.delete();
            }
        }
        if (success)
            Log.i("Installation files deleted");
        else
            Log.i("Could not delete installation files");
    }

    private void clearNotification(App app) {
        NotificationManager notificationManager = (NotificationManager)
                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(app.getPackageName().hashCode());
    }

    private PendingIntent getContentIntent(String packageName) {
        Intent intent = new Intent(context, DetailsActivity.class);
        intent.putExtra("INTENT_PACKAGE_NAME", packageName);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private AppInstallerAbstract getInstallationMethod(Context context) {
        String prefValue = PrefUtil.getString(context, Constants.PREFERENCE_INSTALLATION_METHOD);
        switch (prefValue) {
            case "0":
                return AppInstaller.getInstance(context);
            case "1":
                return AppInstallerRooted.getInstance(context);
            case "2":
                return AppInstallerPrivileged.getInstance(context);
            default:
                return AppInstaller.getInstance(context);
        }
    }

    private String getStatusString(int status) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE:
                return context.getString(R.string.installer_status_failure);
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                return context.getString(R.string.installer_status_failure_aborted);
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                return context.getString(R.string.installer_status_failure_blocked);
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return context.getString(R.string.installer_status_failure_conflict);
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                return context.getString(R.string.installer_status_failure_incompatible);
            case PackageInstaller.STATUS_FAILURE_INVALID:
                return context.getString(R.string.installer_status_failure_invalid);
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                return context.getString(R.string.installer_status_failure_storage);
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                return context.getString(R.string.installer_status_user_action);
            case PackageInstaller.STATUS_SUCCESS:
                return context.getString(R.string.installer_status_success);
            default:
                return context.getString(R.string.installer_status_unknown);
        }
    }

    @Override
    public void onStatusChanged(int status, @Nullable String packageName) {

    }
}
