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

package com.aurora.store.view.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aurora.store.R
import com.aurora.store.data.Filter
import com.aurora.store.data.providers.FilterProvider
import com.aurora.store.databinding.SheetFilterBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterSheet : BaseBottomSheet() {

    private lateinit var B: SheetFilterBinding
    private lateinit var filter: Filter

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetFilterBinding.inflate(inflater, container, false)
        filter = FilterProvider.with(requireContext()).getSavedFilter()
        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        attachSingleChips()
        attachMultipleChips()
        attachActions()
    }

    private fun attachActions() {
        B.btnPositive.setOnClickListener {
            FilterProvider.with(requireContext()).saveFilter(filter)
            dismissAllowingStateLoss()
        }

        B.btnNegative.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun attachSingleChips() {
        B.filterGfs.apply {
            isChecked = filter.gsfDependentApps
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter.gsfDependentApps = checked
            }
        }

        B.filterPaid.apply {
            isChecked = filter.paidApps
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter.paidApps = checked
            }
        }

        B.filterAds.apply {
            isChecked = filter.appsWithAds
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter.appsWithAds = checked
            }
        }
    }

    private fun attachMultipleChips() {
        val downloadLabels = resources.getStringArray(R.array.filterDownloadsLabels)
        val downloadValues = resources.getStringArray(R.array.filterDownloadsValues)
        val ratingLabels = resources.getStringArray(R.array.filterRatingLabels)
        val ratingValues = resources.getStringArray(R.array.filterRatingValues)
        var i = 0

        for (downloadLabel in downloadLabels) {
            val chip = Chip(requireContext())
            chip.id = i
            chip.text = downloadLabel
            chip.isChecked = filter.downloads == downloadValues[i].toInt()
            B.downloadChips.addView(chip)
            i++
        }

        B.downloadChips.setOnCheckedStateChangeListener { _, checkedIds ->
            filter.downloads = downloadValues[checkedIds[0]].toInt()
        }

        i = 0
        for (ratingLabel in ratingLabels) {
            val chip = Chip(requireContext())
            chip.id = i
            chip.text = ratingLabel
            chip.isChecked = filter.rating == ratingValues[i].toFloat()
            B.ratingChips.addView(chip)
            i++
        }

        B.ratingChips.setOnCheckedStateChangeListener { _, checkedIds ->
            filter.rating = ratingValues[checkedIds[0]].toFloat()
        }
    }
}
