package com.aurora.extensions

import android.content.pm.PackageManager

fun PackageManager.getInstallerPackageNameCompat(packageName: String): String? {
    return if (isRAndAbove()) {
        getInstallSourceInfo(packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        return getInstallerPackageName(packageName)
    }
}
