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
import com.aurora.extensions.toast
import com.aurora.gplayapi.PlayClient
import com.aurora.gplayapi.PlayException
import com.aurora.gplayapi.api.TopChartType
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.Category
import com.aurora.gplayapi.model.ContentType
import com.aurora.gplayapi.model.DataSafetyReport
import com.aurora.gplayapi.model.Review
import com.aurora.gplayapi.model.StreamBundle
import com.aurora.gplayapi.model.StreamCluster
import com.aurora.gplayapi.model.TopChart
import com.aurora.gplayapi.session.Capability
import com.aurora.gplayapi.web.WebPlayClient
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.AccountRepository
import com.aurora.store.data.ExodusRepository
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.model.AppState
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.PlexusReport
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.Scores
import com.aurora.store.data.model.StorageRequirement
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
import com.aurora.store.util.StorageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    val authProvider: AuthProvider,
    @ApplicationContext private val context: Context,
    private val playClient: PlayClient,
    private val webPlayClient: WebPlayClient,
    private val downloadHelper: DownloadHelper,
    private val favouriteDao: FavouriteDao,
    private val reviewDao: ReviewDao,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val accountRepository: AccountRepository,
    private val exodusRepository: ExodusRepository
) : ViewModel() {

    private val _app = MutableStateFlow<App?>(null)
    val app = _app.asStateFlow()

    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state = _state.asStateFlow()

    private val _storageWarning = MutableSharedFlow<StorageRequirement>()
    val storageWarning = _storageWarning.asSharedFlow()

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

    private val _categoryIconUrl = MutableStateFlow<String?>(null)
    val categoryIconUrl = _categoryIconUrl.asStateFlow()

    private val _dataSafetyReport = MutableStateFlow<DataSafetyReport?>(null)
    val dataSafetyReport = _dataSafetyReport.asStateFlow()

    private val _exodusReport = MutableStateFlow<Report?>(Report())
    val exodusReport = _exodusReport.asStateFlow()

    private val _exodusReports = MutableStateFlow<List<Report>>(emptyList())
    val exodusReports = _exodusReports.asStateFlow()

    private val _versionLookupInProgress = MutableStateFlow(false)
    val versionLookupInProgress = _versionLookupInProgress.asStateFlow()

    private val _plexusScores = MutableStateFlow<Scores?>(Scores())
    val plexusScores = _plexusScores.asStateFlow()

    private val _testingProgramUpdating = MutableStateFlow(false)
    val testingProgramUpdating = _testingProgramUpdating.asStateFlow()

    // Kept apart from app: App.equals compares only the package name
    private val _testingProgramSubscribed = MutableStateFlow<Boolean?>(null)
    val testingProgramSubscribed = _testingProgramSubscribed.asStateFlow()

    private val _favourite = MutableStateFlow(false)
    val favourite = _favourite.asStateFlow()

    private val _installError = MutableStateFlow<InstallError?>(null)
    val installError = _installError.asStateFlow()

    data class InstallError(val error: String?, val extra: String?)

    // Gated decision 1 (phase-6 migration plan): whether the current session can acquire an
    // app at all, independent of whether that app itself is free. Aurora's own `isAnonymous`
    // means "dispensed Play account", a different concept from a gplayapi-v4 anonymous
    // session, so Install is gated on capability rather than on account presence - otherwise
    // every anonymously-fetched app (whose V2 proto `Price` is always empty, so `isFree` is
    // always false) would lose Install catalogue-wide.
    private val _canDownload = MutableStateFlow(false)
    val canDownload = _canDownload.asStateFlow()

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
            it.certificateHash in CertUtil.getEncodedCertificateHashes(
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
        viewModelScope.launch { _canDownload.value = playClient.supports(Capability.DOWNLOAD) }
    }

    fun fetchAppDetails(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _testingProgramSubscribed.value = null
                _app.value = playClient.apps.details(packageName).copy(
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
            } catch (exception: PlayException.Unauthorized) {
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
            fetchCategoryIcon(app.value!!.categoryStreamUrl.orEmpty())
            fetchSuggestions()
            fetchExodusPrivacyReport(packageName)
            if (app.value!!.requiresGMS()) fetchPlexusReport(packageName)
        }
    }

    suspend fun topChartCluster(chart: TopChart, title: String): StreamCluster? {
        val type = TopChartType.entries.firstOrNull { it.value == chart.chartId } ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { webPlayClient.charts.cluster(chart.category, type) }
                .onFailure { Log.e(TAG, "Failed to fetch top chart", it) }
                .getOrNull()
                ?.takeIf { it.apps.isNotEmpty() }
                ?.copy(title = title)
        }
    }

    private fun fetchCategoryIcon(browseUrl: String) {
        if (browseUrl.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _categoryIconUrl.value = ContentType.entries.firstNotNullOfOrNull { type ->
                val categories = categoryCache.getOrPut(type) {
                    runCatching { playClient.categories.all(type) }.getOrDefault(emptyList())
                }
                categories.firstOrNull { it.browseUrl == browseUrl }?.imageUrl
            }
        }
    }

    fun updateTestingProgramStatus(packageName: String, subscribe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _testingProgramUpdating.value = true
            val enrolled = try {
                val status = playClient.apps.testingProgram(packageName, subscribe)
                if (subscribe) status.subscribed else !status.unsubscribed
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to update testing program status", exception)
                !subscribe
            }
            _testingProgramSubscribed.value = enrolled
            context.toast(
                when {
                    enrolled != subscribe -> R.string.toast_beta_failed
                    subscribe -> R.string.details_beta_delay
                    else -> R.string.toast_beta_left
                }
            )
            _testingProgramUpdating.value = false
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
                val apiReview = playClient.reviews.userReview(app.packageName, isTesting)
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
                // v4's add() throws rather than returning null on a rejected review (unlike v3's
                // addOrEditReview); the catch below is what used to be the "posted == null" branch.
                val posted = playClient.reviews.add(
                    packageName,
                    review.title,
                    review.comment,
                    review.rating,
                    isBeta
                )
                // Cache as pending (not yet synced): Play accepted it but getUserReview may
                // not return it until publishing finishes. Marking it synced prematurely would
                // let the next reconcile mistake the publishing delay for a deletion.
                reviewDao.upsert(
                    LocalReview.fromReview(posted, packageName, email, synced = false)
                )
                _reviewPosted.tryEmit(true)
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
                playClient.reviews.delete(app.packageName, isTesting)
                // Only drop the local copy once Play has accepted the deletion, so a failed
                // network call leaves the review visible rather than silently disappearing.
                if (email.isNotBlank()) reviewDao.delete(app.packageName, email)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to delete review", exception)
            }
        }
    }

    val accounts = accountRepository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun enqueueDownload(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            if (hasSpaceFor(app)) downloadHelper.enqueueApp(app)
        }
    }

    fun enqueueDownloadWith(app: App, accountId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (hasSpaceFor(app)) downloadHelper.enqueueApp(app, accountId)
        }
    }

    private suspend fun hasSpaceFor(app: App): Boolean {
        if (!downloadHelper.needsDownload(app.packageName, app.versionCode)) return true

        val requirement = StorageUtil.check(context, listOf(app.size), app.displayName)
        if (!requirement.isSufficient) _storageWarning.emit(requirement)
        return requirement.isSufficient
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
                if (event is InstallerEvent.Failed && !event.cancelled) {
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
                _featuredReviews.value = playClient.reviews.summary(packageName).reviews
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
            try {
                _dataSafetyReport.value = webPlayClient.dataSafety.fetch(packageName)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch data safety report", exception)
                _dataSafetyReport.value = null
            }
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
                val pageBundle = playClient.apps.detailsStream(streamUrl)
                val pageClusters = pageBundle.clusters.filter {
                    it.title.isNotBlank() && it.apps.isNotEmpty()
                }
                suggestionsState = pageBundle.copy(clusters = pageClusters)
                _suggestionsBundle.value = suggestionsState
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch suggestions stream", exception)
                _suggestionsBundle.value = StreamBundle.EMPTY
            }
        }
    }

    fun loadMoreCluster(cluster: StreamCluster) {
        val cursor = cluster.next ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextPage = playClient.streams.next(cursor)
                val mergedCluster = cluster.copy(
                    apps = cluster.apps + nextPage.apps,
                    next = nextPage.next
                )
                suggestionsState = suggestionsState.copy(
                    clusters = suggestionsState.clusters.map {
                        if (it.id == cluster.id) mergedCluster else it
                    }
                )
                _suggestionsBundle.value = suggestionsState
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch next cluster page", exception)
            }
        }
    }

    private fun fetchExodusPrivacyReport(packageName: String) {
        viewModelScope.launch {
            val reports = exodusRepository.fetchReports(packageName)
                .sortedByDescending { it.versionCode.toLongOrNull() ?: -1L }
            _exodusReport.value = reports.firstOrNull()
            _exodusReports.value = reports
        }
    }

    /**
     * Loads the list of known versions from Exodus for the version picker on the manual download
     * screen. Reuses the reports already fetched for the privacy report when available, and only
     * hits the network when nothing has been loaded yet.
     */
    fun lookupVersions() {
        val packageName = app.value?.packageName ?: return
        if (_exodusReports.value.isNotEmpty() || _versionLookupInProgress.value) return

        viewModelScope.launch {
            _versionLookupInProgress.value = true
            try {
                _exodusReports.value = exodusRepository.fetchReports(packageName)
                    .sortedByDescending { it.versionCode.toLongOrNull() ?: -1L }
            } finally {
                _versionLookupInProgress.value = false
            }
        }
    }

    suspend fun getNewTrackers(packageName: String, installedVersionCode: Long) =
        exodusRepository.getNewTrackers(packageName, installedVersionCode)

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

    private fun getPlexusReport(packageName: String): PlexusReport? {
        val url = "${Constants.PLEXUS_API_URL}/$packageName/?scores=true".toHttpUrl()
        val body = okHttpClient.newCall(Request(url = url)).execute().use { response ->
            response.body.string()
        }
        return json.decodeFromString<PlexusReport>(body)
    }

    private companion object {
        val categoryCache = ConcurrentHashMap<ContentType, List<Category>>()
    }
}
