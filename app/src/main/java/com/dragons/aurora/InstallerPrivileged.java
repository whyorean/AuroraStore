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

package com.dragons.aurora;

import android.Manifest;
import android.content.Context;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Looper;
import android.os.RemoteException;

import com.dragons.aurora.model.App;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class InstallerPrivileged extends InstallerBackground {

    static private final Map<Integer, String> errors = new HashMap<>();

    static {
        errors.put(-1, "Already installed");
        errors.put(-2, "Package archive file is invalid");
        errors.put(-3, "URI passed in is invalid");
        errors.put(-4, "Not enough storage space to install the app");
        errors.put(-5, "Package is already installed with the same name");
        errors.put(-6, "The requested shared user does not exist");
        errors.put(-7, "A previously installed package of the same name has a different signature than the new package");
        errors.put(-8, "The new package requested a shared user which is already installed on the device and does not have matching signature");
        errors.put(-9, "The new package uses a shared library that is not available");
        errors.put(-10, "Could not delete the old package to replace it with its update");
        errors.put(-11, "Error while optimizing and validating its dex files");
        errors.put(-12, "The current SDK version is older than that required by the package");
        errors.put(-13, "Package contains a content provider with the same authority as a provider already installed in the system");
        errors.put(-14, "The current SDK version is newer than that required by the package");
        errors.put(-15, "Test-only package and the caller has not supplied the flag");
        errors.put(-16, "Contains native code, but none that is compatible with the the device's CPU_ABI");
        errors.put(-17, "Uses a feature that is not available");
        errors.put(-18, "A secure container mount point couldn't be accessed on external media");
        errors.put(-19, "Couldn't be installed in the specified install location");
        errors.put(-20, "The new package couldn't be installed in the specified install location because the media is not available");
        errors.put(-21, "Verification timed out");
        errors.put(-22, "Verification did not succeed");
        errors.put(-23, "The package changed from what the calling program expected");
        errors.put(-24, "The new package is assigned a different UID than it previously held");
        errors.put(-25, "The new package has an older version code than the currently installed package");
        errors.put(-100, "The parser was given a path that is not a file, or does not end with .apk");
        errors.put(-101, "The parser was unable to retrieve the AndroidManifest.xml");
        errors.put(-102, "The parser encountered an unexpected exception");
        errors.put(-103, "The parser did not find any certificates in the .apk");
        errors.put(-104, "The parser found inconsistent certificates on the files in the .apk");
        errors.put(-105, "The parser encountered a CertificateEncodingException in one of the files in the .apk");
        errors.put(-106, "The parser encountered a bad or missing package name in the manifest");
        errors.put(-107, "The parser encountered a bad shared user id name in the manifest");
        errors.put(-108, "The parser encountered some structural problem in the manifest");
        errors.put(-109, "The parser did not find any actionable tags (instrumentation or application) in the manifest");
        errors.put(-110, "The system failed to install the package because of system issues");
        errors.put(-111, "The system failed to install the package because the user is restricted from installing apps.");
        errors.put(-112, "The system failed to install the package because it is attempting to define a permission that is already defined by some existing package.");
        errors.put(-113, "The system failed to install the package because its packaged native code did not match any of the ABIs supported by the system.");
    }

    public final int INSTALL_REPLACE_EXISTING = 2;

    public InstallerPrivileged(Context context) {
        super(context);
    }

    @Override
    public boolean verify(App app) {
        if (!super.verify(app)) {
            return false;
        }
        if (context.getPackageManager().checkPermission(Manifest.permission.INSTALL_PACKAGES, BuildConfig.APPLICATION_ID) != PackageManager.PERMISSION_GRANTED) {
            Timber.i("%s not granted", Manifest.permission.INSTALL_PACKAGES);
            ((AuroraApplication) context.getApplicationContext()).removePendingUpdate(app.getPackageName());
            notifyAndToast(R.string.notification_not_privileged, R.string.pref_not_privileged, app);
            return false;
        }
        return true;
    }

    @Override
    protected void install(App app) {
        InstallationState.setInstalling(app.getPackageName());
        File apkFile = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
        if (!apkFile.exists()) {
            Timber.e("Installation requested for apk " + apkFile.getAbsolutePath() + " which does not exist");
            ((AuroraApplication) context.getApplicationContext()).removePendingUpdate(app.getPackageName());
            sendBroadcast(app.getPackageName(), false);
            return;
        }
        PackageManager pm = context.getPackageManager();
        Class<?>[] types = new Class[]{Uri.class, IPackageInstallObserver.class, int.class, String.class};
        try {
            pm.getClass().getMethod("installPackage", types).invoke(pm, Uri.fromFile(apkFile), new InstallObserver(app), INSTALL_REPLACE_EXISTING, BuildConfig.APPLICATION_ID);
        } catch (NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException | NullPointerException e) {
            Timber.e("Could not start privileged installation: " + e.getClass().getName() + " " + e.getMessage());
            ((AuroraApplication) context.getApplicationContext()).removePendingUpdate(app.getPackageName());
            sendBroadcast(app.getPackageName(), false);
        }
    }

    class InstallObserver extends IPackageInstallObserver.Stub {

        private App app;

        public InstallObserver(App app) {
            this.app = app;
        }

        @Override
        public void packageInstalled(String packageName, int returnCode) throws RemoteException {
            Timber.i("Installation of " + packageName + " complete with code " + returnCode);
            boolean success = returnCode > 0;
            if (success) {
                InstallationState.setSuccess(packageName);
            } else {
                InstallationState.setFailure(packageName);
            }
            sendBroadcast(packageName, success);
            Looper.prepare();
            postInstallationResult(app, success);
            if (errors.containsKey(returnCode)) {
                Timber.e(errors.get(returnCode));
            }
            Looper.loop();
        }
    }
}