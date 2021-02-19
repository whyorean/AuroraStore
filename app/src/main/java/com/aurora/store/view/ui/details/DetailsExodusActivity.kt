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

import android.os.Bundle
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.Report
import com.aurora.store.data.providers.ExodusDataProvider
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.util.extensions.browse
import com.aurora.store.util.extensions.close
import com.aurora.store.view.epoxy.views.HeaderViewModel_
import com.aurora.store.view.epoxy.views.details.ExodusViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import org.json.JSONObject

class DetailsExodusActivity : BaseActivity() {

    private lateinit var B: ActivityGenericRecyclerBinding
    private lateinit var app: App
    private lateinit var report: Report

    override fun onConnected() {

    }

    override fun onDisconnected() {

    }

    override fun onReconnected() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityGenericRecyclerBinding.inflate(layoutInflater)

        setContentView(B.root)

        val rawApp: String? = intent.getStringExtra(Constants.STRING_APP)
        val rawExodusTrackers: String? = intent.getStringExtra(Constants.STRING_EXTRA)

        if (rawApp != null) {
            app = gson.fromJson(rawApp, App::class.java)
            report = gson.fromJson(
                rawExodusTrackers,
                Report::class.java
            )
            app.let {
                attachToolbar()
                report.let {
                    updateController(getExodusTrackersFromReport(report))
                }
            }
        }
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.toolbar.setOnClickListener {
            close()
        }
        B.layoutToolbarAction.txtTitle.text = app.displayName
    }

    private fun updateController(reviews: List<ExodusTracker>) {
        B.recycler.withModels {
            add(
                HeaderViewModel_()
                    .id("header")
                    .title(getString(R.string.exodus_view_report))
                    .browseUrl("browse")
                    .click { _ -> browse(Constants.EXODUS_REPORT_URL + report.id) }
            )
            reviews.forEach {
                add(
                    ExodusViewModel_()
                        .id(it.id)
                        .tracker(it)
                        .click { _ ->
                            browse(it.url)
                        }
                )
            }
        }
    }

    private fun getExodusTrackersFromReport(report: Report): List<ExodusTracker> {
        val trackerObjects: List<JSONObject> = fetchLocalTrackers(report.trackers)
        return trackerObjects.map {
            ExodusTracker().apply {
                id = it.getInt("id")
                name = it.getString("name")
                url = it.getString("website")
                signature = it.getString("code_signature")
                date = it.getString("creation_date")
            }
        }.toList()
    }

    private fun fetchLocalTrackers(trackerIds: List<Int>): List<JSONObject> {
        return try {
            ExodusDataProvider
                .with(this)
                .getFilteredTrackers(trackerIds)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
