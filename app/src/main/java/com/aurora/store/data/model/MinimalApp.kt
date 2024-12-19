package com.aurora.store.data.model

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.room.update.Update
import com.aurora.store.util.PackageUtil
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class MinimalApp(
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val displayName: String,
    @IgnoredOnParcel
    val icon: Bitmap? = null
) : Parcelable {

    companion object {

        fun fromApp(app: App): MinimalApp {
            return MinimalApp(
                app.packageName,
                app.versionName,
                app.versionCode,
                app.displayName
            )
        }

        fun toApp(minimalApp: MinimalApp): App {
            return App(minimalApp.packageName).apply {
                versionName = minimalApp.versionName ?: ""
                versionCode = minimalApp.versionCode
                displayName = minimalApp.displayName
            }
        }

        fun fromUpdate(update: Update): MinimalApp {
            return MinimalApp(
                update.packageName,
                update.versionName,
                update.versionCode,
                update.displayName
            )
        }

        fun fromPackageInfo(context: Context, packageInfo: PackageInfo): MinimalApp {
            return MinimalApp(
                packageInfo.packageName,
                packageInfo.versionName ?: "",
                PackageInfoCompat.getLongVersionCode(packageInfo).toInt(),
                packageInfo.applicationInfo!!.loadLabel(context.packageManager).toString(),
                PackageUtil.getIconForPackage(context, packageInfo.packageName)
            )
        }
    }
}
