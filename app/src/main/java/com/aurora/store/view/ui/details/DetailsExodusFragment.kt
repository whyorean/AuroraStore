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
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.Constants
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.Report
import com.aurora.store.databinding.ActivityGenericPagerBinding
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.view.epoxy.views.HeaderViewModel_
import com.aurora.store.view.epoxy.views.details.ExodusViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class DetailsExodusFragment : BaseFragment<ActivityGenericRecyclerBinding>() {

    private val args: DetailsExodusFragmentArgs by navArgs()

    @Inject
    lateinit var exodusTrackers: JSONObject

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.layoutToolbarAction.apply {
            txtTitle.text = args.displayName
            toolbar.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        updateController(getExodusTrackersFromReport(args.report))
    }

    private fun updateController(reviews: List<ExodusTracker>) {
        binding.recycler.withModels {
            add(
                HeaderViewModel_()
                    .id("header")
                    .title(getString(R.string.exodus_view_report))
                    .browseUrl("browse")
                    .click { _ -> context?.browse(Constants.EXODUS_REPORT_URL + args.report.id) }
            )
            reviews.forEach {
                add(
                    ExodusViewModel_()
                        .id(it.id)
                        .tracker(it)
                        .click { _ ->
                            context?.browse(it.url)
                        }
                )
            }
        }
    }

    private fun getExodusTrackersFromReport(report: Report): List<ExodusTracker> {
        val trackerObjects = report.trackers.map {
            exodusTrackers.getJSONObject(it.toString())
        }.toList()

        return trackerObjects.map {
            ExodusTracker().apply {
                id = it.getInt("id")
                name = it.getString("name")
                url = it.getString("website")
                signature = it.getString("code_signature")
                date = it.getString("creation_date")
                description = it.getString("description")
                networkSignature = it.getString("network_signature")
                documentation = listOf(it.getString("documentation"))
                categories = listOf(it.getString("categories"))
            }
        }.toList()
    }
}
