package com.aurora.store.data.room.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.BuildConfig
import com.aurora.store.data.room.download.SharedLib
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "update")
data class Update(
    @PrimaryKey
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val displayName: String,
    val iconURL: String,
    val changelog: String,
    val id: Int,
    val developerName: String,
    val size: Long,
    val updatedOn: String,
    val hasValidCert: Boolean,
    val offerType: Int,
    var fileList: List<PlayFile>,
    val sharedLibs: List<SharedLib>,
    val targetSdk: Int = 1,
    val isIncompatible: Boolean = false
) : Parcelable {

    companion object {
        fun fromApp(context: Context, app: App, isIncompatible: Boolean = false): Update = Update(
            app.packageName,
            app.versionCode,
            app.versionName,
            app.displayName,
            app.iconArtwork.url,
            app.changes,
            app.id,
            app.developerName,
            app.size,
            app.updatedOn,
            app.certificateSetList.any {
                it.certificateSet in CertUtil.getEncodedCertificateHashes(
                    context,
                    app.packageName
                )
            },
            app.offerType,
            app.fileList.filterNot { it.url.isBlank() },
            app.dependencies.dependentLibraries.map { SharedLib.fromApp(it) },
            app.targetSdk,
            isIncompatible
        )
    }

    fun isSelfUpdate(context: Context): Boolean = packageName == context.packageName

    fun isInstalled(context: Context): Boolean = PackageUtil.isInstalled(context, packageName)

    fun isUpToDate(context: Context): Boolean =
        PackageUtil.isInstalled(context, packageName, versionCode)

    /**
     * Update ownership was introduced in Android 14 (API 34) via
     * [android.content.pm.PackageInstaller.SessionParams.setRequestUpdateOwnership]; if a
     * different installer holds it, the OS shows a transfer-approval dialog at install time.
     * Below API 34 the concept does not exist, so this returns false. We also return false when
     * no installer has claimed ownership (`updateOwnerPackageName == null`) since no transfer
     * prompt is shown in that case.
     */
    fun requiresOwnershipTransfer(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        if (isSelfUpdate(context)) return false
        return updateOwnerOnApi34(context.packageManager)?.let {
            it != BuildConfig.APPLICATION_ID
        } ?: false
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun updateOwnerOnApi34(packageManager: PackageManager): String? = try {
        packageManager.getInstallSourceInfo(packageName).updateOwnerPackageName
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
