package com.aurora.extensions

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.aurora.store.BuildConfig

fun PackageManager.getInstallerPackageNameCompat(packageName: String): String? {
    return if (isRAndAbove) {
        getInstallSourceInfo(packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        return getInstallerPackageName(packageName)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
fun PackageManager.getUpdateOwnerPackageNameCompat(packageName: String): String? {
    // https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)
    val installSourceInfo = getInstallSourceInfo(packageName)
    return when {
        isUAndAbove -> {
            // If update ownership is null, we can still silently update it if we installed it
            installSourceInfo.updateOwnerPackageName ?: installSourceInfo.installingPackageName
        }
        isSAndAbove -> installSourceInfo.installingPackageName
        else -> if (packageName == BuildConfig.APPLICATION_ID) BuildConfig.APPLICATION_ID else null
    }
}
