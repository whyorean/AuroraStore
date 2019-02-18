package com.aurora.store.installer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;

import com.aurora.store.InstallationStatus;
import com.aurora.store.utility.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SplitPackageInstaller extends SplitPackageInstallerAbstract {

    public SplitPackageInstaller(Context context) {
        super(context);
    }

    @Override
    protected void installApkFiles(List<File> apkFiles) {
        PackageInstaller packageInstaller = getContext().getPackageManager().getPackageInstaller();
        try {
            PackageInstaller.SessionParams sessionParams =
                    new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionID = packageInstaller.createSession(sessionParams);
            PackageInstaller.Session session = packageInstaller.openSession(sessionID);
            for (File apkFile : apkFiles) {
                InputStream inputStream = new FileInputStream(apkFile);
                OutputStream outputStream = session.openWrite(apkFile.getName(), 0, apkFile.length());
                IOUtils.copy(inputStream, outputStream);
                session.fsync(outputStream);
                inputStream.close();
                outputStream.close();
            }
            Intent callbackIntent = new Intent(getContext(), SplitService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getContext(), 0, callbackIntent, 0);
            session.commit(pendingIntent.getIntentSender());
            session.close();
        } catch (Exception e) {
            Log.w(e.getMessage());
            dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED, null);
            installationCompleted();
        }
    }
}
