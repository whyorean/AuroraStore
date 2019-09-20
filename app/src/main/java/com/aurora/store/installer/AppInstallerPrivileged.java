/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.aurora.store.installer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;

import com.aurora.services.IPrivilegedCallback;
import com.aurora.services.IPrivilegedService;
import com.aurora.store.BuildConfig;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PackageUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppInstallerPrivileged extends AppInstallerAbstract {

    private static final int ACTION_INSTALL_REPLACE_EXISTING = 2;

    private static volatile AppInstallerPrivileged instance;

    private AppInstallerPrivileged(Context context) {
        super(context);
        instance = this;
    }

    public static AppInstallerPrivileged getInstance(Context context) {
        if (instance == null) {
            synchronized (AppInstallerPrivileged.class) {
                if (instance == null)
                    instance = new AppInstallerPrivileged(context);
            }
        }
        return instance;
    }

    public static boolean isServiceOnline(Context context) {
        if (!PackageUtil.isInstalled(context, Constants.SERVICE_PACKAGE)) {
            return false;
        }
        ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent serviceIntent = new Intent(Constants.PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(Constants.PRIVILEGED_EXTENSION_PACKAGE_NAME);
        try {
            context.getApplicationContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    @Override
    protected void installApkFiles(String packageName, List<File> apkFiles) {
        if (!isServiceOnline(getContext())) {
            ContextUtil.runOnUiThread(this::showDisconnectedServicesDialog);
            return;
        }

        List<Uri> uriList = new ArrayList<>();
        for (File file : apkFiles) {
            uriList.add(Uri.parse(file.getPath()));
        }

        ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                IPrivilegedService service = IPrivilegedService.Stub.asInterface(binder);
                IPrivilegedCallback callback = new IPrivilegedCallback.Stub() {
                    @Override
                    public void handleResult(String packageName, int returnCode) {
                        dispatchSessionUpdate(returnCode, packageName);
                    }
                };
                try {
                    service.installSplitPackage(
                            uriList,
                            ACTION_INSTALL_REPLACE_EXISTING,
                            BuildConfig.APPLICATION_ID,
                            callback
                    );
                } catch (RemoteException e) {
                    Log.e(getClass().getSimpleName(), "Connecting to privileged service failed");
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.e(getClass().getSimpleName(), "Disconnected from privileged service");
            }
        };

        Intent serviceIntent = new Intent(Constants.PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(Constants.PRIVILEGED_EXTENSION_PACKAGE_NAME);
        getContext().getApplicationContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void showDisconnectedServicesDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.action_installations);
        builder.setMessage(R.string.pref_install_mode_offline_services);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create();
        builder.show();
    }
}