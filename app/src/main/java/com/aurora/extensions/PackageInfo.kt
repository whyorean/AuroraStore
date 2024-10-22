package com.aurora.extensions

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Process

fun PackageInfo.isValidApp(packageManager: PackageManager): Boolean {
    if (this.applicationInfo == null || this.packageName.isEmpty()) return false

    // Most core AOSP system apps use their package name as label
    if (this.applicationInfo!!.loadLabel(packageManager).startsWith(this.packageName)) return false

    return when {
        isQAndAbove() -> {
            Process.isApplicationUid(this.applicationInfo!!.uid) &&
                    !this.applicationInfo!!.isResourceOverlay && !this.isApex
        }
        isNAndAbove() -> Process.isApplicationUid(this.applicationInfo!!.uid)
        else -> this.versionName != null
    }
}
