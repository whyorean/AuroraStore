/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.AppFile
import com.aurora.store.BuildConfig
import com.aurora.store.data.room.AppFileParceler
import com.aurora.store.data.room.download.SharedLib
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<AppFile, AppFileParceler>
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
    var fileList: List<AppFile>,
    val sharedLibs: List<SharedLib>,
    val targetSdk: Int = 1,
    val isIncompatible: Boolean = false,
    /**
     * What updating is expected to download, from [estimateDownloadSize]. `0` when Play sent no
     * file sizes; [size] stays the full install size for storage checks.
     */
    val downloadSize: Long = 0,
    val newPermissions: List<String> = emptyList()
) : Parcelable {

    companion object {
        fun fromApp(context: Context, app: App, isIncompatible: Boolean = false): Update {
            val certificateHashes = CertUtil.getEncodedCertificateHashes(context, app.packageName)
            return Update(
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
                app.certificateSetList.any { it.certificateHash in certificateHashes },
                app.offerType,
                app.fileList.filterNot { it.url.isBlank() },
                app.dependencies.dependentLibraries.map { SharedLib.fromApp(it) },
                app.targetSdk,
                isIncompatible,
                estimateDownloadSize(
                    app.fileList,
                    installedVersionCode(context, app.packageName),
                    certificateHashes.isNotEmpty()
                ) + estimateLibrariesSize(context, app.dependencies.dependentLibraries),
                PackageUtil.getNewPermissions(context, app.packageName, app.permissions)
            )
        }

        /**
         * The bytes DownloadWorker is expected to transfer for an update's shared [libraries]:
         * every file, whole and unpatched, of each library not already installed at its required
         * version, by the same [PackageUtil.isSharedLibraryInstalled] check DownloadWorker skips
         * them with.
         */
        fun estimateLibrariesSize(context: Context, libraries: List<App>): Long = libraries
            .filterNot {
                PackageUtil.isSharedLibraryInstalled(context, it.packageName, it.versionCode)
            }
            .sumOf { library -> library.fileList.sumOf { it.size } }

        /**
         * The bytes DownloadWorker is expected to transfer for the app's own [files], mirroring its choices
         * rather than Play's own client ([App.updateDownloadSize] counts compressed copies, dex
         * metadata and asset packs, none of which DownloadWorker fetches):
         * - a file Play lists at a version in `1..installedVersionCode` is kept, so it counts 0;
         * - a BASE or SPLIT file with a patch priced against a version at or below the installed
         *   one is fetched as that patch, counted as DownloadWorker's `patchEstimate` counts it;
         * - anything else is fetched whole, at [AppFile.size].
         *
         * No patch counts when nothing is installed ([installedVersionCode] null) or when
         * [canPatch] is false, DownloadWorker's `readPatchBase` refusing an app whose signing
         * certificate cannot be read. The estimate is optimistic: at download time a patch still
         * falls back to the whole file when the installed counterpart is missing or its hash is
         * not the patch's base, when Play serves no `GZIPPED_BSDIFF` patch, or when storage is too
         * short to apply it.
         */
        fun estimateDownloadSize(
            files: List<AppFile>,
            installedVersionCode: Long?,
            canPatch: Boolean
        ): Long = files.sumOf { file ->
            val patch = file.patch?.takeIf {
                installedVersionCode != null &&
                    canPatch &&
                    (file.type == AppFile.Type.BASE || file.type == AppFile.Type.SPLIT) &&
                    it.baseVersionCode <= installedVersionCode
            }
            when {
                installedVersionCode != null && file.versionCode in 1..installedVersionCode -> 0L
                patch != null && patch.maxSize > 0 -> minOf(patch.maxSize, file.size)
                else -> file.size
            }
        }

        /**
         * The installed 32-bit versionCode, which is what DownloadWorker describes to Play when it
         * asks for patches, or null when the app is not installed.
         */
        @Suppress("DEPRECATION")
        private fun installedVersionCode(context: Context, packageName: String): Long? = try {
            context.packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
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
