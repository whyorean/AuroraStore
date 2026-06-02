/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-FileCopyrightText: 2023-2024 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.details

import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.Constants
import com.aurora.extensions.TAG
import com.aurora.extensions.requiresGMS
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport
import com.aurora.gplayapi.data.models.details.TestingProgramStatus
import com.aurora.gplayapi.exceptions.GooglePlayException
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.gplayapi.helpers.web.WebDataSafetyHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.AuroraApp
import com.aurora.store.BuildConfig
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.model.AppState
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.ExodusReport
import com.aurora.store.data.model.PlexusReport
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.Scores
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.data.room.favourite.FavouriteDao
import com.aurora.store.data.room.review.LocalReview
import com.aurora.store.data.room.review.LocalReview.Companion.toReview
import com.aurora.store.data.room.review.ReviewDao
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    val authProvider: AuthProvider,
    @ApplicationContext private val context: Context,
    private val appDetailsHelper: AppDetailsHelper,
    private val reviewsHelper: ReviewsHelper,
    private val webDataSafetyHelper: WebDataSafetyHelper,
    private val downloadHelper: DownloadHelper,
    private val favouriteDao: FavouriteDao,
    private val reviewDao: ReviewDao,
    private val httpClient: IHttpClient,
    private val json: Json
) : ViewModel() {

    private val _app = MutableStateFlow<App?>(null)
    val app = _app.asStateFlow()

    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state = _state.asStateFlow()

    private val _suggestionsBundle = MutableStateFlow<StreamBundle?>(null)
    val suggestionsBundle: StateFlow<StreamBundle?> = _suggestionsBundle.asStateFlow()

    private var suggestionsState: StreamBundle = StreamBundle.EMPTY

    private val _featuredReviews = MutableStateFlow<List<Review>>(emptyList())
    val featuredReviews = _featuredReviews.asStateFlow()

    // The user's own review for the loaded app, backed by Room so it is shown instantly (even
    // offline) and survives restarts while Google takes time to publish it. Scoped by account.
    @OptIn(ExperimentalCoroutinesApi::class)
    val userReview: StateFlow<Review?> = app
        .filterNotNull()
        .flatMapLatest { loadedApp ->
            reviewDao.review(loadedApp.packageName, accountEmail).map { it?.toReview() }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // One-shot signal carrying whether the latest review submission succeeded
    private val _reviewPosted = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val reviewPosted = _reviewPosted.asSharedFlow()

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

    private val _installError = MutableStateFlow<InstallError?>(null)
    val installError = _installError.asStateFlow()

    data class InstallError(val error: String?, val extra: String?)

    private val download = combine(app, downloadHelper.downloadsList) { a, list ->
        if (a?.packageName.isNullOrBlank()) return@combine null
        list.find { d -> d.packageName == a.packageName }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // E-mail of the signed-in account; blank for anonymous sessions. Used to scope cached reviews.
    private val accountEmail: String
        get() = authProvider.authData?.email.orEmpty()

    private val isInstalled: Boolean
        get() = PackageUtil.isInstalled(context, app.value!!.packageName)

    private val isUpdatable: Boolean
        get() = PackageUtil.isUpdatable(context, app.value!!.packageName, app.value!!.versionCode)

    private val isArchived: Boolean
        get() = PackageUtil.isArchived(context, app.value!!.packageName)

    private val isExtendedUpdateEnabled: Boolean
        get() = Preferences.getBoolean(context, PREFERENCE_UPDATES_EXTENDED)

    private val hasValidCerts: Boolean
        get() = app.value!!.certificateSetList.any {
            it.certificateSet in CertUtil.getEncodedCertificateHashes(
                context,
                app.value!!.packageName
            )
        }

    private val hasValidUpdate: Boolean
        get() = (isUpdatable && hasValidCerts) || (isUpdatable && isExtendedUpdateEnabled)

    private val defaultAppState: AppState
        get() = when {
            isInstalled -> {
                if (hasValidUpdate) {
                    AppState.Updatable
                } else {
                    val info = PackageUtil.getPackageInfo(context, app.value!!.packageName)
                    AppState.Installed(
                        versionName = info.versionName ?: String(),
                        versionCode = PackageInfoCompat.getLongVersionCode(info)
                    )
                }
            }

            else -> if (isArchived) AppState.Archived else AppState.Unavailable
        }

    init {
        observeAppState()
    }

    fun fetchAppDetails(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _app.value = appDetailsHelper.getAppByPackageName(packageName).copy(
                    isInstalled = PackageUtil.isInstalled(context, packageName)
                )
                val existingDownload = downloadHelper.getDownload(packageName)

                // A COMPLETED record for an app that is no longer installed means the app was
                // installed then removed while Aurora held a stale record.
                // Remove it so the live download observer doesn't lock the UI in Installing state
                // indefinitely.
                if (existingDownload?.status == DownloadStatus.COMPLETED && !isInstalled) {
                    downloadHelper.removeDownload(packageName)
                    _state.value = defaultAppState
                } else {
                    // Seed state from any in-flight download for this package so reopening
                    // the screen doesn't briefly flash the default install action while the
                    // download flow catches up.
                    _state.value =
                        existingDownload?.let { stateFromDownload(it) } ?: defaultAppState
                }
            } catch (exception: GooglePlayException.AuthException) {
                // The saved Play token has been rejected mid-session. Hand off to
                // Splash to re-validate and rebuild auth, and ask it to bring the
                // user back to this app's details once auth is good again.
                Log.w(TAG, "App details fetch returned ${exception.code}, redirecting to Splash")
                AuroraApp.events.send(AuthEvent.SessionExpired(packageName))
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch app details", exception)
                _app.value = null
                _state.value = AppState.Error(exception.message)
            }
        }.invokeOnCompletion { throwable ->
            // Only proceed if there was no error while fetching the app details
            if (throwable != null || app.value == null) return@invokeOnCompletion

            fetchFavourite(packageName)
            fetchFeaturedReviews(packageName)
            // Reviews can only be submitted for installed apps with a personal account, so the
            // user's existing review is only relevant (and fetchable) in that case.
            if (!authProvider.isAnonymous && app.value!!.isInstalled) {
                fetchUserAppReview(app.value!!)
            }
            fetchDataSafetyReport(packageName)
            fetchSuggestions()
            fetchExodusPrivacyReport(packageName)
            if (app.value!!.requiresGMS()) fetchPlexusReport(packageName)
        }
    }

    fun updateTestingProgramStatus(packageName: String, subscribe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _testingProgramStatus.value =
                    appDetailsHelper.testingProgram(packageName, subscribe)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch testing program status", exception)
            }
        }
    }

    /**
     * Reconciles the locally cached review with the authoritative Play API:
     * - If Play returns a review, it is mirrored locally and marked synced (this also picks up
     *   edits made directly on the Play Store).
     * - If Play returns nothing but we had previously confirmed a review, it was deleted on the
     *   Play Store, so the local copy is dropped.
     * - If Play returns nothing for a review that was never confirmed, it is still being published,
     *   so the local (pending) copy is kept so the user keeps seeing what they submitted.
     */
    fun fetchUserAppReview(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            val email = accountEmail
            if (email.isBlank()) return@launch
            try {
                val isTesting = app.testingProgram?.isSubscribed ?: false
                val apiReview = reviewsHelper.getUserReview(app.packageName, isTesting)
                if (apiReview != null) {
                    reviewDao.upsert(
                        LocalReview.fromReview(apiReview, app.packageName, email, synced = true)
                    )
                } else {
                    val cached = reviewDao.get(app.packageName, email)
                    if (cached != null && cached.synced) {
                        reviewDao.delete(app.packageName, email)
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch user review", exception)
            }
        }
    }

    fun postAppReview(packageName: String, review: Review, isBeta: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val email = accountEmail
            try {
                val posted = reviewsHelper.addOrEditReview(
                    packageName,
                    review.title,
                    review.comment,
                    review.rating,
                    isBeta
                )
                if (posted != null) {
                    // Cache as pending (not yet synced): Play accepted it but getUserReview may
                    // not return it until publishing finishes. Marking it synced prematurely would
                    // let the next reconcile mistake the publishing delay for a deletion.
                    reviewDao.upsert(
                        LocalReview.fromReview(posted, packageName, email, synced = false)
                    )
                }
                _reviewPosted.tryEmit(posted != null)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to post review", exception)
                _reviewPosted.tryEmit(false)
            }
        }
    }

    fun deleteAppReview(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            val email = accountEmail
            try {
                val isTesting = app.testingProgram?.isSubscribed ?: false
                reviewsHelper.deleteReview(app.packageName, isTesting)
                // Only drop the local copy once Play has accepted the deletion, so a failed
                // network call leaves the review visible rather than silently disappearing.
                if (email.isNotBlank()) reviewDao.delete(app.packageName, email)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to delete review", exception)
            }
        }
    }

    fun enqueueDownload(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadHelper.enqueueApp(app)
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
                        added = System.currentTimeMillis()
                    )
                )
            }

            _favourite.value = !favourite.value
        }
    }

    fun dismissInstallError() {
        _installError.value = null
    }

    private fun observeAppState() {
        AuroraApp.events.installerEvent
            .filter { it.packageName == app.value?.packageName }
            .onEach { event ->
                if (event is InstallerEvent.Failed) {
                    _installError.value = InstallError(event.error, event.extra)
                }
                _state.value = when {
                    event is InstallerEvent.Installing -> AppState.Installing(event.progress)
                    else -> defaultAppState
                }
            }.launchIn(viewModelScope)

        download.filterNotNull().onEach {
            _state.value = stateFromDownload(it)
        }.launchIn(viewModelScope)
    }

    // COMPLETED is bridged to Installing so the UI doesn't briefly fall back to the
    // install action between download finishing and the installer's first event.
    // A stale COMPLETED row after install actually finished is handled by the
    // isInstalled check.
    private fun stateFromDownload(download: Download): AppState = when (download.status) {
        DownloadStatus.DOWNLOADING -> AppState.Downloading(
            download.progress.toFloat(),
            download.speed,
            download.timeRemaining
        )

        DownloadStatus.QUEUED -> AppState.Queued

        DownloadStatus.PURCHASING -> AppState.Purchasing

        DownloadStatus.VERIFYING -> AppState.Verifying

        DownloadStatus.COMPLETED,
        DownloadStatus.INSTALLING -> if (isInstalled) defaultAppState else AppState.Installing(0F)

        else -> defaultAppState
    }

    private fun fetchFeaturedReviews(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _featuredReviews.value = reviewsHelper.getReviewSummary(packageName)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch featured app reviews", exception)
                _featuredReviews.value = emptyList()
            }
        }
    }

    private fun fetchFavourite(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _favourite.value = favouriteDao.isFavourite(packageName)
        }
    }

    private fun fetchDataSafetyReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _dataSafetyReport.value = webDataSafetyHelper.fetch(packageName)
        }
    }

    private fun fetchSuggestions() {
        val streamUrl = app.value!!.detailsStreamUrl

        // Bail out if we got no suggestions to offer
        if (streamUrl.isNullOrBlank()) {
            _suggestionsBundle.value = StreamBundle.EMPTY
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pageBundle = appDetailsHelper.getDetailsStream(streamUrl.hashCode(), streamUrl)
                val pageClusters = pageBundle.streamClusters.filterValues {
                    it.clusterTitle.isNotBlank() && it.clusterAppList.isNotEmpty()
                }
                suggestionsState = pageBundle.copy(streamClusters = pageClusters)
                _suggestionsBundle.value = suggestionsState
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch suggestions stream", exception)
                _suggestionsBundle.value = StreamBundle.EMPTY
            }
        }
    }

    fun loadMoreCluster(cluster: StreamCluster) {
        if (!cluster.hasNext()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextPage = appDetailsHelper.getNextStreamCluster(
                    cluster.id,
                    cluster.clusterNextPageUrl
                )
                val existing = suggestionsState.streamClusters[cluster.id] ?: return@launch
                val mergedCluster = existing.copy(
                    clusterAppList = existing.clusterAppList + nextPage.clusterAppList,
                    clusterNextPageUrl = nextPage.clusterNextPageUrl
                )
                suggestionsState = suggestionsState.copy(
                    streamClusters = suggestionsState.streamClusters + (cluster.id to mergedCluster)
                )
                _suggestionsBundle.value = suggestionsState
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch next cluster page", exception)
            }
        }
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

    private fun fetchPlexusReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _plexusScores.value = getPlexusReport(packageName)?.report?.scores
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch compatibility report", exception)
                _plexusScores.value = null
            }
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

    private fun parseExodusResponse(response: String, packageName: String): List<Report> {
        try {
            val jsonObject = JSONObject(response)
            val exodusObject = jsonObject.getJSONObject(packageName)
            val exodusReport = json.decodeFromString<ExodusReport>(exodusObject.toString())

            return exodusReport.reports
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun getPlexusReport(packageName: String): PlexusReport? {
        val url = "${Constants.PLEXUS_API_URL}/$packageName/?scores=true"
        val playResponse = httpClient.get(url, emptyMap())
        return json.decodeFromString<PlexusReport>(String(playResponse.responseBytes))
    }
}
