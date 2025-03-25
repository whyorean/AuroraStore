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
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.details.TestingProgramStatus
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.gplayapi.helpers.web.WebDataSafetyHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.BuildConfig
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.model.ExodusReport
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.PlexusReport
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.Scores
import com.aurora.store.data.paging.GenericPagingSource.Companion.createPager
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.data.room.favourite.FavouriteDao
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PackageUtil.PACKAGE_NAME_GMS
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_SIMILAR
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject
import javax.inject.Inject
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    val authProvider: AuthProvider,
    @ApplicationContext private val context: Context,
    private val purchaseHelper: PurchaseHelper,
    private val appDetailsHelper: AppDetailsHelper,
    private val reviewsHelper: ReviewsHelper,
    private val webDataSafetyHelper: WebDataSafetyHelper,
    private val downloadHelper: DownloadHelper,
    private val favouriteDao: FavouriteDao,
    private val httpClient: IHttpClient,
    private val json: Json,
    private val exodusTrackers: JSONObject
) : ViewModel() {

    private val TAG = AppDetailsViewModel::class.java.simpleName

    private val _app = MutableStateFlow<App?>(App(""))
    val app = _app.asStateFlow()

    private var reviewsNextPageUrl: String? = null
    private val _reviews = MutableStateFlow<PagingData<Review>>(PagingData.empty())
    val reviews = _reviews.asStateFlow()

    private val _userReview = MutableStateFlow<Review?>(null)
    val userReview = _userReview.asStateFlow()

    private val _dataSafetyReport = MutableStateFlow<DataSafetyReport?>(null)
    val dataSafetyReport = _dataSafetyReport.asStateFlow()

    private val _exodusReport = MutableStateFlow<Report?>(Report())
    val exodusReport = _exodusReport.asStateFlow()

    private val _plexusScores = MutableStateFlow<Scores?>(Scores())
    val plexusScores = _plexusScores.asStateFlow()

    private val _testingProgramStatus = MutableStateFlow<TestingProgramStatus?>(null)
    val testingProgramStatus = _testingProgramStatus.asStateFlow()

    private val _favourite = MutableStateFlow(false)
    val favourite = _favourite.asStateFlow()

    private val _purchaseStatus = MutableSharedFlow<Boolean>()
    val purchaseStatus = _purchaseStatus.asSharedFlow()

    val download = combine(app, downloadHelper.downloadsList) { a, list ->
        if (a?.packageName.isNullOrBlank()) return@combine null
        list.find { d -> d.packageName == a?.packageName }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val isExtendedUpdateEnabled: Boolean
        get() = Preferences.getBoolean(context, PREFERENCE_UPDATES_EXTENDED)

    private val isUpdatable: Boolean
        get() = PackageUtil.isUpdatable(
            context,
            app.value!!.packageName,
            app.value!!.versionCode.toLong()
        )

    private val hasValidCerts: Boolean
        get() = app.value!!.certificateSetList.any {
            it.certificateSet in CertUtil.getEncodedCertificateHashes(
                context,
                app.value!!.packageName
            )
        }

    val hasValidUpdate: Boolean
        get() = (isUpdatable && hasValidCerts) || (isUpdatable && isExtendedUpdateEnabled)

    val showSimilarApps: Boolean
        get() = Preferences.getBoolean(context, PREFERENCE_SIMILAR)

    fun fetchAppDetails(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _favourite.value = favouriteDao.isFavourite(packageName)

                _dataSafetyReport.value = webDataSafetyHelper.fetch(packageName)
                _app.value = appDetailsHelper.getAppByPackageName(packageName).copy(
                    isInstalled = PackageUtil.isInstalled(context, packageName)
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch app details", exception)
                _app.value = null
            }
        }.invokeOnCompletion { throwable ->
            // Only proceed if there was no error while fetching the app details
            if (throwable != null) return@invokeOnCompletion

            fetchReviews()
            fetchExodusPrivacyReport(packageName)
            if (app.value!!.dependencies.dependentPackages.contains(PACKAGE_NAME_GMS)) {
                fetchPlexusReport(packageName)
            }
        }
    }

    fun fetchReviews(filter: Review.Filter = Review.Filter.ALL) {
        reviewsNextPageUrl = null

        createPager {
            val reviewsCluster = reviewsNextPageUrl?.let { nextPageUrl ->
                reviewsHelper.next(nextPageUrl)
            } ?: reviewsHelper.getReviews(app.value!!.packageName, filter)

            reviewsNextPageUrl = reviewsCluster.nextPageUrl
            reviewsCluster.reviewList
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _reviews.value = it }
            .launchIn(viewModelScope)
    }

    private fun fetchExodusPrivacyReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _exodusReport.value = getLatestExodusReport(packageName)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch privacy report", exception)
                _exodusReport.value = null
            }
        }
    }

    fun fetchPlexusReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _plexusScores.value = getPlexusReport(packageName)?.report?.scores
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch compatibility report", exception)
                _plexusScores.value = null
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

    fun purchase(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = purchaseHelper.purchase(app.packageName, app.versionCode, app.offerType)
                _purchaseStatus.emit(files.isNotEmpty())
                if (files.isNotEmpty()) download(app.copy(fileList = files.toMutableList()))
            } catch (exception: Exception) {
                _purchaseStatus.emit(false)
                Log.e(TAG, "Failed to purchase the app ", exception)
            }
        }
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

    fun getExodusTrackersFromReport(): List<ExodusTracker> {
        val trackerObjects = exodusReport.value!!.trackers.map {
            exodusTrackers.getJSONObject(it.toString())
        }.toList()

        return trackerObjects.map {
            ExodusTracker(
                id = it.getInt("id"),
                name = it.getString("name"),
                url = it.getString("website"),
                signature = it.getString("code_signature"),
                date = it.getString("creation_date"),
                description = it.getString("description"),
                networkSignature = it.getString("network_signature"),
                documentation = listOf(it.getString("documentation")),
                categories = listOf(it.getString("categories"))
            )
        }.toList()
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
        return json.decodeFromString<PlexusReport>(String(playResponse.responseBytes))
    }

    private fun parseExodusResponse(response: String, packageName: String): List<Report> {
        try {
            val jsonObject = JSONObject(response)
            val exodusObject = jsonObject.getJSONObject(packageName)
            val exodusReport = json.decodeFromString<ExodusReport>(exodusObject.toString())

            return exodusReport.reports
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
