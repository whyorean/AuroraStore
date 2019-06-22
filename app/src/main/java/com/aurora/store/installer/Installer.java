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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.core.content.FileProvider;

import com.aurora.services.IPrivilegedCallback;
import com.aurora.services.IPrivilegedService;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.events.AppInstalledEvent;
import com.aurora.store.events.RxBus;
import com.aurora.store.model.App;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Installer {

    private Context context;

    public Installer(Context context) {
        this.context = context;
    }

    public void install(App app) {
        if (Util.isNativeInstallerEnforced(context))
            install(app.getPackageName(), app.getVersionCode());
        else
            installSplit(app.getPackageName(), app.getVersionCode());
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

        SplitPackageInstallerAbstract installer = getInstallationMethod(context.getApplicationContext());
        context.getApplicationContext().registerReceiver(installer.getBroadcastReceiver(),
                new IntentFilter(SplitService.ACTION_INSTALLATION_STATUS_NOTIFICATION));
        long sessionID = installer.createInstallationSession(apkFiles);
        installer.startInstallationSession(sessionID);
        installer.addStatusListener((installationID, installationStatus, packageNameOrErrorDescription) -> {
            switch (installationStatus) {
                case INSTALLATION_FAILED:
                    clearNotification(packageName);
                    QuickNotification.show(
                            context,
                            PackageUtil.getDisplayName(context, packageName),
                            context.getString(R.string.notification_installation_failed),
                            getContentIntent(packageName));
                    unregisterReceiver(installer);
                    break;
                case INSTALLING:
                    clearNotification(packageName);
                    QuickNotification.show(
                            context,
                            PackageUtil.getDisplayName(context, packageName),
                            context.getString(R.string.notification_installation_progress),
                            getContentIntent(packageName));
                    break;
                case INSTALLATION_SUCCEED:
                    QuickNotification.show(
                            context,
                            PackageUtil.getDisplayName(context, packageName),
                            context.getString(R.string.notification_installation_complete),
                            getContentIntent(packageName));
                    if (Util.shouldDeleteApk(context))
                        clearInstallationFiles(apkFiles);
                    unregisterReceiver(installer);
                    RxBus.publish(new AppInstalledEvent(packageName));
                    break;
            }
        });
    }

    public void uninstall(App app) {
        String prefValue = PrefUtil.getString(context, Constants.PREFERENCE_INSTALLATION_METHOD);
        switch (prefValue) {
            case "0":
                uninstallByPackageManager(app);
                break;
            case "1":
                uninstallByRoot(app);
                break;
            case "2":
                uninstallByServices(app);
                break;
            default:
                uninstallByPackageManager(app);
        }
    }

    private void uninstallByServices(App app) {
        ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                IPrivilegedService service = IPrivilegedService.Stub.asInterface(binder);
                IPrivilegedCallback callback = new IPrivilegedCallback.Stub() {
                    @Override
                    public void handleResult(String packageName, int returnCode) {
                        Log.i("Uninstallation of " + packageName + " complete with code " + returnCode);
                    }
                };
                try {
                    if (!service.hasPrivilegedPermissions()) {
                        Log.e("service.hasPrivilegedPermissions() is false");
                        return;
                    }

                    service.deletePackage(app.getPackageName(), 1, callback);

                } catch (RemoteException e) {
                    Log.e("Connecting to privileged service failed");
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent serviceIntent = new Intent(Constants.PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(Constants.PRIVILEGED_EXTENSION_PACKAGE_NAME);
        context.getApplicationContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void uninstallByPackageManager(App app) {
        Uri uri = Uri.fromParts("package", app.getPackageName(), null);
        Intent intent = new Intent();
        intent.setData(uri);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            intent.setAction(Intent.ACTION_DELETE);
        } else {
            intent.setAction(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        }
        context.startActivity(intent);
    }

    private void uninstallByRoot(App app) {
        new PackageUninstallerRooted().uninstall(app);
    }

    private SplitPackageInstallerAbstract getInstallationMethod(Context context) {
        String prefValue = PrefUtil.getString(context, Constants.PREFERENCE_INSTALLATION_METHOD);
        switch (prefValue) {
            case "0":
                return new SplitPackageInstaller(context);
            case "1":
                return new SplitPackageInstallerRooted(context);
            case "2":
                return new SplitInstallerPrivileged(context);
            default:
                return new SplitPackageInstaller(context);
        }
    }

    private void clearNotification(String packageName) {
        NotificationManager notificationManager = (NotificationManager)
                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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
        try {
            context.getApplicationContext().unregisterReceiver(mInstaller.getBroadcastReceiver());
        } catch (Exception ignored) {
        }
    }

    private PendingIntent getContentIntent(String packageName) {
        Intent intent = new Intent(context, DetailsActivity.class);
        intent.putExtra("INTENT_PACKAGE_NAME", packageName);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
