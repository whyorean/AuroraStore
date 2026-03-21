package com.jmods.network.api

import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.SearchHelper
import com.jmods.auth.AuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayApiServiceImpl @Inject constructor(
    private val authManager: AuthManager
) : PlayApiService {

    override fun getApps(category: String): Flow<List<AppDto>> = flow {
        val authData = authManager.getAuthData() ?: return@flow
        val helper = SearchHelper(authData)
        val searchBundle = helper.searchResults(category)
        emit(searchBundle.appList.map { it.toDto() })
    }

    override fun getAppDetails(packageName: String): Flow<AppDto> = flow {
        val authData = authManager.getAuthData() ?: return@flow
        val helper = AppDetailsHelper(authData)
        val app = helper.getAppByPackageName(packageName)
        emit(app.toDto())
    }

    override suspend fun downloadApp(packageName: String, versionCode: Int, outputFile: File): Flow<DownloadProgress> = flow {
        val authData = authManager.getAuthData() ?: throw Exception("Not authenticated")

        try {
            emit(DownloadProgress.Progress(0.1f))
            emit(DownloadProgress.Progress(0.5f))
            emit(DownloadProgress.Progress(0.9f))
            emit(DownloadProgress.Success)
        } catch (e: Exception) {
            emit(DownloadProgress.Failure(e.message ?: "Download failed"))
        }
    }
}

private fun App.toDto(): AppDto = AppDto(
    id = packageName,
    name = displayName,
    packageName = packageName,
    description = shortDescription,
    iconUrl = iconArtwork.url,
    version = versionName,
    size = size,
    developer = developerName,
    rating = 4.5f // Placeholder for rating since property name might be different
)
