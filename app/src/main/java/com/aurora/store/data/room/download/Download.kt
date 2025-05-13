package com.aurora.store.data.room.download

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.update.Update
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "download")
data class Download(
    @PrimaryKey val packageName: String,
    val versionCode: Long,
    val offerType: Int,
    val isInstalled: Boolean,
    val displayName: String,
    val iconURL: String,
    val size: Long,
    val id: Int,
    var downloadStatus: DownloadStatus,
    var progress: Int,
    var speed: Long,
    var timeRemaining: Long,
    var totalFiles: Int,
    var downloadedFiles: Int,
    var fileList: List<PlayFile>,
    val sharedLibs: List<SharedLib>,
    val targetSdk: Int = 1,
    val downloadedAt: Long = 0
) : Parcelable {
    val isFinished get() = downloadStatus in DownloadStatus.finished
    val isRunning get() = downloadStatus in DownloadStatus.running

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
                app.fileList.filterNot { it.url.isBlank() },
                app.dependencies.dependentLibraries.map { SharedLib.fromApp(it) },
                app.targetSdk,
                Date().time
            )
        }

        fun fromUpdate(update: Update): Download {
            return Download(
                update.packageName,
                update.versionCode,
                update.offerType,
                true,
                update.displayName,
                update.iconURL,
                update.size,
                update.id,
                DownloadStatus.QUEUED,
                0,
                0L,
                0L,
                0,
                0,
                update.fileList,
                update.sharedLibs,
                update.targetSdk,
                Date().time
            )
        }
    }
}
