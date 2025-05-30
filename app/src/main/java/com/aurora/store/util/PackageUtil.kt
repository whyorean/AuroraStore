/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.SharedLibraryInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.aurora.extensions.isHuawei
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isPAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.isVAndAbove
import com.aurora.extensions.isValidApp
import com.aurora.store.BuildConfig
import com.aurora.store.R
import java.util.Locale

object PackageUtil {

    private const val TAG = "PackageUtil"

    const val PACKAGE_NAME_GMS = "com.google.android.gms"
    private const val VERSION_CODE_MICRO_G: Long = 240913402
    private const val VERSION_CODE_MICRO_G_HUAWEI: Long = 240913007

    fun getAllValidPackages(context: Context): List<PackageInfo> {
        return context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            .filter { it.isValidApp(context.packageManager) }
            .sortedBy {
                it.applicationInfo!!.loadLabel(context.packageManager).toString()
                    .lowercase(Locale.getDefault())
            }
    }

    fun hasSupportedMicroG(context: Context): Boolean {
        val isMicroG = CertUtil.isMicroGGMS(context, PACKAGE_NAME_GMS)

        // Do not proceed if MicroG variant is not installed
        if (!isMicroG) return false

        return if (isHuawei) {
            isInstalled(context, PACKAGE_NAME_GMS, VERSION_CODE_MICRO_G_HUAWEI)
        } else {
            isInstalled(context, PACKAGE_NAME_GMS, VERSION_CODE_MICRO_G)
        }
    }

    fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            getPackageInfo(context, packageName, PackageManager.GET_META_DATA)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isInstalled(context: Context, packageName: String, versionCode: Long): Boolean {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            return PackageInfoCompat.getLongVersionCode(packageInfo) >= versionCode.toLong()
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isArchived(context: Context, packageName: String): Boolean {
        return try {
            isVAndAbove && context.packageManager.getArchivedPackage(packageName) != null
        } catch (e: Exception) {
            false
        }
    }

    fun isSharedLibrary(context: Context, packageName: String): Boolean {
        return if (isOAndAbove) {
            getAllSharedLibraries(context).any { it.name == packageName }
        } else {
            false
        }
    }

    fun isSharedLibraryInstalled(context: Context, packageName: String, versionCode: Long): Boolean {
        return if (isOAndAbove) {
            val sharedLibraries = getAllSharedLibraries(context)
            if (isPAndAbove) {
                sharedLibraries.any {
                    it.name == packageName && it.longVersion == versionCode
                }
            } else {
                sharedLibraries.any {
                    @Suppress("DEPRECATION")
                    it.name == packageName && it.version == versionCode.toInt()
                }
            }
        } else {
            false
        }
    }

    fun isUpdatable(context: Context, packageName: String, versionCode: Long): Boolean {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            return versionCode > PackageInfoCompat.getLongVersionCode(packageInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledVersionName(context: Context, packageName: String): String {
        return try {
            getPackageInfo(context, packageName).versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun getInstalledVersionCode(context: Context, packageName: String): Long {
        return try {
            PackageInfoCompat.getLongVersionCode(getPackageInfo(context, packageName))
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    fun isTv(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    fun getLaunchIntent(context: Context, packageName: String?): Intent? {
        val intent = if (isTv(context)) {
            context.packageManager.getLeanbackLaunchIntentForPackage(packageName!!)
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName!!)
        }

        return if (intent == null) {
            null
        } else {
            intent.addCategory(if (isTv(context)) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            intent
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getStorageManagerIntent(context: Context): Intent {
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:${BuildConfig.APPLICATION_ID}".toUri()
        )

        // Check if the intent can be resolved
        val packageManager = context.packageManager
        val isIntentAvailable = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        ).isNotEmpty()

        return if (isIntentAvailable) {
            intent
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    }

    fun getInstallUnknownAppsIntent(): Intent {
        return if (isOAndAbove) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${BuildConfig.APPLICATION_ID}".toUri()
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (isOAndAbove) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            @Suppress("DEPRECATION")
            val secureResult = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS, 0
            )

            return secureResult == 1
        }
    }

    @Throws(Exception::class)
    fun getPackageInfo(context: Context, packageName: String, flags: Int = 0): PackageInfo {
        return if (isTAndAbove) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageInfoFlags.of(flags.toLong())
            )
        } else {
            context.packageManager.getPackageInfo(packageName, flags)
        }
    }

    fun getIconForPackage(context: Context, packageName: String): Bitmap? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val icon = packageInfo.applicationInfo!!.loadIcon(context.packageManager)
            if (icon.intrinsicWidth > 0 && icon.intrinsicHeight > 0) {
                icon.toBitmap(96, 96)
            } else {
                context.packageManager.defaultActivityIcon.toBitmap(96, 96)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get icon for package!", exception)
            null
        }
    }

    fun getIconDrawableForPackage(context: Context, packageName: String): Drawable? {
        val placeholder = AppCompatResources.getDrawable(context, R.drawable.bg_placeholder)

        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val applicationInfo = packageInfo.applicationInfo ?: return placeholder

            applicationInfo.loadIcon(context.packageManager)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get icon for package!", exception)
            placeholder
        }
    }

    private fun getAllSharedLibraries(context: Context, flags: Int = 0): List<SharedLibraryInfo> {
        return if (isTAndAbove) {
            context.packageManager.getSharedLibraries(PackageInfoFlags.of(flags.toLong()))
        } else if (isOAndAbove) {
            context.packageManager.getSharedLibraries(flags)
        } else {
            emptyList()
        }
    }

    fun getFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addDataScheme("package")
        @Suppress("DEPRECATION")
        filter.addAction(Intent.ACTION_PACKAGE_INSTALL)
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        return filter
    }
}
