package com.aurora.store.viewmodel.details

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.Constants
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.details.TestingProgramStatus
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.gplayapi.helpers.web.WebDataSafetyHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.BuildConfig
import com.aurora.store.R
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
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

    private val appStash: MutableMap<String, App> = mutableMapOf()
    private val _app = MutableSharedFlow<App>()
    val app = _app.asSharedFlow()

    private val reviewsStash = mutableMapOf<String, List<Review>>()
    private val _reviews = MutableSharedFlow<List<Review>>()
    val reviews = _reviews.asSharedFlow()

    private val userReviewStash = mutableMapOf<String, Review?>()
    private val _userReview = MutableSharedFlow<Review>()
    val userReview = _userReview.asSharedFlow()

    private val dataSafetyReportStash = mutableMapOf<String, DataSafetyReport>()
    private val _dataSafetyReport = MutableSharedFlow<DataSafetyReport>()
    val dataSafetyReport = _dataSafetyReport.asSharedFlow()

    private val exodusReportStash = mutableMapOf<String, Report?>()
    private val _exodusReport = MutableSharedFlow<Report?>()
    val exodusReport = _exodusReport.asSharedFlow()

    private val plexusReportStash = mutableMapOf<String, PlexusReport?>()
    private val _plexusReport = MutableSharedFlow<PlexusReport?>()
    val plexusReport = _plexusReport.asSharedFlow()

    private val testProgramStatusStash = mutableMapOf<String, TestingProgramStatus?>()
    private val _testingProgramStatus = MutableSharedFlow<TestingProgramStatus?>()
    val testingProgramStatus = _testingProgramStatus.asSharedFlow()

    private val _favourite = MutableStateFlow<Boolean>(false)
    val favourite = _favourite.asStateFlow()

    val download = combine(app, downloadHelper.downloadsList) { a, list ->
        if (a.packageName.isBlank()) return@combine null
        list.find { d -> d.packageName == a.packageName }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun fetchAppDetails(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                checkFavourite(packageName)

                val app: App = appStash.getOrPut(packageName) {
                    appDetailsHelper.getAppByPackageName(packageName).apply {
                        isInstalled = PackageUtil.isInstalled(context, packageName)
                    }
                }

                _app.emit(app)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch app details", exception)
                _app.emit(App(""))
            }
        }
    }

    fun fetchAppReviews(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reviews = reviewsStash.getOrPut(packageName) {
                    reviewsHelper.getReviewSummary(packageName)
                }

                _reviews.emit(reviews)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch app reviews", exception)
                _reviews.emit(emptyList())
            }
        }
    }

    fun fetchAppDataSafetyReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val report = dataSafetyReportStash.getOrPut(packageName) {
                    webDataSafetyHelper.fetch(packageName)
                }
                _dataSafetyReport.emit(report)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch data safety report", exception)
            }
        }
    }

    fun fetchAppReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exodusReport = exodusReportStash.getOrPut(packageName) {
                    getLatestExodusReport(packageName)
                }

                _exodusReport.emit(exodusReport)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch privacy report", exception)
                exodusReportStash[packageName] = null
                _exodusReport.emit(null)
            }
        }
    }

    fun fetchPlexusReport(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val plexusReport = plexusReportStash.getOrPut(packageName) {
                    val url = "${Constants.PLEXUS_API_URL}/${packageName}/?scores=true"
                    val playResponse = httpClient.get(url, emptyMap())
                    gson.fromJson(String(playResponse.responseBytes), PlexusReport::class.java)
                }

                _plexusReport.emit(plexusReport)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch compatibility report", exception)
                plexusReportStash[packageName] = null
                _plexusReport.emit(null)
            }
        }
    }

    fun fetchTestingProgramStatus(packageName: String, subscribe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val testingProgramStatus = testProgramStatusStash.getOrPut(packageName) {
                    appDetailsHelper.testingProgram(packageName, subscribe)
                }

                _testingProgramStatus.emit(testingProgramStatus)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch testing program status", exception)
                testProgramStatusStash[packageName] = null
                _testingProgramStatus.emit(null)
            }
        }
    }

    fun fetchUserAppReview(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stashedUserReview = userReviewStash[app.packageName]
                if (stashedUserReview != null) {
                    _userReview.emit(stashedUserReview)
                    return@launch
                }

                val isTesting = app.testingProgram?.isSubscribed ?: false
                val userReview = reviewsHelper.getUserReview(app.packageName, isTesting)

                if (userReview != null) {
                    userReviewStash[app.packageName] = userReview
                    _userReview.emit(userReview)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch user review", exception)
            }
        }
    }

    fun postAppReview(packageName: String, review: Review, isBeta: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userReview = reviewsHelper.addOrEditReview(
                    packageName,
                    review.title,
                    review.comment,
                    review.rating,
                    isBeta
                )

                if (userReview != null) {
                    context.toast(R.string.toast_rated_success)
                    userReviewStash[packageName] = userReview
                    _userReview.emit(userReview)
                }
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
        val headers: MutableMap<String, String> = mutableMapOf()
        headers["Content-Type"] = Constants.JSON_MIME_TYPE
        headers["Accept"] = Constants.JSON_MIME_TYPE
        headers["Authorization"] = "Token ${BuildConfig.EXODUS_API_KEY}"

        val url = Constants.EXODUS_SEARCH_URL + packageName
        val playResponse = httpClient.get(url, headers)

        val report = parseExodusResponse(String(playResponse.responseBytes), packageName)
            .firstOrNull()

        return report
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
