package com.jmods.data.download

import android.content.Context
import androidx.work.*
import com.jmods.domain.model.App
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JModsDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueueDownload(app: App) {
        val data = workDataOf(
            "packageName" to app.packageName,
            "appName" to app.name,
            "iconUrl" to app.iconUrl,
            "versionCode" to (app.version.toIntOrNull() ?: 0)
        )

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .addTag(app.packageName)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            app.packageName,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun getDownloadStatus(packageName: String): Flow<DownloadState> {
        return workManager.getWorkInfosByTagFlow(packageName).map { infos ->
            val info = infos.firstOrNull() ?: return@map DownloadState.Idle
            when (info.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = info.progress.getFloat("progress", 0f)
                    DownloadState.Downloading(progress)
                }
                WorkInfo.State.SUCCEEDED -> {
                    val apkPath = info.outputData.getString("apkPath")
                    if (apkPath != null) DownloadState.Completed(apkPath) else DownloadState.Failed("No APK path")
                }
                WorkInfo.State.FAILED -> DownloadState.Failed(info.outputData.getString("error") ?: "Download failed")
                else -> DownloadState.Idle
            }
        }
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Completed(val apkPath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
