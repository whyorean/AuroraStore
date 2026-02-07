package com.aurora.extensions

import android.content.pm.PackageManager
import com.aurora.store.BuildConfig

/**
 * Gets the name of package responsible for installing/updating given package
 */
fun PackageManager.getUpdateOwnerPackageNameCompat(packageName: String?): String? {
    // Self-updates can be managed by ourselves
    if (packageName == BuildConfig.APPLICATION_ID) return BuildConfig.APPLICATION_ID

    return when {
        isUAndAbove -> {
            // If update ownership is null, we can still silently update it if we installed it
            val installSourceInfo = getInstallSourceInfo(packageName.toString())
            installSourceInfo.updateOwnerPackageName ?: installSourceInfo.installingPackageName
        }

        isRAndAbove -> {
            val installSourceInfo = getInstallSourceInfo(packageName.toString())
            installSourceInfo.installingPackageName
        }

        else -> {
            @Suppress("DEPRECATION")
            getInstallerPackageName(packageName.toString())
        }
    }
}
