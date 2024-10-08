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
import com.aurora.gplayapi.helpers.web.WebDataSafetyHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.data.model.ExodusReport
import com.aurora.store.data.model.Report
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.room.favourites.Favourite
import com.aurora.store.data.room.favourites.FavouriteDao
import com.aurora.store.data.helper.DownloadHelper
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.reflect.Modifier
import javax.inject.Inject
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadHelper: DownloadHelper,
    private val authProvider: AuthProvider,
    private val favouriteDao: FavouriteDao,
    private val httpClient: IHttpClient
) : ViewModel() {

    private val TAG = AppDetailsViewModel::class.java.simpleName

    private val exodusBaseUrl = "https://reports.exodus-privacy.eu.org/api/search/"
    private val exodusApiKey = "Token bbe6ebae4ad45a9cbacb17d69739799b8df2c7ae"

    private val appDetailsHelper = AppDetailsHelper(authProvider.authData!!).using(httpClient)
    private val reviewsHelper = ReviewsHelper(authProvider.authData!!).using(httpClient)

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

    private val testProgramStatusStash = mutableMapOf<String, TestingProgramStatus?>()
    private val _testingProgramStatus = MutableSharedFlow<TestingProgramStatus?>()
    val testingProgramStatus = _testingProgramStatus.asSharedFlow()

    private val _favourite = MutableStateFlow<Boolean>(false)
    val favourite = _favourite.asStateFlow()

    val downloadsList get() = downloadHelper.downloadsList

    fun fetchAppDetails(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                checkFavourite(packageName)

                val app: App = appStash.getOrPut(packageName) {
                    appDetailsHelper.getAppByPackageName(packageName)
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
                    WebDataSafetyHelper()
                        .using(httpClient)
                        .fetch(packageName)
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

    fun postAppReview(packageName: String, review: Review, isBeta: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userReview = userReviewStash.getOrPut(packageName) {
                    reviewsHelper.addOrEditReview(
                        packageName,
                        review.title,
                        review.comment,
                        review.rating,
                        isBeta
                    )
                }

                if (userReview != null) {
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
        headers["Content-Type"] = "application/json"
        headers["Accept"] = "application/json"
        headers["Authorization"] = exodusApiKey

        val url = exodusBaseUrl + packageName
        val playResponse = httpClient.get(url, headers)

        val report = parseExodusResponse(String(playResponse.responseBytes), packageName)
            .firstOrNull()

        return report
    }

    private fun parseExodusResponse(response: String, packageName: String): List<Report> {
        try {
            val gson = GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                .create()

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
