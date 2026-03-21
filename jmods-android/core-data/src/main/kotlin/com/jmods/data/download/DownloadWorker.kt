package com.jmods.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.jmods.network.api.DownloadProgress
import com.jmods.network.api.PlayApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker
import java.io.File
import kotlinx.coroutines.flow.collect

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: PlayApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val packageName = inputData.getString("packageName") ?: return Result.failure()
        val versionCode = inputData.getInt("versionCode", 0)

        val outputFile = File(applicationContext.cacheDir, "$packageName.apk")

        return try {
            apiService.downloadApp(packageName, versionCode, outputFile).collect { progress ->
                when (progress) {
                    is DownloadProgress.Progress -> {
                        setProgress(workDataOf("progress" to progress.percentage))
                    }
                    is DownloadProgress.Success -> {
                    }
                    is DownloadProgress.Failure -> {
                        throw Exception(progress.error)
                    }
                }
            }
            Result.success(workDataOf("apkPath" to outputFile.absolutePath))
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }
}
