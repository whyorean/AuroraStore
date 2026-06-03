package com.aurora.store.data.work.remote

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.extensions.TAG
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.data.providers.WhitelistProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class WhitelistUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val httpClient: IHttpClient,
    private val whitelistProvider: WhitelistProvider
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val whitelistUrl = "https://raw.githubusercontent.com/kobiamos001/AuroraStore/master/whitelist.json"
            val response = httpClient.get(whitelistUrl, emptyMap())
            if (response.code == 200) {
                val file = File(context.filesDir, "whitelist.json")
                file.writeBytes(response.responseBytes)
                whitelistProvider.refresh()
                Log.d(TAG, "Whitelist updated successfully")
                Result.success()
            } else {
                Log.e(TAG, "Failed to update whitelist: ${response.code}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating whitelist", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "WhitelistUpdateWorker"

        fun schedule(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WhitelistUpdateWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
