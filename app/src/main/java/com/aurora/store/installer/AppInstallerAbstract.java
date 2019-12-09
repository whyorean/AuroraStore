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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

import lombok.Getter;

@Getter
public abstract class AppInstallerAbstract {

    private Context context;
    private BroadcastReceiver broadcastReceiver;
    private Handler handler = new Handler(Looper.getMainLooper());
    private InstallationStatusListener listener;

    AppInstallerAbstract(Context context) {
        this.context = context.getApplicationContext();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
                String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
                dispatchSessionUpdate(status, packageName);
            }
        };
    }

    void addInstallationStatusListener(InstallationStatusListener listener) {
        this.listener = listener;
    }

    void dispatchSessionUpdate(int status, String packageName) {
        handler.post(() -> {
            if (listener != null)
                listener.onStatusChanged(status, packageName);
        });
    }

    protected abstract void installApkFiles(String packageName, List<File> apkFiles);

    public interface InstallationStatusListener {
        void onStatusChanged(int status, @Nullable String packageName);
    }
}
