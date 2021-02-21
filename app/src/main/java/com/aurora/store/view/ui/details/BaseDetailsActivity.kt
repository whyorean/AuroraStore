/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.view.ui.details

import android.content.Intent
import android.os.Build
import android.text.Html
import android.text.Layout
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import com.airbnb.epoxy.EpoxyRecyclerView
import com.aurora.Constants
import com.aurora.gplayapi.data.models.*
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.store.R
import com.aurora.store.data.ViewState
import com.aurora.store.data.model.ExodusReport
import com.aurora.store.data.model.Report
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.*
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.NavigationUtil
import com.aurora.store.util.extensions.hide
import com.aurora.store.util.extensions.load
import com.aurora.store.util.extensions.show
import com.aurora.store.util.extensions.toast
import com.aurora.store.view.custom.RatingView
import com.aurora.store.view.epoxy.controller.DetailsCarouselController
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.view.epoxy.views.*
import com.aurora.store.view.epoxy.views.details.ReviewViewModel_
import com.aurora.store.view.epoxy.views.details.ScreenshotView
import com.aurora.store.view.epoxy.views.details.ScreenshotViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.viewmodel.details.DetailsClusterViewModel
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.json.JSONObject
import java.util.*


abstract class BaseDetailsActivity : BaseActivity() {

    private val exodusBaseUrl = "https://reports.exodus-privacy.eu.org/api/search/"
    private val exodusApiKey = "Token bbe6ebae4ad45a9cbacb17d69739799b8df2c7ae"

    //Sub Section Inflation
    fun inflateAppDescription(B: LayoutDetailsDescriptionBinding, app: App) {
        B.txtInstalls.text = CommonUtil.addDiPrefix(app.installs)
        B.txtSize.text = CommonUtil.addSiPrefix(app.size)
        B.txtRating.text = app.labeledRating
        B.txtSdk.text = ("Target SDK ${app.targetSdk}")
        B.txtUpdated.text = app.updatedOn

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            B.txtDescription.justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD
        }

        B.txtDescription.text = Html.fromHtml(app.shortDescription)

        app.changes.apply {
            if (isEmpty()) {
                B.txtChangelog.text = getString(R.string.details_changelog_unavailable)
            } else {
                B.txtChangelog.text = Html.fromHtml(this)
            }
        }

        B.headerDescription.addClickListener {
            openDetailsMoreActivity(app)
        }

        B.epoxyRecycler.withModels {
            setFilterDuplicates(true)
            var position = 0
            app.screenshots
                //.sortedWith { o1, o2 -> o2.height.compareTo(o1.height) }
                .forEach { artwork ->
                    add(
                        ScreenshotViewModel_()
                            .id(artwork.url)
                            .artwork(artwork)
                            .position(position++)
                            .callback(object : ScreenshotView.ScreenshotCallback {
                                override fun onClick(position: Int) {
                                    openScreenshotActivity(app, position)
                                }
                            })
                    )
                }
        }
    }

    fun inflateAppRatingAndReviews(B: LayoutDetailsReviewBinding, app: App) {
        B.averageRating.text = app.rating.average.toString()
        B.txtReviewCount.text = app.rating.abbreviatedLabel

        var totalStars = 0L
        totalStars += app.rating.oneStar
        totalStars += app.rating.twoStar
        totalStars += app.rating.threeStar
        totalStars += app.rating.fourStar
        totalStars += app.rating.fiveStar

        B.avgRatingLayout.apply {
            removeAllViews()
            addView(addAvgReviews(5, totalStars, app.rating.fiveStar))
            addView(addAvgReviews(4, totalStars, app.rating.fourStar))
            addView(addAvgReviews(3, totalStars, app.rating.threeStar))
            addView(addAvgReviews(2, totalStars, app.rating.twoStar))
            addView(addAvgReviews(1, totalStars, app.rating.oneStar))
        }

        B.averageRating.text = String.format(Locale.getDefault(), "%.1f", app.rating.average)
        B.txtReviewCount.text = app.rating.abbreviatedLabel

        val authData = AuthProvider.with(this).getAuthData()

        B.btnPostReview.setOnClickListener {
            if (authData.isAnonymous) {
                toast(R.string.toast_anonymous_restriction)
            } else {
                addOrUpdateReview(B, app, Review().apply {
                    title = authData.userProfile!!.name
                    rating = B.userStars.rating.toInt()
                    comment = B.inputReview.text.toString()
                })
            }
        }

        B.headerRatingReviews.addClickListener {
            openDetailsReviewActivity(app)
        }

        task {
            fetchFirstFewReviews(app)
        } successUi {
            B.epoxyRecycler.withModels {
                it.take(4)
                    .forEach {
                        add(
                            ReviewViewModel_()
                                .id(it.timeStamp)
                                .review(it)
                        )
                    }
            }
        } failUi {

        }

    }

    fun inflateAppPrivacy(B: LayoutDetailsPrivacyBinding, app: App) {

        task {
            fetchReport(app.packageName)
        } successUi { report ->
            if (report.trackers.isNotEmpty()) {
                B.txtStatus.apply {
                    setTextColor(
                        ContextCompat.getColor(
                            this@BaseDetailsActivity,
                            if (report.trackers.size > 4)
                                R.color.colorRed
                            else
                                R.color.colorOrange
                        )
                    )
                    text =
                        ("${report.trackers.size} ${getString(R.string.exodus_substring)} ${report.version}")
                }

                B.headerPrivacy.addClickListener {
                    NavigationUtil.openExodusActivity(this, app, report)
                }
            } else {
                B.txtStatus.apply {
                    setTextColor(
                        ContextCompat.getColor(
                            this@BaseDetailsActivity,
                            R.color.colorGreen
                        )
                    )
                    text = getString(R.string.exodus_no_tracker)
                }
            }
        } failUi {
            B.txtStatus.text = it.message
        }
    }

    fun inflateAppDevInfo(B: LayoutDetailsDevBinding, app: App) {
        if (app.developerAddress.isNotEmpty()) {
            B.devAddress.apply {
                setTxtSubtitle(
                    HtmlCompat.fromHtml(
                        app.developerAddress,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ).toString()
                )
                visibility = View.VISIBLE
            }
        }

        if (app.developerWebsite.isNotEmpty()) {
            B.devWeb.apply {
                setTxtSubtitle(app.developerWebsite)
                visibility = View.VISIBLE
            }
        }

        if (app.developerEmail.isNotEmpty()) {
            B.devWeb.apply {
                setTxtSubtitle(app.developerEmail)
                visibility = View.VISIBLE
            }
        }
    }

    fun inflateBetaSubscription(B: LayoutDetailsBetaBinding, app: App) {
        app.testingProgram?.let { betaProgram ->
            if (betaProgram.isAvailable) {
                B.root.show()

                updateBetaActions(B, betaProgram.isSubscribed)

                if (betaProgram.isSubscribedAndInstalled) {

                }

                B.imgBeta.load(betaProgram.artwork.url) {

                }

                B.btnBetaAction.setOnClickListener {
                    val authData = AuthProvider.with(this).getAuthData()
                    task {
                        B.btnBetaAction.text = getString(R.string.action_pending)
                        B.btnBetaAction.isEnabled = false
                        AppDetailsHelper(authData).testingProgram(
                            app.packageName,
                            !betaProgram.isSubscribed
                        )
                    } successUi {
                        B.btnBetaAction.isEnabled = true
                        if (it.subscribed) {
                            updateBetaActions(B, true)
                        }
                        if (it.unsubscribed) {
                            updateBetaActions(B, false)
                        }
                    } failUi {
                        updateBetaActions(B, betaProgram.isSubscribed)
                        toast(getString(R.string.details_beta_delay))
                    }
                }
            } else {
                B.root.hide()
            }
        }
    }

    fun inflateAppStream(epoxyRecyclerView: EpoxyRecyclerView, app: App) {
        app.detailsStreamUrl?.let {
            val VM = ViewModelProvider(this).get(DetailsClusterViewModel::class.java)

            val carouselController =
                DetailsCarouselController(object : GenericCarouselController.Callbacks {
                    override fun onHeaderClicked(streamCluster: StreamCluster) {
                        if (streamCluster.clusterBrowseUrl.isNotEmpty())
                            openStreamBrowseActivity(
                                streamCluster.clusterBrowseUrl,
                                streamCluster.clusterTitle
                            )
                        else
                            toast("Browse page unavailable")
                    }

                    override fun onClusterScrolled(streamCluster: StreamCluster) {
                        VM.observeCluster(streamCluster)
                    }

                    override fun onAppClick(app: App) {
                        openDetailsActivity(app)
                    }

                    override fun onAppLongClick(app: App) {

                    }
                })

            VM.liveData.observe(this, {
                when (it) {
                    is ViewState.Empty -> {
                    }
                    is ViewState.Loading -> {

                    }
                    is ViewState.Error -> {

                    }
                    is ViewState.Status -> {

                    }
                    is ViewState.Success<*> -> {
                        carouselController.setData(it.data as StreamBundle)
                    }
                }
            })

            epoxyRecyclerView.setController(carouselController)

            VM.getStreamBundle(it)
        }
    }

    private fun updateBetaActions(B: LayoutDetailsBetaBinding, isSubscribed: Boolean) {
        if (isSubscribed) {
            B.btnBetaAction.text = getString(R.string.action_leave)
            B.txtBetaTitle.text = getString(R.string.details_beta_subscribed)
        } else {
            B.btnBetaAction.text = getString(R.string.action_join)
            B.txtBetaTitle.text = getString(R.string.details_beta_available)
        }
    }

    //Helpers

    open fun getIntentPackageName(intent: Intent): String? {
        if (intent.hasExtra(Constants.STRING_EXTRA)) {
            return intent.getStringExtra(Constants.STRING_EXTRA)
        } else if (intent.scheme != null && (intent.scheme == "market" || intent.scheme == "http" || intent.scheme == "https")
        ) {
            return intent.data!!.getQueryParameter("id")
        } else if (intent.extras != null) {
            val bundle = intent.extras
            return bundle!!.getString(Constants.STRING_EXTRA)
        }
        return null
    }

    private fun addAvgReviews(number: Int, max: Long, rating: Long): RelativeLayout {
        return RatingView(this, number, max.toInt(), rating.toInt())
    }

    private fun addOrUpdateReview(
        B: LayoutDetailsReviewBinding,
        app: App,
        review: Review,
        isBeta: Boolean = false
    ) {
        task {
            val authData = AuthProvider.with(this).getAuthData()
            ReviewsHelper(authData)
                .using(HttpClient.getPreferredClient())
                .addOrEditReview(
                    app.packageName,
                    review.title,
                    review.comment,
                    review.rating,
                    isBeta
                )
        }.successUi {
            it?.let {
                B.userStars.rating = it.rating.toFloat()
                Toast.makeText(this, "Rated successfully", Toast.LENGTH_SHORT).show()
            }
        }.failUi {
            Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFirstFewReviews(app: App): List<Review> {
        val authData = AuthProvider
            .with(this)
            .getAuthData()
        val reviewsHelper = ReviewsHelper(authData)
            .using(HttpClient.getPreferredClient())
        return reviewsHelper.getReviews(app.packageName, Review.Filter.CRITICAL)
    }

    /*---------------------------------- HELPERS FOR APP PRIVACY ---------------------------------*/

    private fun parseResponse(response: String, packageName: String): List<Report> {
        try {
            val jsonObject = JSONObject(response)
            val exodusObject = jsonObject.getJSONObject(packageName)
            val exodusReport: ExodusReport = gson.fromJson(
                exodusObject.toString(),
                ExodusReport::class.java
            )
            return exodusReport.reports
        } catch (e: Exception) {
            throw Exception("No reports found")
        }
    }

    private fun fetchReport(packageName: String): Report {
        val headers: MutableMap<String, String> = mutableMapOf()
        headers["Content-Type"] = "application/json"
        headers["Accept"] = "application/json"
        headers["Authorization"] = exodusApiKey

        val url = exodusBaseUrl + packageName

        val playResponse = HttpClient
            .getPreferredClient()
            .get(url, headers)

        if (playResponse.isSuccessful) {
            return parseResponse(String(playResponse.responseBytes), packageName)[0]
        } else {
            throw Exception("Failed to fetch report")
        }
    }
}
