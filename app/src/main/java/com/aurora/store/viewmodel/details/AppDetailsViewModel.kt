package com.aurora.store.viewmodel.details

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.details.TestingProgramStatus
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.store.data.model.ExodusReport
import com.aurora.store.data.model.Report
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.DownloadWorkerUtil
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.reflect.Modifier
import javax.inject.Inject

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    private val downloadWorkerUtil: DownloadWorkerUtil
) : ViewModel() {

    private val TAG = AppDetailsViewModel::class.java.simpleName

    private val exodusBaseUrl = "https://reports.exodus-privacy.eu.org/api/search/"
    private val exodusApiKey = "Token bbe6ebae4ad45a9cbacb17d69739799b8df2c7ae"

    private val _app = MutableSharedFlow<App>()
    val app = _app.asSharedFlow()

    private val _reviews = MutableSharedFlow<List<Review>>()
    val reviews = _reviews.asSharedFlow()

    private val _userReview = MutableSharedFlow<Review>()
    val userReview = _userReview.asSharedFlow()

    private val _report = MutableSharedFlow<Report?>()
    val report = _report.asSharedFlow()

    private val _testingProgramStatus = MutableSharedFlow<TestingProgramStatus?>()
    val testingProgramStatus = _testingProgramStatus.asSharedFlow()

    val downloadsList get() = downloadWorkerUtil.downloadsList

    fun fetchAppDetails(context: Context, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authData = AuthProvider.with(context).getAuthData()
                _app.emit(
                    AppDetailsHelper(authData).using(HttpClient.getPreferredClient(context))
                        .getAppByPackageName(packageName)
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch app details", exception)
                _app.emit(App(""))
            }
        }
    }

    fun fetchAppReviews(context: Context, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authData = AuthProvider.with(context).getAuthData()
                _reviews.emit(ReviewsHelper(authData).using(HttpClient.getPreferredClient(context))
                    .getReviewSummary(packageName))
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch app reviews", exception)
                _reviews.emit(emptyList())
            }
        }
    }

    fun postAppReview(context: Context, packageName: String, review: Review, isBeta: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authData = AuthProvider.with(context).getAuthData()
                _userReview.emit(ReviewsHelper(authData)
                    .using(HttpClient.getPreferredClient(context))
                    .addOrEditReview(
                        packageName,
                        review.title,
                        review.comment,
                        review.rating,
                        isBeta
                    )!!)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to post review", exception)
                _userReview.emit(Review())
            }
        }
    }


    fun fetchAppReport(context: Context,packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val headers: MutableMap<String, String> = mutableMapOf()
                headers["Content-Type"] = "application/json"
                headers["Accept"] = "application/json"
                headers["Authorization"] = exodusApiKey

                val url = exodusBaseUrl + packageName
                val playResponse = HttpClient.getPreferredClient(context).get(url, headers)

                _report.emit(parseResponse(String(playResponse.responseBytes), packageName)[0])
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch privacy report", exception)
                _report.emit(null)
            }
        }
    }

    fun fetchTestingProgramStatus(context: Context, packageName: String, subscribe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authData = AuthProvider.with(context).getAuthData()
                _testingProgramStatus.emit(
                    AppDetailsHelper(authData).testingProgram(
                        packageName,
                        subscribe
                    )
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch testing program status", exception)
                _testingProgramStatus.emit(null)
            }
        }
    }

    fun download(app: App) {
        viewModelScope.launch { downloadWorkerUtil.enqueueApp(app) }
    }

    fun cancelDownload(app: App) {
        viewModelScope.launch { downloadWorkerUtil.cancelDownload(app.packageName) }
    }

    private fun parseResponse(response: String, packageName: String): List<Report> {
        try {
            val gson = GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                    .create()

            val jsonObject = JSONObject(response)
            val exodusObject = jsonObject.getJSONObject(packageName)
            val exodusReport: ExodusReport = gson.fromJson(
                exodusObject.toString(), ExodusReport::class.java
            )
            return exodusReport.reports
        } catch (e: Exception) {
            throw Exception("No reports found")
        }
    }
}
