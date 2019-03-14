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
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.LongSparseArray;

import androidx.annotation.Nullable;

import com.aurora.store.InstallationStatus;
import com.aurora.store.utility.Log;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SplitPackageInstallerAbstract {

    private Context context;
    private BroadcastReceiver broadcastReceiver;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ExecutorService executorServiceMisc = Executors.newSingleThreadExecutor();
    private ArrayDeque<InstallerQueue> installerQueues = new ArrayDeque<>();
    private ArrayList<InstallationStatusListener> listenerArrayList = new ArrayList<>();
    private LongSparseArray<InstallerQueue> createdInstallationSessions = new LongSparseArray<>();
    private boolean installationInProgress;
    private long lastInstallationID = 1337;
    private InstallerQueue ongoingInstallation;

    protected SplitPackageInstallerAbstract(Context context) {
        this.context = context.getApplicationContext();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getIntExtra(SplitService.EXTRA_INSTALLATION_STATUS, -1)) {
                    case SplitService.STATUS_SUCCESS:
                        dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_SUCCEED,
                                intent.getStringExtra(SplitService.EXTRA_PACKAGE_NAME));
                        installationCompleted();
                        break;
                    case SplitService.STATUS_FAILURE:
                        dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED,
                                intent.getStringExtra(SplitService.EXTRA_ERROR_DESCRIPTION));
                        installationCompleted();
                        break;
                }
            }
        };
        context.registerReceiver(broadcastReceiver, new
                IntentFilter(SplitService.ACTION_INSTALLATION_STATUS_NOTIFICATION));
    }

    public BroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }

    public void setBroadcastReceiver(BroadcastReceiver furtherInstallationEventsReceiver) {
        this.broadcastReceiver = furtherInstallationEventsReceiver;
    }

    protected Context getContext() {
        return context;
    }

    public void addStatusListener(InstallationStatusListener listener) {
        listenerArrayList.add(listener);
    }

    public void removeStatusListener(InstallationStatusListener listener) {
        listenerArrayList.remove(listener);
    }

    public long createInstallationSession(List<File> apkFiles) {
        long installationID = lastInstallationID++;
        createdInstallationSessions.put(installationID, new InstallerQueue(getContext(), apkFiles, installationID));
        return installationID;
    }

    public long createInstallationSession(File zipWithApkFiles) {
        long installationID = lastInstallationID++;
        createdInstallationSessions.put(installationID, new InstallerQueue(getContext(), zipWithApkFiles, installationID));
        return installationID;
    }

    public void startInstallationSession(long sessionID) {
        InstallerQueue installation = createdInstallationSessions.get(sessionID);
        createdInstallationSessions.remove(sessionID);
        if (installation == null)
            return;

        installerQueues.addLast(installation);
        dispatchSessionUpdate(installation.getId(), InstallationStatus.QUEUED, null);
        processQueue();
    }

    public boolean isInstallationInProgress() {
        return installationInProgress;
    }

    private void processQueue() {
        if (installerQueues.size() == 0 || installationInProgress)
            return;

        InstallerQueue installation = installerQueues.removeFirst();
        ongoingInstallation = installation;
        installationInProgress = true;

        dispatchCurrentSessionUpdate(InstallationStatus.INSTALLING, null);
        executorService.execute(() -> {
            List<File> apkFiles;
            try {
                apkFiles = installation.getApkFiles();
            } catch (Exception e) {
                Log.w(e.getMessage());
                dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED, e.getMessage());
                installationCompleted();
                return;
            }
            installApkFiles(apkFiles);
        });
    }

    protected abstract void installApkFiles(List<File> apkFiles);

    protected void installationCompleted() {
        installationInProgress = false;
        InstallerQueue lastInstallation = ongoingInstallation;
        executorServiceMisc.submit(lastInstallation::clear);
        ongoingInstallation = null;
        processQueue();
    }

    protected void dispatchSessionUpdate(long sessionID, InstallationStatus status, String packageNameOrError) {
        handler.post(() -> {
            for (InstallationStatusListener listener : listenerArrayList)
                listener.onStatusChanged(sessionID, status, packageNameOrError);
        });
    }

    protected void dispatchCurrentSessionUpdate(InstallationStatus status, String packageNameOrError) {
        dispatchSessionUpdate(ongoingInstallation.getId(), status, packageNameOrError);
    }

    public interface InstallationStatusListener {
        void onStatusChanged(long installationID, InstallationStatus status, @Nullable String packageNameOrErrorDescription);
    }
}
