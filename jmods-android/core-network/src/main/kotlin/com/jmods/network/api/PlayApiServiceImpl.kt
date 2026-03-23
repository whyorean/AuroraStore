package com.jmods.network.api

import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.gplayapi.helpers.SearchHelper
import com.jmods.auth.AuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayApiServiceImpl @Inject constructor(
    private val authManager: AuthManager
) : PlayApiService {

    private val client = OkHttpClient()

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

    override fun searchApps(query: String): Flow<List<AppDto>> = flow {
        val authData = authManager.getAuthData() ?: return@flow
        val helper = SearchHelper(authData)
        val searchBundle = helper.searchResults(query)
        emit(searchBundle.appList.map { it.toDto() })
    }

    override suspend fun downloadApp(packageName: String, versionCode: Int, outputFile: File): Flow<DownloadProgress> = flow {
        val authData = authManager.getAuthData() ?: throw Exception("Not authenticated")

        try {
            val purchaseHelper = PurchaseHelper(authData)
            val purchaseResponse = purchaseHelper.purchase(packageName, versionCode)
            val downloadUrl = purchaseResponse.downloadUrl

            val request = Request.Builder().url(downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Failed to download file: ${response.code}")

                val body = response.body ?: throw Exception("Response body is null")
                val totalBytes = body.contentLength()
                var bytesDownloaded = 0L

                body.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead
                            if (totalBytes > 0) {
                                emit(DownloadProgress.Progress(bytesDownloaded.toFloat() / totalBytes))
                            }
                        }
                    }
                }
            }
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
    rating = 4.5f,
    versionCode = versionCode,
    screenshots = screenshots.map { it.url }
)
