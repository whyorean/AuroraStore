package com.aurora.extensions

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.content.pm.PackageInfoCompat

fun PackageInfo.isValidApp(packageManager: PackageManager): Boolean {
    if (this.applicationInfo == null || this.packageName.isEmpty()) return false

    // Filter out core AOSP system apps
    if (this.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
        if (this.packageName.endsWith(".resources")) return false
        if (this.applicationInfo!!.loadLabel(packageManager).startsWith(this.packageName)) return false
        if (this.versionName?.endsWith("system image") == true) return false
        if (this.versionName?.endsWith("-initial") == true) return false
        if (this.versionName == Build.VERSION.RELEASE && PackageInfoCompat.getLongVersionCode(this) == Build.VERSION.SDK_INT.toLong()) return false
    }

    return when {
        isQAndAbove -> {
            Process.isApplicationUid(this.applicationInfo!!.uid) &&
                    !this.applicationInfo!!.isResourceOverlay && !this.isApex
        }
        isNAndAbove -> Process.isApplicationUid(this.applicationInfo!!.uid)
        else -> this.versionName != null
    }
}
