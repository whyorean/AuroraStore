package com.aurora.store.installer;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Installer {

    private Context context;

    public Installer(Context context) {
        this.context = context;
    }

    public void install(App app) {
        installSplit(app.getPackageName(), app.getVersionCode());
    }

    /*
     * Native install method is now deprecated in favour of split installer,
     * Why does ot still exist in source ? I might reuse it xD
     * @param packageName
     * @param versionCode
     *
     */

    @Deprecated
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

        SplitPackageInstallerAbstract installer = getInstallationMethod(context);
        long sessionID = installer.createInstallationSession(apkFiles);
        installer.startInstallationSession(sessionID);
        installer.addStatusListener((installationID, installationStatus, packageNameOrErrorDescription) -> {
            switch (installationStatus) {
                case INSTALLATION_FAILED:
                    clearNotification(packageName);
                    new QuickNotification(context).show(context.getString(R.string.app_name),
                            String.format(Locale.getDefault(),
                                    context.getString(R.string.notification_installation_failed),
                                    packageName));
                    unregisterReceiver(installer);
                    break;
                case INSTALLING:
                    clearNotification(packageName);
                    new QuickNotification(context).show(context.getString(R.string.app_name),
                            String.format(Locale.getDefault(),
                                    context.getString(R.string.notification_installation_progress),
                                    packageName));
                    break;
                case INSTALLATION_SUCCEED:
                    new QuickNotification(context).show(context.getString(R.string.app_name),
                            String.format(Locale.getDefault(),
                                    context.getString(R.string.notification_installation_complete),
                                    packageName));
                    if (Util.shouldDeleteApk(context))
                        clearInstallationFiles(apkFiles);
                    unregisterReceiver(installer);
                    break;
            }
        });
    }

    private SplitPackageInstallerAbstract getInstallationMethod(Context context) {
        String prefValue = PrefUtil.getString(context, Constants.PREFERENCE_INSTALLATION_METHOD);
        switch (prefValue) {
            case "0":
                return new SplitPackageInstaller(context);
            case "1":
                return new SplitPackageInstallerRooted(context);
            default:
                return new SplitPackageInstaller(context);
        }
    }

    private void clearNotification(String packageName) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(packageName.hashCode());
    }

    private void clearInstallationFiles(List<File> apkFiles) {
        boolean success = false;
        for (File file : apkFiles)
            success = file.delete();
        if (success)
            Log.i("Installation files deleted");
        else
            Log.i("Could not delete installation files");
    }

    private void unregisterReceiver(SplitPackageInstallerAbstract mInstaller) {
        context.unregisterReceiver(mInstaller.getBroadcastReceiver());
    }
}
