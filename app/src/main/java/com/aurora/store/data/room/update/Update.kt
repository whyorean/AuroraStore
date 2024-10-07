package com.aurora.store.data.room.update

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File
import com.aurora.store.data.room.download.SharedLib
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "update")
data class Update(
    @PrimaryKey
    val packageName: String,
    val versionCode: Int,
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
    var fileList: List<File>,
    val sharedLibs: List<SharedLib>,
    val targetSdk: Int
) : Parcelable {

    companion object {
        fun fromApp(context: Context, app: App): Update {
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
                app.certificateSetList.any {
                    it.certificateSet in CertUtil.getEncodedCertificateHashes(
                        context, app.packageName
                    )
                },
                app.offerType,
                app.fileList.filterNot { it.url.isBlank() },
                app.dependencies.dependentLibraries.map { SharedLib.fromApp(it) },
                app.targetSdk
            )
        }
    }

    fun isSelfUpdate(): Boolean {
        return packageName == Constants.APP_ID
    }

    fun isInstalled(context: Context): Boolean {
        return PackageUtil.isInstalled(context, packageName)
    }

    fun isUpToDate(context: Context): Boolean {
        return PackageUtil.isInstalled(context, packageName, versionCode)
    }
}
