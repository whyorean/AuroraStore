package com.aurora.store.installer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.aurora.store.InstallationStatus;
import com.aurora.store.utility.Root;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aurora.store.Constants.TAG;

public class SplitPackageInstallerRooted extends SplitPackageInstaller {

    private Root root;

    public SplitPackageInstallerRooted(Context context) {
        super(context);
        root = new Root();
    }

    @Override
    protected void installApkFiles(List<File> apkFiles) {
        try {
            if (root.isTerminated() || !root.isAcquired()) {
                root = new Root();
                if (!root.isAcquired()) {
                    dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED,
                            "No Root Available");
                    installationCompleted();
                    return;
                }
            }

            int totalSize = 0;
            for (File apkFile : apkFiles)
                totalSize += apkFile.length();

            String result = ensureCommandSucceeded(root.exec(String.format(Locale.getDefault(),
                    "pm install-create -r -S %d",
                    totalSize)));

            Pattern sessionIdPattern = Pattern.compile("(\\d+)");
            Matcher sessionIdMatcher = sessionIdPattern.matcher(result);
            boolean found = sessionIdMatcher.find();
            int sessionId = Integer.parseInt(sessionIdMatcher.group(1));

            for (File apkFile : apkFiles)
                ensureCommandSucceeded(root.exec(String.format(Locale.getDefault(),
                        "cat \"%s\" | pm install-write -S %d %d \"%s\"",
                        apkFile.getAbsolutePath(),
                        apkFile.length(),
                        sessionId,
                        apkFile.getName())));

            result = ensureCommandSucceeded(root.exec(String.format(Locale.getDefault(),
                    "pm install-commit %d ",
                    sessionId)));

            if (result.toLowerCase().contains("success"))
                dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_SUCCEED,
                        getPackageNameFromApk(apkFiles));
            else
                dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED,
                        String.format(Locale.getDefault(), "Error Root : %s", result));

            installationCompleted();
        } catch (Exception e) {
            Log.w(TAG, e);
            dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED,
                    String.format(Locale.getDefault(), "Error Root : %s", e.getMessage()));
            installationCompleted();
        }
    }

    private String ensureCommandSucceeded(String result) {
        if (result == null || result.length() == 0)
            throw new RuntimeException(root.readError());
        return result;
    }

    private String getPackageNameFromApk(List<File> apkFiles) {
        for (File apkFile : apkFiles) {
            PackageInfo packageInfo = getContext().getPackageManager()
                    .getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            if (packageInfo != null)
                return packageInfo.packageName;
        }
        return "null";
    }
}
