/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-FileCopyrightText: 2023-2024 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.details

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.details.TestingProgramStatus
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.gplayapi.helpers.web.WebDataSafetyHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.BuildConfig
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.model.ExodusReport
import com.aurora.store.data.model.PlexusReport
import com.aurora.store.data.model.Report
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.data.room.favourite.FavouriteDao
import com.aurora.store.util.PackageUtil
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    val authProvider: AuthProvider,
    @ApplicationContext private val context: Context,
    private val appDetailsHelper: AppDetailsHelper,
    private val reviewsHelper: ReviewsHelper,
    private val webDataSafetyHelper: WebDataSafetyHelper,
    private val downloadHelper: DownloadHelper,
    private val favouriteDao: FavouriteDao,
    private val httpClient: IHttpClient,
    private val gson: Gson
) : ViewModel() {

    private val TAG = AppDetailsViewModel::class.java.simpleName

    private val _app = MutableStateFlow<App?>(App(""))
    val app = _app.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>?>(null)
    val reviews = _reviews.asStateFlow()

    private val _userReview = MutableStateFlow<Review?>(null)
    val userReview = _userReview.asStateFlow()

    private val _dataSafetyReport = MutableStateFlow<DataSafetyReport?>(null)
    val dataSafetyReport = _dataSafetyReport.asStateFlow()

    private val _exodusReport = MutableStateFlow<Report?>(null)
    val exodusReport = _exodusReport.asStateFlow()

    private val _plexusReport = MutableStateFlow<PlexusReport?>(null)
    val plexusReport = _plexusReport.asStateFlow()

    private val _testingProgramStatus = MutableStateFlow<TestingProgramStatus?>(null)
    val testingProgramStatus = _testingProgramStatus.asStateFlow()

    private val _favourite = MutableStateFlow(false)
    val favourite = _favourite.asStateFlow()

    val download = combine(app, downloadHelper.downloadsList) { a, list ->
        if (a?.packageName.isNullOrBlank()) return@combine null
        list.find { d -> d.packageName == a?.packageName }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun fetchAppDetails(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                checkFavourite(packageName)
                val appDetails = appDetailsHelper.getAppByPackageName(packageName).apply {
                    isInstalled = PackageUtil.isInstalled(context, packageName)
                }
                _app.emit(appDetails)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch app details", exception)
                _app.emit(null)
            }
        }
    }

    fun fetchAppReviews(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _reviews.value = reviewsHelper.getReviewSummary(packageName)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch app reviews", exception)
                _reviews.emit(emptyList())
            }
        }
    }

    fun fetchAppDataSafetyReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _dataSafetyReport.value = webDataSafetyHelper.fetch(packageName)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch data safety report", exception)
            }
        }
    }

    fun fetchAppReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _exodusReport.value = getLatestExodusReport(packageName)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch privacy report", exception)
            }
        }
    }

    fun fetchPlexusReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _plexusReport.value = getPlexusReport(packageName)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch compatibility report", exception)
            }
        }
    }

    fun fetchTestingProgramStatus(packageName: String, subscribe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _testingProgramStatus.value = appDetailsHelper.testingProgram(packageName, subscribe)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch testing program status", exception)
            }
        }
    }

    fun fetchUserAppReview(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isTesting = app.testingProgram?.isSubscribed ?: false
                _userReview.value = reviewsHelper.getUserReview(app.packageName, isTesting)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch user review", exception)
            }
        }
    }

    fun postAppReview(packageName: String, review: Review, isBeta: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _userReview.value = reviewsHelper.addOrEditReview(
                    packageName,
                    review.title,
                    review.comment,
                    review.rating,
                    isBeta
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to post review", exception)
            }
        }
    }

    fun download(app: App) {
        viewModelScope.launch { downloadHelper.enqueueApp(app) }
    }

    fun cancelDownload(app: App) {
        viewModelScope.launch { downloadHelper.cancelDownload(app.packageName) }
    }

    fun toggleFavourite(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            if (favourite.value) {
                favouriteDao.delete(app.packageName)
            } else {
                favouriteDao.insert(
                    Favourite(
                        packageName = app.packageName,
                        displayName = app.displayName,
                        iconURL = app.iconArtwork.url,
                        mode = Favourite.Mode.MANUAL,
                        added = System.currentTimeMillis(),
                    )
                )
            }

            _favourite.value = !favourite.value
        }
    }

    private fun checkFavourite(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _favourite.value = favouriteDao.isFavourite(packageName)
        }
    }

    private fun getLatestExodusReport(packageName: String): Report? {
        val url = "${Constants.EXODUS_SEARCH_URL}$packageName"
        val headers = mutableMapOf(
            "Content-Type" to Constants.JSON_MIME_TYPE,
            "Accept" to Constants.JSON_MIME_TYPE,
            "Authorization" to "Token ${BuildConfig.EXODUS_API_KEY}"
        )

        val playResponse = httpClient.get(url, headers)
        return parseExodusResponse(String(playResponse.responseBytes), packageName)
            .firstOrNull()
    }

    private fun getPlexusReport(packageName: String): PlexusReport? {
        val url = "${Constants.PLEXUS_API_URL}/${packageName}/?scores=true"
        val playResponse = httpClient.get(url, emptyMap())
        return gson.fromJson(String(playResponse.responseBytes), PlexusReport::class.java)
    }

    private fun parseExodusResponse(response: String, packageName: String): List<Report> {
        try {
            val jsonObject = JSONObject(response)
            val exodusObject = jsonObject.getJSONObject(packageName)
            val exodusReport: ExodusReport = gson.fromJson(
                exodusObject.toString(), ExodusReport::class.java
            )

            return exodusReport.reports
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
