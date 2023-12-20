package com.aurora.store.data.room.download

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.model.DownloadStatus
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "download")
data class Download(
    @PrimaryKey val packageName: String,
    val versionCode: Int,
    val offerType: Int,
    val isInstalled: Boolean,
    val displayName: String,
    val iconURL: String,
    val size: Long,
    val id: Int,
    var status: DownloadStatus,
    var progress: Int,
    var speed: Long,
    var timeRemaining: Long,
    var totalFiles: Int,
    var downloadedFiles: Int,
    val sharedLibs: List<SharedLib>
) : Parcelable {
    val isFinished get() = status in DownloadStatus.finished

    companion object {
        fun fromApp(app: App): Download {
            return Download(
                app.packageName,
                app.versionCode,
                app.offerType,
                app.isInstalled,
                app.displayName,
                app.iconArtwork.url,
                app.size,
                app.id,
                DownloadStatus.QUEUED,
                0,
                0L,
                0L,
                0,
                0,
                app.dependencies.dependentLibraries.map { SharedLib.fromApp(it) }
            )
        }
    }
}
