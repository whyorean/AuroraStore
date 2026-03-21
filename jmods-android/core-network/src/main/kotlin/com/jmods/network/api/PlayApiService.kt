package com.jmods.network.api

import kotlinx.coroutines.flow.Flow
import java.io.File

interface PlayApiService {
    fun getApps(category: String): Flow<List<AppDto>>
    fun getAppDetails(packageName: String): Flow<AppDto>
    suspend fun downloadApp(packageName: String, versionCode: Int, outputFile: File): Flow<DownloadProgress>
}

sealed class DownloadProgress {
    data class Progress(val percentage: Float) : DownloadProgress()
    object Success : DownloadProgress()
    data class Failure(val error: String) : DownloadProgress()
}
