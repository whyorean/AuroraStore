package com.aurora.store.data.room.update

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.PlayFile
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
    val targetSdk: Int = 1
) : Parcelable {

    companion object {
        fun fromApp(context: Context, app: App): Update = Update(
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
            app.targetSdk
        )
    }

    fun isSelfUpdate(context: Context): Boolean = packageName == context.packageName

    fun isInstalled(context: Context): Boolean = PackageUtil.isInstalled(context, packageName)

    fun isUpToDate(context: Context): Boolean =
        PackageUtil.isInstalled(context, packageName, versionCode)
}
